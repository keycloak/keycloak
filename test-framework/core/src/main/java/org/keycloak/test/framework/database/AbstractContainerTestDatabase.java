package org.keycloak.test.framework.database;

import org.jboss.logging.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

public abstract class AbstractContainerTestDatabase implements TestDatabase {

    private static final Logger LOGGER = Logger.getLogger(AbstractContainerTestDatabase.class);

    private JdbcDatabaseContainer<?> container;

    @SuppressWarnings("resource")
    public void start() {
        container = createContainer();
        container.withStartupTimeout(Duration.ofMinutes(30))
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(AbstractContainerTestDatabase.class)))
                .start();
        try {
            String postStartCommand = getPostStartCommand();
            if (postStartCommand != null) {
                LOGGER.tracev("Running post start command: {0}", postStartCommand);
                String result = container.execInContainer("bash", "-c", postStartCommand).getStdout();
                LOGGER.tracev(result);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        container.stop();
    }

    @Override
    public Map<String, String> serverConfig() {
        return Map.of(
                "db", getKeycloakDatabaseName(),
                "db-url", container.getJdbcUrl(),
                "db-username", container.getUsername(),
                "db-password", container.getPassword()
        );
    }

    public abstract JdbcDatabaseContainer<?> createContainer();

    public String getPostStartCommand() {
        return null;
    }

    public abstract String getKeycloakDatabaseName();

}
