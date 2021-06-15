package cz.skodape.taskrunner.storage.instance.prune;

import cz.skodape.taskrunner.storage.StorageException;
import cz.skodape.taskrunner.storage.instance.TaskReference;
import cz.skodape.taskrunner.storage.instance.model.TaskInstance;
import cz.skodape.taskrunner.storage.instance.model.TaskStatus;
import cz.skodape.taskrunner.storage.instance.storage.WritableTaskStorage;
import cz.skodape.taskrunner.storage.template.TaskTemplateStorage;
import cz.skodape.taskrunner.storage.template.model.TaskTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class PruneService implements Runnable {

    /**
     * We check for tasks to delete every two minutes.
     */
    public static final int CHECK_INTERVAL_SECONDS = 120;

    private static final Logger LOG =
            LoggerFactory.getLogger(PruneService.class);

    private Thread thread = null;

    private final WritableTaskStorage taskStorage;

    private final TaskTemplateStorage templateStorage;

    public PruneService(
            WritableTaskStorage taskStorage,
            TaskTemplateStorage templateStorage) {
        this.taskStorage = taskStorage;
        this.templateStorage = templateStorage;
    }

    public void start() {
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.setName("prune-service");
        thread.start();
        LOG.info("Prune service is running ...");
    }

    public void stop() {
        LOG.info("Stopping prune service ...");
        while (true) {
            thread.interrupt();
            try {
                thread.join(1000);
            } catch (InterruptedException ex) {
                LOG.info("Interrupted while waiting on prune thread.");
                break;
            }
            if (!thread.isAlive()) {
                break;
            }
            LOG.info("Waiting for prune thread to finish.");
        }
    }

    @Override
    public void run() {
        List<TaskTemplate> templates = getTemplatesWitTll();
        while (true) {
            sleep();
            if (Thread.interrupted()) {
                LOG.info("Prune thread was interrupted and is ending.");
                return;
            }
            for (TaskTemplate template : templates) {
                checkTasks(template);
            }
        }
    }

    private List<TaskTemplate> getTemplatesWitTll() {
        return templateStorage.getTemplateIds().stream()
                .map(templateStorage::getTemplate)
                .filter(template -> template.timeToLive != null)
                .collect(Collectors.toList());
    }

    private void sleep() {
        try {
            Thread.sleep(CHECK_INTERVAL_SECONDS * 1000);
        } catch (InterruptedException ex) {
            return;
        }
    }

    private void checkTasks(TaskTemplate template) {
        taskStorage.getTasksForTemplate(template.id)
                .forEach(reference -> checkTask(template, reference));
    }

    private void checkTask(TaskTemplate template, TaskReference reference) {
        TaskInstance instance;
        try {
            instance = taskStorage.getTaskInstance(reference);
        } catch (StorageException ex) {
            LOG.debug("Can't read task '{}':'{}'",
                    reference.getTemplate(), reference.getId());
            return;
        }
        if (!TaskStatus.isFinished(instance.status)) {
            return;
        }
        Instant deleteTime = instance.lastChange.plus(
                template.timeToLive, ChronoUnit.SECONDS);
        if (deleteTime.isAfter(Instant.now())) {
            return;
        }
        LOG.debug("Removing task '{}':'{}",
                reference.getTemplate(), reference.getId());
        taskStorage.delete(reference);
    }

}
