package org.keycloak.tests.db;

import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.conditions.DisabledForDatabases;
import org.keycloak.testframework.database.DatabaseConfig;
import org.keycloak.testframework.database.DatabaseConfigBuilder;
import org.keycloak.testframework.database.EnterpriseDbTestDatabase;
import org.keycloak.testframework.database.PostgresTestDatabase;
import org.keycloak.testframework.database.TestDatabase;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

@KeycloakIntegrationTest(config = CaseSensitiveSchemaTest.CaseSensitiveServerConfig.class)
// MSSQL does not support setting the default schema per session
// TiDb does not support setting the default schema per session.
@DisabledForDatabases({"mssql", "tidb"})
public class CaseSensitiveSchemaTest extends AbstractDBSchemaTest {

    @InjectTestDatabase(config = CaseSensitiveDatabaseConfig.class)
    TestDatabase db;

    public static class CaseSensitiveServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {

            return switch (dbType()) {
                // DBs that convert unquoted to lower-case by default
                case PostgresTestDatabase.NAME, EnterpriseDbTestDatabase.NAME
                        -> config.option("db-schema", "KEYCLOAK");
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
            if (PostgresTestDatabase.NAME.equals(dbType()) || EnterpriseDbTestDatabase.NAME.equals(dbType())) {
                database.initScript("org/keycloak/tests/db/case-sensitive-schema-postgres.sql");
            }
            return database;
        }
    }
}
