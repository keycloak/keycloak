package org.keycloak.tests.broker;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.jpa.JpaRealmProviderFactory;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
public interface InterfaceOIDCIdentityProviderStoreTokenTest extends InterfaceIdentityProviderStoreTokenTest {

    TimeOffSet getTimeOffSet();
    OAuthClient getOauthClientExternal();

    @Override
    default boolean isRefreshTokenAllowed() {
        return true;
    }

    @Test
    default void testRefreshTokenFetchExternalIdpTokenSuccess() {
        OAuthClient oauth = getOAuthClient();
        ManagedRealm realm = getRealm();

        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        AbstractHttpResponse externalTokens = doFetchExternalIdpToken(internalTokens.getAccessToken());
        Assertions.assertEquals(200, externalTokens.getStatusCode());
        checkSuccessfulTokenResponse(externalTokens);

        String realmName = realm.getName();
        String oldTokenFromDatabase = getRunOnServer().fetch(session -> {
            RealmModel r = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(r, "testuser");
            return session.getProvider(UserProvider.class, JpaRealmProviderFactory.PROVIDER_ID).getFederatedIdentity(r, user, IDP_ALIAS).getToken();
        }, String.class);

        getTimeOffSet().set(((AccessTokenResponse) externalTokens).getExpiresIn() - IdentityProviderModel.DEFAULT_MIN_VALIDITY_TOKEN + 1);

        internalTokens = oauth.doRefreshTokenRequest(internalTokens.getRefreshToken());
        Assertions.assertEquals(200, internalTokens.getStatusCode());
        AbstractHttpResponse externalTokens2 = doFetchExternalIdpToken(internalTokens.getAccessToken());
        Assertions.assertEquals(200, externalTokens2.getStatusCode());
        checkSuccessfulTokenResponse(externalTokens);

        // Check that we now have a different access and refresh token
        Assertions.assertNotEquals(((AccessTokenResponse) externalTokens).getAccessToken(),
                ((AccessTokenResponse) externalTokens2).getAccessToken());

        String newTokenFromDatabase = getRunOnServer().fetch(session -> {
            RealmModel r = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(r, "testuser");
            return session.getProvider(UserProvider.class, JpaRealmProviderFactory.PROVIDER_ID).getFederatedIdentity(r, user, IDP_ALIAS).getToken();
        }, String.class);

        // Ensure that the new token has been persisted
        Assertions.assertNotEquals(newTokenFromDatabase, oldTokenFromDatabase);

        getTimeOffSet().set(0);
    }

    @Test
    default void testRefreshTokenFetchExternalIdpTokenFailure() {
        OAuthClient oauth = getOAuthClient();
        ManagedRealm realm = getRealm();

        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        AbstractHttpResponse externalTokens = doFetchExternalIdpToken(internalTokens.getAccessToken());
        Assertions.assertEquals(200, externalTokens.getStatusCode());
        checkSuccessfulTokenResponse(externalTokens);

        getTimeOffSet().set(((AccessTokenResponse) externalTokens).getExpiresIn() + 10);

        IdentityProviderResource resource = realm.admin().identityProviders().get(IDP_ALIAS);
        IdentityProviderRepresentation idp = resource.toRepresentation();
        idp.getConfig().put("clientSecret", "wrongpassword");
        resource.update(idp);

        internalTokens = oauth.doRefreshTokenRequest(internalTokens.getRefreshToken());
        Assertions.assertEquals(200, internalTokens.getStatusCode());
        AbstractHttpResponse error = doFetchExternalIdpToken(internalTokens.getAccessToken());
        Assertions.assertFalse(error.isSuccess());

        logout();

        //restore secret manually, clientSecret cannot be cleaned up automatically
        idp.getConfig().put("clientSecret", "test-secret");
        resource.update(idp);

        getTimeOffSet().set(0);
    }

    @Test
    default void testRefreshTokenFetchExternalIdpFromDifferentLogin() {
        OAuthClient oauth = getOAuthClient();
        ManagedRealm realm = getRealm();

        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        //set created user password
        String userid = realm.admin().users().search("testuser", true).get(0).getId();
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue("password");
        cred.setTemporary(Boolean.FALSE);
        realm.admin().users().get(userid).resetPassword(cred);

        AccessTokenResponse internalTokensPasswordGrant = oauth.passwordGrantRequest("testuser", "password").send();
        Assertions.assertTrue(internalTokensPasswordGrant.isSuccess());

        //external token from direct grant
        AbstractHttpResponse externalTokensPasswordGrant = doFetchExternalIdpToken(internalTokensPasswordGrant.getAccessToken());
        Assertions.assertEquals(200, externalTokensPasswordGrant.getStatusCode());

        getTimeOffSet().set(((AccessTokenResponse) externalTokensPasswordGrant).getExpiresIn() - IdentityProviderModel.DEFAULT_MIN_VALIDITY_TOKEN + 1);

        //test refresh with external token from direct grant
        internalTokensPasswordGrant = oauth.doRefreshTokenRequest(internalTokensPasswordGrant.getRefreshToken());
        Assertions.assertEquals(200, internalTokensPasswordGrant.getStatusCode());
        AbstractHttpResponse externalTokensPasswordGrant2 = doFetchExternalIdpToken(internalTokens.getAccessToken());
        Assertions.assertEquals(200, externalTokensPasswordGrant2.getStatusCode());

        // Check that now we have a different token
        Assertions.assertNotEquals(((AccessTokenResponse) externalTokensPasswordGrant).getAccessToken(),
                ((AccessTokenResponse) externalTokensPasswordGrant2).getAccessToken());

        if (!isIdentityBrokeringAPIV1()) {
            //disable the store token
            realm.updateIdentityProvider(IDP_ALIAS, idp -> {
                idp.setStoreToken(false);
            });

            //success because external token are in the user session after the broker login
            AbstractHttpResponse externalTokens = doFetchExternalIdpToken(internalTokens.getAccessToken());
            Assertions.assertEquals(200, externalTokens.getStatusCode());

            //fail because external token are not in the user session
            externalTokensPasswordGrant = doFetchExternalIdpToken(internalTokensPasswordGrant.getAccessToken());
            Assertions.assertEquals(400, externalTokensPasswordGrant.getStatusCode());
        }

        getTimeOffSet().set(0);
    }

    static class ExternalRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {

            realm.addUser("testuser")
                    .name("Test", "User")
                    .email("test@localhost")
                    .emailVerified(Boolean.TRUE)
                    .password("password");
            return realm;
        }
    }

    public static class IdpRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.identityProvider(IdentityProviderBuilder.create()
                    .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                    .alias(IDP_ALIAS)
                    .setAttribute("clientId", "test-app-external-realm")
                    .setAttribute("clientSecret", "test-secret")
                    .setAttribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                    .setAttribute(OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL, "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/openid-connect/token")
                    .setAttribute("authorizationUrl", "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/openid-connect/auth")
                    .setAttribute("logoutUrl", "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/openid-connect/logout")
                    .setAttribute("backchannelSupported", Boolean.TRUE.toString())
                    .setAttribute(OIDCIdentityProviderConfig.JWKS_URL, "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/openid-connect/cert")
                    .storeToken(true)
                    .addReadTokenRoleOnCreate(true)
                    .build());
            return realm;
        }
    }

    public static class TestClientConfig implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId("test-app")
                    .serviceAccountsEnabled(true)
                    .directAccessGrantsEnabled(true)
                    .redirectUris("http://localhost:8080/*")
                    .secret("test-secret");
        }
    }
}
