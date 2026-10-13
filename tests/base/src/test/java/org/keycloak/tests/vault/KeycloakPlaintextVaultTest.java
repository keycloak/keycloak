package org.keycloak.tests.vault;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

@KeycloakIntegrationTest(config = KeycloakPlaintextVaultTest.PlaintextVaultConfig.class)
class KeycloakPlaintextVaultTest extends AbstractKeycloakVaultTest {

    static class PlaintextVaultConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("vault", "file")
                    .option("vault-dir", vaultResourcePath("vault"));
        }
    }
}
