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
@KeycloakIntegrationTest(config = CaseSensitiveSchemaTest.CaseSensitiveServerConfig.class)
public class CaseSensitiveSchemaTest extends AbstractDBSchemaTest {

    @InjectTestDatabase(lifecycle = LifeCycle.CLASS, config = CaseSensitiveDatabaseConfig.class)
    TestDatabase db;

    public static class CaseSensitiveServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {

            return switch (dbType()) {
                // DBs that convert unquoted to lower-case by default
                case PostgresTestDatabase.NAME -> config.option("db-schema", "KEYCLOAK");
                // DBs that convert unquoted to upper-case by default
                case "dev-file", "dev-mem" ->
                        config.option("db-url-properties", ";INIT=CREATE SCHEMA IF NOT EXISTS keycloak").option("db-schema", "keycloak");
                default -> config.option("db-schema", "keycloak");
            };
        }
    }

    public static class CaseSensitiveDatabaseConfig implements DatabaseConfig {
        @Override
        public DatabaseConfigBuilder configure(DatabaseConfigBuilder database) {
            if (PostgresTestDatabase.NAME.equals(dbType())) {
                database.initScript("org/keycloak/tests/db/case-sensitive-schema-postgres.sql");
            }
            return database;
        }
    }
}
