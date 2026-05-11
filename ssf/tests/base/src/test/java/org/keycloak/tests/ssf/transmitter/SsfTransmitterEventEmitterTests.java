package org.keycloak.tests.ssf.transmitter;

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
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
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
 * Integration tests for the synthetic SSF event emitter admin endpoint
 * ({@code POST /admin/realms/{realm}/ssf/clients/{clientIdentifier}/events/emit}).
 *
 * <p>This endpoint lets a trusted IAM management client forward upstream
 * events (e.g. LDAP credential changes that Keycloak didn't observe) to
 * an SSF receiver. Auth is a custom pipeline layered on admin auth:
 * <ol>
 *     <li>Receiver must have {@code ssf.allowEmitEvents=true}.</li>
 *     <li>Receiver must have a non-empty {@code ssf.emitEventsRole}.</li>
 *     <li>Calling token must be a service-account token (M2M only).</li>
 *     <li>Calling client must hold that client role on the receiver.</li>
 * </ol>
 *
 * <p>After the gate, the receiver's normal dispatch filters still apply
 * (subject subscription + {@code events_requested}). The tests here
 * cover every branch of that matrix plus a SET shape regression guard.
 *
 * <p>Realm layout:
 * <ul>
 *     <li>{@link #RECEIVER} — receiver client with the feature opted in and
 *         the emitter role configured.</li>
 *     <li>{@link #MGMT_EMITTER} — management client whose service account
 *         gets granted the emitter role in {@link #setup()}.</li>
 *     <li>{@link #MGMT_NO_ROLE} — management client whose service account
 *         does NOT hold the role, used to cover the missing-role branch.</li>
 *     <li>{@link #TEST_USER} — subject of the synthetic events; pre-
 *         subscribed to the receiver in {@link #setup()}.</li>
 * </ul>
 */
@KeycloakIntegrationTest(config = SsfTransmitterEventEmitterTests.EmitServerConfig.class)
public class SsfTransmitterEventEmitterTests {

    static final String RECEIVER = "ssf-receiver-emit";
    static final String RECEIVER_SECRET = "receiver-emit-secret";

    static final String MGMT_EMITTER = "ssf-mgmt-emitter";
    static final String MGMT_EMITTER_SECRET = "mgmt-emitter-secret";

    static final String MGMT_NO_ROLE = "ssf-mgmt-no-role";
    static final String MGMT_NO_ROLE_SECRET = "mgmt-no-role-secret";

    static final String EMITTER_ROLE = "ssf-event-emitter";

    static final String TEST_USER = "emit-tester";
    static final String TEST_EMAIL = "emit-tester@local.test";
    static final String TEST_PASSWORD = "test";

    static final String PUSH_CONTEXT_PATH = "/ssf/push-emit";
    static final String MOCK_PUSH_ENDPOINT = "http://127.0.0.1:8500" + PUSH_CONTEXT_PATH;
    static final String PUSH_AUTH_HEADER = "Bearer dummy-emit-receiver";

    static final long PUSH_WAIT_SECONDS = 5;

    @InjectRealm(config = EmitRealm.class)
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
    public void setup() throws IOException {
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

        // Ensure a stable starting state for the attributes the auth
        // pipeline reads. Tests that exercise the 403 branches override
        // these temporarily and reset them back in finally blocks.
        // Role value is stored in the same format the admin UI role
        // picker produces ("clientId.roleName" for client roles), so
        // SsfAuthUtil.hasRole looks it up under resource_access[receiver].
        setReceiverAttributes(true, RECEIVER + "." + EMITTER_ROLE);

        // SSF scopes + stream so the receiver can actually receive
        // synthetic events. The receiver needs a registered stream for
        // the dispatch path to find a delivery endpoint.
        assignOptionalClientScopes(RECEIVER, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
        createPushStream();

        // Define the emitter client role on the receiver and grant it
        // to MGMT_EMITTER's service account. This is the one-time wiring
        // a customer would do manually in the admin console.
        ensureReceiverRole(EMITTER_ROLE);
        grantRoleToServiceAccount(MGMT_EMITTER, RECEIVER, EMITTER_ROLE);

        // Pre-subscribe TEST_USER so the subject filter doesn't drop
        // the happy-path event. Tests that cover the unsubscribed
        // branch use a different subject lookup.
        subscribeTestUser();
    }

    @AfterEach
    public void cleanup() {
        bestEffortDeleteStream();
        bestEffortRemoveNotify();
        bestEffortDeleteTestOrganizations();
        try {
            mockReceiverServer.removeContext(PUSH_CONTEXT_PATH);
        } catch (IllegalArgumentException ignored) {
        }
    }

    // ---- happy path ----

    @Test
    public void emit_happyPath_dispatchesAndPushes() throws Exception {
        String mgmtToken = obtainServiceAccountToken(MGMT_EMITTER, MGMT_EMITTER_SECRET);

        try (SimpleHttpResponse res = emit(mgmtToken, "CaepCredentialChange", TEST_EMAIL,
                Map.of("credential_type", "password", "change_type", "update"))) {
            Assertions.assertEquals(200, res.getStatus(),
                    "emit should succeed for a properly authorized management client");
            JsonNode body = res.asJson();
            Assertions.assertEquals("dispatched", body.get("status").asText(),
                    "status should be 'dispatched' when all filters pass");
            Assertions.assertNotNull(body.get("jti"), "response should include the SET jti for correlation");
            Assertions.assertFalse(body.get("jti").asText().isBlank(), "jti should be non-blank");
        }

        String push = pushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNotNull(push, "dispatched event should reach the mock receiver");

        // Regression guard on SET shape: the synthetic event must come
        // through as a fully-formed SSF 1.0 SET with iss/aud/events and
        // the user sub_id that the receiver's configured user subject
        // format dictates.
        JsonNode set = decodeSet(push);
        Assertions.assertEquals(realm.getBaseUrl(), set.get("iss").asText(),
                "synthetic SET should carry the realm issuer");
        JsonNode events = set.path("events");
        Assertions.assertTrue(events.has(CaepCredentialChange.TYPE),
                "synthetic SET should carry the caller-provided event type");
        Assertions.assertEquals("password",
                events.path(CaepCredentialChange.TYPE).path("credential_type").asText(),
                "event payload fields should survive the round-trip");
        Assertions.assertTrue(set.path("sub_id").isObject(),
                "SSF 1.0 synthetic SET should carry a top-level sub_id for the resolved user");
    }

    // ---- auth matrix ----

    @Test
    public void emit_receiverOptedOut_returns403() throws Exception {
        setReceiverAttributes(false, RECEIVER + "." + EMITTER_ROLE);
        String mgmtToken = obtainServiceAccountToken(MGMT_EMITTER, MGMT_EMITTER_SECRET);

        try (SimpleHttpResponse res = emit(mgmtToken, "CaepCredentialChange", TEST_EMAIL,
                Map.of("credential_type", "password"))) {
            Assertions.assertEquals(403, res.getStatus(),
                    "emit must be refused when receiver did not opt in");
            Assertions.assertEquals("emit_not_allowed", res.asJson().get("error").asText());
        }
        Assertions.assertNull(pushes.poll(1, TimeUnit.SECONDS),
                "refused call must not result in any push");
    }

    @Test
    public void emit_roleNotConfigured_returns403() throws Exception {
        setReceiverAttributes(true, "");
        String mgmtToken = obtainServiceAccountToken(MGMT_EMITTER, MGMT_EMITTER_SECRET);

        try (SimpleHttpResponse res = emit(mgmtToken, "CaepCredentialChange", TEST_EMAIL,
                Map.of("credential_type", "password"))) {
            Assertions.assertEquals(403, res.getStatus(),
                    "empty emit-events role must block dispatch — refuse rather than accept anonymously");
            Assertions.assertEquals("emit_role_not_configured", res.asJson().get("error").asText());
        }
    }

    @Test
    public void emit_notServiceAccount_returns403() throws Exception {
        // Note: the @InjectAdminClient token is itself a service-account
        // token (the temp-admin client uses client_credentials), so it
        // would slip past step 3. Use a real user-delegated token
        // instead via password grant for TEST_USER, which the auth
        // pipeline must refuse regardless of any roles the user holds.
        AccessTokenResponse userTokenResponse =
                oauthClient.passwordGrantRequest(TEST_USER, TEST_PASSWORD).send();
        Assertions.assertNotNull(userTokenResponse.getAccessToken(),
                "password grant for TEST_USER should succeed");
        String userToken = userTokenResponse.getAccessToken();

        try (SimpleHttpResponse res = emit(userToken, "CaepCredentialChange", TEST_EMAIL,
                Map.of("credential_type", "password"))) {
            Assertions.assertEquals(403, res.getStatus(),
                    "user-delegated tokens must not be accepted as emitters");
            Assertions.assertEquals("not_service_account", res.asJson().get("error").asText());
        }
    }

    @Test
    public void emit_missingRole_returns403() throws Exception {
        String mgmtToken = obtainServiceAccountToken(MGMT_NO_ROLE, MGMT_NO_ROLE_SECRET);

        try (SimpleHttpResponse res = emit(mgmtToken, "CaepCredentialChange", TEST_EMAIL,
                Map.of("credential_type", "password"))) {
            Assertions.assertEquals(403, res.getStatus(),
                    "caller without the emitter role must be refused");
            Assertions.assertEquals("emit_role_missing", res.asJson().get("error").asText());
        }
    }

    @Test
    public void emit_noBearerToken_returns401() throws Exception {
        String url = emitEndpointUrl();
        try (SimpleHttpResponse res = http.doPost(url)
                .json(Map.of("eventType", "CaepCredentialChange",
                        "sub_id", Map.of("format", "email", "email", TEST_EMAIL),
                        "event", Map.of("credential_type", "password")))
                .asResponse()) {
            Assertions.assertEquals(401, res.getStatus(),
                    "unauthenticated call must be rejected by the admin auth layer");
        }
    }

    // ---- request validation ----

    @Test
    public void emit_missingEventType_returns400() throws Exception {
        String mgmtToken = obtainServiceAccountToken(MGMT_EMITTER, MGMT_EMITTER_SECRET);
        try (SimpleHttpResponse res = http.doPost(emitEndpointUrl())
                .auth(mgmtToken)
                .json(Map.of(
                        "sub_id", Map.of("format", "email", "email", TEST_EMAIL),
                        "event", Map.of("credential_type", "password")))
                .asResponse()) {
            Assertions.assertEquals(400, res.getStatus(),
                    "eventType is required at the top level");
            Assertions.assertEquals("invalid_request", res.asJson().get("error").asText());
        }
    }

    @Test
    public void emit_missingSubjectId_returns400() throws Exception {
        String mgmtToken = obtainServiceAccountToken(MGMT_EMITTER, MGMT_EMITTER_SECRET);
        try (SimpleHttpResponse res = http.doPost(emitEndpointUrl())
                .auth(mgmtToken)
                .json(Map.of(
                        "eventType", "CaepCredentialChange",
                        "event", Map.of("credential_type", "password")))
                .asResponse()) {
            Assertions.assertEquals(400, res.getStatus(),
                    "sub_id is required");
            Assertions.assertEquals("invalid_request", res.asJson().get("error").asText());
        }
    }

    // ---- dispatch filter branches ----

    @Test
    public void emit_unsubscribedSubject_returnsDroppedUnsubscribed() throws Exception {
        // Remove the notify attribute so the subject is no longer
        // subscribed — default_subjects=NONE means the subject filter
        // will drop.
        bestEffortRemoveNotify();

        String mgmtToken = obtainServiceAccountToken(MGMT_EMITTER, MGMT_EMITTER_SECRET);
        try (SimpleHttpResponse res = emit(mgmtToken, "CaepCredentialChange", TEST_EMAIL,
                Map.of("credential_type", "password"))) {
            Assertions.assertEquals(200, res.getStatus());
            Assertions.assertEquals("dropped_unsubscribed",
                    res.asJson().get("status").asText(),
                    "unsubscribed subject should be filtered, not dispatched");
        }
        Assertions.assertNull(pushes.poll(2, TimeUnit.SECONDS),
                "dropped_unsubscribed must not result in a push");
    }

    @Test
    public void emit_eventTypeNotRequested_returnsDroppedFiltered() throws Exception {
        // The stream created in setup() only requests
        // CaepCredentialChange. Emitting a CaepSessionRevoked should be
        // dropped by the events_requested filter.
        String mgmtToken = obtainServiceAccountToken(MGMT_EMITTER, MGMT_EMITTER_SECRET);
        try (SimpleHttpResponse res = emit(mgmtToken, "CaepSessionRevoked", TEST_EMAIL,
                Map.of())) {
            Assertions.assertEquals(200, res.getStatus());
            Assertions.assertEquals("dropped_filtered",
                    res.asJson().get("status").asText(),
                    "event type outside events_requested should be filtered");
        }
        Assertions.assertNull(pushes.poll(2, TimeUnit.SECONDS),
                "dropped_filtered must not result in a push");
    }

    @Test
    public void emit_unknownEventType_returnsUnknownEventType() throws Exception {
        String mgmtToken = obtainServiceAccountToken(MGMT_EMITTER, MGMT_EMITTER_SECRET);
        try (SimpleHttpResponse res = emit(mgmtToken, "NotARegisteredEvent", TEST_EMAIL,
                Map.of())) {
            Assertions.assertEquals(400, res.getStatus());
            Assertions.assertEquals("unknown_event_type",
                    res.asJson().get("error").asText(),
                    "unknown alias / URI should be reported distinctly");
        }
    }

    @Test
    public void emit_complexSubjectId_dispatchesAndForwardsVerbatim() throws Exception {
        // The whole point of moving to RFC 9493 SubjectId on the wire is
        // expressing things admin shorthand can't — e.g. a session-revoked
        // event that names both the user and the revoked session. Build
        // the receiver to accept session-revoked, then push a complex
        // sub_id and assert the receiver sees the same nested shape.
        setStreamEventsRequested(Set.of(CaepSessionRevoked.TYPE));

        String userUuid = realm.admin().users().searchByEmail(TEST_EMAIL, true).get(0).getId();
        String issuer = realm.getBaseUrl();

        String mgmtToken = obtainServiceAccountToken(MGMT_EMITTER, MGMT_EMITTER_SECRET);
        try (SimpleHttpResponse res = http.doPost(emitEndpointUrl())
                .auth(mgmtToken)
                .json(Map.of(
                        "eventType", "CaepSessionRevoked",
                        "sub_id", Map.of(
                                "format", "complex",
                                "user", Map.of("format", "iss_sub", "iss", issuer, "sub", userUuid),
                                "session", Map.of("format", "opaque", "id", "fake-session-id-123")),
                        "event", Map.of("event_timestamp", System.currentTimeMillis() / 1000)))
                .asResponse()) {
            Assertions.assertEquals(200, res.getStatus());
            Assertions.assertEquals("dispatched", res.asJson().get("status").asText(),
                    "complex sub_id should resolve via the user facet and dispatch");
        }

        String push = pushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNotNull(push, "complex-subject event should reach the mock receiver");
        JsonNode set = decodeSet(push);
        JsonNode subId = set.path("sub_id");
        Assertions.assertEquals("complex", subId.path("format").asText(),
                "transmitter must forward the emitter's complex sub_id verbatim");
        Assertions.assertEquals("iss_sub", subId.path("user").path("format").asText());
        Assertions.assertEquals(userUuid, subId.path("user").path("sub").asText());
        Assertions.assertEquals("fake-session-id-123", subId.path("session").path("id").asText(),
                "session facet must round-trip through the dispatch + sign + push pipeline");
    }

    @Test
    public void emit_orgSubjectViaTenant_dispatches() throws Exception {
        // Org-only emission: the sub_id is a complex subject whose only
        // facet is the tenant (org alias). The user facet is omitted, so
        // resolution lands on the org and the receiver's per-org notify
        // attribute drives the subscription gate.
        String orgAlias = createOrgWithNotify();

        String mgmtToken = obtainServiceAccountToken(MGMT_EMITTER, MGMT_EMITTER_SECRET);
        try (SimpleHttpResponse res = http.doPost(emitEndpointUrl())
                .auth(mgmtToken)
                .json(Map.of(
                        "eventType", "CaepCredentialChange",
                        "sub_id", Map.of(
                                "format", "complex",
                                "tenant", Map.of("format", "opaque", "id", orgAlias)),
                        "event", Map.of("credential_type", "password", "change_type", "update")))
                .asResponse()) {
            Assertions.assertEquals(200, res.getStatus());
            Assertions.assertEquals("dispatched", res.asJson().get("status").asText(),
                    "tenant-only sub_id should dispatch when the org is subscribed to the receiver");
        }

        String push = pushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNotNull(push, "org-subject event should reach the mock receiver");
        JsonNode set = decodeSet(push);
        Assertions.assertEquals("complex", set.path("sub_id").path("format").asText());
        Assertions.assertEquals(orgAlias, set.path("sub_id").path("tenant").path("id").asText(),
                "tenant facet must be forwarded verbatim");
    }

    @Test
    public void emit_orgSubjectNotSubscribed_returnsDroppedUnsubscribed() throws Exception {
        // Org exists but has no ssf.notify attribute set for the receiver;
        // default_subjects=NONE on the receiver means the dispatch is
        // refused.
        String orgAlias = createOrgWithoutNotify();

        String mgmtToken = obtainServiceAccountToken(MGMT_EMITTER, MGMT_EMITTER_SECRET);
        try (SimpleHttpResponse res = http.doPost(emitEndpointUrl())
                .auth(mgmtToken)
                .json(Map.of(
                        "eventType", "CaepCredentialChange",
                        "sub_id", Map.of(
                                "format", "complex",
                                "tenant", Map.of("format", "opaque", "id", orgAlias)),
                        "event", Map.of("credential_type", "password")))
                .asResponse()) {
            Assertions.assertEquals(200, res.getStatus());
            Assertions.assertEquals("dropped_unsubscribed",
                    res.asJson().get("status").asText(),
                    "unsubscribed org should be filtered like an unsubscribed user");
        }
    }

    @Test
    public void emit_streamLifecycleEvent_returnsEventTypeNotEmittable() throws Exception {
        // SsfStreamVerificationEvent + SsfStreamUpdatedEvent are
        // protocol-internal lifecycle signals owned by the transmitter.
        // Letting an external emitter forge them would let it spoof
        // transmitter behaviour towards the receiver, so the API must
        // refuse them up front (regardless of whether the receiver
        // requested them or the subject would resolve).
        String mgmtToken = obtainServiceAccountToken(MGMT_EMITTER, MGMT_EMITTER_SECRET);
        try (SimpleHttpResponse res = emit(mgmtToken, "SsfStreamVerificationEvent", TEST_EMAIL,
                Map.of("state", "test"))) {
            Assertions.assertEquals(400, res.getStatus());
            Assertions.assertEquals("event_type_not_emittable",
                    res.asJson().get("error").asText(),
                    "stream lifecycle events must be rejected by the synthetic emitter");
        }
        Assertions.assertNull(pushes.poll(1, TimeUnit.SECONDS),
                "rejected stream event must not produce a push");
    }

    @Test
    public void emit_subjectNotFound_returnsSubjectNotFound() throws Exception {
        String mgmtToken = obtainServiceAccountToken(MGMT_EMITTER, MGMT_EMITTER_SECRET);
        try (SimpleHttpResponse res = emit(mgmtToken, "CaepCredentialChange",
                "nonexistent@nowhere.test",
                Map.of("credential_type", "password"))) {
            Assertions.assertEquals(400, res.getStatus());
            Assertions.assertEquals("subject_not_found",
                    res.asJson().get("error").asText(),
                    "unresolvable subject should be reported distinctly");
        }
    }

    // --- helpers ---------------------------------------------------------

    protected String emitEndpointUrl() {
        // Emit endpoint looks the receiver up by OAuth clientId, not
        // the internal UUID — consistent with how an IAM management
        // client would configure a well-known target.
        return keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + RECEIVER + "/events/emit";
    }

    protected SimpleHttpResponse emit(String token, String eventType,
                                      String userEmail, Map<String, Object> event) throws IOException {
        // Wire shape is an RFC 9493 SubjectId — pick the simplest
        // format that fits the test user (email) so assertions stay
        // readable. Tests covering other formats build their own
        // sub_id payload inline.
        return http.doPost(emitEndpointUrl())
                .auth(token)
                .json(Map.of(
                        "eventType", eventType,
                        "sub_id", Map.of("format", "email", "email", userEmail),
                        "event", event))
                .asResponse();
    }

    protected String obtainServiceAccountToken(String clientId, String secret) throws IOException {
        String tokenUrl = realm.getBaseUrl() + "/protocol/openid-connect/token";
        try (SimpleHttpResponse response = http.doPost(tokenUrl)
                .authBasic(clientId, secret)
                .param("grant_type", "client_credentials")
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus(),
                    () -> "client_credentials grant should succeed for " + clientId);
            return response.asJson().get("access_token").asText();
        }
    }

    protected String obtainReceiverManageToken() throws IOException {
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

    protected void createPushStream() throws IOException {
        StreamDeliveryConfig delivery = new StreamDeliveryConfig();
        delivery.setMethod(Ssf.DELIVERY_METHOD_PUSH_URI);
        delivery.setEndpointUrl(MOCK_PUSH_ENDPOINT);
        delivery.setAuthorizationHeader(PUSH_AUTH_HEADER);

        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setDelivery(delivery);
        streamConfig.setEventsRequested(Set.of(CaepCredentialChange.TYPE));
        streamConfig.setDescription("Synthetic emitter integration test");

        String token = obtainReceiverManageToken();
        try (SimpleHttpResponse response = http.doPost(SsfTransmitterUrls.getStreamsEndpointUrl(realm.getBaseUrl()))
                .json(streamConfig)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(201, response.getStatus(),
                    "stream creation should succeed in test setup");
        }
    }

    /**
     * Re-creates the receiver's SSF stream with a different
     * {@code events_requested} set. Used by tests that exercise event
     * types other than the default {@link CaepCredentialChange} the
     * baseline stream registers in {@link #createPushStream()}.
     */
    protected void setStreamEventsRequested(Set<String> eventsRequested) throws IOException {
        bestEffortDeleteStream();

        StreamDeliveryConfig delivery = new StreamDeliveryConfig();
        delivery.setMethod(Ssf.DELIVERY_METHOD_PUSH_URI);
        delivery.setEndpointUrl(MOCK_PUSH_ENDPOINT);
        delivery.setAuthorizationHeader(PUSH_AUTH_HEADER);

        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setDelivery(delivery);
        streamConfig.setEventsRequested(eventsRequested);
        streamConfig.setDescription("Synthetic emitter integration test (custom events)");

        String token = obtainReceiverManageToken();
        try (SimpleHttpResponse response = http.doPost(SsfTransmitterUrls.getStreamsEndpointUrl(realm.getBaseUrl()))
                .json(streamConfig)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(201, response.getStatus(),
                    "stream re-creation should succeed");
        }
    }

    protected void subscribeTestUser() throws IOException {
        String url = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + RECEIVER + "/subjects/add";
        try (SimpleHttpResponse ignored = http.doPost(url)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .json(Map.of("type", "user-email", "value", TEST_EMAIL))
                .asResponse()) {
        }
    }

    protected void setReceiverAttributes(boolean allowEmit, String role) {
        ClientResource clientResource = realm.admin().clients().get(findClientByClientId(RECEIVER).getId());
        ClientRepresentation rep = clientResource.toRepresentation();
        Map<String, String> attrs = rep.getAttributes();
        attrs.put(ClientStreamStore.SSF_ALLOW_EMIT_EVENTS_KEY, String.valueOf(allowEmit));
        attrs.put(ClientStreamStore.SSF_EMIT_EVENTS_ROLE_KEY, role);
        rep.setAttributes(attrs);
        clientResource.update(rep);
    }

    protected void ensureReceiverRole(String roleName) {
        ClientResource receiver = realm.admin().clients().get(findClientByClientId(RECEIVER).getId());
        boolean exists = receiver.roles().list().stream().anyMatch(r -> roleName.equals(r.getName()));
        if (exists) {
            return;
        }
        RoleRepresentation role = new RoleRepresentation();
        role.setName(roleName);
        role.setDescription("Grants the holder permission to push synthetic SSF events for this receiver.");
        receiver.roles().create(role);
    }

    protected void grantRoleToServiceAccount(String mgmtClientId, String targetClientId, String roleName) {
        ClientRepresentation mgmt = findClientByClientId(mgmtClientId);
        UserRepresentation saUser = realm.admin().clients().get(mgmt.getId()).getServiceAccountUser();

        String targetUuid = findClientByClientId(targetClientId).getId();
        RoleRepresentation role = realm.admin().clients().get(targetUuid).roles().get(roleName).toRepresentation();

        // The admin API is idempotent for add() — if the mapping already
        // exists it's a no-op, which is what we want for @BeforeEach.
        realm.admin().users().get(saUser.getId()).roles().clientLevel(targetUuid).add(List.of(role));
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

    protected void bestEffortDeleteStream() {
        try {
            String url = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                    + "/ssf/clients/" + RECEIVER + "/stream";
            http.doDelete(url).auth(adminClient.tokenManager().getAccessTokenString()).asResponse().close();
        } catch (Exception ignored) {
        }
    }

    protected void bestEffortRemoveNotify() {
        try {
            String url = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                    + "/ssf/clients/" + RECEIVER + "/subjects/remove";
            http.doPost(url)
                    .auth(adminClient.tokenManager().getAccessTokenString())
                    .json(Map.of("type", "user-email", "value", TEST_EMAIL))
                    .asResponse().close();
        } catch (Exception ignored) {
        }
    }

    protected JsonNode decodeSet(String encoded) throws Exception {
        JWSInput jws = new JWSInput(encoded);
        return JsonSerialization.readValue(jws.getContent(), JsonNode.class);
    }

    /**
     * Creates a test organization with a unique alias and pre-subscribes
     * it to the receiver via the {@code ssf.notify.<receiverClientId>}
     * org attribute. Returns the alias so the test can use it as the
     * tenant id in the {@code sub_id} payload.
     */
    protected String createOrgWithNotify() {
        String alias = "emit-org-notified-" + System.nanoTime();
        return createTestOrganization(alias, true);
    }

    protected String createOrgWithoutNotify() {
        String alias = "emit-org-silent-" + System.nanoTime();
        return createTestOrganization(alias, false);
    }

    protected String createTestOrganization(String alias, boolean withNotifyAttribute) {
        OrganizationRepresentation rep = new OrganizationRepresentation();
        rep.setName(alias);
        rep.setAlias(alias);
        rep.addDomain(new org.keycloak.representations.idm.OrganizationDomainRepresentation(alias + ".local.test"));
        if (withNotifyAttribute) {
            // ssf.notify.<receiverClientId>=true so the synthetic emitter's
            // subscription gate accepts the org in default_subjects=NONE
            // mode. The emitter implementation uses
            // SsfNotifyAttributes.isOrganizationNotified, which reads exactly
            // this attribute.
            rep.singleAttribute("ssf.notify." + RECEIVER, "true");
        }
        try (jakarta.ws.rs.core.Response response = realm.admin().organizations().create(rep)) {
            Assertions.assertEquals(201, response.getStatus(),
                    "test organization creation should succeed");
        }
        return alias;
    }

    protected void bestEffortDeleteTestOrganizations() {
        try {
            realm.admin().organizations().getAll().stream()
                    .filter(o -> o.getAlias() != null
                            && (o.getAlias().startsWith("emit-org-notified-")
                                    || o.getAlias().startsWith("emit-org-silent-")))
                    .forEach(o -> realm.admin().organizations().get(o.getId()).delete());
        } catch (Exception ignored) {
        }
    }

    // --- config ----------------------------------------------------------

    public static class EmitServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            // ORGANIZATION feature is required for the tenant-subject
            // emit test to exercise the OrganizationProvider lookup
            // path. Without it, EventEmitterService.resolveOrganization
            // short-circuits to null and the test would always fall to
            // SUBJECT_NOT_FOUND.
            config.features(Profile.Feature.SSF, Profile.Feature.ORGANIZATION);
            config.log().categoryLevel("org.keycloak.ssf", "DEBUG");
            // Async pushes flow through the outbox — without a fast
            // drainer tick every happy-path assertion times out.
            config.spiOption("ssf-transmitter", "default",
                    DefaultSsfTransmitterProviderFactory.CONFIG_OUTBOX_DRAINER_INTERVAL, "500ms");
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

    public static class EmitRealm implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-transmitter-emit");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");
            realm.organizationsEnabled(true);

            realm.eventsEnabled(true);
            realm.adminEventsEnabled(true);
            realm.eventsListeners("jboss-logging", "ssf-events");

            realm.users(
                    UserBuilder.create(TEST_USER)
                            .email(TEST_EMAIL)
                            .firstName("Emit")
                            .lastName("Tester")
                            .enabled(true)
                            .password(TEST_PASSWORD)
                            .build()
            );

            // Receiver: opt-in attributes are set in @BeforeEach so each
            // test can override them via setReceiverAttributes.
            realm.clients(
                    ClientBuilder.create(RECEIVER)
                            .secret(RECEIVER_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "http://127.0.0.1:8500/*")
                            .attribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "NONE")
                            .build()
            );

            // Management client that will be granted the emitter role
            // in @BeforeEach. fullScopeEnabled is required so the token
            // carries the receiver's client role in its resource_access
            // claim — AdminAuth.hasAppRole checks both user.hasRole and
            // client.hasScope, and the latter fails for fullScope=false
            // unless the role is explicitly scope-mapped.
            realm.clients(
                    ClientBuilder.create(MGMT_EMITTER)
                            .secret(MGMT_EMITTER_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .fullScopeEnabled(true)
                            .build()
            );

            // Management client that never gets the role — covers the
            // emit_role_missing branch.
            realm.clients(
                    ClientBuilder.create(MGMT_NO_ROLE)
                            .secret(MGMT_NO_ROLE_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .fullScopeEnabled(true)
                            .build()
            );

            return realm;
        }
    }
}
