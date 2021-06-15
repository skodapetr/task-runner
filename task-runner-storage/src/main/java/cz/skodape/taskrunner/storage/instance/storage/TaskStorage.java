package cz.skodape.taskrunner.storage.instance.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.skodape.taskrunner.storage.DirectoryUtils;
import cz.skodape.taskrunner.storage.ModelException;
import cz.skodape.taskrunner.storage.StorageException;
import cz.skodape.taskrunner.storage.instance.TaskReference;
import cz.skodape.taskrunner.storage.instance.model.TaskConfiguration;
import cz.skodape.taskrunner.storage.instance.model.TaskConfigurationJacksonAdapter;
import cz.skodape.taskrunner.storage.instance.model.TaskInstance;
import cz.skodape.taskrunner.storage.instance.model.TaskInstanceJacksonAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public abstract class TaskStorage {

    protected static final String INSTANCE_FILE_NAME = "status.json";

    protected static final String CONFIGURATION_FILE_NAME =
            "configuration.json";

    protected static final String STDOUT_FILE_NAME = "stdout";

    protected static final String ERROUT_FILE_NAME = "errout";

    protected static final String INPUT_DIR_NAME = "input";

    protected static final String PUBLIC_DIR_NAME = "public";

    protected static final String WORKING_DIR_NAME = "working";

    protected final ObjectMapper objectMapper = new ObjectMapper();

    public abstract TaskReference getTask(String template, String name);

    public TaskInstance getTaskInstance(TaskReference reference)
            throws StorageException {
        File file = getTaskInstanceFile(reference);
        try {
            return TaskInstanceJacksonAdapter.asInstance(asObjectNode(file));
        } catch (NullPointerException | ModelException ex) {
            throw new StorageException(
                    "Can't load task instance from '{}'", file, ex);
        }
    }

    protected File getTaskInstanceFile(TaskReference reference) {
        return new File(getTaskDirectory(reference), INSTANCE_FILE_NAME);
    }

    protected abstract File getTaskDirectory(TaskReference reference);

    protected ObjectNode asObjectNode(File file) throws StorageException {
        try {
            return (ObjectNode) objectMapper.readTree(file);
        } catch (IOException ex) {
            throw new StorageException("Can't read json file.", ex);
        }
    }

    public TaskConfiguration getTaskConfiguration(TaskReference reference)
            throws StorageException {
        File file = getTaskConfigurationFile(reference);
        return TaskConfigurationJacksonAdapter.asConfiguration(
                asObjectNode(file));
    }

    protected File getTaskConfigurationFile(TaskReference reference) {
        return new File(getTaskDirectory(reference), CONFIGURATION_FILE_NAME);
    }

    public File getTaskInputDirectory(TaskReference reference) {
        return new File(getTaskDirectory(reference), INPUT_DIR_NAME);
    }

    public File getTaskWorkingDirectory(TaskReference reference) {
        return new File(getTaskDirectory(reference), WORKING_DIR_NAME);
    }

    public File getTaskPublicDirectory(TaskReference reference) {
        return new File(getTaskDirectory(reference), PUBLIC_DIR_NAME);
    }

    public File getTaskStdOut(TaskReference reference) {
        return new File(getTaskDirectory(reference), STDOUT_FILE_NAME);
    }

    public File getTaskErrOut(TaskReference reference) {
        return new File(getTaskDirectory(reference), ERROUT_FILE_NAME);
    }

    public void delete(TaskReference reference) {
        // TODO Add into a queue and try to delete in future if a delete fail.
        DirectoryUtils.delete(getTaskDirectory(reference));
    }

    public void updateTaskInstance(
            TaskReference reference, TaskInstance instance)
            throws StorageException {
        File taskFile = getTaskInstanceFile(reference);
        writeAndSwap(taskFile, TaskInstanceJacksonAdapter.asJson(instance));
    }

    protected void writeAndSwap(File file, JsonNode content)
            throws StorageException {
        File taskSwapFile = new File(
                file.getParent(), file.getName() + ".swap");
        try {
            objectMapper.writeValue(taskSwapFile, content);
            Files.move(taskSwapFile.toPath(), file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new StorageException("Can't save task.", ex);
        }
    }

}
