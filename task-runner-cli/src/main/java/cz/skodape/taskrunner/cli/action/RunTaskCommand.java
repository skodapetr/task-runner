package cz.skodape.taskrunner.cli.action;

import cz.skodape.taskrunner.cli.AppConfiguration;
import cz.skodape.taskrunner.executor.TaskExecutor;
import cz.skodape.taskrunner.storage.StorageException;
import cz.skodape.taskrunner.storage.instance.SingleTaskStorage;
import cz.skodape.taskrunner.storage.instance.TaskReference;
import cz.skodape.taskrunner.storage.instance.model.TaskInstance;
import cz.skodape.taskrunner.storage.template.TaskTemplateStorage;
import cz.skodape.taskrunner.storage.template.model.TaskTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class RunTaskCommand {

    private static final Logger LOG =
            LoggerFactory.getLogger(RunTaskCommand.class);

    private final File templateDirectory;

    private final File taskDirectory;

    public RunTaskCommand(AppConfiguration configuration) {
        this.templateDirectory = configuration.templateDirectory;
        this.taskDirectory = configuration.taskDirectory;
    }

    public void execute() {
        TaskTemplateStorage templateStorage = new TaskTemplateStorage();
        templateStorage.load(templateDirectory);
        SingleTaskStorage taskStorage = new SingleTaskStorage(taskDirectory);
        TaskReference reference = taskStorage.getTask();
        if (reference == null) {
            LOG.error("Can't find task '{}' from {}",
                    taskDirectory.getName(),
                    taskDirectory.getParent());
            return;
        }
        TaskInstance task;
        try {
            task = taskStorage.getTaskInstance(reference);
        } catch (StorageException ex) {
            LOG.error("Can't load task: {}", reference);
            return;
        }
        TaskTemplate template = templateStorage.getTemplate(task.template);
        if (template == null) {
            LOG.error("Missing task template: {}", task.template);
            return;
        }
        (new TaskExecutor(reference, template, taskStorage)).run();
    }

}
