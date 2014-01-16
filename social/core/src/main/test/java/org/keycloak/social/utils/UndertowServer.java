package org.keycloak.social.utils;

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;

import javax.servlet.ServletException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UndertowServer {

    private PathHandler root;
    private ServletContainer container;
    private Undertow server;
    private String hostname;
    private int port;

    public UndertowServer(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;

        root = new PathHandler();
        container = ServletContainer.Factory.newInstance();
    }

    public void start() {
        Undertow.Builder builder = Undertow.builder().addListener(port, hostname);
        server = builder.setHandler(root).build();
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    public void deploy(DeploymentInfo deploymentInfo) {
        DeploymentManager manager = container.addDeployment(deploymentInfo);
        manager.deploy();
        try {
            root.addPath(deploymentInfo.getContextPath(), manager.start());
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    public void undeploy(String deploymentName) {
        DeploymentManager deployment = container.getDeployment(deploymentName);
        try {
            deployment.stop();
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

}

