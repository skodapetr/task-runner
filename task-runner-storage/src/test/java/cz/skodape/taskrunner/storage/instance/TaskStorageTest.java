package cz.skodape.taskrunner.storage.instance;

import cz.skodape.taskrunner.storage.TestResources;
import cz.skodape.taskrunner.storage.StorageException;
import cz.skodape.taskrunner.storage.instance.model.TaskConfiguration;
import cz.skodape.taskrunner.storage.instance.model.TaskInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class TaskStorageTest {

    @Test
    public void loadDirectoryStorage() throws StorageException {
        File directory = TestResources.file("./task");
        DirectoryTaskStorage storage = new DirectoryTaskStorage(directory);

        Assertions.assertNotNull(storage.getTask("add-integer", "task-000"));
        Assertions.assertNotNull(storage.getTask("add-integer", "task-001"));
        Assertions.assertNull(storage.getTask("add-integer", "task-002"));
        Assertions.assertNotNull(storage.getTask("add-string", "task-003"));

        Assertions.assertEquals(3, storage.getTasks().size());
        Assertions.assertEquals(
                2,
                storage.getTasksForTemplate("add-integer").size());

        TaskInstance task000 =
                storage.getTaskInstance(storage.getTask(
                        "add-integer", "task-000"));
        Assertions.assertEquals("task-000", task000.id);

        TaskConfiguration task000Config =
                storage.getTaskConfiguration(storage.getTask(
                        "add-integer", "task-000"));
        Assertions.assertEquals(2, task000Config.configuration.size());

        TaskInstance task001 =
                storage.getTaskInstance(storage.getTask(
                        "add-integer", "task-001"));
        Assertions.assertEquals("task-001", task001.id);
    }

    @Test
    public void loadSingleFile() throws StorageException {
        File directory = TestResources.file("./task/add-integer/task-000");
        SingleTaskStorage storage = new SingleTaskStorage(directory);

        TaskInstance task000 = storage.getTaskInstance(storage.getTask());
        Assertions.assertEquals("task-000", task000.id);
    }

}
