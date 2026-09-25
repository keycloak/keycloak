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

package org.keycloak.tests.admin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.Test;

import static jakarta.ws.rs.core.HttpHeaders.WWW_AUTHENTICATE;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@KeycloakIntegrationTest
public class AdminBearerChallengeTest {

    private static final String USERNAME = "realm-admin";
    private static final String PASSWORD = "password";

    private static final String UNAUTHORIZED_ERROR = "HTTP 401 Unauthorized";

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @InjectRealm(config = AdminAuthRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectClient(config = PasswordGrantClientConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedClient testClient;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @Test
    public void adminRestNoToken() throws IOException {
        try (SimpleHttpResponse response = adminGet(null)) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE), is("Bearer"));
            assertErrorBody(response);
        }
    }

    @Test
    public void adminRestGarbageToken() throws IOException {
        try (SimpleHttpResponse response = adminGet("not-a-jwt")) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE),
                    is("Bearer error=\"invalid_token\", error_description=\"Token verification failed\""));
            assertErrorBody(response, "Token verification failed");
        }
    }

    @Test
    public void adminRestUnknownRealm() throws IOException {
        // Realm lookup happens before signature verification, so invalid token is enough.
        String header = Base64Url.encode("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64Url.encode(("{\"iss\":\"" + keycloakUrls.getBase() + "/realms/nonexistent\"}").getBytes(StandardCharsets.UTF_8));
        String token = String.join(".", header, payload, "AAAA");

        try (SimpleHttpResponse response = adminGet(token)) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE),
                    is("Bearer error=\"invalid_token\", error_description=\"Token verification failed\""));
            assertErrorBody(response, "Token verification failed");
        }
    }

    @Test
    public void adminRestInvalidSignature() throws IOException {
        String[] jwtParts = oauth.doPasswordGrantRequest(USERNAME, PASSWORD).getAccessToken().split("\\.");
        String header = jwtParts[0];
        String payload = jwtParts[1];
        String signature = jwtParts[2];
        String tamperedSignature = "A".repeat(signature.length());
        String tokenWithBadSignature = String.join(".", header, payload, tamperedSignature);

        try (SimpleHttpResponse response = adminGet(tokenWithBadSignature)) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE),
                    is("Bearer realm=\"" + realm.getName() + "\", error=\"invalid_token\", error_description=\"Token signature invalid\""));
            assertErrorBody(response, "Token signature invalid");
        }
    }

    @Test
    public void adminRestExpiredToken() throws IOException {
        String token = oauth.doPasswordGrantRequest(USERNAME, PASSWORD).getAccessToken();
        timeOffSet.set(600);

        try (SimpleHttpResponse response = adminGet(token)) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE),
                    is("Bearer realm=\"" + realm.getName() + "\", error=\"invalid_token\", error_description=\"Token outside validity period\""));
            assertErrorBody(response, "Token outside validity period");
        }
    }

    @Test
    public void adminRestRealmNotBefore() throws IOException {
        String token = oauth.doPasswordGrantRequest(USERNAME, PASSWORD).getAccessToken();
        realm.updateWithCleanup(r -> r.notBefore(Math.toIntExact(Time.currentTimeSeconds() + 60)));

        try (SimpleHttpResponse response = adminGet(token)) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE),
                    is("Bearer realm=\"" + realm.getName() + "\", error=\"invalid_token\", error_description=\"Token outside validity period\""));
            assertErrorBody(response, "Token outside validity period");
        }
    }

    @Test
    public void adminRestValidToken() throws IOException {
        try (SimpleHttpResponse response = adminGet(oauth.doPasswordGrantRequest(USERNAME, PASSWORD).getAccessToken())) {
            assertThat(response.getStatus(), is(200));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE), is(nullValue()));
        }
    }

    @Test
    public void adminRestNonBearerAuthScheme() throws IOException {
        String url = keycloakUrls.getAdmin() + "/realms/" + realm.getName() + "/clients?max=1";
        try (SimpleHttpResponse response = simpleHttp.doGet(url).header("Authorization", "Basic dXNlcjpwYXNz").asResponse()) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE), is("Bearer"));
            assertErrorBody(response);
        }
    }

    @Test
    public void adminRestEmptyBearerToken() throws IOException {
        String url = keycloakUrls.getAdmin() + "/realms/" + realm.getName() + "/clients?max=1";
        try (SimpleHttpResponse response = simpleHttp.doGet(url).header("Authorization", "Bearer ").asResponse()) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE), is("Bearer"));
            assertErrorBody(response);
        }
    }

    @Test
    public void adminRestDeletedClient() throws IOException {
        String token = oauth.client(testClient.getClientId(), testClient.getSecret())
                .doPasswordGrantRequest(USERNAME, PASSWORD).getAccessToken();
        assertThat(adminStatus(token), is(200));

        testClient.admin().remove();

        try (SimpleHttpResponse response = adminGet(token)) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE),
                    is("Bearer realm=\"" + realm.getName() + "\", error=\"invalid_token\", error_description=\"Client not found\""));
            assertErrorBody(response, "Client not found");
        }
    }

    @Test
    public void adminRestDisabledClient() throws IOException {
        String token = oauth.client(testClient.getClientId(), testClient.getSecret())
            .doPasswordGrantRequest(USERNAME, PASSWORD).getAccessToken();
        assertThat(adminStatus(token), is(200));

        ClientResource client = testClient.admin();
        ClientRepresentation rep = client.toRepresentation();
        rep.setEnabled(false);
        client.update(rep);

        try (SimpleHttpResponse response = adminGet(token)) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE),
                    is("Bearer realm=\"" + realm.getName() + "\", error=\"invalid_token\", error_description=\"Token verification failed\""));
            assertErrorBody(response, "Token verification failed");
        }
    }

    @Test
    public void adminRestDisabledUser() throws IOException {
        String token = oauth.client(testClient.getClientId(), testClient.getSecret())
            .doPasswordGrantRequest(USERNAME, PASSWORD).getAccessToken();
        assertThat(adminStatus(token), is(200));

        realm.updateUserWithCleanup(USERNAME, user -> user.enabled(false));

        // A disabled user causes a VerificationException, which produces a generic error description.
        // TODO: consider providing a more specific error description for disabled users.
        try (SimpleHttpResponse response = adminGet(token)) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE),
                    is("Bearer realm=\"" + realm.getName() + "\", error=\"invalid_token\", error_description=\"Token verification failed\""));
            assertErrorBody(response, "Token verification failed");
        }
    }

    @Test
    public void adminRestLoggedOutSession() throws IOException {
        String token = oauth.doPasswordGrantRequest(USERNAME, PASSWORD).getAccessToken();
        assertThat(adminStatus(token), is(200));

        AccountHelper.logout(realm.admin(), USERNAME);

        // A missing user session causes a VerificationException, which produces a generic error description.
        // TODO: consider providing a more specific error description for missing user sessions.
        try (SimpleHttpResponse response = adminGet(token)) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE),
                    is("Bearer realm=\"" + realm.getName() + "\", error=\"invalid_token\", error_description=\"Token verification failed\""));
            assertErrorBody(response, "Token verification failed");
        }
    }

    @Test
    public void adminConsoleWhoAmINoToken() throws IOException {
        try (SimpleHttpResponse response = whoamiGet(null)) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE), is("Bearer realm=\"" + realm.getName() + "\""));
            assertErrorBody(response);
        }
    }

    @Test
    public void adminConsoleWhoAmIGarbageToken() throws IOException {
        try (SimpleHttpResponse response = whoamiGet("not-a-jwt")) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE),
                    is("Bearer realm=\"" + realm.getName() + "\", error=\"invalid_token\", error_description=\"Token verification failed\""));
            assertErrorBody(response, "Token verification failed");
        }
    }

    @Test
    public void adminConsoleWhoAmIInvalidSignature() throws IOException {
        String[] jwtParts = oauth.doPasswordGrantRequest(USERNAME, PASSWORD).getAccessToken().split("\\.");
        String header = jwtParts[0];
        String payload = jwtParts[1];
        String signature = jwtParts[2];
        String tamperedSignature = "A".repeat(signature.length());
        String tokenWithBadSignature = String.join(".", header, payload, tamperedSignature);

        try (SimpleHttpResponse response = whoamiGet(tokenWithBadSignature)) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE),
                    is("Bearer realm=\"" + realm.getName() + "\", error=\"invalid_token\", error_description=\"Token signature invalid\""));
            assertErrorBody(response, "Token signature invalid");
        }
    }

    @Test
    public void adminConsoleWhoAmIUnknownRealmToken() throws IOException {
        String header = Base64Url.encode("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64Url.encode(("{\"iss\":\"" + keycloakUrls.getBase() + "/realms/nonexistent\"}").getBytes(StandardCharsets.UTF_8));
        String token = String.join(".", header, payload, "AAAA");

        try (SimpleHttpResponse response = whoamiGet(token)) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE),
                    is("Bearer realm=\"" + realm.getName() + "\", error=\"invalid_token\", error_description=\"Token verification failed\""));
            assertErrorBody(response, "Token verification failed");
        }
    }

    @Test
    public void adminConsoleWhoAmIExpiredToken() throws IOException {
        String token = oauth.doPasswordGrantRequest(USERNAME, PASSWORD).getAccessToken();
        timeOffSet.set(600);

        try (SimpleHttpResponse response = whoamiGet(token)) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE),
                    is("Bearer realm=\"" + realm.getName() + "\", error=\"invalid_token\", error_description=\"Token outside validity period\""));
            assertErrorBody(response, "Token outside validity period");
        }
    }

    @Test
    public void adminConsoleWhoAmIRealmNotBefore() throws IOException {
        String token = oauth.doPasswordGrantRequest(USERNAME, PASSWORD).getAccessToken();
        realm.updateWithCleanup(r -> r.notBefore(Math.toIntExact(Time.currentTimeSeconds() + 60)));

        try (SimpleHttpResponse response = whoamiGet(token)) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE),
                    is("Bearer realm=\"" + realm.getName() + "\", error=\"invalid_token\", error_description=\"Token outside validity period\""));
            assertErrorBody(response, "Token outside validity period");
        }
    }

    @Test
    public void adminConsoleWhoAmINonBearerAuthScheme() throws IOException {
        String url = keycloakUrls.getBase() + "/admin/" + realm.getName() + "/console/whoami";
        try (SimpleHttpResponse response = simpleHttp.doGet(url).header("Authorization", "Basic dXNlcjpwYXNz").asResponse()) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE), is("Bearer realm=\"" + realm.getName() + "\""));
            assertErrorBody(response);
        }
    }

    @Test
    public void adminConsoleWhoAmIEmptyBearerToken() throws IOException {
        String url = keycloakUrls.getBase() + "/admin/" + realm.getName() + "/console/whoami";
        try (SimpleHttpResponse response = simpleHttp.doGet(url).header("Authorization", "Bearer ").asResponse()) {
            assertThat(response.getStatus(), is(401));
            assertThat(response.getFirstHeader(WWW_AUTHENTICATE), is("Bearer realm=\"" + realm.getName() + "\""));
            assertErrorBody(response);
        }
    }

    private void assertErrorBody(SimpleHttpResponse response) throws IOException {
        assertThat(response.asJson(Map.class), is(Map.of("error", UNAUTHORIZED_ERROR)));
    }

    private void assertErrorBody(SimpleHttpResponse response, String errorDescription) throws IOException {
        assertThat(response.asJson(Map.class), is(Map.of("error", UNAUTHORIZED_ERROR, "error_description", errorDescription)));
    }

    private SimpleHttpResponse adminGet(String token) throws IOException {
        String url = keycloakUrls.getAdmin() + "/realms/" + realm.getName() + "/clients?max=1";
        SimpleHttpRequest request = simpleHttp.doGet(url);
        return token != null ? request.auth(token).asResponse() : request.asResponse();
    }

    private int adminStatus(String token) throws IOException {
        String url = keycloakUrls.getAdmin() + "/realms/" + realm.getName() + "/clients?max=1";
        return simpleHttp.doGet(url).auth(token).asStatus();
    }

    private SimpleHttpResponse whoamiGet(String token) throws IOException {
        String url = keycloakUrls.getBase() + "/admin/" + realm.getName() + "/console/whoami";
        SimpleHttpRequest request = simpleHttp.doGet(url);
        return token != null ? request.auth(token).asResponse() : request.asResponse();
    }

    public static class AdminAuthRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.users(UserBuilder.create()
                    .username(USERNAME)
                    .password(PASSWORD)
                    .name("Realm", "Admin")
                    .email("realm-admin@localhost")
                    .emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN));
        }
    }

    public static class PasswordGrantClientConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.secret("test-secret").directAccessGrantsEnabled();
        }
    }
}
