package org.keycloak.tests.clustering;

import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.conditions.DisabledForDatabases;
import org.keycloak.testframework.database.DatabaseConfig;
import org.keycloak.testframework.database.DatabaseConfigBuilder;
import org.keycloak.testframework.database.TestDatabase;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = JdbcPingCustomSchemaTest.JdbcPingCustomSchemaServerConfig.class)
@DisabledForDatabases({"mariadb", "mssql", "mysql", "oracle", "tidb"})
public class JdbcPingCustomSchemaTest {

    @InjectTestDatabase(config = JdbcPingCustomSchemaDatabaseConfig.class, lifecycle = LifeCycle.CLASS)
    TestDatabase db;

    @Test
    public void testClusterFormed() {
        // no-op ClusteredKeycloakServer will fail if a cluster is not formed
    }

    public static class JdbcPingCustomSchemaServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("db-schema", "KEYCLOAK");
        }
    }

    public static class JdbcPingCustomSchemaDatabaseConfig implements DatabaseConfig {
        @Override
        public DatabaseConfigBuilder configure(DatabaseConfigBuilder database) {
            database.initScript("org/keycloak/tests/clustering/case-sensitive-schema-postgres.sql");
            return database;
        }
    }
}
