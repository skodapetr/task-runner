package cz.skodape.taskrunner.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.skodape.taskrunner.storage.StorageException;
import cz.skodape.taskrunner.storage.instance.TaskReference;
import cz.skodape.taskrunner.storage.instance.storage.WritableTaskStorage;
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
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Path("task")
public class TaskRestApi extends Application {

    static class ReferenceWrap {

        public final TaskReference task;

        public final TaskTemplate template;

        public ReferenceWrap(TaskReference task, TaskTemplate template) {
            this.task = task;
            this.template = template;
        }

    }

    private static final Logger LOG =
            LoggerFactory.getLogger(TaskRestApi.class);

    private static final DateTimeFormatter dataFormat;

    private final WritableTaskStorage taskStorage;

    private final TaskTemplateStorage templateStorage;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final FileToResponse fileToResponse = new FileToResponse();

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
    public Response getAll() {
        ObjectNode root = objectMapper.createObjectNode();
        List<TaskReference> tasks = new ArrayList<>();
        for (String templateId : templateStorage.getTemplateIds()) {
            TaskTemplate template = templateStorage.getTemplate(templateId);
            if (template.disableListing) {
                continue;
            }
            tasks.addAll(taskStorage.getTasksForTemplate(templateId));
        }
        root.set("data", asArrayNode(tasks));
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
            Request request,
            @Context UriInfo uriInfo,
            @PathParam("template") String templatePath,
            @FormDataParam("input") List<FormDataBodyPart> files) {
        return (new CreateTaskAction(taskStorage, templateStorage)).create(
                templatePath, null, uriInfo, files,
                this::responseReferenceForPost
        );
    }

    /**
     * We may need to apply redirect here.
     */
    private Response responseReferenceForPost(
            TaskTemplate template, TaskReference reference) {
        if (template.postResponseRedirectUrl == null) {
            // We can't return location as it is resolved to absolute URL,
            // so instead we return the task identification.
            return Response.status(Response.Status.CREATED)
                    .header("task-runner-template", reference.getTemplate())
                    .header("task-runner-task", reference.getId())
                    .build();
        }
        String url = CreateRedirectUrl.createUrl(template, reference);
        return Response.status(Response.Status.SEE_OTHER)
                .header("location", url)
                .build();
    }

    @GET
    @Path("/{template}")
    public Response getForTemplate(@PathParam("template") String templatePath) {
        ObjectNode root = objectMapper.createObjectNode();
        TaskTemplate template = templateStorage.getTemplateByPath(templatePath);
        if (template == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (template.disableListing) {
            root.set("data", objectMapper.createArrayNode());
        } else {
            root.set(
                    "data",
                    asArrayNode(taskStorage.getTasksForTemplate(template.id)));
        }
        return Response.ok((StreamingOutput) (object) ->
                objectMapper.writeValue(object, root))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    @GET
    @Path("/{template}/{id}/status.json")
    public Response getTaskStatus(
            @PathParam("template") String templatePath,
            @PathParam("id") String taskId) {
        return this.getTask(templatePath, taskId);
    }

    @GET
    @Path("/{template}/{id}")
    public Response getTask(
            @PathParam("template") String templatePath,
            @PathParam("id") String taskId) {
        TaskTemplate template = templateStorage.getTemplateByPath(templatePath);
        if (template == null) {
            return notFound();
        }
        TaskReference reference = taskStorage.getTask(template.id, taskId);
        if (reference != null) {
            return responseStatus(reference);
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

    private Response responseStatus(TaskReference reference) {
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
        return (new CreateTaskAction(taskStorage, templateStorage)).create(
                templatePath, taskId, null, Collections.emptyList(),
                (template, reference) -> responseStatus(reference)
        );
    }

    @GET
    @Path("/{template}/{id}/stdout")
    public Response getStdOut(
            @PathParam("template") String templatePath,
            @PathParam("id") String taskId) {
        return forTaskReference(templatePath, taskId, reference -> {
            File file = taskStorage.getTaskStdOut(reference.task);
            return fileToResponse.streamFile(file);
        });
    }

    private Response forTaskReference(
            String templatePath, String taskId,
            Function<ReferenceWrap, Response> handler) {
        TaskTemplate template = templateStorage.getTemplateByPath(templatePath);
        if (template == null) {
            return notFound();
        }
        TaskReference reference = taskStorage.getTask(template.id, taskId);
        if (reference == null) {
            return notFound();
        }
        return handler.apply(new ReferenceWrap(reference, template));
    }

    @GET
    @Path("/{template}/{id}/errout")
    public Response getErrOut(
            @PathParam("template") String templatePath,
            @PathParam("id") String taskId) {
        return forTaskReference(templatePath, taskId, reference -> {
            File file = taskStorage.getTaskErrOut(reference.task);
            return fileToResponse.streamFile(file);
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
        if (!isFileNameSecure(fileName)) {
            return notFound();
        }
        return forTaskReference(templatePath, taskId, reference -> {
            File publicDir = taskStorage.getTaskPublicDirectory(reference.task);
            File file = new File(publicDir, fileName);
            if (reference.template.allowGzipPublicFiles) {
                return fileToResponse.streamFileOrGzip(file);
            } else {
                return fileToResponse.streamFile(file);
            }
        });
    }

    private static boolean isFileNameSecure(String fileName) {
        return !fileName.contains("/./") && !fileName.contains("/../");
    }

}
