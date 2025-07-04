package org.keycloak.testframework.database;

import org.jboss.logging.Logger;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.logging.JBossLogConsumer;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public abstract class AbstractContainerTestDatabase implements TestDatabase {

    protected boolean reuse;

    protected JdbcDatabaseContainer<?> container;

    public AbstractContainerTestDatabase() {
        reuse = Config.getValueTypeConfig(TestDatabase.class, "reuse", false, Boolean.class);
    }

    public void start() {
        container = createContainer();
        container = container.withStartupTimeout(Duration.ofMinutes(10))
                .withLogConsumer(new JBossLogConsumer(Logger.getLogger("managed.db." + getDatabaseVendor())))
                .withReuse(reuse);
        withDatabaseAndUser(getDatabase(), getUsername(), getPassword());
        container.start();

        try {
            List<String> postStartCommand = getPostStartCommand();
            if (postStartCommand != null) {
                getLogger().tracev("Running post start command: {0}", String.join(" ", postStartCommand));
                String result = container.execInContainer(postStartCommand.toArray(new String[0])).getStdout();
                getLogger().tracev(result);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void withDatabaseAndUser(String database, String username, String password) {
        container.withDatabaseName(database);
        container.withUsername(username);
        container.withPassword(password);
    }

    public void stop() {
        if (!reuse) {
            container.stop();
        }
    }

    @Override
    public Map<String, String> serverConfig() {
        return serverConfig(false);
    }

    public Map<String, String> serverConfig(boolean internal) {
        return Map.of(
                "db", getDatabaseVendor(),
                "db-url", getJdbcUrl(internal),
                "db-username", getUsername(),
                "db-password", getPassword()
        );
    }

    public abstract JdbcDatabaseContainer<?> createContainer();

    public List<String> getPostStartCommand() {
        return null;
    }

    public String getDatabase() {
        return "keycloak";
    }

    public String getUsername() {
        return "keycloak";
    }

    public String getPassword() {
        return "keycloak";
    }

    public String getJdbcUrl(boolean internal) {
        var url = container.getJdbcUrl();
        if (internal) {
            var ip = container.getContainerInfo().getNetworkSettings().getNetworks().values().iterator().next().getIpAddress();
            return url.replace(container.getHost() + ":" + container.getFirstMappedPort(), ip + ":" + container.getExposedPorts().get(0));
        }
        return url;
    }

    public abstract String getDatabaseVendor();

    public abstract Logger getLogger();

}
