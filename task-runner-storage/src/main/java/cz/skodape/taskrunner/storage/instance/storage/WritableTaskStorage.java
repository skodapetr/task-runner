package cz.skodape.taskrunner.storage.instance.storage;

import cz.skodape.taskrunner.storage.DirectoryUtils;
import cz.skodape.taskrunner.storage.instance.TaskReference;
import cz.skodape.taskrunner.storage.instance.model.TaskConfiguration;
import cz.skodape.taskrunner.storage.instance.model.TaskConfigurationJacksonAdapter;
import cz.skodape.taskrunner.storage.instance.model.TaskInstance;
import cz.skodape.taskrunner.storage.StorageException;
import cz.skodape.taskrunner.storage.instance.model.TaskInstanceJacksonAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Add support for creation of new tasks.
 */
public class WritableTaskStorage extends DirectoryTaskStorage {

    private static final int NUMBER_OF_RETRY_FOR_CREATE = 10;

    /**
     * A task identification used for creating new tasks.
     */
    public static class NewTaskData {

        /**
         * New task reference.
         */
        private final TaskReference taskReference;

        /**
         * Directory in which the new task should be prepared.
         */
        public final File directory;

        public NewTaskData(TaskReference taskReference, File directory) {
            this.taskReference = taskReference;
            this.directory = directory;
        }

        public String getName() {
            return taskReference.getId();
        }

    }

    private static final Logger LOG =
            LoggerFactory.getLogger(WritableTaskStorage.class);

    private final File workingDirectory;

    public WritableTaskStorage(File dataDirectory, File workingDirectory) {
        super(dataDirectory);
        this.workingDirectory = workingDirectory;
    }

    public NewTaskData reserveNewTask(String template)
            throws StorageException {
        getNewTaskDirectory().mkdirs();
        File taskDirectory = createNewTaskDirectory(template);
        return new NewTaskData(
                TaskReference.create(template, taskDirectory.getName()),
                taskDirectory);
    }


    private File createNewTaskDirectory(String template)
            throws StorageException {
        for (int i = 0; i < NUMBER_OF_RETRY_FOR_CREATE; ++i) {
            UUID uuid = UUID.randomUUID();
            String name = uuid.toString();
            File file = new File(getNewTaskDirectory(), name);
            if (!file.mkdirs()) {
                continue;
            }
            if (isFileUsed(template, name)) {
                file.delete();
                continue;
            }
            return file;
        }
        throw new StorageException("Can't create storage directory");
    }

    private boolean isFileUsed(String template, String name) {
        return getTaskDirectory(template, name).exists();
    }

    private File getNewTaskDirectory() {
        return workingDirectory;
    }

    /**
     * Return null if the task already exist.
     */
    public NewTaskData reserveNewTaskForName(String template, String name) {
        String fileName = template + "-" + name;
        File file = new File(getNewTaskDirectory(), fileName);
        if (!file.mkdirs()) {
            LOG.info("Can't create working directory for '{}'", fileName);
            return null;
        }
        if (isFileUsed(template, name)) {
            LOG.info(
                    "Can't reserve task name '{}' as it is already used.",
                    fileName
            );
            file.delete();
            return null;
        }
        return new NewTaskData(TaskReference.create(template, name), file);
    }

    public TaskReference addNewTask(NewTaskData reference)
            throws StorageException {
        File destination = getTaskDirectory(reference.taskReference);
        // Make sure data directory exists.
        destination.getParentFile().mkdirs();
        try {
            Files.move(reference.directory.toPath(), destination.toPath(),
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            throw new StorageException("Can't save task.", ex);
        }
        return reference.taskReference;
    }

    public void updateTaskInstance(
            NewTaskData reference, TaskInstance instance)
            throws StorageException {
        File taskFile = getTaskInstanceFile(reference);
        writeAndSwap(taskFile, TaskInstanceJacksonAdapter.asJson(instance));
    }

    protected File getTaskInstanceFile(NewTaskData reference) {
        return new File(reference.directory, INSTANCE_FILE_NAME);
    }

    public File getTaskInputDirectory(NewTaskData reference) {
        return new File(reference.directory, INPUT_DIR_NAME);
    }

    public void updateTaskConfiguration(
            NewTaskData reference, TaskConfiguration configuration)
            throws StorageException {
        File taskFile = getTaskConfigurationFile(reference);
        writeAndSwap(
                taskFile,
                TaskConfigurationJacksonAdapter.asJson(configuration));
    }

    protected File getTaskConfigurationFile(NewTaskData reference) {
        return new File(reference.directory, CONFIGURATION_FILE_NAME);
    }

    /**
     * Make sure that directory for given task of given template name exists.
     */
    public void secureTemplateTaskDirectory(String name) {
        File path = new File(dataDirectory, name);
        if (!path.exists()) {
            path.mkdirs();
        }
    }

    public void delete(TaskReference reference) {
        // TODO Add into a queue and try to delete in future if a delete fail.
        DirectoryUtils.delete(getTaskDirectory(reference));
    }

}
