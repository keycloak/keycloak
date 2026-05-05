package org.keycloak.tests.ssf.transmitter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
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
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.caep.CaepCredentialChange;
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
 * Integration tests for RFC 8936 poll-based SET delivery.
 *
 * <p>The test realm carries:
 * <ul>
 *     <li>{@link #RECEIVER_POLL} — POLL receiver, default test subject. Stream
 *         is created on demand inside each test.</li>
 *     <li>{@link #RECEIVER_POLL_OTHER} — second POLL receiver in the same
 *         realm, used to exercise cross-receiver path-mismatch ownership
 *         checks (a poll URL that names another receiver's
 *         clientId / streamId must collapse to a silent 404).</li>
 *     <li>{@link #RECEIVER_PUSH_MIXED} — PUSH receiver in the same realm,
 *         used to confirm the dispatcher routes events to the correct
 *         outbox path per stream and that PUSH and POLL receivers
 *         coexist in one realm.</li>
 * </ul>
 *
 * <p>Default subjects is {@code ALL} so the dispatcher's subject filter
 * doesn't drop the test events without us having to subscribe each user
 * — except for {@link #subjectFilter_doesNotEnqueueUnsubscribedSubjects}
 * which exercises a NONE-mode receiver explicitly.
 */
@KeycloakIntegrationTest(config = SsfTransmitterPollDeliveryTests.PollDeliveryKeycloakServerConfig.class)
public class SsfTransmitterPollDeliveryTests {

    static final String RECEIVER_POLL = "ssf-receiver-poll";
    static final String RECEIVER_POLL_SECRET = "receiver-poll-secret";

    static final String RECEIVER_POLL_OTHER = "ssf-receiver-poll-other";
    static final String RECEIVER_POLL_OTHER_SECRET = "receiver-poll-other-secret";

    static final String RECEIVER_POLL_NONE = "ssf-receiver-poll-none";
    static final String RECEIVER_POLL_NONE_SECRET = "receiver-poll-none-secret";

    static final String RECEIVER_PUSH_MIXED = "ssf-receiver-push-mixed";
    static final String RECEIVER_PUSH_MIXED_SECRET = "receiver-push-mixed-secret";

    static final String TEST_USER = "polltester";
    static final String TEST_PASSWORD = "test";

    static final String UNSUBSCRIBED_USER = "unsubscribed";
    static final String UNSUBSCRIBED_PASSWORD = "test";

    static final String PUSH_CONTEXT_PATH = "/ssf/push-poll-mixed";
    static final String MOCK_PUSH_ENDPOINT = "http://127.0.0.1:8500" + PUSH_CONTEXT_PATH;
    static final String EXPECTED_PUSH_AUTH_HEADER = "Bearer dummy-poll-mixed-receiver";

    static final long PUSH_WAIT_SECONDS = 5;

    @InjectRealm(config = PollDeliveryRealm.class)
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

        // SSF scopes are auto-created by RealmPostCreateEvent after client
        // import, so they have to be assigned via the admin API rather
        // than declared on the realm config.
        assignOptionalClientScopes(RECEIVER_POLL, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
        assignOptionalClientScopes(RECEIVER_POLL_OTHER, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
        assignOptionalClientScopes(RECEIVER_POLL_NONE, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
        assignOptionalClientScopes(RECEIVER_PUSH_MIXED, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
    }

    @AfterEach
    public void cleanup() {
        List.of(RECEIVER_POLL, RECEIVER_POLL_OTHER, RECEIVER_POLL_NONE, RECEIVER_PUSH_MIXED)
                .forEach(this::bestEffortDeleteStream);
        try {
            mockReceiverServer.removeContext(PUSH_CONTEXT_PATH);
        } catch (IllegalArgumentException ignored) {
        }
    }

    // --- happy path ----------------------------------------------------

    @Test
    public void poll_dispatcherEnqueuesAndPollReturnsAndAcksClearTheQueue() throws Exception {

        String token = obtainReceiverToken(RECEIVER_POLL, RECEIVER_POLL_SECRET);
        StreamConfig stream = createPollStream(token, Set.of(CaepSessionRevoked.TYPE));

        // Sanity: the transmitter wrote the spec-mandated poll URL into
        // delivery.endpoint_url on the create response.
        Assertions.assertEquals(
                expectedPollUrl(RECEIVER_POLL, stream.getStreamId()),
                stream.getDelivery().getEndpointUrl(),
                "stream-create response must populate delivery.endpoint_url with the transmitter-owned poll URL");

        // Trigger a real LOGOUT user event — the SSF event listener
        // builds a CaepSessionRevoked SET, and (since the stream is on
        // POLL) the dispatcher enqueues it into the outbox instead of
        // pushing.
        triggerUserLogout(TEST_USER, TEST_PASSWORD);

        // First poll: pulls the pending SET.
        JsonNode firstPoll = poll(token, RECEIVER_POLL, stream.getStreamId(),
                pollBody(null, true, List.of()));
        JsonNode firstSets = firstPoll.path("sets");
        Assertions.assertTrue(firstSets.isObject() && firstSets.size() == 1,
                "first poll should return exactly the one enqueued SET");
        Assertions.assertFalse(firstPoll.path("moreAvailable").asBoolean(),
                "moreAvailable should be false when the returned batch is below maxEvents");

        String jti = firstSets.fieldNames().next();
        JsonNode set = decodeSet(firstSets.path(jti).asText());
        Assertions.assertEquals(realm.getBaseUrl(), set.get("iss").asText(),
                "polled SET should carry the realm issuer");
        Assertions.assertTrue(set.path("events").has(CaepSessionRevoked.TYPE),
                "polled SET should carry the CAEP session-revoked event");

        // Second poll WITH ack of the jti from the first poll → the
        // outbox row should transition to DELIVERED and the second
        // poll's sets map should be empty.
        JsonNode secondPoll = poll(token, RECEIVER_POLL, stream.getStreamId(),
                pollBody(null, true, List.of(jti)));
        Assertions.assertTrue(secondPoll.path("sets").isObject() && secondPoll.path("sets").isEmpty(),
                "after acking the jti, the next poll must not return it again");

        // Third poll (no ack, nothing pending) — also empty. Belt and
        // braces guard against the ack accidentally re-enqueuing.
        JsonNode thirdPoll = poll(token, RECEIVER_POLL, stream.getStreamId(),
                pollBody(null, true, List.of()));
        Assertions.assertTrue(thirdPoll.path("sets").isObject() && thirdPoll.path("sets").isEmpty(),
                "third poll with no pending events must return empty sets");
    }

    @Test
    public void poll_ackOfAlreadyAckedJti_isIdempotent() throws Exception {

        String token = obtainReceiverToken(RECEIVER_POLL, RECEIVER_POLL_SECRET);
        StreamConfig stream = createPollStream(token, Set.of(CaepSessionRevoked.TYPE));

        triggerUserLogout(TEST_USER, TEST_PASSWORD);

        JsonNode batch = poll(token, RECEIVER_POLL, stream.getStreamId(),
                pollBody(null, true, List.of()));
        Assertions.assertEquals(1, batch.path("sets").size());
        String jti = batch.path("sets").fieldNames().next();

        // Two consecutive acks of the same jti — the second is the
        // idempotency case (row is already DELIVERED). Endpoint must
        // not error, and the sets map must remain empty.
        for (int i = 0; i < 2; i++) {
            JsonNode response = poll(token, RECEIVER_POLL, stream.getStreamId(),
                    pollBody(null, true, List.of(jti)));
            Assertions.assertEquals(0, response.path("sets").size(),
                    "ack of already-acked jti must remain a no-op");
        }
    }

    // --- batching + moreAvailable -------------------------------------

    @Test
    public void poll_multiBatch_drainAcrossPolls() throws Exception {

        String token = obtainReceiverToken(RECEIVER_POLL, RECEIVER_POLL_SECRET);
        StreamConfig stream = createPollStream(token, Set.of(CaepSessionRevoked.TYPE));

        // Three logouts → three CaepSessionRevoked rows. Use maxEvents=2
        // so the first poll returns 2 with moreAvailable=true and the
        // second returns the remaining 1 with moreAvailable=false.
        triggerUserLogout(TEST_USER, TEST_PASSWORD);
        triggerUserLogout(TEST_USER, TEST_PASSWORD);
        triggerUserLogout(TEST_USER, TEST_PASSWORD);

        JsonNode firstPoll = poll(token, RECEIVER_POLL, stream.getStreamId(),
                pollBody(2, true, List.of()));
        Assertions.assertEquals(2, firstPoll.path("sets").size(),
                "first poll should return exactly maxEvents");
        Assertions.assertTrue(firstPoll.path("moreAvailable").asBoolean(),
                "moreAvailable should be true while the queue still holds rows");

        // Ack what we got and pull the rest.
        Set<String> firstAcks = jtiKeys(firstPoll.path("sets"));
        JsonNode secondPoll = poll(token, RECEIVER_POLL, stream.getStreamId(),
                pollBody(2, true, List.copyOf(firstAcks)));
        Assertions.assertEquals(1, secondPoll.path("sets").size(),
                "second poll should drain the remaining row");
        Assertions.assertFalse(secondPoll.path("moreAvailable").asBoolean(),
                "moreAvailable should be false once the queue is below the batch size");
    }

    // --- dispatch filters --------------------------------------------

    @Test
    public void dispatcher_unsupportedEvent_doesNotEnqueueForPoll() throws Exception {

        // Receiver only requests credential-change events; a logout
        // (session-revoked) must not show up in the poll output at all.
        String token = obtainReceiverToken(RECEIVER_POLL, RECEIVER_POLL_SECRET);
        StreamConfig stream = createPollStream(token, Set.of(CaepCredentialChange.TYPE));

        triggerUserLogout(TEST_USER, TEST_PASSWORD);

        JsonNode response = poll(token, RECEIVER_POLL, stream.getStreamId(),
                pollBody(null, true, List.of()));
        Assertions.assertEquals(0, response.path("sets").size(),
                "events_requested filter must run at enqueue time so unsubscribed types never reach the outbox");
    }

    @Test
    public void subjectFilter_doesNotEnqueueUnsubscribedSubjects() throws Exception {

        // RECEIVER_POLL_NONE has default_subjects=NONE — only explicitly
        // subscribed users should produce events. The unsubscribed user
        // logging out must not generate a poll-able SET.
        String token = obtainReceiverToken(RECEIVER_POLL_NONE, RECEIVER_POLL_NONE_SECRET);
        StreamConfig stream = createPollStream(token, Set.of(CaepSessionRevoked.TYPE));

        triggerUserLogout(UNSUBSCRIBED_USER, UNSUBSCRIBED_PASSWORD);

        JsonNode response = poll(token, RECEIVER_POLL_NONE, stream.getStreamId(),
                pollBody(null, true, List.of()));
        Assertions.assertEquals(0, response.path("sets").size(),
                "subject filter must run at enqueue time so unsubscribed subjects never reach the outbox");
    }

    // --- mixed PUSH + POLL receivers in same realm --------------------

    @Test
    public void mixedDelivery_pushAndPollReceiversCoexist() throws Exception {

        // PUSH receiver — same realm, separate stream.
        String pushToken = obtainReceiverToken(RECEIVER_PUSH_MIXED, RECEIVER_PUSH_MIXED_SECRET);
        createPushStream(pushToken, Set.of(CaepSessionRevoked.TYPE));

        // POLL receiver — same realm, separate stream.
        String pollToken = obtainReceiverToken(RECEIVER_POLL, RECEIVER_POLL_SECRET);
        StreamConfig pollStream = createPollStream(pollToken, Set.of(CaepSessionRevoked.TYPE));

        // One user event fans out to both streams via the dispatcher.
        triggerUserLogout(TEST_USER, TEST_PASSWORD);

        // PUSH receiver: drained by the outbox drainer + delivered to
        // the mock HTTP endpoint.
        String pushed = pushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNotNull(pushed,
                "PUSH receiver must receive the pushed SET via its mock endpoint");

        // POLL receiver: pulls the same logical event via /poll.
        JsonNode polled = poll(pollToken, RECEIVER_POLL, pollStream.getStreamId(),
                pollBody(null, true, List.of()));
        Assertions.assertEquals(1, polled.path("sets").size(),
                "POLL receiver must see the same logical event via the poll endpoint");
    }

    // --- NACK (setErrs) ----------------------------------------------

    @Test
    public void poll_setErrs_movesRowsToDeadLetterAndOutOfPoll() throws Exception {

        String token = obtainReceiverToken(RECEIVER_POLL, RECEIVER_POLL_SECRET);
        StreamConfig stream = createPollStream(token, Set.of(CaepSessionRevoked.TYPE));

        triggerUserLogout(TEST_USER, TEST_PASSWORD);

        // First poll: pull the SET so we know the jti.
        JsonNode firstPoll = poll(token, RECEIVER_POLL, stream.getStreamId(),
                pollBody(null, true, List.of()));
        Assertions.assertEquals(1, firstPoll.path("sets").size());
        String jti = firstPoll.path("sets").fieldNames().next();

        // Second poll: NACK the jti via setErrs. Receiver's NACK
        // descriptor (per RFC 8936 §2.1) carries err + description.
        Map<String, Object> body = pollBodyAsMap(null, true, List.of());
        body.put("setErrs", Map.of(jti,
                Map.of("err", "invalid_issuer",
                       "description", "test NACK reason")));
        try (SimpleHttpResponse response = http.doPost(pollEndpoint(RECEIVER_POLL, stream.getStreamId()))
                .json(body)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            int status = response.getStatus();
            Assertions.assertEquals(200, status, "NACK poll should return 200; was " + status);
            JsonNode resp = response.asJson();
            Assertions.assertEquals(0, resp.path("sets").size(),
                    "the NACK'd row must not be returned again on the same poll");
        }

        // Third poll: row should not come back — it's now DEAD_LETTER,
        // not PENDING, so the read query skips it.
        JsonNode thirdPoll = poll(token, RECEIVER_POLL, stream.getStreamId(),
                pollBody(null, true, List.of()));
        Assertions.assertEquals(0, thirdPoll.path("sets").size(),
                "DEAD_LETTER row must not reappear in subsequent polls");
    }

    // --- batch caps --------------------------------------------------

    @Test
    public void poll_ackOverCap_returns400() throws Exception {

        String token = obtainReceiverToken(RECEIVER_POLL, RECEIVER_POLL_SECRET);
        StreamConfig stream = createPollStream(token, Set.of(CaepSessionRevoked.TYPE));

        // 1001 > MAX_BATCH_CAP (1000) → 400 invalid_request, no DB
        // work performed. Build a fake jti list of the cap+1 size; the
        // resource rejects on size alone before looking anything up.
        List<String> oversized = new java.util.ArrayList<>(1001);
        for (int i = 0; i < 1001; i++) {
            oversized.add("urn:ietf:params:secevent:txn:fake-" + i);
        }
        try (SimpleHttpResponse response = http.doPost(pollEndpoint(RECEIVER_POLL, stream.getStreamId()))
                .json(pollBodyAsMap(null, true, oversized))
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(400, response.getStatus(),
                    "ack over the batch cap must be rejected with 400");
            Assertions.assertEquals("invalid_request", response.asJson().get("err").asText());
        }
    }

    @Test
    public void poll_setErrsOverCap_returns400() throws Exception {

        String token = obtainReceiverToken(RECEIVER_POLL, RECEIVER_POLL_SECRET);
        StreamConfig stream = createPollStream(token, Set.of(CaepSessionRevoked.TYPE));

        Map<String, Map<String, Object>> oversized = new java.util.LinkedHashMap<>();
        for (int i = 0; i < 1001; i++) {
            oversized.put("urn:ietf:params:secevent:txn:fake-" + i,
                    Map.of("err", "invalid_issuer", "description", "x"));
        }
        Map<String, Object> body = pollBodyAsMap(null, true, List.of());
        body.put("setErrs", oversized);

        try (SimpleHttpResponse response = http.doPost(pollEndpoint(RECEIVER_POLL, stream.getStreamId()))
                .json(body)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(400, response.getStatus(),
                    "setErrs over the batch cap must be rejected with 400");
            Assertions.assertEquals("invalid_request", response.asJson().get("err").asText());
        }
    }

    // --- ownership / 404 cases ---------------------------------------

    @Test
    public void poll_crossReceiverPath_returns404() throws Exception {

        // Receiver A creates a stream; receiver B tries to poll A's URL
        // with B's own token. Path clientId belongs to A, token belongs
        // to B → silent 404, no enumeration oracle.
        String aToken = obtainReceiverToken(RECEIVER_POLL, RECEIVER_POLL_SECRET);
        StreamConfig aStream = createPollStream(aToken, Set.of(CaepSessionRevoked.TYPE));

        String bToken = obtainReceiverToken(RECEIVER_POLL_OTHER, RECEIVER_POLL_OTHER_SECRET);

        try (SimpleHttpResponse response = http.doPost(pollEndpoint(RECEIVER_POLL, aStream.getStreamId()))
                .json(pollBodyAsMap(null, true, List.of()))
                .auth(bToken)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(404, response.getStatus(),
                    "polling another receiver's URL must collapse to a silent 404");
        }
    }

    @Test
    public void poll_pathStreamIdMismatch_returns404() throws Exception {

        // Receiver A and receiver B both create streams. Receiver A
        // polls its own clientId path but B's stream id. Token's
        // clientId matches, but the stream id doesn't belong to A → 404.
        String aToken = obtainReceiverToken(RECEIVER_POLL, RECEIVER_POLL_SECRET);
        createPollStream(aToken, Set.of(CaepSessionRevoked.TYPE));

        String bToken = obtainReceiverToken(RECEIVER_POLL_OTHER, RECEIVER_POLL_OTHER_SECRET);
        StreamConfig bStream = createPollStream(bToken, Set.of(CaepSessionRevoked.TYPE));

        try (SimpleHttpResponse response = http.doPost(pollEndpoint(RECEIVER_POLL, bStream.getStreamId()))
                .json(pollBodyAsMap(null, true, List.of()))
                .auth(aToken)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(404, response.getStatus(),
                    "stream id from another receiver in our own client path must collapse to 404");
        }
    }

    @Test
    public void poll_unauthenticated_returns401() throws Exception {

        // A bearer-less request is rejected by Keycloak's
        // BearerTokenAuthenticator at the pre-resource layer — this is
        // the standard 401, not the silent 404 we use for ownership
        // mismatches.
        String token = obtainReceiverToken(RECEIVER_POLL, RECEIVER_POLL_SECRET);
        StreamConfig stream = createPollStream(token, Set.of(CaepSessionRevoked.TYPE));

        try (SimpleHttpResponse response = http.doPost(pollEndpoint(RECEIVER_POLL, stream.getStreamId()))
                .json(pollBodyAsMap(null, true, List.of()))
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(401, response.getStatus(),
                    "unauthenticated poll must be rejected at the auth layer");
        }
    }

    // --- stream delete cascade ---------------------------------------

    @Test
    public void streamDelete_cascadePurgesPendingPollRows() throws Exception {

        String token = obtainReceiverToken(RECEIVER_POLL, RECEIVER_POLL_SECRET);
        StreamConfig stream = createPollStream(token, Set.of(CaepSessionRevoked.TYPE));

        triggerUserLogout(TEST_USER, TEST_PASSWORD);

        // Sanity: the row is enqueued and visible to a poll.
        JsonNode beforeDelete = poll(token, RECEIVER_POLL, stream.getStreamId(),
                pollBody(null, true, List.of()));
        Assertions.assertEquals(1, beforeDelete.path("sets").size());

        // Delete the stream via admin (the receiver-facing DELETE
        // requires a valid streamId on the client which we have).
        deleteStreamViaAdmin(RECEIVER_POLL);

        // The receiver re-creates a stream. The new stream id is
        // different, and any rows from the previous stream must have
        // been purged so they don't come back through the new poll
        // endpoint.
        StreamConfig recreated = createPollStream(token, Set.of(CaepSessionRevoked.TYPE));
        Assertions.assertNotEquals(stream.getStreamId(), recreated.getStreamId(),
                "recreated stream must have a fresh streamId");

        JsonNode afterRecreate = poll(token, RECEIVER_POLL, recreated.getStreamId(),
                pollBody(null, true, List.of()));
        Assertions.assertEquals(0, afterRecreate.path("sets").size(),
                "stream-delete must cascade-purge outstanding poll rows so they don't appear on the new stream");
    }

    // --- helpers ------------------------------------------------------

    protected String pollEndpoint(String clientId, String streamId) {
        return SsfTransmitterUrls.getPollEndpointUrl(realm.getBaseUrl(), clientId, streamId);
    }

    protected String expectedPollUrl(String clientId, String streamId) {
        return SsfTransmitterUrls.getPollEndpointUrl(realm.getBaseUrl(), clientId, streamId);
    }

    /**
     * Wraps the poll endpoint call so each test reads as a one-liner.
     * Returns the parsed response body — caller asserts on
     * {@code sets} / {@code moreAvailable}.
     */
    protected JsonNode poll(String token, String clientId, String streamId, Map<String, Object> body) throws IOException {
        try (SimpleHttpResponse response = http.doPost(pollEndpoint(clientId, streamId))
                .json(body)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            int status = response.getStatus();
            Assertions.assertEquals(200, status, "poll should return 200 OK; was " + status);
            return response.asJson();
        }
    }

    protected Map<String, Object> pollBody(Integer maxEvents, boolean returnImmediately, List<String> ack) {
        return pollBodyAsMap(maxEvents, returnImmediately, ack);
    }

    protected Map<String, Object> pollBodyAsMap(Integer maxEvents, boolean returnImmediately, List<String> ack) {
        // HashMap (not Map.of) because maxEvents may be null and Map.of
        // rejects null values.
        Map<String, Object> body = new java.util.HashMap<>();
        if (maxEvents != null) {
            body.put("maxEvents", maxEvents);
        }
        body.put("returnImmediately", returnImmediately);
        body.put("ack", ack);
        return body;
    }

    protected Set<String> jtiKeys(JsonNode setsNode) {
        Set<String> keys = new HashSet<>();
        setsNode.fieldNames().forEachRemaining(keys::add);
        return keys;
    }

    protected JsonNode decodeSet(String encoded) throws Exception {
        JWSInput jws = new JWSInput(encoded);
        return JsonSerialization.readValue(jws.getContent(), JsonNode.class);
    }

    protected void triggerUserLogout(String username, String password) {
        AccessTokenResponse tokenResponse = oauthClient.passwordGrantRequest(username, password).send();
        Assertions.assertNotNull(tokenResponse.getAccessToken(),
                () -> "password grant should succeed for " + username);
        Assertions.assertNotNull(tokenResponse.getRefreshToken(),
                () -> "password grant should produce a refresh token for " + username);
        oauthClient.doLogout(tokenResponse.getRefreshToken());
    }

    protected StreamConfig createPollStream(String token, Set<String> eventsRequested) throws IOException {
        StreamDeliveryConfig delivery = new StreamDeliveryConfig();
        delivery.setMethod(Ssf.DELIVERY_METHOD_POLL_URI);
        // Receiver-supplied endpoint_url is intentionally null — the
        // transmitter generates the URL itself for poll streams.
        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setDelivery(delivery);
        streamConfig.setEventsRequested(eventsRequested);
        streamConfig.setDescription("Poll delivery integration test");

        try (SimpleHttpResponse response = http.doPost(streamsEndpoint())
                .json(streamConfig)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            int status = response.getStatus();
            Assertions.assertEquals(201, status, "stream create should return 201; was " + status);
            return response.asJson(StreamConfig.class);
        }
    }

    protected StreamConfig createPushStream(String token, Set<String> eventsRequested) throws IOException {
        StreamDeliveryConfig delivery = new StreamDeliveryConfig();
        delivery.setMethod(Ssf.DELIVERY_METHOD_PUSH_URI);
        delivery.setEndpointUrl(MOCK_PUSH_ENDPOINT);
        delivery.setAuthorizationHeader(EXPECTED_PUSH_AUTH_HEADER);

        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setDelivery(delivery);
        streamConfig.setEventsRequested(eventsRequested);
        streamConfig.setDescription("Mixed-delivery integration test (push side)");

        try (SimpleHttpResponse response = http.doPost(streamsEndpoint())
                .json(streamConfig)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(201, response.getStatus());
            return response.asJson(StreamConfig.class);
        }
    }

    protected String streamsEndpoint() {
        return SsfTransmitterUrls.getStreamsEndpointUrl(realm.getBaseUrl());
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
        deleteStreamViaAdminInternal(client.getClientId());
    }

    protected void deleteStreamViaAdmin(String clientId) {
        ClientRepresentation client = findClientByClientId(clientId);
        Assertions.assertNotNull(client, () -> "expected client '" + clientId + "' to exist");
        deleteStreamViaAdminInternal(client.getClientId());
    }

    protected void deleteStreamViaAdminInternal(String clientOauthId) {
        String adminStreamUrl = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + clientOauthId + "/stream";
        try (SimpleHttpResponse ignored = http.doDelete(adminStreamUrl)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .asResponse()) {
            // 204 / 404 both fine for the @AfterEach cleanup.
        } catch (IOException e) {
            // best-effort
        }
    }

    public static class PollDeliveryKeycloakServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            config.features(Profile.Feature.SSF);
            config.log().categoryLevel("org.keycloak.ssf", "DEBUG");
            // Verification rate limit relaxed — these tests don't
            // exercise /verify, but the stream-create flow can fire a
            // transmitter-initiated verification asynchronously, and a
            // 60s default can stretch test runtime.
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_MIN_VERIFICATION_INTERVAL_SECONDS, "0");
            // The PUSH side of the mixed-delivery test needs the
            // outbox drainer to tick well inside PUSH_WAIT_SECONDS, or
            // the await on the mock receiver times out.
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

    public static class PollDeliveryRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-transmitter-poll-delivery");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");

            realm.eventsEnabled(true);
            realm.adminEventsEnabled(true);
            realm.eventsListeners("jboss-logging", "ssf-events");

            realm.users(
                    UserBuilder.create(TEST_USER)
                            .email(TEST_USER + "@local.test")
                            .firstName("Polly")
                            .lastName("Tester")
                            .enabled(true)
                            .password(TEST_PASSWORD)
                            .build()
            );

            realm.users(
                    UserBuilder.create(UNSUBSCRIBED_USER)
                            .email(UNSUBSCRIBED_USER + "@local.test")
                            .firstName("Unsub")
                            .lastName("Tester")
                            .enabled(true)
                            .password(UNSUBSCRIBED_PASSWORD)
                            .build()
            );

            // POLL receiver, default ALL — the dispatcher's subject
            // filter is permissive so happy-path tests don't have to
            // pre-subscribe the test user.
            realm.clients(
                    ClientBuilder.create(RECEIVER_POLL)
                            .secret(RECEIVER_POLL_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "http://127.0.0.1:8500/*")
                            .attribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "ALL")
                            .build()
            );

            // Second POLL receiver — used only by the cross-receiver
            // ownership tests.
            realm.clients(
                    ClientBuilder.create(RECEIVER_POLL_OTHER)
                            .secret(RECEIVER_POLL_OTHER_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "http://127.0.0.1:8500/*")
                            .attribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "ALL")
                            .build()
            );

            // POLL receiver with default_subjects=NONE — used only by
            // the subject-filter test.
            realm.clients(
                    ClientBuilder.create(RECEIVER_POLL_NONE)
                            .secret(RECEIVER_POLL_NONE_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "http://127.0.0.1:8500/*")
                            .attribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "NONE")
                            .build()
            );

            // PUSH receiver — used only by the mixed-delivery test.
            realm.clients(
                    ClientBuilder.create(RECEIVER_PUSH_MIXED)
                            .secret(RECEIVER_PUSH_MIXED_SECRET)
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
