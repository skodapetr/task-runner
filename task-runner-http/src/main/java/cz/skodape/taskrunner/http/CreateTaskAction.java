package cz.skodape.taskrunner.http;

import cz.skodape.taskrunner.storage.StorageException;
import cz.skodape.taskrunner.storage.instance.TaskBuilder;
import cz.skodape.taskrunner.storage.instance.TaskReference;
import cz.skodape.taskrunner.storage.instance.WritableTaskStorage;
import cz.skodape.taskrunner.storage.template.TaskTemplateStorage;
import cz.skodape.taskrunner.storage.template.model.TaskTemplate;
import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Handles task creation.
 */
class CreateTaskAction {

    private final static String FILE_PART_NAME = "input";

    private final WritableTaskStorage taskStorage;

    private final TaskTemplateStorage templateStorage;

    public CreateTaskAction(
            WritableTaskStorage taskStorage,
            TaskTemplateStorage templateStorage) {
        this.taskStorage = taskStorage;
        this.templateStorage = templateStorage;
    }

    public Response create(
            String templatePath, String taskId,
            UriInfo uriInfo, List<FormDataBodyPart> files,
            Function<TaskReference, Response> responseFactory) {
        TaskTemplate template = templateStorage.getTemplateByPath(templatePath);
        if (template == null || template.readOnly) {
            return serverError();
        }
        TaskBuilder builder = createBuilder(template, taskId);
        if (builder == null) {
            return serverError();
        }
        TaskReference reference;
        try {
            addConfiguration(builder, uriInfo);
            addFiles(builder, template, files);
            reference = builder.build();
        } catch (StorageException ex) {
            builder.clear();
            return serverError();
        }
        return responseFactory.apply(reference);
    }

    private static Response serverError() {
        return Response.serverError().build();
    }

    private TaskBuilder createBuilder(TaskTemplate template, String taskId) {
        if (taskId == null) {
            try {
                return TaskBuilder.create(taskStorage, template);
            } catch (StorageException ex) {
                return null;
            }
        } else {
            return TaskBuilder.createForName(taskStorage, template, taskId);
        }
    }

    private static void addConfiguration(TaskBuilder builder, UriInfo uriInfo)
            throws StorageException {
        if (uriInfo == null) {
            builder.addConfiguration(Collections.emptyMap());
            return;
        }
        Map<String, String> configuration = new HashMap<>();
        for (var entry : uriInfo.getQueryParameters().entrySet()) {
            List<String> values = entry.getValue();
            if (values.isEmpty()) {
                continue;
            }
            String key = entry.getKey();
            configuration.put(key, values.get(0));
        }
        builder.addConfiguration(configuration);
    }

    private static void addFiles(
            TaskBuilder builder,
            TaskTemplate template,
            List<FormDataBodyPart> files) throws StorageException {
        if (!template.allowInputFiles) {
            return;
        }
        for (FormDataBodyPart part : files) {
            if (!FILE_PART_NAME.equals(part.getName())) {
                continue;
            }
            FormDataContentDisposition contentDisposition =
                    part.getFormDataContentDisposition();
            String name = contentDisposition.getFileName();
            BodyPartEntity bodyPart = (BodyPartEntity) part.getEntity();
            builder.addFile(name, bodyPart.getInputStream());
        }
    }

}
