package cz.skodape.taskrunner.storage.instance;

public class TaskReference {

    private final String template;

    private final String id;

    protected TaskReference(String template, String id) {
        this.template = template;
        this.id = id;
    }

    public String getTemplate() {
        return template;
    }

    public String getId() {
        return id;
    }

    public static TaskReference create(String template, String name) {
        return new TaskReference(template, name);
    }

    @Override
    public String toString() {
        return template + ":" + id;
    }

}
