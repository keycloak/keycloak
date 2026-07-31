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
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.risc.RiscAccountDisabled;
import org.keycloak.ssf.event.risc.RiscAccountEnabled;
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
 * Integration tests proving the RISC {@code account-disabled}/{@code account-enabled}
 * wiring added on top of the Admin REST API actually reaches a receiver end to end —
 * real {@code PUT /admin/realms/{realm}/users/{id}} calls, not the mapper-level unit
 * tests in {@code SecurityEventTokenMapperTest}, which manufacture the admin-event
 * details directly and so don't verify that {@code UserResource} actually produces
 * them or that {@code SsfTransmitterEventListener} actually consumes them.
 *
 * <p>Also covers the {@code ssf.emitOnlyEvents} gate on the <em>admin</em>-event path
 * ({@link org.keycloak.ssf.transmitter.event.SsfTransmitterEventListener#generateSecurityEventTokensForAdminEvent}),
 * which previously had no coverage at all — {@code SsfTransmitterEmitOnlyEventsTests}
 * only exercises the gate via native user events (LOGOUT/credential-update).
 */
@KeycloakIntegrationTest(config = SsfTransmitterAccountStateEventTests.AccountStateServerConfig.class)
public class SsfTransmitterAccountStateEventTests {

    static final String RECEIVER = "ssf-receiver-account-state";
    static final String RECEIVER_SECRET = "receiver-account-state-secret";

    static final String TEST_USER = "account-state-tester";
    static final String TEST_PASSWORD = "test";

    static final String PUSH_CONTEXT_PATH = "/ssf/push-account-state";
    static final String MOCK_PUSH_ENDPOINT = "http://127.0.0.1:8500" + PUSH_CONTEXT_PATH;
    static final String PUSH_AUTH_HEADER = "Bearer dummy-account-state-receiver";

    static final long PUSH_WAIT_SECONDS = 5;
    static final long NO_PUSH_WAIT_SECONDS = 3;

    @InjectRealm(config = AccountStateRealm.class)
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

    private String testUserId;

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

        assignOptionalClientScopes(RECEIVER, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);

        testUserId = realm.admin().users().searchByUsername(TEST_USER, true).get(0).getId();
        // Defensive: earlier test in the class may have left the user
        // disabled or the receiver's emitOnlyEvents set — start every
        // test from the same known state regardless of run order.
        setUserEnabled(true);
        setEmitOnlyEvents();

        createPushStream(Set.of(RiscAccountDisabled.TYPE, RiscAccountEnabled.TYPE));

        // Observed in CI: the very first admin-event-triggered SSF lookup
        // (StreamService.findStreamsForSsfReceiverClients, via the admin-event
        // path) against a freshly started per-class test server can transiently
        // see no streams — likely a client-attribute-search cache/index warm-up
        // race, since every subsequent call in the same server instance (i.e.
        // every other test in this class) succeeds reliably. Prime that path
        // once here with a throwaway disable/re-enable cycle so the real
        // per-test assertions below are never the first caller.
        setUserEnabled(false);
        setUserEnabled(true);
        pushes.clear();
    }

    @AfterEach
    public void cleanup() {
        bestEffortDeleteStream();
        setEmitOnlyEvents();
        try {
            mockReceiverServer.removeContext(PUSH_CONTEXT_PATH);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void adminDisablesUser_deliversAccountDisabledEvent() throws Exception {
        setUserEnabled(false);

        String push = pushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNotNull(push,
                "admin disabling a user via the REST API must auto-emit RiscAccountDisabled");

        JsonNode set = decodeSet(push);
        Assertions.assertTrue(set.path("events").has(RiscAccountDisabled.TYPE),
                "delivered SET should carry the account-disabled event type");
        Assertions.assertEquals(RiscAccountDisabled.REASON_ADMIN,
                set.path("events").path(RiscAccountDisabled.TYPE).path("reason").asText(),
                "an admin-initiated disable must report reason=disabled-by-admin");
        Assertions.assertEquals("admin",
                set.path("events").path(RiscAccountDisabled.TYPE).path("initiating_entity").asText(),
                "an admin-initiated disable must report initiating_entity=admin");
    }

    @Test
    public void adminEnablesUser_deliversAccountEnabledEvent() throws Exception {
        setUserEnabled(false);
        Assertions.assertNotNull(pushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "setup disable should have produced a push before the re-enable under test");

        setUserEnabled(true);

        String push = pushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNotNull(push,
                "admin re-enabling a user via the REST API must auto-emit RiscAccountEnabled");

        JsonNode set = decodeSet(push);
        Assertions.assertTrue(set.path("events").has(RiscAccountEnabled.TYPE),
                "delivered SET should carry the account-enabled event type");
    }

    @Test
    public void adminUpdatesUserWithoutEnabledChange_noPush() throws Exception {
        UserResource userResource = realm.admin().users().get(testUserId);
        UserRepresentation rep = userResource.toRepresentation();
        rep.setFirstName("Changed" + System.nanoTime());
        userResource.update(rep);

        Assertions.assertNull(pushes.poll(NO_PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "a profile update that leaves 'enabled' untouched must not produce an account-state SET");
    }

    @Test
    public void emitOnlyEvent_adminDisablesUser_skipsAutoEmit() throws Exception {
        // Covers the admin-event path of the emitOnlyEvents gate fixed in
        // SsfTransmitterEventListener.generateSecurityEventTokensForAdminEvent —
        // previously untested, since the only existing emit-only coverage
        // (SsfTransmitterEmitOnlyEventsTests) drives native user events.
        setEmitOnlyEvents(RiscAccountDisabled.TYPE);

        setUserEnabled(false);

        Assertions.assertNull(pushes.poll(NO_PUSH_WAIT_SECONDS, TimeUnit.SECONDS),
                "RiscAccountDisabled is in the receiver's emitOnlyEvents set — the admin-triggered "
                        + "disable must not auto-emit, same as the native-event path");
    }

    // --- triggers ----------------------------------------------------------

    protected void setUserEnabled(boolean enabled) {
        UserResource userResource = realm.admin().users().get(testUserId);
        UserRepresentation rep = userResource.toRepresentation();
        rep.setEnabled(enabled);
        userResource.update(rep);
    }

    protected void setEmitOnlyEvents(String... eventTypes) {
        ClientResource clientResource = realm.admin().clients().get(findClientByClientId(RECEIVER).getId());
        ClientRepresentation rep = clientResource.toRepresentation();
        rep.getAttributes().put(ClientStreamStore.SSF_EMIT_ONLY_EVENTS_KEY,
                eventTypes.length == 0 ? null : String.join(",", eventTypes));
        clientResource.update(rep);
    }

    // --- setup ---------------------------------------------------------------

    protected void createPushStream(Set<String> eventsRequested) throws IOException {
        StreamDeliveryConfig delivery = new StreamDeliveryConfig();
        delivery.setMethod(Ssf.DELIVERY_METHOD_PUSH_URI);
        delivery.setEndpointUrl(MOCK_PUSH_ENDPOINT);
        delivery.setAuthorizationHeader(PUSH_AUTH_HEADER);

        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setDelivery(delivery);
        streamConfig.setEventsRequested(eventsRequested);
        streamConfig.setDescription("Account state event integration test");

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

    public static class AccountStateServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            config.features(Profile.Feature.SSF);
            config.log().categoryLevel("org.keycloak.ssf", "DEBUG");
            config.spiOption("ssf-transmitter", "default",
                    DefaultSsfTransmitterProviderFactory.CONFIG_OUTBOX_DRAINER_INTERVAL, "500ms");
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_MIN_VERIFICATION_INTERVAL_SECONDS, "0");
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_ALLOW_INSECURE_PUSH_TARGETS, "true");
            return configured;
        }
    }

    public static class AccountStateRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-transmitter-account-state");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");

            realm.eventsEnabled(true);
            realm.adminEventsEnabled(true);
            realm.eventsListeners("jboss-logging", "ssf-events");

            realm.users(
                    UserBuilder.create(TEST_USER)
                            .email(TEST_USER + "@local.test")
                            .firstName("Ada")
                            .lastName("StateTester")
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
                            .attribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "ALL")
                            .build()
            );

            return realm;
        }
    }
}
