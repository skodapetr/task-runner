package cz.skodape.taskrunner.cli.service;

import cz.skodape.taskrunner.http.JettyHttpServer;
import cz.skodape.taskrunner.http.HttpServerException;
import cz.skodape.taskrunner.storage.instance.WritableTaskStorage;
import cz.skodape.taskrunner.storage.template.TaskTemplateStorage;

public class HttpService {

    private final WritableTaskStorage taskStorage;

    private final TaskTemplateStorage templateStorage;

    private final int port;

    private JettyHttpServer httpServer;

    public HttpService(
            WritableTaskStorage taskStorage,
            TaskTemplateStorage templateStorage,
            int port) {
        this.taskStorage = taskStorage;
        this.templateStorage = templateStorage;
        this.port = port;
    }

    public void start() throws HttpServerException {
        httpServer = new JettyHttpServer(taskStorage, templateStorage, port);
        httpServer.start();
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop();
        }
    }

}
