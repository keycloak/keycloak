package org.keycloak.testframework.database;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.keycloak.testframework.util.ContainerImages;

import org.jboss.logging.Logger;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.YugabyteDBYSQLContainer;
import org.testcontainers.utility.DockerImageName;

class YugabyteTestDatabase extends AbstractContainerTestDatabase {

    private static final Logger LOGGER = Logger.getLogger(YugabyteTestDatabase.class);

    public static final String NAME = "yugabyte";

    // Database created by the image, used to connect before the requested database exists
    private static final String DEFAULT_DATABASE = "yugabyte";

    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        return new YugabyteDBYSQLContainer(DockerImageName.parse(ContainerImages.getContainerImageName(NAME)).asCompatibleSubstituteFor("yugabytedb/yugabyte")) {
            // Keycloak and the test framework connect with the PostgreSQL driver, as YugabyteDB is tested as a plain PostgreSQL database
            @Override
            public String getDriverClassName() {
                return "org.postgresql.Driver";
            }

            // The init script must run in the database created by the post start command, not in the default database
            @Override
            protected void runInitScriptIfRequired() {
            }

            @Override
            public String getJdbcUrl() {
                return "jdbc:postgresql://" + getHost() + ":" + getMappedPort(5433) + "/" + getDatabaseName() + constructUrlParameters("?", "&");
            }
        };
    }

    @Override
    public void withDatabaseAndUser(String database, String username, String password) {
        // The requested database is created in the post start command, so that it is created explicitly as non-colocated and the init script runs in it
        container.withDatabaseName(DEFAULT_DATABASE);
        container.withUsername(username);
        container.withPassword(password);
    }

    @Override
    public List<String> getPostStartCommand() {
        // The database is deliberately not colocated. Colocation stores all tables in a single tablet, which creates the Keycloak schema about 40% faster (240s instead of 400s).
        // However, during the Keycloak migration on a colocated database the transaction holding Keycloak's database lock was aborted ("current transaction is expired or aborted by a conflict"),
        // so the lock was silently lost and releasing it failed. This did not happen in a non-colocated database. The exact trigger was not identified.
        // Colocation is also a suspected cause of intermittent "schema version mismatch" errors.
        // The command runs on every start, including re-used containers, so it only creates the database if it doesn't exist yet.
        String ysqlsh = "PGPASSWORD='" + getPassword() + "' ysqlsh -h \"$(hostname)\" -U " + getUsername() + " -d " + DEFAULT_DATABASE;
        String command = ysqlsh + " -tAc \"SELECT 1 FROM pg_database WHERE datname = '" + getDatabase() + "'\" | grep -q 1"
                + " || " + ysqlsh + " -c 'CREATE DATABASE \"" + getDatabase() + "\" WITH COLOCATION = false'";
        String initScript = readInitScript();
        if (initScript != null) {
            // Statements are sent via stdin so that each runs on its own, and statements failing on a reused container (for example "schema already exists") don't fail the command
            command += " && printf '%s' '" + initScript.replace("'", "'\\''") + "' | PGPASSWORD='" + getPassword() + "' ysqlsh -h \"$(hostname)\" -U " + getUsername() + " -d \"" + getDatabase() + "\" -q";
        }
        return List.of("bash", "-c", command);
    }

    private String readInitScript() {
        if (config.getInitScript() == null) {
            return null;
        }
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(config.getInitScript())) {
            if (is == null) {
                throw new IllegalArgumentException("Init script not found: " + config.getInitScript());
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String getJdbcUrl(boolean internal) {
        return super.getJdbcUrl(internal).replaceFirst("/" + DEFAULT_DATABASE + "(\\?|$)", "/" + getDatabase() + "$1");
    }

    @Override
    public String getDatabaseVendor() {
        // YugabyteDB is tested as a plain PostgreSQL database
        return "postgres";
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
