package cz.skodape.taskrunner.storage.instance.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.skodape.taskrunner.storage.ModelException;

import java.sql.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;

public class TaskInstanceJacksonAdapter {

    public static JsonNode asJson(TaskInstance instance) {
        ObjectMapper mapper = new ObjectMapper();
        DateFormat dateFormat = createDateFormat();
        //
        ObjectNode root = mapper.createObjectNode();
        root.put("id", instance.id);
        root.put("created", formatInstant(dateFormat, instance.created));
        root.put("lastChange", formatInstant(dateFormat, instance.lastChange));
        root.put("status", instance.status.asString());
        root.put("lastFinishedStep", instance.lastFinishedStep);
        root.put("stepCount", instance.stepCount);
        root.put("template", instance.template);
        if (instance.pid != null) {
            root.put("pid", instance.pid);
        }
        return root;
    }

    private static String formatInstant(
            DateFormat dateFormat, Instant instant) {
        return dateFormat.format(Date.from(instant));
    }

    private static DateFormat createDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    }

    public static TaskInstance asInstance(JsonNode node) throws ModelException {
        DateFormat dateFormat = createDateFormat();
        //
        TaskInstance instance = new TaskInstance();
        instance.id = node.get("id").textValue();
        instance.created = parseInstant(dateFormat, node.get("created"));
        instance.lastChange = parseInstant(dateFormat, node.get("lastChange"));
        instance.status = TaskStatus.fromString(node.get("status").textValue());
        instance.lastFinishedStep = node.get("lastFinishedStep").intValue();
        instance.stepCount = node.get("stepCount").asInt();
        instance.template = node.get("template").textValue();
        if (node.get("pid") != null) {
            instance.pid = node.get("pid").longValue();
        }
        return instance;
    }

    private static Instant parseInstant(DateFormat dateFormat, JsonNode node)
            throws ModelException {
        if (node == null) {
            return null;
        }
        try {
            return dateFormat.parse(node.textValue()).toInstant();
        } catch (ParseException ex) {
            throw new ModelException("Can't parse date.", ex);
        }
    }

}
