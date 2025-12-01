package org.keycloak.tests.clustering;

import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.database.TestDatabase;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.tests.db.CaseSensitiveSchemaTest;

import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = CaseSensitiveSchemaTest.CaseSensitiveServerConfig.class)
public class JdbcPingCustomSchemaTest {
    @InjectTestDatabase(config = CaseSensitiveSchemaTest.CaseSensitiveDatabaseConfig.class, lifecycle = LifeCycle.CLASS)
    TestDatabase db;

    @Test
    public void testClusterFormed() {
        // no-op ClusteredKeycloakServer will fail if a cluster is not formed
    }
}
