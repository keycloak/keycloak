package org.keycloak.tests.broker;

import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.broker.provider.UserAuthenticationIdentityProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.jpa.JpaRealmProviderFactory;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
public interface InterfaceIdentityProviderStoreTokenV2Test extends InterfaceIdentityProviderStoreTokenTest {

    TimeOffSet getTimeOffSet();

    @Override
    default boolean isIdentityBrokeringAPIV1() {
        return false;
    }

    @Override
    default AbstractHttpResponse doFetchExternalIdpToken(String token) {
        return getOAuthClient().doFetchExternalIdpTokenPost(IDP_ALIAS, token);
    }

    @Test
    default void testIdentityBrokeringAPIV1Disabled() {
        OAuthClient oauth = getOAuthClient();

        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        AbstractHttpResponse externalTokens = oauth.doFetchExternalIdpTokenString(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertFalse(externalTokens.isSuccess());
    }

    @Test
    default void testPublicClient() {
        ManagedRealm realm = getRealm();
        OAuthClient oauth = getOAuthClient();

        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        ClientResource clientResource = AdminApiUtil.findClientByClientId(realm.admin(), "test-app");
        ClientRepresentation clientRep = clientResource.toRepresentation();
        realm.cleanup().add(r -> {
            clientRep.setPublicClient(false);
            clientResource.update(clientRep);
        });
        clientRep.setPublicClient(true);
        clientResource.update(clientRep);
        AbstractHttpResponse externalTokens = doFetchExternalIdpToken(internalTokens.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode());
        Assertions.assertEquals(OAuthErrorException.INVALID_CLIENT, ((AccessTokenResponse) externalTokens).getError());
    }

    @Test
    default void testDifferentAudience() {
        ManagedRealm realm = getRealm();
        OAuthClient oauth = getOAuthClient();

        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        realm.admin().clients().create(ClientConfigBuilder.create()
                .clientId("test-app-other")
                .attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.TRUE.toString())
                .attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, IDP_ALIAS)
                .secret("test-secret")
                .build());
        realm.cleanup().add(r -> oauth.client("test-app", "test-secret"));
        AccessTokenResponse externalTokens = oauth.client("test-app-other", "test-secret").doFetchExternalIdpTokenPost(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode());
        Assertions.assertEquals(OAuthErrorException.UNAUTHORIZED_CLIENT, externalTokens.getError());
        Assertions.assertEquals("Client is not within the token audience", externalTokens.getErrorDescription());
    }

    @Test
    default void testOIDCIdentityProviderStoreTokenGrantViaClientSettings() {
        ManagedRealm realm = getRealm();
        OAuthClient oauth = getOAuthClient();

        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        // external access enabled initially
        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpTokenPost(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertTrue(tokenResponse.isSuccess());
        checkSuccessfulTokenResponse(externalTokens);

        // remove external token enabled
        ClientResource clientResource = AdminApiUtil.findClientByClientId(realm.admin(), "test-app");
        ManagedClient client = new ManagedClient(clientResource.toRepresentation(), clientResource);
        client.updateWithCleanup(c-> c.attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.FALSE.toString()));
        externalTokens = oauth.doFetchExternalIdpTokenPost(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode());

        // external access disabled but idp selected
        client.updateWithCleanup(c-> c.attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.FALSE.toString()).attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, IDP_ALIAS));
        externalTokens = oauth.doFetchExternalIdpTokenPost(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode());

        // external access enabled but idp different
        client.updateWithCleanup(c-> c.attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.TRUE.toString()).attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, "other-idp"));
        externalTokens = oauth.doFetchExternalIdpTokenPost(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode());

        // enable again
        client.updateWithCleanup(c-> c.attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.TRUE.toString()).attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, IDP_ALIAS));
        externalTokens = oauth.doFetchExternalIdpTokenPost(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertTrue(tokenResponse.isSuccess());
        checkSuccessfulTokenResponse(externalTokens);
    }

    @Test
    default void testStoreTokenDisabled() {
        ManagedRealm realm = getRealm();
        OAuthClient oauth = getOAuthClient();
        realm.updateIdentityProvider(IDP_ALIAS, idp -> {
            idp.setStoreToken(false);
            idp.getConfig().put(IdentityProviderModel.STORE_TOKEN_IN_SESSION, Boolean.TRUE.toString());
        });

        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());
        AccessToken accessToken = oauth.parseToken(internalTokens.getAccessToken(), AccessToken.class);

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpTokenPost(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertTrue(externalTokens.isSuccess());
        checkSuccessfulTokenResponse(externalTokens);

        String oldTokenFromDatabase = getTokenFromDatabase(realm.getName());

        //Ensure that the token is null in the db
        Assertions.assertNull(oldTokenFromDatabase);
        // ensure the token is saved in the session
        Assertions.assertNotNull(getTokenFromSession(realm.getName(), accessToken.getSessionId()));

        if (isRefreshTokenAllowed()) {
            // now test extra refresh of the token in session
            getTimeOffSet().set(externalTokens.getExpiresIn() - IdentityProviderModel.DEFAULT_MIN_VALIDITY_TOKEN + 1);

            internalTokens = oauth.doRefreshTokenRequest(internalTokens.getRefreshToken());
            Assertions.assertEquals(200, internalTokens.getStatusCode());
            AccessTokenResponse externalTokens2 = oauth.doFetchExternalIdpTokenPost(IDP_ALIAS, internalTokens.getAccessToken());
            Assertions.assertEquals(200, externalTokens2.getStatusCode());
            checkSuccessfulTokenResponse(externalTokens2);

            // Check that we now have a different access token
            Assertions.assertNotEquals(externalTokens.getAccessToken(), externalTokens2.getAccessToken());

            // Ensure that the new token is null in the db
            Assertions.assertNull(getTokenFromDatabase(realm.getName()));
            // ensure the token is saved in the session
            Assertions.assertNotNull(getTokenFromSession(realm.getName(), accessToken.getSessionId()));

            getTimeOffSet().set(0);
        }
    }

    @Test
    default void testStoreTokenInSessionDisabled() {
        ManagedRealm realm = getRealm();
        OAuthClient oauth = getOAuthClient();
        realm.updateIdentityProvider(IDP_ALIAS, idp -> {
            idp.setStoreToken(true);
            idp.getConfig().put(IdentityProviderModel.STORE_TOKEN_IN_SESSION, Boolean.FALSE.toString());
        });

        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        AccessToken accessToken = oauth.parseToken(internalTokens.getAccessToken(), AccessToken.class);
        Assertions.assertTrue(internalTokens.isSuccess());

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpTokenPost(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(200, externalTokens.getStatusCode());
        checkSuccessfulTokenResponse(externalTokens);

        // ensure the token is in database
        Assertions.assertNotNull(getTokenFromDatabase(realm.getName()));

        // ensure the token is not saved in the session
        Assertions.assertNull(getTokenFromSession(realm.getName(), accessToken.getSessionId()));

        if (isRefreshTokenAllowed()) {
            // now test extra refresh of the token in session
            getTimeOffSet().set(externalTokens.getExpiresIn() - IdentityProviderModel.DEFAULT_MIN_VALIDITY_TOKEN + 1);

            internalTokens = oauth.doRefreshTokenRequest(internalTokens.getRefreshToken());
            Assertions.assertEquals(200, internalTokens.getStatusCode());
            AccessTokenResponse externalTokens2 = oauth.doFetchExternalIdpTokenPost(IDP_ALIAS, internalTokens.getAccessToken());
            Assertions.assertEquals(200, externalTokens2.getStatusCode());
            checkSuccessfulTokenResponse(externalTokens2);

            // Check that we now have a different access token
            Assertions.assertNotEquals(externalTokens.getAccessToken(), externalTokens2.getAccessToken());

            // ensure the token is in database
            Assertions.assertNotNull(getTokenFromDatabase(realm.getName()));
            // ensure the token is not saved in the session
            Assertions.assertNull(getTokenFromSession(realm.getName(), accessToken.getSessionId()));

            getTimeOffSet().set(0);
        }
    }

    @Test
    default void testStoreTokenAllDisabled() {
        ManagedRealm realm = getRealm();
        OAuthClient oauth = getOAuthClient();
        realm.updateIdentityProvider(IDP_ALIAS, idp -> {
            idp.setStoreToken(false);
            idp.getConfig().put(IdentityProviderModel.STORE_TOKEN_IN_SESSION, Boolean.FALSE.toString());
        });

        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());
        AccessToken accessToken = oauth.parseToken(internalTokens.getAccessToken(), AccessToken.class);

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpTokenPost(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(400, externalTokens.getStatusCode());
        Assertions.assertEquals("token_expired", externalTokens.getError());
        Assertions.assertEquals("No token stored.", externalTokens.getErrorDescription());

        // ensure the token is not in database
        Assertions.assertNull(getTokenFromDatabase(realm.getName()));
        // ensure the token is not saved in the session
        Assertions.assertNull(getTokenFromSession(realm.getName(), accessToken.getSessionId()));
    }

    @Test
    default void testStoreTokenAllEnabled() {
        ManagedRealm realm = getRealm();
        OAuthClient oauth = getOAuthClient();
        realm.updateIdentityProvider(IDP_ALIAS, idp -> {
            idp.setStoreToken(true);
            idp.getConfig().put(IdentityProviderModel.STORE_TOKEN_IN_SESSION, Boolean.TRUE.toString());
        });

        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());
        AccessToken accessToken = oauth.parseToken(internalTokens.getAccessToken(), AccessToken.class);

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpTokenPost(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertTrue(externalTokens.isSuccess());
        checkSuccessfulTokenResponse(externalTokens);

        String oldTokenFromDatabase = getTokenFromDatabase(realm.getName());

        //Ensure that the token is present in the db
        Assertions.assertNotNull(oldTokenFromDatabase);
        // ensure the token is saved in the session
        Assertions.assertNotNull(getTokenFromSession(realm.getName(), accessToken.getSessionId()));

        if (isRefreshTokenAllowed()) {
            // now test extra refresh of the token in session
            getTimeOffSet().set(externalTokens.getExpiresIn() - IdentityProviderModel.DEFAULT_MIN_VALIDITY_TOKEN + 1);

            internalTokens = oauth.doRefreshTokenRequest(internalTokens.getRefreshToken());
            Assertions.assertEquals(200, internalTokens.getStatusCode());
            AccessTokenResponse externalTokens2 = oauth.doFetchExternalIdpTokenPost(IDP_ALIAS, internalTokens.getAccessToken());
            Assertions.assertEquals(200, externalTokens2.getStatusCode());
            checkSuccessfulTokenResponse(externalTokens2);

            // Check that we now have a different access token
            Assertions.assertNotEquals(externalTokens.getAccessToken(), externalTokens2.getAccessToken());

            // Ensure that the new token is null in the db
            Assertions.assertNotNull(getTokenFromDatabase(realm.getName()));
            // ensure the token is saved in the session
            Assertions.assertNotNull(getTokenFromSession(realm.getName(), accessToken.getSessionId()));

            getTimeOffSet().set(0);
        }
    }

    default String getTokenFromDatabase(String realmName) {
        return getRunOnServer().fetch(session -> {
             RealmModel r = session.realms().getRealmByName(realmName);
             UserModel user = session.users().getUserByUsername(r, "testuser");
             return session.getProvider(UserProvider.class, JpaRealmProviderFactory.PROVIDER_ID).getFederatedIdentity(r, user, IDP_ALIAS).getToken();
         }, String.class);
    }

    default String getTokenFromSession(String realmName, String sessionId) {
        return getRunOnServer().fetch(session -> {
            RealmModel r = session.realms().getRealmByName(realmName);
            UserSessionModel userSession = session.sessions().getUserSession(r, sessionId);
            return userSession.getNote(UserAuthenticationIdentityProvider.FEDERATED_ACCESS_TOKEN);
        }, String.class);
    }

    static class ExternalClientConfig implements ClientConfig {
        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId("test-app")
                .serviceAccountsEnabled(true)
                .directAccessGrantsEnabled(true)
                .attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.TRUE.toString())
                .attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, IDP_ALIAS)
                .secret("test-secret");
        }
    }
}
