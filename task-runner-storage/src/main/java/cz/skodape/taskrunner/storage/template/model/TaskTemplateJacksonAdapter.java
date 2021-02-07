package cz.skodape.taskrunner.storage.template.model;

import com.fasterxml.jackson.databind.JsonNode;
import cz.skodape.taskrunner.storage.instance.model.TaskStep;

public class TaskTemplateJacksonAdapter {

    public static TaskTemplate asTaskTemplate(JsonNode node) {
        TaskTemplate specification = new TaskTemplate();
        specification.id = node.get("id").textValue();
        specification.urlPath = node.get("urlPath").textValue();
        if (node.has("allowInputFiles")) {
            specification.allowInputFiles =
                    node.get("allowInputFiles").booleanValue();
        }
        if (node.has("createOnGet")) {
            specification.createOnGet = node.get("createOnGet").booleanValue();
        }
        if (node.has("mergeErrOutToStdOut")) {
            specification.mergeErrOutToStdOut =
                    node.get("mergeErrOutToStdOut").booleanValue();
        }
        if (node.has("readOnly")) {
            specification.readOnly = node.get("readOnly").booleanValue();
        }
        if (node.has("steps")) {
            for (JsonNode stepNode : node.get("steps")) {
                specification.steps.add(asTaskStep(stepNode));
            }
        }
        if (node.has("taskGetIdentificationTransformation")) {
            String value = node.get("taskGetIdentificationTransformation")
                    .textValue();
            specification.taskGetIdentificationTransformation =
                    StringTransformation.fromString(value);
        }
        if (node.has("allowGzipPublicFiles")) {
            specification.allowGzipPublicFiles =
                    node.get("allowGzipPublicFiles").booleanValue();
        }
        return specification;
    }

    protected static TaskStep asTaskStep(JsonNode node) {
        TaskStep step = new TaskStep();
        step.name = node.get("name").textValue();
        step.command = node.get("command").textValue();
        return step;
    }

}
