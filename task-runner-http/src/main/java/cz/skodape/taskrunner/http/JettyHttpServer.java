package cz.skodape.taskrunner.http;

import cz.skodape.taskrunner.storage.instance.WritableTaskStorage;
import cz.skodape.taskrunner.storage.template.TaskTemplateStorage;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyHttpServer {

    private static final Logger LOG =
            LoggerFactory.getLogger(JettyHttpServer.class);

    private final TaskRestApi restApi;

    private final int port;

    private Server server;

    public JettyHttpServer(
            WritableTaskStorage taskStorage,
            TaskTemplateStorage templateStorage,
            int port) {
        this.restApi = new TaskRestApi(taskStorage, templateStorage);
        this.port = port;
    }

    public void start() throws HttpServerException {
        server = new Server(port);

        // TODO Add default 404 response

        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setContextPath("/");
        contextHandler.addServlet(getRestApiServlet(), "/api/v1/*");

        server.setHandler(new ContextHandlerCollection(contextHandler));

        try {
            server.start();
        } catch (Exception ex) {
            throw new HttpServerException("Can't start server.", ex);
        }
        LOG.info("HTTP server listening at: {}", port);
    }

    private ServletHolder getRestApiServlet() {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.registerInstances(restApi);
        resourceConfig.register(MultiPartFeature.class);
        //
        ServletHolder holder = new ServletHolder();
        holder.setServlet(new ServletContainer(resourceConfig));
        //
        return holder;
    }

    public void stop() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception ex) {
                LOG.error("Can't stop server.", ex);
            }
        }
    }

}
