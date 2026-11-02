/*
 * Copyright 2026 Andrzej Świętek
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
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.social.google.GoogleIdentityProviderFactory;
import org.keycloak.testframework.annotations.InjectHttpServer;
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
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.util.HttpServerUtil;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the token request parameters setting of identity providers. The token URL of the OpenID Connect provider points
 * at a forwarding endpoint that records each request before passing it on to the external realm.
 */
@KeycloakIntegrationTest(config = IdentityProviderTokenRequestParametersTest.IdentityBrokeringAPIV2ServerConfig.class)
public class IdentityProviderTokenRequestParametersTest {

    private static final String IDP_ALIAS = "token-request-parameters-idp";
    private static final String EXTERNAL_REALM_NAME = "external-realm";
    private static final String TOKEN_PATH = "/token-request-parameters/token";
    private static final String CONFIGURED_PARAMETERS =
            "[{\"key\":\"response_shape\",\"value\":\"flat&nested=false,list\"},{\"key\":\"tenant\",\"value\":\"acme\"}]";

    private static final String CLIENT_SESSION_STATE_PARAMETER = "[{\"key\":\"client_session_state\",\"value\":\"configured\"}]";

    private static final HttpClient FORWARDING_CLIENT = HttpClient.newHttpClient();

    @InjectRealm(config = ConsumerRealmConfig.class)
    ManagedRealm realm;

    @InjectRealm(ref = "external-realm", config = ExternalRealmConfig.class)
    ManagedRealm externalRealm;

    @InjectOAuthClient(ref = "external-realm", realmRef = "external-realm", config = ExternalRealmClientConfig.class)
    OAuthClient oauthExternal;

    @InjectOAuthClient(config = ConsumerClientConfig.class)
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectHttpServer
    HttpServer httpServer;

    private final List<Map<String, String>> tokenRequests = new CopyOnWriteArrayList<>();

    @BeforeEach
    void forwardTokenRequests() {
        URI externalTokenEndpoint = URI.create(externalRealm.getBaseUrl() + "/protocol/openid-connect/token");
        httpServer.createContext(TOKEN_PATH, exchange -> recordAndForward(exchange, externalTokenEndpoint));
    }

    @AfterEach
    void cleanup() {
        httpServer.removeContext(TOKEN_PATH);
        tokenRequests.clear();
        timeOffSet.set(0);
        realm.admin().users().search("testuser", true).stream().findFirst().map(UserRepresentation::getId)
                .ifPresent(this::logoutAndDeleteConsumerUser);
        AccountHelper.logout(externalRealm.admin(), "testuser");
    }

    private void logoutAndDeleteConsumerUser(String id) {
        AccountHelper.logout(realm.admin(), "testuser");
        realm.admin().users().delete(id).close();
    }

    @Test
    public void parametersSentOnCodeExchangeAndRefresh() {
        AccessTokenResponse internalTokens = loginWithIdP();

        assertThat(tokenRequest(OAuth2Constants.AUTHORIZATION_CODE), allOf(
                hasEntry("response_shape", "flat&nested=false,list"),
                hasEntry("tenant", "acme"),
                hasEntry(OAuth2Constants.CLIENT_ID, "test-app-external-realm")));

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpTokenPost(IDP_ALIAS, internalTokens.getAccessToken());
        assertThat(externalTokens.getStatusCode(), is(200));
        timeOffSet.set(externalTokens.getExpiresIn() - IdentityProviderModel.DEFAULT_MIN_VALIDITY_TOKEN + 1);
        AccessTokenResponse refreshedInternalTokens = oauth.doRefreshTokenRequest(internalTokens.getRefreshToken());
        AccessTokenResponse refreshedExternalTokens = oauth.doFetchExternalIdpTokenPost(IDP_ALIAS, refreshedInternalTokens.getAccessToken());
        assertThat(refreshedExternalTokens.getStatusCode(), is(200));

        assertThat(tokenRequest(OAuth2Constants.REFRESH_TOKEN), allOf(
                hasEntry("response_shape", "flat&nested=false,list"),
                hasEntry("tenant", "acme"),
                hasEntry(OAuth2Constants.CLIENT_ID, "test-app-external-realm")));
    }

    @Test
    public void nothingAddedWhenNotConfigured() {
        realm.updateIdentityProvider(IDP_ALIAS, idp -> idp.getConfig().remove(OAuth2IdentityProviderConfig.TOKEN_REQUEST_PARAMETERS));

        loginWithIdP();

        assertThat(tokenRequest(OAuth2Constants.AUTHORIZATION_CODE).keySet(), containsInAnyOrder(
                OAuth2Constants.CODE, OAuth2Constants.REDIRECT_URI, OAuth2Constants.GRANT_TYPE,
                OAuth2Constants.CLIENT_ID, OAuth2Constants.CLIENT_SECRET));
    }

    @Test
    public void reservedNameRefused() {
        assertThat(saveRefusal("[{\"key\":\"client_id\",\"value\":\"other-client\"}]"),
                equalTo("Invalid token request parameters: 'client_id' is set by Keycloak and cannot be configured"));
    }

    @Test
    public void blankAndDuplicateNamesRefused() {
        assertThat(saveRefusal("[{\"key\":\" \",\"value\":\"a\"},{\"key\":\"tenant\",\"value\":\"acme\"},{\"key\":\"tenant\",\"value\":\"other\"}]"),
                equalTo("Invalid token request parameters: a parameter has a blank name; 'tenant' is configured more than once"));
        assertThat(realm.admin().identityProviders().get(IDP_ALIAS).toRepresentation().getConfig(),
                hasEntry(OAuth2IdentityProviderConfig.TOKEN_REQUEST_PARAMETERS, CONFIGURED_PARAMETERS));
    }

    @Test
    public void namesWithSurroundingWhitespaceRefused() {
        assertThat(saveRefusal("[{\"key\":\" tenant\",\"value\":\"acme\"},{\"key\":\"region\\u00A0\",\"value\":\"eu\"}]"),
                equalTo("Invalid token request parameters: ' tenant' has leading or trailing whitespace; 'region ' has leading or trailing whitespace"));
    }

    @Test
    public void controlCharactersAndQuotesEscapedInRefusal() {
        assertThat(saveRefusal("[{\"key\":\"x\\u001B[31m\\n\",\"value\":\"a\"},{\"key\":\"it's\",\"value\":\"a\"},{\"key\":\"it's\",\"value\":\"b\"}]"),
                equalTo("Invalid token request parameters: 'it\\'s' is configured more than once; 'x ' has leading or trailing whitespace"));
    }

    @Test
    public void malformedSettingRefused() {
        String malformed = "Invalid token request parameters: the value is not a list of name/value pairs with text names and values";
        assertThat(saveRefusal("tenant=acme"), equalTo(malformed));
        assertThat(saveRefusal("[{\"key\":\"tenant\",\"value\":null}]"), equalTo(malformed));
        assertThat(saveRefusal("[{\"key\":\"tenant\",\"value\":\"acme\"}] []"), equalTo(malformed));
        assertThat(saveRefusal("[{\"key\":\"tenant\",\"value\":5}]"), equalTo(malformed));
        assertThat(saveRefusal("[{\"key\":\"tenant\",\"value\":true}]"), equalTo(malformed));
        assertThat(saveRefusal(" "), equalTo(malformed));
        assertThat(saveRefusal("\n"), equalTo(malformed));
        assertThat(saveRefusal("\u00A0"), equalTo(malformed));
    }

    @Test
    public void emptyValueSent() {
        realm.updateIdentityProvider(IDP_ALIAS, idp -> idp.getConfig().put(OAuth2IdentityProviderConfig.TOKEN_REQUEST_PARAMETERS,
                "[{\"key\":\"tenant\",\"value\":\"\"}]"));

        loginWithIdP();

        assertThat(tokenRequest(OAuth2Constants.AUTHORIZATION_CODE), hasEntry("tenant", ""));
    }

    @Test
    public void clientSessionStateAllowedForOpenIdConnect() {
        realm.updateIdentityProvider(IDP_ALIAS,
                idp -> idp.getConfig().put(OAuth2IdentityProviderConfig.TOKEN_REQUEST_PARAMETERS, CLIENT_SESSION_STATE_PARAMETER));

        assertThat(realm.admin().identityProviders().get(IDP_ALIAS).toRepresentation().getConfig(),
                hasEntry(OAuth2IdentityProviderConfig.TOKEN_REQUEST_PARAMETERS, CLIENT_SESSION_STATE_PARAMETER));
    }

    @Test
    public void clientSessionStateRefusedForKeycloakOpenIdConnect() {
        assertThat(creationRefusal(KeycloakOIDCIdentityProviderFactory.PROVIDER_ID, CLIENT_SESSION_STATE_PARAMETER), equalTo(
                "Invalid token request parameters: 'client_session_state' is set by Keycloak and cannot be configured"));
    }

    @Test
    public void reservedNameRefusedForGoogle() {
        assertThat(creationRefusal(GoogleIdentityProviderFactory.PROVIDER_ID, "[{\"key\":\"client_id\",\"value\":\"other-client\"}]"),
                equalTo("Invalid token request parameters: 'client_id' is set by Keycloak and cannot be configured"));
    }

    private AccessTokenResponse loginWithIdP() {
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();
        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        assertThat(internalTokens.getStatusCode(), is(200));
        return internalTokens;
    }

    private Map<String, String> tokenRequest(String grantType) {
        List<Map<String, String>> matching = tokenRequests.stream()
                .filter(parameters -> grantType.equals(parameters.get(OAuth2Constants.GRANT_TYPE)))
                .toList();
        assertThat(matching, hasSize(1));
        return matching.get(0);
    }

    private String saveRefusal(String tokenRequestParameters) {
        IdentityProviderResource resource = realm.admin().identityProviders().get(IDP_ALIAS);
        IdentityProviderRepresentation original = resource.toRepresentation();
        realm.cleanup().add(r -> r.identityProviders().get(IDP_ALIAS).update(original));
        IdentityProviderRepresentation idp = resource.toRepresentation();
        idp.getConfig().put(OAuth2IdentityProviderConfig.TOKEN_REQUEST_PARAMETERS, tokenRequestParameters);
        BadRequestException refused = assertThrows(BadRequestException.class, () -> resource.update(idp));
        return refused.getResponse().readEntity(ErrorRepresentation.class).getErrorMessage();
    }

    private String creationRefusal(String providerId, String tokenRequestParameters) {
        IdentityProviderRepresentation idp = IdentityProviderBuilder.create()
                .providerId(providerId)
                .alias(providerId + "-token-request-parameters-idp")
                .attribute("clientId", "test-app-external-realm")
                .attribute("clientSecret", "test-secret")
                .attribute(OAuth2IdentityProviderConfig.TOKEN_REQUEST_PARAMETERS, tokenRequestParameters)
                .build();
        try (Response response = realm.admin().identityProviders().create(idp)) {
            if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
                realm.cleanup().add(r -> r.identityProviders().get(idp.getAlias()).remove());
            }
            assertThat(response.getStatus(), is(400));
            return response.readEntity(ErrorRepresentation.class).getErrorMessage();
        }
    }

    private void recordAndForward(HttpExchange exchange, URI externalTokenEndpoint) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        tokenRequests.add(formParameters(new String(body, StandardCharsets.UTF_8)));
        HttpResponse<byte[]> response = send(forwardedRequest(externalTokenEndpoint, exchange.getRequestHeaders(), body));
        HttpServerUtil.sendResponse(exchange, response.statusCode(),
                Map.of(HttpHeaders.CONTENT_TYPE, List.of("application/json")), response.body());
    }

    private static HttpRequest forwardedRequest(URI externalTokenEndpoint, Headers headers, byte[] body) {
        HttpRequest.Builder forwarded = HttpRequest.newBuilder(externalTokenEndpoint)
                .header(HttpHeaders.CONTENT_TYPE, headers.getFirst(HttpHeaders.CONTENT_TYPE))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        Optional.ofNullable(headers.getFirst(HttpHeaders.AUTHORIZATION))
                .ifPresent(authorization -> forwarded.header(HttpHeaders.AUTHORIZATION, authorization));
        return forwarded.build();
    }

    private static Map<String, String> formParameters(String body) {
        return Arrays.stream(body.split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toUnmodifiableMap(
                        IdentityProviderTokenRequestParametersTest::decodedName,
                        IdentityProviderTokenRequestParametersTest::decodedValue));
    }

    private static String decodedName(String[] pair) {
        return URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
    }

    private static String decodedValue(String[] pair) {
        return pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
    }

    private static HttpResponse<byte[]> send(HttpRequest request) throws IOException {
        try {
            return FORWARDING_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    public static class IdentityBrokeringAPIV2ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.IDENTITY_BROKERING_API_V2);
        }
    }

    public static class ConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            String externalRealmUrl = "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/openid-connect";
            return realm.identityProviders(IdentityProviderBuilder.create()
                    .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                    .alias(IDP_ALIAS)
                    .attribute("clientId", "test-app-external-realm")
                    .attribute("clientSecret", "test-secret")
                    .attribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                    .attribute("authorizationUrl", externalRealmUrl + "/auth")
                    .attribute(OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL, "http://127.0.0.1:8500" + TOKEN_PATH)
                    .attribute(OAuth2IdentityProviderConfig.TOKEN_REQUEST_PARAMETERS, CONFIGURED_PARAMETERS)
                    .storeToken(true)
                    .build());
        }
    }

    public static class ExternalRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.users(UserBuilder.create("testuser")
                    .name("Test", "User")
                    .email("test@localhost")
                    .emailVerified(Boolean.TRUE)
                    .password("password"));
        }
    }

    public static class ExternalRealmClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("test-app")
                    .redirectUris("http://localhost:8080/*")
                    .secret("test-secret");
        }
    }

    public static class ConsumerClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("test-app")
                    .attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.TRUE.toString())
                    .attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, IDP_ALIAS)
                    .secret("test-secret");
        }
    }
}
