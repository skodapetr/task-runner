package cz.skodape.taskrunner.cli.action;

import cz.skodape.taskrunner.cli.AppConfiguration;
import cz.skodape.taskrunner.cli.service.ExecutorService;
import cz.skodape.taskrunner.cli.service.HttpService;
import cz.skodape.taskrunner.http.HttpServerException;
import cz.skodape.taskrunner.storage.instance.observer.ObserverService;
import cz.skodape.taskrunner.storage.instance.prune.PruneService;
import cz.skodape.taskrunner.storage.instance.storage.WritableTaskStorage;
import cz.skodape.taskrunner.storage.template.TaskTemplateStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StartServices {

    private static final Logger LOG =
            LoggerFactory.getLogger(StartServices.class);

    private final AppConfiguration configuration;

    private WritableTaskStorage taskStorage;

    private TaskTemplateStorage templateStorage;

    private ExecutorService executorService;

    private HttpService httpService;

    private ObserverService observerService;

    private PruneService pruneService;

    public StartServices(AppConfiguration configuration) {
        this.configuration = configuration;
    }

    public void execute() {
        initializeStorage();
        try {
            startExecutorService();
            startHttpService();
            startObserverService();
            startPruneService();
            waitForEternity();
        } catch (Exception ex) {
            LOG.error("Error running services.", ex);
        } finally {
            stopAll();
        }
    }

    private void initializeStorage() {
        templateStorage = new TaskTemplateStorage();
        templateStorage.load(configuration.templateDirectory);
        taskStorage = new WritableTaskStorage(
                configuration.taskDirectory, configuration.workingDirectory);
        if (configuration.restartRunningTasks) {
            (new PrepareStorage(taskStorage,templateStorage)).prepare();
        }
        observerService = new ObserverService(taskStorage);
        pruneService = new PruneService(taskStorage, templateStorage);
    }

    private void startExecutorService() {
        if (configuration.executorThreads == null
                || configuration.executorThreads == 0) {
            return;
        }
        LOG.info("Starting executor ...");
        executorService = new ExecutorService(
                taskStorage,
                templateStorage,
                observerService,
                configuration.executorThreads);
        executorService.start();
        LOG.info("Starting executor ... done");
    }

    private void startHttpService() throws HttpServerException {
        if (configuration.httpPort == null) {
            return;
        }
        LOG.info("Starting HTTP server ...");
        httpService = new HttpService(
                taskStorage, templateStorage, configuration.httpPort);
        httpService.start();
    }

    private void startObserverService() throws IOException {
        observerService.start();
    }

    private void startPruneService() {
        pruneService.start();
    }

    /**
     * The main thread just wait as it does nothing.
     */
    private void waitForEternity() {
        LOG.info("All requested services are running");
        // TODO Check for SIGTERM
        while (true) {
            try {
                Thread.sleep(60 * 1000);
            } catch (InterruptedException ex) {
                break;
            }
        }
        LOG.info("Time to end this.");
    }

    private void stopAll() {
        if (executorService != null) {
            executorService.stop();
        }
        if (httpService != null) {
            httpService.stop();
        }
        if (observerService != null) {
            observerService.stop();
        }
        if (pruneService != null) {
            pruneService.stop();
        }
    }

}
