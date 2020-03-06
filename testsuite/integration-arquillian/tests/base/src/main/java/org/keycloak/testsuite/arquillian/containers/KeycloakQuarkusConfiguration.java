package org.keycloak.testsuite.arquillian.containers;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author mhajas
 */
public class KeycloakQuarkusConfiguration implements ContainerConfiguration {

    private Path providersPath = Paths.get(System.getProperty("auth.server.home"));
    private int startupTimeoutInSeconds = 60;

    @Override
    public void validate() throws ConfigurationException {

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
