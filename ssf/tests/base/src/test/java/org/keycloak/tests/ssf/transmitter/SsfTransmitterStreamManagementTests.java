package org.keycloak.tests.ssf.transmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.caep.CaepCredentialChange;
import org.keycloak.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.ssf.transmitter.SsfScopes;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.StreamConfigInputRepresentation;
import org.keycloak.ssf.transmitter.stream.StreamConfigUpdateRepresentation;
import org.keycloak.ssf.transmitter.stream.StreamDeliveryConfig;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.support.SsfTransmitterUrls;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.server.DefaultKeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the SSF Transmitter Stream Management REST API exposed at
 * {@code /realms/{realm}/ssf/transmitter/streams}.
 *
 * <p>Each test uses one of three preconfigured receiver clients created by
 * {@link StreamManagementRealm}:
 * <ul>
 *     <li>{@link #RECEIVER_RW} — confidential client with both {@code ssf.read}
 *         and {@code ssf.manage} as optional scopes, used to exercise the
 *         happy paths.</li>
 *     <li>{@link #RECEIVER_RO} — confidential client with only {@code ssf.read}
 *         as optional scope, used to assert that stream management operations
 *         are gated on {@code ssf.manage}.</li>
 *     <li>{@link #RECEIVER_SCOPED} — confidential client with a custom
 *         {@code ssf.supportedEvents} attribute that restricts the events the
 *         transmitter is willing to deliver for this particular stream.</li>
 * </ul>
 *
 * <p>Between tests an {@link AfterEach} hook cleans up any stream registered
 * for any of the three receiver clients via the admin
 * {@code /admin/realms/{realm}/ssf/clients/{clientUuid}/stream} endpoint so
 * each test starts from a clean slate.
 */
@KeycloakIntegrationTest(config = SsfTransmitterStreamManagementTests.StreamManagementKeycloakServerConfig.class)
public class SsfTransmitterStreamManagementTests {

    static final String RECEIVER_RW = "ssf-receiver-rw";
    static final String RECEIVER_RW_SECRET = "receiver-rw-secret";

    static final String RECEIVER_RO = "ssf-receiver-ro";
    static final String RECEIVER_RO_SECRET = "receiver-ro-secret";

    static final String RECEIVER_SCOPED = "ssf-receiver-scoped";
    static final String RECEIVER_SCOPED_SECRET = "receiver-scoped-secret";

    static final String DUMMY_PUSH_ENDPOINT = "http://127.0.0.1:65535/ssf/push";
    static final String DUMMY_PUSH_AUTH_HEADER = "Bearer dummy-receiver-token";

    @InjectRealm(config = StreamManagementRealm.class)
    ManagedRealm realm;

    @InjectSimpleHttp
    SimpleHttp http;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @BeforeEach
    public void assignSsfScopesToReceiverClients() {
        // The SSF client scopes (ssf.read / ssf.manage) are created on the
        // RealmPostCreateEvent which fires *after* the realm representation
        // (including its clients) has been imported. As a result, optional
        // client scopes declared via the test framework's RealmConfig are
        // silently dropped because the scopes don't yet exist at import time.
        // Assign them here via the admin REST API once both clients and scopes
        // are present.
        assignOptionalClientScopes(RECEIVER_RW, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
        assignOptionalClientScopes(RECEIVER_RO, SsfScopes.SCOPE_SSF_READ);
        assignOptionalClientScopes(RECEIVER_SCOPED, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
    }

    @AfterEach
    public void cleanupStreams() {
        List.of(RECEIVER_RW, RECEIVER_RO, RECEIVER_SCOPED)
                .forEach(this::bestEffortDeleteStream);
    }

    @Test
    public void testCreateStreamRequiresSsfManageScope() throws IOException {

        // The read-only receiver only carries the ssf.read scope; without
        // ssf.manage the transmitter must refuse to create a stream.
        String roToken = obtainReceiverAccessToken(RECEIVER_RO, RECEIVER_RO_SECRET, SsfScopes.SCOPE_SSF_MANAGE);
        Assertions.assertNull(roToken,
                "client without ssf.manage should not be able to obtain a token with the ssf.manage scope");

        String readOnlyToken = obtainReceiverAccessToken(RECEIVER_RO, RECEIVER_RO_SECRET, SsfScopes.SCOPE_SSF_READ);
        Assertions.assertNotNull(readOnlyToken, "should obtain a token with just the ssf.read scope");

        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));

        try (SimpleHttpResponse response = postStream(readOnlyToken, request)) {
            Assertions.assertEquals(401, response.getStatus(),
                    "creating a stream without the ssf.manage scope should be rejected with 401");
        }
    }

    @Test
    public void testCreateStreamReturnsFullEventTypeUris() throws IOException {

        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);

        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(
                CaepCredentialChange.TYPE,
                CaepSessionRevoked.TYPE));

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus(),
                    "stream creation should succeed");

            StreamConfig created = response.asJson(StreamConfig.class);
            Assertions.assertNotNull(created.getStreamId(), "stream_id should be assigned by the transmitter");
            Assertions.assertEquals(realm.getBaseUrl(), created.getIssuer(),
                    "iss should match the realm base URL");
            Assertions.assertNotNull(created.getAudience());
            Assertions.assertFalse(created.getAudience().isEmpty(), "aud should be populated");

            // Regression guard for the alias -> URI resolution fix. The stream
            // config returned by the SSF endpoints must always carry the full
            // event type URIs even though the admin UI operates on aliases.
            Assertions.assertTrue(
                    created.getEventsSupported().contains(CaepCredentialChange.TYPE),
                    "events_supported should carry the full CAEP credential-change URI");
            Assertions.assertTrue(
                    created.getEventsSupported().contains(CaepSessionRevoked.TYPE),
                    "events_supported should carry the full CAEP session-revoked URI");
            Assertions.assertTrue(
                    created.getEventsDelivered().contains(CaepCredentialChange.TYPE),
                    "events_delivered should carry the full CAEP credential-change URI");
            Assertions.assertTrue(
                    created.getEventsDelivered().contains(CaepSessionRevoked.TYPE),
                    "events_delivered should carry the full CAEP session-revoked URI");
            Assertions.assertTrue(
                    created.getEventsRequested().contains(CaepCredentialChange.TYPE),
                    "events_requested should be echoed back as full URIs");

            Assertions.assertNotNull(created.getDelivery());
            Assertions.assertEquals(Ssf.DELIVERY_METHOD_PUSH_URI, created.getDelivery().getMethod());
            Assertions.assertEquals(DUMMY_PUSH_ENDPOINT, created.getDelivery().getEndpointUrl());
        }
    }

    @Test
    public void testDuplicateStreamRejected() throws IOException {

        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus(), "first stream creation should succeed");
        }

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(409, response.getStatus(),
                    "second stream creation by the same receiver should fail with 409");
        }
    }

    @Test
    public void testGetStreamByIdReturnsRegisteredStream() throws IOException {

        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepSessionRevoked.TYPE));

        String createdStreamId;
        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus());
            createdStreamId = response.asJson(StreamConfig.class).getStreamId();
        }

        try (SimpleHttpResponse response = http.doGet(streamsEndpoint())
                .param("stream_id", createdStreamId)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus());

            StreamConfig fetched = response.asJson(StreamConfig.class);
            Assertions.assertEquals(createdStreamId, fetched.getStreamId());
            Assertions.assertTrue(fetched.getEventsRequested().contains(CaepSessionRevoked.TYPE));
            Assertions.assertTrue(fetched.getEventsDelivered().contains(CaepSessionRevoked.TYPE));
        }
    }

    @Test
    public void testPatchStreamNarrowsRequestedEvents() throws IOException {

        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(
                CaepCredentialChange.TYPE,
                CaepSessionRevoked.TYPE));

        String streamId;
        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus());
            streamId = response.asJson(StreamConfig.class).getStreamId();
        }

        StreamConfigUpdateRepresentation patch = buildPushStreamRequest(Set.of(CaepSessionRevoked.TYPE));
        patch.setStreamId(streamId);

        try (SimpleHttpResponse response = patchStream(token, patch)) {
            Assertions.assertEquals(200, response.getStatus(), "PATCH should succeed");
            StreamConfig updated = response.asJson(StreamConfig.class);
            Assertions.assertEquals(Set.of(CaepSessionRevoked.TYPE), updated.getEventsRequested(),
                    "events_requested should be narrowed");
            Assertions.assertTrue(updated.getEventsDelivered().contains(CaepSessionRevoked.TYPE));
            Assertions.assertFalse(updated.getEventsDelivered().contains(CaepCredentialChange.TYPE),
                    "events_delivered should no longer include the dropped event");
        }
    }

    @Test
    public void testPatchStreamPreservesOmittedEventsRequested() throws IOException {

        // Exercises §8.1.1.3 merge semantics: a PATCH that only carries
        // description must not wipe events_requested. Guards against the
        // pre-refactor bug where updateStream unconditionally called
        // existingStream.setEventsRequested(patch.getEventsRequested()),
        // which clobbered events_requested to null on a description-only
        // update.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));

        String streamId;
        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus());
            streamId = response.asJson(StreamConfig.class).getStreamId();
        }

        StreamConfigUpdateRepresentation patch = new StreamConfigUpdateRepresentation();
        patch.setStreamId(streamId);
        patch.setDescription("updated description only");

        try (SimpleHttpResponse response = patchStream(token, patch)) {
            Assertions.assertEquals(200, response.getStatus(), "PATCH should succeed");
            StreamConfig updated = response.asJson(StreamConfig.class);
            Assertions.assertEquals("updated description only", updated.getDescription(),
                    "description should reflect the PATCH");
            Assertions.assertEquals(Set.of(CaepCredentialChange.TYPE), updated.getEventsRequested(),
                    "events_requested must be preserved when absent from the PATCH body");
            Assertions.assertTrue(updated.getEventsDelivered().contains(CaepCredentialChange.TYPE),
                    "events_delivered must be preserved when events_requested did not change");
        }
    }

    @Test
    public void testPatchStreamPreservesOmittedDescription() throws IOException {

        // Exercises §8.1.1.3 merge semantics: a PATCH that only narrows
        // events_requested must not wipe the existing description.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(
                CaepCredentialChange.TYPE,
                CaepSessionRevoked.TYPE));
        String originalDescription = request.getDescription();

        String streamId;
        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus());
            streamId = response.asJson(StreamConfig.class).getStreamId();
        }

        StreamConfigUpdateRepresentation patch = new StreamConfigUpdateRepresentation();
        patch.setStreamId(streamId);
        patch.setEventsRequested(Set.of(CaepSessionRevoked.TYPE));

        try (SimpleHttpResponse response = patchStream(token, patch)) {
            Assertions.assertEquals(200, response.getStatus(), "PATCH should succeed");
            StreamConfig updated = response.asJson(StreamConfig.class);
            Assertions.assertEquals(originalDescription, updated.getDescription(),
                    "description must be preserved when absent from the PATCH body");
            Assertions.assertEquals(Set.of(CaepSessionRevoked.TYPE), updated.getEventsRequested());
        }
    }

    @Test
    public void testPatchStreamRejectsTransmitterControlledFields() throws IOException {

        // §8.1.1.3: "Only the fields that the Receiver wishes to modify are
        // included in the patch object". A receiver attempting to change
        // transmitter-supplied fields such as iss/aud or the Keycloak
        // extension timestamps must be rejected with 400 rather than
        // silently ignored — ignoring them would make the request look
        // successful to a compromised/misbehaving receiver.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));

        String streamId;
        Integer originalCreatedAt;
        Set<String> originalAudience;
        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus());
            StreamConfig created = response.asJson(StreamConfig.class);
            streamId = created.getStreamId();
            originalCreatedAt = created.getCreatedAt();
            originalAudience = created.getAudience();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("stream_id", streamId);
        body.put("description", "attempted take-over");
        body.put("iss", "https://attacker.example.com");
        body.put("aud", Set.of("https://attacker.example.com"));
        body.put("kc_created_at", 0);

        try (SimpleHttpResponse response = http.doPatch(streamsEndpoint())
                .json(body)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(400, response.getStatus(),
                    "PATCH with transmitter-controlled fields must be rejected");
        }

        // Double-check: nothing on the stored stream actually changed.
        try (SimpleHttpResponse response = http.doGet(streamsEndpoint())
                .param("stream_id", streamId)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus());
            StreamConfig fetched = response.asJson(StreamConfig.class);
            Assertions.assertEquals(originalCreatedAt, fetched.getCreatedAt(),
                    "kc_created_at must not be altered by a rejected PATCH");
            Assertions.assertEquals(originalAudience, fetched.getAudience(),
                    "aud must not be altered by a rejected PATCH");
            Assertions.assertNotEquals("attempted take-over", fetched.getDescription(),
                    "a rejected PATCH must not leave partial writes behind");
        }
    }

    @Test
    public void testPatchStreamPreservesCreatedAtAndStampsUpdatedAt() throws IOException {

        // Regression for the timestamp plumbing: create stamps both
        // kc_created_at and kc_updated_at, update preserves kc_created_at
        // and refreshes kc_updated_at.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));

        Integer originalCreatedAt;
        String streamId;
        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus());
            StreamConfig created = response.asJson(StreamConfig.class);
            streamId = created.getStreamId();
            originalCreatedAt = created.getCreatedAt();
            Assertions.assertNotNull(originalCreatedAt,
                    "create must stamp kc_created_at");
            Assertions.assertNotNull(created.getUpdatedAt(),
                    "create must stamp kc_updated_at");
        }

        StreamConfigUpdateRepresentation patch = new StreamConfigUpdateRepresentation();
        patch.setStreamId(streamId);
        patch.setDescription("second revision");

        try (SimpleHttpResponse response = patchStream(token, patch)) {
            Assertions.assertEquals(200, response.getStatus());
            StreamConfig updated = response.asJson(StreamConfig.class);
            Assertions.assertEquals(originalCreatedAt, updated.getCreatedAt(),
                    "kc_created_at must not change across updates");
            Assertions.assertNotNull(updated.getUpdatedAt(),
                    "kc_updated_at must remain set after a PATCH");
        }
    }

    @Test
    public void testPutStreamRejectsReceiverSuppliedTransmitterFields() throws IOException {

        // §8.1.1.4: a receiver round-tripping a GET response must NOT include
        // transmitter-controlled fields (iss, aud, events_supported, the
        // kc_* extensions, …) in a PUT body. The wire DTO
        // StreamConfigUpdateRepresentation does not declare those fields, so
        // Jackson rejects the request at bind time with 400 and nothing on
        // the stored stream is touched.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));

        String streamId;
        Integer originalCreatedAt;
        Set<String> originalAudience;
        String originalIssuer;
        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus());
            StreamConfig created = response.asJson(StreamConfig.class);
            streamId = created.getStreamId();
            originalCreatedAt = created.getCreatedAt();
            originalAudience = created.getAudience();
            originalIssuer = created.getIssuer();
        }

        // Hand-built raw body so we can smuggle fields Jackson would never
        // write from a StreamConfigUpdateRepresentation instance.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("stream_id", streamId);
        body.put("description", "attempted take-over");
        body.put("events_requested", Set.of(CaepSessionRevoked.TYPE));
        body.put("iss", "https://attacker.example.com");
        body.put("aud", Set.of("https://attacker.example.com"));
        body.put("kc_created_at", 0);

        try (SimpleHttpResponse response = http.doPut(streamsEndpoint())
                .json(body)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(400, response.getStatus(),
                    "PUT with transmitter-controlled fields must be rejected at bind time");
        }

        // Stored stream must be unchanged.
        try (SimpleHttpResponse response = http.doGet(streamsEndpoint())
                .param("stream_id", streamId)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus());
            StreamConfig fetched = response.asJson(StreamConfig.class);
            Assertions.assertEquals(originalIssuer, fetched.getIssuer(),
                    "iss must not be altered by a rejected PUT");
            Assertions.assertEquals(originalAudience, fetched.getAudience(),
                    "aud must not be altered by a rejected PUT");
            Assertions.assertEquals(originalCreatedAt, fetched.getCreatedAt(),
                    "kc_created_at must not be altered by a rejected PUT");
            Assertions.assertNotEquals("attempted take-over", fetched.getDescription(),
                    "a rejected PUT must not leave partial writes behind");
        }
    }

    @Test
    public void testCreateStreamRejectsOversizedDescription() throws IOException {

        // description > 255 characters must be rejected with 400.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));
        request.setDescription("x".repeat(256));

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(400, response.getStatus(),
                    "oversized description must be rejected");
        }
    }

    @Test
    public void testCreateStreamRejectsOversizedEndpointUrl() throws IOException {

        // delivery.endpoint_url > 512 characters must be rejected with 400.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));
        request.getDelivery().setEndpointUrl("https://example.com/" + "x".repeat(520));

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(400, response.getStatus(),
                    "oversized delivery.endpoint_url must be rejected");
        }
    }

    @Test
    public void testCreateStreamRejectsOversizedAuthorizationHeader() throws IOException {

        // delivery.authorization_header > 1024 characters must be rejected with 400.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));
        request.getDelivery().setAuthorizationHeader("Bearer " + "x".repeat(1024));

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(400, response.getStatus(),
                    "oversized delivery.authorization_header must be rejected");
        }
    }

    @Test
    public void testCreateStreamRejectsTooManyEventsRequested() throws IOException {

        // events_requested with > 32 entries must be rejected with 400.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        Set<String> tooMany = new java.util.LinkedHashSet<>();
        for (int i = 0; i < 33; i++) {
            tooMany.add("https://example.com/event/" + i);
        }
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(tooMany);

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(400, response.getStatus(),
                    "events_requested exceeding 32 entries must be rejected");
        }
    }

    @Test
    public void testCreateStreamRejectsOversizedEventType() throws IOException {

        // An events_requested entry > 256 characters must be rejected with 400.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(
                Set.of("https://example.com/event/" + "x".repeat(260)));

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(400, response.getStatus(),
                    "oversized events_requested entry must be rejected");
        }
    }

    @Test
    public void testPatchStreamRejectsOversizedDescription() throws IOException {

        // A PATCH that tries to grow the description past the cap must be
        // rejected with 400 and leave the stored stream intact.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));

        String streamId;
        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus());
            streamId = response.asJson(StreamConfig.class).getStreamId();
        }

        StreamConfigUpdateRepresentation patch = new StreamConfigUpdateRepresentation();
        patch.setStreamId(streamId);
        patch.setDescription("x".repeat(256));

        try (SimpleHttpResponse response = patchStream(token, patch)) {
            Assertions.assertEquals(400, response.getStatus(),
                    "oversized description on PATCH must be rejected");
        }
    }

    @Test
    public void testLegacyStreamConfigBlobIsReadableAndMigratedOnUpdate() throws IOException {

        // Pre-refactor installations stored the whole StreamConfig as one
        // serialized-JSON client attribute. ClientStreamStore.extractStreamConfig
        // must still be able to read those blobs (forward-compat for existing
        // data), and the next storeStreamConfig() call (triggered here by a
        // PATCH) must migrate the stream onto the split-attribute layout —
        // including removing the legacy blob key so subsequent reads go
        // through the split path.
        String legacyStreamId = "legacy-stream-" + java.util.UUID.randomUUID();
        String legacyBlob = "{"
                + "\"stream_id\":\"" + legacyStreamId + "\","
                + "\"iss\":\"https://legacy.example.com/realms/test\","
                + "\"aud\":[\"https://legacy.example.com/receiver\"],"
                + "\"events_supported\":[\"" + CaepCredentialChange.TYPE + "\"],"
                + "\"events_requested\":[\"" + CaepCredentialChange.TYPE + "\"],"
                + "\"events_delivered\":[\"" + CaepCredentialChange.TYPE + "\"],"
                + "\"delivery\":{\"method\":\"" + Ssf.DELIVERY_METHOD_PUSH_URI + "\","
                + "\"endpoint_url\":\"" + DUMMY_PUSH_ENDPOINT + "\","
                + "\"authorization_header\":\"" + DUMMY_PUSH_AUTH_HEADER + "\"},"
                + "\"description\":\"legacy stream from pre-refactor data\","
                + "\"kc_status\":\"enabled\","
                + "\"kc_created_at\":100,"
                + "\"kc_updated_at\":100"
                + "}";

        ClientRepresentation rwClient = findClientByClientId(RECEIVER_RW);
        Assertions.assertNotNull(rwClient);
        rwClient.getAttributes().put(ClientStreamStore.SSF_STREAM_ID_KEY, legacyStreamId);
        rwClient.getAttributes().put(ClientStreamStore.SSF_STREAM_CONFIG_KEY, legacyBlob);
        rwClient.getAttributes().put(ClientStreamStore.SSF_STATUS_KEY, "enabled");
        realm.admin().clients().get(rwClient.getId()).update(rwClient);

        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);

        // GET must read the legacy blob transparently.
        try (SimpleHttpResponse response = http.doGet(streamsEndpoint())
                .param("stream_id", legacyStreamId)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus(),
                    "GET should read a pre-refactor legacy blob transparently");
            StreamConfig fetched = response.asJson(StreamConfig.class);
            Assertions.assertEquals(legacyStreamId, fetched.getStreamId());
            Assertions.assertEquals(Set.of(CaepCredentialChange.TYPE), fetched.getEventsRequested());
            Assertions.assertEquals("legacy stream from pre-refactor data", fetched.getDescription());
        }

        // PATCH the description. This goes through storeStreamConfig, which
        // removes the legacy blob and writes the split attributes.
        StreamConfigUpdateRepresentation patch = new StreamConfigUpdateRepresentation();
        patch.setStreamId(legacyStreamId);
        patch.setDescription("migrated description");
        try (SimpleHttpResponse response = patchStream(token, patch)) {
            Assertions.assertEquals(200, response.getStatus(),
                    "PATCH against a legacy stream should succeed and migrate its storage");
        }

        // Verify the legacy blob is gone and the split attributes are present.
        ClientRepresentation migrated = findClientByClientId(RECEIVER_RW);
        Assertions.assertNotNull(migrated.getAttributes());
        Assertions.assertNull(
                migrated.getAttributes().get(ClientStreamStore.SSF_STREAM_CONFIG_KEY),
                "legacy blob attribute must be removed after a storing write");
        Assertions.assertEquals("migrated description",
                migrated.getAttributes().get(ClientStreamStore.SSF_STREAM_DESCRIPTION_KEY),
                "description should now live in its dedicated split attribute");
        Assertions.assertEquals(Ssf.DELIVERY_METHOD_PUSH_URI,
                migrated.getAttributes().get(ClientStreamStore.SSF_STREAM_DELIVERY_METHOD_KEY),
                "delivery.method should have been migrated into its split attribute");
        Assertions.assertEquals(DUMMY_PUSH_ENDPOINT,
                migrated.getAttributes().get(ClientStreamStore.SSF_STREAM_DELIVERY_ENDPOINT_URL_KEY),
                "delivery.endpoint_url should have been migrated into its split attribute");

        // And the stream is still readable via the public API.
        try (SimpleHttpResponse response = http.doGet(streamsEndpoint())
                .param("stream_id", legacyStreamId)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus());
            StreamConfig fetched = response.asJson(StreamConfig.class);
            Assertions.assertEquals("migrated description", fetched.getDescription());
            Assertions.assertEquals(Set.of(CaepCredentialChange.TYPE), fetched.getEventsRequested());
        }
    }

    @Test
    public void testCreateStreamHonoursClientSupportedEvents() throws IOException {

        // RECEIVER_SCOPED is configured with ssf.supportedEvents=CaepSessionRevoked
        // so requesting additional events should still yield a delivered set
        // narrowed to the client-configured allow list.
        String token = obtainManageToken(RECEIVER_SCOPED, RECEIVER_SCOPED_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(
                CaepCredentialChange.TYPE,
                CaepSessionRevoked.TYPE));

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus());

            StreamConfig created = response.asJson(StreamConfig.class);
            Assertions.assertEquals(Set.of(CaepSessionRevoked.TYPE), created.getEventsSupported(),
                    "events_supported should reflect the client-configured supported events");
            Assertions.assertEquals(Set.of(CaepSessionRevoked.TYPE), created.getEventsDelivered(),
                    "events_delivered should be narrowed to the client-configured supported events");
        }
    }

    @Test
    public void testDeleteStreamClearsOnlyStreamAttributes() throws IOException {

        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus());
        }

        try (SimpleHttpResponse response = http.doDelete(streamsEndpoint())
                .auth(token)
                .asResponse()) {
            Assertions.assertEquals(204, response.getStatus(), "DELETE should return 204");
        }

        ClientRepresentation rwClient = findClientByClientId(RECEIVER_RW);
        Assertions.assertNotNull(rwClient, "receiver client should still exist");
        Assertions.assertNotNull(rwClient.getAttributes());
        // Regression guard for SSF_STREAM_KEYS narrowing: deleting the stream
        // must not clear the receiver-level configuration (enabled flag etc.).
        Assertions.assertEquals("true", rwClient.getAttributes().get(ClientStreamStore.SSF_ENABLED_KEY),
                "deleting the stream must not clear the ssf.enabled flag");
        Assertions.assertNull(rwClient.getAttributes().get(ClientStreamStore.SSF_STREAM_ID_KEY),
                "ssf.streamId attribute should be removed on delete");
        Assertions.assertNull(rwClient.getAttributes().get(ClientStreamStore.SSF_STREAM_CONFIG_KEY),
                "ssf.streamConfig attribute should be removed on delete");
    }

    @Test
    public void testAdminDeleteClientStreamEndpoint() throws IOException {

        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus());
        }

        String rwClientUuid = findClientByClientId(RECEIVER_RW).getId();
        String adminStreamUrl = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + rwClientUuid + "/stream";

        // Admin GET should return the stream before delete.
        try (SimpleHttpResponse response = http.doGet(adminStreamUrl)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus(),
                    "admin stream endpoint should return 200 while the stream exists");
        }

        try (SimpleHttpResponse response = http.doDelete(adminStreamUrl)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .asResponse()) {
            Assertions.assertEquals(204, response.getStatus(),
                    "admin delete stream endpoint should return 204");
        }

        // After delete the admin GET should return 404.
        try (SimpleHttpResponse response = http.doGet(adminStreamUrl)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(404, response.getStatus(),
                    "admin stream endpoint should return 404 after the stream was deleted");
        }
    }

    // --- helpers ---------------------------------------------------------

    protected String streamsEndpoint() {
        return SsfTransmitterUrls.streamsEndpoint(realm.getBaseUrl());
    }

    /**
     * Builds a {@link StreamConfigUpdateRepresentation} populated with a dummy
     * push-delivery configuration, the given events_requested set, and a
     * description. The returned type is the PATCH/PUT DTO ({@code Update})
     * rather than the POST DTO ({@code Input}) so tests can call
     * {@link StreamConfigUpdateRepresentation#setStreamId(String)} on it when
     * exercising update/replace paths; on create the null {@code stream_id}
     * is omitted from the wire JSON and the POST handler binds the rest into
     * a {@link StreamConfigInputRepresentation} via the inherited setters.
     */
    protected StreamConfigUpdateRepresentation buildPushStreamRequest(Set<String> eventsRequested) {
        StreamDeliveryConfig delivery = new StreamDeliveryConfig();
        delivery.setMethod(Ssf.DELIVERY_METHOD_PUSH_URI);
        delivery.setEndpointUrl(DUMMY_PUSH_ENDPOINT);
        delivery.setAuthorizationHeader(DUMMY_PUSH_AUTH_HEADER);

        StreamConfigUpdateRepresentation request = new StreamConfigUpdateRepresentation();
        request.setDelivery(delivery);
        request.setEventsRequested(eventsRequested);
        request.setDescription("Stream management integration test");
        return request;
    }

    protected SimpleHttpResponse postStream(String accessToken, StreamConfigInputRepresentation input) throws IOException {
        return http.doPost(streamsEndpoint())
                .json(input)
                .auth(accessToken)
                .acceptJson()
                .asResponse();
    }

    protected SimpleHttpResponse patchStream(String accessToken, StreamConfigUpdateRepresentation update) throws IOException {
        return http.doPatch(streamsEndpoint())
                .json(update)
                .auth(accessToken)
                .acceptJson()
                .asResponse();
    }

    protected String obtainManageToken(String clientId, String secret) throws IOException {
        // Request both scopes so the same token can be used for create/update
        // (needs ssf.manage) and subsequent read-back (needs ssf.read).
        String token = obtainReceiverAccessToken(clientId, secret,
                SsfScopes.SCOPE_SSF_MANAGE + " " + SsfScopes.SCOPE_SSF_READ);
        Assertions.assertNotNull(token, () ->
                "expected client '" + clientId + "' to be able to obtain an access token with scopes "
                        + SsfScopes.SCOPE_SSF_MANAGE + " and " + SsfScopes.SCOPE_SSF_READ);
        return token;
    }

    /**
     * Performs a client_credentials grant against the realm's token endpoint
     * using HTTP basic auth and the given scope, and returns the access token
     * string (or {@code null} if the token request was rejected).
     */
    protected String obtainReceiverAccessToken(String clientId, String secret, String scope) throws IOException {
        String tokenUrl = realm.getBaseUrl() + "/protocol/openid-connect/token";
        try (SimpleHttpResponse response = http.doPost(tokenUrl)
                .authBasic(clientId, secret)
                .param("grant_type", "client_credentials")
                .param("scope", scope)
                .asResponse()) {
            if (response.getStatus() != 200) {
                return null;
            }
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

    /**
     * Assigns the given realm client scopes to the receiver client as optional
     * scopes via the admin REST API. Idempotent — skips scopes that are
     * already assigned.
     */
    protected void assignOptionalClientScopes(String clientId, String... scopeNames) {
        ClientRepresentation client = findClientByClientId(clientId);
        Assertions.assertNotNull(client, () -> "expected client '" + clientId + "' to be present in realm");
        ClientResource clientResource = realm.admin().clients().get(client.getId());

        Set<String> alreadyAssigned = clientResource.getOptionalClientScopes().stream()
                .map(ClientScopeRepresentation::getName)
                .collect(java.util.stream.Collectors.toSet());

        List<ClientScopeRepresentation> allScopes = realm.admin().clientScopes().findAll();
        for (String scopeName : scopeNames) {
            if (alreadyAssigned.contains(scopeName)) {
                continue;
            }
            ClientScopeRepresentation scope = allScopes.stream()
                    .filter(s -> scopeName.equals(s.getName()))
                    .findFirst()
                    .orElse(null);
            Assertions.assertNotNull(scope, () ->
                    "expected realm scope '" + scopeName + "' to exist; make sure SSF scopes are auto-provisioned");
            clientResource.addOptionalClientScope(scope.getId());
        }
    }

    protected void bestEffortDeleteStream(String clientId) {
        ClientRepresentation client = findClientByClientId(clientId);
        if (client == null) {
            return;
        }
        String adminStreamUrl = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + client.getId() + "/stream";
        try (SimpleHttpResponse response = http.doDelete(adminStreamUrl)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .asResponse()) {
            // 204 means deleted, 404 means nothing to delete — both are fine.
        } catch (IOException ignored) {
        }
    }

    public static class StreamManagementKeycloakServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            config.features(Profile.Feature.SSF);
            config.log().categoryLevel("org.keycloak.protocol.ssf", "DEBUG");
            return configured;
        }
    }

    public static class StreamManagementRealm implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.name("ssf-transmitter-stream-mgmt");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");

            // The ssf.read / ssf.manage client scopes are auto-created by the
            // SSF RealmPostCreateEvent listener only *after* the clients are
            // imported, so we assign them to each receiver client via the
            // admin REST API from a @BeforeEach hook instead of declaring
            // them here as optionalClientScopes(...).

            realm.addClient(RECEIVER_RW)
                    .secret(RECEIVER_RW_SECRET)
                    .serviceAccountsEnabled(true)
                    .directAccessGrantsEnabled(false)
                    .publicClient(false)
                    .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true");

            realm.addClient(RECEIVER_RO)
                    .secret(RECEIVER_RO_SECRET)
                    .serviceAccountsEnabled(true)
                    .directAccessGrantsEnabled(false)
                    .publicClient(false)
                    .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true");

            realm.addClient(RECEIVER_SCOPED)
                    .secret(RECEIVER_SCOPED_SECRET)
                    .serviceAccountsEnabled(true)
                    .directAccessGrantsEnabled(false)
                    .publicClient(false)
                    .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                    .attribute(ClientStreamStore.SSF_STREAM_SUPPORTED_EVENTS_KEY,
                            CaepSessionRevoked.class.getSimpleName());

            return realm;
        }
    }
}
