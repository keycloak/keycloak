package org.keycloak.testsuite.arquillian.containers;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;
import org.jboss.logging.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author mhajas
 */
public class KeycloakQuarkusConfiguration implements ContainerConfiguration {

    protected static final Logger log = Logger.getLogger(KeycloakQuarkusConfiguration.class);

    private int bindHttpPortOffset = 100;
    private int bindHttpPort = 8080;
    private int bindHttpsPortOffset = 0;
    private int bindHttpsPort = Integer.valueOf(System.getProperty("auth.server.https.port", "8543"));
    private Path providersPath = Paths.get(System.getProperty("auth.server.home"));
    private int startupTimeoutInSeconds = 60;

    @Override
    public void validate() throws ConfigurationException {
        int basePort = getBindHttpPort();
        int newPort = basePort + bindHttpPortOffset;
        setBindHttpPort(newPort);

        int baseHttpsPort = getBindHttpsPort();
        int newHttpsPort = baseHttpsPort + bindHttpsPortOffset;
        setBindHttpsPort(newHttpsPort);

        log.info("Keycloak will listen for http on port: " + newPort + " and for https on port: " + newHttpsPort);
    }

    public int getBindHttpPortOffset() {
        return bindHttpPortOffset;
    }

    public void setBindHttpPortOffset(int bindHttpPortOffset) {
        this.bindHttpPortOffset = bindHttpPortOffset;
    }

    public int getBindHttpsPortOffset() {
        return bindHttpsPortOffset;
    }

    public void setBindHttpsPortOffset(int bindHttpsPortOffset) {
        this.bindHttpsPortOffset = bindHttpsPortOffset;
    }

    public int getBindHttpsPort() {
        return this.bindHttpsPort;
    }

    public void setBindHttpsPort(int bindHttpsPort) {
        this.bindHttpsPort = bindHttpsPort;
    }

    public int getBindHttpPort() {
        return bindHttpPort;
    }

    public void setBindHttpPort(int bindHttpPort) {
        this.bindHttpPort = bindHttpPort;
    }

    public Path getProvidersPath() {
        return providersPath;
    }

    public void setProvidersPath(Path providersPath) {
        this.providersPath = providersPath;
    }

    public int getStartupTimeoutInSeconds() {
        return startupTimeoutInSeconds;
    }

    public void setStartupTimeoutInSeconds(int startupTimeoutInSeconds) {
        this.startupTimeoutInSeconds = startupTimeoutInSeconds;
    }
}
