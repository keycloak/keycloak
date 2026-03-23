package org.keycloak.tests.broker;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.jpa.JpaRealmProviderFactory;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
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
        return getOAuthClient().doFetchExternalIdpToken(IDP_ALIAS, token);
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
        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertTrue(tokenResponse.isSuccess());
        checkSuccessfulTokenResponse(externalTokens);

        // remove external token enabled
        ClientResource clientResource = AdminApiUtil.findClientByClientId(realm.admin(), "test-app");
        ManagedClient client = new ManagedClient(clientResource.toRepresentation(), clientResource);
        client.updateWithCleanup(c-> c.attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.FALSE.toString()));
        externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode());

        // external access disabled but idp selected
        client.updateWithCleanup(c-> c.attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.FALSE.toString()).attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, IDP_ALIAS));
        externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode());

        // external access enabled but idp different
        client.updateWithCleanup(c-> c.attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.TRUE.toString()).attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, "other-idp"));
        externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode());

        // enable again
        client.updateWithCleanup(c-> c.attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.TRUE.toString()).attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, IDP_ALIAS));
        externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertTrue(tokenResponse.isSuccess());
        checkSuccessfulTokenResponse(externalTokens);
    }

    @Test
    default void testStoreTokenDisabled() {
        ManagedRealm realm = getRealm();
        OAuthClient oauth = getOAuthClient();
        realm.updateIdentityProvider(IDP_ALIAS, idp -> {
            idp.setStoreToken(false);
        });

        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertTrue(externalTokens.isSuccess());
        checkSuccessfulTokenResponse(externalTokens);

        String realmName = realm.getName();
        String oldTokenFromDatabase = getRunOnServer().fetch(session -> {
            RealmModel r = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(r, "testuser");
            return session.getProvider(UserProvider.class, JpaRealmProviderFactory.PROVIDER_ID).getFederatedIdentity(r, user, IDP_ALIAS).getToken();
        }, String.class);

        //Ensure that the token is null in the db
        Assertions.assertNull(oldTokenFromDatabase);

        if (isRefreshTokenAllowed()) {
            // now test extra refresh of the token in session
            getTimeOffSet().set(externalTokens.getExpiresIn() - IdentityProviderModel.DEFAULT_MIN_VALIDITY_TOKEN + 1);

            internalTokens = oauth.doRefreshTokenRequest(internalTokens.getRefreshToken());
            Assertions.assertEquals(200, internalTokens.getStatusCode());
            AccessTokenResponse externalTokens2 = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
            Assertions.assertEquals(200, externalTokens2.getStatusCode());

            // Check that we now have a different access and refresh token
            Assertions.assertNotEquals(externalTokens.getAccessToken(), externalTokens2.getAccessToken());
            Assertions.assertNull(externalTokens.getRefreshToken());
            Assertions.assertNull(externalTokens2.getRefreshToken());

            String newTokenFromDatabase = getRunOnServer().fetch(session -> {
                RealmModel r = session.realms().getRealmByName(realmName);
                UserModel user = session.users().getUserByUsername(r, "testuser");
                return session.getProvider(UserProvider.class, JpaRealmProviderFactory.PROVIDER_ID).getFederatedIdentity(r, user, IDP_ALIAS).getToken();
            }, String.class);

            // Ensure that the new token is null in the db
            Assertions.assertNull(newTokenFromDatabase);

            getTimeOffSet().set(0);
        }
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
