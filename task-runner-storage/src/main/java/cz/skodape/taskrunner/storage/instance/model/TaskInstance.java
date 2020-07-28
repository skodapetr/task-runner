package cz.skodape.taskrunner.storage.instance.model;

import cz.skodape.taskrunner.storage.SuppressFBWarnings;

import java.time.Instant;

@SuppressFBWarnings(value = {"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
public class TaskInstance {

    public String id;

    public Instant created;

    public Instant lastChange;

    public TaskStatus status;

    public int lastFinishedStep;

    public int stepCount;

    public String template;

    public Long pid;

}
