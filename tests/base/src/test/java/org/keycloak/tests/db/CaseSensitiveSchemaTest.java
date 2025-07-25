package org.keycloak.tests.db;

import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.conditions.DisabledForDatabases;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.database.DatabaseConfigBuilder;
import org.keycloak.testframework.database.PostgresTestDatabase;
import org.keycloak.testframework.database.TestDatabase;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.RoleBuilder;

@KeycloakIntegrationTest(config = CaseSensitiveSchemaTest.KeycloakConfig.class)
// MSSQL does not support setting the default schema per session
@DisabledForDatabases("mssql")
public class CaseSensitiveSchemaTest {
    @InjectTestDatabase(lifecycle = LifeCycle.CLASS, config = DatabaseConfigurator.class)
    TestDatabase db;

    @InjectClient
    ManagedClient managedClient;

    @Test
    public void testCaseSensitiveSchema() {
        RoleRepresentation role1 = RoleBuilder.create()
              .name("role1")
              .description("role1-description")
              .singleAttribute("role1-attr-key", "role1-attr-val")
              .build();

        RolesResource roles = managedClient.admin().roles();
        roles.create(role1);
        roles.deleteRole(role1.getName());
    }

    protected static String dbType() {
        String database = Config.getSelectedSupplier(TestDatabase.class);
        return database == null ? "dev-mem" : database;
    }

    public static class KeycloakConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {

            return switch (dbType()) {
                // DBs that convert unquoted to lower-case by default
                case PostgresTestDatabase.NAME -> config.option("db-schema", "KEYCLOAK");
                // DBs that convert unquoted to upper-case by default
                case "dev-file", "dev-mem" -> config.option("db-url-properties", ";INIT=CREATE SCHEMA IF NOT EXISTS keycloak").option("db-schema", "keycloak");
                default -> config.option("db-schema", "keycloak");
            };
        }
    }

    public static class DatabaseConfigurator implements org.keycloak.testframework.database.DatabaseConfigurator {
        @Override
        public DatabaseConfigBuilder configure(DatabaseConfigBuilder builder) {
            if (PostgresTestDatabase.NAME.equals(dbType())) {
                builder.withInitScript("org/keycloak/tests/db/case-sensitive-schema-postgres.sql");
            }
            return builder;
        }
    }
}
