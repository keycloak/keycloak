package org.keycloak.tests.ssf;

import java.io.IOException;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.common.Profile;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.protocol.ssf.endpoint.SsfPushDeliveryResource;
import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.event.subjects.EmailSubjectId;
import org.keycloak.protocol.ssf.event.subjects.SubjectId;
import org.keycloak.protocol.ssf.event.types.SsfEvent;
import org.keycloak.protocol.ssf.event.types.caep.SessionRevoked;
import org.keycloak.protocol.ssf.receiver.transmitter.SsfTransmitterMetadata;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectHttpServer;
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
import org.keycloak.testframework.util.HttpServerUtil;
import org.keycloak.tests.utils.KeyUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.entity.StringEntity;
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

        ssfReceiverProviderRegistration = createSsfReceiverProviderRegistration();

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

    public IdentityProviderRepresentation createSsfReceiverProviderRegistration() {
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
        config.put("transmitterAccessToken", "dummy-transmitter-token");
        config.put("pushAuthorizationHeader", "expected-push-auth-header");
        ssfReceiverRegistration.setConfig(config);
        return ssfReceiverRegistration;
    }

    public SecurityEventToken generateSecurityEventToken(SubjectId subjectId, SsfEvent event) {

        var securityEventToken = new SecurityEventToken();
        securityEventToken.setId(UUID.randomUUID().toString());
        securityEventToken.issuer("http://127.0.0.1:8500");
        securityEventToken.setTxn(UUID.randomUUID().toString());
        securityEventToken.addAudience("https://keycloak-stream-audience");
        securityEventToken.issuedNow();
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

    protected void sendSsfSetViaPushDelivery(String encodedSetToken) {

        String ssfReceiverAlias = ssfReceiverProviderRegistration.getAlias();
        String pushAuthorizationHeader = ssfReceiverProviderRegistration.getConfig().get("pushAuthorizationHeader");
        String ssfPushEndpoint = realm.getBaseUrl() + "/ssf/push/" + ssfReceiverAlias;

        try (SimpleHttpResponse response = http.doPost(ssfPushEndpoint)
                .header(HttpHeaders.CONTENT_TYPE, SsfPushDeliveryResource.APPLICATION_SECEVENT_JWT_TYPE)
                .header(HttpHeaders.AUTHORIZATION, pushAuthorizationHeader)
                .entity(new StringEntity(encodedSetToken))
                .asResponse()) {
            if (response.getStatus() != 202) {
                Map reponsePayload = response.asJson(Map.class);
                System.out.println(reponsePayload);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String encodeSecurityEventToken(Object tokenPayload, KeyWrapper key) {
        return new JWSBuilder()
                .type(SecurityEventToken.TYPE)
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
