package org.keycloak.tests.ssf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.protocol.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.protocol.ssf.event.subjects.EmailSubjectId;
import org.keycloak.protocol.ssf.event.subjects.SubjectId;
import org.keycloak.protocol.ssf.event.SsfEvent;
import org.keycloak.protocol.ssf.event.caep.SessionRevoked;
import org.keycloak.protocol.ssf.transmitter.metadata.SsfTransmitterMetadata;
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
import org.keycloak.tests.utils.KeyUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = SsfReceiverTests.SsfKeycloakServerConfig.class)
public class SsfReceiverTests {

    @InjectRealm(config = SsfReceiverRealm.class)
    ManagedRealm realm;

    @InjectSimpleHttp
    SimpleHttp http;

    @InjectHttpServer
    HttpServer mockTransmitterServer;

    @InjectOAuthClient
    OAuthClient oauthClient;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    KeyWrapper keyWrapper;

    IdentityProviderRepresentation ssfReceiverProviderRegistration;

    @BeforeEach
    public void setup() throws IOException {

        // create keypair
        KeyPair es256KeyPair = KeyUtils.generateECKey("ES256");

        keyWrapper = new KeyWrapper();
        keyWrapper.setAlgorithm("ES256");
        keyWrapper.setKid("ssf-transmitter-key-1");
        keyWrapper.setPrivateKey(es256KeyPair.getPrivate());

        ssfReceiverProviderRegistration = createSsfReceiverRegistration();

        realm.admin().identityProviders().create(ssfReceiverProviderRegistration);

        // create public key JWKS
        JWK ecPubKey = JWKBuilder.create().ec(es256KeyPair.getPublic(), KeyUse.SIG);
        ecPubKey.setKeyId(keyWrapper.getKid());
        Map<String, Object> jwks = new HashMap<>(Map.of("keys", List.of(ecPubKey)));

        mockTransmitterServer.createContext("/jwks.json", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {

                String jwksString = JsonSerialization.writeValueAsString(jwks);

                HttpServerUtil.sendResponse(exchange, 200,
                        Map.of("Content-Type", List.of("application/json")),
                        jwksString
                );
            }
        });

        mockTransmitterServer.createContext("/.well-known/ssf-configuration", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {

                // create minimal mock ssf-configuration
                SsfTransmitterMetadata transmitterMetadata = new SsfTransmitterMetadata();
                transmitterMetadata.setIssuer("http://127.0.0.1:8500");
                transmitterMetadata.setSpecVersion("1_0");
                transmitterMetadata.setJwksUri("http://127.0.0.1:8500/jwks.json");
                transmitterMetadata.setVerificationEndpoint("http://127.0.0.1:8500/verify");
                transmitterMetadata.setDeliveryMethodSupported(
                        Set.of("urn:ietf:rfc:8935") // PUSH Delivery
                );
                transmitterMetadata.setDefaultSubjects("NONE");
                String transmitterMetadataJson = JsonSerialization.writeValueAsString(transmitterMetadata);

                HttpServerUtil.sendResponse(exchange, 200,
                        Map.of("Content-Type", List.of("application/json")),
                        transmitterMetadataJson
                );
            }
        });

    }

    @AfterEach
    public void cleanup() {
        // Remove IdP created in setup to avoid conflicts on next test run
        try {
            realm.admin().identityProviders().get("dummy-transmitter").remove();
        } catch (Exception ignored) {
        }

        // Remove mock server contexts to allow re-creation in next setup
        try {
            mockTransmitterServer.removeContext("/jwks.json");
        } catch (Exception ignored) {
        }
        try {
            mockTransmitterServer.removeContext("/.well-known/ssf-configuration");
        } catch (Exception ignored) {
        }
    }

    public IdentityProviderRepresentation createSsfReceiverRegistration() {
        var ssfReceiverRegistration = new IdentityProviderRepresentation();
        ssfReceiverRegistration.setAlias("dummy-transmitter");
        ssfReceiverRegistration.setProviderId("ssf-receiver");
        ssfReceiverRegistration.setDisplayName("Dummy SSF Receiver");
        ssfReceiverRegistration.setEnabled(true);
        Map<String, String> config = new HashMap<>();
        config.put("clientId", "dummy-transmitter-client");
        config.put("streamId", "dummy-stream-id");
        config.put("description", "Description SSF Receiver");
        config.put("streamAudience", "https://keycloak-stream-audience");
        config.put("issuer", "http://127.0.0.1:8500");
        config.put("transmitterToken", "dummy-transmitter-token");
        config.put("transmitterTokenType", "ACCESS_TOKEN");
        config.put("pushAuthorizationHeader", "expected-push-auth-header");
        ssfReceiverRegistration.setConfig(config);
        return ssfReceiverRegistration;
    }

    public SsfSecurityEventToken generateSecurityEventToken(SubjectId subjectId, SsfEvent event) {

        var securityEventToken = new SsfSecurityEventToken();
        securityEventToken.setJti(UUID.randomUUID().toString());
        securityEventToken.setIss("http://127.0.0.1:8500");
        securityEventToken.setTxn(UUID.randomUUID().toString());
        securityEventToken.setAud(new String[]{"https://keycloak-stream-audience"});
        securityEventToken.setIat(Time.currentTime());
        securityEventToken.setSubjectId(subjectId);
        securityEventToken.setEvents(Map.of(event.getEventType(), event));

        return securityEventToken;
    }

    @Test
    public void testSetPushDelivery() throws InterruptedException {

        // generate a SSF SET for session revoked
        var testerSubject = new EmailSubjectId();
        testerSubject.setEmail("tester@local.test");

        var sessionRevoked = new SessionRevoked();
        sessionRevoked.setEventTimestamp(System.currentTimeMillis());

        var securityEventToken = generateSecurityEventToken(testerSubject, sessionRevoked);

        String encodedSetToken = encodeSecurityEventToken(securityEventToken, keyWrapper);

        // password grant implicitly creates an active user session
        AccessTokenResponse accessTokenResponse = oauthClient
                .passwordGrantRequest("tester", "test")
                .send();

        String userAccessToken = accessTokenResponse.getAccessToken();

        // check if access token is associated with active user session
        var introspectionResponse = oauthClient.doIntrospectionAccessTokenRequest(userAccessToken);
        try {
            Assertions.assertTrue(introspectionResponse.asJsonNode().get("active").asBoolean());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        TimeUnit.SECONDS.sleep(1);

        // PUSH session revoked CAEP event via SSF
        sendSsfSetViaPushDelivery(encodedSetToken);

        TimeUnit.SECONDS.sleep(1);

        // access token should no longer be associated with active user session
        introspectionResponse = oauthClient.doIntrospectionAccessTokenRequest(userAccessToken);
        try {
            Assertions.assertFalse(introspectionResponse.asJsonNode().get("active").asBoolean());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testVerificationWithClientCredentials() throws IOException {

        String ccReceiverAlias = "cc-transmitter";

        // Track what the mock verify endpoint receives
        AtomicReference<String> receivedVerifyAuthHeader = new AtomicReference<>();
        AtomicReference<String> receivedTokenRequestBody = new AtomicReference<>();

        // Mock /token endpoint that validates client credentials and returns an access token
        mockTransmitterServer.createContext("/token", exchange -> {
            try {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                receivedTokenRequestBody.set(body);

                // Return a mock access token response
                String tokenResponse = "{\"access_token\":\"mock-cc-token\",\"token_type\":\"bearer\",\"expires_in\":300}";
                HttpServerUtil.sendResponse(exchange, 200,
                        Map.of("Content-Type", List.of("application/json")),
                        tokenResponse
                );
            } catch (Exception e) {
                HttpServerUtil.sendResponse(exchange, 500, Map.of(), "Internal Error");
            }
        });

        // Mock /verify endpoint that records the Authorization header and returns 204
        mockTransmitterServer.createContext("/verify", exchange -> {
            receivedVerifyAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            HttpServerUtil.sendResponse(exchange, 204, Map.of());
        });

        // Create an SSF receiver with CLIENT_CREDENTIALS auth method
        var ccReceiverRegistration = new IdentityProviderRepresentation();
        ccReceiverRegistration.setAlias(ccReceiverAlias);
        ccReceiverRegistration.setProviderId("ssf-receiver");
        ccReceiverRegistration.setDisplayName("CC SSF Receiver");
        ccReceiverRegistration.setEnabled(true);
        Map<String, String> ccConfig = new HashMap<>();
        ccConfig.put("streamId", "cc-stream-id");
        ccConfig.put("description", "Client Credentials SSF Receiver");
        ccConfig.put("streamAudience", "https://keycloak-stream-audience");
        ccConfig.put("issuer", "http://127.0.0.1:8500");
        ccConfig.put("transmitterAuthMethod", "CLIENT_CREDENTIALS");
        ccConfig.put("tokenUrl", "http://127.0.0.1:8500/token");
        ccConfig.put("clientId", "test-cc-client");
        ccConfig.put("clientSecret", "test-cc-secret");
        ccConfig.put("clientAuthMethod", "client_secret_post");
        ccConfig.put("pushAuthorizationHeader", "expected-push-auth-header");
        ccReceiverRegistration.setConfig(ccConfig);

        realm.admin().identityProviders().create(ccReceiverRegistration);

        try {
            // Trigger verification via admin API
            String verifyUrl = keycloakUrls.getAdmin() + "/realms/" + realm.getName() + "/ssf/receivers/" + ccReceiverAlias + "/verify";
            try (SimpleHttpResponse response = http.doPost(verifyUrl)
                    .auth(adminClient.tokenManager().getAccessTokenString())
                    .entity(new StringEntity(""))
                    .asResponse()) {
                Assertions.assertEquals(204, response.getStatus(),
                        "Verification should succeed with 204");
            }

            // Assert the mock token endpoint was called with client credentials
            String tokenBody = receivedTokenRequestBody.get();
            Assertions.assertNotNull(tokenBody, "Token endpoint should have been called");
            Assertions.assertTrue(tokenBody.contains("grant_type=client_credentials"),
                    "Token request should contain grant_type=client_credentials");
            Assertions.assertTrue(tokenBody.contains("client_id=test-cc-client"),
                    "Token request should contain client_id");
            Assertions.assertTrue(tokenBody.contains("client_secret=test-cc-secret"),
                    "Token request should contain client_secret");

            // Assert the verification endpoint received the dynamically obtained token
            Assertions.assertEquals("Bearer mock-cc-token", receivedVerifyAuthHeader.get(),
                    "Verification endpoint should receive the dynamically obtained token");
        } finally {
            // Cleanup: remove the receiver
            realm.admin().identityProviders().get(ccReceiverAlias).remove();
            mockTransmitterServer.removeContext("/token");
            mockTransmitterServer.removeContext("/verify");
        }
    }

    protected void sendSsfSetViaPushDelivery(String encodedSetToken) {

        String ssfReceiverAlias = ssfReceiverProviderRegistration.getAlias();
        String pushAuthorizationHeader = "Bearer " + ssfReceiverProviderRegistration.getConfig().get("pushAuthorizationHeader");
        String ssfPushEndpoint = realm.getBaseUrl() + "/ssf/receivers/"+ ssfReceiverAlias + "/push/";

        try (SimpleHttpResponse response = http.doPost(ssfPushEndpoint)
                .header(HttpHeaders.CONTENT_TYPE, Ssf.APPLICATION_SECEVENT_JWT_TYPE)
                .header(HttpHeaders.AUTHORIZATION, pushAuthorizationHeader)
                .entity(new StringEntity(encodedSetToken))
                .asResponse()) {
            if (response.getStatus() != 202) {
                Map reponsePayload = response.asJson(Map.class);
                System.out.println(reponsePayload);
                throw new RuntimeException("Unexpected response status: " + response.getStatus() + " " + reponsePayload + "");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String encodeSecurityEventToken(Object tokenPayload, KeyWrapper key) {
        return new JWSBuilder()
                .type(Ssf.SECEVENT_JWT_TYPE)
                .jsonContent(tokenPayload)
                .sign(new ECDSASignatureSignerContext(key));
    }

    public static class SsfKeycloakServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            var configure = super.configure(config);
            config.features(Profile.Feature.SSF);
            config.log().categoryLevel("org.keycloak.protocol.ssf", "DEBUG");

            return configure;
        }
    }

    public static class SsfReceiverRealm implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {

            realm.name("ssf-receiver-test");

            // How to configure default client scopes?

            // client used to call into the receiver push endpoint
//            ClientConfigBuilder ssfClient = realm.addClient("ssf-transmitter-client");
//            ssfClient.clientId("ssf-client");
//            ssfClient.secret("secret");
//            ssfClient.serviceAccountsEnabled(true);

            UserConfigBuilder tester = realm.addUser("tester");
            tester.email("tester@local.test");
            tester.firstName("Theo");
            tester.lastName("Tester");
            tester.enabled(true);
            tester.password("test");

            return realm;
        }
    }
}
