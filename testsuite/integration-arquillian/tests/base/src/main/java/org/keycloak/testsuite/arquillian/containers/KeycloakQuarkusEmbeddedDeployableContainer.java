package org.keycloak.testsuite.arquillian.containers;

import java.util.List;

import org.keycloak.Keycloak;
import org.keycloak.common.Version;

import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.logging.Logger;

/**
 * @author mhajas
 */
public class KeycloakQuarkusEmbeddedDeployableContainer extends AbstractQuarkusDeployableContainer {

    private static final Logger log = Logger.getLogger(KeycloakQuarkusEmbeddedDeployableContainer.class);
    
    private static final String KEYCLOAK_VERSION = Version.VERSION;

    private Keycloak keycloak;

    @Override
    public void start() throws LifecycleException {
        try {
            List<String> args = getArgs();
            log.debugf("Quarkus process arguments: %s", args);
            keycloak = configure().start(args);
            waitForReadiness();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() throws LifecycleException {
        if (keycloak != null) {
            try {
                keycloak.stop();
            } catch (Exception e) {
                throw new RuntimeException("Failed to stop the server", e);
            } finally {
                keycloak = null;
            }
        }
    }

    private Keycloak.Builder configure() {
        return Keycloak.builder()
                .setHomeDir(configuration.getProvidersPath())
                .setVersion(KEYCLOAK_VERSION)
                .addDependency("org.keycloak.testsuite", "integration-arquillian-testsuite-providers", KEYCLOAK_VERSION)
                .addDependency("org.keycloak.testsuite", "integration-arquillian-testsuite-providers-deployment", KEYCLOAK_VERSION)
                .addDependency("org.keycloak.testsuite", "integration-arquillian-tests-base", KEYCLOAK_VERSION)
                .addDependency("org.keycloak.testsuite", "integration-arquillian-tests-base", KEYCLOAK_VERSION, "tests");
    }

    @Override
    protected List<String> configureArgs(List<String> args) {
        System.setProperty("quarkus.http.test-port", String.valueOf(configuration.getBindHttpPort()));
        System.setProperty("quarkus.http.test-ssl-port", String.valueOf(configuration.getBindHttpsPort()));
        return args;
    }

    @Override
    protected void checkLiveness() {
        // no-op, Keycloak would throw an exception in the test JVM if something went wrong
    }
}
