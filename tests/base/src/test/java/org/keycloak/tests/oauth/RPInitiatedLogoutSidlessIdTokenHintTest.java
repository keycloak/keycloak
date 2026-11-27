/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.oauth;

import org.keycloak.OAuth2Constants;
import org.keycloak.representations.IDToken;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LogoutConfirmPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * An {@code id_token_hint} without a {@code sid} claim cannot be bound to the browser session, so RP-initiated
 * logout must ask the user for confirmation instead of terminating the browser session right away.
 */
@KeycloakIntegrationTest
public class RPInitiatedLogoutSidlessIdTokenHintTest {

    private static final String SERVICE_CLIENT_ID = "sidless-hint-service-client";
    private static final String SERVICE_CLIENT_SECRET = "secret";

    @InjectRealm(config = SidlessHintRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectUser(config = SidlessHintUserConfig.class)
    ManagedUser user;

    @InjectPage
    LogoutConfirmPage logoutConfirmPage;

    public static class SidlessHintRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.clients(ClientBuilder.create(SERVICE_CLIENT_ID)
                    .secret(SERVICE_CLIENT_SECRET)
                    .serviceAccountsEnabled());
        }
    }

    public static class SidlessHintUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("sidless-hint-user")
                    .name("Sidless", "Hint")
                    .password("password")
                    .email("sidless-hint-user@localhost")
                    .emailVerified(true);
        }
    }

    @Test
    public void logoutWithSidlessIdTokenHintRequiresConfirmation() {
        AuthorizationEndpointResponse login = oauth.doLogin(user.getUsername(), user.getPassword());
        Assertions.assertTrue(login.isSuccess());
        Assertions.assertEquals(1, user.admin().getUserSessions().size());

        // A client credentials grant runs in a transient session, so its ID token carries no sid
        AccessTokenResponse serviceTokens = oauth.client(SERVICE_CLIENT_ID, SERVICE_CLIENT_SECRET)
                .scope(OAuth2Constants.SCOPE_OPENID)
                .doClientCredentialsGrantAccessTokenRequest();
        Assertions.assertTrue(serviceTokens.isSuccess());
        IDToken idToken = oauth.parseToken(serviceTokens.getIdToken(), IDToken.class);
        Assertions.assertNull(idToken.getSessionState());

        oauth.logoutForm().idTokenHint(serviceTokens.getIdToken()).open();

        logoutConfirmPage.assertCurrent();
        Assertions.assertEquals(1, user.admin().getUserSessions().size());
    }
}
