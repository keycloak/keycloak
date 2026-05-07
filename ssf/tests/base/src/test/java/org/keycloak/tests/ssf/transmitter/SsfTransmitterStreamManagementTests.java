package org.keycloak.tests.ssf.transmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.events.outbox.OutboxStore;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.OutboxEntryEntity;
import org.keycloak.models.jpa.entities.OutboxEntryStatus;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.caep.CaepCredentialChange;
import org.keycloak.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.ssf.transmitter.SsfScopes;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.admin.SsfClientStreamRepresentation;
import org.keycloak.ssf.transmitter.outbox.SsfOutboxKinds;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.StreamConfigInputRepresentation;
import org.keycloak.ssf.transmitter.stream.StreamConfigUpdateRepresentation;
import org.keycloak.ssf.transmitter.stream.StreamDeliveryConfig;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.support.SsfAuthUtil;
import org.keycloak.ssf.transmitter.support.SsfTransmitterUrls;
import org.keycloak.testframework.annotations.InjectAdminClient;
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
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
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

    // Receiver used to exercise the service-account gate explicitly: it
    // enables both the client-credentials flow (the receiver's own SA)
    // and direct access grants (so a regular user can mint a token with
    // the ssf.manage scope). Tests then assert that the gate accepts the
    // SA bearer and rejects the user bearer when SA is required, and
    // accepts the user bearer when the SA requirement is opted out.
    static final String RECEIVER_USER_AUTH = "ssf-receiver-user-auth";
    static final String RECEIVER_USER_AUTH_SECRET = "receiver-user-auth-secret";
    static final String SSF_USER = "ssf-user";
    static final String SSF_USER_PASSWORD = "ssf-user-password";

    static final String DUMMY_PUSH_ENDPOINT = "http://127.0.0.1:65535/ssf/push";

    /**
     * Allow-list pattern that matches {@link #DUMMY_PUSH_ENDPOINT}. Pinned
     * on the receiver clients via {@code ssf.validPushUrls} so the SSRF
     * gate accepts the dummy URL during test setup; the http scheme and
     * loopback host are tolerated only because the
     * {@code allow-insecure-push-targets} SPI flag is on in
     * {@link StreamManagementKeycloakServerConfig}.
     */
    static final String DUMMY_PUSH_ALLOWLIST = "http://127.0.0.1:65535/*";
    static final String DUMMY_PUSH_AUTH_HEADER = "Bearer dummy-receiver-token";

    @InjectRealm(config = StreamManagementRealm.class)
    ManagedRealm realm;

    @InjectSimpleHttp
    SimpleHttp http;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    // The OAuthClient is provisioned by the test framework with its own
    // default test-app client; we don't use that default — we drive the
    // password-grant request against our own RECEIVER_USER_AUTH client by
    // overriding realm/client/scope on each call. The injection only
    // exists to give us access to the framework's HTTP machinery.
    @InjectOAuthClient
    OAuthClient oauth;

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
        assignOptionalClientScopes(RECEIVER_USER_AUTH, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
    }

    @AfterEach
    public void cleanupStreams() {
        List.of(RECEIVER_RW, RECEIVER_RO, RECEIVER_SCOPED, RECEIVER_USER_AUTH)
                .forEach(this::bestEffortDeleteStream);
        // The opt-out test flips ssf.requireServiceAccount on RECEIVER_USER_AUTH;
        // reset to "" rather than removing — Keycloak's admin client
        // PUT-merge on ClientRepresentation doesn't reliably delete an
        // attribute by omitting it from the body, so we explicitly blank
        // it out and let the gate's null/blank check apply the default.
        setClientAttribute(RECEIVER_USER_AUTH, ClientStreamStore.SSF_REQUIRE_SERVICE_ACCOUNT_KEY, "");
        // The SSRF-gate tests mutate ssf.validPushUrls / ssf.allowedDeliveryMethods
        // on RECEIVER_RW; restore both to the realm-bootstrap baseline
        // (validPushUrls = DUMMY_PUSH_ALLOWLIST, allowedDeliveryMethods
        // blank = both push and poll allowed) so subsequent tests start
        // from a clean slate. Same blank-string-not-clear pattern as above.
        setClientAttribute(RECEIVER_RW, ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, DUMMY_PUSH_ALLOWLIST);
        setClientAttribute(RECEIVER_RW, ClientStreamStore.SSF_ALLOWED_DELIVERY_METHODS_KEY, "");
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
    public void testPatchStreamWidensRequestedEvents() throws IOException {

        // Mirror of testPatchStreamNarrowsRequestedEvents in the other
        // direction: a PATCH that ADDS an event to events_requested must
        // recompute events_delivered to include the newly-requested
        // type, since that's the set the dispatcher gates on.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(
                CaepSessionRevoked.TYPE));

        String streamId;
        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus());
            StreamConfig created = response.asJson(StreamConfig.class);
            streamId = created.getStreamId();
            Assertions.assertEquals(Set.of(CaepSessionRevoked.TYPE), created.getEventsRequested(),
                    "events_requested should match the create body");
            Assertions.assertTrue(created.getEventsDelivered().contains(CaepSessionRevoked.TYPE));
            Assertions.assertFalse(created.getEventsDelivered().contains(CaepCredentialChange.TYPE),
                    "events_delivered must not include events not yet requested");
        }

        StreamConfigUpdateRepresentation patch = buildPushStreamRequest(Set.of(
                CaepSessionRevoked.TYPE,
                CaepCredentialChange.TYPE));
        patch.setStreamId(streamId);

        try (SimpleHttpResponse response = patchStream(token, patch)) {
            Assertions.assertEquals(200, response.getStatus(), "PATCH should succeed");
            StreamConfig updated = response.asJson(StreamConfig.class);
            Assertions.assertEquals(
                    Set.of(CaepSessionRevoked.TYPE, CaepCredentialChange.TYPE),
                    updated.getEventsRequested(),
                    "events_requested should be widened to include the added event");
            Assertions.assertTrue(updated.getEventsDelivered().contains(CaepSessionRevoked.TYPE),
                    "events_delivered must keep the previously-requested event");
            Assertions.assertTrue(updated.getEventsDelivered().contains(CaepCredentialChange.TYPE),
                    "events_delivered must include the newly-requested event after recompute");
        }
    }

    @Test
    public void testAdminUpdateStreamStatusPersistsAndDefaultsReason() throws IOException {

        // The admin stream-status endpoint funnels through
        // StreamService.updateStreamStatusAsAdmin → updateStreamStatus,
        // which is the same path receiver-side POST /streams/status
        // hits — so the spec-mandated stream-updated SET dispatch
        // and outbox alignment fire here too. This test covers the
        // wiring + the default-reason behaviour. The actual SET
        // dispatch path is covered by the existing receiver-side
        // status tests (e.g. SsfTransmitterPushDeliveryTests
        // .testDisabledStreamDoesNotReceivePush) since both paths
        // share updateStreamStatus.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(
                CaepCredentialChange.TYPE));

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus());
        }

        String adminStatusUrl = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + RECEIVER_RW + "/stream/status";

        // Caller omits reason — the endpoint should default it to the
        // documented "Transmitter status override" marker (written
        // from the receiver's perspective: receivers only see the
        // transmitter as the actor, not which actor on the
        // transmitter side initiated the change).
        try (SimpleHttpResponse response = http.doPost(adminStatusUrl)
                .json(Map.of("status", "paused"))
                .auth(adminClient.tokenManager().getAccessTokenString())
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus());
            var body = response.asJson();
            Assertions.assertEquals("paused", body.get("status").asText(),
                    "response should echo the new status");
            Assertions.assertEquals("Transmitter status override", body.get("reason").asText(),
                    "missing reason should be defaulted to the transmitter marker");
        }

        // Verify the new status is persisted by reading via the admin GET.
        SsfClientStreamRepresentation stored = fetchAdminStreamRepresentation(RECEIVER_RW);
        Assertions.assertEquals("paused", stored.getStatus(),
                "stream status should be persisted as paused");

        // Caller-supplied reason must win.
        try (SimpleHttpResponse response = http.doPost(adminStatusUrl)
                .json(Map.of("status", "enabled", "reason", "manual recovery"))
                .auth(adminClient.tokenManager().getAccessTokenString())
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus());
            var body = response.asJson();
            Assertions.assertEquals("enabled", body.get("status").asText());
            Assertions.assertEquals("manual recovery", body.get("reason").asText(),
                    "caller-supplied reason must not be overwritten");
        }
    }

    @Test
    public void testPatchNarrowDeadLettersQueuedRowsForRemovedEventTypes() throws IOException {

        // When a PATCH narrows events_requested, already-queued
        // non-terminal outbox rows whose event type is no longer in
        // events_delivered must stop being delivered — neither the
        // push drainer nor the poll endpoint re-checks
        // events_requested per row, so without this transition the
        // receiver would still receive events it has just opted out
        // of. We dead-letter rather than delete so the audit trail
        // for real Keycloak-side events is preserved; standard
        // dead-letter retention purges them eventually.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(
                CaepCredentialChange.TYPE,
                CaepSessionRevoked.TYPE));

        final String streamId;
        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus());
            streamId = response.asJson(StreamConfig.class).getStreamId();
        }

        // Seed one PENDING outbox row of each event type via runOnServer
        // (using the server-side OutboxStore so we don't rely on a
        // failing push to land rows in PENDING). Use unique jtis so we
        // can identify them after the PATCH.
        final String credJti = "test-evict-cred-" + UUID.randomUUID();
        final String sessJti = "test-evict-sess-" + UUID.randomUUID();
        runOnServer.run(session -> {
            var serverRealm = session.getContext().getRealm();
            var receiver = serverRealm.getClientByClientId(RECEIVER_RW);
            OutboxStore store = new OutboxStore(session);
            store.enqueuePending(SsfOutboxKinds.PUSH, serverRealm.getId(), receiver.getId(),
                    streamId, credJti, CaepCredentialChange.TYPE, "encoded-cred", null);
            store.enqueuePending(SsfOutboxKinds.PUSH, serverRealm.getId(), receiver.getId(),
                    streamId, sessJti, CaepSessionRevoked.TYPE, "encoded-sess", null);
        });

        // Pre-condition sanity: both rows are present and PENDING.
        runOnServer.run(session -> {
            var serverRealm = session.getContext().getRealm();
            var receiver = serverRealm.getClientByClientId(RECEIVER_RW);
            OutboxStore store = new OutboxStore(session);
            OutboxEntryEntity credRow = store.findByOwnerAndCorrelationId(SsfOutboxKinds.PUSH, receiver.getId(), credJti);
            OutboxEntryEntity sessRow = store.findByOwnerAndCorrelationId(SsfOutboxKinds.PUSH, receiver.getId(), sessJti);
            Assertions.assertNotNull(credRow, "seeded credential-change row should be present");
            Assertions.assertEquals(OutboxEntryStatus.PENDING, credRow.getStatus(),
                    "seed should land as PENDING");
            Assertions.assertNotNull(sessRow, "seeded session-revoked row should be present");
            Assertions.assertEquals(OutboxEntryStatus.PENDING, sessRow.getStatus(),
                    "seed should land as PENDING");
        });

        // PATCH narrows events_requested to drop CaepCredentialChange.
        StreamConfigUpdateRepresentation patch = buildPushStreamRequest(Set.of(CaepSessionRevoked.TYPE));
        patch.setStreamId(streamId);
        try (SimpleHttpResponse response = patchStream(token, patch)) {
            Assertions.assertEquals(200, response.getStatus(), "PATCH should succeed");
            StreamConfig updated = response.asJson(StreamConfig.class);
            Assertions.assertFalse(updated.getEventsDelivered().contains(CaepCredentialChange.TYPE),
                    "events_delivered should no longer include the dropped event");
        }

        // The credential-change row must have been parked as
        // DEAD_LETTER with the documented reason; the session-revoked
        // row stays PENDING.
        runOnServer.run(session -> {
            var serverRealm = session.getContext().getRealm();
            var receiver = serverRealm.getClientByClientId(RECEIVER_RW);
            OutboxStore store = new OutboxStore(session);
            OutboxEntryEntity credRow = store.findByOwnerAndCorrelationId(SsfOutboxKinds.PUSH, receiver.getId(), credJti);
            OutboxEntryEntity sessRow = store.findByOwnerAndCorrelationId(SsfOutboxKinds.PUSH, receiver.getId(), sessJti);
            Assertions.assertNotNull(credRow,
                    "credential-change row must be retained for audit, not deleted");
            Assertions.assertEquals(OutboxEntryStatus.DEAD_LETTER, credRow.getStatus(),
                    "credential-change row should be parked as DEAD_LETTER on PATCH narrow");
            Assertions.assertEquals("event_type_no_longer_requested", credRow.getLastError(),
                    "DEAD_LETTER reason should identify the cause");
            Assertions.assertNotNull(sessRow,
                    "session-revoked row must be preserved (still in events_delivered)");
            Assertions.assertEquals(OutboxEntryStatus.PENDING, sessRow.getStatus(),
                    "session-revoked row must remain PENDING");
        });
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
        Set<String> originalAudience;
        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus());
            StreamConfig created = response.asJson(StreamConfig.class);
            streamId = created.getStreamId();
            originalAudience = created.getAudience();
        }
        // kc_created_at is admin-only and not on the receiver-facing wire;
        // read it through the admin endpoint so we can assert it didn't move.
        Integer originalCreatedAt = fetchAdminStreamRepresentation(RECEIVER_RW).getCreatedAt();

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
            Assertions.assertEquals(originalAudience, fetched.getAudience(),
                    "aud must not be altered by a rejected PATCH");
            Assertions.assertNotEquals("attempted take-over", fetched.getDescription(),
                    "a rejected PATCH must not leave partial writes behind");
        }
        Assertions.assertEquals(originalCreatedAt, fetchAdminStreamRepresentation(RECEIVER_RW).getCreatedAt(),
                "kc_created_at must not be altered by a rejected PATCH");
    }

    @Test
    public void testPatchStreamPreservesCreatedAtAndStampsUpdatedAt() throws IOException {

        // Regression for the timestamp plumbing: create stamps both
        // kc_created_at and kc_updated_at, update preserves kc_created_at
        // and refreshes kc_updated_at. Both fields are admin-only and not
        // surfaced on the receiver-facing wire — we read them through the
        // admin /ssf/clients/{uuid}/stream endpoint instead.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));

        String streamId;
        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus());
            streamId = response.asJson(StreamConfig.class).getStreamId();
        }

        SsfClientStreamRepresentation afterCreate = fetchAdminStreamRepresentation(RECEIVER_RW);
        Integer originalCreatedAt = afterCreate.getCreatedAt();
        Assertions.assertNotNull(originalCreatedAt,
                "create must stamp kc_created_at");
        Assertions.assertNotNull(afterCreate.getUpdatedAt(),
                "create must stamp kc_updated_at");

        StreamConfigUpdateRepresentation patch = new StreamConfigUpdateRepresentation();
        patch.setStreamId(streamId);
        patch.setDescription("second revision");

        try (SimpleHttpResponse response = patchStream(token, patch)) {
            Assertions.assertEquals(200, response.getStatus());
        }

        SsfClientStreamRepresentation afterPatch = fetchAdminStreamRepresentation(RECEIVER_RW);
        Assertions.assertEquals(originalCreatedAt, afterPatch.getCreatedAt(),
                "kc_created_at must not change across updates");
        Assertions.assertNotNull(afterPatch.getUpdatedAt(),
                "kc_updated_at must remain set after a PATCH");
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
        Set<String> originalAudience;
        String originalIssuer;
        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus());
            StreamConfig created = response.asJson(StreamConfig.class);
            streamId = created.getStreamId();
            originalAudience = created.getAudience();
            originalIssuer = created.getIssuer();
        }
        // kc_created_at lives on the admin representation only.
        Integer originalCreatedAt = fetchAdminStreamRepresentation(RECEIVER_RW).getCreatedAt();

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
            Assertions.assertNotEquals("attempted take-over", fetched.getDescription(),
                    "a rejected PUT must not leave partial writes behind");
        }
        Assertions.assertEquals(originalCreatedAt, fetchAdminStreamRepresentation(RECEIVER_RW).getCreatedAt(),
                "kc_created_at must not be altered by a rejected PUT");
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
    public void testCreateStreamRejectsMalformedEndpointUrl() throws IOException {

        // delivery.endpoint_url that isn't a parseable URI must be rejected
        // up front rather than dead-lettering every queued event for that
        // receiver on first push attempt.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));
        request.getDelivery().setEndpointUrl("not a url");

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(400, response.getStatus(),
                    "malformed delivery.endpoint_url must be rejected");
        }
    }

    @Test
    public void testCreateStreamRejectsNonHttpEndpointUrl() throws IOException {

        // The push transport is HTTP-only; receivers can't supply ftp://,
        // file://, javascript:, etc. — those would just dead-letter on
        // first push attempt. Reject the scheme up front.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));
        request.getDelivery().setEndpointUrl("ftp://example.com/push");

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(400, response.getStatus(),
                    "non-http(s) delivery.endpoint_url must be rejected");
        }
    }

    @Test
    public void testCreateStreamRejectsRelativeEndpointUrl() throws IOException {

        // A relative URL like "/ssf/push" has no scheme/host the
        // transmitter can reach, so it must be rejected.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));
        request.getDelivery().setEndpointUrl("/ssf/push");

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(400, response.getStatus(),
                    "relative delivery.endpoint_url must be rejected");
        }
    }

    @Test
    public void testCreateStreamRejectsPushWhenValidPushUrlsEmpty() throws IOException {

        // SSRF gate. Receiver clients that don't declare an
        // ssf.validPushUrls allow-list cannot create PUSH streams —
        // ssf.validPushUrls is the per-client SSRF defence and the
        // transmitter refuses to push to a URL the operator hasn't
        // explicitly approved. Migration story for existing PUSH
        // receivers after the gate lands: admin adds at least one entry.
        // Use the empty-string blank value (rather than clearClientAttribute)
        // because the Keycloak admin client's PUT-merge semantics don't
        // remove an existing attribute by omitting it — to actually wipe
        // it we set it to "" and let readValidPushUrls treat blank as empty.
        setClientAttribute(RECEIVER_RW, ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "");

        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(400, response.getStatus(),
                    "PUSH stream creation must be rejected when ssf.validPushUrls is empty");
        }
    }

    @Test
    public void testCreateStreamRejectsPushUrlOutsideAllowList() throws IOException {

        // SSRF gate. Receiver-supplied URL doesn't match any
        // ssf.validPushUrls entry → 400. The realm-bootstrap pins
        // RECEIVER_RW to http://127.0.0.1:65535/* ; the request below
        // tries to push to a different host, which must be refused.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));
        request.getDelivery().setEndpointUrl("http://attacker.example.com/ssf/push");

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(400, response.getStatus(),
                    "push URL outside the receiver's ssf.validPushUrls must be rejected");
        }
    }

    @Test
    public void testCreateStreamAcceptsExactMatchInValidPushUrls() throws IOException {

        // Exact-match entry. Pin the receiver to a single concrete URL;
        // the receiver requests the same URL → 201.
        String exactUrl = "http://127.0.0.1:65535/ssf/exact-pin";
        setClientAttribute(RECEIVER_RW, ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, exactUrl);

        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));
        request.getDelivery().setEndpointUrl(exactUrl);

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus(),
                    "URL that exactly matches a ssf.validPushUrls entry must be accepted");
        }
    }

    @Test
    public void testCreateStreamAcceptsWildcardMatchInValidPushUrls() throws IOException {

        // Trailing-* wildcard entry. Pin the receiver to a host[:port]
        // prefix; receiver requests any URL underneath → 201.
        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));
        request.getDelivery().setEndpointUrl("http://127.0.0.1:65535/ssf/under/wildcard/pin");

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus(),
                    "URL that matches a trailing-* ssf.validPushUrls entry must be accepted");
        }
    }

    @Test
    public void testCreateStreamRejectsPushWhenAllowedDeliveryMethodsRestrictsToPoll() throws IOException {

        // Capability gate. Admin restricts the receiver to POLL only;
        // the receiver tries to register a PUSH stream → 400 with the
        // "delivery method 'push' is not allowed" message.
        setClientAttribute(RECEIVER_RW, ClientStreamStore.SSF_ALLOWED_DELIVERY_METHODS_KEY, "poll");

        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(400, response.getStatus(),
                    "PUSH stream creation must be rejected when ssf.allowedDeliveryMethods restricts to poll");
        }
    }

    @Test
    public void testCreateStreamRejectsBareWildcardInValidPushUrls() throws IOException {

        // Bare-* tightening. Even if the operator tries to "open up"
        // the allow-list with a single * entry, the validator drops it
        // and treats the allow-list as empty — disabling the SSRF
        // defence with one keystroke must not be possible.
        setClientAttribute(RECEIVER_RW, ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "*");

        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);
        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));

        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(400, response.getStatus(),
                    "bare '*' entry in ssf.validPushUrls must not be honoured as a free pass");
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

        String adminStreamUrl = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + RECEIVER_RW + "/stream";

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

    @Test
    public void testCreateStreamAcceptsServiceAccountBearer() throws IOException {

        // Explicit regression for the service-account gate in
        // SsfAuthUtil.checkScopePermission: with the default policy
        // (ssf.requireServiceAccount attribute absent → required) the
        // receiver's own service-account bearer must be accepted.
        // UserModel.getServiceAccountClientLink() returns the receiver
        // client's clientId for service-account users, so the gate must
        // deny iff that link does NOT match — a missing negation here
        // had the inverse effect (denied SA bearers, accepted user
        // bearers). Many other tests in this class hit this path
        // implicitly through obtainManageToken(); naming it here makes
        // the regression intent explicit.
        String saToken = obtainManageToken(RECEIVER_USER_AUTH, RECEIVER_USER_AUTH_SECRET);

        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));
        try (SimpleHttpResponse response = postStream(saToken, request)) {
            Assertions.assertEquals(201, response.getStatus(),
                    "service-account bearer must be accepted under the default SA-required policy");
        }
    }

    @Test
    public void testCreateStreamRejectsUserBearerWhenServiceAccountRequired() throws IOException {

        // Negative side of the SA gate: with the default policy a
        // password-grant token (regular user, not the receiver's SA)
        // must be rejected even when it carries the ssf.manage scope.
        // The token is mintable here because RECEIVER_USER_AUTH has
        // direct access grants enabled and ssf.manage is in the
        // user's optional scopes — so the gate is the only thing
        // standing between a regular user and stream management.
        String userToken = obtainPasswordGrantToken(
                RECEIVER_USER_AUTH, RECEIVER_USER_AUTH_SECRET,
                SSF_USER, SSF_USER_PASSWORD,
                SsfScopes.SCOPE_SSF_MANAGE + " " + SsfScopes.SCOPE_SSF_READ);
        Assertions.assertNotNull(userToken,
                "test setup must allow the user to mint an ssf.manage token; the gate, not OAuth, is what must reject it");

        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));
        try (SimpleHttpResponse response = postStream(userToken, request)) {
            Assertions.assertEquals(401, response.getStatus(),
                    "regular-user bearer must be rejected when ssf.requireServiceAccount=true");
        }
    }

    @Test
    public void testCreateStreamAcceptsUserBearerWhenServiceAccountOptedOut() throws IOException {

        // Opt-out path: when ssf.requireServiceAccount is explicitly
        // "false", the SA gate is skipped entirely and a regular-user
        // bearer with ssf.manage is allowed. The opt-out is reset by
        // the @AfterEach hook so subsequent tests run under the
        // default SA-required policy.
        setClientAttribute(RECEIVER_USER_AUTH, ClientStreamStore.SSF_REQUIRE_SERVICE_ACCOUNT_KEY, "false");

        String userToken = obtainPasswordGrantToken(
                RECEIVER_USER_AUTH, RECEIVER_USER_AUTH_SECRET,
                SSF_USER, SSF_USER_PASSWORD,
                SsfScopes.SCOPE_SSF_MANAGE + " " + SsfScopes.SCOPE_SSF_READ);
        Assertions.assertNotNull(userToken);

        StreamConfigUpdateRepresentation request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));
        try (SimpleHttpResponse response = postStream(userToken, request)) {
            Assertions.assertEquals(201, response.getStatus(),
                    "with ssf.requireServiceAccount=false, a regular-user bearer must pass the gate");
        }
    }

    @Test
    public void testServiceAccountGateRejectsAnotherClientsServiceAccount() {

        // White-box coverage for the SA-gate's mismatch branch. The
        // receiver-side SSF endpoints only ever observe a bearer's bound
        // client (authResult.client()) and resource owner
        // (authResult.user()). For a client_credentials token Keycloak's
        // auth pipeline guarantees user.getServiceAccountClientLink() ==
        // authResult.client().getId() by construction — so a token whose
        // user is the SA of a *different* client cannot arise from the
        // public token endpoint, and there is no realistic HTTP path that
        // exercises this branch.
        //
        // To cover it anyway we fabricate the mismatched pairing on the
        // server side: authResult.client() = RECEIVER_USER_AUTH (the
        // bearer's bound receiver) but authResult.user() = RECEIVER_RW's
        // service-account user (whose link points to RECEIVER_RW's UUID).
        // The gate must reject because the link belongs to RECEIVER_RW,
        // not RECEIVER_USER_AUTH.
        runOnServer.run(session -> {
            var serverRealm = session.getContext().getRealm();
            ClientModel target = serverRealm.getClientByClientId(RECEIVER_USER_AUTH);
            ClientModel other = serverRealm.getClientByClientId(RECEIVER_RW);
            Assertions.assertNotNull(target, "RECEIVER_USER_AUTH must exist");
            Assertions.assertNotNull(other, "RECEIVER_RW must exist");

            UserModel otherSa = session.users().getServiceAccount(other);
            Assertions.assertNotNull(otherSa, "RECEIVER_RW must have a service-account user");
            Assertions.assertEquals(other.getId(), otherSa.getServiceAccountClientLink(),
                    "sanity: SA link points to RECEIVER_RW's UUID, not RECEIVER_USER_AUTH's");

            AccessToken token = new AccessToken();
            token.setScope(Ssf.SCOPE_SSF_MANAGE);
            AuthenticationManager.AuthResult auth =
                    new AuthenticationManager.AuthResult(otherSa, null, token, target);

            session.setAttribute(SsfAuthUtil.AUTH_KEY, auth);
            try {
                Assertions.assertFalse(SsfAuthUtil.canManage(),
                        "SA gate must reject a bearer whose user is the SA of a different client");
            } finally {
                session.removeAttribute(SsfAuthUtil.AUTH_KEY);
            }
        });
    }

    // --- helpers ---------------------------------------------------------

    protected String streamsEndpoint() {
        return SsfTransmitterUrls.getStreamsEndpointUrl(realm.getBaseUrl());
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

    protected String obtainPasswordGrantToken(String clientId, String secret,
                                              String username, String password,
                                              String scope) {
        return oauth.realm(realm.getName())
                .client(clientId, secret)
                .scope(scope)
                .doPasswordGrantRequest(username, password)
                .getAccessToken();
    }

    /**
     * Writes the given attribute on the receiver client through the admin
     * REST API. Used to flip {@code ssf.requireServiceAccount} at test time.
     */
    protected void setClientAttribute(String clientId, String key, String value) {
        ClientRepresentation client = findClientByClientId(clientId);
        Assertions.assertNotNull(client, () -> "expected client '" + clientId + "' to be present in realm");
        if (client.getAttributes() == null) {
            client.setAttributes(new java.util.HashMap<>());
        }
        client.getAttributes().put(key, value);
        realm.admin().clients().get(client.getId()).update(client);
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
                + "/ssf/clients/" + client.getClientId() + "/stream";
        try (SimpleHttpResponse response = http.doDelete(adminStreamUrl)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .asResponse()) {
            // 204 means deleted, 404 means nothing to delete — both are fine.
        } catch (IOException ignored) {
        }
    }

    /**
     * Fetches the current admin representation of the receiver client's stream
     * via {@code GET /admin/realms/{realm}/ssf/clients/{clientUuid}/stream}.
     * Used by tests that need to read admin-only fields (kc_created_at,
     * kc_updated_at, kc_status_reason) — these are intentionally not exposed
     * on the receiver-facing {@code StreamConfig} wire shape.
     */
    protected SsfClientStreamRepresentation fetchAdminStreamRepresentation(String clientId) throws IOException {
        ClientRepresentation client = findClientByClientId(clientId);
        Assertions.assertNotNull(client, () -> "expected client '" + clientId + "' to exist");
        String adminStreamUrl = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + client.getClientId() + "/stream";
        try (SimpleHttpResponse response = http.doGet(adminStreamUrl)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus(),
                    "admin GET stream should succeed for client " + clientId);
            return response.asJson(SsfClientStreamRepresentation.class);
        }
    }

    public static class StreamManagementKeycloakServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            config.features(Profile.Feature.SSF);
            config.log().categoryLevel("org.keycloak.protocol.ssf", "DEBUG");
            // The stream-management tests use a dummy loopback push URL
            // (DUMMY_PUSH_ENDPOINT) intentionally — pushes never succeed;
            // the assertions are about validation/storage paths. Relax the
            // http-scheme + private-host gate so the dummy URL is accepted;
            // the per-client ssf.validPushUrls allow-list configured on
            // each receiver below is still the SSRF defence under test.
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_ALLOW_INSECURE_PUSH_TARGETS, "true");
            return configured;
        }
    }

    public static class StreamManagementRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-transmitter-stream-mgmt");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");

            // The ssf.read / ssf.manage client scopes are auto-created by the
            // SSF RealmPostCreateEvent listener only *after* the clients are
            // imported, so we assign them to each receiver client via the
            // admin REST API from a @BeforeEach hook instead of declaring
            // them here as optionalClientScopes(...).

            realm.clients(
                    ClientBuilder.create(RECEIVER_RW)
                            .secret(RECEIVER_RW_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, DUMMY_PUSH_ALLOWLIST)
                            .build()
            );

            realm.clients(
                    ClientBuilder.create(RECEIVER_RO)
                            .secret(RECEIVER_RO_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, DUMMY_PUSH_ALLOWLIST)
                            .build()
            );

            realm.clients(
                    ClientBuilder.create(RECEIVER_SCOPED)
                            .secret(RECEIVER_SCOPED_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_STREAM_SUPPORTED_EVENTS_KEY,
                                    CaepSessionRevoked.class.getSimpleName())
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, DUMMY_PUSH_ALLOWLIST)
                            .build()
            );

            // SA-gate fixture: both client_credentials (the receiver's own SA)
            // and direct access grants (a regular user obtaining an
            // ssf.manage token for the same client) are valid token paths
            // here, so the only thing that distinguishes the two bearers
            // at the SSF layer is the SA gate itself.
            realm.clients(
                    ClientBuilder.create(RECEIVER_USER_AUTH)
                            .secret(RECEIVER_USER_AUTH_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(true)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, DUMMY_PUSH_ALLOWLIST)
                            .build()
            );

            realm.users(
                    UserBuilder.create(SSF_USER)
                            .email(SSF_USER + "@example.org")
                            .firstName("SSF")
                            .lastName("User")
                            .enabled(true)
                            .password(SSF_USER_PASSWORD)
                            .build()
            );

            return realm;
        }
    }
}
