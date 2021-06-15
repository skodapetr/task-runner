package cz.skodape.taskrunner.storage.instance.observer;

import cz.skodape.taskrunner.storage.instance.TaskReference;
import cz.skodape.taskrunner.storage.instance.storage.DirectoryTaskStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class StorageObserver implements Runnable {

    private static final Logger LOG =
            LoggerFactory.getLogger(StorageObserver.class);

    private final DirectoryTaskStorage storage;

    private final List<StorageListener> listeners;

    private final Map<WatchKey, String> watchKeyToTemplate;

    private final WatchService watchService;

    /**
     * When someone make a copy of a directory, we capture an empty
     * directory. As a solution we store it as a pending
     * and check a few times fot the status file.
     */
    private final List<PendingDirectory> pendingDirectories = new ArrayList<>();

    private Instant nextPendingCheck = Instant.now();

    public StorageObserver(
            DirectoryTaskStorage storage,
            List<StorageListener> listeners,
            Map<WatchKey, String> watchKeyToTemplate,
            WatchService watchService) {
        this.storage = storage;
        this.listeners = listeners;
        this.watchKeyToTemplate = watchKeyToTemplate;
        this.watchService = watchService;
    }

    @Override
    public void run() {
        WatchKey key;
        while (true) {
            try {
                key = watchService.poll(
                        ObserverService.POOL_TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                LOG.info("Ending watch.");
                break;
            }
            checkPending();
            if (key == null) {
                continue;
            }
            String template = watchKeyToTemplate.get(key);
            for (var event : key.pollEvents()) {
                @SuppressWarnings("unchecked")
                Path path = ((WatchEvent<Path>)event).context();
                String id = path.getFileName().toString();
                var kind = event.kind();
                if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {
                    onNewPath(template, id);
                }
                if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
                    onDeletePath(template, id);
                }
            }
            key.reset();
        }
    }

    private void checkPending() {
        if (pendingDirectories.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        if (now.isBefore(nextPendingCheck)) {
            return;
        }
        nextPendingCheck = now.plus(
                ObserverService.PENDING_CHECK_INTERVAL, ChronoUnit.SECONDS);
        for (PendingDirectory pending : pendingDirectories) {
            TaskReference reference = storage.getTask(
                    pending.template, pending.id);
            if (reference == null) {
                pending.retryCounter -= 1;
                continue;
            }
            for (StorageListener listener : listeners) {
                listener.onNewTask(reference);
            }
            pending.retryCounter = -1;
        }
        // Remove all consumed.
        pendingDirectories.removeIf(pending -> pending.retryCounter < 1);
    }

    public void onNewPath(String template, String id) {
        TaskReference reference = storage.getTask(template, id);
        if (reference == null) {
            pendingDirectories.add(new PendingDirectory(template, id));
            return;
        }
        for (StorageListener listener : listeners) {
            listener.onNewTask(reference);
        }
    }

    public void onDeletePath(String template, String id) {
        TaskReference reference = storage.getTask(template, id);
        for (StorageListener listener : listeners) {
            listener.onDeleteTask(reference);
        }
    }

}
