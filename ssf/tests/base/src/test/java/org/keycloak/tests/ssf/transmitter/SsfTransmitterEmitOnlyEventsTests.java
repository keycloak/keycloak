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
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
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
 * Integration tests for the per-receiver {@code ssf.emitOnlyEvents}
 * gate ({@link SsfTransmitterEventListener#isEmitOnlyEventForReceiver}).
 *
 * <p>The gate runs in the native-listener path only — when the listener
 * resolves a Keycloak event into an SSF event whose type appears in the
 * receiver's emit-only set, the token is dropped before it ever
 * reaches the dispatcher / outbox. The synthetic-emit endpoint
 * deliberately bypasses this gate; that's its whole purpose.
 *
 * <p>Tests use a single receiver configured with
 * {@code ssf.emitOnlyEvents=CaepSessionRevoked} and
 * {@code ssf.defaultSubjects=ALL} (so the subject filter doesn't
 * confuse the picture), plus a registered push stream pointing at a
 * mock HTTP receiver. Three branches are exercised:
 *
 * <ol>
 *     <li><b>Native LOGOUT is suppressed.</b> A real password-grant /
 *         logout flow fires a Keycloak {@code LOGOUT} event which would
 *         normally map to {@code CaepSessionRevoked}. With the gate in
 *         place, no push reaches the receiver.</li>
 *     <li><b>Synthetic emit still delivers.</b> An admin caller posts to
 *         the {@code /events/emit} endpoint for the same
 *         {@code CaepSessionRevoked} type and the SET arrives at the
 *         mock receiver.</li>
 *     <li><b>Gate is event-type-specific.</b> A credential-update event
 *         (not in the emit-only set) still auto-emits as
 *         {@code CaepCredentialChange}.</li>
 * </ol>
 */
@KeycloakIntegrationTest(config = SsfTransmitterEmitOnlyEventsTests.EmitOnlyServerConfig.class)
public class SsfTransmitterEmitOnlyEventsTests {

    static final String RECEIVER = "ssf-receiver-emit-only";
    static final String RECEIVER_SECRET = "receiver-emit-only-secret";

    static final String TEST_USER = "emit-only-tester";
    static final String TEST_PASSWORD = "test";

    static final String PUSH_CONTEXT_PATH = "/ssf/push-emit-only";
    static final String MOCK_PUSH_ENDPOINT = "http://127.0.0.1:8500" + PUSH_CONTEXT_PATH;
    static final String PUSH_AUTH_HEADER = "Bearer dummy-emit-only-receiver";

    /**
     * Wait for an expected push. Long enough to give the outbox drainer
     * (configured at 500ms ticks below) at least 6 ticks to pick the
     * row up before the test calls it a miss.
     */
    static final long PUSH_WAIT_SECONDS = 5;

    /**
     * Wait used when asserting that NO push arrives. Shorter — we just
     * need to give the listener a fair chance to run; if a push were
     * coming, it would arrive well inside this window.
     */
    static final long NO_PUSH_WAIT_SECONDS = 3;

    @InjectRealm(config = EmitOnlyRealm.class)
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

        // Realm-built clients can't have optional scopes assigned at
        // declaration time — the SSF scopes are created by the realm
        // post-create event after the client import lands. Re-assign
        // here per test so the receiver's CC token can request them.
        assignOptionalClientScopes(RECEIVER, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);

        // Stream registers both event types so the gate is what's
        // exercised — not the events_requested filter.
        createPushStream(Set.of(CaepSessionRevoked.TYPE, CaepCredentialChange.TYPE));
    }

    @AfterEach
    public void cleanup() {
        bestEffortDeleteStream();
        try {
            mockReceiverServer.removeContext(PUSH_CONTEXT_PATH);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void emitOnlyEvent_userLogout_skipsAutoEmit() throws Exception {
        // The receiver's ssf.emitOnlyEvents contains CaepSessionRevoked
        // (set via the realm config). A real LOGOUT must therefore not
        // produce any push — the listener drops the token before it can
        // reach the outbox. This is the ASM use case.
        triggerUserLogout();

        Assertions.assertNull(pushes.poll(NO_PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "LOGOUT must not auto-emit when the resolved event type is in ssf.emitOnlyEvents");
    }

    @Test
    public void emitOnlyEvent_syntheticEmit_stillDelivers() throws Exception {
        // Synthetic emit bypasses the emit-only gate — that's the
        // whole point of having both knobs. An admin caller uses the
        // canManage bypass to skip allowEmitEvents / role checks; the
        // gate we care about here is purely event-type based.
        String adminToken = adminClient.tokenManager().getAccessTokenString();

        try (SimpleHttpResponse res = emit(adminToken, "CaepSessionRevoked",
                Map.of("event_timestamp", 1700000000L))) {
            Assertions.assertEquals(200, res.getStatus(),
                    () -> "synthetic emit should succeed; got body: " + safeBody(res));
            Assertions.assertEquals("dispatched", res.asJson().get("status").asText(),
                    "emitOnlyEvents must not gate the synthetic-emit path");
        }

        String push = pushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNotNull(push,
                "synthetic-emit dispatched a CaepSessionRevoked SET — push must reach the mock receiver");

        JsonNode set = decodeSet(push);
        Assertions.assertTrue(set.path("events").has(CaepSessionRevoked.TYPE),
                "delivered SET should carry the synthetically-emitted event type");
    }

    @Test
    public void nonEmitOnlyEvent_credentialChange_stillAutoEmits() throws Exception {
        // Sanity check: the gate is event-type-specific. CaepSessionRevoked
        // is in the emit-only set, but CaepCredentialChange is NOT, so
        // a credential-update event must still flow through the listener
        // → mapper → dispatcher → push pipeline as usual.
        triggerCredentialEvent(EventType.UPDATE_CREDENTIAL, "password");

        String push = pushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNotNull(push,
                "credential-change is not in emitOnlyEvents — auto-emit must still fire");

        JsonNode set = decodeSet(push);
        Assertions.assertTrue(set.path("events").has(CaepCredentialChange.TYPE),
                "delivered SET should be the credential-change event, proving the gate is per-event-type");
    }

    // --- triggers --------------------------------------------------------

    /**
     * Fires a real Keycloak {@code LOGOUT} event by issuing a password
     * grant (creating a user session) and immediately logging out with
     * the refresh token. Mirrors the helper of the same name on
     * {@code SsfTransmitterPushDeliveryTests}.
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
     * Fires a credential-related Keycloak event from inside the server
     * JVM via {@link RunOnServerClient}. EventBuilder/EventStore are
     * server-side only, so this is the cleanest way to provoke one
     * without going through a real password change flow.
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

    // --- emit + setup ----------------------------------------------------

    protected SimpleHttpResponse emit(String token, String eventType, Map<String, Object> event) throws IOException {
        String url = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + RECEIVER + "/events/emit";
        return http.doPost(url)
                .auth(token)
                .json(Map.of(
                        "eventType", eventType,
                        "subjectType", "user-username",
                        "subjectValue", TEST_USER,
                        "event", event))
                .asResponse();
    }

    protected void createPushStream(Set<String> eventsRequested) throws IOException {
        StreamDeliveryConfig delivery = new StreamDeliveryConfig();
        delivery.setMethod(Ssf.DELIVERY_METHOD_PUSH_URI);
        delivery.setEndpointUrl(MOCK_PUSH_ENDPOINT);
        delivery.setAuthorizationHeader(PUSH_AUTH_HEADER);

        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setDelivery(delivery);
        streamConfig.setEventsRequested(eventsRequested);
        streamConfig.setDescription("Manual-only events integration test");

        String token = obtainReceiverManageToken();
        try (SimpleHttpResponse response = http.doPost(SsfTransmitterUrls.getStreamsEndpointUrl(realm.getBaseUrl()))
                .json(streamConfig)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(201, response.getStatus(),
                    () -> "stream creation should succeed in test setup; body=" + safeBody(response));
        }
    }

    protected String obtainReceiverManageToken() throws IOException {
        String tokenUrl = realm.getBaseUrl() + "/protocol/openid-connect/token";
        try (SimpleHttpResponse response = http.doPost(tokenUrl)
                .authBasic(RECEIVER, RECEIVER_SECRET)
                .param("grant_type", "client_credentials")
                .param("scope", SsfScopes.SCOPE_SSF_MANAGE + " " + SsfScopes.SCOPE_SSF_READ)
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus(),
                    "client_credentials grant should succeed for the receiver");
            return response.asJson().get("access_token").asText();
        }
    }

    // --- helpers ---------------------------------------------------------

    protected JsonNode decodeSet(String encoded) throws Exception {
        JWSInput jws = new JWSInput(encoded);
        return JsonSerialization.readValue(jws.getContent(), JsonNode.class);
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

    protected ClientRepresentation findClientByClientId(String clientId) {
        List<ClientRepresentation> clients = realm.admin().clients().findByClientId(clientId);
        if (clients.isEmpty()) {
            return null;
        }
        return clients.get(0);
    }

    protected void bestEffortDeleteStream() {
        ClientRepresentation client = findClientByClientId(RECEIVER);
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

    private String safeBody(SimpleHttpResponse response) {
        try {
            return response.asString();
        } catch (Exception e) {
            return "<no body: " + e.getMessage() + ">";
        }
    }

    // --- config ----------------------------------------------------------

    public static class EmitOnlyServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            config.features(Profile.Feature.SSF);
            config.log().categoryLevel("org.keycloak.ssf", "DEBUG");
            // Async pushes flow through the outbox — without a fast
            // drainer tick the auto-emit assertion below times out.
            config.spiOption("ssf-transmitter", "default",
                    DefaultSsfTransmitterProviderFactory.CONFIG_OUTBOX_DRAINER_INTERVAL, "500ms");
            // Verification rate limiter is unrelated; keep it lax so it
            // doesn't interfere if a transmitter-initiated verification
            // SET is fired during stream-create.
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

    public static class EmitOnlyRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-transmitter-emit-only");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");

            realm.eventsEnabled(true);
            realm.adminEventsEnabled(true);
            realm.eventsListeners("jboss-logging", "ssf-events");

            realm.users(
                    UserBuilder.create(TEST_USER)
                            .email(TEST_USER + "@local.test")
                            .firstName("Mona")
                            .lastName("OnlyTester")
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
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "http://127.0.0.1:8500/*")
                            // ALL keeps the subject filter out of the picture so
                            // the only suppression in play is emitOnlyEvents.
                            .attribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "ALL")
                            // The headline configuration: session-revoked is
                            // supported but never auto-emitted.
                            .attribute(ClientStreamStore.SSF_EMIT_ONLY_EVENTS_KEY, "CaepSessionRevoked")
                            .build()
            );

            return realm;
        }
    }
}
