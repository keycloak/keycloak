package org.keycloak.tests.broker.oidc;

import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.utils.DefaultKeyProviders;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.tests.common.CustomProvidersServerConfig;

import org.junit.jupiter.api.BeforeEach;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class KcOidcBrokerPrivateKeyJwtMissingUseTest extends AbstractKcOidcBrokerTest {

    @BeforeEach
    void configurePrivateKeyJwtWithMissingUse() {
        ComponentRepresentation ecdsaKey = new ComponentRepresentation();
        ecdsaKey.setName("ecdsa-generated");
        ecdsaKey.setProviderId("ecdsa-generated");
        ecdsaKey.setProviderType(KeyProvider.class.getName());
        MultivaluedHashMap<String, String> keyConfig = new MultivaluedHashMap<>();
        keyConfig.putSingle("priority", DefaultKeyProviders.DEFAULT_PRIORITY);
        keyConfig.putSingle("ecdsaEllipticCurveKey", "P-384");
        ecdsaKey.setConfig(keyConfig);
        consumerRealm.admin().components().add(ecdsaKey).close();

        String consumerBaseUrl = consumerRealm.getBaseUrl();
        ClientRepresentation client = providerRealm.admin().clients().findByClientId(CLIENT_ID).get(0);
        client.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
        client.getAttributes().put(OIDCConfigAttributes.USE_JWKS_URL, "true");
        client.getAttributes().put(OIDCConfigAttributes.JWKS_URL,
                consumerBaseUrl + "/missing-use-jwks/jwks");
        providerRealm.admin().clients().get(client.getId()).update(client);

        IdentityProviderRepresentation idp = consumerRealm.admin()
                .identityProviders().get(getIdpAlias()).toRepresentation();
        idp.getConfig().put("clientSecret", null);
        idp.getConfig().put("clientAuthMethod", OIDCLoginProtocol.PRIVATE_KEY_JWT);
        idp.getConfig().put("clientAssertionSigningAlg", "ES384");
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
    }
}
