package cz.skodape.taskrunner.storage.instance;

import cz.skodape.taskrunner.storage.DirectoryUtils;
import cz.skodape.taskrunner.storage.instance.model.TaskConfiguration;
import cz.skodape.taskrunner.storage.instance.model.TaskInstance;
import cz.skodape.taskrunner.storage.instance.storage.WritableTaskStorage;
import cz.skodape.taskrunner.storage.template.model.TaskTemplate;
import cz.skodape.taskrunner.storage.instance.model.TaskStatus;
import cz.skodape.taskrunner.storage.StorageException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Map;

/**
 * Does not perform validation.
 */
public class TaskBuilder {

    private final WritableTaskStorage storage;

    private final WritableTaskStorage.NewTaskData task;

    private final TaskTemplate template;

    public TaskBuilder(
            WritableTaskStorage storage,
            WritableTaskStorage.NewTaskData task,
            TaskTemplate template) {
        this.storage = storage;
        this.task = task;
        this.template = template;
    }

    public TaskBuilder addFile(String name, InputStream stream)
            throws StorageException {
        File directory = storage.getTaskInputDirectory(task);
        directory.mkdirs();
        File file = new File(directory, name);
        try (OutputStream outputStream = new FileOutputStream(file)) {
            stream.transferTo(outputStream);
        } catch (IOException ex) {
            throw new StorageException("Can't save file: {}", name, ex);
        }
        return this;
    }

    public TaskBuilder addConfiguration(Map<String, String> configuration)
            throws StorageException {
        storage.updateTaskConfiguration(
                task, new TaskConfiguration(configuration));
        return this;
    }

    public TaskReference build() throws StorageException {
        TaskInstance instance = new TaskInstance();
        instance.created = Instant.now();
        instance.lastChange = instance.created;
        instance.status = TaskStatus.QUEUED;
        instance.id = task.getName();
        instance.template = template.id;
        instance.lastFinishedStep = 0;
        instance.stepCount = template.steps.size();
        storage.updateTaskInstance(task, instance);
        return storage.addNewTask(task);
    }

    /**
     * Should be called in the build is not finished with success.
     */
    public void clear() {
        DirectoryUtils.delete(task.directory);
    }

    public static TaskBuilder create(
            WritableTaskStorage storage, TaskTemplate template)
            throws StorageException {
        var instance = storage.reserveNewTask(template.id);
        return new TaskBuilder(storage, instance, template);
    }

    public static TaskBuilder createForName(
            WritableTaskStorage storage, TaskTemplate template, String name) {
        String normalizedName =
                template.taskGetIdentificationTransformation.transform(name);
        var instance = storage.reserveNewTaskForName(
                template.id, normalizedName);
        if (instance == null) {
            return null;
        }
        return new TaskBuilder(storage, instance, template);
    }

}
