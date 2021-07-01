package cz.skodape.taskrunner.http;

import cz.skodape.taskrunner.storage.instance.TaskReference;
import cz.skodape.taskrunner.storage.template.model.TaskTemplate;

public class CreateRedirectUrl {

    public static String createUrl(
            TaskTemplate template, TaskReference reference) {
        return template.postResponseRedirectUrl
                .replace("${_.id}", reference.getId());
    }

}
