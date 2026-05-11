package org.keycloak.tests.ssf.transmitter;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
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
 * Tests for the SSF Transmitter push delivery failure-handling behaviour.
 *
 * <p>These tests don't measure exact timeout values (which are inherently
 * flaky) — they assert the observable invariants that matter for the
 * transmitter:
 * <ul>
 *     <li>a push that fails because the receiver returned a non-2xx status
 *         must not crash or poison the dispatcher, and subsequent
 *         deliveries to the same stream must continue to fire;</li>
 *     <li>a push to an unreachable receiver must not block deliveries to
 *         other healthy streams dispatched from the same event.</li>
 * </ul>
 */
@KeycloakIntegrationTest(config = SsfTransmitterPushFailureTests.PushFailureKeycloakServerConfig.class)
public class SsfTransmitterPushFailureTests {

    static final String RECEIVER_ERROR = "ssf-receiver-push-error";
    static final String RECEIVER_ERROR_SECRET = "receiver-push-error-secret";

    static final String RECEIVER_UNREACH = "ssf-receiver-push-unreach";
    static final String RECEIVER_UNREACH_SECRET = "receiver-push-unreach-secret";

    static final String RECEIVER_HEALTHY = "ssf-receiver-push-healthy";
    static final String RECEIVER_HEALTHY_SECRET = "receiver-push-healthy-secret";

    static final String TEST_USER = "failure-tester";
    static final String TEST_PASSWORD = "test";

    static final String ERROR_CONTEXT_PATH = "/ssf/push-failure-500";
    static final String ERROR_PUSH_ENDPOINT = "http://127.0.0.1:8500" + ERROR_CONTEXT_PATH;

    static final String HEALTHY_CONTEXT_PATH = "/ssf/push-failure-healthy";
    static final String HEALTHY_PUSH_ENDPOINT = "http://127.0.0.1:8500" + HEALTHY_CONTEXT_PATH;

    static final String EXPECTED_PUSH_AUTH_HEADER = "Bearer dummy-push-failure-receiver";

    static final long PUSH_WAIT_SECONDS = 10;

    @InjectRealm(config = PushFailureRealm.class)
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

    private final BlockingQueue<CapturedPush> errorPushes = new LinkedBlockingQueue<>();
    private final BlockingQueue<CapturedPush> healthyPushes = new LinkedBlockingQueue<>();

    @BeforeEach
    public void setup() {
        errorPushes.clear();
        healthyPushes.clear();

        mockReceiverServer.createContext(ERROR_CONTEXT_PATH,
                new CapturingHandler(errorPushes, 500, "{\"error\":\"simulated\"}"));
        mockReceiverServer.createContext(HEALTHY_CONTEXT_PATH,
                new CapturingHandler(healthyPushes, 202, null));

        assignOptionalClientScopes(RECEIVER_ERROR,
                SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
        assignOptionalClientScopes(RECEIVER_UNREACH,
                SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
        assignOptionalClientScopes(RECEIVER_HEALTHY,
                SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
    }

    @AfterEach
    public void cleanup() {
        List.of(RECEIVER_ERROR, RECEIVER_UNREACH, RECEIVER_HEALTHY)
                .forEach(this::bestEffortDeleteStream);

        for (String ctx : List.of(ERROR_CONTEXT_PATH, HEALTHY_CONTEXT_PATH)) {
            try {
                mockReceiverServer.removeContext(ctx);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Test
    public void testPushReceiverErrorDoesNotDisruptTransmitter() throws Exception {

        String token = obtainReceiverToken(RECEIVER_ERROR, RECEIVER_ERROR_SECRET);
        createPushStream(token, ERROR_PUSH_ENDPOINT, Set.of(CaepSessionRevoked.TYPE));

        // First logout: dispatched to the failing receiver which returns 500.
        // The transmitter should log the failure (verified by inspection of
        // PushDeliveryService warn output in the test logs) but continue
        // running, allowing subsequent pushes to dispatch as usual.
        triggerUserLogout();
        Assertions.assertNotNull(errorPushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "first push should reach the failing receiver (even though it responds 500)");

        // Second logout: the dispatcher must still be able to deliver another
        // push to the same receiver. This proves that the 500 response from
        // the previous push did not poison the executor or crash the
        // dispatcher.
        triggerUserLogout();
        Assertions.assertNotNull(errorPushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "subsequent push should still reach the failing receiver");
    }

    @Test
    public void testFailingStreamDoesNotBlockHealthyStream() throws Exception {

        // Configure two receivers for the same realm — one pointing at an
        // unreachable port (connect should fail), one at the healthy mock
        // receiver. A single LOGOUT user event is fanned out to both.
        String unreachableEndpoint = "http://127.0.0.1:" + reserveUnusedPort() + "/ssf/push-failure-unreach";
        // The receiver client's ssf.validPushUrls is configured for the
        // healthy mock port at realm-bootstrap time; override here with
        // an entry that matches the dynamically-allocated unreachable
        // port so the SSRF gate accepts the URL the test is about to
        // register.
        setReceiverValidPushUrls(RECEIVER_UNREACH, unreachableEndpoint);

        String unreachToken = obtainReceiverToken(RECEIVER_UNREACH, RECEIVER_UNREACH_SECRET);
        createPushStream(unreachToken, unreachableEndpoint, Set.of(CaepSessionRevoked.TYPE));

        String healthyToken = obtainReceiverToken(RECEIVER_HEALTHY, RECEIVER_HEALTHY_SECRET);
        createPushStream(healthyToken, HEALTHY_PUSH_ENDPOINT, Set.of(CaepSessionRevoked.TYPE));

        triggerUserLogout();

        // The healthy receiver must still get its push even though the
        // sibling unreachable receiver causes a connect failure on the
        // dispatch executor.
        Assertions.assertNotNull(healthyPushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "healthy stream should receive a push despite the unreachable sibling");
    }

    // --- helpers ---------------------------------------------------------

    /**
     * Binds a {@link ServerSocket} on an OS-assigned port and closes it
     * immediately, returning the port number. The chance the OS reassigns
     * the same port during the short test window is effectively zero, so
     * the returned port is "unreachable" for the purpose of provoking
     * connection refusals.
     */
    protected int reserveUnusedPort() throws IOException {
        try (ServerSocket ss = new ServerSocket(0)) {
            return ss.getLocalPort();
        }
    }

    /**
     * Updates {@code ssf.validPushUrls} on the receiver client at test
     * time. Used by tests whose push URL is computed at runtime (e.g.
     * a dynamically allocated unreachable port) and therefore can't be
     * pinned in the realm-bootstrap allow-list.
     */
    protected void setReceiverValidPushUrls(String clientId, String exactUrl) {
        ClientRepresentation client = findClientByClientId(clientId);
        Assertions.assertNotNull(client, () -> "expected client '" + clientId + "' to be present in realm");
        if (client.getAttributes() == null) {
            client.setAttributes(new java.util.HashMap<>());
        }
        client.getAttributes().put(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, exactUrl);
        realm.admin().clients().get(client.getId()).update(client);
    }

    protected void triggerUserLogout() {
        AccessTokenResponse tokenResponse = oauthClient.passwordGrantRequest(TEST_USER, TEST_PASSWORD).send();
        Assertions.assertNotNull(tokenResponse.getAccessToken(),
                "password grant should succeed for the test user");
        Assertions.assertNotNull(tokenResponse.getRefreshToken(),
                "password grant response should include a refresh token");
        oauthClient.doLogout(tokenResponse.getRefreshToken());
    }

    protected StreamConfig createPushStream(String token, String endpointUrl, Set<String> eventsRequested) throws IOException {
        StreamDeliveryConfig delivery = new StreamDeliveryConfig();
        delivery.setMethod(Ssf.DELIVERY_METHOD_PUSH_URI);
        delivery.setEndpointUrl(endpointUrl);
        delivery.setAuthorizationHeader(EXPECTED_PUSH_AUTH_HEADER);

        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setDelivery(delivery);
        streamConfig.setEventsRequested(eventsRequested);
        streamConfig.setDescription("Push failure integration test");

        try (SimpleHttpResponse response = http.doPost(SsfTransmitterUrls.getStreamsEndpointUrl(realm.getBaseUrl()))
                .json(streamConfig)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(201, response.getStatus(), "stream creation should succeed");
            return response.asJson(StreamConfig.class);
        }
    }

    protected String obtainReceiverToken(String clientId, String secret) throws IOException {
        String tokenUrl = realm.getBaseUrl() + "/protocol/openid-connect/token";
        try (SimpleHttpResponse response = http.doPost(tokenUrl)
                .authBasic(clientId, secret)
                .param("grant_type", "client_credentials")
                .param("scope", SsfScopes.SCOPE_SSF_MANAGE + " " + SsfScopes.SCOPE_SSF_READ)
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus(),
                    () -> "CC grant for client '" + clientId + "' should succeed");
            return response.asJson().get("access_token").asText();
        }
    }

    protected ClientRepresentation findClientByClientId(String clientId) {
        List<ClientRepresentation> clients = realm.admin().clients().findByClientId(clientId);
        if (clients.isEmpty()) {
            return null;
        }
        return clients.get(0);
    }

    protected void assignOptionalClientScopes(String clientId, String... scopeNames) {
        ClientRepresentation client = findClientByClientId(clientId);
        Assertions.assertNotNull(client, () -> "expected client '" + clientId + "' to exist");
        ClientResource clientResource = realm.admin().clients().get(client.getId());

        Set<String> alreadyAssigned = clientResource.getOptionalClientScopes().stream()
                .map(ClientScopeRepresentation::getName)
                .collect(Collectors.toSet());

        List<ClientScopeRepresentation> allScopes = realm.admin().clientScopes().findAll();
        for (String scopeName : scopeNames) {
            if (alreadyAssigned.contains(scopeName)) {
                continue;
            }
            ClientScopeRepresentation scope = allScopes.stream()
                    .filter(s -> scopeName.equals(s.getName()))
                    .findFirst()
                    .orElse(null);
            Assertions.assertNotNull(scope,
                    () -> "expected realm scope '" + scopeName + "' to exist");
            clientResource.addOptionalClientScope(scope.getId());
        }
    }

    protected void bestEffortDeleteStream(String clientId) {
        ClientRepresentation client = findClientByClientId(clientId);
        if (client == null) {
            return;
        }
        String adminStreamUrl = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + client.getClientId() + "/stream";
        try (SimpleHttpResponse ignored = http.doDelete(adminStreamUrl)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .asResponse()) {
            // 204 / 404 both fine
        } catch (IOException e) {
            // best-effort
        }
    }

    /**
     * Mock receiver handler that enqueues each incoming request and
     * responds with the configured status and body.
     */
    protected static final class CapturingHandler implements HttpHandler {

        private final BlockingQueue<CapturedPush> sink;
        private final int responseStatus;
        private final String responseBody;

        CapturingHandler(BlockingQueue<CapturedPush> sink, int responseStatus, String responseBody) {
            this.sink = sink;
            this.responseStatus = responseStatus;
            this.responseBody = responseBody;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (InputStream is = exchange.getRequestBody()) {
                sink.add(new CapturedPush(
                        new String(is.readAllBytes(), StandardCharsets.UTF_8),
                        exchange.getRequestHeaders().getFirst("Authorization"),
                        exchange.getRequestHeaders().getFirst("Content-Type")));
            }
            if (responseBody != null) {
                HttpServerUtil.sendResponse(exchange, responseStatus,
                        Map.of("Content-Type", List.of("application/json")),
                        responseBody);
            } else {
                HttpServerUtil.sendResponse(exchange, responseStatus, Map.of());
            }
        }
    }

    /**
     * Snapshot of the request the mock receiver saw from the transmitter.
     */
    protected static final class CapturedPush {
        final String body;
        final String authorizationHeader;
        final String contentType;

        CapturedPush(String body, String authorizationHeader, String contentType) {
            this.body = body;
            this.authorizationHeader = authorizationHeader;
            this.contentType = contentType;
        }
    }

    public static class PushFailureKeycloakServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            config.features(Profile.Feature.SSF);
            config.log().categoryLevel("org.keycloak.protocol.ssf", "DEBUG");
            // Tighten the connect timeout so the unreachable-receiver test
            // fails fast instead of waiting for the OS-level default.
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS, "500");
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS, "500");
            // Rate limiter isn't exercised here but turn it off so the
            // oauth triggers aren't subject to the 60 s default.
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_MIN_VERIFICATION_INTERVAL_SECONDS, "0");
            // Async pushes (and retries) flow through the outbox — tick
            // the drainer fast so the PUSH_WAIT_SECONDS awaits see the
            // first attempt and at least one retry within the window.
            config.spiOption("ssf-transmitter", "default",
                    DefaultSsfTransmitterProviderFactory.CONFIG_OUTBOX_DRAINER_INTERVAL, "500ms");
            // Test pushes to a local mock server on a loopback URL (http://127.0.0.1:NNNN/...).
            // Relax the http-scheme + private-host gate so the mock URL is accepted; the
            // per-client ssf.validPushUrls allow-list configured on each receiver below
            // is still the SSRF defence.
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_ALLOW_INSECURE_PUSH_TARGETS, "true");
            return configured;
        }
    }

    public static class PushFailureRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-transmitter-push-failure");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");

            realm.eventsEnabled(true);
            realm.adminEventsEnabled(true);
            realm.eventsListeners("jboss-logging", "ssf-events");

            realm.users(
                    UserBuilder.create(TEST_USER)
                            .email(TEST_USER + "@local.test")
                            .firstName("Fail")
                            .lastName("Tester")
                            .enabled(true)
                            .password(TEST_PASSWORD)
                            .build()
            );

            realm.clients(
                    ClientBuilder.create(RECEIVER_ERROR)
                            .secret(RECEIVER_ERROR_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "http://127.0.0.1:8500/*")
                            .attribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "ALL")
                            .build()
            );

            realm.clients(
                    ClientBuilder.create(RECEIVER_UNREACH)
                            .secret(RECEIVER_UNREACH_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "http://127.0.0.1:8500/*")
                            .attribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "ALL")
                            .build()
            );

            realm.clients(
                    ClientBuilder.create(RECEIVER_HEALTHY)
                            .secret(RECEIVER_HEALTHY_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "http://127.0.0.1:8500/*")
                            .attribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "ALL")
                            .build()
            );

            return realm;
        }
    }
}
