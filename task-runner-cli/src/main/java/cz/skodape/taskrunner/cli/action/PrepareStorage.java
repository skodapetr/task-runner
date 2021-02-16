package cz.skodape.taskrunner.cli.action;

import cz.skodape.taskrunner.storage.StorageException;
import cz.skodape.taskrunner.storage.instance.TaskReference;
import cz.skodape.taskrunner.storage.instance.WritableTaskStorage;
import cz.skodape.taskrunner.storage.instance.model.TaskInstance;
import cz.skodape.taskrunner.storage.instance.model.TaskStatus;
import cz.skodape.taskrunner.storage.template.TaskTemplateStorage;
import cz.skodape.taskrunner.storage.template.model.TaskTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

/**
 * There might be unfinished task etc ...
 */
public class PrepareStorage {

    private static final Logger LOG =
            LoggerFactory.getLogger(PrepareStorage.class);

    private final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

    private final WritableTaskStorage taskStorage;

    private final TaskTemplateStorage templateStorage;

    public PrepareStorage(
            WritableTaskStorage taskStorage,
            TaskTemplateStorage templateStorage) {
        this.taskStorage = taskStorage;
        this.templateStorage = templateStorage;
    }

    public void prepare() {
        LOG.info("Checking tasks ...");
        for (String templateName : templateStorage.getTemplateIds()) {
            TaskTemplate template = templateStorage.getTemplate(templateName);
            if (template.readOnly) {
                continue;
            }
            taskStorage.getTasksForTemplate(templateName)
                    .forEach(this::prepareTaskReference);
        }
        LOG.info("Checking tasks ... done");
    }

    protected void prepareTaskReference(TaskReference reference) {
        TaskInstance task;
        try {
            task = taskStorage.getTaskInstance(reference);
        } catch (StorageException ex) {
            LOG.warn("Can't read task: {}", reference);
            return;
        }
        if (task.status == TaskStatus.RUNNING) {
            try {
                sanitizeRunningTask(reference, task);
            } catch (IOException | StorageException ex) {
                LOG.warn("Can't sanitize task: {}", reference);
            }
        }
    }

    protected void sanitizeRunningTask(
            TaskReference reference, TaskInstance task
    ) throws IOException, StorageException {
        File stdout = taskStorage.getTaskStdOut(reference);
        String restartMessage = ""
                + "\n[ "
                + DATE_FORMAT.format(new Date())
                + " ] : Restarting to last step ...\n";
        Files.writeString(stdout.toPath(), restartMessage);
        task.status = TaskStatus.QUEUED;
        task.lastChange = Instant.now();
        //
        taskStorage.updateTaskInstance(reference, task);
    }

}
