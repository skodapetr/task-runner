package cz.skodape.taskrunner.storage.instance;

import java.io.File;

/**
 * Allow to use a single directory with a task as a storage.
 */
public class SingleTaskStorage extends TaskStorage {

    private final File taskDirectory;

    private final TaskReference taskReference;

    public SingleTaskStorage(File taskDirectory) {
        this.taskDirectory = taskDirectory;
        this.taskReference = TaskReference.create("", taskDirectory.getName());
    }

    @Override
    public TaskReference getTask(String template, String name) {
        throw new UnsupportedOperationException();
    }

    public TaskReference getTask() {
        return taskReference;
    }

    @Override
    protected File getTaskDirectory(TaskReference reference) {
        return taskDirectory;
    }

}
