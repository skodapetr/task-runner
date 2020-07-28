package cz.skodape.taskrunner.executor;

import cz.skodape.taskrunner.storage.instance.model.TaskConfiguration;
import cz.skodape.taskrunner.storage.instance.model.TaskInstance;
import cz.skodape.taskrunner.storage.instance.model.TaskStep;
import cz.skodape.taskrunner.storage.template.model.TaskTemplate;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskStepsToCommands {

    private final TaskConfiguration configuration;

    private final TaskInstance taskInstance;

    private final TaskTemplate taskTemplate;

    private final File publicDirectory;

    private final File workingDirectory;

    private final File inputDirectory;

    public TaskStepsToCommands(
            TaskConfiguration configuration,
            TaskInstance taskInstance,
            TaskTemplate taskTemplate,
            File publicDirectory,
            File workingDirectory,
            File inputDirectory) {
        this.configuration = configuration;
        this.taskInstance = taskInstance;
        this.taskTemplate = taskTemplate;
        this.publicDirectory = publicDirectory;
        this.workingDirectory = workingDirectory;
        this.inputDirectory = inputDirectory;
    }

    public List<String> asCommands() {
        List<String> result = new ArrayList<>();
        Map<String, String> substitutions = prepareSubstitutionMap();
        for (TaskStep step : taskTemplate.steps) {
            String command = step.command;
            for (var entry : substitutions.entrySet()) {
                command = command.replace(entry.getKey(), entry.getValue());
            }
            result.add(command);
        }
        return result;
    }

    private Map<String, String> prepareSubstitutionMap() {
        Map<String, String> result = new HashMap<>();
        result.put("${_.input}", inputDirectory.getAbsolutePath());
        result.put("${_.public}", publicDirectory.getAbsolutePath());
        result.put("${_.working}", workingDirectory.getAbsolutePath());
        result.put("${_.id}", taskInstance.id);
        for (var entry : configuration.configuration.entrySet()) {
            result.put("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

}
