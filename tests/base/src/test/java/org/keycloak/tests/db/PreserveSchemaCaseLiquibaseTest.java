package org.keycloak.tests.db;

import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.database.DatabaseConfig;
import org.keycloak.testframework.database.DatabaseConfigBuilder;
import org.keycloak.testframework.database.PostgresTestDatabase;
import org.keycloak.testframework.database.TestDatabase;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

@DisabledIfEnvironmentVariable(named = "KC_TEST_DATABASE", matches = "mssql", disabledReason = "MSSQL does not support setting the default schema per session")
@DisabledIfEnvironmentVariable(named = "KC_TEST_DATABASE", matches = "oracle", disabledReason = "Oracle image does not support configuring user/databases with '-'")
@KeycloakIntegrationTest(config = PreserveSchemaCaseLiquibaseTest.PreserveSchemaCaseServerConfig.class)
public class PreserveSchemaCaseLiquibaseTest extends AbstractDBSchemaTest {

    @InjectTestDatabase(lifecycle = LifeCycle.CLASS, config = PreserveSchemaCaseDatabaseConfig.class)
    TestDatabase db;

    public static class PreserveSchemaCaseServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            switch (dbType()) {
                case "dev-file":
                case "dev-mem":
                    config.option("db-url-properties", ";INIT=CREATE SCHEMA IF NOT EXISTS \"keycloak-t\"");
            }
            return config.option("db-schema", "keycloak-t");
        }
    }

    private static class PreserveSchemaCaseDatabaseConfig implements DatabaseConfig {
        @Override
        public DatabaseConfigBuilder configure(DatabaseConfigBuilder database) {
            if (dbType().equals(PostgresTestDatabase.NAME)) {
                return database.initScript("org/keycloak/tests/db/preserve-schema-case-liquibase-postgres.sql");
            }
            return database.database("keycloak-t");
        }
    }
}
