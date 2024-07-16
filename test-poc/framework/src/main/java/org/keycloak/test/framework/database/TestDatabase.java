package org.keycloak.test.framework.database;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

public class TestDatabase {

    private final DatabaseConfig databaseConfig;

    private GenericContainer<?> container;

    public TestDatabase(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public void start() {
        if (databaseConfig.getContainerImage() != null) {
            container = createContainer();
            container.withStartupTimeout(Duration.ofMinutes(5)).start();
            databaseConfig.url(getJdbcUrl());
            if (container instanceof MSSQLServerContainer) {
                databaseConfig.username(((JdbcDatabaseContainer<?>) container).getUsername());
                databaseConfig.password(((JdbcDatabaseContainer<?>) container).getPassword());
            }
        }
    }

    public void stop() {
        if (databaseConfig.getContainerImage() != null) {
            container.stop();
            container = null;
        } else if ("dev-mem".equals(databaseConfig.getVendor())) {
            // TODO Stop in-mem H2 database
        }
    }

    public Map<String, String> getServerConfig() {
        return databaseConfig.toConfig();
    }

    public String getJdbcUrl() {
        return ((JdbcDatabaseContainer<?>)container).getJdbcUrl();
    }

    private JdbcDatabaseContainer<?> configureJdbcContainer(JdbcDatabaseContainer<?> jdbcDatabaseContainer) {
        if (jdbcDatabaseContainer instanceof MSSQLServerContainer) {
            return jdbcDatabaseContainer;
        }

        return jdbcDatabaseContainer
                .withDatabaseName("keycloak")
                .withUsername(databaseConfig.getUsername())
                .withPassword(databaseConfig.getPassword());
    }

    private GenericContainer<?> createContainer() {
        return switch (databaseConfig.getVendor()) {
            case PostgresDatabaseSupplier.VENDOR -> {
                DockerImageName POSTGRES = DockerImageName.parse(databaseConfig.getContainerImage()).asCompatibleSubstituteFor("postgres");
                yield configureJdbcContainer(new PostgreSQLContainer<>(POSTGRES));
            }
            case MariaDBDatabaseSupplier.VENDOR -> {
                DockerImageName MARIADB = DockerImageName.parse(databaseConfig.getContainerImage()).asCompatibleSubstituteFor("mariadb");
                yield configureJdbcContainer(new MariaDBContainer<>(MARIADB));
            }
            case MySQLDatabaseSupplier.VENDOR -> {
                DockerImageName MYSQL = DockerImageName.parse(databaseConfig.getContainerImage()).asCompatibleSubstituteFor("mysql");
                yield configureJdbcContainer(new MySQLContainer<>(MYSQL));
            }
            case MSSQLServerDatabaseSupplier.VENDOR -> {
                DockerImageName MSSQL = DockerImageName.parse(databaseConfig.getContainerImage()).asCompatibleSubstituteFor("sqlserver");
                yield configureJdbcContainer(new MSSQLServerContainer<>(MSSQL));
            }
            default -> throw new RuntimeException("Unsupported database: " + databaseConfig.getVendor());
        };
    }

}
