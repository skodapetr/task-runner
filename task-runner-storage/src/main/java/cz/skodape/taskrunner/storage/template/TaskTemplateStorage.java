package cz.skodape.taskrunner.storage.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import cz.skodape.taskrunner.storage.template.model.TaskTemplate;
import cz.skodape.taskrunner.storage.template.model.TaskTemplateJacksonAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TaskTemplateStorage {

    private static final Logger LOG =
            LoggerFactory.getLogger(TaskTemplateStorage.class);

    private Map<String, TaskTemplate> templatesById = new HashMap<>();

    private Map<String, TaskTemplate> templatesByPath = new HashMap<>();

    public void load(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            LOG.info("No specifications found.");
            return;
        }
        for (File file : files) {
            loadTemplate(file);
        }
        LOG.info("Templates loaded: {}", templatesById.size());
    }

    private void loadTemplate(File file) {
        try {
            JsonNode node = readFile(file);
            if (node == null) {
                // Ignore files we can not load.
                return;
            }
            TaskTemplate template =
                    TaskTemplateJacksonAdapter.asTaskTemplate(node);
            templatesById.put(template.id, template);
            templatesByPath.put(template.urlPath, template);
        } catch (NullPointerException | IOException ex) {
            LOG.warn("Can't read: {}", file, ex);
        }
    }

    private static JsonNode readFile(File file) throws IOException {
        ObjectMapper objectMapper;
        if (isYaml(file.getName())) {
            objectMapper = new ObjectMapper(new YAMLFactory());
        } else if (isJson(file.getName())) {
            objectMapper = new ObjectMapper();
        } else {
            return null;
        }
        JsonNode node = objectMapper.readTree(file);
        if (isYaml(file.getName())) {
            node = node.get("template");
        }
        return node;
    }

    private static boolean isYaml(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".yaml") || lowerName.endsWith(".yml");
    }

    private static boolean isJson(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".json");
    }

    public TaskTemplate getTemplate(String id) {
        return templatesById.get(id);
    }

    public TaskTemplate getTemplateByPath(String path) {
        return templatesByPath.get(path);
    }

    public Set<String> getTemplateIds() {
        return templatesById.keySet();
    }

}
