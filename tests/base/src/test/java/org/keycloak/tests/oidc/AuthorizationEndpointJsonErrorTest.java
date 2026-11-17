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

package org.keycloak.tests.oidc;

import java.util.List;
import java.util.stream.Stream;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ClientData;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.ErrorPageException;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.AuthenticationExecutionExportBuilder;
import org.keycloak.testframework.realm.AuthenticationFlowBuilder;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.util.JsonSerialization;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class AuthorizationEndpointJsonErrorTest {

    @InjectRealm(config = TestRealm.class)
    ManagedRealm realm;

    @InjectKeycloakUrls
    KeycloakUrls urls;

    @InjectHttpClient(followRedirects = false)
    CloseableHttpClient httpClient;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    static Stream<Arguments> errors() {
        return Stream.of(
                List.of("client_id=missing", 400, "invalid_client", "Client not found."),
                List.of("client_id=disabled", 400, "invalid_client", "Client disabled."),
                List.of("client_id=bearer", 403, "unauthorized_client", "Bearer-only applications are not allowed to initiate browser login"),
                List.of("client_id=saml", 400, "unauthorized_client", "Wrong client protocol."),
                List.of("", 400, "invalid_request", "Invalid Request"),
                List.of("client_id=valid&client_id=valid", 400, "invalid_request", "Invalid Request"),
                List.of("client_id=valid&redirect_uri=https://unregistered.example/cb", 400, "invalid_request", "Invalid parameter: redirect_uri"),
                List.of("client_id=valid&request=not-a-jwt", 400, "invalid_request", "Invalid Request"),
                List.of("client_id=valid&claims=%7B%22id_token%22%3A%7B%22acr%22%3A%7B%22essential%22%3Atrue%2C%22values%22%3A%5B%22not-a-level%22%5D%7D%7D%7D", 400, "invalid_request", "Invalid parameter: claims")
        ).flatMap(test -> Stream.of(false, true).map(post -> Arguments.of(test.get(0), test.get(1), test.get(2), test.get(3), post)));
    }

    @ParameterizedTest
    @MethodSource("errors")
    public void jsonErrors(String query, int status, String error, String description, boolean post) throws Exception {
        try (CloseableHttpResponse response = request(query, "application/json", "en", post)) {
            assertEquals(status, response.getStatusLine().getStatusCode());
            assertNull(response.getFirstHeader("Location"));
            assertTrue(response.getEntity().getContentType().getValue().startsWith("application/json"));
            OAuth2ErrorRepresentation body = JsonSerialization.readValue(EntityUtils.toString(response.getEntity()), OAuth2ErrorRepresentation.class);
            assertEquals(error, body.getError());
            assertEquals(description, body.getErrorDescription());
        }
    }

    @ParameterizedTest
    @MethodSource("errors")
    public void htmlErrors(String query, int status, String error, String description, boolean post) throws Exception {
        try (CloseableHttpResponse response = request(query, "text/html", "en", post)) {
            assertEquals(status, response.getStatusLine().getStatusCode());
            assertNull(response.getFirstHeader("Location"));
            assertTrue(response.getEntity().getContentType().getValue().startsWith("text/html"));
            assertTrue(EntityUtils.toString(response.getEntity()).contains(description));
        }
    }

    @Test
    public void localizedDescriptionKeepsStableCode() throws Exception {
        try (CloseableHttpResponse response = request("client_id=missing", "application/json", "de", false)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
            OAuth2ErrorRepresentation body = JsonSerialization.readValue(EntityUtils.toString(response.getEntity()), OAuth2ErrorRepresentation.class);
            assertEquals("invalid_client", body.getError());
            assertNotNull(body.getErrorDescription());
            assertNotEquals("Client not found.", body.getErrorDescription());
        }
    }

    @Test
    public void verifiedRedirectStillReceivesOAuthError() throws Exception {
        try (CloseableHttpResponse response = request("client_id=valid&response_type=invalid", "application/json", "en", false)) {
            assertEquals(302, response.getStatusLine().getStatusCode());
            String location = response.getFirstHeader("Location").getValue();
            assertTrue(location.startsWith("https://client.example/cb"));
            assertTrue(location.contains("error=unsupported_response_type"));
        }
    }

    @Test
    public void directGrantErrorRetainsLegacyFormat() throws Exception {
        HttpPost request = new HttpPost(urls.getBase() + "/realms/" + realm.getName() + "/protocol/openid-connect/token");
        request.setHeader("Accept", "application/json");
        request.setEntity(new UrlEncodedFormEntity(List.of(
                new BasicNameValuePair("client_id", "valid"),
                new BasicNameValuePair("grant_type", "password"),
                new BasicNameValuePair("username", "test"),
                new BasicNameValuePair("password", "test"))));
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            assertEquals(401, response.getStatusLine().getStatusCode());
            OAuth2ErrorRepresentation body = JsonSerialization.readValue(EntityUtils.toString(response.getEntity()), OAuth2ErrorRepresentation.class);
            assertEquals("Access denied", body.getError());
            assertNull(body.getErrorDescription());
        }
    }

    @Test
    public void unsupportedResponseTypeWithoutDescriptionRendersErrorPage() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = realm.getClientByClientId("valid");
            session.getContext().setClient(client);
            OIDCLoginProtocol protocol = new OIDCLoginProtocol(session, realm, session.getContext().getUri(),
                    session.getContext().getRequestHeaders(), new EventBuilder(realm, session).event(EventType.LOGIN));
            ClientData clientData = new ClientData("https://client.example/cb", "invalid", null, null);

            ErrorPageException exception = assertThrows(ErrorPageException.class,
                    () -> protocol.sendError(client, clientData, LoginProtocol.Error.CANCELLED_BY_USER));
            try (Response response = exception.getResponse()) {
                assertEquals(400, response.getStatus());
                assertTrue(response.getMediaType().isCompatible(MediaType.TEXT_HTML_TYPE));
                assertTrue(response.getEntity().toString().contains("unsupported_response_type"));
            }
        });
    }

    private CloseableHttpResponse request(String query, String accept, String locale, boolean post) throws Exception {
        URIBuilder uri = new URIBuilder(urls.getBase() + "/realms/" + realm.getName() + "/protocol/openid-connect/auth?" + query);
        if (uri.getQueryParams().stream().noneMatch(p -> p.getName().equals("redirect_uri"))) {
            uri.addParameter("redirect_uri", "https://client.example/cb");
        }
        if (uri.getQueryParams().stream().noneMatch(p -> p.getName().equals("response_type"))) {
            uri.addParameter("response_type", "code");
        }
        uri.addParameter("scope", "openid");
        HttpRequestBase request;
        if (post) {
            HttpPost form = new HttpPost(new URIBuilder(uri.build()).removeQuery().build());
            form.setEntity(new UrlEncodedFormEntity(uri.getQueryParams()));
            request = form;
        } else {
            request = new HttpGet(uri.build());
        }
        request.setHeader("Accept", accept);
        request.setHeader("Accept-Language", locale);
        return httpClient.execute(request);
    }

    public static class TestRealm implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.build().setDirectGrantFlow("deny-grant");
            realm.authenticationFlows(AuthenticationFlowBuilder.create("deny-grant", "Deny direct grants", "basic-flow", true, false)
                    .authenticationExecutions(AuthenticationExecutionExportBuilder.authenticator("deny-access-authenticator", "REQUIRED", 0, false)));
            return realm.internationalizationEnabled(true).supportedLocales("en", "de").defaultLocale("en")
                    .clients(ClientBuilder.create("valid").publicClient().directAccessGrantsEnabled().protocol("openid-connect").redirectUris("https://client.example/cb"),
                            ClientBuilder.create("disabled").enabled(false).protocol("openid-connect"),
                            ClientBuilder.create("bearer").bearerOnly(true).protocol("openid-connect"),
                            ClientBuilder.create("saml").protocol("saml"));
        }
    }
}
