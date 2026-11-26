package org.keycloak.tests.broker.oidc;

import java.net.URL;

import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;

import org.junit.jupiter.api.BeforeEach;

@KeycloakIntegrationTest(config = KcOidcBrokerVaultTest.VaultBrokerServerConfig.class)
public class KcOidcBrokerVaultTest extends AbstractKcOidcBrokerTest {

    static final String VAULT_CLIENT_SECRET = "${vault.oidc_idp}";

    @BeforeEach
    void configureVaultClientSecret() {
        IdentityProviderRepresentation idp = consumerRealm.admin()
                .identityProviders().get(getIdpAlias()).toRepresentation();
        idp.getConfig().put("clientSecret", VAULT_CLIENT_SECRET);
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
    }

    static class VaultBrokerServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("vault", "file")
                    .option("vault-dir", vaultResourcePath());
        }

        private static String vaultResourcePath() {
            URL url = KcOidcBrokerVaultTest.class.getResource("vault");
            if (url == null) {
                throw new RuntimeException("Unable to find vault resource directory");
            }
            return url.getPath();
        }
    }
}
