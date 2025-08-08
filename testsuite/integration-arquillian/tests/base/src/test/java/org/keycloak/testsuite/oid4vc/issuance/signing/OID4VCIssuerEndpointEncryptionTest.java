/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.oid4vc.issuance.signing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialResponseEncryption;
import org.keycloak.protocol.oid4vc.model.ErrorResponse;
import org.keycloak.protocol.oid4vc.model.ErrorType;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for Credential Request and Response Encryption as per spec
 *
 * @author Bertrand Ogen
 */

public class OID4VCIssuerEndpointEncryptionTest extends OID4VCIssuerEndpointTest {

    private JWK getIssuerJwk(KeycloakSession session) {
        CredentialIssuer issuerMetadata = (CredentialIssuer) new OID4VCIssuerWellKnownProvider(session).getConfig();
        return issuerMetadata.getCredentialRequestEncryption().getJwks().getKeys()[0]; // Assume first key
    }

    private String createJwePayload(KeycloakSession session, CredentialRequest credentialRequest, String alg, String enc, String zip) {
        try {
            JWK requestJwk = getIssuerJwk(session);
            PublicKey requestPublicKey = JWKParser.create(requestJwk).toPublicKey();
            byte[] content = JsonSerialization.writeValueAsBytes(credentialRequest);
            JWEHeader header = new JWEHeader.JWEHeaderBuilder()
                    .keyId(requestJwk.getKeyId())
                    .algorithm(alg != null ? alg : JWEConstants.RSA_OAEP_256)
                    .encryptionAlgorithm(enc != null ? enc : "A256GCM")
                    .compressionAlgorithm(zip)
                    .build();
            JWE jwe = new JWE()
                    .header(header)
                    .content(content);
            jwe.getKeyStorage().setEncryptionKey(requestPublicKey);
            return jwe.encodeJwe();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create JWE payload: " + e.getMessage(), e);
        }
    }

    @Test
    public void testRequestCredentialWithEncryption() {
        final String scopeName = jwtTypeCredentialClientScope.getName();
        String token = getBearerToken(oauth, client, scopeName);
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            Map<String, Object> jwkPair;
            try {
                jwkPair = generateRsaJwkWithPrivateKey();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to generate JWK", e);
            }
            JWK responseJwk = (JWK) jwkPair.get("jwk");
            PrivateKey responsePrivateKey = (PrivateKey) jwkPair.get("privateKey");

            CredentialRequest credentialRequest = new CredentialRequest()
                    .setFormat(Format.JWT_VC)
                    .setCredentialIdentifier(scopeName)
                    .setCredentialResponseEncryption(
                            new CredentialResponseEncryption()
                                    .setEnc("A256GCM")
                                    .setJwk(responseJwk));

            String requestPayload = createJwePayload(session, credentialRequest, JWEConstants.RSA_OAEP_256, "A256GCM", null);

            Response credentialResponse = issuerEndpoint.requestCredential(requestPayload);

            assertEquals("The credential request should be answered successfully.",
                    HttpStatus.SC_OK, credentialResponse.getStatus());
            assertEquals("Response should be JWT type for encrypted responses",
                    org.keycloak.utils.MediaType.APPLICATION_JWT, credentialResponse.getMediaType().toString());

            String encryptedResponse = (String) credentialResponse.getEntity();
            CredentialResponse decryptedResponse;
            try {
                decryptedResponse = decryptJweResponse(encryptedResponse, responsePrivateKey);
            } catch (IOException | JWEException e) {
                Assert.fail("Failed to decrypt JWE response: " + e.getMessage());
                return;
            }

            assertNotNull("Decrypted response should contain a credential", decryptedResponse.getCredentials());
            JsonWebToken jsonWebToken;
            try {
                jsonWebToken = TokenVerifier.create((String) decryptedResponse.getCredentials().get(0).getCredential(), JsonWebToken.class).getToken();
            } catch (VerificationException e) {
                Assert.fail("Failed to verify JWT: " + e.getMessage());
                return;
            }
            assertNotNull("A valid credential string should have been responded", jsonWebToken);
            VerifiableCredential credential = JsonSerialization.mapper.convertValue(
                    jsonWebToken.getOtherClaims().get("vc"), VerifiableCredential.class);
            assertTrue("The static claim should be set.", credential.getCredentialSubject().getClaims().containsKey("scope-name"));
        });
    }

    @Test
    public void testRequestCredentialWithCompression() {
        final String scopeName = jwtTypeCredentialClientScope.getName();
        String token = getBearerToken(oauth, client, scopeName);
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            Map<String, Object> jwkPair;
            try {
                jwkPair = generateRsaJwkWithPrivateKey();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to generate JWK", e);
            }
            JWK responseJwk = (JWK) jwkPair.get("jwk");
            PrivateKey responsePrivateKey = (PrivateKey) jwkPair.get("privateKey");

            CredentialRequest credentialRequest = new CredentialRequest()
                    .setFormat(Format.JWT_VC)
                    .setCredentialIdentifier(scopeName)
                    .setCredentialResponseEncryption(
                            new CredentialResponseEncryption()
                                    .setEnc("A256GCM")
                                    .setZip("DEF")
                                    .setJwk(responseJwk));

            String requestPayload = createJwePayload(session, credentialRequest, JWEConstants.RSA_OAEP_256, "A256GCM", "DEF");

            Response credentialResponse = issuerEndpoint.requestCredential(requestPayload);

            assertEquals(HttpStatus.SC_OK, credentialResponse.getStatus());
            assertEquals(org.keycloak.utils.MediaType.APPLICATION_JWT, credentialResponse.getMediaType().toString());

            String encryptedResponse = (String) credentialResponse.getEntity();
            try {
                String[] parts = encryptedResponse.split("\\.");
                String header = new String(Base64Url.decode(parts[0]), StandardCharsets.UTF_8);
                JsonObject headerJson = JsonSerialization.readValue(header, JsonObject.class);

                JsonElement zipElement = headerJson.get("zip");
                assertNotNull("zip field should exist in header", zipElement);
                assertEquals("DEF", zipElement.getAsString());

                CredentialResponse decryptedResponse = decryptJweResponse(encryptedResponse, responsePrivateKey);
                assertNotNull(decryptedResponse.getCredentials());
            } catch (Exception e) {
                Assert.fail("Failed to process JWE response: " + e.getMessage());
            }
        });
    }

    @Test
    public void testRequestCredentialWithInvalidJWE() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            // Malformed JWE (invalid structure)
            String invalidJwePayload = "header.payload.signature.invalid";
            try {
                issuerEndpoint.requestCredential(invalidJwePayload);
                Assert.fail("Expected BadRequestException for invalid JWE");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue(error.getErrorDescription().contains("Failed to decrypt JWE"));
            }
        });
    }

    @Test
    public void testRequestCredentialWithUnsupportedJWEAlgorithm() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CredentialRequest credentialRequest = new CredentialRequest()
                    .setFormat(Format.JWT_VC)
                    .setCredentialIdentifier("test-credential");

            // Use unsupported algorithm
            String requestPayload = createJwePayload(session, credentialRequest, "RSA-OAEP", "A256GCM", null);
            try {
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException for unsupported JWE algorithm");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue(error.getErrorDescription().contains("Unsupported algorithm: RSA-OAEP"));
            }
        });
    }

    @Test
    public void testRequestCredentialWithUnsupportedJWEEncryption() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CredentialRequest credentialRequest = new CredentialRequest()
                    .setFormat(Format.JWT_VC)
                    .setCredentialIdentifier("test-credential");

            // Use unsupported encryption
            String requestPayload = createJwePayload(session, credentialRequest, JWEConstants.RSA_OAEP_256, "A128GCM", null);
            try {
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException for unsupported JWE encryption");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue(error.getErrorDescription().contains("Unsupported encryption algorithm: A128GCM"));
            }
        });
    }

    @Test
    public void testRequestCredentialWithUnsupportedJWECompression() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CredentialRequest credentialRequest = new CredentialRequest()
                    .setFormat(Format.JWT_VC)
                    .setCredentialIdentifier("test-credential");

            // Use unsupported compression
            String requestPayload = createJwePayload(session, credentialRequest, JWEConstants.RSA_OAEP_256, "A256GCM", "UNSUPPORTED-ZIP");
            try {
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException for unsupported JWE compression");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue(error.getErrorDescription().contains("Unsupported compression algorithm: UNSUPPORTED-ZIP"));
            }
        });
    }

    @Test
    public void testRequestCredentialWithInvalidJWKKeyId() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CredentialRequest credentialRequest = new CredentialRequest()
                    .setFormat(Format.JWT_VC)
                    .setCredentialIdentifier("test-credential");

            // Create JWE with invalid kid
            try {
                byte[] content = JsonSerialization.writeValueAsBytes(credentialRequest);
                JWEHeader header = new JWEHeader.JWEHeaderBuilder()
                        .keyId("invalid-kid")
                        .algorithm(JWEConstants.RSA_OAEP_256)
                        .encryptionAlgorithm("A256GCM")
                        .build();
                JWE jwe = new JWE()
                        .header(header)
                        .content(content);
                // Note: Skip encryption to simulate invalid kid, as encryption would fail without valid key
                String requestPayload = Base64Url.encode(header.toString().getBytes()) + ".." + Base64Url.encode(content) + "..";
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException for invalid JWK kid");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue(error.getErrorDescription().contains("No JWK found for kid: invalid-kid"));
            }
        });
    }

    @Test
    public void testRequestCredentialWithMismatchedJWKAlgorithm() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CredentialRequest credentialRequest = new CredentialRequest()
                    .setFormat(Format.JWT_VC)
                    .setCredentialIdentifier("test-credential");

            // Create JWE with mismatched alg
            try {
                JWK requestJwk = getIssuerJwk(session);
                PublicKey requestPublicKey = JWKParser.create(requestJwk).toPublicKey();
                byte[] content = JsonSerialization.writeValueAsBytes(credentialRequest);
                JWEHeader header = new JWEHeader.JWEHeaderBuilder()
                        .keyId(requestJwk.getKeyId())
                        .algorithm("RSA-OAEP") // Mismatch with JWK's alg (RSA-OAEP-256)
                        .encryptionAlgorithm("A256GCM")
                        .build();
                JWE jwe = new JWE()
                        .header(header)
                        .content(content);
                jwe.getKeyStorage().setEncryptionKey(requestPublicKey);
                String requestPayload = jwe.encodeJwe();
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException for mismatched JWK algorithm");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue(error.getErrorDescription().contains("JWE alg does not match JWK alg: RSA-OAEP"));
            } catch (JWEException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testRequestCredentialWithMissingPrivateKey() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CredentialRequest credentialRequest = new CredentialRequest()
                    .setFormat(Format.JWT_VC)
                    .setCredentialIdentifier("test-credential");

            // Use a kid that doesn’t exist in the realm’s key store
            String nonExistentKid = UUID.randomUUID().toString();
            String requestPayload = createJwePayload(session, credentialRequest, JWEConstants.RSA_OAEP_256, "A256GCM", null);
            try {
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException for missing private key");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue(error.getErrorDescription().contains("No private key found for kid: " + nonExistentKid));
            }
        });
    }

    @Test
    public void testRequestCredentialWithInvalidJsonPayload() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            // Invalid JSON payload
            String requestPayload = "{invalid: json}";
            try {
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException for invalid JSON payload");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_CREDENTIAL_REQUEST, error.getError());
                assertTrue(error.getErrorDescription().contains("Invalid JSON payload"));
            }
        });
    }

    @Test
    public void testRequestCredentialWithNullPayload() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            // Null payload
            try {
                issuerEndpoint.requestCredential(null);
                Assert.fail("Expected BadRequestException for null payload");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_CREDENTIAL_REQUEST, error.getError());
                assertTrue(error.getErrorDescription().contains("Invalid JSON payload"));
            }
        });
    }

    @Test
    public void testRequestCredentialWithEmptyPayload() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            // Empty payload
            try {
                issuerEndpoint.requestCredential("");
                Assert.fail("Expected BadRequestException for empty payload");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_CREDENTIAL_REQUEST, error.getError());
                assertTrue(error.getErrorDescription().contains("Invalid JSON payload"));
            }
        });
    }

    @Test
    public void testRequestCredentialWithEncryptionRequiredButMissing() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            RealmModel realm = session.getContext().getRealm();
            realm.setAttribute("oid4vci.encryption.required", "true");

            try {
                AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                CredentialRequest credentialRequest = new CredentialRequest()
                        .setFormat(Format.JWT_VC)
                        .setCredentialIdentifier("test-credential");

                String requestPayload;
                try {
                    requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
                } catch (JsonProcessingException e) {
                    Assert.fail("Failed to serialize CredentialRequest: " + e.getMessage());
                    return;
                }

                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException due to missing encryption when required");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertEquals("Encryption is required by the Credential Issuer, but the request is not a JWE.",
                        error.getErrorDescription());
            } finally {
                realm.removeAttribute("oid4vci.encryption.required");
            }
        });
    }

    @Test
    public void testRequestCredentialWithValidJsonPayload() {
        final String scopeName = jwtTypeCredentialClientScope.getName();
        String token = getBearerToken(oauth, client, scopeName);
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CredentialRequest credentialRequest = new CredentialRequest()
                    .setFormat(Format.JWT_VC)
                    .setCredentialIdentifier(scopeName);

            String requestPayload;
            try {
                requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
            } catch (JsonProcessingException e) {
                Assert.fail("Failed to serialize CredentialRequest: " + e.getMessage());
                return;
            }

            Response credentialResponse = issuerEndpoint.requestCredential(requestPayload);
            assertEquals("The credential request should be answered successfully.",
                    HttpStatus.SC_OK, credentialResponse.getStatus());
            assertEquals("Response should be JSON type for unencrypted responses",
                    MediaType.APPLICATION_JSON, credentialResponse.getMediaType().toString());

            CredentialResponse credentialResponseVO = JsonSerialization.mapper
                    .convertValue(credentialResponse.getEntity(), CredentialResponse.class);
            JsonWebToken jsonWebToken;
            try {
                jsonWebToken = TokenVerifier.create((String) credentialResponseVO.getCredentials().get(0).getCredential(), JsonWebToken.class).getToken();
            } catch (VerificationException e) {
                Assert.fail("Failed to verify JWT: " + e.getMessage());
                return;
            }
            assertNotNull("A valid credential string should have been responded", jsonWebToken);
            VerifiableCredential credential = JsonSerialization.mapper.convertValue(
                    jsonWebToken.getOtherClaims().get("vc"), VerifiableCredential.class);
            assertTrue("The static claim should be set.", credential.getCredentialSubject().getClaims().containsKey("scope-name"));
        });
    }

    // Existing response encryption tests (updated for String payload)
    @Test
    public void testRequestCredentialWithIncompleteEncryptionParams() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            JWK jwk = JWKParser.create().parse("{\"kty\":\"RSA\",\"n\":\"test-n\",\"e\":\"AQAB\"}").getJwk();
            CredentialRequest credentialRequest = new CredentialRequest()
                    .setFormat(Format.JWT_VC)
                    .setCredentialIdentifier("test-credential")
                    .setCredentialResponseEncryption(
                            new CredentialResponseEncryption()
                                    .setJwk(jwk));

            String requestPayload;
            try {
                requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
            } catch (JsonProcessingException e) {
                Assert.fail("Failed to serialize CredentialRequest: " + e.getMessage());
                return;
            }

            try {
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException due to missing encryption parameter 'enc'");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue("Error message should specify missing parameters",
                        error.getErrorDescription().contains("Missing required encryption parameters: enc"));
            }
        });
    }

    @Test
    public void testCredentialIssuanceWithEncryption() throws Exception {
        testCredentialIssuanceWithAuthZCodeFlow(jwtTypeCredentialClientScope,
                (testClientId, testScope) -> {
                    String scopeName = jwtTypeCredentialClientScope.getName();
                    return getBearerToken(oauth.clientId(testClientId).openid(false).scope(scopeName));
                },
                m -> {
                    String accessToken = (String) m.get("accessToken");
                    WebTarget credentialTarget = (WebTarget) m.get("credentialTarget");
                    CredentialRequest credentialRequest = (CredentialRequest) m.get("credentialRequest");

                    Map<String, Object> jwkPair;
                    try {
                        jwkPair = generateRsaJwkWithPrivateKey();
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException("Failed to generate JWK", e);
                    }
                    JWK jwk = (JWK) jwkPair.get("jwk");
                    PrivateKey privateKey = (PrivateKey) jwkPair.get("privateKey");

                    credentialRequest.setCredentialResponseEncryption(
                            new CredentialResponseEncryption()
                                    .setEnc("A256GCM")
                                    .setJwk(jwk));

                    String requestPayload;
                    try {
                        requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
                    } catch (IOException e) {
                        Assert.fail("Failed to serialize CredentialRequest: " + e.getMessage());
                        return;
                    }

                    try (Response response = credentialTarget.request()
                            .header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken)
                            .post(Entity.json(requestPayload))) {

                        assertEquals(200, response.getStatus());
                        assertEquals("application/jwt", response.getMediaType().toString());

                        String encryptedResponse = response.readEntity(String.class);
                        CredentialResponse decryptedResponse;
                        try {
                            decryptedResponse = decryptJweResponse(encryptedResponse, privateKey);
                        } catch (IOException | JWEException e) {
                            Assert.fail("Failed to decrypt JWE response: " + e.getMessage());
                            return;
                        }

                        JsonWebToken jsonWebToken;
                        try {
                            jsonWebToken = TokenVerifier.create(
                                    (String) decryptedResponse.getCredentials().get(0).getCredential(),
                                    JsonWebToken.class
                            ).getToken();
                        } catch (VerificationException e) {
                            Assert.fail("Failed to verify JWT: " + e.getMessage());
                            return;
                        }

                        assertEquals("did:web:test.org", jsonWebToken.getIssuer());
                        VerifiableCredential credential = JsonSerialization.mapper.convertValue(
                                jsonWebToken.getOtherClaims().get("vc"),
                                VerifiableCredential.class
                        );
                        assertEquals(List.of(jwtTypeCredentialClientScope.getName()), credential.getType());
                        assertEquals(TEST_DID, credential.getIssuer());
                        assertEquals("john@email.cz", credential.getCredentialSubject().getClaims().get("email"));
                    }
                });
    }

    @Test
    public void testRequestCredentialWithUnsupportedEncryption() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            JWK jwk;
            try {
                jwk = generateRsaJwk();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to generate JWK", e);
            }

            CredentialRequest credentialRequest = new CredentialRequest()
                    .setFormat(Format.JWT_VC)
                    .setCredentialIdentifier("test-credential")
                    .setCredentialResponseEncryption(
                            new CredentialResponseEncryption()
                                    .setEnc("UNSUPPORTED-ENC")
                                    .setJwk(jwk));

            String requestPayload;
            try {
                requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
            } catch (JsonProcessingException e) {
                Assert.fail("Failed to serialize CredentialRequest: " + e.getMessage());
                return;
            }

            try {
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException due to unsupported encryption algorithm");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue(error.getErrorDescription().contains("UNSUPPORTED-ENC"));
            }
        });
    }

    @Test
    public void testRequestCredentialWithUnsupportedCompression() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            JWK jwk;
            try {
                jwk = generateRsaJwk();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to generate JWK", e);
            }

            CredentialRequest credentialRequest = new CredentialRequest()
                    .setFormat(Format.JWT_VC)
                    .setCredentialIdentifier("test-credential")
                    .setCredentialResponseEncryption(
                            new CredentialResponseEncryption()
                                    .setEnc("A256GCM")
                                    .setZip("UNSUPPORTED-ZIP")
                                    .setJwk(jwk));

            String requestPayload;
            try {
                requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
            } catch (JsonProcessingException e) {
                Assert.fail("Failed to serialize CredentialRequest: " + e.getMessage());
                return;
            }

            try {
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException due to unsupported compression algorithm");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue(error.getErrorDescription().contains("UNSUPPORTED-ZIP"));
            }
        });
    }

    @Test
    public void testRequestCredentialWithInvalidJWK() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            JWK jwk = JWKParser.create().parse("{\"kty\":\"RSA\",\"e\":\"AQAB\"}").getJwk();
            CredentialRequest credentialRequest = new CredentialRequest()
                    .setFormat(Format.JWT_VC)
                    .setCredentialIdentifier("test-credential")
                    .setCredentialResponseEncryption(
                            new CredentialResponseEncryption()
                                    .setEnc("A256GCM")
                                    .setJwk(jwk));

            String requestPayload;
            requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

            try {
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException due to invalid JWK missing modulus");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue(error.getErrorDescription().contains("JWK"));
            }
        });
    }

    @Test
    public void testRequestCredentialWithWrongKeyTypeJWK() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            JWK jwk = JWKParser.create().parse("{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"test-x\",\"y\":\"test-y\"}").getJwk();
            CredentialRequest credentialRequest = new CredentialRequest()
                    .setFormat(Format.JWT_VC)
                    .setCredentialIdentifier("test-credential")
                    .setCredentialResponseEncryption(
                            new CredentialResponseEncryption()
                                    .setEnc("A256GCM")
                                    .setJwk(jwk));

            String requestPayload;
            try {
                requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
            } catch (JsonProcessingException e) {
                Assert.fail("Failed to serialize CredentialRequest: " + e.getMessage());
                return;
            }

            try {
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException due to wrong JWK key type");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue(error.getErrorDescription().contains("JWK"));
            }
        });
    }
}
