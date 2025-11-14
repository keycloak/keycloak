package org.keycloak.testframework.database;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.logging.JBossLogConsumer;

import org.jboss.logging.Logger;
import org.testcontainers.containers.JdbcDatabaseContainer;

public abstract class AbstractContainerTestDatabase implements TestDatabase {

    protected boolean reuse;

    protected JdbcDatabaseContainer<?> container;
    protected DatabaseConfiguration config;

    public void start(DatabaseConfiguration config) {
        this.config = config;

        String reuseProp = Config.getValueTypeFQN(TestDatabase.class, "reuse");
        boolean reuseConfigured = Config.get(reuseProp, false, Boolean.class);
        if (config.isPreventReuse() && reuseConfigured) {
            getLogger().warnf("Ignoring '%s' as test explicitly prevents it", reuseProp);
            this.reuse = false;
        } else {
            this.reuse = reuseConfigured;
        }

        container = createContainer();
        container = container.withStartupTimeout(Duration.ofMinutes(10))
                .withLogConsumer(new JBossLogConsumer(Logger.getLogger("managed.db." + getDatabaseVendor())))
                .withReuse(reuse)
                .withInitScript(config.getInitScript());
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
        return config.getDatabase() == null ? "keycloak" : config.getDatabase();
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
