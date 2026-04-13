package org.keycloak.tests.ssf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.protocol.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.protocol.ssf.transmitter.SsfScopes;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.protocol.ssf.transmitter.stream.StreamConfig;
import org.keycloak.protocol.ssf.transmitter.stream.StreamDeliveryConfig;
import org.keycloak.protocol.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
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
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end interop test that exercises both SSF sides of Keycloak in a
 * single server, wired across two realms:
 *
 * <ul>
 *     <li>{@link #TX_REALM_NAME} acts as the SSF Transmitter. A receiver
 *         service-account client is registered with the SSF scopes and an
 *         {@code ssf.streamAudience} attribute so the emitted SETs carry a
 *         stable audience claim.</li>
 *     <li>{@link #RX_REALM_NAME} acts as the SSF Receiver. It registers an
 *         {@code ssf-receiver} identity provider pointing at the transmitter
 *         realm's issuer and reuses that realm's JWKS for signature
 *         verification. The default {@code expectedSignatureAlgorithms}
 *         ({@code RS256}) matches the transmitter's default, so no per-realm
 *         override is needed — which is exactly the CAEP interop profile
 *         §2.6 story we want to validate.</li>
 * </ul>
 *
 * <p>Rather than make the transmitter push directly into the receiver realm
 * (async, hard to observe), the stream is pointed at an in-process MITM
 * HTTP server that captures the signed SET body. The test then forwards the
 * captured body to the receiver realm's {@code /ssf/receivers/{alias}/push}
 * endpoint and asserts a {@code 202}. That separates the two ends cleanly:
 *
 * <ul>
 *     <li>Capture proves the transmitter realm produced a real,
 *         RS256-signed SET via the full event listener → mapper → dispatcher
 *         → encoder pipeline, with the expected {@code iss}/{@code aud}
 *         claims and header algorithm.</li>
 *     <li>{@code 202} from the receiver realm proves it fetched the
 *         transmitter realm's well-known metadata + JWKS, verified the
 *         signature under the allow-list, validated {@code iss}/{@code aud},
 *         and handed the SET to the receiver event listener without
 *         error.</li>
 * </ul>
 */
@KeycloakIntegrationTest(config = SsfTransmitterReceiverInteropTests.InteropKeycloakServerConfig.class)
public class SsfTransmitterReceiverInteropTests {

    static final String TX_REALM_NAME = "ssf-tx-interop";
    static final String RX_REALM_NAME = "ssf-rx-interop";

    static final String RX_IDP_ALIAS = "tx-realm-transmitter";

    static final String RECEIVER_CLIENT_ID = "ssf-interop-receiver";
    static final String RECEIVER_CLIENT_SECRET = "ssf-interop-receiver-secret";

    static final String TEST_USER = "tester";
    static final String TEST_PASSWORD = "test";
    static final String TEST_EMAIL = "tester@local.test";

    /**
     * Shared aud claim the transmitter stamps on every SET (via the
     * {@code ssf.streamAudience} receiver-client attribute) and the
     * receiver realm expects (via its {@code streamAudience} IdP config).
     */
    static final String SHARED_STREAM_AUDIENCE = "https://keycloak-interop-stream";

    /**
     * Bare bearer token stored in the receiver realm's
     * {@code pushAuthorizationHeader} IdP attribute. {@code
     * SsfReceiversResource#isValidPushAuthorizationHeader} strips the
     * leading {@code "Bearer "} from the received header before
     * comparing, so the config value must be the raw token (no prefix)
     * while the wire value carries the prefix.
     */
    static final String PUSH_AUTH_TOKEN = "ssf-interop-push-auth";

    /**
     * Full {@code Authorization} header value the transmitter stamps on
     * stream deliveries via {@link StreamDeliveryConfig#setAuthorizationHeader},
     * and that the test forwards verbatim to the receiver realm.
     */
    static final String PUSH_AUTHORIZATION_HEADER = "Bearer " + PUSH_AUTH_TOKEN;

    static final String MITM_CONTEXT_PATH = "/ssf/interop-mitm";
    static final String MITM_PUSH_ENDPOINT = "http://127.0.0.1:8500" + MITM_CONTEXT_PATH;

    static final long CAPTURE_WAIT_SECONDS = 5;

    @InjectRealm(ref = "tx", config = TransmitterRealm.class)
    ManagedRealm txRealm;

    @InjectRealm(ref = "rx", config = ReceiverRealm.class)
    ManagedRealm rxRealm;

    @InjectOAuthClient(ref = "tx", realmRef = "tx")
    OAuthClient txOauth;

    @InjectSimpleHttp
    SimpleHttp http;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectHttpServer
    HttpServer mitmServer;

    private final BlockingQueue<CapturedPush> pushes = new LinkedBlockingQueue<>();

    @BeforeEach
    public void setup() {
        pushes.clear();

        mitmServer.createContext(MITM_CONTEXT_PATH, new HttpHandler() {
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

        // The SSF scopes are created by RealmPostCreateEvent after client
        // import, so declaring them as optionalClientScopes on the realm
        // config is silently dropped. Reassign them here on the transmitter
        // realm's receiver client so the CC grant can actually request them.
        assignOptionalClientScopes(txRealm, RECEIVER_CLIENT_ID,
                SsfScopes.SCOPE_SSF_READ, SsfScopes.SCOPE_SSF_MANAGE);
    }

    @AfterEach
    public void cleanup() {
        bestEffortDeleteTxStream();
        try {
            mitmServer.removeContext(MITM_CONTEXT_PATH);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testTransmitterRealmSetIsAcceptedByReceiverRealm() throws Exception {

        String txToken = obtainTxReceiverToken();
        StreamConfig stream = createTxStream(txToken, Set.of(CaepSessionRevoked.TYPE));

        Assertions.assertEquals(Set.of(SHARED_STREAM_AUDIENCE), stream.getAudience(),
                "stream audience should be sourced from the ssf.streamAudience client attribute");

        triggerTxUserLogout();

        CapturedPush captured = awaitPush();

        // The MITM capture proves the transmitter realm produced a real,
        // RS256-signed SET via the full pipeline. Sanity-check the push
        // headers and the SET envelope before handing it to the receiver
        // realm.
        Assertions.assertEquals(PUSH_AUTHORIZATION_HEADER, captured.authorizationHeader,
                "push should carry the configured receiver authorization header verbatim");
        Assertions.assertEquals(Ssf.APPLICATION_SECEVENT_JWT_TYPE, captured.contentType,
                "push content-type should be application/secevent+jwt");

        JWSInput jws = new JWSInput(captured.body);
        JWSHeader header = jws.getHeader();
        Assertions.assertEquals("RS256", header.getRawAlgorithm(),
                "transmitter should sign with RS256 by default per CAEP interop profile §2.6");
        Assertions.assertEquals(Ssf.SECEVENT_JWT_TYPE, header.getType(),
                "JWS typ should be secevent+jwt");

        JsonNode set = JsonSerialization.readValue(jws.getContent(), JsonNode.class);
        Assertions.assertEquals(txRealm.getBaseUrl(), set.get("iss").asText(),
                "SET iss should match the transmitter realm's base URL");
        Assertions.assertEquals(SHARED_STREAM_AUDIENCE, set.get("aud").asText(),
                "SET aud should match the shared stream audience");
        Assertions.assertTrue(set.path("events").has(CaepSessionRevoked.TYPE),
                "SET should carry the CAEP session-revoked event");

        // Hand the captured SET to the receiver realm's push endpoint. That
        // is the slice we actually want to cover: receiver realm fetches
        // the transmitter realm's well-known metadata and JWKS (both
        // reachable internally because both realms live in the same
        // Keycloak process), verifies the RS256 signature against the
        // expected allow-list, and runs iss/aud validation. A clean 202
        // means every step succeeded.
        String rxPushUrl = rxRealm.getBaseUrl() + "/ssf/receivers/" + RX_IDP_ALIAS + "/push/";
        int rxStatus;
        String rxBody;
        try (SimpleHttpResponse rxResponse = http.doPost(rxPushUrl)
                .header(HttpHeaders.CONTENT_TYPE, Ssf.APPLICATION_SECEVENT_JWT_TYPE)
                .header(HttpHeaders.AUTHORIZATION, PUSH_AUTHORIZATION_HEADER)
                .entity(new StringEntity(captured.body))
                .asResponse()) {
            rxStatus = rxResponse.getStatus();
            rxBody = rxStatus == 202 ? "" : rxResponse.asString();
        }
        Assertions.assertEquals(202, rxStatus,
                "receiver realm should accept the SET signed by the transmitter realm; got status "
                        + rxStatus + " body=" + rxBody);
    }

    // --- helpers ---------------------------------------------------------

    /**
     * Performs a password grant for the transmitter realm's test user and
     * then calls the OIDC logout endpoint with the resulting refresh
     * token. That fires a {@code LOGOUT} user event which the SSF event
     * listener maps to a {@link CaepSessionRevoked} SET for every enabled
     * stream on the transmitter realm.
     */
    protected void triggerTxUserLogout() {
        AccessTokenResponse tokenResponse = txOauth
                .passwordGrantRequest(TEST_USER, TEST_PASSWORD)
                .send();
        Assertions.assertNotNull(tokenResponse.getAccessToken(),
                "password grant should succeed for the test user");
        Assertions.assertNotNull(tokenResponse.getRefreshToken(),
                "password grant response should include a refresh token");
        txOauth.doLogout(tokenResponse.getRefreshToken());
    }

    protected CapturedPush awaitPush() throws InterruptedException {
        CapturedPush captured = pushes.poll(CAPTURE_WAIT_SECONDS, TimeUnit.SECONDS);
        Assertions.assertNotNull(captured,
                () -> "expected a push within " + CAPTURE_WAIT_SECONDS + "s but the MITM saw nothing");
        return captured;
    }

    protected StreamConfig createTxStream(String token, Set<String> eventsRequested) throws IOException {
        StreamDeliveryConfig delivery = new StreamDeliveryConfig();
        delivery.setMethod(Ssf.DELIVERY_METHOD_PUSH_URI);
        delivery.setEndpointUrl(MITM_PUSH_ENDPOINT);
        delivery.setAuthorizationHeader(PUSH_AUTHORIZATION_HEADER);

        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setDelivery(delivery);
        streamConfig.setEventsRequested(eventsRequested);
        streamConfig.setDescription("Transmitter/Receiver interop test stream");

        try (SimpleHttpResponse response = http.doPost(Ssf.streamsEndpoint(txRealm.getBaseUrl()))
                .json(streamConfig)
                .auth(token)
                .acceptJson()
                .asResponse()) {
            Assertions.assertEquals(201, response.getStatus(), "stream creation should succeed");
            return response.asJson(StreamConfig.class);
        }
    }

    protected String obtainTxReceiverToken() throws IOException {
        String tokenUrl = txRealm.getBaseUrl() + "/protocol/openid-connect/token";
        try (SimpleHttpResponse response = http.doPost(tokenUrl)
                .authBasic(RECEIVER_CLIENT_ID, RECEIVER_CLIENT_SECRET)
                .param("grant_type", "client_credentials")
                .param("scope", SsfScopes.SCOPE_SSF_MANAGE + " " + SsfScopes.SCOPE_SSF_READ)
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus(),
                    () -> "CC grant for '" + RECEIVER_CLIENT_ID + "' should succeed");
            return response.asJson().get("access_token").asText();
        }
    }

    protected void assignOptionalClientScopes(ManagedRealm realm, String clientId, String... scopeNames) {
        List<ClientRepresentation> matching = realm.admin().clients().findByClientId(clientId);
        Assertions.assertFalse(matching.isEmpty(), () -> "expected client '" + clientId + "' to exist");
        ClientResource clientResource = realm.admin().clients().get(matching.get(0).getId());

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
            Assertions.assertNotNull(scope, () -> "expected realm scope '" + scopeName + "' to exist");
            clientResource.addOptionalClientScope(scope.getId());
        }
    }

    protected void bestEffortDeleteTxStream() {
        List<ClientRepresentation> matching = txRealm.admin().clients().findByClientId(RECEIVER_CLIENT_ID);
        if (matching.isEmpty()) {
            return;
        }
        String adminStreamUrl = keycloakUrls.getAdmin() + "/realms/" + txRealm.getName()
                + "/ssf/clients/" + matching.get(0).getId() + "/stream";
        try (SimpleHttpResponse ignored = http.doDelete(adminStreamUrl)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .asResponse()) {
            // 204 / 404 both fine
        } catch (IOException ignored) {
        }
    }

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

    public static class InteropKeycloakServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            config.features(Profile.Feature.SSF);
            config.log().categoryLevel("org.keycloak.protocol.ssf", "DEBUG");
            // This test doesn't exercise /verify; relax the rate limiter so
            // transmitter-initiated verification on stream create doesn't
            // interact with subsequent runs.
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_MIN_VERIFICATION_INTERVAL_SECONDS, "0");
            return configured;
        }
    }

    public static class TransmitterRealm implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.name(TX_REALM_NAME);

            realm.eventsEnabled(true);
            realm.adminEventsEnabled(true);
            realm.eventsListeners("jboss-logging", "ssf-events");

            UserConfigBuilder tester = realm.addUser(TEST_USER);
            tester.email(TEST_EMAIL);
            tester.firstName("Theo");
            tester.lastName("Tester");
            tester.enabled(true);
            tester.password(TEST_PASSWORD);

            realm.addClient(RECEIVER_CLIENT_ID)
                    .secret(RECEIVER_CLIENT_SECRET)
                    .serviceAccountsEnabled(true)
                    .directAccessGrantsEnabled(false)
                    .publicClient(false)
                    .attribute(ClientStreamStore.SSF_ENABLED_KEY, "true")
                    // Pin the aud claim on every emitted SET so the
                    // receiver realm can match it exactly.
                    .attribute(ClientStreamStore.SSF_STREAM_AUDIENCE_KEY, SHARED_STREAM_AUDIENCE);

            return realm;
        }
    }

    public static class ReceiverRealm implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.name(RX_REALM_NAME);

            // Receiver realm hosts an SSF receiver IdP pointing at the
            // transmitter realm. Issuer + stream audience are hardcoded to
            // the values produced by the transmitter realm so iss/aud
            // validation matches on push. The default
            // expectedSignatureAlgorithms allow-list ({RS256}) matches the
            // transmitter's default signing alg — the whole point of the
            // interop check.
            IdentityProviderRepresentation ssfReceiver = new IdentityProviderRepresentation();
            ssfReceiver.setAlias(RX_IDP_ALIAS);
            ssfReceiver.setProviderId("ssf-receiver");
            ssfReceiver.setDisplayName("Transmitter realm SSF receiver");
            ssfReceiver.setEnabled(true);
            Map<String, String> config = new HashMap<>();
            config.put("description", "Inbound SSF stream from the transmitter realm");
            config.put("issuer", defaultTxBaseUrl());
            config.put("streamAudience", SHARED_STREAM_AUDIENCE);
            config.put("streamId", "interop-stream");
            config.put("transmitterToken", "unused-static-token");
            config.put("transmitterTokenType", "ACCESS_TOKEN");
            config.put("pushAuthorizationHeader", PUSH_AUTH_TOKEN);
            ssfReceiver.setConfig(config);
            realm.identityProvider(ssfReceiver);

            return realm;
        }

        /**
         * The test framework boots Keycloak on {@code localhost:8080} by
         * default. The IdP is created before {@link ManagedRealm#getBaseUrl()}
         * is reachable, so the transmitter realm's issuer URL is reconstructed
         * here from the same convention every other SSF test relies on.
         */
        static String defaultTxBaseUrl() {
            return "http://localhost:8080/realms/" + TX_REALM_NAME;
        }
    }
}
