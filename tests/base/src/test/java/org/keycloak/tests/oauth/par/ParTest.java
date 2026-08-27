/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.oauth.par;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.ParConfig;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.client.policies.AbstractClientPoliciesTest;
import org.keycloak.testsuite.util.oauth.ParResponse;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for Pushed Authorization Requests (PAR) security properties.
 */
@KeycloakIntegrationTest
public class ParTest extends AbstractClientPoliciesTest {

    private static final String CLIENT_REDIRECT_URI1 = "https://localhost:8543/auth/realms/realm-a/app/auth/cb";
    private static final String CLIENT_REDIRECT_URI2 = "https://localhost:8543/auth/realms/realm-b/app/auth/cb";

    /** Primary realm: PAR is issued here. */
    @InjectRealm(ref = "realm-a", config = ParRealmConfig1.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realmA;

    /** Secondary realm: attempts to consume realm-a's PAR request_uri. */
    @InjectRealm(ref = "realm-b", config = ParRealmConfig2.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realmB;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectPage
    ErrorPage errorPage;

    @InjectPage
    LoginPage loginPage;

    /**
     * Test that a PAR {@code request_uri} issued in realm-a is rejected when presented to the authorization
     * endpoint of realm-b, even when realm-b has an identical client (same clientId and secret).
     * <p>
     * Without the realm-scoped cache key fix the PAR entry stored as {@code par:<UUID>} would be found
     * in realm-b's single-use cache lookup, allowing cross-realm consumption.
     */
    @Test
    public void testParCrossRealmRejected() {
        // Issue PAR in realm-a
        oauth.realm("realm-a");
        oauth.client("par-test-client", "par-test-secret");
        oauth.redirectUri(CLIENT_REDIRECT_URI1);
        ParResponse pResp = oauth.doPushedAuthorizationRequest();
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // Attempt to use realm-a's request_uri at realm-b's authorization endpoint
        oauth.realm("realm-b");
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        oauth.loginForm().requestUri(requestUri).open();

        // Must show an error page — realm-b must not accept a PAR from realm-a
        assertThat(driver.getCurrentUrl(),
                not(containsString("/login-actions/authenticate")));
        errorPage.assertCurrent();

        // Try again with correct realm. Login page should be shown after PAR data successfully consumed
        oauth.realm("realm-a");
        oauth.loginForm().requestUri(requestUri).open();
        assertThat(driver.getCurrentUrl(), startsWith(realmA.getBaseUrl() + "/login-actions/authenticate"));
        loginPage.assertCurrent();
    }

    /**
     * Test that a PAR {@code request_uri} issued for client-A is rejected when client-B presents it at
     * the authorization endpoint.  Without the {@code par.client.id} validation the server would silently
     * accept the request, letting client-B start a flow with client-A's authorization parameters.
     */
    @Test
    public void testParClientIdMismatchRejected() {
        // Create a second client in realm-a with the same redirect_uri but a different clientId
        String clientBId = "par-client-b";
        String clientBSecret = "par-secret-b";
        try (Response resp = realmA.admin().clients().create(
                ClientBuilder.create(clientBId)
                        .secret(clientBSecret)
                        .redirectUris(CLIENT_REDIRECT_URI1)
                        .build())) {
            assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
        }

        // Issue PAR as client-A
        oauth.realm("realm-a");
        oauth.client("par-test-client", "par-test-secret");
        oauth.redirectUri(CLIENT_REDIRECT_URI1);
        ParResponse pResp = oauth.doPushedAuthorizationRequest();
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // Switch to client-B and attempt to use client-A's request_uri
        oauth.client(clientBId, clientBSecret);
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        oauth.loginForm().requestUri(requestUri).open();

        // Must show an error page — client-B must not consume client-A's PAR
        assertThat(driver.getCurrentUrl(),
                not(startsWith(realmA.getBaseUrl() + "/login-actions/authenticate")));
        errorPage.assertCurrent();
    }

    // -------------------------------------------------------------------------
    // Realm / client configurations
    // -------------------------------------------------------------------------

    /** Both test realms use the same config: PAR enabled, short lifespan, one pre-registered client.Only difference is client redirect URI */
    public static class ParRealmConfig1 implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .attribute(ParConfig.PAR_REQUEST_URI_LIFESPAN, "45")
                    .clients(ClientBuilder.create("par-test-client")
                            .secret("par-test-secret")
                            .redirectUris(CLIENT_REDIRECT_URI1));
        }
    }

    public static class ParRealmConfig2 implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .attribute(ParConfig.PAR_REQUEST_URI_LIFESPAN, "45")
                    .clients(ClientBuilder.create("par-test-client")
                            .secret("par-test-secret")
                            .redirectUris(CLIENT_REDIRECT_URI2));
        }
    }
}
