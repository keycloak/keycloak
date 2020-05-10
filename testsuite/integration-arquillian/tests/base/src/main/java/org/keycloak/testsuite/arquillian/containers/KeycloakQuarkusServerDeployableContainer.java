package org.keycloak.testsuite.arquillian.containers;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.SuiteContext;

/**
 * @author mhajas
 */
public class KeycloakQuarkusServerDeployableContainer implements DeployableContainer<KeycloakQuarkusConfiguration> {

    protected static final Logger log = Logger.getLogger(KeycloakQuarkusServerDeployableContainer.class);
    
    private KeycloakQuarkusConfiguration configuration;
    private Process container;

    @Inject
    private Instance<SuiteContext> suiteContext;

    @Override
    public Class<KeycloakQuarkusConfiguration> getConfigurationClass() {
        return KeycloakQuarkusConfiguration.class;
    }

    @Override
    public void setup(KeycloakQuarkusConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start() throws LifecycleException {
        try {
            container = startContainer();
            waitForReadiness();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() throws LifecycleException {
        container.destroy();
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return null;
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        return null;
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {

    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {

    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {

    }

    private Process startContainer() throws IOException {
        if (AuthServerTestEnricher.AUTH_SERVER_SSL_REQUIRED) {
            throw new IllegalStateException("Quarkus server does not yet support SSL");
        }

        ProcessBuilder pb = new ProcessBuilder(getProcessCommands());
        File wrkDir = configuration.getProvidersPath().resolve("bin").toAbsolutePath().toFile();
        ProcessBuilder builder = pb.directory(wrkDir).inheritIO();

        builder.environment().put("KEYCLOAK_ADMIN", "admin");
        builder.environment().put("KEYCLOAK_ADMIN_PASSWORD", "admin");

        return builder.start();
    }

    private String[] getProcessCommands() {
        List<String> commands = new ArrayList<>();

        commands.add("./kc.sh");

        if (Boolean.valueOf(System.getProperty("auth.server.debug", "false"))) {
            commands.add("--debug");
            commands.add(System.getProperty("auth.server.debug.port", "5005"));
        }

        commands.add("-Dquarkus.http.port=" + suiteContext.get().getAuthServerInfo().getContextRoot().getPort());

        return commands.toArray(new String[commands.size()]);
    }

    private void waitForReadiness() throws MalformedURLException, LifecycleException {
        SuiteContext suiteContext = this.suiteContext.get();
        //TODO: not sure if the best endpoint but it makes sure that everything is properly initialized. Once we have
        // support for MP Health this should change
        URL contextRoot = new URL(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/");
        HttpURLConnection connection;

        long startTime = System.currentTimeMillis();

        while (true) {
            if (System.currentTimeMillis() - startTime > getStartTimeout()) {
                stop();
                throw new IllegalStateException("Timeout [" + getStartTimeout() + "] while waiting for Quarkus server");
            }

            try {
                connection = (HttpURLConnection) contextRoot.openConnection();

                connection.setReadTimeout((int) getStartTimeout());
                connection.setConnectTimeout((int) getStartTimeout());
                connection.connect();

                if (connection.getResponseCode() == 200) {
                    break;
                }

                connection.disconnect();
            } catch (Exception ignore) {
            }
        }
        
        log.infof("Keycloak is ready at %s", this.suiteContext.get().getAuthServerInfo().getContextRoot());
    }

    private long getStartTimeout() {
        return TimeUnit.SECONDS.toMillis(configuration.getStartupTimeoutInSeconds());
    }
}
