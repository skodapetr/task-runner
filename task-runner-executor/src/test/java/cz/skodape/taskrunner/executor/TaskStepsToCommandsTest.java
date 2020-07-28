package cz.skodape.taskrunner.executor;

import cz.skodape.taskrunner.storage.instance.model.TaskConfiguration;
import cz.skodape.taskrunner.storage.instance.model.TaskInstance;
import cz.skodape.taskrunner.storage.instance.model.TaskStep;
import cz.skodape.taskrunner.storage.template.model.TaskTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TaskStepsToCommandsTest {

    @Test
    public void testPrepareCommand000() {
        TaskConfiguration configuration = new TaskConfiguration();
        configuration.configuration.put("left", "1");
        configuration.configuration.put("right", "2");

        TaskTemplate template = new TaskTemplate();
        template.steps.add(new TaskStep(
                null,
                "echo '$(${left} + ${left} + ${right})'"));
        template.steps.add(new TaskStep(
                null,
                "cp ${_.input} ${_.working}"));
        template.steps.add(new TaskStep(
                null,
                "touch ${_.public} ${_.id}"));

        File publicDir = new File("./public");
        File workingDir = new File("./working");
        File inputDir = new File("./input");

        TaskInstance instance = new TaskInstance();
        instance.id = "TID";

        TaskStepsToCommands taskStepsToCommands = new TaskStepsToCommands(
                configuration, instance, template,
                publicDir, workingDir, inputDir);

        List<String> expected = new ArrayList<>();
        expected.add("echo '$(1 + 1 + 2)'");
        expected.add(
                "cp " + inputDir.getAbsolutePath()
                        + " " + workingDir.getAbsolutePath());
        expected.add("touch " + publicDir.getAbsolutePath() + " TID");

        List<String> actual = taskStepsToCommands.asCommands();
        Assertions.assertEquals(expected.size(), actual.size());
        Assertions.assertEquals(expected.get(0), actual.get(0));
        Assertions.assertEquals(expected.get(1), actual.get(1));
        Assertions.assertEquals(expected.get(2), actual.get(2));
    }

}
