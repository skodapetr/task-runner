package cz.skodape.taskrunner.cli.service;

import cz.skodape.taskrunner.executor.TaskExecutor;
import cz.skodape.taskrunner.storage.StorageException;
import cz.skodape.taskrunner.storage.instance.storage.StorageObserver;
import cz.skodape.taskrunner.storage.instance.TaskReference;
import cz.skodape.taskrunner.storage.instance.storage.WritableTaskStorage;
import cz.skodape.taskrunner.storage.instance.model.TaskInstance;
import cz.skodape.taskrunner.storage.instance.model.TaskStatus;
import cz.skodape.taskrunner.storage.template.TaskTemplateStorage;
import cz.skodape.taskrunner.storage.template.model.TaskTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecutorService {

    private static final Logger LOG =
            LoggerFactory.getLogger(ExecutorService.class);

    private final WritableTaskStorage taskStorage;

    private final TaskTemplateStorage templateStorage;

    private final int executorThreads;

    private ThreadPoolExecutor threadPool;

    private StorageObserver observer;

    public ExecutorService(
            WritableTaskStorage taskStorage,
            TaskTemplateStorage templateStorage,
            int executorThreads) {
        this.taskStorage = taskStorage;
        this.templateStorage = templateStorage;
        this.executorThreads = executorThreads;
    }

    public void start() throws IOException {
        createTemplateDirectories();
        createThreadPool();
        startQueued();
        registerStorageObserver();
    }

    /**
     * We need this for {@link #registerStorageObserver()} as it
     * {@link StorageObserver} need directories to exist.
     */
    private void createTemplateDirectories() {
        for (String templateName : templateStorage.getTemplateIds()) {
            taskStorage.secureTemplateTaskDirectory(templateName);
        }
    }

    private void createThreadPool() {
        // TODO Use custom naming for threads see Executors#defaultThreadFactory
        threadPool = (ThreadPoolExecutor)
                Executors.newFixedThreadPool(executorThreads);
    }

    private void startQueued() {
        for (String templateId : templateStorage.getTemplateIds()) {
            TaskTemplate template = templateStorage.getTemplate(templateId);
            if (template.readOnly) {
                continue;
            }
            List<TaskReference> tasks =
                    taskStorage.getTasksForTemplate(templateId);
            for (TaskReference reference : tasks) {
                checkAndRunTask(reference);
            }
        }
    }

    private void checkAndRunTask(TaskReference reference) {
        TaskInstance task;
        try {
            task = taskStorage.getTaskInstance(reference);
        } catch (StorageException ex) {
            LOG.error("Can't get task {}", reference, ex);
            return;
        }
        if (TaskStatus.QUEUED != task.status) {
            return;
        }
        LOG.info("Adding task to a queue {}", reference);
        TaskTemplate template = templateStorage.getTemplate(task.template);
        if (template == null) {
            LOG.error("Invalid template for {}", reference);
            return;
        }
        threadPool.execute(new TaskExecutor(reference, template, taskStorage));
    }

    private void registerStorageObserver() throws IOException {
        observer = new StorageObserver(taskStorage, this::checkAndRunTask);
        observer.start();
    }

    public void stop() {
        if (observer != null) {
            observer.stop();
        }
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                while (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    LOG.info("Awaiting completion of threads ...");
                }
            } catch (InterruptedException ex) {
                LOG.info("Interrupted when waiting for tasks to finish.");
            }
        }
    }

}
