package cz.skodape.taskrunner.storage.instance.observer;

class PendingDirectory {

    public final String template;

    public final String id;

    public int retryCounter = ObserverService.CHECK_PENDING_COUNT;

    public PendingDirectory(String template, String id) {
        this.template = template;
        this.id = id;
    }

}
