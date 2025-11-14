package org.keycloak.tests.db;

import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.conditions.DisabledForDatabases;
import org.keycloak.testframework.database.DatabaseConfig;
import org.keycloak.testframework.database.DatabaseConfigBuilder;
import org.keycloak.testframework.database.EnterpriseDbTestDatabase;
import org.keycloak.testframework.database.PostgresTestDatabase;
import org.keycloak.testframework.database.TestDatabase;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

@KeycloakIntegrationTest(config = PreserveSchemaCaseLiquibaseTest.PreserveSchemaCaseServerConfig.class)
// MSSQL does not support setting the default schema per session.
// TiDb does not support setting the default schema per session.
// Oracle image does not support configuring user/databases with '-'
@DisabledForDatabases({ "mssql", "oracle", "tidb" })
public class PreserveSchemaCaseLiquibaseTest extends AbstractDBSchemaTest {

    @InjectTestDatabase(config = PreserveSchemaCaseDatabaseConfig.class, lifecycle = LifeCycle.CLASS)
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
            if (dbType().equals(PostgresTestDatabase.NAME) || dbType().equals(EnterpriseDbTestDatabase.NAME)) {
                return database.initScript("org/keycloak/tests/db/preserve-schema-case-liquibase-postgres.sql");
            }
            return database.database("keycloak-t");
        }
    }
}
