package org.keycloak.testframework.database;

import java.util.List;

import org.keycloak.testframework.util.ContainerImages;

import org.jboss.logging.Logger;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.YugabyteDBYSQLContainer;
import org.testcontainers.utility.DockerImageName;

class YugabyteTestDatabase extends AbstractContainerTestDatabase {

    private static final Logger LOGGER = Logger.getLogger(YugabyteTestDatabase.class);

    public static final String NAME = "yugabyte";

    // Database created by the image, used to connect before the colocated database exists
    private static final String DEFAULT_DATABASE = "yugabyte";

    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        return new YugabyteDBYSQLContainer(DockerImageName.parse(ContainerImages.getContainerImageName(NAME)).asCompatibleSubstituteFor("yugabytedb/yugabyte")) {
            // Keycloak and the test framework connect with the PostgreSQL driver, as YugabyteDB is tested as a plain PostgreSQL database
            @Override
            public String getDriverClassName() {
                return "org.postgresql.Driver";
            }

            @Override
            public String getJdbcUrl() {
                return "jdbc:postgresql://" + getHost() + ":" + getMappedPort(5433) + "/" + getDatabaseName() + constructUrlParameters("?", "&");
            }
        };
    }

    @Override
    public void withDatabaseAndUser(String database, String username, String password) {
        // The requested database is created colocated in the post start command, as the image would create it non-colocated
        container.withDatabaseName(DEFAULT_DATABASE);
        container.withUsername(username);
        container.withPassword(password);
    }

    @Override
    public List<String> getPostStartCommand() {
        // Colocation stores all tables in a single tablet, which is faster and lighter for the many small tables of Keycloak.
        // The command runs on every start, including re-used containers, so it only creates the database if it doesn't exist yet.
        String ysqlsh = "PGPASSWORD='" + getPassword() + "' ysqlsh -h \"$(hostname)\" -U " + getUsername() + " -d " + DEFAULT_DATABASE;
        return List.of("bash", "-c",
                ysqlsh + " -tAc \"SELECT 1 FROM pg_database WHERE datname = '" + getDatabase() + "'\" | grep -q 1"
                        + " || " + ysqlsh + " -c 'CREATE DATABASE " + getDatabase() + " WITH COLOCATION = true'");
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
