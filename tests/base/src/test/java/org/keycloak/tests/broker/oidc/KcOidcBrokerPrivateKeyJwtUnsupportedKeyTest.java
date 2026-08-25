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
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.broker.BrokerLoginTest;
import org.keycloak.tests.broker.KcOidcBrokerConfigSupport;
import org.keycloak.tests.common.CustomProvidersServerConfig;

import org.junit.jupiter.api.BeforeEach;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class KcOidcBrokerPrivateKeyJwtUnsupportedKeyTest implements BrokerLoginTest, KcOidcBrokerConfigSupport {

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = KcOidcBrokerConfigSupport.OidcProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = KcOidcBrokerConfigSupport.OidcConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectOAuthClient(realmRef = "consumer")
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver webDriver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    IdpReviewUserProfilePage updateProfilePage;

    @Override
    public ManagedRealm getProviderRealm() {
        return providerRealm;
    }

    @Override
    public ManagedRealm getConsumerRealm() {
        return consumerRealm;
    }

    @Override
    public OAuthClient getOAuthClient() {
        return oauth;
    }

    @Override
    public ManagedWebDriver getWebDriver() {
        return webDriver;
    }

    @Override
    public LoginPage getLoginPage() {
        return loginPage;
    }

    @Override
    public IdpReviewUserProfilePage getUpdateProfilePage() {
        return updateProfilePage;
    }

    @BeforeEach
    void configurePrivateKeyJwtWithUnsupportedKey() {
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
                consumerBaseUrl + "/unsupported-key-jwks/jwks");
        providerRealm.admin().clients().get(client.getId()).update(client);

        IdentityProviderRepresentation idp = consumerRealm.admin()
                .identityProviders().get(getIdpAlias()).toRepresentation();
        idp.getConfig().put("clientSecret", "");
        idp.getConfig().put("clientAuthMethod", OIDCLoginProtocol.PRIVATE_KEY_JWT);
        idp.getConfig().put("clientAssertionSigningAlg", "ES384");
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);
    }
}
