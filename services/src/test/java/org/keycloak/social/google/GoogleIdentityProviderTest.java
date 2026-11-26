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
package org.keycloak.social.google;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.oidc.mappers.AdvancedClaimToGroupMapper;
import org.keycloak.broker.oidc.mappers.AdvancedClaimToRoleMapper;
import org.keycloak.broker.oidc.mappers.ClaimToRoleMapper;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.IdentityBrokerState;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class GoogleIdentityProviderTest {

    private static final String IDP_ALIAS = "google";
    private static final String SUBJECT = "google-user-123";
    private static final String USER_EMAIL = "alice@example.com";
    private static final String ACCESS_TOKEN = "ya29.test-token";

    @Test
    public void extractIdentity_defaultConfigWithUserInfo_retrievesAccountWithoutCallingGroupsApi() throws Exception {
        AtomicInteger userInfoCalls = new AtomicInteger();
        AtomicInteger groupsApiCalls = new AtomicInteger();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/userinfo", exchange -> {
            userInfoCalls.incrementAndGet();
            byte[] body = ("{\"sub\":\"" + SUBJECT + "\",\"name\":\"Alice Smith\",\"given_name\":\"Alice\","
                    + "\"family_name\":\"Smith\",\"email\":\"" + USER_EMAIL + "\",\"email_verified\":true}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.createContext("/v1/groups/", exchange -> {
            groupsApiCalls.incrementAndGet();
            exchange.sendResponseHeaders(500, -1);
        });
        server.start();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            KeycloakSession session = createSessionWithHttpClient(httpClient);
            // Default config without setting groupRetrievalMode
            GoogleIdentityProviderConfig defaultConfig = new GoogleIdentityProviderFactory().createConfig();
            defaultConfig.setAlias(IDP_ALIAS);
            defaultConfig.setClientId("google-client-id");
            defaultConfig.setEnabled(true);

            GoogleIdentityProvider provider = new GoogleIdentityProvider(session, defaultConfig);
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            defaultConfig.setUserInfoUrl(baseUrl + "/v1/userinfo");

            JsonWebToken idToken = new JsonWebToken();
            idToken.subject(SUBJECT);

            BrokeredIdentityContext identity = provider.extractIdentity(null, ACCESS_TOKEN, idToken);

            Assert.assertEquals(1, userInfoCalls.get());
            Assert.assertEquals(0, groupsApiCalls.get());
            Assert.assertEquals(GoogleIdentityProviderConfig.GroupRetrievalMode.DISABLED, defaultConfig.getGroupRetrievalMode());
            Assert.assertEquals(SUBJECT, identity.getId());
            Assert.assertEquals(USER_EMAIL, identity.getEmail());
            Assert.assertEquals(USER_EMAIL, identity.getUsername());
            Assert.assertEquals("Alice", identity.getFirstName());
            Assert.assertEquals("Smith", identity.getLastName());
            Assert.assertEquals( Boolean.TRUE, identity.getContextData().get(IDToken.EMAIL_VERIFIED));
            Assert.assertNull(AbstractClaimMapper.getClaimValue(identity, GoogleIdentityProvider.GROUPS_CLAIM_NAME));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void extractIdentity_groupRetrievalDisabled_doesNotFetchGroups() throws IOException {
        GoogleIdentityProviderConfig config = createConfig(GoogleIdentityProviderConfig.GroupRetrievalMode.DISABLED);
        List<String> calledUrls = new ArrayList<>();
        TestGoogleIdentityProvider provider = new TestGoogleIdentityProvider(config, (url, token, email, pageToken) -> {
            calledUrls.add(url);
            return JsonSerialization.createObjectNode();
        });

        BrokeredIdentityContext identity = provider.extractIdentity(null, ACCESS_TOKEN, buildIdToken(USER_EMAIL));

        Assert.assertTrue(calledUrls.isEmpty());
        Assert.assertNull(AbstractClaimMapper.getClaimValue(identity, GoogleIdentityProvider.GROUPS_CLAIM_NAME));
    }

    @Test
    public void extractIdentity_directGroupsWithPagination_populatesGroupsClaimAndUserInfo() throws IOException {
        GoogleIdentityProviderConfig config = createConfig(GoogleIdentityProviderConfig.GroupRetrievalMode.DIRECT);
        List<String> calledUrls = new ArrayList<>();
        List<String> pageTokens = new ArrayList<>();

        TestGoogleIdentityProvider provider = new TestGoogleIdentityProvider(config, (url, token, email, pageToken) -> {
            calledUrls.add(url);
            pageTokens.add(pageToken);
            Assert.assertEquals(ACCESS_TOKEN, token);
            Assert.assertEquals(USER_EMAIL, email);
            if (pageToken == null) {
                return JsonSerialization.readValue(
                        "{\"memberships\":[{\"group\":\"groups/1\",\"groupKey\":{\"id\":\"devs@example.com\"}}],\"nextPageToken\":\"page-2\"}",
                        JsonNode.class);
            } else {
                Assert.assertEquals("page-2", pageToken);
                return JsonSerialization.readValue(
                        "{\"memberships\":[{\"group\":\"groups/2\",\"groupKey\":{\"id\":\"admins@example.com\"}}]}",
                        JsonNode.class);
            }
        });

        BrokeredIdentityContext identity = provider.extractIdentity(null, ACCESS_TOKEN, buildIdToken(USER_EMAIL));

        Assert.assertEquals(
                List.of(GoogleIdentityProvider.SEARCH_DIRECT_GROUPS_URL, GoogleIdentityProvider.SEARCH_DIRECT_GROUPS_URL),
                calledUrls);
        Assert.assertEquals(Arrays.asList(null, "page-2"), pageTokens);

        List<String> expectedGroups = List.of("devs@example.com", "admins@example.com");
        Assert.assertEquals(expectedGroups, AbstractClaimMapper.getClaimValue(identity, GoogleIdentityProvider.GROUPS_CLAIM_NAME));

        JsonNode userInfo = (JsonNode) identity.getContextData().get(GoogleIdentityProvider.USER_INFO);
        Assert.assertEquals(expectedGroups, AbstractJsonUserAttributeMapper.getJsonValue(userInfo, GoogleIdentityProvider.GROUPS_CLAIM_NAME));

        IdentityProviderMapperModel roleMapperModel = new IdentityProviderMapperModel();
        roleMapperModel.setConfig(Map.of(
                AbstractClaimMapper.CLAIM, GoogleIdentityProvider.GROUPS_CLAIM_NAME,
                AbstractClaimMapper.CLAIM_VALUE, "admins@example.com"));
        TestableClaimToRoleMapper roleMapper = new TestableClaimToRoleMapper();
        Assert.assertTrue(roleMapper.applies(roleMapperModel, identity));
    }

    @Test
    public void extractIdentity_transitiveGroups_callsTransitiveEndpoint() throws IOException {
        GoogleIdentityProviderConfig config = createConfig(GoogleIdentityProviderConfig.GroupRetrievalMode.TRANSITIVE);
        List<String> calledUrls = new ArrayList<>();

        TestGoogleIdentityProvider provider = new TestGoogleIdentityProvider(config, (url, token, email, pageToken) -> {
            calledUrls.add(url);
            return JsonSerialization.readValue(
                    "{\"memberships\":[{\"group\":\"groups/1\",\"groupKey\":{\"id\":\"nested-team@example.com\"}},{\"group\":\"groups/2\",\"groupKey\":{\"id\":\"parent-org@example.com\"}}]}",
                    JsonNode.class);
        });

        BrokeredIdentityContext identity = provider.extractIdentity(null, ACCESS_TOKEN, buildIdToken(USER_EMAIL));

        Assert.assertEquals(List.of(GoogleIdentityProvider.SEARCH_TRANSITIVE_GROUPS_URL), calledUrls);
        Assert.assertEquals(
                List.of("nested-team@example.com", "parent-org@example.com"),
                AbstractClaimMapper.getClaimValue(identity, GoogleIdentityProvider.GROUPS_CLAIM_NAME));
    }

    @Test(expected = IdentityBrokerException.class)
    public void extractIdentity_missingEmailWhenGroupsEnabled_throwsIdentityBrokerException() throws IOException {
        GoogleIdentityProviderConfig config = createConfig(GoogleIdentityProviderConfig.GroupRetrievalMode.DIRECT);
        TestGoogleIdentityProvider provider = new TestGoogleIdentityProvider(config, (url, token, email, pageToken) -> {
            throw new AssertionError("Should not be called when email is missing");
        });

        provider.extractIdentity(null, ACCESS_TOKEN, buildIdToken(null));
    }

    @Test(expected = IdentityBrokerException.class)
    public void extractIdentity_apiFailure_throwsIdentityBrokerException() throws IOException {
        GoogleIdentityProviderConfig config = createConfig(GoogleIdentityProviderConfig.GroupRetrievalMode.TRANSITIVE);
        TestGoogleIdentityProvider provider = new TestGoogleIdentityProvider(config, (url, token, email, pageToken) -> {
            throw new IdentityBrokerException("Could not retrieve user groups from Google Cloud Identity API.");
        });

        provider.extractIdentity(null, ACCESS_TOKEN, buildIdToken(USER_EMAIL));
    }

    @Test
    public void createAuthorizationUrl_appendsGroupsScopeOnlyWhenEnabledAndNotPresent() {
        AuthenticationSessionModel authSession = (AuthenticationSessionModel) Proxy.newProxyInstance(
                AuthenticationSessionModel.class.getClassLoader(),
                new Class<?>[]{AuthenticationSessionModel.class},
                (proxy, method, args) -> null);
        AuthenticationRequest request = new AuthenticationRequest(
                null, null, authSession, null, null,
                IdentityBrokerState.decoded("state", "client", "client-id", "tab", "data"),
                "https://localhost/redirect");

        GoogleIdentityProviderConfig disabledConfig = createConfig(GoogleIdentityProviderConfig.GroupRetrievalMode.DISABLED);
        TestGoogleIdentityProvider disabledProvider = new TestGoogleIdentityProvider(disabledConfig, null);
        URI disabledUri = disabledProvider.createAuthorizationUrl(request).build();
        Assert.assertFalse(disabledUri.getQuery().contains("cloud-identity.groups.readonly"));

        GoogleIdentityProviderConfig directConfig = createConfig(GoogleIdentityProviderConfig.GroupRetrievalMode.DIRECT);
        TestGoogleIdentityProvider directProvider = new TestGoogleIdentityProvider(directConfig, null);
        URI directUri = directProvider.createAuthorizationUrl(request).build();
        Assert.assertTrue(directUri.getQuery().contains("cloud-identity.groups.readonly"));

        // Ensure scope is not duplicated if already configured in defaultScope
        GoogleIdentityProviderConfig transitiveConfig = createConfig(GoogleIdentityProviderConfig.GroupRetrievalMode.TRANSITIVE);
        transitiveConfig.setDefaultScope(GoogleIdentityProvider.DEFAULT_SCOPE + " " + GoogleIdentityProvider.CLOUD_IDENTITY_GROUPS_SCOPE);
        TestGoogleIdentityProvider transitiveProvider = new TestGoogleIdentityProvider(transitiveConfig, null);
        String query = transitiveProvider.createAuthorizationUrl(request).build().getQuery();
        int firstIdx = query.indexOf("cloud-identity.groups.readonly");
        int lastIdx = query.lastIndexOf("cloud-identity.groups.readonly");
        Assert.assertTrue(firstIdx > 0);
        Assert.assertEquals(firstIdx, lastIdx);
    }

    @Test
    public void extractIdentity_scopedMode_fetchesMembershipGraphForConfiguredGroupsAndDeduplicates() throws IOException {
        List<String> calledUrls = new ArrayList<>();
        GoogleIdentityProviderConfig config = createConfig(GoogleIdentityProviderConfig.GroupRetrievalMode.SCOPED);
        config.setScopedGroups(" umbrella-a@example.com , groups/group-id-2 , group-id-3 ");

        TestGoogleIdentityProvider provider = new TestGoogleIdentityProvider(config, (url, token, email, pageToken) -> {
            calledUrls.add(url);
            Assert.assertNull(pageToken);
            if (GoogleIdentityProvider.LOOKUP_GROUP_URL.equals(url)) {
                Assert.assertEquals("umbrella-a@example.com", email);
                return JsonSerialization.mapper.readTree("{\"name\":\"groups/group-id-1\"}");
            }
            if (url.endsWith("/groups/group-id-1/memberships:getMembershipGraph")) {
                return JsonSerialization.mapper.readTree(
                        "{\"done\":true,\"response\":{\"groups\":[" +
                                "{\"name\":\"groups/shared-id\",\"groupKey\":{\"id\":\"shared-team@example.com\"}}," +
                                "{\"name\":\"groups/group-id-1\",\"groupKey\":{\"id\":\"umbrella-a@example.com\"}}" +
                                "]}}");
            }
            if (url.endsWith("/groups/group-id-2/memberships:getMembershipGraph")) {
                return JsonSerialization.mapper.readTree(
                        "{\"done\":true,\"response\":{\"groups\":[" +
                                "{\"name\":\"groups/shared-id\",\"groupKey\":{\"id\":\"shared-team@example.com\"}}," +
                                "{\"name\":\"groups/group-id-2\",\"groupKey\":{\"id\":\"umbrella-b@example.com\"}}" +
                                "]}}");
            }
            if (url.endsWith("/groups/group-id-3/memberships:getMembershipGraph")) {
                return JsonSerialization.mapper.readTree("{\"done\":true,\"response\":{}}");
            }
            throw new AssertionError("Unexpected URL: " + url);
        });

        BrokeredIdentityContext identity = provider.extractIdentity(null, ACCESS_TOKEN, buildIdToken(USER_EMAIL));

        Assert.assertEquals(List.of(
                GoogleIdentityProvider.LOOKUP_GROUP_URL,
                "https://cloudidentity.googleapis.com/v1/groups/group-id-1/memberships:getMembershipGraph",
                "https://cloudidentity.googleapis.com/v1/groups/group-id-2/memberships:getMembershipGraph",
                "https://cloudidentity.googleapis.com/v1/groups/group-id-3/memberships:getMembershipGraph"), calledUrls);
        Assert.assertEquals(
                List.of("shared-team@example.com", "umbrella-a@example.com", "umbrella-b@example.com"),
                AbstractClaimMapper.getClaimValue(identity, GoogleIdentityProvider.GROUPS_CLAIM_NAME));
    }

    @Test
    public void extractIdentity_scopedMode_incompleteOrErrorOperationThrowsIdentityBrokerException() {
        GoogleIdentityProviderConfig config = createConfig(GoogleIdentityProviderConfig.GroupRetrievalMode.SCOPED);
        config.setScopedGroups("group-id-1");

        TestGoogleIdentityProvider incompleteProvider = new TestGoogleIdentityProvider(
                config, (url, token, email, pageToken) -> JsonSerialization.mapper.readTree("{\"done\":false}"));
        Assert.assertThrows(IdentityBrokerException.class,
                () -> incompleteProvider.extractIdentity(null, ACCESS_TOKEN, buildIdToken(USER_EMAIL)));

        TestGoogleIdentityProvider errorProvider = new TestGoogleIdentityProvider(
                config, (url, token, email, pageToken) -> JsonSerialization.mapper.readTree(
                        "{\"done\":true,\"error\":{\"code\":13,\"message\":\"Internal error\"}}"));
        Assert.assertThrows(IdentityBrokerException.class,
                () -> errorProvider.extractIdentity(null, ACCESS_TOKEN, buildIdToken(USER_EMAIL)));
    }

    @Test
    public void configValidation_validatesGroupRetrievalMode() {
        GoogleIdentityProviderFactory factory = new GoogleIdentityProviderFactory();
        GoogleIdentityProviderConfig config = factory.createConfig();
        config.setGroupRetrievalMode(GoogleIdentityProviderConfig.GroupRetrievalMode.TRANSITIVE);
        config.validate(null);
        Assert.assertEquals(GoogleIdentityProviderConfig.GroupRetrievalMode.TRANSITIVE, config.getGroupRetrievalMode());

        config.setGroupRetrievalMode(GoogleIdentityProviderConfig.GroupRetrievalMode.SCOPED);
        Assert.assertThrows(IllegalArgumentException.class, () -> config.validate(null));

        config.setScopedGroups(" , groups/ , ");
        Assert.assertThrows(IllegalArgumentException.class, () -> config.validate(null));

        config.setScopedGroups("01abc23def45ghi,groups/02xyz89uvw12rst");
        config.validate(null);

        config.getConfig().put(GoogleIdentityProviderConfig.GROUP_RETRIEVAL_MODE, "INVALID_MODE");
        Assert.assertThrows(IllegalArgumentException.class, () -> config.validate(null));
    }

    @Test
    public void mappers_includeGoogleProviderAsCompatible() {
        Assert.assertTrue(Arrays.asList(AdvancedClaimToGroupMapper.COMPATIBLE_PROVIDERS).contains(GoogleIdentityProviderFactory.PROVIDER_ID));
        Assert.assertTrue(Arrays.asList(ClaimToRoleMapper.COMPATIBLE_PROVIDERS).contains(GoogleIdentityProviderFactory.PROVIDER_ID));
        Assert.assertTrue(Arrays.asList(AdvancedClaimToRoleMapper.COMPATIBLE_PROVIDERS).contains(GoogleIdentityProviderFactory.PROVIDER_ID));
    }

    @Test
    public void fetchGroupsPage_realHttpRoundTripAndErrorHandling() throws Exception {
        AtomicReference<String> receivedAuthHeader = new AtomicReference<>();
        AtomicReference<String> receivedQuery = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/groups/-/memberships:searchDirectGroups", exchange -> {
            receivedAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            receivedQuery.set(exchange.getRequestURI().getRawQuery());
            byte[] body = "{\"memberships\":[{\"group\":\"groups/1\",\"groupKey\":{\"id\":\"real-http@example.com\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.createContext("/v1/groups/-/memberships:searchTransitiveGroups", exchange -> {
            byte[] body = "{\"error\":{\"code\":403,\"message\":\"Permission denied\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(403, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            KeycloakSession session = createSessionWithHttpClient(httpClient);
            GoogleIdentityProvider provider = new GoogleIdentityProvider(
                    session, createConfig(GoogleIdentityProviderConfig.GroupRetrievalMode.DIRECT));

            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            JsonNode page = provider.fetchGroupsPage(
                    baseUrl + "/v1/groups/-/memberships:searchDirectGroups",
                    ACCESS_TOKEN,
                    "o'hara@example.com",
                    "token-123");

            Assert.assertEquals("Bearer " + ACCESS_TOKEN, receivedAuthHeader.get());
            Assert.assertTrue(receivedQuery.get().contains("pageToken=token-123"));
            Assert.assertTrue(receivedQuery.get().contains("member_key_id"));
            Assert.assertEquals("real-http@example.com", page.get("memberships").get(0).get("groupKey").get("id").asText());

            Assert.assertThrows(IdentityBrokerException.class, () -> provider.fetchGroupsPage(
                    baseUrl + "/v1/groups/-/memberships:searchTransitiveGroups",
                    ACCESS_TOKEN,
                    USER_EMAIL,
                    null));
        } finally {
            server.stop(0);
        }
    }

    /**
     * Live integration test against Google Cloud Identity API when {@code GOOGLE_TEST_ACCESS_TOKEN}
     * and {@code GOOGLE_TEST_USER_EMAIL} environment variables are provided (for example from
     * Google OAuth 2.0 Playground with the {@code https://www.googleapis.com/auth/cloud-identity.groups.readonly} scope).
     */
    @Test
    public void extractIdentity_liveGoogleCloudIdentityToken() throws Exception {
        String liveAccessToken = System.getenv("GOOGLE_TEST_ACCESS_TOKEN");
        String liveEmail = System.getenv("GOOGLE_TEST_USER_EMAIL");
        Assume.assumeNotNull(liveAccessToken, liveEmail);

        String modeEnv = System.getenv("GOOGLE_TEST_GROUP_MODE");
        GoogleIdentityProviderConfig.GroupRetrievalMode mode = modeEnv != null
                ? GoogleIdentityProviderConfig.GroupRetrievalMode.fromString(modeEnv)
                : GoogleIdentityProviderConfig.GroupRetrievalMode.DIRECT;

        HttpClientBuilder clientBuilder = HttpClients.custom();
        String quotaProject = System.getenv("GOOGLE_TEST_QUOTA_PROJECT");
        if (quotaProject != null && !quotaProject.isBlank()) {
            clientBuilder.setDefaultHeaders(List.of(new BasicHeader("X-Goog-User-Project", quotaProject)));
        }

        try (CloseableHttpClient httpClient = clientBuilder.build()) {
            KeycloakSession session = createSessionWithHttpClient(httpClient);
            GoogleIdentityProviderConfig config = createConfig(mode);
            String scopedGroups = System.getenv("GOOGLE_TEST_SCOPED_GROUPS");
            if (scopedGroups != null && !scopedGroups.isBlank()) {
                config.setScopedGroups(scopedGroups);
            }
            GoogleIdentityProvider provider = new GoogleIdentityProvider(session, config);
            BrokeredIdentityContext identity = provider.extractIdentity(null, liveAccessToken, buildIdToken(liveEmail));
            Object groups = AbstractClaimMapper.getClaimValue(identity, GoogleIdentityProvider.GROUPS_CLAIM_NAME);
            Assert.assertNotNull(groups);
            Assert.assertTrue(groups instanceof List<?> && !((List<?>) groups).isEmpty());
        }
    }

    private KeycloakSession createSessionWithHttpClient(CloseableHttpClient httpClient) {
        HttpClientProvider httpClientProvider = new HttpClientProvider() {
            @Override
            public CloseableHttpClient getHttpClient() {
                return httpClient;
            }

            @Override
            public long getMaxConsumedResponseSize() {
                return HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE;
            }

            @Override
            public int postText(String uri, String text) {
                return 0;
            }

            @Override
            public String getString(String uri) {
                return null;
            }

            @Override
            public InputStream getInputStream(String uri) {
                return null;
            }

            @Override
            public void close() {
            }
        };

        return (KeycloakSession) Proxy.newProxyInstance(
                KeycloakSession.class.getClassLoader(),
                new Class<?>[]{KeycloakSession.class},
                (proxy, method, args) -> {
                    if ("getProvider".equals(method.getName()) && args != null && args.length == 1
                            && HttpClientProvider.class.equals(args[0])) {
                        return httpClientProvider;
                    }
                    return null;
                });
    }

    private GoogleIdentityProviderConfig createConfig(GoogleIdentityProviderConfig.GroupRetrievalMode mode) {
        IdentityProviderModel model = new IdentityProviderModel();
        GoogleIdentityProviderConfig config = new GoogleIdentityProviderConfig(model);
        config.setAlias(IDP_ALIAS);
        config.setClientId("google-client-id");
        config.setEnabled(true);
        config.setDisableUserInfoService(true);
        config.setGroupRetrievalMode(mode);
        return config;
    }

    private JsonWebToken buildIdToken(String email) {
        JsonWebToken token = new JsonWebToken();
        token.subject(SUBJECT);
        if (email != null) {
            token.getOtherClaims().put(IDToken.EMAIL, email);
        }
        return token;
    }

    @FunctionalInterface
    private interface GroupsPageFetcher {
        JsonNode fetch(String endpointUrl, String accessToken, String email, String pageToken) throws IOException;
    }

    private static class TestGoogleIdentityProvider extends GoogleIdentityProvider {

        private final GroupsPageFetcher fetcher;

        TestGoogleIdentityProvider(GoogleIdentityProviderConfig config, GroupsPageFetcher fetcher) {
            super(null, config);
            this.fetcher = fetcher;
        }

        @Override
        protected JsonNode fetchGroupsPage(String endpointUrl, String accessToken, String email, String pageToken) throws IOException {
            return fetcher.fetch(endpointUrl, accessToken, email, pageToken);
        }
    }

    private static class TestableClaimToRoleMapper extends ClaimToRoleMapper {
        @Override
        public boolean applies(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
            return super.applies(mapperModel, context);
        }
    }
}
