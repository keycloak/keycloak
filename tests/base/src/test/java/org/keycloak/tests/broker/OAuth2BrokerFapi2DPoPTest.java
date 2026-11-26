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
package org.keycloak.tests.broker;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oauth.OAuth2IdentityProviderFactory;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.common.util.Base64Url;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.util.HttpServerUtil;
import org.keycloak.util.JsonSerialization;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Brokers to a fake OAuth 2.0 identity provider that asks for DPoP nonces or changes the state.
 */
@KeycloakIntegrationTest
public class OAuth2BrokerFapi2DPoPTest {

    private static final String IDP_ALIAS = "fake-idp";
    private static final String IDP_BASE_URL = "http://127.0.0.1:8500/fake-idp";
    private static final String DPOP_NONCE = "nonce-from-the-server";
    private static final String NEW_DPOP_NONCE = "new-nonce-from-the-server";
    private static final String USER_INFO_DPOP_NONCE = "nonce-from-the-user-info-endpoint";
    private static final String ACCESS_TOKEN = "fake-access-token";

    @InjectRealm(lifecycle = LifeCycle.METHOD, config = ConsumerRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    ErrorPage errorPage;

    @InjectHttpServer
    HttpServer httpServer;

    private FakeIdentityProvider idp;

    @AfterEach
    void removeFakeIdentityProvider() {
        if (idp != null) {
            idp.remove();
        }
    }

    @Test
    public void testNoncesAreUsedUntilTheServerSuppliesNewOnes() {
        idp = new FakeIdentityProvider(httpServer, "DPoP");

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);

        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());
        // the request is sent again with the nonce the server asks for
        Assertions.assertEquals(Arrays.asList(null, DPOP_NONCE), idp.proofNonces.get("/par"));
        // the next token request uses the nonce supplied with the pushed authorization response
        Assertions.assertEquals(List.of(NEW_DPOP_NONCE), idp.proofNonces.get("/token"));
        // the user info endpoint has a nonce of its own
        Assertions.assertEquals(Arrays.asList(null, USER_INFO_DPOP_NONCE), idp.proofNonces.get("/userinfo"));
        Assertions.assertEquals("DPoP " + ACCESS_TOKEN, idp.userInfoAuthorization);
    }

    @Test
    public void testBearerTokenIsRejectedWhenDPoPIsEnabled() {
        idp = new FakeIdentityProvider(httpServer, "Bearer");

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);

        errorPage.assertCurrent();
        Assertions.assertNull(idp.proofNonces.get("/userinfo"));
    }

    /**
     * As the conformance test FAPI2SPFinalClientTestEnsureAuthorizationResponseWithInvalidStateFails. The suffix is
     * AAAA instead of 1 to keep the state valid base64.
     */
    @Test
    public void testAuthorizationResponseWithAnotherStateIsRejected() {
        idp = new FakeIdentityProvider(httpServer, "DPoP");
        idp.stateSuffix = "AAAA";

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);

        errorPage.assertCurrent();
        Assertions.assertNull(idp.proofNonces.get("/token"));
    }

    private static class FakeIdentityProvider {

        private final HttpServer httpServer;
        private final String tokenType;
        private final Map<String, List<String>> proofNonces = new HashMap<>();
        private Map<String, String> pushedRequest;
        private String userInfoAuthorization;
        private String stateSuffix = "";
        private String authorizationServerNonce = DPOP_NONCE;

        FakeIdentityProvider(HttpServer httpServer, String tokenType) {
            this.httpServer = httpServer;
            this.tokenType = tokenType;
            httpServer.createContext("/fake-idp/par", this::par);
            httpServer.createContext("/fake-idp/auth", this::authorize);
            httpServer.createContext("/fake-idp/token", this::token);
            httpServer.createContext("/fake-idp/userinfo", this::userInfo);
        }

        void remove() {
            for (String path : List.of("/par", "/auth", "/token", "/userinfo")) {
                httpServer.removeContext("/fake-idp" + path);
            }
        }

        private void par(HttpExchange exchange) throws IOException {
            Map<String, String> parameters = parseForm(exchange);
            if (askForNonce(exchange, "/par", false)) {
                return;
            }
            pushedRequest = parameters;
            authorizationServerNonce = NEW_DPOP_NONCE;
            HttpServerUtil.sendResponse(exchange, 201, Map.of("Content-Type", List.of("application/json"),
                            OAuth2Constants.DPOP_NONCE_HEADER, List.of(NEW_DPOP_NONCE)),
                    JsonSerialization.writeValueAsString(Map.of("request_uri", "urn:fake:request", "expires_in", 60)));
        }

        private void authorize(HttpExchange exchange) {
            String redirect = pushedRequest.get(OAuth2Constants.REDIRECT_URI)
                    + "?code=fake-code"
                    + "&state=" + URLEncoder.encode(pushedRequest.get(OAuth2Constants.STATE) + stateSuffix, StandardCharsets.UTF_8)
                    + "&iss=" + URLEncoder.encode(IDP_BASE_URL, StandardCharsets.UTF_8);
            HttpServerUtil.sendResponse(exchange, 302, Map.of("Location", List.of(redirect)));
        }

        private void token(HttpExchange exchange) throws IOException {
            parseForm(exchange);
            if (askForNonce(exchange, "/token", false)) {
                return;
            }
            json(exchange, 200, Map.of("access_token", ACCESS_TOKEN, "token_type", tokenType, "expires_in", 300));
        }

        private void userInfo(HttpExchange exchange) throws IOException {
            if (askForNonce(exchange, "/userinfo", true)) {
                return;
            }
            userInfoAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
            json(exchange, 200, Map.of("sub", "fake-user", "preferred_username", "fake-user",
                    "email", "fake-user@localhost", "given_name", "Fake", "family_name", "User"));
        }

        private boolean askForNonce(HttpExchange exchange, String endpoint, boolean resourceServer) throws IOException {
            String nonce = proofNonce(exchange.getRequestHeaders().getFirst(OAuth2Constants.DPOP_HTTP_HEADER));
            proofNonces.computeIfAbsent(endpoint, e -> new ArrayList<>()).add(nonce);
            String expectedNonce = resourceServer ? USER_INFO_DPOP_NONCE : authorizationServerNonce;
            if (expectedNonce.equals(nonce)) {
                return false;
            }
            Map<String, List<String>> headers = new HashMap<>();
            headers.put(OAuth2Constants.DPOP_NONCE_HEADER, List.of(expectedNonce));
            if (resourceServer) {
                headers.put("WWW-Authenticate", List.of("DPoP error=\"use_dpop_nonce\""));
                HttpServerUtil.sendResponse(exchange, 401, headers);
            } else {
                headers.put("Content-Type", List.of("application/json"));
                HttpServerUtil.sendResponse(exchange, 400, headers, "{\"error\":\"use_dpop_nonce\"}");
            }
            return true;
        }

        private static String proofNonce(String proof) throws IOException {
            Assertions.assertNotNull(proof, "The request has no DPoP proof");
            Map<?, ?> claims = JsonSerialization.readValue(Base64Url.decode(proof.split("\\.")[1]), Map.class);
            return (String) claims.get("nonce");
        }

        private static Map<String, String> parseForm(HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            return Arrays.stream(body.split("&"))
                    .map(parameter -> parameter.split("=", 2))
                    .collect(Collectors.toMap(parameter -> URLDecoder.decode(parameter[0], StandardCharsets.UTF_8),
                            parameter -> parameter.length > 1 ? URLDecoder.decode(parameter[1], StandardCharsets.UTF_8) : "",
                            (first, second) -> first));
        }

        private static void json(HttpExchange exchange, int status, Map<String, ?> body) throws IOException {
            HttpServerUtil.sendResponse(exchange, status, Map.of("Content-Type", List.of("application/json")),
                    JsonSerialization.writeValueAsString(body));
        }
    }

    static class ConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.identityProviders(IdentityProviderBuilder.create()
                    .providerId(OAuth2IdentityProviderFactory.PROVIDER_ID)
                    .alias(IDP_ALIAS)
                    .attribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                    .attribute("clientId", "fake-client")
                    .attribute("clientSecret", "fake-secret")
                    .attribute("authorizationUrl", IDP_BASE_URL + "/auth")
                    .attribute(OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL, IDP_BASE_URL + "/token")
                    .attribute("userInfoUrl", IDP_BASE_URL + "/userinfo")
                    .attribute(OAuth2IdentityProviderConfig.ISSUER, IDP_BASE_URL)
                    .attribute(OAuth2IdentityProviderConfig.PAR_ENABLED, "true")
                    .attribute(OAuth2IdentityProviderConfig.PAR_URL, IDP_BASE_URL + "/par")
                    .attribute(OAuth2IdentityProviderConfig.DPOP_ENABLED, "true")
                    .attribute(OAuth2IdentityProviderConfig.DPOP_SIGNING_ALG, Algorithm.RS256)
                    .attribute(OAuth2IdentityProviderConfig.AUTHORIZATION_RESPONSE_ISSUER_REQUIRED, "true")
                    .build());
        }
    }
}
