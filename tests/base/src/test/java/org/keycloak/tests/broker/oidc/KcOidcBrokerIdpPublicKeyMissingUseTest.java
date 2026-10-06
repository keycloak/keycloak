package org.keycloak.tests.broker.oidc;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.tests.common.CustomProvidersServerConfig;

import org.junit.jupiter.api.BeforeEach;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class KcOidcBrokerIdpPublicKeyMissingUseTest extends AbstractKcOidcBrokerTest {

    @BeforeEach
    void configureMissingUseJwks() {
        IdentityProviderRepresentation idp = consumerRealm.admin()
                .identityProviders().get(getIdpAlias()).toRepresentation();
        idp.getConfig().put("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_BASIC);
        idp.getConfig().put(OIDCIdentityProviderConfig.JWKS_URL,
                providerRealm.getBaseUrl() + "/missing-use-jwks/jwks");
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
    }
}
