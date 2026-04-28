package org.keycloak.tests.ssf.subject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.ssf.transmitter.DefaultSsfTransmitterProviderFactory;
import org.keycloak.ssf.transmitter.SsfScopes;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.StreamDeliveryConfig;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.support.SsfTransmitterUrls;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.DefaultKeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.HttpServerUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for SSF subject management: admin add/remove/ignore
 * endpoints, the dispatcher's subject subscription filter, and the
 * auto-notify-on-login behaviour.
 *
 * <p>The realm is configured with {@code default_subjects=NONE} on the
 * receiver client so events are only delivered to explicitly subscribed
 * users. A test user is pre-created for the password-grant + logout flow.
 */
@KeycloakIntegrationTest(config = SsfSubjectManagementTests.SubjectMgmtServerConfig.class)
public class SsfSubjectManagementTests {

    static final String RECEIVER = "ssf-receiver-subjects";
    static final String RECEIVER_SECRET = "receiver-subjects-secret";

    static final String TEST_USER = "subject-tester";
    static final String TEST_EMAIL = "subject-tester@local.test";
    static final String TEST_PASSWORD = "test";

    static final String PUSH_CONTEXT_PATH = "/ssf/push-subjects";
    static final String MOCK_PUSH_ENDPOINT = "http://127.0.0.1:8500" + PUSH_CONTEXT_PATH;
    static final String PUSH_AUTH_HEADER = "Bearer dummy-subjects-receiver";

    static final long PUSH_WAIT_SECONDS = 5;

    @InjectRealm(config = SubjectMgmtRealm.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauthClient;

    @InjectSimpleHttp
    SimpleHttp http;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectHttpServer
    HttpServer mockReceiverServer;

    private final BlockingQueue<String> pushes = new LinkedBlockingQueue<>();

    @BeforeEach
    public void setup() {
        pushes.clear();
        mockReceiverServer.createContext(PUSH_CONTEXT_PATH, new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try (InputStream is = exchange.getRequestBody()) {
                    pushes.add(new String(is.readAllBytes(), StandardCharsets.UTF_8));
                }
                HttpServerUtil.sendResponse(exchange, 202, Map.of());
            }
        });
        assignOptionalClientScopes(RECEIVER, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
    }

    @AfterEach
    public void cleanup() {
        bestEffortDeleteStream(RECEIVER);
        try {
            mockReceiverServer.removeContext(PUSH_CONTEXT_PATH);
        } catch (IllegalArgumentException ignored) {
        }
        // Clear the ssf.notify attribute from the test user
        bestEffortClearNotifyAttribute();
    }

    // ---- admin add / remove / ignore endpoints ----

    @Test
    public void adminAddSubject_byEmail_returns200() throws IOException {

        try (SimpleHttpResponse res = adminSubjectRequest(RECEIVER, "subjects/add",
                "user-email", TEST_EMAIL)) {
            Assertions.assertEquals(200, res.getStatus());
            var body = res.asJson();
            Assertions.assertEquals("added", body.get("status").asText());
            Assertions.assertEquals("user", body.get("entity_type").asText());
            Assertions.assertNotNull(body.get("entity_id").asText());
        }
    }

    @Test
    public void adminAddSubject_idempotent_returns200() throws IOException {

        try (SimpleHttpResponse res = adminSubjectRequest(RECEIVER, "subjects/add",
                "user-email", TEST_EMAIL)) {
            Assertions.assertEquals(200, res.getStatus());
        }
        try (SimpleHttpResponse res = adminSubjectRequest(RECEIVER, "subjects/add",
                "user-email", TEST_EMAIL)) {
            Assertions.assertEquals(200, res.getStatus(),
                    "second add for the same subject should still succeed (idempotent)");
        }
    }

    @Test
    public void adminRemoveSubject_returns204() throws IOException {

        try (SimpleHttpResponse ignored = adminSubjectRequest(RECEIVER, "subjects/add",
                "user-email", TEST_EMAIL)) {}
        try (SimpleHttpResponse res = adminSubjectRequest(RECEIVER, "subjects/remove",
                "user-email", TEST_EMAIL)) {
            Assertions.assertEquals(204, res.getStatus());
        }
    }

    @Test
    public void adminIgnoreSubject_returns200() throws IOException {

        try (SimpleHttpResponse res = adminSubjectRequest(RECEIVER, "subjects/ignore",
                "user-email", TEST_EMAIL)) {
            Assertions.assertEquals(200, res.getStatus());
            var body = res.asJson();
            Assertions.assertEquals("ignored", body.get("status").asText());
        }
    }

    @Test
    public void adminAddSubject_unknownUser_returns404() throws IOException {

        try (SimpleHttpResponse res = adminSubjectRequest(RECEIVER, "subjects/add",
                "user-email", "nonexistent@nowhere.test")) {
            Assertions.assertEquals(404, res.getStatus());
        }
    }

    @Test
    public void adminAddSubject_byUsername_returns200() throws IOException {

        try (SimpleHttpResponse res = adminSubjectRequest(RECEIVER, "subjects/add",
                "user-username", TEST_USER)) {
            Assertions.assertEquals(200, res.getStatus());
            var body = res.asJson();
            Assertions.assertEquals("added", body.get("status").asText());
            Assertions.assertEquals("user", body.get("entity_type").asText());
        }
    }

    // ---- dispatcher filter: NONE mode ----

    @Test
    public void dispatcher_defaultSubjectsNone_skipsUnsubscribedUser() throws Exception {
        String token = obtainReceiverToken();
        createPushStream(token);

        // User is NOT subscribed — logout should NOT produce a push
        triggerUserLogout();

        String push = pushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNull(push,
                "unsubscribed user's event should NOT reach the receiver in NONE mode");
    }

    @Test
    public void dispatcher_defaultSubjectsNone_deliversSubscribedUser() throws Exception {

        // Subscribe the user first
        try (SimpleHttpResponse ignored = adminSubjectRequest(RECEIVER, "subjects/add",
                "user-email", TEST_EMAIL)) {}

        String token = obtainReceiverToken();
        createPushStream(token);

        triggerUserLogout();

        String push = pushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNotNull(push,
                "subscribed user's event should reach the receiver in NONE mode");
    }

    @Test
    public void dispatcher_defaultSubjectsNone_ignoredUserDoesNotReceive() throws Exception {

        // Explicitly ignore the user
        try (SimpleHttpResponse ignored = adminSubjectRequest(RECEIVER, "subjects/ignore",
                "user-email", TEST_EMAIL)) {}

        String token = obtainReceiverToken();
        createPushStream(token);

        triggerUserLogout();

        String push = pushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNull(push,
                "ignored user's event should NOT reach the receiver");
    }

    // ---- helpers ----

    protected SimpleHttpResponse adminSubjectRequest(String clientId, String action,
                                                     String type, String value) throws IOException {
        String url = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + clientId + "/" + action;
        return http.doPost(url)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .json(Map.of("type", type, "value", value))
                .asResponse();
    }

    protected String obtainReceiverToken() throws IOException {
        String tokenUrl = realm.getBaseUrl() + "/protocol/openid-connect/token";
        try (SimpleHttpResponse response = http.doPost(tokenUrl)
                .authBasic(RECEIVER, RECEIVER_SECRET)
                .param("grant_type", "client_credentials")
                .param("scope", SsfScopes.SCOPE_SSF_MANAGE + " " + SsfScopes.SCOPE_SSF_READ)
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus());
            return response.asJson().get("access_token").asText();
        }
    }

    protected StreamConfig createPushStream(String token) throws IOException {
        StreamDeliveryConfig delivery = new StreamDeliveryConfig();
        delivery.setMethod(Ssf.DELIVERY_METHOD_PUSH_URI);
        delivery.setEndpointUrl(MOCK_PUSH_ENDPOINT);
        delivery.setAuthorizationHeader(PUSH_AUTH_HEADER);

        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setDelivery(delivery);
        streamConfig.setEventsRequested(Set.of(CaepSessionRevoked.TYPE));
        streamConfig.setDescription("Subject management integration test");

        try (SimpleHttpResponse response = http.doPost(SsfTransmitterUrls.getStreamsEndpointUrl(realm.getBaseUrl()))
                .json(streamConfig)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(201, response.getStatus());
            return response.asJson(StreamConfig.class);
        }
    }

    protected void triggerUserLogout() {
        AccessTokenResponse tokenResponse = oauthClient.passwordGrantRequest(TEST_USER, TEST_PASSWORD).send();
        Assertions.assertNotNull(tokenResponse.getAccessToken());
        oauthClient.doLogout(tokenResponse.getRefreshToken());
    }

    protected ClientRepresentation findClientByClientId(String clientId) {
        List<ClientRepresentation> clients = realm.admin().clients().findByClientId(clientId);
        Assertions.assertFalse(clients.isEmpty(),
                () -> "expected client '" + clientId + "' to exist");
        return clients.get(0);
    }

    protected void assignOptionalClientScopes(String clientId, String... scopeNames) {
        ClientRepresentation client = findClientByClientId(clientId);
        ClientResource clientResource = realm.admin().clients().get(client.getId());
        Set<String> alreadyAssigned = clientResource.getOptionalClientScopes().stream()
                .map(ClientScopeRepresentation::getName)
                .collect(Collectors.toSet());
        List<ClientScopeRepresentation> allScopes = realm.admin().clientScopes().findAll();
        for (String scopeName : scopeNames) {
            if (alreadyAssigned.contains(scopeName)) continue;
            ClientScopeRepresentation scope = allScopes.stream()
                    .filter(s -> scopeName.equals(s.getName()))
                    .findFirst().orElse(null);
            Assertions.assertNotNull(scope);
            clientResource.addOptionalClientScope(scope.getId());
        }
    }

    protected void bestEffortDeleteStream(String clientId) {
        ClientRepresentation client = findClientByClientId(clientId);
        if (client == null) return;
        String url = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + client.getClientId() + "/stream";
        try (SimpleHttpResponse ignored = http.doDelete(url)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .asResponse()) {
        } catch (IOException ignored) {
        }
    }

    protected void bestEffortClearNotifyAttribute() {
        try {
                adminSubjectRequest(RECEIVER, "subjects/remove", "user-email", TEST_EMAIL).close();
        } catch (Exception ignored) {
        }
    }

    // ---- config ----

    public static class SubjectMgmtServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            config.features(Profile.Feature.SSF);
            config.log().categoryLevel("org.keycloak.ssf", "DEBUG");
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_MIN_VERIFICATION_INTERVAL_SECONDS, "0");
            config.spiOption("ssf-transmitter", "default",
                    DefaultSsfTransmitterProviderFactory.CONFIG_OUTBOX_DRAINER_INTERVAL, "500ms");
            return configured;
        }
    }

    public static class SubjectMgmtRealm implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-subject-mgmt");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");

            realm.eventsEnabled(true);
            realm.adminEventsEnabled(true);
            realm.eventsListeners("jboss-logging", "ssf-events");

            realm.users(
                    UserBuilder.create(TEST_USER)
                    .email(TEST_EMAIL)
                    .firstName("Subject")
                    .lastName("Tester")
                    .enabled(true)
                    .password(TEST_PASSWORD)
                    .build()
            );

            realm.clients(
                    ClientBuilder.create(RECEIVER)
                    .secret(RECEIVER_SECRET)
                    .serviceAccountsEnabled(true)
                    .directAccessGrantsEnabled(false)
                    .publicClient(false)
                    .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                    .attribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "NONE")
                    .build()
            );

            return realm;
        }
    }
}
