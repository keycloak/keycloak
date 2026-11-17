package org.keycloak.tests.broker.oidc;

import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class KcOidcBrokerNonceParameterTest extends AbstractKcOidcBrokerTest {

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = NonceConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectRunOnServer(realmRef = "consumer")
    RunOnServerClient runOnServer;

    @Test
    public void testNonceSet() {
        disableUpdateProfileOnFirstLogin();

        oauth.client("consumer-client");

        AuthorizationEndpointResponse authzResponse = doLoginSocialWithNonce("123456");
        assertTrue(authzResponse.isSuccess());
        AccessTokenResponse response = oauth.doAccessTokenRequest(authzResponse.getCode());
        IDToken idToken = toIdToken(response.getIdToken());

        assertEquals("123456", idToken.getNonce());
        assertNotNull(brokeredIdToken(idToken.getSessionId()).getNonce());
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
        assertNull(brokeredIdToken(idToken.getSessionId()).getNonce());
    }

    private AuthorizationEndpointResponse doLoginSocialWithNonce(String nonce) {
        oauth.loginForm().nonce(nonce).open();
        loginPage.clickSocial(IDP_OIDC_ALIAS);
        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();
        return oauth.parseLoginResponse();
    }

    /**
     * The ID token the identity provider issued is held as a session note. It is a credential of the external provider,
     * so it cannot be read through a protocol mapper and has to be read from the session directly.
     */
    private IDToken brokeredIdToken(String sessionId) {
        String realmName = consumerRealm.getName();
        String encoded = runOnServer.fetch(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserSessionModel userSession = session.sessions().getUserSession(realm, sessionId);
            return userSession.getNote(OIDCIdentityProvider.FEDERATED_ID_TOKEN + ":" + IDP_OIDC_ALIAS);
        }, String.class);

        assertNotNull(encoded, "Identity provider did not store an ID token in the user session");
        return toIdToken(encoded);
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
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider())
                    .clients(ClientBuilder.create("consumer-client")
                            .publicClient()
                            .redirectUris("*"));
        }
    }
}
