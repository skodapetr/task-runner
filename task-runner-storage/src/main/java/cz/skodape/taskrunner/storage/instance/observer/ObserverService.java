package cz.skodape.taskrunner.storage.instance.observer;

import cz.skodape.taskrunner.storage.instance.storage.DirectoryTaskStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Observe state of the storage and notify listeners.
 */
public class ObserverService {

    public static final int CHECK_PENDING_COUNT = 6;

    public static final int POOL_TIMEOUT = 5;

    public static final int PENDING_CHECK_INTERVAL = 10;

    private static final Logger LOG =
            LoggerFactory.getLogger(ObserverService.class);

    private final DirectoryTaskStorage storage;

    private final List<StorageListener> listeners = new ArrayList<>();

    private final Map<WatchKey, String> watchKeyToTemplate = new HashMap<>();

    private Thread thread = null;

    public ObserverService(DirectoryTaskStorage storage) {
        this.storage = storage;
    }

    /**
     * All listeners should be registered before this service is started.
     */
    public void start() throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        File dataRoot = storage.getDataDirectory();
        for (String template : storage.getTemplates()) {
            Path path = new File(dataRoot, template).toPath();
            WatchKey key = path.register(
                    watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE);
            watchKeyToTemplate.put(key, template);
            LOG.info("Monitoring {}", path);
        }
        StorageObserver observerThread = new StorageObserver(
                storage, listeners, watchKeyToTemplate, watchService);
        thread = new Thread(observerThread);
        thread.setDaemon(true);
        thread.setName("storage-observer");
        thread.start();
        LOG.info("Storage observer is running ...");
    }

    public void stop() {
        LOG.info("Stopping storage observer ...");
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
            LOG.info("Waiting for observer thread to finish.");
        }
        watchKeyToTemplate.clear();
    }

    public void addListener(StorageListener listener) {
        listeners.add(listener);
    }

    public List<StorageListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    public Map<WatchKey, String> getWatchKeyToTemplate() {
        return Collections.unmodifiableMap(watchKeyToTemplate);
    }

}
