package org.keycloak.tests.ssf.transmitter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.SsfEvent;
import org.keycloak.ssf.event.SsfEventProviderFactory;
import org.keycloak.ssf.event.caep.CaepCredentialChange;
import org.keycloak.ssf.transmitter.SsfScopes;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.admin.SsfConfigRepresentation;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.StreamDeliveryConfig;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.support.SsfTransmitterUrls;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.server.DefaultKeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.tests.providers.ssf.TestSsfEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that custom {@link SsfEvent}
 * implementations contributed by a third-party
 * {@link SsfEventProviderFactory} are
 * picked up by the SSF event registry and usable in stream configurations.
 *
 * <p>The test uses the {@code keycloak-tests-custom-providers} module, which
 * contains {@code TestSsfEventProviderFactory} contributing the custom
 * {@link TestSsfEvent} with a distinctive event type URI and alias. The
 * module is deployed into the embedded test server via
 * {@code config.dependency(...)} in
 * {@link CustomEventKeycloakServerConfig}.
 */
@KeycloakIntegrationTest(config = SsfTransmitterCustomEventTests.CustomEventKeycloakServerConfig.class)
public class SsfTransmitterCustomEventTests {

    static final String RECEIVER_ID = "ssf-receiver-custom-event";
    static final String RECEIVER_SECRET = "receiver-custom-event-secret";

    static final String DUMMY_PUSH_ENDPOINT = "http://127.0.0.1:65535/ssf/push";
    static final String DUMMY_PUSH_AUTH_HEADER = "Bearer dummy-custom-event-receiver";

    @InjectRealm(config = CustomEventRealm.class)
    ManagedRealm realm;

    @InjectSimpleHttp
    SimpleHttp http;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @BeforeEach
    public void setup() {
        assignOptionalClientScopes(RECEIVER_ID, SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
    }

    @AfterEach
    public void cleanup() {
        bestEffortDeleteStream(RECEIVER_ID);
    }

    @Test
    public void testCustomEventAliasIsAdvertisedInAdminConfig() throws IOException {

        String adminConfigUrl = keycloakUrls.getAdmin() + "/realms/" + realm.getName() + "/ssf/config";

        try (SimpleHttpResponse response = http.doGet(adminConfigUrl)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus());

            SsfConfigRepresentation config = response.asJson(SsfConfigRepresentation.class);
            Assertions.assertNotNull(config.getAvailableSupportedEvents());
            Assertions.assertTrue(
                    config.getAvailableSupportedEvents().contains(TestSsfEvent.class.getSimpleName()),
                    () -> "availableSupportedEvents should include the custom TestSsfEvent alias, got "
                            + config.getAvailableSupportedEvents());
            // Sanity: the built-in events should still be present — the custom
            // factory's contribution must be merged, not replace the default
            // registry.
            Assertions.assertTrue(
                    config.getAvailableSupportedEvents().contains(CaepCredentialChange.class.getSimpleName()),
                    "built-in CaepCredentialChange alias should still be advertised");

            // The custom event must also show up in the "default supported
            // events" set (the fallback used when a receiver client doesn't
            // configure its own ssf.supportedEvents attribute). When the
            // supported-events SPI property is unset, the provider returns
            // every emittable event in the registry, which must include the
            // SPI-contributed custom event.
            Assertions.assertNotNull(config.getDefaultSupportedEvents());
            Assertions.assertTrue(
                    config.getDefaultSupportedEvents().contains(TestSsfEvent.class.getSimpleName()),
                    () -> "defaultSupportedEvents should include the custom TestSsfEvent alias, got "
                            + config.getDefaultSupportedEvents());
            Assertions.assertTrue(
                    config.getDefaultSupportedEvents().contains(CaepCredentialChange.class.getSimpleName()),
                    "defaultSupportedEvents should still include the built-in CaepCredentialChange alias");
        }
    }

    @Test
    public void testStreamRoundTripsCustomEventUri() throws IOException {

        String token = obtainReceiverToken(RECEIVER_ID, RECEIVER_SECRET);

        StreamDeliveryConfig delivery = new StreamDeliveryConfig();
        delivery.setMethod(Ssf.DELIVERY_METHOD_PUSH_URI);
        delivery.setEndpointUrl(DUMMY_PUSH_ENDPOINT);
        delivery.setAuthorizationHeader(DUMMY_PUSH_AUTH_HEADER);

        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setDelivery(delivery);
        streamConfig.setEventsRequested(Set.of(TestSsfEvent.TYPE));
        streamConfig.setDescription("Custom event integration test");

        try (SimpleHttpResponse response = http.doPost(SsfTransmitterUrls.getStreamsEndpointUrl(realm.getBaseUrl()))
                .json(streamConfig)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(201, response.getStatus(),
                    "stream creation with a custom event type should succeed");

            StreamConfig created = response.asJson(StreamConfig.class);
            Assertions.assertNotNull(created.getEventsSupported());
            Assertions.assertTrue(
                    created.getEventsSupported().contains(TestSsfEvent.TYPE),
                    () -> "events_supported should contain the custom event URI, got "
                            + created.getEventsSupported());
            Assertions.assertTrue(
                    created.getEventsRequested().contains(TestSsfEvent.TYPE),
                    "events_requested should carry the custom event URI");
            Assertions.assertTrue(
                    created.getEventsDelivered().contains(TestSsfEvent.TYPE),
                    "events_delivered should include the custom event URI (it's in events_requested and the client has no restricting supportedEvents attribute)");
        }
    }

    // --- helpers ---------------------------------------------------------

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

    public static class CustomEventKeycloakServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            config.features(Profile.Feature.SSF);
            config.log().categoryLevel("org.keycloak.protocol.ssf", "DEBUG");
            // Deploy the custom-providers test module so that
            // TestSsfEventProviderFactory is discovered via META-INF/services
            // and contributes TestSsfEvent to the SSF event registry.
            config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
            // Test pushes to a local mock server on a loopback URL (http://127.0.0.1:NNNN/...).
            // Relax the http-scheme + private-host gate so the mock URL is accepted; the
            // per-client ssf.validPushUrls allow-list configured on each receiver below
            // is still the SSRF defence.
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_ALLOW_INSECURE_PUSH_TARGETS, "true");
            return configured;
        }
    }

    public static class CustomEventRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-transmitter-custom-event");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");

            realm.clients(
                    ClientBuilder.create(RECEIVER_ID)
                            .secret(RECEIVER_SECRET)
                            .serviceAccountsEnabled(true)
                            .directAccessGrantsEnabled(false)
                            .publicClient(false)
                            .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                            .attribute(ClientStreamStore.SSF_VALID_PUSH_URLS_KEY, "http://127.0.0.1:65535/*")
                            // Opt into the custom test event. Without an explicit
                            // ssf.supportedEvents attribute, the transmitter falls
                            // back to getDefaultSupportedEvents() which is a
                            // hardcoded set of built-in CAEP events and would
                            // exclude any custom SPI-contributed events.
                            .attribute(ClientStreamStore.SSF_STREAM_SUPPORTED_EVENTS_KEY,
                                    TestSsfEvent.class.getSimpleName())
                            .build()
            );

            return realm;
        }
    }
}
