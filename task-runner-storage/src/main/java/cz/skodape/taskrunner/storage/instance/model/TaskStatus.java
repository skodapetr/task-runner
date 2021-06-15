package cz.skodape.taskrunner.storage.instance.model;

public enum TaskStatus {
    QUEUED("queued"),
    RUNNING("running"),
    FAILED("failed"),
    SUCCESSFUL("successful");

    private String name;

    TaskStatus(String name) {
        this.name = name;
    }

    public String asString() {
        return name;
    }

    public static TaskStatus fromString(String name) {
        for (TaskStatus status : TaskStatus.values()) {
            if (status.name.equals(name)) {
                return status;
            }
        }
        return null;
    }

    public static boolean isFinished(TaskStatus status) {
        return status == FAILED || status == SUCCESSFUL;
    }

}
