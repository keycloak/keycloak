package org.keycloak.tests.clustering;

import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.infinispan.CacheType;
import org.keycloak.testframework.database.TestDatabase;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.db.CaseSensitiveSchemaTest;

@KeycloakIntegrationTest(config = JdbcPingCustomSchemaTest.JdbcPingKeycloakServerConfig.class)
public class JdbcPingCustomSchemaTest {
    @InjectTestDatabase(config = CaseSensitiveSchemaTest.CaseSensitiveDatabaseConfig.class, lifecycle = LifeCycle.CLASS)
    TestDatabase db;

    @Test
    public void testClusterFormed() {
        // no-op ClusteredKeycloakServer will fail if a cluster is not formed
    }

    public static class JdbcPingKeycloakServerConfig extends CaseSensitiveSchemaTest.CaseSensitiveServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            config.setCache(CacheType.ISPN);

            return super.configure(config);
        }
    }
}
