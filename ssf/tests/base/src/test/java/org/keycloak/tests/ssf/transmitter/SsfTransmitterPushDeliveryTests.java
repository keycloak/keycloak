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
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.SsfProfile;
import org.keycloak.ssf.event.caep.CaepCredentialChange;
import org.keycloak.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.ssf.stream.StreamStatus;
import org.keycloak.ssf.stream.StreamStatusValue;
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
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
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
 * Tests for the SSF Transmitter push delivery pipeline end-to-end:
 * Keycloak user event → {@code SsfTransmitterEventListener} → SET mapping →
 * {@code SecurityEventTokenDispatcher} → {@code PushDeliveryService} → mock
 * receiver.
 *
 * <p>Each test fires a real LOGOUT user event by obtaining an access token
 * via a password grant (creating a user session) and then logging the
 * refresh token out, which the SSF event listener maps to a
 * {@link CaepSessionRevoked} SET and pushes to the mock receiver.
 *
 * <p>Three receiver clients are registered by {@link PushDeliveryRealm}:
 * <ul>
 *     <li>{@link #RECEIVER_SSF} — default SSF 1.0 profile, used for the
 *         happy path that covers a standard SSF SET payload.</li>
 *     <li>{@link #RECEIVER_SSE_CAEP} — legacy SSE CAEP profile, used to
 *         regression-guard the enum-switch fix in
 *         {@code SecurityEventTokenDispatcher#getNarrowedEventToken}.</li>
 *     <li>{@link #RECEIVER_CRED_ONLY} — receiver that only requests the
 *         CAEP credential-change event, used to assert that SETs carrying
 *         un-requested event types are not delivered.</li>
 * </ul>
 */
@KeycloakIntegrationTest(config = SsfTransmitterPushDeliveryTests.PushDeliveryKeycloakServerConfig.class)
public class SsfTransmitterPushDeliveryTests {

    static final String RECEIVER_SSF = "ssf-receiver-push-ssf";
    static final String RECEIVER_SSF_SECRET = "receiver-push-ssf-secret";

    static final String RECEIVER_SSE_CAEP = "ssf-receiver-push-sse-caep";
    static final String RECEIVER_SSE_CAEP_SECRET = "receiver-push-sse-caep-secret";

    static final String RECEIVER_CRED_ONLY = "ssf-receiver-push-cred-only";
    static final String RECEIVER_CRED_ONLY_SECRET = "receiver-push-cred-only-secret";

    static final String TEST_USER = "tester";
    static final String TEST_PASSWORD = "test";

    static final String PUSH_CONTEXT_PATH = "/ssf/push-delivery";
    static final String MOCK_PUSH_ENDPOINT = "http://127.0.0.1:8500" + PUSH_CONTEXT_PATH;
    static final String EXPECTED_PUSH_AUTH_HEADER = "Bearer dummy-push-delivery-receiver";

    static final long PUSH_WAIT_SECONDS = 5;

    @InjectRealm(config = PushDeliveryRealm.class)
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

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    private final BlockingQueue<CapturedPush> pushes = new LinkedBlockingQueue<>();

    @BeforeEach
    public void setup() {
        pushes.clear();

        mockReceiverServer.createContext(PUSH_CONTEXT_PATH, new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try (InputStream is = exchange.getRequestBody()) {
                    pushes.add(new CapturedPush(
                            new String(is.readAllBytes(), StandardCharsets.UTF_8),
                            exchange.getRequestHeaders().getFirst("Authorization"),
                            exchange.getRequestHeaders().getFirst("Content-Type")));
                }
                HttpServerUtil.sendResponse(exchange, 202, Map.of());
            }
        });

        // Assign the SSF scopes to every receiver client. These scopes are
        // created by RealmPostCreateEvent after client import, so declaring
        // them as optionalClientScopes on the realm config is silently
        // dropped.
        assignOptionalClientScopes(RECEIVER_SSF, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
        assignOptionalClientScopes(RECEIVER_SSE_CAEP, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
        assignOptionalClientScopes(RECEIVER_CRED_ONLY, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
    }

    @AfterEach
    public void cleanup() {
        List.of(RECEIVER_SSF, RECEIVER_SSE_CAEP, RECEIVER_CRED_ONLY)
                .forEach(this::bestEffortDeleteStream);
        try {
            mockReceiverServer.removeContext(PUSH_CONTEXT_PATH);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testPushDeliversCaepSessionRevokedOnUserLogout() throws Exception {

        String token = obtainReceiverToken(RECEIVER_SSF, RECEIVER_SSF_SECRET);
        StreamConfig stream = createPushStream(token, Set.of(CaepSessionRevoked.TYPE, CaepCredentialChange.TYPE));

        triggerUserLogout();

        CapturedPush captured = awaitPush();

        // Regression guard: the push must carry the receiver-supplied
        // authorization header verbatim and the SSF SET content-type.
        Assertions.assertEquals(EXPECTED_PUSH_AUTH_HEADER, captured.authorizationHeader,
                "push authorization header should be forwarded verbatim");
        Assertions.assertEquals(Ssf.APPLICATION_SECEVENT_JWT_TYPE, captured.contentType,
                "content-type should be application/secevent+jwt");

        JsonNode set = decodeSet(captured);

        Assertions.assertEquals(realm.getBaseUrl(), set.get("iss").asText());
        Assertions.assertEquals(stream.getAudience(), extractAudience(set),
                "aud in the SET should match the stream audience");

        JsonNode events = set.path("events");
        Assertions.assertTrue(events.has(CaepSessionRevoked.TYPE),
                "SET should carry the CAEP session-revoked event");

        // SSF 1.0 profile carries sub_id at the token level (complex user +
        // session subject id), not nested under the event.
        JsonNode subId = set.path("sub_id");
        Assertions.assertFalse(subId.isMissingNode(), "SSF 1.0 SET should carry a top-level sub_id");
        Assertions.assertTrue(subId.path("user").isObject(), "sub_id.user should describe the logged-out user");
        Assertions.assertTrue(subId.path("session").isObject(), "sub_id.session should describe the revoked session");
    }

    /**
     * Regression guard: an admin-UI-flavored stream (where
     * {@code events_requested} carries event <b>aliases</b> rather than
     * full URIs) must still receive matching pushes. Before the
     * dispatcher's gate canonicalized both sides through
     * {@link org.keycloak.ssf.event.SsfEventRegistry SsfEventRegistry},
     * the URI extracted from the SET token never matched the alias-form
     * set, every dispatch was suppressed with reason
     * {@code EVENT_NOT_REQUESTED}, and no push ever reached the
     * receiver — silently.
     *
     * <p>The receiver-facing stream-create endpoint deliberately accepts
     * both URI and alias form (see
     * {@link ClientStreamStore#getEventsConfig
     * ClientStreamStore.getEventsConfig}); persisting aliases here
     * mirrors what the admin UI sends.
     */
    @Test
    public void testPushDeliveredWhenEventsRequestedUsesAliases() throws Exception {

        String token = obtainReceiverToken(RECEIVER_SSF, RECEIVER_SSF_SECRET);
        // Pass the simple-class-name aliases the admin UI sends, not
        // CaepSessionRevoked.TYPE / CaepCredentialChange.TYPE URIs.
        StreamConfig stream = createPushStream(token, Set.of("CaepSessionRevoked", "CaepCredentialChange"));

        triggerUserLogout();

        CapturedPush captured = awaitPush();

        JsonNode set = decodeSet(captured);
        Assertions.assertEquals(stream.getAudience(), extractAudience(set),
                "aud in the SET should match the stream audience");
        Assertions.assertTrue(set.path("events").has(CaepSessionRevoked.TYPE),
                "SET should carry the CAEP session-revoked event even though events_requested was alias-flavored");
    }

    @Test
    public void testSseCaepProfileNarrowsEventShape() throws Exception {

        String token = obtainReceiverToken(RECEIVER_SSE_CAEP, RECEIVER_SSE_CAEP_SECRET);
        createPushStream(token, Set.of(CaepSessionRevoked.TYPE));

        triggerUserLogout();

        CapturedPush captured = awaitPush();
        JsonNode set = decodeSet(captured);

        // Regression guard for the SsfProfile enum-vs-String comparison bug:
        // with SSE CAEP profile, the SseCaepEventConverter wraps the subject
        // inside the event body and the SET no longer carries a top-level
        // sub_id. If the converter is not invoked (original bug), the payload
        // is still SSF 1.0-shaped with sub_id at the token level.
        Assertions.assertTrue(set.path("sub_id").isMissingNode() || set.path("sub_id").isNull(),
                "SSE CAEP SET should not carry a top-level sub_id");

        JsonNode event = set.path("events").path(CaepSessionRevoked.TYPE);
        Assertions.assertFalse(event.isMissingNode(), "SET should carry a CAEP session-revoked event");
        Assertions.assertTrue(event.path("subject").isObject(),
                "SSE CAEP event should carry a nested 'subject' object");
        Assertions.assertTrue(event.path("subject").path("user").isObject(),
                "SSE CAEP event.subject should wrap a 'user' entry");
        // Per SSE 1.0 §3.2 each facet of a complex subject is a sibling
        // key under `subject`. The native CaepSessionRevoked path always
        // carries a complex subject (user + session), so both facets
        // must appear at the same depth — not nested under user.
        Assertions.assertTrue(event.path("subject").path("session").isObject(),
                "SSE CAEP event.subject.session must be a sibling of subject.user, not nested under it");
        Assertions.assertTrue(event.path("subject").path("user").path("session").isMissingNode(),
                "regression guard: complex subject must not be wrapped under subject.user");
    }

    @Test
    public void testUnsupportedEventNotDelivered() throws Exception {

        // RECEIVER_CRED_ONLY only requests credential-change events; a
        // session-revoked push should be discarded by the dispatcher's
        // events_requested filter and never reach the mock receiver.
        String token = obtainReceiverToken(RECEIVER_CRED_ONLY, RECEIVER_CRED_ONLY_SECRET);
        createPushStream(token, Set.of(CaepCredentialChange.TYPE));

        triggerUserLogout();

        Assertions.assertNull(pushes.poll(2, TimeUnit.SECONDS),
                "no push should be delivered when the event type is not in events_requested");
    }

    @Test
    public void testDisabledStreamDoesNotReceivePush() throws Exception {

        String token = obtainReceiverToken(RECEIVER_SSF, RECEIVER_SSF_SECRET);
        StreamConfig stream = createPushStream(token, Set.of(CaepSessionRevoked.TYPE));

        // Disable the stream. The dispatcher must skip delivery of
        // regular events, but the spec requires a stream-updated SET
        // on every status transition, so we expect exactly one push
        // — the stream-updated notification — and then silence.
        StreamStatus disableStatus = new StreamStatus();
        disableStatus.setStreamId(stream.getStreamId());
        disableStatus.setStatus(StreamStatusValue.disabled.getStatusCode());
        try (SimpleHttpResponse response = http.doPost(streamsStatusEndpoint())
                .json(disableStatus)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus(),
                    "POST /streams/status should succeed");
        }

        // Drain the stream-updated SET fired by the disable transition.
        CapturedPush streamUpdated = awaitPush();
        JsonNode statusSet = decodeSet(streamUpdated);
        Assertions.assertTrue(
                statusSet.path("events").has(
                        "https://schemas.openid.net/secevent/ssf/event-type/stream-updated"),
                "disable transition should emit a stream-updated SET");

        triggerUserLogout();

        Assertions.assertNull(pushes.poll(2, TimeUnit.SECONDS),
                "disabled stream should not receive regular event pushes after the status transition");
    }

    @Test
    public void testPushDeliversCredentialChangeUpdateOnUpdateCredential() throws Exception {

        // UPDATE_CREDENTIAL with details.credential_type=password →
        // CAEP credential-change with change_type=update,
        // credential_type=password.
        String token = obtainReceiverToken(RECEIVER_CRED_ONLY, RECEIVER_CRED_ONLY_SECRET);
        StreamConfig stream = createPushStream(token, Set.of(CaepCredentialChange.TYPE));

        triggerCredentialEvent(EventType.UPDATE_CREDENTIAL, "password");

        CapturedPush captured = awaitPush();
        JsonNode set = decodeSet(captured);

        Assertions.assertEquals(stream.getAudience(), extractAudience(set),
                "aud should match the stream audience");

        JsonNode credentialChange = set.path("events").path(CaepCredentialChange.TYPE);
        Assertions.assertFalse(credentialChange.isMissingNode(),
                "SET should carry a CAEP credential-change event");
        Assertions.assertEquals("update", credentialChange.path("change_type").asText(),
                "UPDATE_CREDENTIAL must map to CAEP change_type=update");
        Assertions.assertEquals("password", credentialChange.path("credential_type").asText(),
                "credential_type should be propagated from the event details");
    }

    @Test
    public void testPushDeliversCredentialChangeDeleteOnRemoveCredential() throws Exception {

        // REMOVE_CREDENTIAL with details.credential_type=otp →
        // CAEP credential-change with change_type=delete,
        // credential_type=otp. Regression guard: prior behaviour
        // hardcoded change_type=update for every credential event.
        String token = obtainReceiverToken(RECEIVER_CRED_ONLY, RECEIVER_CRED_ONLY_SECRET);
        createPushStream(token, Set.of(CaepCredentialChange.TYPE));

        triggerCredentialEvent(EventType.REMOVE_CREDENTIAL, "otp");

        CapturedPush captured = awaitPush();
        JsonNode set = decodeSet(captured);

        JsonNode credentialChange = set.path("events").path(CaepCredentialChange.TYPE);
        Assertions.assertFalse(credentialChange.isMissingNode(),
                "SET should carry a CAEP credential-change event");
        Assertions.assertEquals("delete", credentialChange.path("change_type").asText(),
                "REMOVE_CREDENTIAL must map to CAEP change_type=delete");
        Assertions.assertEquals("app", credentialChange.path("credential_type").asText(),
                "credential_type should be propagated from the event details");
    }

    @Test
    public void testPushDeliversCredentialChangeUpdateOnResetPassword() throws Exception {

        // RESET_PASSWORD doesn't carry a credential_type detail —
        // the mapper falls back to "password". change_type stays
        // update because the password value is being changed, not
        // added or removed.
        String token = obtainReceiverToken(RECEIVER_CRED_ONLY, RECEIVER_CRED_ONLY_SECRET);
        createPushStream(token, Set.of(CaepCredentialChange.TYPE));

        triggerCredentialEvent(EventType.RESET_PASSWORD, null);

        CapturedPush captured = awaitPush();
        JsonNode set = decodeSet(captured);

        JsonNode credentialChange = set.path("events").path(CaepCredentialChange.TYPE);
        Assertions.assertFalse(credentialChange.isMissingNode(),
                "SET should carry a CAEP credential-change event");
        Assertions.assertEquals("update", credentialChange.path("change_type").asText(),
                "RESET_PASSWORD must map to CAEP change_type=update");
        Assertions.assertEquals("password", credentialChange.path("credential_type").asText(),
                "credential_type should fall back to 'password' when the event has no detail");
    }

    // --- helpers ---------------------------------------------------------

    /**
     * Obtains a new access token via password grant for {@link #TEST_USER},
     * then invokes the OIDC logout endpoint with the refresh token. That
     * fires a {@code LOGOUT} user event which the SSF event listener maps
     * to a {@link CaepSessionRevoked} SET for every enabled stream.
     */
    protected void triggerUserLogout() {
        AccessTokenResponse tokenResponse = oauthClient.passwordGrantRequest(TEST_USER, TEST_PASSWORD).send();
        Assertions.assertNotNull(tokenResponse.getAccessToken(),
                "password grant should succeed for the test user");
        Assertions.assertNotNull(tokenResponse.getRefreshToken(),
                "password grant response should include a refresh token");
        oauthClient.doLogout(tokenResponse.getRefreshToken());
    }

    /**
     * Fires a Keycloak credential-related user event from inside the
     * server context — the same code path that real flows use, so the
     * SSF event listener picks it up and the dispatcher narrows it
     * into a CAEP credential-change SET. Uses {@code runOnServer}
     * because EventBuilder/EventStore live in the server JVM and
     * have no public REST shim. {@code credentialType} is written to
     * {@link Details#CREDENTIAL_TYPE} when non-null; pass {@code null}
     * to omit the detail (e.g. for {@link EventType#RESET_PASSWORD}).
     */
    protected void triggerCredentialEvent(EventType type, String credentialType) {
        runOnServer.run(session -> {
            RealmModel serverRealm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(serverRealm, TEST_USER);
            EventBuilder builder = new EventBuilder(serverRealm, session)
                    .event(type)
                    .user(user);
            if (credentialType != null) {
                builder.detail(Details.CREDENTIAL_TYPE, credentialType);
            }
            builder.success();
        });
    }

    protected CapturedPush awaitPush() throws InterruptedException {
        CapturedPush captured = pushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNotNull(captured,
                () -> "expected a push within " + PUSH_WAIT_SECONDS + "s but the mock receiver saw nothing");
        return captured;
    }

    protected JsonNode decodeSet(CapturedPush captured) throws JWSInputException, IOException {
        JWSInput jws = new JWSInput(captured.body);
        // Parse as raw JsonNode; the typed SsfSecurityEventToken pulls in a
        // custom Jackson deserializer that needs a live Keycloak session.
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

    protected StreamConfig createPushStream(String token, Set<String> eventsRequested) throws IOException {
        StreamDeliveryConfig delivery = new StreamDeliveryConfig();
        delivery.setMethod(Ssf.DELIVERY_METHOD_PUSH_URI);
        delivery.setEndpointUrl(MOCK_PUSH_ENDPOINT);
        delivery.setAuthorizationHeader(EXPECTED_PUSH_AUTH_HEADER);

        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setDelivery(delivery);
        streamConfig.setEventsRequested(eventsRequested);
        streamConfig.setDescription("Push delivery integration test");

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

    protected String streamsStatusEndpoint() {
        return SsfTransmitterUrls.getStreamStatusEndpointUrl(realm.getBaseUrl());
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

    public static class PushDeliveryKeycloakServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            config.features(Profile.Feature.SSF);
            config.log().categoryLevel("org.keycloak.protocol.ssf", "DEBUG");
            // Relax the min-verification-interval rate limiter so these
            // tests aren't affected by it (they don't exercise /verify).
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_MIN_VERIFICATION_INTERVAL_SECONDS, "0");
            // Async pushes flow through the outbox — the drainer needs to
            // tick well inside PUSH_WAIT_SECONDS or every await times out.
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

    public static class PushDeliveryRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-transmitter-push-delivery");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");

            // Enable user events and register the SSF event listener so
            // LOGOUT user events flow into the SSF transmitter pipeline.
            realm.eventsEnabled(true);
            realm.adminEventsEnabled(true);
            realm.eventsListeners("jboss-logging", "ssf-events");

            // Test user used by the password grant + logout flow in each
            // test. Creating a password grant for this user implicitly
            // creates a user session that the subsequent logout terminates.
            realm.users(
                    UserBuilder.create(TEST_USER)
                            .email(TEST_USER + "@local.test")
                            .firstName("Theo")
                            .lastName("Tester")
                            .enabled(true)
                            .password(TEST_PASSWORD)
                            .build()
            );

            realm.clients(
                    ClientBuilder.create(RECEIVER_SSF)
                            .secret(RECEIVER_SSF_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "http://127.0.0.1:8500/*")
                            .attribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "ALL")
                            .build()
            );

            realm.clients(
                    ClientBuilder.create(RECEIVER_SSE_CAEP)
                            .secret(RECEIVER_SSE_CAEP_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "http://127.0.0.1:8500/*")
                            .attribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "ALL")
                            .attribute(ClientStreamStore.SSF_PROFILE_KEY, SsfProfile.SSE_CAEP.name())
                            .build()
            );

            realm.clients(
                    ClientBuilder.create(RECEIVER_CRED_ONLY)
                            .secret(RECEIVER_CRED_ONLY_SECRET)
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
