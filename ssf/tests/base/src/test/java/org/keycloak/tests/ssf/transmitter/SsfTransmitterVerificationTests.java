package org.keycloak.tests.ssf.transmitter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.caep.CaepCredentialChange;
import org.keycloak.ssf.event.stream.SsfStreamVerificationEvent;
import org.keycloak.ssf.transmitter.SsfScopes;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.StreamDeliveryConfig;
import org.keycloak.ssf.transmitter.stream.StreamVerificationRequest;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.support.SsfTransmitterUrls;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.server.DefaultKeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.HttpServerUtil;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the SSF Transmitter stream verification flow exposed at
 * {@code /realms/{realm}/ssf/transmitter/verify}.
 *
 * <p>Each test creates a stream that points its push endpoint at an in-process
 * mock receiver {@link HttpServer} that captures incoming SETs into a blocking
 * queue. The tests then trigger verification (either receiver- or
 * transmitter-initiated) and assert that the dispatched SET is well-formed
 * and carries the expected {@code https://schemas.openid.net/secevent/ssf/event-type/verification}
 * event.
 *
 * <p>Two receiver clients are registered by {@link VerificationRealm}:
 * <ul>
 *     <li>{@link #RECEIVER_RCV_INITIATED} — auto-verify disabled (the default);
 *         used to exercise the {@code /verify} endpoint and the min-verification-
 *         interval rate limit.</li>
 *     <li>{@link #RECEIVER_TXMIT_INITIATED} — auto-verify enabled with a 200 ms
 *         dispatch delay; used to test that the transmitter proactively
 *         dispatches a verification SET shortly after stream creation.</li>
 * </ul>
 */
@KeycloakIntegrationTest(config = SsfTransmitterVerificationTests.VerificationKeycloakServerConfig.class)
public class SsfTransmitterVerificationTests {

    static final String RECEIVER_RCV_INITIATED = "ssf-receiver-rcv-initiated";
    static final String RECEIVER_RCV_INITIATED_SECRET = "receiver-rcv-secret";

    static final String RECEIVER_TXMIT_INITIATED = "ssf-receiver-txmit-initiated";
    static final String RECEIVER_TXMIT_INITIATED_SECRET = "receiver-txmit-secret";

    static final String PUSH_CONTEXT_PATH = "/ssf/push";
    static final String MOCK_RECEIVER_BASE_URL = "http://127.0.0.1:8500";
    static final String MOCK_PUSH_ENDPOINT = MOCK_RECEIVER_BASE_URL + PUSH_CONTEXT_PATH;
    static final String EXPECTED_PUSH_AUTH_HEADER = "Bearer dummy-verification-receiver";

    /**
     * Must be larger than the transmitter's async dispatch + socket timeouts
     * but small enough to keep the suite fast.
     */
    static final long PUSH_WAIT_SECONDS = 5;

    @InjectRealm(config = VerificationRealm.class)
    ManagedRealm realm;

    @InjectSimpleHttp
    SimpleHttp http;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectHttpServer
    HttpServer mockReceiverServer;

    private final BlockingQueue<CapturedPush> pushes = new LinkedBlockingQueue<>();

    @BeforeEach
    public void setup() {
        pushes.clear();

        mockReceiverServer.createContext(PUSH_CONTEXT_PATH, new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try (InputStream is = exchange.getRequestBody()) {
                    CapturedPush captured = new CapturedPush(
                            new String(is.readAllBytes(), StandardCharsets.UTF_8),
                            exchange.getRequestHeaders().getFirst("Authorization"),
                            exchange.getRequestHeaders().getFirst("Content-Type"));
                    pushes.add(captured);
                }
                HttpServerUtil.sendResponse(exchange, 202, Map.of());
            }
        });

        // The SSF scopes are auto-created on RealmPostCreateEvent, i.e. after
        // the realm representation has been imported; assign them to each
        // receiver client from here so the CC grant can request them.
        assignOptionalClientScopes(RECEIVER_RCV_INITIATED,
                SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
        assignOptionalClientScopes(RECEIVER_TXMIT_INITIATED,
                SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
    }

    @AfterEach
    public void cleanup() {
        List.of(RECEIVER_RCV_INITIATED, RECEIVER_TXMIT_INITIATED)
                .forEach(this::bestEffortDeleteStream);

        try {
            mockReceiverServer.removeContext(PUSH_CONTEXT_PATH);
        } catch (IllegalArgumentException ignored) {
            // context already removed
        }
    }

    @Test
    public void testReceiverInitiatedVerification() throws Exception {

        String token = obtainManageAndReadToken(RECEIVER_RCV_INITIATED, RECEIVER_RCV_INITIATED_SECRET);

        StreamConfig createdStream = createPushStream(token, Set.of(CaepCredentialChange.TYPE));

        String state = "verify-state-" + UUID.randomUUID();
        StreamVerificationRequest request = new StreamVerificationRequest();
        request.setStreamId(createdStream.getStreamId());
        request.setState(state);

        try (SimpleHttpResponse response = http.doPost(verificationEndpoint())
                .json(request)
                .auth(token)
                .asResponse()) {
            Assertions.assertEquals(204, response.getStatus(),
                    "/verify should accept the request and return 204");
        }

        CapturedPush captured = awaitPush();
        assertVerificationSet(captured, createdStream, state);
    }

    @Test
    public void testTransmitterInitiatedVerificationOnStreamCreate() throws Exception {

        String token = obtainManageAndReadToken(RECEIVER_TXMIT_INITIATED, RECEIVER_TXMIT_INITIATED_SECRET);

        // Creating the stream should (after ~200 ms) cause the transmitter
        // to dispatch a verification SET without a state field.
        StreamConfig createdStream = createPushStream(token, Set.of(CaepCredentialChange.TYPE));

        CapturedPush captured = awaitPush();
        JsonNode set = decodeSet(captured);

        Assertions.assertEquals(realm.getBaseUrl(), set.get("iss").asText());
        Assertions.assertEquals(createdStream.getAudience(), extractAudience(set),
                "aud in the SET should match the stream audience");

        JsonNode verificationEvent = set.path("events").path(SsfStreamVerificationEvent.TYPE);
        Assertions.assertFalse(verificationEvent.isMissingNode(),
                "SET should carry the SSF stream verification event");
        // Per the SSF spec, transmitter-initiated verification events MUST NOT
        // include a state parameter.
        Assertions.assertTrue(verificationEvent.path("state").isMissingNode()
                        || verificationEvent.path("state").isNull(),
                "transmitter-initiated verification event must not carry a state field");

        Assertions.assertEquals(EXPECTED_PUSH_AUTH_HEADER, captured.authorizationHeader,
                "push authorization header should be forwarded verbatim to the receiver");
        Assertions.assertEquals(Ssf.APPLICATION_SECEVENT_JWT_TYPE, captured.contentType,
                "push content type should be application/secevent+jwt");
    }

    // --- helpers ---------------------------------------------------------

    protected void assertVerificationSet(CapturedPush captured, StreamConfig stream, String expectedState) throws JWSInputException, IOException {
        JsonNode set = decodeSet(captured);

        Assertions.assertTrue(set.hasNonNull("jti"), "SET must carry a jti");
        Assertions.assertEquals(realm.getBaseUrl(), set.get("iss").asText(),
                "iss should match the realm base URL");
        Set<String> audClaim = extractAudience(set);
        Assertions.assertEquals(stream.getAudience(), audClaim,
                "aud in the SET should match the stream audience");

        JsonNode verificationEvent = set.path("events").path(SsfStreamVerificationEvent.TYPE);
        Assertions.assertFalse(verificationEvent.isMissingNode(),
                "SET should carry the SSF stream verification event");
        Assertions.assertEquals(expectedState, verificationEvent.path("state").asText(null),
                "verification event should echo the state supplied by the receiver");

        Assertions.assertEquals(EXPECTED_PUSH_AUTH_HEADER, captured.authorizationHeader,
                "push authorization header should be forwarded verbatim to the receiver");
        Assertions.assertEquals(Ssf.APPLICATION_SECEVENT_JWT_TYPE, captured.contentType,
                "push content type should be application/secevent+jwt");
    }

    /**
     * Parses the captured JWS payload as a raw {@link JsonNode} instead of
     * {@code SsfSecurityEventToken}. The latter relies on
     * {@code SsfEventMapJsonDeserializer}, which calls
     * {@code Ssf.events().getRegistry()} and therefore needs a live Keycloak
     * session on the current thread — we don't have one in the test JVM.
     * Signature verification against the realm JWKS is covered by a separate
     * integration test; here we only care about the payload.
     */
    protected JsonNode decodeSet(CapturedPush captured) throws JWSInputException, IOException {
        Assertions.assertNotNull(captured, "expected a pushed SET but the queue was empty");
        JWSInput jws = new JWSInput(captured.body);
        return JsonSerialization.readValue(jws.getContent(), JsonNode.class);
    }

    protected Set<String> extractAudience(JsonNode set) {
        JsonNode aud = set.get("aud");
        Assertions.assertNotNull(aud, "SET should carry an aud claim");
        if (aud.isArray()) {
            Set<String> result = new java.util.LinkedHashSet<>();
            aud.forEach(element -> result.add(element.asText()));
            return result;
        }
        return Set.of(aud.asText());
    }

    protected CapturedPush awaitPush() throws InterruptedException {
        CapturedPush captured = pushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNotNull(captured,
                () -> "expected a push within " + PUSH_WAIT_SECONDS + "s but the mock receiver saw nothing");
        return captured;
    }

    protected StreamConfig createPushStream(String token, Set<String> eventsRequested) throws IOException {
        StreamDeliveryConfig delivery = new StreamDeliveryConfig();
        delivery.setMethod(Ssf.DELIVERY_METHOD_PUSH_URI);
        delivery.setEndpointUrl(MOCK_PUSH_ENDPOINT);
        delivery.setAuthorizationHeader(EXPECTED_PUSH_AUTH_HEADER);

        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setDelivery(delivery);
        streamConfig.setEventsRequested(eventsRequested);
        streamConfig.setDescription("Verification integration test");

        try (SimpleHttpResponse response = http.doPost(streamsEndpoint())
                .json(streamConfig)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(201, response.getStatus(), "stream creation should succeed");
            return response.asJson(StreamConfig.class);
        }
    }

    protected String streamsEndpoint() {
        return SsfTransmitterUrls.getStreamsEndpointUrl(realm.getBaseUrl());
    }

    protected String verificationEndpoint() {
        return SsfTransmitterUrls.getStreamVerificationEndpointUrl(realm.getBaseUrl());
    }

    protected String obtainManageAndReadToken(String clientId, String secret) throws IOException {
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
            // 204 / 404 are both fine.
        } catch (IOException e) {
            // best-effort
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

    public static class VerificationKeycloakServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            config.features(Profile.Feature.SSF);
            config.log().categoryLevel("org.keycloak.protocol.ssf", "DEBUG");
            // Disable the min verification interval rate limiter so tests
            // in this class aren't subject to the default 60 s window.
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_MIN_VERIFICATION_INTERVAL_SECONDS, "0");
            // Test pushes to a local mock server on a loopback URL (http://127.0.0.1:NNNN/...).
            // Relax the http-scheme + private-host gate so the mock URL is accepted; the
            // per-client ssf.validPushUrls allow-list configured on each receiver below
            // is still the SSRF defence.
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_ALLOW_INSECURE_PUSH_TARGETS, "true");
            return configured;
        }
    }

    public static class VerificationRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-transmitter-verification");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");

            // Auto-verify defaults to off; this client exercises the
            // /verify endpoint and the min-verification-interval rate
            // limit without a post-create auto-fire.
            realm.clients(
                    ClientBuilder.create(RECEIVER_RCV_INITIATED)
                            .secret(RECEIVER_RCV_INITIATED_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "http://127.0.0.1:8500/*")
                            .build()
            );

            // Auto-verify enabled with a short dispatch delay; tests that
            // the transmitter proactively dispatches a verification SET
            // shortly after stream creation.
            realm.clients(
                    ClientBuilder.create(RECEIVER_TXMIT_INITIATED)
                            .secret(RECEIVER_TXMIT_INITIATED_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "http://127.0.0.1:8500/*")
                            .attribute(ClientStreamStore.SSF_AUTO_VERIFY_STREAM_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VERIFICATION_DELAY_MILLIS_KEY, "200")
                            .build()
            );

            return realm;
        }
    }
}
