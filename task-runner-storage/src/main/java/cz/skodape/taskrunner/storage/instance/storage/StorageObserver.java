package cz.skodape.taskrunner.storage.instance.storage;

import cz.skodape.taskrunner.storage.instance.TaskReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class StorageObserver {

    private static final int CHECK_PENDING_COUNT = 6;

    private static final int POOL_TIMEOUT = 5;

    private static final int PENDING_CHECK_INTERVAL = 10;

    private static class PendingDirectory {

        private final String template;

        private final String id;

        private int retryCounter = CHECK_PENDING_COUNT;

        public PendingDirectory(String template, String id) {
            this.template = template;
            this.id = id;
        }

    }

    private class Observer implements Runnable {

        private final WatchService watchService;

        /**
         * When someone make a copy of a directory, we capture an empty
         * directory. As a solution we store it as a pending
         * and check a few times fot the status file.
         */
        private List<PendingDirectory> pendingDirectories = new ArrayList<>();

        private Instant nextPendingCheck = Instant.now();

        public Observer(WatchService watchService) {
            this.watchService = watchService;
        }

        @Override
        public void run() {
            WatchKey key;
            while (true) {
                try {
                    key = watchService.poll(POOL_TIMEOUT, TimeUnit.SECONDS);
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
                    Path path = ((WatchEvent<Path>) event).context();
                    String id = path.getFileName().toString();
                    onNewFile(template, id);
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
                    PENDING_CHECK_INTERVAL, ChronoUnit.SECONDS);
            for (PendingDirectory pending : pendingDirectories) {
                TaskReference reference =
                        storage.getTask(pending.template, pending.id);
                if (reference == null) {
                    pending.retryCounter -= 1;
                    continue;
                }
                listener.onNewTask(reference);
                pending.retryCounter = -1;
            }
            // Remove all consumed.
            pendingDirectories.removeIf(pending -> pending.retryCounter < 1);
        }

        public void onNewFile(String template, String id) {
            TaskReference reference = storage.getTask(template, id);
            if (reference == null) {
                pendingDirectories.add(new PendingDirectory(template, id));
                return;
            }
            listener.onNewTask(reference);
        }

    }

    public interface Listener {

        void onNewTask(TaskReference reference);

    }

    private static final Logger LOG =
            LoggerFactory.getLogger(StorageObserver.class);

    private final DirectoryTaskStorage storage;

    private final Listener listener;

    private Thread thread = null;

    private final Map<WatchKey, String> watchKeyToTemplate = new HashMap<>();

    public StorageObserver(DirectoryTaskStorage storage, Listener listener) {
        this.storage = storage;
        this.listener = listener;
    }

    public void start() throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        File dataRoot = storage.getDataDirectory();
        for (String template : storage.getTemplates()) {
            Path path = new File(dataRoot, template).toPath();
            WatchKey key = path.register(
                    watchService, StandardWatchEventKinds.ENTRY_CREATE);
            watchKeyToTemplate.put(key, template);
            LOG.info("Monitoring {}", path);
        }
        Observer observerThread = new Observer(watchService);
        thread = new Thread(observerThread);
        thread.setDaemon(true);
        thread.setName("storage-observer");
        thread.start();
        LOG.info("Storage observer is running ...");
    }

    public void stop() {
        LOG.info("Stopping storage observer.");
        while (true) {
            thread.interrupt();
            try {
                thread.join(1000);
            } catch (InterruptedException ex) {
                LOG.info("Interrupted while waiting on observer thread.");
                break;
            }
            if (!thread.isAlive()) {
                break;
            }
            LOG.info("Waiting for observer thread fo finish.");
        }
        watchKeyToTemplate.clear();
    }

}
