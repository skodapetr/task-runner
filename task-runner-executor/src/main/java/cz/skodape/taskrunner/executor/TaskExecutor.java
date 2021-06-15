package cz.skodape.taskrunner.executor;

import cz.skodape.taskrunner.storage.StorageException;
import cz.skodape.taskrunner.storage.instance.TaskReference;
import cz.skodape.taskrunner.storage.instance.storage.TaskStorage;
import cz.skodape.taskrunner.storage.instance.model.TaskInstance;
import cz.skodape.taskrunner.storage.instance.model.TaskStatus;
import cz.skodape.taskrunner.storage.template.model.TaskTemplate;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public class TaskExecutor implements Runnable {

    private static final Logger LOG =
            LoggerFactory.getLogger(TaskExecutor.class);

    private final TaskReference task;

    private final TaskTemplate template;

    private final TaskStorage storage;

    public TaskExecutor(
            TaskReference task, TaskTemplate template,
            TaskStorage storage) {
        this.task = task;
        this.template = template;
        this.storage = storage;
    }

    @Override
    public void run() {
        LOG.info("Running task: {} ...", task);
        try {
            prepareDirectories();
            executeTask();
        } catch (Throwable ex) {
            LOG.error("Execution of task: '{}' failed.", task, ex);
        }
        LOG.info("Running task: {} ... done", task);
    }

    private void prepareDirectories() {
        storage.getTaskPublicDirectory(task).mkdirs();
        storage.getTaskWorkingDirectory(task).mkdirs();
        storage.getTaskInputDirectory(task).mkdirs();
    }

    private void executeTask() throws StorageException {
        onStartTask();
        List<String> commands = createCommands();
        if (executeCommands(commands)) {
            onExecutionFinished();
        } else {
            onExecutionFailed();
        }
    }

    private void onStartTask() throws StorageException {
        TaskInstance instance = storage.getTaskInstance(task);
        instance.pid = ProcessHandle.current().pid();
        instance.status = TaskStatus.RUNNING;
        instance.lastChange = Instant.now();
        storage.updateTaskInstance(task, instance);
    }

    private List<String> createCommands() throws StorageException {
        TaskStepsToCommands taskStepsToCommands = new TaskStepsToCommands(
                storage.getTaskConfiguration(task),
                storage.getTaskInstance(task),
                template,
                storage.getTaskPublicDirectory(task),
                storage.getTaskWorkingDirectory(task),
                storage.getTaskInputDirectory(task));
        return taskStepsToCommands.asCommands();
    }

    private boolean executeCommands(List<String> commands)
            throws StorageException {
        for (int index = 0; index < commands.size(); ++index) {
            String name = template.steps.get(index).name;
            String command = commands.get(index);
            LOG.info("Executing command {}/{} : {}",
                    index + 1, commands.size(), name);
            if (executeCommand(command)) {
                onTaskFinished();
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean executeCommand(String command) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        setCommand(processBuilder, command);
        ProcessBuilder.Redirect stdout =
                ProcessBuilder.Redirect.appendTo(storage.getTaskStdOut(task));
        processBuilder.redirectOutput(stdout);
        if (template.mergeErrOutToStdOut) {
            processBuilder.redirectError(stdout);
        } else {
            processBuilder.redirectError(
                    ProcessBuilder.Redirect.appendTo(
                            storage.getTaskErrOut(task)));
        }
        processBuilder.directory(storage.getTaskWorkingDirectory(task));
        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException ex) {
            LOG.error("Can't start process.", ex);
            return false;
        }
        try {
            process.waitFor();
        } catch (InterruptedException ex) {
            LOG.error("Killing process.");
            process.destroy();
            return false;
        }
        if (process.exitValue() > 0) {
            LOG.error("Process ended with status {}", process.exitValue());
            return false;
        }
        return true;
    }

    private void setCommand(ProcessBuilder processBuilder, String command) {
        if (SystemUtils.IS_OS_WINDOWS) {
            processBuilder.command("cmd.exe", "/c", command);
        } else {
            processBuilder.command("sh", "-c", command);
        }
    }

    private void onTaskFinished() throws StorageException {
        TaskInstance instance = storage.getTaskInstance(task);
        instance.lastFinishedStep += 1;
        instance.lastChange = Instant.now();
        storage.updateTaskInstance(task, instance);
    }

    private void onExecutionFailed() throws StorageException {
        TaskInstance instance = storage.getTaskInstance(task);
        instance.status = TaskStatus.FAILED;
        instance.lastChange = Instant.now();
        instance.pid = null;
        storage.updateTaskInstance(task, instance);
    }

    private void onExecutionFinished() throws StorageException {
        TaskInstance instance = storage.getTaskInstance(task);
        instance.status = TaskStatus.SUCCESSFUL;
        instance.lastChange = Instant.now();
        instance.pid = null;
        storage.updateTaskInstance(task, instance);
    }

}
