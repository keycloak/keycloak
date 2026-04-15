package org.keycloak.tests.ssf;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.caep.CaepCredentialChange;
import org.keycloak.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.ssf.transmitter.SsfScopes;
import org.keycloak.ssf.transmitter.SsfTransmitterUrls;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.StreamDeliveryConfig;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
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

        StreamConfig request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));

        try (SimpleHttpResponse response = postStream(readOnlyToken, request)) {
            Assertions.assertEquals(401, response.getStatus(),
                    "creating a stream without the ssf.manage scope should be rejected with 401");
        }
    }

    @Test
    public void testCreateStreamReturnsFullEventTypeUris() throws IOException {

        String token = obtainManageToken(RECEIVER_RW, RECEIVER_RW_SECRET);

        StreamConfig request = buildPushStreamRequest(Set.of(
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
        StreamConfig request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));

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
        StreamConfig request = buildPushStreamRequest(Set.of(CaepSessionRevoked.TYPE));

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
        StreamConfig request = buildPushStreamRequest(Set.of(
                CaepCredentialChange.TYPE,
                CaepSessionRevoked.TYPE));

        String streamId;
        try (SimpleHttpResponse response = postStream(token, request)) {
            Assertions.assertEquals(201, response.getStatus());
            streamId = response.asJson(StreamConfig.class).getStreamId();
        }

        StreamConfig patch = buildPushStreamRequest(Set.of(CaepSessionRevoked.TYPE));
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
    public void testCreateStreamHonoursClientSupportedEvents() throws IOException {

        // RECEIVER_SCOPED is configured with ssf.supportedEvents=CaepSessionRevoked
        // so requesting additional events should still yield a delivered set
        // narrowed to the client-configured allow list.
        String token = obtainManageToken(RECEIVER_SCOPED, RECEIVER_SCOPED_SECRET);
        StreamConfig request = buildPushStreamRequest(Set.of(
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
        StreamConfig request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));

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
        StreamConfig request = buildPushStreamRequest(Set.of(CaepCredentialChange.TYPE));

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

    protected StreamConfig buildPushStreamRequest(Set<String> eventsRequested) {
        StreamDeliveryConfig delivery = new StreamDeliveryConfig();
        delivery.setMethod(Ssf.DELIVERY_METHOD_PUSH_URI);
        delivery.setEndpointUrl(DUMMY_PUSH_ENDPOINT);
        delivery.setAuthorizationHeader(DUMMY_PUSH_AUTH_HEADER);

        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setDelivery(delivery);
        streamConfig.setEventsRequested(eventsRequested);
        streamConfig.setDescription("Stream management integration test");
        return streamConfig;
    }

    protected SimpleHttpResponse postStream(String accessToken, StreamConfig streamConfig) throws IOException {
        return http.doPost(streamsEndpoint())
                .json(streamConfig)
                .auth(accessToken)
                .acceptJson()
                .asResponse();
    }

    protected SimpleHttpResponse patchStream(String accessToken, StreamConfig streamConfig) throws IOException {
        return http.doPatch(streamsEndpoint())
                .json(streamConfig)
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
