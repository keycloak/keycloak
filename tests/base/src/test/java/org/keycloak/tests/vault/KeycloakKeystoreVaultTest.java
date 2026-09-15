package org.keycloak.tests.vault;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

@KeycloakIntegrationTest(config = KeycloakKeystoreVaultTest.KeystoreVaultConfig.class)
class KeycloakKeystoreVaultTest extends AbstractKeycloakVaultTest {

    static class KeystoreVaultConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("vault", "keystore")
                    .option("vault-file", vaultResourcePath("vault/myks"))
                    .option("vault-pass", "keystorepassword");
        }
    }
}
