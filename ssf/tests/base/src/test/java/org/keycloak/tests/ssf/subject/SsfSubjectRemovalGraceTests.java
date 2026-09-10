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
 * Integration tests for the SSF 1.0 §9.3 "Malicious Subject Removal"
 * tombstone defense. The transmitter SPI is configured with a positive
 * {@code subject-removal-grace-seconds} so that receiver-driven
 * {@code POST /streams/subjects/remove} calls leave a tombstone the
 * dispatcher honors for the configured window.
 *
 * <p>Coverage:
 *
 * <ul>
 *     <li>Receiver-driven remove inside the grace → events keep flowing.</li>
 *     <li>Admin-driven remove → events stop immediately even with grace
 *         configured (operator actions are trusted, no tombstone is
 *         stamped).</li>
 *     <li>Per-receiver override ({@code ssf.subjectRemovalGraceSeconds=0})
 *         disables the grace for one receiver while the transmitter
 *         default stays positive.</li>
 *     <li>Re-adding the subject after a receiver-remove clears the
 *         tombstone — a subsequent receiver-remove starts a fresh
 *         grace window.</li>
 * </ul>
 *
 * <p>The realm uses {@code default_subjects=NONE} so the dispatcher's
 * NONE-mode no-marker fallback path (the only place the tombstone is
 * checked) is the one being exercised.
 */
@KeycloakIntegrationTest(config = SsfSubjectRemovalGraceTests.RemovalGraceServerConfig.class)
public class SsfSubjectRemovalGraceTests {

    static final String RECEIVER_GRACE = "ssf-receiver-grace";
    static final String RECEIVER_GRACE_SECRET = "receiver-grace-secret";

    /**
     * Receiver with a per-receiver override of {@code 0} — opts OUT
     * of the grace window even though the transmitter-wide SPI value
     * is positive.
     */
    static final String RECEIVER_NO_GRACE = "ssf-receiver-no-grace";
    static final String RECEIVER_NO_GRACE_SECRET = "receiver-no-grace-secret";

    static final String TEST_USER = "grace-tester";
    static final String TEST_EMAIL = "grace-tester@local.test";
    static final String TEST_PASSWORD = "test";

    static final String PUSH_CONTEXT_GRACE = "/ssf/push-grace";
    static final String PUSH_CONTEXT_NO_GRACE = "/ssf/push-no-grace";
    static final String MOCK_PUSH_ENDPOINT_GRACE = "http://127.0.0.1:8500" + PUSH_CONTEXT_GRACE;
    static final String MOCK_PUSH_ENDPOINT_NO_GRACE = "http://127.0.0.1:8500" + PUSH_CONTEXT_NO_GRACE;
    static final String PUSH_AUTH_HEADER = "Bearer dummy-grace-receiver";

    /**
     * Transmitter-wide grace large enough that timing-related test
     * flakiness is impossible — receiver-removes inside this window
     * should always still deliver. Real deployments would use minutes
     * to hours.
     */
    static final int TRANSMITTER_GRACE_SECONDS = 300;

    static final long PUSH_WAIT_SECONDS = 5;

    @InjectRealm(config = RemovalGraceRealm.class)
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

    private final BlockingQueue<String> pushesGrace = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> pushesNoGrace = new LinkedBlockingQueue<>();

    @BeforeEach
    public void setup() {
        pushesGrace.clear();
        pushesNoGrace.clear();
        mockReceiverServer.createContext(PUSH_CONTEXT_GRACE, queueingHandler(pushesGrace));
        mockReceiverServer.createContext(PUSH_CONTEXT_NO_GRACE, queueingHandler(pushesNoGrace));
        assignOptionalClientScopes(RECEIVER_GRACE, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
        assignOptionalClientScopes(RECEIVER_NO_GRACE, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
    }

    @AfterEach
    public void cleanup() {
        bestEffortDeleteStream(RECEIVER_GRACE);
        bestEffortDeleteStream(RECEIVER_NO_GRACE);
        try {
            mockReceiverServer.removeContext(PUSH_CONTEXT_GRACE);
            mockReceiverServer.removeContext(PUSH_CONTEXT_NO_GRACE);
        } catch (IllegalArgumentException ignored) {
        }
        // Admin-remove clears the per-user notify state on both
        // receivers without leaving a tombstone, so the next test
        // starts from a clean slate.
        bestEffortAdminClear(RECEIVER_GRACE);
        bestEffortAdminClear(RECEIVER_NO_GRACE);
    }

    // ---- §9.3 grace coverage ----

    @Test
    public void receiverDrivenRemove_inheritsTransmitterGrace_keepsDeliveringInWindow() throws Exception {
        // Subscribe the test user as a subject and create a PUSH stream.
        adminAdd(RECEIVER_GRACE, TEST_EMAIL);
        String token = obtainReceiverToken(RECEIVER_GRACE, RECEIVER_GRACE_SECRET);
        createPushStream(token, MOCK_PUSH_ENDPOINT_GRACE);

        // Sanity: subscribed user → push arrives.
        triggerUserLogout();
        Assertions.assertNotNull(pushesGrace.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "subscribed user's logout should reach the receiver before remove");

        // Receiver-driven remove stamps the tombstone. We do NOT clear
        // the receiver's mock push endpoint queue — only the per-test
        // queue is cleared in setUp.
        receiverRemove(RECEIVER_GRACE, RECEIVER_GRACE_SECRET, token, TEST_EMAIL);

        // Inside the configured grace window, a fresh logout must
        // still produce a push. The transmitter-wide SPI value
        // (TRANSMITTER_GRACE_SECONDS) is large enough that no real
        // wallclock can drift past it during the test.
        triggerUserLogout();
        Assertions.assertNotNull(pushesGrace.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "receiver-driven remove should not stop delivery while inside the §9.3 grace window");
    }

    @Test
    public void adminRemove_withTransmitterGrace_stopsDeliveryImmediately() throws Exception {
        // Admin add → receiver subscribed.
        adminAdd(RECEIVER_GRACE, TEST_EMAIL);
        String token = obtainReceiverToken(RECEIVER_GRACE, RECEIVER_GRACE_SECRET);
        createPushStream(token, MOCK_PUSH_ENDPOINT_GRACE);

        triggerUserLogout();
        Assertions.assertNotNull(pushesGrace.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "subscribed user's logout should reach the receiver before remove");

        // Admin-driven remove deliberately skips the tombstone — even
        // with a positive transmitter grace, admin removes take effect
        // immediately.
        adminRemove(RECEIVER_GRACE, TEST_EMAIL);

        triggerUserLogout();
        Assertions.assertNull(pushesGrace.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "admin remove should stop delivery immediately, no §9.3 grace tail");
    }

    @Test
    public void receiverDrivenRemove_perReceiverOverrideZero_stopsImmediately() throws Exception {
        // RECEIVER_NO_GRACE has ssf.subjectRemovalGraceSeconds=0
        // declared in its realm config — opts out of the transmitter
        // grace.
        adminAdd(RECEIVER_NO_GRACE, TEST_EMAIL);
        String token = obtainReceiverToken(RECEIVER_NO_GRACE, RECEIVER_NO_GRACE_SECRET);
        createPushStream(token, MOCK_PUSH_ENDPOINT_NO_GRACE);

        triggerUserLogout();
        Assertions.assertNotNull(pushesNoGrace.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "subscribed user's logout should reach the receiver before remove");

        receiverRemove(RECEIVER_NO_GRACE, RECEIVER_NO_GRACE_SECRET, token, TEST_EMAIL);

        triggerUserLogout();
        Assertions.assertNull(pushesNoGrace.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "per-receiver override of 0 should disable §9.3 grace even though the transmitter default is positive");
    }

    @Test
    public void readd_clearsTombstone() throws Exception {
        adminAdd(RECEIVER_GRACE, TEST_EMAIL);
        String token = obtainReceiverToken(RECEIVER_GRACE, RECEIVER_GRACE_SECRET);
        createPushStream(token, MOCK_PUSH_ENDPOINT_GRACE);

        // First receiver-remove writes a tombstone.
        receiverRemove(RECEIVER_GRACE, RECEIVER_GRACE_SECRET, token, TEST_EMAIL);

        // Re-add must clear the tombstone (re-subscribing trumps any
        // prior receiver-driven remove). After re-add, deliveries flow
        // via the include marker again, not via the grace fallback —
        // we exercise the include path here to prove the tombstone
        // doesn't keep lingering.
        receiverAdd(RECEIVER_GRACE, RECEIVER_GRACE_SECRET, token, TEST_EMAIL);

        triggerUserLogout();
        Assertions.assertNotNull(pushesGrace.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "re-added subject should receive events via the include marker after the tombstone was cleared");

        // Admin-remove with no tombstone → no further deliveries.
        // Demonstrates that the prior tombstone really was cleared:
        // admin removes don't write tombstones, so if the prior one
        // were still around, the next event would still slip through
        // the grace path. It must not.
        adminRemove(RECEIVER_GRACE, TEST_EMAIL);

        triggerUserLogout();
        Assertions.assertNull(pushesGrace.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "after re-add → admin-remove the user should be fully unsubscribed, no §9.3 grace tail (tombstone was cleared on re-add)");
    }

    // ---- helpers ----

    protected HttpHandler queueingHandler(BlockingQueue<String> sink) {
        return new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try (InputStream is = exchange.getRequestBody()) {
                    sink.add(new String(is.readAllBytes(), StandardCharsets.UTF_8));
                }
                HttpServerUtil.sendResponse(exchange, 202, Map.of());
            }
        };
    }

    protected SimpleHttpResponse adminSubjectRequest(String clientId, String action,
                                                     String type, String value) throws IOException {
        String url = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + clientId + "/" + action;
        return http.doPost(url)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .json(Map.of("type", type, "value", value))
                .asResponse();
    }

    protected void adminAdd(String clientId, String email) throws IOException {
        try (SimpleHttpResponse res = adminSubjectRequest(clientId, "subjects/add", "user-email", email)) {
            Assertions.assertEquals(200, res.getStatus(),
                    "admin subjects/add should succeed for " + clientId);
        }
    }

    protected void adminRemove(String clientId, String email) throws IOException {
        try (SimpleHttpResponse res = adminSubjectRequest(clientId, "subjects/remove", "user-email", email)) {
            Assertions.assertEquals(200, res.getStatus(),
                    "admin subjects/remove should succeed for " + clientId);
        }
    }

    protected void bestEffortAdminClear(String clientId) {
        try {
            adminSubjectRequest(clientId, "subjects/remove", "user-email", TEST_EMAIL).close();
        } catch (Exception ignored) {
        }
    }

    protected void receiverAdd(String clientId, String secret, String token, String email) throws IOException {
        String streamId = readStreamId(clientId, secret, token);
        // Receiver subjects/add — uses the SSF subject format
        // {format: "email", email: "..."}.
        Map<String, Object> body = Map.of(
                "stream_id", streamId,
                "subject", Map.of("format", "email", "email", email));
        String url = realm.getBaseUrl() + "/ssf/transmitter/subjects/add";
        try (SimpleHttpResponse res = http.doPost(url)
                .json(body)
                .auth(token)
                .asResponse()) {
            Assertions.assertEquals(200, res.getStatus(),
                    "receiver subjects/add should succeed for " + clientId);
        }
    }

    protected void receiverRemove(String clientId, String secret, String token, String email) throws IOException {
        String streamId = readStreamId(clientId, secret, token);
        Map<String, Object> body = Map.of(
                "stream_id", streamId,
                "subject", Map.of("format", "email", "email", email));
        String url = realm.getBaseUrl() + "/ssf/transmitter/subjects/remove";
        try (SimpleHttpResponse res = http.doPost(url)
                .json(body)
                .auth(token)
                .asResponse()) {
            Assertions.assertEquals(204, res.getStatus(),
                    "receiver subjects/remove should succeed for " + clientId);
        }
    }

    protected String readStreamId(String clientId, String secret, String token) throws IOException {
        // GET /streams returns a JSON array (the receiver may carry
        // more than one stream in principle — the transmitter caps at
        // one but the wire shape stays an array). Pull the first
        // element's stream_id.
        try (SimpleHttpResponse response = http.doGet(SsfTransmitterUrls.getStreamsEndpointUrl(realm.getBaseUrl()))
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus());
            var arr = response.asJson();
            Assertions.assertTrue(arr.isArray() && arr.size() > 0,
                    () -> "expected /streams to return a non-empty array for " + clientId);
            return arr.get(0).get("stream_id").asText();
        }
    }

    protected String obtainReceiverToken(String clientId, String secret) throws IOException {
        String tokenUrl = realm.getBaseUrl() + "/protocol/openid-connect/token";
        try (SimpleHttpResponse response = http.doPost(tokenUrl)
                .authBasic(clientId, secret)
                .param("grant_type", "client_credentials")
                .param("scope", SsfScopes.SCOPE_SSF_MANAGE + " " + SsfScopes.SCOPE_SSF_READ)
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus());
            return response.asJson().get("access_token").asText();
        }
    }

    protected StreamConfig createPushStream(String token, String pushEndpoint) throws IOException {
        StreamDeliveryConfig delivery = new StreamDeliveryConfig();
        delivery.setMethod(Ssf.DELIVERY_METHOD_PUSH_URI);
        delivery.setEndpointUrl(pushEndpoint);
        delivery.setAuthorizationHeader(PUSH_AUTH_HEADER);

        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setDelivery(delivery);
        streamConfig.setEventsRequested(Set.of(CaepSessionRevoked.TYPE));
        streamConfig.setDescription("§9.3 grace integration test");

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

    // ---- config ----

    public static class RemovalGraceServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            config.features(Profile.Feature.SSF);
            config.log().categoryLevel("org.keycloak.ssf", "DEBUG");
            // Disable the per-receiver verify rate-limit so test setup
            // doesn't trip over auto-verification timing.
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_MIN_VERIFICATION_INTERVAL_SECONDS, "0");
            // Keep the drainer ticking quickly so push delivery times
            // stay inside the test's PUSH_WAIT_SECONDS window.
            config.spiOption("ssf-transmitter", "default",
                    DefaultSsfTransmitterProviderFactory.CONFIG_OUTBOX_DRAINER_INTERVAL, "500ms");
            // The whole point of this test class — enable the §9.3
            // protection at the transmitter level. Per-receiver
            // overrides on individual clients can still flip it back to 0.
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_SUBJECT_REMOVAL_GRACE_SECONDS,
                    String.valueOf(TRANSMITTER_GRACE_SECONDS));
            // Test pushes to a local mock server on a loopback URL (http://127.0.0.1:NNNN/...).
            // Relax the http-scheme + private-host gate so the mock URL is accepted; the
            // per-client ssf.validPushUrls allow-list configured on each receiver below
            // is still the SSRF defence.
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_ALLOW_INSECURE_PUSH_TARGETS, "true");
            return configured;
        }
    }

    public static class RemovalGraceRealm implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-removal-grace");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");

            realm.eventsEnabled(true);
            realm.adminEventsEnabled(true);
            realm.eventsListeners("jboss-logging", "ssf-events");

            realm.users(
                    UserBuilder.create(TEST_USER)
                            .email(TEST_EMAIL)
                            .firstName("Grace")
                            .lastName("Tester")
                            .enabled(true)
                            .password(TEST_PASSWORD)
                            .build()
            );

            // Receiver A: inherits the transmitter-wide grace.
            realm.clients(
                    ClientBuilder.create(RECEIVER_GRACE)
                            .secret(RECEIVER_GRACE_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "http://127.0.0.1:8500/*")
                            .attribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "NONE")
                            .build()
            );

            // Receiver B: explicit per-receiver override of 0 — opts
            // OUT of the transmitter-wide grace.
            realm.clients(
                    ClientBuilder.create(RECEIVER_NO_GRACE)
                            .secret(RECEIVER_NO_GRACE_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "http://127.0.0.1:8500/*")
                            .attribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "NONE")
                            .attribute(ClientStreamStore.SSF_SUBJECT_REMOVAL_GRACE_SECONDS_KEY, "0")
                            .build()
            );

            return realm;
        }
    }
}
