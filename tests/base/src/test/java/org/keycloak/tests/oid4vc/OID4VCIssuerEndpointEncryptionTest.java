package org.keycloak.tests.oid4vc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.util.Base64Url;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.jose.jwk.RSAPublicJWK;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialResponseEncryption;
import org.keycloak.protocol.oid4vc.model.ErrorResponse;
import org.keycloak.protocol.oid4vc.model.ErrorType;
import org.keycloak.protocol.oid4vc.model.NonceResponse;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.managers.AppAuthManager.BearerTokenAuthenticator;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;
import static org.keycloak.jose.jwe.JWEConstants.A256GCM;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider.ATTR_ENCRYPTION_REQUIRED;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider.ATTR_REQUEST_ENCRYPTION_REQUIRED;
import static org.keycloak.tests.oid4vc.OID4VCProofTestUtils.generateJwtProof;
import static org.keycloak.utils.MediaType.APPLICATION_JWT;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCIssuerEndpointEncryptionTest extends OID4VCIssuerEndpointTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @AfterEach
    void logoutAfterEach() {
        AccountHelper.logout(testRealm.admin(), "john");
    }

    @Test
    void testRequestCredentialWithEncryption() throws Exception {
        FlowData flow = prepareFlow();
        Map<String, Object> jwkPair = generateRsaJwkWithPrivateKey();
        JWK responseJwk = (JWK) jwkPair.get("jwk");
        PrivateKey responsePrivateKey = (PrivateKey) jwkPair.get("privateKey");

        CredentialRequest credentialRequest = new CredentialRequest()
                .setCredentialIdentifier(flow.credentialIdentifier())
                .setProofs(new Proofs().setJwt(List.of(generateJwtProof(flow.issuer(), flow.cNonce()))))
                .setCredentialResponseEncryption(new CredentialResponseEncryption().setEnc(A256GCM).setJwk(responseJwk));

        String requestJson = JsonSerialization.writeValueAsString(credentialRequest);
        JWK requestEncryptionJwk = flow.issuerMetadata().getCredentialRequestEncryption().getJwks().getKeys()[0];
        String encryptedRequest = encryptRequest(requestJson, requestEncryptionJwk, false);

        try (Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
            WebTarget target = httpClient.target(flow.issuerMetadata().getCredentialEndpoint());
            try (Response response = target.request()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + flow.token())
                    .post(Entity.entity(encryptedRequest, APPLICATION_JWT))) {
                assertEquals(200, response.getStatus());
                assertEquals(APPLICATION_JWT, response.getMediaType().toString());

                CredentialResponse decryptedResponse = decryptJweResponse(response.readEntity(String.class), responsePrivateKey);
                assertNotNull(decryptedResponse.getCredentials());
                JsonWebToken jwt = TokenVerifier.create((String) decryptedResponse.getCredentials().get(0).getCredential(), JsonWebToken.class).getToken();
                VerifiableCredential credential = JsonSerialization.mapper.convertValue(jwt.getOtherClaims().get("vc"), VerifiableCredential.class);
                assertTrue(credential.getCredentialSubject().getClaims().containsKey("scope-name"));
            }
        }
    }

    @Test
    void testEncryptedCredentialRequest() throws Exception {
        setRealmAttributes(Map.of(ATTR_REQUEST_ENCRYPTION_REQUIRED, "true"));
        try {
            testRequestCredentialWithEncryption();
        } finally {
            setRealmAttributes(Map.of(ATTR_REQUEST_ENCRYPTION_REQUIRED, "false"));
        }
    }

    @Test
    void testEncryptedCredentialRequestWithCompression() throws Exception {
        setRealmAttributes(Map.of(ATTR_REQUEST_ENCRYPTION_REQUIRED, "true", "oid4vci.request.zip.algorithms", "DEF"));
        try {
            FlowData flow = prepareFlow();
            Map<String, Object> jwkPair = generateRsaJwkWithPrivateKey();
            JWK responseJwk = (JWK) jwkPair.get("jwk");
            PrivateKey responsePrivateKey = (PrivateKey) jwkPair.get("privateKey");

            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialIdentifier(flow.credentialIdentifier())
                    .setProofs(new Proofs().setJwt(List.of(generateJwtProof(flow.issuer(), flow.cNonce()))))
                    .setCredentialResponseEncryption(new CredentialResponseEncryption().setEnc(A256GCM).setJwk(responseJwk));

            String requestJson = JsonSerialization.writeValueAsString(credentialRequest);
            JWK requestEncryptionJwk = flow.issuerMetadata().getCredentialRequestEncryption().getJwks().getKeys()[0];
            String encryptedRequest = encryptRequest(requestJson, requestEncryptionJwk, true);

            try (Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
                WebTarget target = httpClient.target(flow.issuerMetadata().getCredentialEndpoint());
                try (Response response = target.request()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + flow.token())
                        .post(Entity.entity(encryptedRequest, APPLICATION_JWT))) {
                    assertEquals(200, response.getStatus());
                    assertEquals(APPLICATION_JWT, response.getMediaType().toString());
                    CredentialResponse decryptedResponse = decryptJweResponse(response.readEntity(String.class), responsePrivateKey);
                    assertNotNull(decryptedResponse.getCredentials());
                }
            }
        } finally {
            setRealmAttributes(Map.of(
                    ATTR_REQUEST_ENCRYPTION_REQUIRED, "false",
                    "oid4vci.request.zip.algorithms", ""
            ));
        }
    }

    @Test
    void testRequestCredentialWithIncompleteEncryptionParams() throws Throwable {
        String token = getBearerToken(oauth, client, jwtTypeCredentialScope.getName());
        withCausePropagation(() -> runOnServer.run(session -> {
            var authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            var endpoint = prepareIssuerEndpoint(session, authenticator);

            JWK jwk = JWKParser.create().parse("{\"kty\":\"RSA\",\"n\":\"test-n\",\"e\":\"AQAB\"}").getJwk();
            CredentialRequest request = new CredentialRequest()
                    .setCredentialIdentifier("test-credential")
                    .setCredentialResponseEncryption(new CredentialResponseEncryption().setJwk(jwk));

            try {
                endpoint.requestCredential(JsonSerialization.writeValueAsString(request));
                fail("Expected BadRequestException");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue(), error.getError());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }));
    }

    @Test
    void testRequestCredentialWithUnsupportedResponseEncryption() throws Exception {
        FlowData flow = prepareFlow();
        Map<String, Object> jwkPair = generateRsaJwkWithPrivateKey();
        JWK responseJwk = (JWK) jwkPair.get("jwk");

        CredentialRequest request = new CredentialRequest()
                .setCredentialIdentifier(flow.credentialIdentifier())
                .setProofs(new Proofs().setJwt(List.of(generateJwtProof(flow.issuer(), flow.cNonce()))))
                .setCredentialResponseEncryption(new CredentialResponseEncryption().setEnc("A128GCM").setJwk(responseJwk));

        String requestJson = JsonSerialization.writeValueAsString(request);
        String encryptedRequest = encryptRequest(requestJson, flow.issuerMetadata().getCredentialRequestEncryption().getJwks().getKeys()[0], false);

        try (Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
            WebTarget target = httpClient.target(flow.issuerMetadata().getCredentialEndpoint());
            try (Response response = target.request()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + flow.token())
                    .post(Entity.entity(encryptedRequest, APPLICATION_JWT))) {
                assertEquals(400, response.getStatus());
                ErrorResponse error = JsonSerialization.readValue(response.readEntity(String.class), ErrorResponse.class);
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue(), error.getError());
            }
        }
    }

    @Test
    void testRequestCredentialWithUnsupportedResponseCompression() throws Exception {
        FlowData flow = prepareFlow();
        Map<String, Object> jwkPair = generateRsaJwkWithPrivateKey();
        JWK responseJwk = (JWK) jwkPair.get("jwk");

        CredentialRequest request = new CredentialRequest()
                .setCredentialIdentifier(flow.credentialIdentifier())
                .setProofs(new Proofs().setJwt(List.of(generateJwtProof(flow.issuer(), flow.cNonce()))))
                .setCredentialResponseEncryption(new CredentialResponseEncryption().setEnc(A256GCM).setZip("UNSUPPORTED-ZIP").setJwk(responseJwk));

        String requestJson = JsonSerialization.writeValueAsString(request);
        String encryptedRequest = encryptRequest(requestJson, flow.issuerMetadata().getCredentialRequestEncryption().getJwks().getKeys()[0], false);

        try (Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
            WebTarget target = httpClient.target(flow.issuerMetadata().getCredentialEndpoint());
            try (Response response = target.request()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + flow.token())
                    .post(Entity.entity(encryptedRequest, APPLICATION_JWT))) {
                assertEquals(400, response.getStatus());
                ErrorResponse error = JsonSerialization.readValue(response.readEntity(String.class), ErrorResponse.class);
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue(), error.getError());
            }
        }
    }

    @Test
    void testRequestCredentialWithInvalidJWK() throws Exception {
        FlowData flow = prepareFlow();
        JWK invalidJwk = JWKParser.create().parse("{\"kty\":\"RSA\",\"alg\":\"RSA-OAEP-256\",\"e\":\"AQAB\"}").getJwk();

        CredentialRequest request = new CredentialRequest()
                .setCredentialIdentifier(flow.credentialIdentifier())
                .setProofs(new Proofs().setJwt(List.of(generateJwtProof(flow.issuer(), flow.cNonce()))))
                .setCredentialResponseEncryption(new CredentialResponseEncryption().setEnc(A256GCM).setJwk(invalidJwk));

        String requestJson = JsonSerialization.writeValueAsString(request);
        String encryptedRequest = encryptRequest(requestJson, flow.issuerMetadata().getCredentialRequestEncryption().getJwks().getKeys()[0], false);

        try (Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
            WebTarget target = httpClient.target(flow.issuerMetadata().getCredentialEndpoint());
            try (Response response = target.request()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + flow.token())
                    .post(Entity.entity(encryptedRequest, APPLICATION_JWT))) {
                assertEquals(400, response.getStatus());
                ErrorResponse error = JsonSerialization.readValue(response.readEntity(String.class), ErrorResponse.class);
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue(), error.getError());
            }
        }
    }

    @Test
    void testRequestCredentialWithMissingResponseEncryptionWhenRequired() throws Exception {
        setRealmAttributes(Map.of(ATTR_ENCRYPTION_REQUIRED, "true"));
        try {
            FlowData flow = prepareFlow();
            CredentialRequest request = new CredentialRequest()
                    .setCredentialIdentifier(flow.credentialIdentifier())
                    .setProofs(new Proofs().setJwt(List.of(generateJwtProof(flow.issuer(), flow.cNonce()))));

            String requestJson = JsonSerialization.writeValueAsString(request);
            String encryptedRequest = encryptRequest(requestJson, flow.issuerMetadata().getCredentialRequestEncryption().getJwks().getKeys()[0], false);

            try (Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
                WebTarget target = httpClient.target(flow.issuerMetadata().getCredentialEndpoint());
                try (Response response = target.request()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + flow.token())
                        .post(Entity.entity(encryptedRequest, APPLICATION_JWT))) {
                    assertEquals(400, response.getStatus());
                    ErrorResponse error = JsonSerialization.readValue(response.readEntity(String.class), ErrorResponse.class);
                    assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS.getValue(), error.getError());
                }
            }
        } finally {
            setRealmAttributes(Map.of(ATTR_ENCRYPTION_REQUIRED, "false"));
        }
    }

    private FlowData prepareFlow() {
        String scopeName = jwtTypeCredentialScope.getName();
        String credConfigId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);
        CredentialIssuer issuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(issuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, TEST_USER, scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        assertNotNull(authDetailsResponse);
        assertFalse(authDetailsResponse.isEmpty());
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);
        assertNotNull(credentialIdentifier);
        String cNonce = getCNonce();
        return new FlowData(token, credentialIdentifier, issuer.getCredentialIssuer(), cNonce, issuer);
    }

    private String getCNonce() {
        UriBuilder builder = UriBuilder.fromUri(keycloakUrls.getBase());
        URI oid4vcUri = RealmsResource.protocolUrl(builder)
                .build(testRealm.getName(), OID4VCLoginProtocolFactory.PROTOCOL_ID);
        String nonceUrl = String.format("%s/%s", oid4vcUri, OID4VCIssuerEndpoint.NONCE_PATH);

        try (Client restClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
            WebTarget nonceTarget = restClient.target(nonceUrl);
            Invocation.Builder nonceInvocationBuilder = nonceTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, null)
                    .header(HttpHeaders.COOKIE, null);
            try (Response response = nonceInvocationBuilder.post(null)) {
                assertEquals(HttpStatus.SC_OK, response.getStatus());
                assertTrue(response.getMediaType().toString().startsWith(MediaType.APPLICATION_JSON_TYPE.toString()));
                NonceResponse nonceResponse = JsonSerialization.readValue(response.readEntity(String.class), NonceResponse.class);
                return nonceResponse.getNonce();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String encryptRequest(String payload, JWK issuerEncJwk, boolean useCompression) throws Exception {
        PublicKey publicKey = JWKParser.create(issuerEncJwk).toPublicKey();
        JWEHeader.JWEHeaderBuilder builder = new JWEHeader.JWEHeaderBuilder()
                .keyId(issuerEncJwk.getKeyId())
                .algorithm(issuerEncJwk.getAlgorithm())
                .encryptionAlgorithm(A256GCM)
                .type("JWT");
        if (useCompression) {
            builder.compressionAlgorithm("DEF");
        }

        byte[] content = useCompression ? compressPayload(payload.getBytes(StandardCharsets.UTF_8))
                : payload.getBytes(StandardCharsets.UTF_8);
        JWE jwe = new JWE().header(builder.build()).content(content);
        jwe.getKeyStorage().setEncryptionKey(publicKey);
        return jwe.encodeJwe();
    }

    private static byte[] compressPayload(byte[] payload) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(out, new Deflater(Deflater.DEFAULT_COMPRESSION, true))) {
            deflater.write(payload);
        }
        return out.toByteArray();
    }

    private static Map<String, Object> generateRsaJwkWithPrivateKey() throws NoSuchAlgorithmException {
        var keyGen = java.security.KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        var keyPair = keyGen.generateKeyPair();
        var publicKey = (RSAPublicKey) keyPair.getPublic();

        String modulus = Base64Url.encode(publicKey.getModulus().toByteArray());
        String exponent = Base64Url.encode(publicKey.getPublicExponent().toByteArray());

        RSAPublicJWK jwk = new RSAPublicJWK();
        jwk.setKeyType("RSA");
        jwk.setPublicKeyUse("enc");
        jwk.setAlgorithm("RSA-OAEP");
        jwk.setModulus(modulus);
        jwk.setPublicExponent(exponent);

        return Map.of("jwk", jwk, "privateKey", keyPair.getPrivate());
    }

    private static CredentialResponse decryptJweResponse(String encryptedResponse, PrivateKey privateKey) throws IOException, JWEException {
        JWE jwe = new JWE(encryptedResponse);
        jwe.getKeyStorage().setDecryptionKey(privateKey);
        jwe.verifyAndDecodeJwe();
        return JsonSerialization.readValue(jwe.getContent(), CredentialResponse.class);
    }

    private record FlowData(String token, String credentialIdentifier, String issuer, String cNonce,
                            CredentialIssuer issuerMetadata) {
    }
}
