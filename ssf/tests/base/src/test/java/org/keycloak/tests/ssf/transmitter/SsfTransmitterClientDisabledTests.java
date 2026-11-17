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
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.caep.CaepCredentialChange;
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
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.DefaultKeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.HttpServerUtil;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the per-client SSF disable gate
 * (keycloak/keycloak#50050).
 *
 * <p>Disabling an SSF receiver client (the standard client on/off toggle)
 * must take it off the air for event delivery — mirroring the realm-level
 * transmitter disable — while leaving the stream configuration intact so
 * re-enabling the client resumes delivery. The gate is implemented in
 * {@link org.keycloak.ssf.transmitter.support.SsfUtil#isReceiverEnabled}
 * and applied across the dispatch path, the synthetic-emit path, the
 * receiver auth pipeline, and auto-notify-on-login.
 *
 * <p>These tests exercise the behaviour end-to-end through the synthetic
 * emit endpoint and the async push pipeline, which is the path that gives
 * a clean, observable signal: a configured receiver delivers while
 * enabled, returns {@code receiver_disabled} (HTTP 400) with no push while
 * disabled, and delivers again once re-enabled — proving the stream
 * survived the disable.
 *
 * <p>The admin-caller path is used for emit (the {@code @InjectAdminClient}
 * token has manage-clients), which bypasses the receiver's
 * {@code allowEmitEvents}/role wiring and keeps the setup focused on the
 * disable gate itself.
 */
@KeycloakIntegrationTest(config = SsfTransmitterClientDisabledTests.DisabledServerConfig.class)
public class SsfTransmitterClientDisabledTests {

    static final String RECEIVER = "ssf-receiver-disabled";
    static final String RECEIVER_SECRET = "receiver-disabled-secret";

    static final String TEST_USER = "disabled-tester";
    static final String TEST_EMAIL = "disabled-tester@local.test";

    static final String PUSH_CONTEXT_PATH = "/ssf/push-disabled";
    static final String MOCK_PUSH_ENDPOINT = "http://127.0.0.1:8500" + PUSH_CONTEXT_PATH;
    static final String PUSH_AUTH_HEADER = "Bearer dummy-disabled-receiver";

    static final long PUSH_WAIT_SECONDS = 5;

    @InjectRealm(config = DisabledRealm.class)
    ManagedRealm realm;

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

        // A previous test may have left the receiver disabled; restore a
        // known-enabled state before (re)creating the stream, which needs
        // a receiver token only an enabled client can obtain.
        setReceiverEnabled(true);

        assignOptionalClientScopes(RECEIVER, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
        createPushStream();
        subscribeTestUser();
    }

    @AfterEach
    public void cleanup() {
        // Re-enable first so the stream-delete (receiver-context) call and
        // the next test's setup both run against an enabled client.
        try {
            setReceiverEnabled(true);
        } catch (Exception ignored) {
        }
        bestEffortDeleteStream();
        bestEffortRemoveNotify();
        try {
            mockReceiverServer.removeContext(PUSH_CONTEXT_PATH);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void enabledReceiver_emitDispatchesAndPushes() throws Exception {
        // Baseline: with the receiver enabled the emit endpoint dispatches
        // and the SET reaches the mock receiver. Establishes that the rest
        // of the wiring is sound, so the disabled-case assertions below
        // can attribute the difference to the disable gate alone.
        try (SimpleHttpResponse res = emitAsAdmin("CaepCredentialChange", TEST_EMAIL,
                Map.of("credential_type", "password", "change_type", "update"))) {
            Assertions.assertEquals(200, res.getStatus(),
                    "emit should succeed while the receiver client is enabled");
            Assertions.assertEquals("dispatched", res.asJson().get("status").asText(),
                    "status should be 'dispatched' when the receiver is enabled and all filters pass");
        }
        Assertions.assertNotNull(pushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "dispatched event should reach the mock receiver while enabled");
    }

    @Test
    public void disabledReceiver_emitReturnsReceiverDisabled_andNoPush() throws Exception {
        setReceiverEnabled(false);

        try (SimpleHttpResponse res = emitAsAdmin("CaepCredentialChange", TEST_EMAIL,
                Map.of("credential_type", "password", "change_type", "update"))) {
            Assertions.assertEquals(400, res.getStatus(),
                    "emit against a disabled receiver client must not report success");
            Assertions.assertEquals("receiver_disabled", res.asJson().get("error").asText(),
                    "error code should name the disabled receiver so the caller can act on it");
        }

        Assertions.assertNull(pushes.poll(2, TimeUnit.SECONDS),
                "a disabled receiver must not receive any push");
    }

    @Test
    public void reEnablingReceiver_resumesDelivery() throws Exception {
        // Disable → emit is gated → re-enable → emit delivers again. The
        // stream is never re-created here, so a successful push after
        // re-enable proves the disable did not discard the stream config.
        setReceiverEnabled(false);
        try (SimpleHttpResponse res = emitAsAdmin("CaepCredentialChange", TEST_EMAIL,
                Map.of("credential_type", "password", "change_type", "update"))) {
            Assertions.assertEquals(400, res.getStatus(), "disabled receiver should be gated");
            Assertions.assertEquals("receiver_disabled", res.asJson().get("error").asText());
        }
        Assertions.assertNull(pushes.poll(1, TimeUnit.SECONDS), "no push while disabled");

        setReceiverEnabled(true);
        try (SimpleHttpResponse res = emitAsAdmin("CaepCredentialChange", TEST_EMAIL,
                Map.of("credential_type", "password", "change_type", "update"))) {
            Assertions.assertEquals(200, res.getStatus(),
                    "re-enabling the client should resume SSF event delivery");
            Assertions.assertEquals("dispatched", res.asJson().get("status").asText(),
                    "delivery should resume against the surviving stream once the client is enabled again");
        }
        Assertions.assertNotNull(pushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "event should reach the mock receiver again after re-enable — the stream survived the disable");
    }

    @Test
    public void disabledReceiver_adminCanStillDeleteStream() throws Exception {
        // Regression guard: the disable gate must NOT leak into admin-side
        // stream management. Disabling the client and then deleting its
        // stream as an admin must actually remove the stream (and cascade
        // the outbox purge), not silently no-op behind a 204. The gate
        // belongs on the delivery paths, not on the management lookup
        // getStream() relies on.
        setReceiverEnabled(false);

        String deleteUrl = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + RECEIVER + "/stream";
        try (SimpleHttpResponse res = http.doDelete(deleteUrl)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .asResponse()) {
            Assertions.assertEquals(204, res.getStatus(),
                    "admin stream delete should succeed even when the receiver client is disabled");
        }

        // The stream must really be gone — admin GET reads the stream via
        // the ungated getStreamForClient, so a 404 here proves the delete
        // took effect rather than no-opping behind an idempotent 204.
        String getUrl = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + RECEIVER + "/stream";
        try (SimpleHttpResponse res = http.doGet(getUrl)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .asResponse()) {
            Assertions.assertEquals(404, res.getStatus(),
                    "stream must actually be deleted for a disabled client, not left behind");
        }
    }

    // --- helpers ---------------------------------------------------------

    protected String emitEndpointUrl() {
        return keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + RECEIVER + "/events/emit";
    }

    protected SimpleHttpResponse emitAsAdmin(String eventType, String userEmail,
                                             Map<String, Object> event) throws IOException {
        // Admin token has manage-clients on the receiver, so the emit auth
        // pipeline takes the admin fast-path and we reach the dispatch
        // filters (including the disable gate) without the role wiring.
        return http.doPost(emitEndpointUrl())
                .auth(adminClient.tokenManager().getAccessTokenString())
                .json(Map.of(
                        "eventType", eventType,
                        "sub_id", Map.of("format", "email", "email", userEmail),
                        "event", event))
                .asResponse();
    }

    protected void setReceiverEnabled(boolean enabled) {
        ClientResource clientResource = realm.admin().clients().get(findClientByClientId(RECEIVER).getId());
        ClientRepresentation rep = clientResource.toRepresentation();
        rep.setEnabled(enabled);
        clientResource.update(rep);
    }

    protected void createPushStream() throws IOException {
        StreamDeliveryConfig delivery = new StreamDeliveryConfig();
        delivery.setMethod(Ssf.DELIVERY_METHOD_PUSH_URI);
        delivery.setEndpointUrl(MOCK_PUSH_ENDPOINT);
        delivery.setAuthorizationHeader(PUSH_AUTH_HEADER);

        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setDelivery(delivery);
        streamConfig.setEventsRequested(Set.of(CaepCredentialChange.TYPE));
        streamConfig.setDescription("Client-disabled gate integration test");

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

    protected void subscribeTestUser() throws IOException {
        String url = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + RECEIVER + "/subjects/add";
        try (SimpleHttpResponse ignored = http.doPost(url)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .json(Map.of("type", "user-email", "value", TEST_EMAIL))
                .asResponse()) {
        }
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

    // --- config ----------------------------------------------------------

    public static class DisabledServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            config.features(Profile.Feature.SSF);
            config.log().categoryLevel("org.keycloak.ssf", "DEBUG");
            // Async pushes flow through the outbox — without a fast drainer
            // tick the happy-path push assertions time out.
            config.spiOption("ssf-transmitter", "default",
                    DefaultSsfTransmitterProviderFactory.CONFIG_OUTBOX_DRAINER_INTERVAL, "500ms");
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_MIN_VERIFICATION_INTERVAL_SECONDS, "0");
            // Mock receiver lives on a loopback URL; relax the http-scheme +
            // private-host SSRF gate. The per-client ssf.validPushUrls
            // allow-list below remains the SSRF defence.
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_ALLOW_INSECURE_PUSH_TARGETS, "true");
            return configured;
        }
    }

    public static class DisabledRealm implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-transmitter-client-disabled");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");

            realm.eventsEnabled(true);
            realm.adminEventsEnabled(true);
            realm.eventsListeners("jboss-logging", "ssf-events");

            realm.users(
                    UserBuilder.create(TEST_USER)
                            .email(TEST_EMAIL)
                            .firstName("Disabled")
                            .lastName("Tester")
                            .enabled(true)
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
                            .attribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "NONE")
                            .build()
            );

            return realm;
        }
    }
}
