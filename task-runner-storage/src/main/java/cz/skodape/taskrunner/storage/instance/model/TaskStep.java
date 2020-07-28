package cz.skodape.taskrunner.storage.instance.model;

import cz.skodape.taskrunner.storage.SuppressFBWarnings;

@SuppressFBWarnings(value = {"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
public class TaskStep {

    public String name;

    public String command;

    public TaskStep() {
    }

    public TaskStep(String name, String command) {
        this.name = name;
        this.command = command;
    }

}
