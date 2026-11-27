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

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.risc.RiscAccountDisabled;
import org.keycloak.ssf.event.risc.RiscAccountPurged;
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
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testframework.util.HttpServerUtil;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Admin-triggered SSF events in a realm with fine-grained admin permissions (v2)
 * enabled, where the acting admin is restricted.
 *
 * <p>Events fired during an admin request run in that request's session, so the
 * listener's model queries are filtered by the acting admin's permissions unless
 * they opt out. Client queries are filtered for an admin holding a {@code query-*}
 * role but neither {@code view-clients} nor {@code manage-clients}: such an admin
 * cannot see the receiver client, so the receiver enumeration came back empty and
 * the event was discarded. The other transmitter tests did not catch this because
 * their realms have fine-grained admin permissions disabled, and their admin is not
 * restricted in this way.
 */
@KeycloakIntegrationTest(config = SsfTransmitterRestrictedAdminEventTests.RestrictedAdminServerConfig.class)
public class SsfTransmitterRestrictedAdminEventTests {

    static final String RECEIVER = "ssf-receiver-restricted-admin";
    static final String RECEIVER_SECRET = "receiver-restricted-admin-secret";

    static final String RESTRICTED_ADMIN = "restricted-admin";
    static final String RESTRICTED_ADMIN_CLIENT = "restricted-admin-client";

    static final String PUSH_CONTEXT_PATH = "/ssf/push-restricted-admin";
    static final String MOCK_PUSH_ENDPOINT = "http://127.0.0.1:8500" + PUSH_CONTEXT_PATH;
    static final String PUSH_AUTH_HEADER = "Bearer dummy-restricted-admin-receiver";

    static final long PUSH_WAIT_SECONDS = 5;

    @InjectRealm(config = RestrictedAdminRealm.class)
    ManagedRealm realm;

    @InjectAdminClient(ref = "restricted", mode = InjectAdminClient.Mode.MANAGED_REALM,
            client = RESTRICTED_ADMIN_CLIENT, user = RESTRICTED_ADMIN)
    Keycloak restrictedAdminClient;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectSimpleHttp
    SimpleHttp http;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectHttpServer
    HttpServer mockReceiverServer;

    private final BlockingQueue<String> pushes = new LinkedBlockingQueue<>();

    @BeforeEach
    public void setup() throws IOException {
        pushes.clear();

        mockReceiverServer.createContext(PUSH_CONTEXT_PATH, exchange -> {
            try (InputStream is = exchange.getRequestBody()) {
                pushes.add(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
            HttpServerUtil.sendResponse(exchange, 202, Map.of());
        });

        assignOptionalClientScopes(RECEIVER, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
        createPushStream(Set.of(RiscAccountPurged.TYPE, RiscAccountDisabled.TYPE));
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
    public void restrictedAdminDeletesUser_deliversAccountPurgedEvent() throws Exception {
        String userId = createUser("restricted-purge");
        assertRestrictedAdminCannotViewReceiver();

        try (Response response = restrictedAdminClient.realm(realm.getName()).users().delete(userId)) {
            Assertions.assertEquals(204, response.getStatus(),
                    "the restricted admin must be allowed to delete the user");
        }

        JsonNode set = decodeSet(awaitPush(
                "a user deletion by an admin who cannot view clients must still emit RiscAccountPurged"));
        Assertions.assertTrue(set.path("events").has(RiscAccountPurged.TYPE),
                "delivered SET should carry the account-purged event type");
        Assertions.assertEquals(userId, set.path("sub_id").path("sub").asText(),
                "sub_id.sub must identify the user that was purged");
    }

    @Test
    public void restrictedAdminDisablesUser_deliversAccountDisabledEvent() throws Exception {
        String userId = createUser("restricted-disable");
        assertRestrictedAdminCannotViewReceiver();

        UserResource user = restrictedAdminClient.realm(realm.getName()).users().get(userId);
        UserRepresentation rep = user.toRepresentation();
        rep.setEnabled(false);
        user.update(rep);

        JsonNode set = decodeSet(awaitPush(
                "a user disable by an admin who cannot view clients must still emit RiscAccountDisabled"));
        Assertions.assertTrue(set.path("events").has(RiscAccountDisabled.TYPE),
                "delivered SET should carry the account-disabled event type");
        Assertions.assertEquals(userId, set.path("sub_id").path("sub").asText(),
                "sub_id.sub must identify the user that was disabled");
    }

    /**
     * Precondition: the receiver client is outside what the restricted admin may
     * view, which is what makes fine-grained filtering hide it from client queries
     * in that admin's requests. Also pins that delivering the event does not grant
     * the admin any access to the receiver.
     */
    protected void assertRestrictedAdminCannotViewReceiver() throws IOException {
        String url = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + RECEIVER + "/stream";
        try (SimpleHttpResponse response = http.doGet(url)
                .auth(restrictedAdminClient.tokenManager().getAccessTokenString())
                .asResponse()) {
            Assertions.assertEquals(403, response.getStatus(),
                    () -> "the restricted admin must not be able to view the receiver; body=" + safeBody(response));
        }
    }

    // --- setup --------------------------------------------------------------

    protected String createUser(String username) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(username + "@local.test");
        user.setFirstName("Restricted");
        user.setLastName("Tester");
        user.setEnabled(true);

        try (Response response = realm.admin().users().create(user)) {
            Assertions.assertEquals(201, response.getStatus(),
                    () -> "user creation should succeed in test setup for " + username);
            return ApiUtil.getCreatedId(response);
        }
    }

    protected void createPushStream(Set<String> eventsRequested) throws IOException {
        StreamDeliveryConfig delivery = new StreamDeliveryConfig();
        delivery.setMethod(Ssf.DELIVERY_METHOD_PUSH_URI);
        delivery.setEndpointUrl(MOCK_PUSH_ENDPOINT);
        delivery.setAuthorizationHeader(PUSH_AUTH_HEADER);

        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setDelivery(delivery);
        streamConfig.setEventsRequested(eventsRequested);
        streamConfig.setDescription("Restricted admin event integration test");

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
        return clients.isEmpty() ? null : clients.get(0);
    }

    protected void bestEffortDeleteStream() {
        String adminStreamUrl = keycloakUrls.getAdmin() + "/realms/" + realm.getName()
                + "/ssf/clients/" + RECEIVER + "/stream";
        try (SimpleHttpResponse ignored = http.doDelete(adminStreamUrl)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .asResponse()) {
            // 204 / 404 both fine
        } catch (IOException e) {
            // best-effort
        }
    }

    // --- helpers ------------------------------------------------------------

    protected String awaitPush(String message) throws InterruptedException {
        String push = pushes.poll(PUSH_WAIT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNotNull(push, message);
        return push;
    }

    protected JsonNode decodeSet(String encoded) throws Exception {
        JWSInput jws = new JWSInput(encoded);
        return JsonSerialization.readValue(jws.getContent(), JsonNode.class);
    }

    private String safeBody(SimpleHttpResponse response) {
        try {
            return response.asString();
        } catch (Exception e) {
            return "<no body: " + e.getMessage() + ">";
        }
    }

    // --- config -------------------------------------------------------------

    public static class RestrictedAdminServerConfig extends DefaultKeycloakServerConfig {
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

    public static class RestrictedAdminRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-transmitter-restricted-admin");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");
            realm.adminPermissionsEnabled(true);

            realm.eventsEnabled(true);
            realm.adminEventsEnabled(true);
            realm.eventsListeners("jboss-logging", "ssf-events");

            realm.clients(
                    ClientBuilder.create(RECEIVER)
                            .secret(RECEIVER_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "http://127.0.0.1:8500/*")
                            .attribute(ClientStreamStore.SSF_DEFAULT_SUBJECTS_KEY, "ALL")
                            .build(),
                    ClientBuilder.create(RESTRICTED_ADMIN_CLIENT)
                            .secret("restricted-admin-secret")
                            .directAccessGrantsEnabled(true)
                            .build()
            );

            // A user administrator: may manage users, but holds no client role.
            // query-users is what activates fine-grained filtering of clients for
            // this admin; manage-users alone would leave client queries unfiltered.
            realm.users(UserBuilder.create(RESTRICTED_ADMIN)
                    .name("Restricted", "Admin")
                    .email("restricted-admin@local.test")
                    .emailVerified(true)
                    .password("password")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.QUERY_USERS,
                            AdminRoles.MANAGE_USERS));

            return realm;
        }
    }
}
