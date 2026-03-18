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
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class IdentityProviderStoreTokenTest extends AbstractIdentityProviderStoreTokenTest {

    @InjectRealm(config = IdpRealmConfig.class)
    ManagedRealm realm;

    @InjectRealm(ref = "external-realm", config = ExternalRealmConfig.class)
    ManagedRealm externalRealm;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @Override
    protected ManagedRealm getRealm() {
        return realm;
    }

    @Override
    protected ManagedRealm getExternalRealm() {
        return externalRealm;
    }

    @Override
    protected void checkSuccessfulTokenResponse(AccessTokenResponse externalTokens) {
        Assertions.assertNotNull(externalTokens.getAccessToken());
        UserInfoResponse userInfoResponse = oauthExternal.userInfoRequest(externalTokens.getAccessToken()).send();
        Assertions.assertEquals(200, userInfoResponse.getStatusCode());
        Assertions.assertNotNull(userInfoResponse.getUserInfo().getPreferredUsername());
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
        checkSuccessfulTokenResponse(externalTokens);

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
        checkSuccessfulTokenResponse(externalTokens);

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

        timeOffSet.set(0);
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
        checkSuccessfulTokenResponse(externalTokens);

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

        timeOffSet.set(0);
    }

    @Test
    @Override
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

        timeOffSet.set(0);
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
}
