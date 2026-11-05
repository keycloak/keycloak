package org.keycloak.broker.kubernetes;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.JsonSerialization;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KubernetesJwksEndpointLoaderTest {

    private static final String TRUSTED_JWKS_URL = "https://kubernetes.default.svc/openid/v1/jwks";
    private static final String EXTERNAL_JWKS_URL = "https://oidc.eks.us-east-1.amazonaws.com/id/12345/keys";

    private HttpServer server;
    private String serverBaseUrl;
    private final List<RecordedRequest> recordedRequests = new CopyOnWriteArrayList<>();
    private CloseableHttpClient httpClient;
    private KeycloakSession session;

    private record RecordedRequest(String path, String authorization) {
    }

    @BeforeAll
    static void initCrypto() {
        CryptoIntegration.init(KubernetesJwksEndpointLoaderTest.class.getClassLoader());
    }

    @BeforeEach
    void setUp() throws IOException {
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
        JWK jwk = JWKBuilder.create().kid("test-key").rs256(keyPair.getPublic());
        JSONWebKeySet jwks = new JSONWebKeySet();
        jwks.setKeys(new JWK[]{jwk});
        String jwksJson = JsonSerialization.writeValueAsString(jwks);

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serverBaseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

        server.createContext("/.well-known/openid-configuration", exchange -> {
            recordedRequests.add(new RecordedRequest(exchange.getRequestURI().getPath(), exchange.getRequestHeaders().getFirst("Authorization")));
            String discoveryResponse = "{\"issuer\":\"" + serverBaseUrl + "\",\"jwks_uri\":\"" + serverBaseUrl + "/jwks\"}";
            sendResponse(exchange, 200, discoveryResponse, "application/json");
        });

        server.createContext("/jwks", exchange -> {
            recordedRequests.add(new RecordedRequest(exchange.getRequestURI().getPath(), exchange.getRequestHeaders().getFirst("Authorization")));
            sendResponse(exchange, 200, jwksJson, "application/jwk-set+json");
        });

        server.start();

        int serverPort = server.getAddress().getPort();
        httpClient = HttpClients.custom()
                .setRoutePlanner((target, request, context) -> {
                    if ("kubernetes.default.svc".equalsIgnoreCase(target.getHostName()) || "kubernetes".equalsIgnoreCase(target.getHostName())) {
                        return new HttpRoute(new HttpHost("127.0.0.1", serverPort, "http"));
                    }
                    return new DefaultRoutePlanner(null).determineRoute(target, request, context);
                })
                .build();

        HttpClientProvider httpClientProvider = (HttpClientProvider) Proxy.newProxyInstance(
                HttpClientProvider.class.getClassLoader(),
                new Class<?>[]{HttpClientProvider.class},
                (proxy, method, args) -> {
                    if ("getHttpClient".equals(method.getName())) {
                        return httpClient;
                    }
                    if ("getMaxConsumedResponseSize".equals(method.getName())) {
                        return HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE;
                    }
                    return null;
                }
        );

        session = (KeycloakSession) Proxy.newProxyInstance(
                KeycloakSession.class.getClassLoader(),
                new Class<?>[]{KeycloakSession.class},
                (proxy, method, args) -> {
                    if ("getProvider".equals(method.getName()) && args != null && args.length == 1 && args[0] == HttpClientProvider.class) {
                        return httpClientProvider;
                    }
                    return null;
                }
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        if (server != null) {
            server.stop(0);
        }
        if (httpClient != null) {
            httpClient.close();
        }
    }

    @Test
    void loadKeysDoesNotSendAuthorizationHeaderToPublicJwksEndpoint(@TempDir Path tempDir) throws Exception {
        Path tokenFile = tempDir.resolve("token");
        String token = createToken(serverBaseUrl);
        Files.writeString(tokenFile, token);

        KubernetesJwksEndpointLoader loader = new KubernetesJwksEndpointLoader(
                session, serverBaseUrl, serverBaseUrl + "/jwks", false, tokenFile.toFile());

        PublicKeysWrapper keys = loader.loadKeys();
        assertNotNull(keys);
        assertEquals(1, recordedRequests.size());
        assertEquals("/jwks", recordedRequests.get(0).path());
        assertNull(recordedRequests.get(0).authorization());
    }

    @Test
    void loadKeysSendsAuthorizationHeaderWhenForwardingEnabled(@TempDir Path tempDir) throws Exception {
        Path tokenFile = tempDir.resolve("token");
        String token = createToken(serverBaseUrl);
        Files.writeString(tokenFile, token);

        KubernetesJwksEndpointLoader loader = new KubernetesJwksEndpointLoader(
                session, serverBaseUrl, serverBaseUrl + "/jwks", true, tokenFile.toFile());

        PublicKeysWrapper keys = loader.loadKeys();
        assertNotNull(keys);
        assertEquals(1, recordedRequests.size());
        assertEquals("/jwks", recordedRequests.get(0).path());
        assertEquals("Bearer " + token, recordedRequests.get(0).authorization());
    }

    @Test
    void loadKeysSendsTokenToTrustedEndpointEvenWhenForwardingDisabled(@TempDir Path tempDir) throws Exception {
        String trustedIssuer = "https://kubernetes.default.svc";
        Path tokenFile = tempDir.resolve("token");
        String token = createToken(trustedIssuer);
        Files.writeString(tokenFile, token);

        KubernetesJwksEndpointLoader loader = new KubernetesJwksEndpointLoader(
                session, trustedIssuer, "https://kubernetes.default.svc/jwks", false, tokenFile.toFile());

        PublicKeysWrapper keys = loader.loadKeys();
        assertNotNull(keys);
        assertEquals(1, recordedRequests.size());
        assertEquals("/jwks", recordedRequests.get(0).path());
        assertEquals("Bearer " + token, recordedRequests.get(0).authorization());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void loadKeysRequiresOptInForNamespaceRelativeHost(boolean forwardingEnabled, @TempDir Path tempDir) throws Exception {
        Path tokenFile = tempDir.resolve("token");
        String token = createToken(serverBaseUrl);
        Files.writeString(tokenFile, token);

        KubernetesJwksEndpointLoader loader = new KubernetesJwksEndpointLoader(
                session, serverBaseUrl, "https://kubernetes/jwks", forwardingEnabled, tokenFile.toFile());

        assertNotNull(loader.loadKeys());
        assertEquals(1, recordedRequests.size());
        assertEquals("/jwks", recordedRequests.get(0).path());
        assertEquals(forwardingEnabled ? "Bearer " + token : null, recordedRequests.get(0).authorization());
    }

    @Test
    void loadKeysDoesNotSendTokenWhenIssuerMismatchesEvenIfForwardingEnabled(@TempDir Path tempDir) throws Exception {
        Path tokenFile = tempDir.resolve("token");
        String token = createToken("https://other-issuer.example.test");
        Files.writeString(tokenFile, token);

        KubernetesJwksEndpointLoader loader = new KubernetesJwksEndpointLoader(
                session, serverBaseUrl, serverBaseUrl + "/jwks", true, tokenFile.toFile());

        PublicKeysWrapper keys = loader.loadKeys();
        assertNotNull(keys);
        assertEquals(1, recordedRequests.size());
        assertEquals("/jwks", recordedRequests.get(0).path());
        assertNull(recordedRequests.get(0).authorization());
    }

    @Test
    void loadKeysFallbackDiscoveryDoesNotSendTokenToExternalEndpoints(@TempDir Path tempDir) throws Exception {
        Path tokenFile = tempDir.resolve("token");
        String token = createToken(serverBaseUrl);
        Files.writeString(tokenFile, token);

        KubernetesJwksEndpointLoader loader = new KubernetesJwksEndpointLoader(
                session, serverBaseUrl, null, false, tokenFile.toFile());

        PublicKeysWrapper keys = loader.loadKeys();
        assertNotNull(keys);
        assertEquals(2, recordedRequests.size());
        assertEquals("/.well-known/openid-configuration", recordedRequests.get(0).path());
        assertNull(recordedRequests.get(0).authorization());
        assertEquals("/jwks", recordedRequests.get(1).path());
        assertNull(recordedRequests.get(1).authorization());
    }

    @Test
    void loadKeysFallbackDiscoverySendsTokenWhenForwardingEnabled(@TempDir Path tempDir) throws Exception {
        Path tokenFile = tempDir.resolve("token");
        String token = createToken(serverBaseUrl);
        Files.writeString(tokenFile, token);

        KubernetesJwksEndpointLoader loader = new KubernetesJwksEndpointLoader(
                session, serverBaseUrl, null, true, tokenFile.toFile());

        PublicKeysWrapper keys = loader.loadKeys();
        assertNotNull(keys);
        assertEquals(2, recordedRequests.size());
        assertEquals("/.well-known/openid-configuration", recordedRequests.get(0).path());
        assertEquals("Bearer " + token, recordedRequests.get(0).authorization());
        assertEquals("/jwks", recordedRequests.get(1).path());
        assertEquals("Bearer " + token, recordedRequests.get(1).authorization());
    }

    @Test
    void isTokenForwardingAllowedDisabledByDefault() {
        KubernetesJwksEndpointLoader loader = new KubernetesJwksEndpointLoader(null, "https://issuer.example.test", EXTERNAL_JWKS_URL);

        assertTrue(loader.isTokenForwardingAllowed(TRUSTED_JWKS_URL));
        assertFalse(loader.isTokenForwardingAllowed(EXTERNAL_JWKS_URL));
    }

    @Test
    void isTokenForwardingAllowedWhenExplicitlyDisabled() {
        KubernetesJwksEndpointLoader loader = new KubernetesJwksEndpointLoader(null, "https://issuer.example.test", EXTERNAL_JWKS_URL, false);

        assertTrue(loader.isTokenForwardingAllowed(TRUSTED_JWKS_URL));
        assertFalse(loader.isTokenForwardingAllowed(EXTERNAL_JWKS_URL));
    }

    @Test
    void isTokenForwardingAllowedWhenExplicitlyEnabled() {
        KubernetesJwksEndpointLoader loader = new KubernetesJwksEndpointLoader(null, "https://issuer.example.test", EXTERNAL_JWKS_URL, true);

        assertTrue(loader.isTokenForwardingAllowed(TRUSTED_JWKS_URL));
        assertTrue(loader.isTokenForwardingAllowed(EXTERNAL_JWKS_URL));
        assertTrue(loader.isTokenForwardingAllowed("https://login.microsoftonline.com/common/discovery/v2.0/keys"));
    }

    @Test
    void isTokenForwardingAllowedWithConfig() {
        KubernetesIdentityProviderConfig config = new KubernetesIdentityProviderConfig();
        config.getConfig().put(KubernetesIdentityProviderConfig.ISSUER, "https://issuer.example.test");
        config.getConfig().put(OIDCIdentityProviderConfig.JWKS_URL, EXTERNAL_JWKS_URL);

        KubernetesJwksEndpointLoader loaderDefault = new KubernetesJwksEndpointLoader(null, config);
        assertTrue(loaderDefault.isTokenForwardingAllowed(TRUSTED_JWKS_URL));
        assertFalse(loaderDefault.isTokenForwardingAllowed(EXTERNAL_JWKS_URL));

        config.setForwardTokenToAdditionalEndpoints(true);
        KubernetesJwksEndpointLoader loaderEnabled = new KubernetesJwksEndpointLoader(null, config);
        assertTrue(loaderEnabled.isTokenForwardingAllowed(TRUSTED_JWKS_URL));
        assertTrue(loaderEnabled.isTokenForwardingAllowed(EXTERNAL_JWKS_URL));
    }

    private String createToken(String issuer) {
        return new JWSBuilder().jsonContent(new JsonWebToken().issuer(issuer)).none();
    }

    private void sendResponse(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
