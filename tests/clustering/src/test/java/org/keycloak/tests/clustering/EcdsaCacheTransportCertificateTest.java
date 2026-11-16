package org.keycloak.tests.clustering;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = EcdsaCacheTransportCertificateTest.EcdsaServerConfig.class)
public class EcdsaCacheTransportCertificateTest {

    @Test
    public void testClusterFormedWithEcdsaCertificate() {
        // no-op ClusteredKeycloakServer will fail if the nodes cannot form a cluster using the mTLS transport
    }

    public static class EcdsaServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config
                    .option("spi-jgroups-mtls--default--algorithm", "ECDSA")
                    .option("spi-jgroups-mtls--default--curve", "secp384r1");
        }
    }
}
