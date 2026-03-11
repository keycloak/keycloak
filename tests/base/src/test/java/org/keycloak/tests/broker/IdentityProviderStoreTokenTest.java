/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.tests.broker;

import java.util.Optional;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.jpa.JpaRealmProviderFactory;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class IdentityProviderStoreTokenTest {

    public static String IDP_ALIAS = "oidc-idp-alias";

    public static String EXTERNAL_REALM_NAME = "external-realm";

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectOAuthClient(ref = "external-realm", realmRef = "external-realm", config = TestClientConfig.class)
    OAuthClient oauthExternal;

    @InjectPage
    LoginPage loginPage;

    @InjectRealm(config = IdpRealmConfig.class)
    protected ManagedRealm realm;

    @InjectRealm(ref = "external-realm", config = ExternalRealmConfig.class)
    ManagedRealm externalRealm;

    @InjectClient(attachTo = "test-app")
    ManagedClient client;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @AfterEach
    public void logout() {
        Optional<UserRepresentation> userResult = realm.admin().users().search("testuser", true).stream().findFirst();
        if (userResult.isPresent()) {
            AccountHelper.logout(realm.admin(), "testuser");
            realm.admin().users().delete(userResult.get().getId()).close();
        }

        userResult = externalRealm.admin().users().search("testuser", true).stream().findFirst();
        if (userResult.isPresent()) {
            AccountHelper.logout(externalRealm.admin(), "testuser");
        }
    }

    @Test
    public void testOIDCIdentityProviderStoreTokenManualRoleGrant() {
        realm.updateIdentityProvider(IDP_ALIAS, idp-> {
            idp.setAddReadTokenRoleOnCreate(false);
        });

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        //user without the role tries to read the stored token
        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode());

        AccountHelper.logout(realm.admin(), "testuser");

        //grant the role to the user and repeat the login
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel brokerClient = realm.getClientByClientId(Constants.BROKER_SERVICE_CLIENT_ID);
            RoleModel readTokenRole = brokerClient.getRole(Constants.READ_TOKEN_ROLE);
            UserModel user = session.users().getUserByUsername(realm, "testuser");
            user.grantRole(readTokenRole);
        });

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(200, externalTokens.getStatusCode());

        UserInfoResponse userInfoResponse = oauthExternal.userInfoRequest(externalTokens.getAccessToken()).send();
        Assertions.assertEquals(200, userInfoResponse.getStatusCode());
        Assertions.assertNotNull(userInfoResponse.getUserInfo().getPreferredUsername());
    }

    @Test
    public void testOIDCIdentityProviderStoreTokenRoleGrantOnUserCreation() {
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertEquals(200, externalTokens.getStatusCode());

        //use the stored token to refresh for a refresh token request
        UserInfoResponse userInfoResponse = oauthExternal.userInfoRequest(externalTokens.getAccessToken()).send();
        Assertions.assertEquals(200, userInfoResponse.getStatusCode());
    }

    @Test
    public void testOIDCIdentityProviderStoreTokenGrantViaClientSettings() {
        realm.updateIdentityProvider(IDP_ALIAS, idp -> {
            idp.setAddReadTokenRoleOnCreate(false);
        });

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        //external access disabled
        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode());

        //external access enabled but idp is not selected
        client.updateWithCleanup(c-> c.attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.TRUE.toString()));
        externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode());

        //external access disabled but idp selected
        client.updateWithCleanup(c-> c.attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.FALSE.toString()).attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, IDP_ALIAS));
        externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode());

        //external access enabled and idp selected
        client.updateWithCleanup(c-> c.attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.TRUE.toString()).attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, IDP_ALIAS));
        externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertEquals(200, externalTokens.getStatusCode());

        //restore attributes as cleanup for client is wip
        client.updateWithCleanup(c-> c.attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.FALSE.toString()).attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, null));
    }

    @Test
    public void testRefreshTokenFetchExternalIdpTokenSuccess() {
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(200, externalTokens.getStatusCode());

        String realmName = realm.getName();
        String oldTokenFromDatabase = runOnServer.fetch(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(realm, "testuser");
            return session.getProvider(UserProvider.class, JpaRealmProviderFactory.PROVIDER_ID).getFederatedIdentity(realm, user, IDP_ALIAS).getToken();
        }, String.class);

        timeOffSet.set(externalTokens.getExpiresIn() - IdentityProviderModel.DEFAULT_MIN_VALIDITY_TOKEN + 1);

        internalTokens = oauth.doRefreshTokenRequest(internalTokens.getRefreshToken());
        Assertions.assertEquals(200, internalTokens.getStatusCode());
        AccessTokenResponse externalTokens2 = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(200, externalTokens2.getStatusCode());

        // Check that we now have a different access and refresh token
        Assertions.assertNotEquals(externalTokens.getAccessToken(), externalTokens2.getAccessToken());
        Assertions.assertNull(externalTokens.getRefreshToken());
        Assertions.assertNull(externalTokens2.getRefreshToken());

        String newTokenFromDatabase = runOnServer.fetch(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(realm, "testuser");
            return session.getProvider(UserProvider.class, JpaRealmProviderFactory.PROVIDER_ID).getFederatedIdentity(realm, user, IDP_ALIAS).getToken();
        }, String.class);

        // Ensure that the new token has been persisted
        Assertions.assertNotEquals(newTokenFromDatabase, oldTokenFromDatabase);
    }

    @Test
    public void testRefreshTokenFetchExternalIdpTokenFailure() {
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(200, externalTokens.getStatusCode());

        timeOffSet.set(externalTokens.getExpiresIn() + 10);

        IdentityProviderResource resource = realm.admin().identityProviders().get(IDP_ALIAS);
        IdentityProviderRepresentation idp = resource.toRepresentation();
        idp.getConfig().put("clientSecret", "wrongpassword");
        resource.update(idp);

        internalTokens = oauth.doRefreshTokenRequest(internalTokens.getRefreshToken());
        Assertions.assertEquals(200, internalTokens.getStatusCode());
        AccessTokenResponse error = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(400, error.getStatusCode());

        logout();

        //restore secret manually, clientSecret cannot be cleaned up automatically
        idp.getConfig().put("clientSecret", "test-secret");
        resource.update(idp);
    }

    @Test
    public void testStoreTokenDisabled() {
        realm.updateIdentityProvider(IDP_ALIAS, idp -> {
            idp.setStoreToken(false);
        });

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(200, externalTokens.getStatusCode());

        String realmName = realm.getName();
        String oldTokenFromDatabase = runOnServer.fetch(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(realm, "testuser");
            return session.getProvider(UserProvider.class, JpaRealmProviderFactory.PROVIDER_ID).getFederatedIdentity(realm, user, IDP_ALIAS).getToken();
        }, String.class);

        //Ensure that the token is null in the db
        Assertions.assertNull(oldTokenFromDatabase);

        timeOffSet.set(externalTokens.getExpiresIn() - IdentityProviderModel.DEFAULT_MIN_VALIDITY_TOKEN + 1);

        internalTokens = oauth.doRefreshTokenRequest(internalTokens.getRefreshToken());
        Assertions.assertEquals(200, internalTokens.getStatusCode());
        AccessTokenResponse externalTokens2 = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(200, externalTokens2.getStatusCode());

        // Check that we now have a different access and refresh token
        Assertions.assertNotEquals(externalTokens.getAccessToken(), externalTokens2.getAccessToken());
        Assertions.assertNull(externalTokens.getRefreshToken());
        Assertions.assertNull(externalTokens2.getRefreshToken());

        String newTokenFromDatabase = runOnServer.fetch(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(realm, "testuser");
            return session.getProvider(UserProvider.class, JpaRealmProviderFactory.PROVIDER_ID).getFederatedIdentity(realm, user, IDP_ALIAS).getToken();
        }, String.class);

        // Ensure that the new token is null in the db
        Assertions.assertNull(newTokenFromDatabase);
    }

    @Test
    public void testIdpDisabled() {
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        realm.updateIdentityProvider(IDP_ALIAS, idp -> {
            idp.setEnabled(false);
        });

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(502, externalTokens.getStatusCode());
    }

    @Test
    public void testUserDisabled() {
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        realm.updateUser("testuser", user -> {
            user.setEnabled(false);
        });

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(400, externalTokens.getStatusCode());
    }

    @Test
    public void testRefreshTokenFetchExternalIdpFromDifferentLogin() {
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

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
        AccessTokenResponse externalTokensPasswordGrant = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokensPasswordGrant.getAccessToken());
        Assertions.assertEquals(200, externalTokensPasswordGrant.getStatusCode());

        timeOffSet.set(externalTokensPasswordGrant.getExpiresIn() - IdentityProviderModel.DEFAULT_MIN_VALIDITY_TOKEN + 1);

        //test refresh with external token from direct grant
        internalTokensPasswordGrant = oauth.doRefreshTokenRequest(internalTokensPasswordGrant.getRefreshToken());
        Assertions.assertEquals(200, internalTokensPasswordGrant.getStatusCode());
        AccessTokenResponse externalTokensPasswordGrant2 = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(200, externalTokensPasswordGrant2.getStatusCode());

        // Check that now we have a different token
        Assertions.assertNotEquals(externalTokensPasswordGrant.getAccessToken(), externalTokensPasswordGrant2.getAccessToken());

        //disable the store token
        realm.updateIdentityProvider(IDP_ALIAS, idp-> {
            idp.setStoreToken(false);
        });

        //success because external token are in the user session after the broker login
        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(200, externalTokens.getStatusCode());

        //fail because external token are not in the user session
        externalTokensPasswordGrant = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokensPasswordGrant.getAccessToken());
        Assertions.assertEquals(400, externalTokensPasswordGrant.getStatusCode());
        timeOffSet.set(0);
    }

    public static class ExternalRealmConfig implements RealmConfig {

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
                    .setAttribute("clientId", "test-app")
                    .setAttribute("clientSecret", "test-secret")
                    .setAttribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                    .setAttribute(OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL, "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/openid-connect/token")
                    .setAttribute("authorizationUrl", "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/openid-connect/auth")
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
