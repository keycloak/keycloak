package org.keycloak.tests.db;

import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.conditions.DisabledForDatabases;
import org.keycloak.testframework.database.DatabaseConfigBuilder;
import org.keycloak.testframework.database.PostgresTestDatabase;
import org.keycloak.testframework.database.TestDatabase;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

@KeycloakIntegrationTest(config = PreserveSchemaCaseLiquibaseTest.KeycloakConfig.class)
// MSSQL does not support setting the default schema per session.
// TiDb does not support setting the default schema per session.
// Oracle image does not support configuring user/databases with '-'
@DisabledForDatabases({ "mssql", "oracle", "tidb" })
public class PreserveSchemaCaseLiquibaseTest extends CaseSensitiveSchemaTest {

    @InjectTestDatabase(lifecycle = LifeCycle.CLASS, config = DatabaseConfigurator.class)
    TestDatabase db;

    public static class KeycloakConfig implements KeycloakServerConfig {
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

    public static class DatabaseConfigurator implements org.keycloak.testframework.database.DatabaseConfigurator {
        @Override
        public DatabaseConfigBuilder configure(DatabaseConfigBuilder builder) {
            if (dbType().equals(PostgresTestDatabase.NAME)) {
                return builder.withInitScript("org/keycloak/tests/db/preserve-schema-case-liquibase-postgres.sql");
            }
            return builder.withDatabase("keycloak-t");
        }
    }
}
