package org.keycloak.tests.clustering;

import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.database.TestDatabase;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.tests.db.CaseSensitiveSchemaTest;

@KeycloakIntegrationTest(config = CaseSensitiveSchemaTest.KeycloakConfig.class)
public class JdbcPingCustomSchemaTest {
    @InjectTestDatabase(lifecycle = LifeCycle.CLASS, config = CaseSensitiveSchemaTest.DatabaseConfigurator.class)
    TestDatabase db;

    @Test
    public void testClusterFormed() {
        // no-op ClusteredKeycloakServer will fail if a cluster is not formed
    }
}
