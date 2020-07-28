package cz.skodape.taskrunner.storage.instance.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TaskConfigurationJacksonAdapter {

    public static JsonNode asJson(TaskConfiguration configuration) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        for (var entry : configuration.configuration.entrySet()) {
            root.put(entry.getKey(), entry.getValue());
        }
        return root;
    }

    public static TaskConfiguration asConfiguration(JsonNode node) {
        TaskConfiguration configuration = new TaskConfiguration();
        var iterator = node.fields();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            configuration.configuration.put(
                    entry.getKey(),
                    entry.getValue().textValue());
        }
        return configuration;
    }

}
