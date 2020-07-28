package cz.skodape.taskrunner.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.skodape.taskrunner.storage.StorageException;
import cz.skodape.taskrunner.storage.instance.TaskReference;
import cz.skodape.taskrunner.storage.instance.WritableTaskStorage;
import cz.skodape.taskrunner.storage.instance.model.TaskInstance;
import cz.skodape.taskrunner.storage.template.TaskTemplateStorage;
import cz.skodape.taskrunner.storage.template.model.TaskTemplate;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Path("task")
public class TaskRestApi extends Application {

    private static final Logger LOG =
            LoggerFactory.getLogger(TaskRestApi.class);

    private static final DateTimeFormatter dataFormat;

    private final WritableTaskStorage taskStorage;

    private final TaskTemplateStorage templateStorage;

    private final ObjectMapper objectMapper = new ObjectMapper();

    static {
        dataFormat = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .toFormatter()
                .withZone(ZoneId.of("UTC"));
    }

    public TaskRestApi(
            WritableTaskStorage taskStorage,
            TaskTemplateStorage templateStorage) {
        this.taskStorage = taskStorage;
        this.templateStorage = templateStorage;
    }

    @GET
    @Path("/")
    public Response getAll() {
        ObjectNode root = objectMapper.createObjectNode();
        root.set("data", asArrayNode(taskStorage.getTasks()));
        return Response.ok((StreamingOutput) (object) ->
                objectMapper.writeValue(object, root))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    private ArrayNode asArrayNode(Collection<TaskReference> references) {
        ArrayNode root = objectMapper.createArrayNode();
        for (TaskReference reference : references) {
            TaskInstance instance;
            try {
                instance = taskStorage.getTaskInstance(reference);
            } catch (StorageException ex) {
                // Ignore invalid records.
                continue;
            }
            root.add(asObjectNode(instance));
        }
        return root;
    }

    private ObjectNode asObjectNode(TaskInstance instance) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("id", instance.id);
        root.put("created", dataFormat.format(instance.created));
        root.put("lastChange", dataFormat.format(instance.lastChange));
        root.put("lastFinishedStep", instance.lastFinishedStep);
        root.put("stepCount", instance.stepCount);
        root.put("template", templateIdToPath(instance.template));
        root.put("status", instance.status.asString());
        return root;
    }

    private String templateIdToPath(String templateId) {
        TaskTemplate template = templateStorage.getTemplate(templateId);
        if (template == null) {
            return null;
        }
        return template.urlPath;
    }

    @POST
    @Path("/{template}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response createTask(
            @Context UriInfo uriInfo,
            @PathParam("template") String templatePath,
            @FormDataParam("input") List<FormDataBodyPart> files) {
        return (new CreateTaskAction(taskStorage, templateStorage))
                .create(templatePath, null, uriInfo, files,
                        (reference -> Response.created(
                                createLocationForNewTask(reference)
                        ).build()));
    }

    private URI createLocationForNewTask(TaskReference reference) {
        TaskTemplate template = templateStorage.getTemplate(
                reference.getTemplate());
        String uriAsString = template.newLocationHttpTemplate
                .replace("{template}", reference.getTemplate())
                .replace("{task}", reference.getId());
        return URI.create(uriAsString);
    }

    @GET
    @Path("/{template}")
    public Response getForTemplate(@PathParam("template") String templatePath) {
        ObjectNode root = objectMapper.createObjectNode();
        TaskTemplate template = templateStorage.getTemplateByPath(templatePath);
        root.set(
                "data",
                asArrayNode(taskStorage.getTasksForTemplate(template.id)));
        return Response.ok((StreamingOutput) (object) ->
                objectMapper.writeValue(object, root))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    @GET
    @Path("/{template}/{id}")
    public Response getStatus(
            @PathParam("template") String templatePath,
            @PathParam("id") String taskId) {
        TaskTemplate template = templateStorage.getTemplateByPath(templatePath);
        if (template == null) {
            return notFound();
        }
        TaskReference reference = taskStorage.getTask(template.id, taskId);
        if (reference != null) {
            return responseReference(reference);
        }
        if (template.createOnGet) {
            return createOnGet(templatePath, taskId);
        } else {
            return notFound();
        }
    }

    private static Response notFound() {
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    private Response responseReference(TaskReference reference) {
        TaskInstance task;
        try {
            task = taskStorage.getTaskInstance(reference);
        } catch (StorageException ex) {
            LOG.info("Can't read task: {}", reference);
            return serverError();
        }
        ObjectNode root = asObjectNode(task);
        return Response.ok((StreamingOutput) (object) ->
                objectMapper.writeValue(object, root))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    private static Response serverError() {
        return Response.serverError().build();
    }

    private Response createOnGet(String templatePath, String taskId) {
        return (new CreateTaskAction(taskStorage, templateStorage))
                .create(templatePath, taskId, null, Collections.emptyList(),
                        (this::responseReference));
    }

    @GET
    @Path("/{template}/{id}/stdout")
    public Response getStdOut(
            @PathParam("template") String templatePath,
            @PathParam("id") String taskId) {
        return forTaskReference(templatePath, taskId, reference -> {
            File file = taskStorage.getTaskStdOut(reference);
            return streamFile(file);
        });
    }

    private Response forTaskReference(
            String templatePath, String taskId,
            Function<TaskReference, Response> handler) {
        TaskTemplate template = templateStorage.getTemplateByPath(templatePath);
        if (template == null) {
            return notFound();
        }
        TaskReference reference = taskStorage.getTask(template.id, taskId);
        if (reference == null) {
            return notFound();
        }
        return handler.apply(reference);
    }

    private Response streamFile(File file) {
        if (file.exists()) {
            return Response.ok((StreamingOutput) (output) -> {
                try (InputStream stream = new FileInputStream(file)) {
                    stream.transferTo(output);
                }
            }).build();
        } else {
            return notFound();
        }
    }

    @GET
    @Path("/{template}/{id}/errout")
    public Response getErrOut(
            @PathParam("template") String templatePath,
            @PathParam("id") String taskId) {
        return forTaskReference(templatePath, taskId, reference -> {
            File file = taskStorage.getTaskErrOut(reference);
            return streamFile(file);
        });
    }

    @GET
    @Path("/{template}/{id}/public/{file: .*}")
    public Response getPublicFile(
            @PathParam("template") String templatePath,
            @PathParam("id") String taskId,
            @PathParam("file") String userFileName,
            @Context UriInfo uriInfo) {
        final String fileName = userFileName.replace("\\", "/");
        if (!isSecure(fileName)) {
            return notFound();
        }
        return forTaskReference(templatePath, taskId, reference -> {
            File publicDir = taskStorage.getTaskPublicDirectory(reference);
            File file = new File(publicDir, fileName);
            return streamFile(file);
        });
    }

    private static boolean isSecure(String fileName) {
        return !fileName.contains("/./") && !fileName.contains("/../");
    }

}
