package cz.skodape.taskrunner.cli.action;

import cz.skodape.taskrunner.cli.AppConfiguration;
import cz.skodape.taskrunner.cli.service.ExecutorService;
import cz.skodape.taskrunner.cli.service.HttpService;
import cz.skodape.taskrunner.http.HttpServerException;
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

    public StartServices(AppConfiguration configuration) {
        this.configuration = configuration;
    }

    public void execute() {
        initializeStorage();
        try {
            startExecutorService();
            startHttpService();
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
    }

    private void startExecutorService() throws IOException {
        if (configuration.executorThreads == null
                || configuration.executorThreads == 0) {
            return;
        }
        LOG.info("Starting executor ...");
        executorService = new ExecutorService(
                taskStorage,
                templateStorage,
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

    /**
     * The main thread just wait as it does nothing.
     */
    private void waitForEternity() {
        LOG.info("Running ...");
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
    }

}
