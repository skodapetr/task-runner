package cz.skodape.taskrunner.storage.instance;

import cz.skodape.taskrunner.storage.TestResources;
import cz.skodape.taskrunner.storage.DirectoryUtils;
import cz.skodape.taskrunner.storage.StorageException;
import cz.skodape.taskrunner.storage.instance.model.TaskInstance;
import cz.skodape.taskrunner.storage.template.model.TaskTemplate;
import cz.skodape.taskrunner.storage.instance.model.TaskStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class WritableTaskStorageTest {

    @Test
    public void createStorageAndTask() throws IOException, StorageException {
        File directory = Files.createTempDirectory("task-runner-").toFile();
        WritableTaskStorage storage = new WritableTaskStorage(
                new File(directory, "data"),
                new File(directory, "working"));

        Assertions.assertEquals(0L, storage.getTasks().size());

        TaskTemplate specification = new TaskTemplate();
        specification.id = "template";

        TaskBuilder builder = TaskBuilder.create(storage, specification);
        String fileA = "content aaa";
        builder.addFile(
                "file-A",
                new ByteArrayInputStream(
                        fileA.getBytes(StandardCharsets.UTF_8)));

        String fileB = "content bbb";
        builder.addFile(
                "file-B",
                new ByteArrayInputStream(
                        fileB.getBytes(StandardCharsets.UTF_8)));

        Map<String, String> configuration = new HashMap<>();
        configuration.put("A", "a");
        configuration.put("B", "b");
        builder.addConfiguration(configuration);

        Assertions.assertEquals(0L, storage.getTasks().size());

        TaskReference task = builder.build();
        Instant after = Instant.now();

        Assertions.assertEquals(1L, storage.getTasks().size());

        Assertions.assertEquals(
                configuration,
                storage.getTaskConfiguration(task).configuration);

        Assertions.assertEquals(
                fileA,
                TestResources.asString(new File(
                        storage.getTaskInputDirectory(task),
                        "file-A")));

        Assertions.assertEquals(
                fileB,
                TestResources.asString(new File(
                        storage.getTaskInputDirectory(task),
                        "file-B")));

        TaskInstance instance = storage.getTaskInstance(task);
        Assertions.assertFalse(instance.created.isAfter(after));
        Assertions.assertFalse(instance.lastChange.isAfter(after));

        Assertions.assertEquals(TaskStatus.QUEUED, instance.status);

        Assertions.assertNotNull(instance.id);
        Assertions.assertEquals(specification.id, instance.template);

        DirectoryUtils.delete(directory);
    }

}
