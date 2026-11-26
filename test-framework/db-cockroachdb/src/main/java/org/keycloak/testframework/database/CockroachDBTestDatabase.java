package org.keycloak.testframework.database;

import java.util.List;

import org.keycloak.testframework.util.ContainerImages;

import org.jboss.logging.Logger;
import org.testcontainers.cockroachdb.CockroachContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

class CockroachDBTestDatabase extends AbstractContainerTestDatabase {

    private static final Logger LOGGER = Logger.getLogger(CockroachDBTestDatabase.class);

    public static final String NAME = "cockroachdb";

    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        // The image only creates a user with a password in secure mode, which would need TLS and certificates on the client side.
        // Insecure mode ignores passwords, so the user and the database are created in the post start command.
        return new CockroachContainer(DockerImageName.parse(ContainerImages.getContainerImageName(NAME)).asCompatibleSubstituteFor("cockroachdb/cockroach"))
                .withCommand("start-single-node", "--insecure");
    }

    @Override
    public void withDatabaseAndUser(String database, String username, String password) {
        // Not supported by the image in insecure mode
    }

    @Override
    public List<String> getPostStartCommand() {
        // CockroachDB defaults to SERIALIZABLE, which fails with retryable errors that Keycloak doesn't retry.
        // READ COMMITTED behaves like PostgreSQL, which is what Keycloak is built for.
        String sql = "CREATE DATABASE IF NOT EXISTS \\\"" + getDatabase() + "\\\";"
                + " CREATE USER IF NOT EXISTS " + getUsername() + ";"
                + " GRANT ALL ON DATABASE \\\"" + getDatabase() + "\\\" TO " + getUsername() + ";"
                + " ALTER ROLE " + getUsername() + " SET default_transaction_isolation = 'read committed';"
                + " ALTER ROLE " + getUsername() + " SET create_table_with_schema_locked = false;"
                + " ALTER ROLE " + getUsername() + " SET multiple_active_portals_enabled = true;";
        return List.of("bash", "-c", "./cockroach sql --insecure -e \"" + sql + "\"");
    }

    @Override
    public String getJdbcUrl(boolean internal) {
        return super.getJdbcUrl(internal).replaceFirst("(:\\d+)/[^/?]*", "$1/" + getDatabase());
    }

    @Override
    public String getDatabaseVendor() {
        // CockroachDB is tested as a plain PostgreSQL database
        return "postgres";
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
