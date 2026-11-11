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
package org.keycloak.tests.oauth;


import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.OAuthErrorException.INVALID_GRANT;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class AuthorizationCodeTest {

    @InjectRealm(config =  AuthzCodeRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @InjectPage
    ErrorPage errorPage;

    @InjectHttpClient(followRedirects = false)
    CloseableHttpClient httpClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;
    
    @InjectRunOnServer(permittedPackages = {"org.keycloak.tests", "org.keycloak.admin"})
    RunOnServerClient runOnServer;

    // Issue 51003
    @Test
    public void authorizationCodeCanBeRedeemedAfterClientUuidRetargeting() {
        String redirectUri = oauth.getRedirectUri();

        ClientRepresentation clientRep = ClientConfigBuilder.create()
                .clientId("retarget-client")
                .secret("secret")
                .redirectUris(redirectUri)
                .build();
        try (Response resp = managedRealm.admin().clients().create(clientRep)) {
            String retargetClientUuid = ApiUtil.getCreatedId(resp);
            managedRealm.cleanup().add(r -> r.clients().delete(retargetClientUuid));

            AuthorizationEndpointResponse retargetClientLogin = oauth
                    .client("retarget-client", "secret")
                    .redirectUri(redirectUri)
                    .loginForm()
                    .doLogin("test-user@localhost", "password");
            Assertions.assertNotNull(retargetClientLogin.getCode());

            AuthorizationEndpointResponse testAppLogin = oauth
                    .client("test-app", "test-secret")
                    .redirectUri(redirectUri)
                    .loginForm()
                    .doLoginWithCookie();
            String testAppCode = testAppLogin.getCode();
            Assertions.assertNotNull(testAppCode);

            String testAppUuid = managedRealm.admin()
                    .clients()
                    .findByClientId("test-app")
                    .get(0)
                    .getId();

            String retargetedCode = testAppCode.replace(testAppUuid, retargetClientUuid);

            events.clear();

            // Attempt to use "code" created for different client should fail
            oauth.client("retarget-client", "secret").redirectUri(redirectUri);
            AccessTokenResponse tokenResponse = oauth
                    .accessTokenRequest(retargetedCode)
                    .redirectUri(redirectUri)
                    .send();

            Assertions.assertFalse(tokenResponse.isSuccess());
            assertEquals(INVALID_GRANT, tokenResponse.getError());
            EventAssertion.assertError(events.poll()).type(EventType.CODE_TO_TOKEN_ERROR).error(Errors.INVALID_CODE);
        }
    }
    
    @Test
    public void authorizationCodeExchange() {
        AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");
        Assertions.assertTrue(response.isRedirected());
        String code = response.getCode();
        Assertions.assertNotNull(code);

        EventAssertion.expectLoginSuccess(events.poll());

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(code).send();
        Assertions.assertTrue(tokenResponse.isSuccess());
        Assertions.assertNotNull(tokenResponse.getAccessToken());
        EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN);
    }

    @Test
    public void authorizationCodeCannotBeRedeemedByDifferentUser() {
        AuthorizationEndpointResponse response = oauth.loginForm().doLogin("test-user@localhost", "password");
        Assertions.assertTrue(response.isRedirected());
        String code = response.getCode();
        Assertions.assertNotNull(code);

        EventAssertion.expectLoginSuccess(events.poll());

        // Tamper with the stored code data so that it is bound to a different user than the one owning the user session
        String codeUUID = code.split("\\.")[0];
        runOnServer.run(session -> {
            SingleUseObjectProvider codeStore = session.singleUseObjects();
            Map<String, String> data = codeStore.get(OAuth2CodeParser.CACHE_KEY_PREFIX + codeUUID);
            Map<String, String> modified = new HashMap<>(data);
            modified.put(OAuth2Code.USER_ID_NOTE, "different-user-id");
            codeStore.put(OAuth2CodeParser.CACHE_KEY_PREFIX + codeUUID, 300, modified);
        });

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(code).send();
        Assertions.assertFalse(tokenResponse.isSuccess());
        assertEquals(INVALID_GRANT, tokenResponse.getError());
        EventAssertion.assertError(events.poll()).type(EventType.CODE_TO_TOKEN_ERROR).error(Errors.INVALID_CODE);
    }

    @Test
    public void forgedCodeDoesNotDetachClientSessionOfAnotherClient() {
        String redirectUri = oauth.getRedirectUri();

        ClientRepresentation clientRep = ClientConfigBuilder.create()
                .clientId("attacker-client")
                .secret("secret")
                .redirectUris(redirectUri)
                .build();
        try (Response resp = managedRealm.admin().clients().create(clientRep)) {
            managedRealm.cleanup().add(r -> r.clients().delete(ApiUtil.getCreatedId(resp)));

            // The victim client logs in and exchanges its code for tokens
            AuthorizationEndpointResponse victimLogin = oauth
                    .client("test-app", "test-secret")
                    .redirectUri(redirectUri)
                    .loginForm()
                    .doLogin("test-user@localhost", "password");
            AccessTokenResponse victimTokens = oauth.accessTokenRequest(victimLogin.getCode()).send();
            Assertions.assertTrue(victimTokens.isSuccess());

            String victimClientUuid = managedRealm.admin()
                    .clients()
                    .findByClientId("test-app")
                    .get(0)
                    .getId();
            String forgedCode = "forged-code." + victimLogin.getSessionState() + "." + victimClientUuid;

            events.clear();

            // The attacker client sends a forged code pointing at the victim's client session
            AccessTokenResponse tokenResponse = oauth
                    .client("attacker-client", "secret")
                    .redirectUri(redirectUri)
                    .accessTokenRequest(forgedCode)
                    .send();

            Assertions.assertFalse(tokenResponse.isSuccess());
            assertEquals(INVALID_GRANT, tokenResponse.getError());
            EventAssertion.assertError(events.poll()).type(EventType.CODE_TO_TOKEN_ERROR).error(Errors.INVALID_CODE);

            // The victim client session must not have been detached, so its refresh token still works
            AccessTokenResponse refreshResponse = oauth
                    .client("test-app", "test-secret")
                    .redirectUri(redirectUri)
                    .doRefreshTokenRequest(victimTokens.getRefreshToken());
            Assertions.assertTrue(refreshResponse.isSuccess());
        }
    }

    @Test
    public void reusedCodeStillDetachesOwnClientSession() {
        String redirectUri = oauth.getRedirectUri();

        AuthorizationEndpointResponse login = oauth
                .client("test-app", "test-secret")
                .redirectUri(redirectUri)
                .loginForm()
                .doLogin("test-user@localhost", "password");
        String code = login.getCode();

        AccessTokenResponse tokens = oauth.accessTokenRequest(code).send();
        Assertions.assertTrue(tokens.isSuccess());

        events.clear();

        // Reusing the own code detaches the client session, so the previously issued refresh token stops working
        AccessTokenResponse reuseResponse = oauth.accessTokenRequest(code).send();
        Assertions.assertFalse(reuseResponse.isSuccess());
        assertEquals(INVALID_GRANT, reuseResponse.getError());

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(tokens.getRefreshToken());
        Assertions.assertFalse(refreshResponse.isSuccess());
    }

    private static class AuthzCodeRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realmBuilder) {
            RealmConfigBuilder realmCfgBuilder = realmBuilder.name("test");
            realmCfgBuilder.addUser(UserConfigBuilder.create().username("test-user@localhost")
                            .name("test", "user")
                            .email("test-user@localhost")
                            .emailVerified(true)
                            .password("password")
                            .roles("user", "offline_access").build());
            return realmCfgBuilder;
        }
    }

}
