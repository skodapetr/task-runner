package cz.skodape.taskrunner.storage.instance.storage;

import cz.skodape.taskrunner.storage.DirectoryUtils;
import cz.skodape.taskrunner.storage.instance.TaskBuilder;
import cz.skodape.taskrunner.storage.instance.TaskReference;
import cz.skodape.taskrunner.storage.instance.observer.ObserverService;
import cz.skodape.taskrunner.storage.template.model.TaskTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class TaskObserverTest {

    @Test
    public void observeNewTask() throws Exception {
        List<TaskReference> newTasks = new ArrayList<>();
        File directory = Files.createTempDirectory("task-runner-").toFile();
        WritableTaskStorage storage = new WritableTaskStorage(
                new File(directory, "data"),
                new File(directory, "working"));
        (new File(directory, "data" + File.separator + "add-string")).mkdirs();
        ObserverService observer = new ObserverService(storage, newTasks::add);
        observer.start();
        TaskTemplate specification = new TaskTemplate();
        specification.id = "add-string";
        var instance = storage.reserveNewTask(specification.id);
        TaskBuilder builder = new TaskBuilder(storage, instance, specification);
        builder.build();
        // Wait so the new file event is captured.
        // TODO We should replace this with more deterministic approach.
        Thread.sleep(100);
        int newTaskCount = newTasks.size();
        observer.stop();
        DirectoryUtils.delete(directory);
        Assertions.assertEquals(1, newTaskCount);
    }

}
