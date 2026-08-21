package org.keycloak.tests.broker.oidc;

import java.util.Map;

import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.broker.OidcBrokerConfigSupport;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class KcOidcBrokerNonceParameterTest implements OidcBrokerConfigSupport {

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = OidcBrokerConfigSupport.OidcProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = NonceConsumerRealmConfig.class)
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

    @Test
    public void testNonceSet() {
        disableUpdateProfileOnFirstLogin();

        oauth.client("consumer-client");

        AuthorizationEndpointResponse authzResponse = doLoginSocialWithNonce("123456");
        assertTrue(authzResponse.isSuccess());
        AccessTokenResponse response = oauth.doAccessTokenRequest(authzResponse.getCode());
        IDToken idToken = toIdToken(response.getIdToken());

        assertEquals("123456", idToken.getNonce());
        String federatedIdTokenString = (String) idToken.getOtherClaims().get(OIDCIdentityProvider.FEDERATED_ID_TOKEN);
        assertNotNull(federatedIdTokenString);
        IDToken federatedIdToken = toIdToken(federatedIdTokenString);
        assertNotNull(federatedIdToken.getNonce());
    }

    @Test
    public void testNonceNotSet() {
        disableUpdateProfileOnFirstLogin();

        IdentityProviderRepresentation idpRep = consumerRealm.admin().identityProviders().get(IDP_OIDC_ALIAS).toRepresentation();
        idpRep.getConfig().put("disableNonce", Boolean.TRUE.toString());
        consumerRealm.admin().identityProviders().get(IDP_OIDC_ALIAS).update(idpRep);

        oauth.client("consumer-client");

        AuthorizationEndpointResponse authzResponse = doLoginSocialWithNonce(null);
        assertTrue(authzResponse.isSuccess());
        AccessTokenResponse response = oauth.doAccessTokenRequest(authzResponse.getCode());
        IDToken idToken = toIdToken(response.getIdToken());

        assertNull(idToken.getNonce());
        String federatedIdTokenString = (String) idToken.getOtherClaims().get(OIDCIdentityProvider.FEDERATED_ID_TOKEN);
        assertNotNull(federatedIdTokenString);
        IDToken federatedIdToken = toIdToken(federatedIdTokenString);
        assertNull(federatedIdToken.getNonce());
    }

    private AuthorizationEndpointResponse doLoginSocialWithNonce(String nonce) {
        oauth.loginForm().nonce(nonce).open();
        loginPage.clickSocial(IDP_OIDC_ALIAS);
        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();
        return oauth.parseLoginResponse();
    }

    private IDToken toIdToken(String encoded) {
        try {
            return new JWSInput(encoded).readJsonContent(IDToken.class);
        } catch (JWSInputException cause) {
            throw new RuntimeException("Failed to deserialize token", cause);
        }
    }

    static class NonceConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            ProtocolMapperRepresentation sessionNoteMapper = new ProtocolMapperRepresentation();
            sessionNoteMapper.setName(OIDCIdentityProvider.FEDERATED_ID_TOKEN);
            sessionNoteMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            sessionNoteMapper.setProtocolMapper(UserSessionNoteMapper.PROVIDER_ID);
            sessionNoteMapper.setConfig(Map.of(
                    ProtocolMapperUtils.USER_SESSION_NOTE,
                    OIDCIdentityProvider.FEDERATED_ID_TOKEN + ":" + IDP_OIDC_ALIAS,
                    OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME,
                    OIDCIdentityProvider.FEDERATED_ID_TOKEN,
                    OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN,
                    Boolean.TRUE.toString()));

            return OidcBrokerConfigSupport.configureConsumerRealm(realm,
                    OidcBrokerConfigSupport.createOidcIdentityProvider())
                    .clients(ClientBuilder.create("consumer-client")
                            .publicClient()
                            .redirectUris("*")
                            .protocolMappers(sessionNoteMapper));
        }
    }
}
