/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.broker.oauth.OAuth2IdentityProviderFactory;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;

import org.junit.jupiter.api.Assertions;

/**
 * Tests that the pure OAuth2 identity provider correctly refreshes an expired stored token
 * when {@code POST /broker/{alias}/token} (V2) is called.
 *
 * Covers the bug where {@code AbstractOAuth2IdentityProvider.exchangeStoredToken()} returned
 * the expired token without attempting a refresh (unlike the V1 path fixed in #39508).
 */
@KeycloakIntegrationTest(config = OAuth2IdentityProviderStoreTokenV2Test.IdentityBrokeringAPIV2ServerConfig.class)
public class OAuth2IdentityProviderStoreTokenV2Test implements InterfaceIdentityProviderStoreTokenV2Test {

    @InjectRealm(config = OAuth2IdpRealmConfig.class)
    ManagedRealm realm;

    @InjectRealm(ref = "external-realm", config = ExternalRealmConfig.class)
    ManagedRealm externalRealm;

    @InjectOAuthClient(ref = "external-realm", realmRef = "external-realm", config = ExternalTestClientConfig.class)
    OAuthClient oauthExternal;

    @InjectOAuthClient(config = ExternalClientConfig.class)
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @Override
    public ManagedRealm getRealm() {
        return realm;
    }

    @Override
    public ManagedRealm getExternalRealm() {
        return externalRealm;
    }

    @Override
    public OAuthClient getOAuthClient() {
        return oauth;
    }

    @Override
    public LoginPage getLoginPage() {
        return loginPage;
    }

    @Override
    public RunOnServerClient getRunOnServer() {
        return runOnServer;
    }

    @Override
    public TimeOffSet getTimeOffSet() {
        return timeOffSet;
    }

    public OAuthClient getOauthClientExternal() {
        return oauthExternal;
    }

    @Override
    public boolean isRefreshTokenAllowed() {
        return true;
    }

    @Override
    public void checkSuccessfulTokenResponse(AbstractHttpResponse response) {
        Assertions.assertInstanceOf(AccessTokenResponse.class, response);
        AccessTokenResponse externalTokens = (AccessTokenResponse) response;
        Assertions.assertNotNull(externalTokens.getAccessToken());
        Assertions.assertNull(externalTokens.getRefreshToken());
        Assertions.assertNull(externalTokens.getIdToken());
        UserInfoResponse userInfoResponse = oauthExternal.userInfoRequest(externalTokens.getAccessToken()).send();
        Assertions.assertEquals(200, userInfoResponse.getStatusCode());
        Assertions.assertNotNull(userInfoResponse.getUserInfo().getPreferredUsername());
    }

    static class IdentityBrokeringAPIV2ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.IDENTITY_BROKERING_API_V2);
        }
    }

    public static class ExternalRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.users(UserBuilder.create("testuser")
                    .name("Test", "User")
                    .email("test@localhost")
                    .emailVerified(Boolean.TRUE)
                    .password("password"));
            return realm;
        }
    }

    public static class OAuth2IdpRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.identityProviders(IdentityProviderBuilder.create()
                    .providerId(OAuth2IdentityProviderFactory.PROVIDER_ID)
                    .alias(IDP_ALIAS)
                    .attribute("clientId", "test-app-external-realm")
                    .attribute("clientSecret", "test-secret")
                    .attribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                    .attribute(OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL,
                            "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/openid-connect/token")
                    .attribute("authorizationUrl",
                            "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/openid-connect/auth")
                    .attribute("userInfoUrl",
                            "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/openid-connect/userinfo")
                    .attribute("defaultScope", "openid")
                    .storeToken(true)
                    .addReadTokenRoleOnCreate(true)
                    .build());
            return realm;
        }
    }

    public static class ExternalTestClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("test-app")
                    .serviceAccountsEnabled(true)
                    .directAccessGrantsEnabled(true)
                    .redirectUris("http://localhost:8080/*")
                    .secret("test-secret");
        }
    }

    public static class ExternalClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("test-app")
                    .serviceAccountsEnabled(true)
                    .directAccessGrantsEnabled(true)
                    .attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.TRUE.toString())
                    .attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, IDP_ALIAS)
                    .secret("test-secret");
        }
    }
}
