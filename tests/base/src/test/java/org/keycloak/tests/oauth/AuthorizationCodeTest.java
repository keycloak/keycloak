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


import jakarta.ws.rs.core.Response;

import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
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
