package cz.skodape.taskrunner.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import cz.skodape.taskrunner.storage.instance.model.TaskConfiguration;
import cz.skodape.taskrunner.storage.instance.model.TaskConfigurationJacksonAdapter;
import cz.skodape.taskrunner.storage.instance.model.TaskInstance;
import cz.skodape.taskrunner.storage.instance.model.TaskInstanceJacksonAdapter;
import cz.skodape.taskrunner.storage.instance.model.TaskStatus;
import cz.skodape.taskrunner.storage.template.model.TaskTemplate;
import cz.skodape.taskrunner.storage.template.model.TaskTemplateJacksonAdapter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class JsonAdapterTest {

    private final DateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private final ObjectMapper mapper = new ObjectMapper();

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Test
    public void loadTask() throws IOException, ModelException, ParseException {
        var node = mapper.readTree(TestResources.file(
                "./task/add-integer/task-000/status.json"));
        TaskInstance actual = TaskInstanceJacksonAdapter.asInstance(node);
        //
        Assertions.assertEquals("task-000", actual.id);
        Assertions.assertEquals(
                dateFormat.parse("2020-03-01T23:59:11").toInstant(),
                actual.created);
        Assertions.assertEquals(
                dateFormat.parse("2020-03-01T23:59:11").toInstant(),
                actual.lastChange);
        Assertions.assertEquals(TaskStatus.QUEUED, actual.status);
        Assertions.assertEquals(0, actual.lastFinishedStep);
        Assertions.assertEquals(3, actual.stepCount);
        Assertions.assertEquals("add-integer", actual.template);
        Assertions.assertEquals(1234, actual.pid);
    }

    @Test
    public void loadTaskConfiguration() throws IOException {
        var node = mapper.readTree(TestResources.file(
                "./task/add-integer/task-000/configuration.json"));
        TaskConfiguration actual =
                TaskConfigurationJacksonAdapter.asConfiguration(node);
        //
        Assertions.assertEquals(2, actual.configuration.size());
        Assertions.assertEquals("1", actual.configuration.get("left"));
        Assertions.assertEquals("2", actual.configuration.get("right"));
    }

    @Test
    public void loadTaskSpecificationAddInteger() throws IOException {
        var node = mapper.readTree(TestResources.file(
                "./specification/add-integer.json"));
        TaskTemplate actual = TaskTemplateJacksonAdapter.asTaskTemplate(node);
        //
        Assertions.assertEquals("add-integer", actual.id);
        Assertions.assertFalse(actual.allowInputFiles);
        Assertions.assertEquals(2, actual.steps.size());
        Assertions.assertEquals("echo", actual.steps.get(0).name);
        Assertions.assertEquals("echo ${left}", actual.steps.get(0).command);
        Assertions.assertEquals("echo", actual.steps.get(1).name);
        Assertions.assertEquals("echo ${right}", actual.steps.get(1).command);
    }

    @Test
    public void loadTaskSpecificationTimeToLive() throws IOException {
        var node = yamlMapper.readTree(TestResources.file(
                "./specification/add-string-with-ttl.yaml")).get("template");
        TaskTemplate actual = TaskTemplateJacksonAdapter.asTaskTemplate(node);
        //
        Assertions.assertEquals("add-string", actual.id);
        Assertions.assertEquals(5 * 60, actual.timeToLive);
    }

    @Test
    public void loadTaskSpecificationPostKey() throws IOException {
        var node = yamlMapper.readTree(TestResources.file(
                "./specification/post-content.yaml")).get("template");
        TaskTemplate actual = TaskTemplateJacksonAdapter.asTaskTemplate(node);
        //
        Assertions.assertEquals("post-content", actual.id);
        Assertions.assertTrue(actual.keyFromPost);
    }

    @Test
    public void taskConfigurationJsonAndBack() {
        TaskConfiguration expected = new TaskConfiguration();
        expected.configuration.put("one", "1");
        expected.configuration.put("two", "2");
        TaskConfiguration actual =
                TaskConfigurationJacksonAdapter.asConfiguration(
                        TaskConfigurationJacksonAdapter.asJson(expected));
        Assertions.assertEquals("1", actual.configuration.get("one"));
        Assertions.assertEquals("2", actual.configuration.get("two"));
    }

    @Test
    public void taskStatusJsonAndBack() throws ModelException {
        TaskInstance expected = new TaskInstance();
        expected.id = "000";
        expected.created = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        expected.lastChange = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        expected.status = TaskStatus.RUNNING;
        expected.lastFinishedStep = 1;
        expected.template = "task";
        expected.pid = 123L;
        TaskInstance actual =
                TaskInstanceJacksonAdapter.asInstance(
                        TaskInstanceJacksonAdapter.asJson(expected));
        Assertions.assertEquals(expected.id, actual.id);
        Assertions.assertEquals(expected.created, actual.created);
        Assertions.assertEquals(expected.lastChange, actual.lastChange);
        Assertions.assertEquals(expected.status, actual.status);
        Assertions.assertEquals(
                expected.lastFinishedStep, actual.lastFinishedStep);
        Assertions.assertEquals(expected.template, actual.template);
        Assertions.assertEquals(expected.pid, actual.pid);
    }

}
