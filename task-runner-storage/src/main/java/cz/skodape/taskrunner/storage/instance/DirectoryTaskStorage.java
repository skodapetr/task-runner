package cz.skodape.taskrunner.storage.instance;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DirectoryTaskStorage extends TaskStorage {

    protected final File dataDirectory;

    public DirectoryTaskStorage(File directory) {
        this.dataDirectory = directory;
        getDataDirectory().mkdirs();
    }

    public TaskReference getTask(String template, String name) {
        TaskReference reference = TaskReference.create(template, name);
        if (getTaskInstanceFile(reference).exists()) {
            return reference;
        } else {
            return null;
        }
    }

    protected File getTaskDirectory(TaskReference reference) {
        return getTaskDirectory(reference.getTemplate(), reference.getId());
    }

    protected File getTaskDirectory(String template, String id) {
        return new File(
                getDataDirectory(),
                template + File.separator + id);
    }

    File getDataDirectory() {
        return dataDirectory;
    }

    public List<TaskReference> getTasks() {
        List<TaskReference> result = new ArrayList<>();
        for (String templateId : getTemplates()) {
            result.addAll(getTasksForTemplate(templateId));
        }
        return result;
    }

    List<String> getTemplates() {
        File[] directories = getDataDirectory().listFiles();
        if (directories == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(directories)
                .stream()
                .map(file -> file.getName())
                .collect(Collectors.toList());
    }

    public List<TaskReference> getTasksForTemplate(String templateId) {
        if (templateId == null) {
            return Collections.emptyList();
        }
        File[] content = new File(getDataDirectory(), templateId).listFiles();
        if (content == null) {
            return Collections.emptyList();
        }
        return Stream.of(content)
                .map(file -> getTask(templateId, file.getName()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

}
