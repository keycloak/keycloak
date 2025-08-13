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
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;
import org.junit.Test;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.models.KeyManager;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialResponseEncryption;
import org.keycloak.protocol.oid4vc.model.ErrorResponse;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.testsuite.Assert;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.MediaType;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.jose.jwe.JWEConstants.A256GCM;
import static org.keycloak.protocol.oid4vc.model.ErrorType.INVALID_ENCRYPTION_PARAMETERS;
import static org.keycloak.utils.MediaType.APPLICATION_JWT;

/**
 * Test class for Credential Request and Response Encryption as per spec
 *
 * @author Bertrand Ogen
 */
public class OID4VCIssuerEndpointEncryptionTest extends OID4VCIssuerEndpointTest {

    private static final Logger LOGGER = Logger.getLogger(OID4VCIssuerEndpointEncryptionTest.class);

    @Test
    public void testRequestCredentialWithEncryption() {
        final String scopeName = jwtTypeCredentialClientScope.getName();
        String token = getBearerToken(oauth, client, scopeName);
        testingClient
                .server(TEST_REALM_NAME)
                .run((session -> {
                    AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                    authenticator.setTokenString(token);
                    OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                    Map<String, Object> jwkPair;
                    try {
                        jwkPair = generateRsaJwkWithPrivateKey();
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException("Failed to generate JWK", e);
                    }
                    JWK jwk = (JWK) jwkPair.get("jwk");
                    PrivateKey privateKey = (PrivateKey) jwkPair.get("privateKey");

                    CredentialRequest credentialRequest = new CredentialRequest()
                            .setFormat(Format.JWT_VC)
                            .setCredentialIdentifier(scopeName)
                            .setCredentialResponseEncryption(
                                    new CredentialResponseEncryption()
                                            .setEnc(A256GCM)
                                            .setJwk(jwk));

                    String credentialRequestPayload;
                    credentialRequestPayload = JsonSerialization.writeValueAsString(credentialRequest);

                    Response credentialResponse = issuerEndpoint.requestCredential(credentialRequestPayload);

                    assertEquals("The credential request should be answered successfully.",
                            HttpStatus.SC_OK, credentialResponse.getStatus());
                    assertEquals("Response should be JWT type for encrypted responses",
                            APPLICATION_JWT, credentialResponse.getMediaType().toString());

                    String encryptedResponse = (String) credentialResponse.getEntity();
                    CredentialResponse decryptedResponse;
                    try {
                        decryptedResponse = decryptJweResponse(encryptedResponse, privateKey);
                    } catch (IOException | JWEException e) {
                        fail("Failed to decrypt JWE response: " + e.getMessage());
                        return;
                    }

                    // Verify the decrypted payload
                    assertNotNull("Decrypted response should contain a credential", decryptedResponse.getCredentials());
                    JsonWebToken jsonWebToken;
                    try {
                        jsonWebToken = TokenVerifier.create((String) decryptedResponse.getCredentials().get(0).getCredential(), JsonWebToken.class).getToken();
                    } catch (VerificationException e) {
                        fail("Failed to verify JWT: " + e.getMessage());
                        return;
                    }
                    assertNotNull("A valid credential string should have been responded", jsonWebToken);
                    VerifiableCredential credential = JsonSerialization.mapper.convertValue(
                            jsonWebToken.getOtherClaims().get("vc"), VerifiableCredential.class);
                    assertTrue("The static claim should be set.", credential.getCredentialSubject().getClaims().containsKey("scope-name"));
                }));
    }

    @Test
    public void testUnencryptedRequestWhenEncryptionRequired() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            RealmModel realm = session.getContext().getRealm();
            realm.setAttribute("oid4vci.encryption.required", "true");
            realm.setAttribute("oid4vci.request.enc.algorithms", "A256GCM");

            try {
                AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                CredentialRequest credentialRequest = new CredentialRequest()
                        .setFormat(Format.JWT_VC)
                        .setCredentialConfigurationId(jwtTypeCredentialClientScope.getName());

                String requestPayload;
                try {
                    requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
                } catch (JsonProcessingException e) {
                    fail("Failed to serialize CredentialRequest: " + e.getMessage());
                    return;
                }

                try {
                    issuerEndpoint.requestCredential(requestPayload);
                    fail("Expected BadRequestException due to unencrypted request when encryption is required");
                } catch (BadRequestException e) {
                    ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                    assertEquals(INVALID_ENCRYPTION_PARAMETERS, error.getError());
                    assertEquals("Encryption is required by the Credential Issuer, but the request is not a JWE.", error.getErrorDescription());
                }
            } finally {
                realm.removeAttribute("oid4vci.encryption.required");
                realm.removeAttribute("oid4vci.request.enc.algorithms");
            }
        });
    }

    @Test
    public void testMediaTypeHandling() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            RealmModel realm = session.getContext().getRealm();
            realm.setAttribute("oid4vci.encryption.required", "false");
            realm.setAttribute("oid4vci.request.enc.algorithms", "A256GCM");
            realm.setAttribute("oid4vci.request.zip.algorithms", "DEF");

            try {
                AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                // Test 1: Unencrypted request with application/json
                CredentialRequest unencryptedRequest = new CredentialRequest()
                        .setFormat(Format.JWT_VC)
                        .setCredentialConfigurationId(jwtTypeCredentialClientScope.getName());

                String unencryptedPayload = JsonSerialization.writeValueAsString(unencryptedRequest);
                Response unencryptedResponse = issuerEndpoint.requestCredential(unencryptedPayload);
                assertEquals("Unencrypted request should be processed successfully", 200, unencryptedResponse.getStatus());
                assertEquals("Unencrypted response should be application/json", MediaType.APPLICATION_JSON, unencryptedResponse.getMediaType().toString());
                CredentialResponse unencryptedCredentialResponse = (CredentialResponse) unencryptedResponse.getEntity();
                assertNotNull("Response should contain credentials", unencryptedCredentialResponse.getCredentials());
                assertFalse("Credentials should not be empty", unencryptedCredentialResponse.getCredentials().isEmpty());

                // Test 2: Encrypted request with application/jwt
                KeyManager keyManager = session.keys();
                KeyWrapper encryptionKey = keyManager.getKeysStream(realm)
                        .filter(key -> KeyUse.ENC.equals(key.getUse()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No encryption key found"));

                String encryptedPayload = createEncryptedCredentialRequest(unencryptedPayload, encryptionKey);
                assertNotNull("Encrypted request should not be null", encryptedPayload);

                session.getContext().getHttpRequest().getHttpHeaders().getRequestHeaders()
                        .putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JWT);

                Response encryptedResponse = issuerEndpoint.requestCredential(encryptedPayload);
                assertEquals("Encrypted request should be processed successfully", 200, encryptedResponse.getStatus());
                assertEquals("Encrypted response should be application/jwt", MediaType.APPLICATION_JWT, encryptedResponse.getMediaType().toString());

                // Decrypt and verify response
                CredentialResponse encryptedCredentialResponse = decryptJweResponse((String) encryptedResponse.getEntity(), (PrivateKey) encryptionKey.getPrivateKey());
                assertNotNull("Response should contain credentials", encryptedCredentialResponse.getCredentials());
                assertFalse("Credentials should not be empty", encryptedCredentialResponse.getCredentials().isEmpty());

            } catch (Exception e) {
                LOGGER.error("Test failed", e);
                throw new RuntimeException(e);
            } finally {
                realm.removeAttribute("oid4vci.encryption.required");
                realm.removeAttribute("oid4vci.request.enc.algorithms");
                realm.removeAttribute("oid4vci.request.zip.algorithms");
            }
        });
    }

    @Test
    public void testEncryptedCredentialRequest() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            try {
                // Enable request encryption requirement
                RealmModel realm = session.getContext().getRealm();
                realm.setAttribute("oid4vci.encryption.required", "true");
                realm.setAttribute("oid4vci.request.enc.algorithms", "A256GCM");

                AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                CredentialRequest credentialRequest = new CredentialRequest()
                        .setCredentialConfigurationId(jwtTypeCredentialClientScope.getName());
                LOGGER.debugf("Credential request: %s", credentialRequest);

                String requestJson = JsonSerialization.writeValueAsString(credentialRequest);
                LOGGER.debugf("Credential request JSON: %s", requestJson);

                KeyManager keyManager = session.keys();
                KeyWrapper encryptionKey = keyManager.getKeysStream(realm)
                        .filter(key -> KeyUse.ENC.equals(key.getUse()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No encryption key found"));
                assertNotNull("No encryption key found", encryptionKey);
                LOGGER.debugf("Found encryption key: kid=%s, alg=%s", encryptionKey.getKid(), encryptionKey.getAlgorithm());

                // Encrypt the request as JWE
                String encryptedRequest;
                try {
                    encryptedRequest = createEncryptedCredentialRequest(requestJson, encryptionKey);
                    assertNotNull("Encrypted request should not be null", encryptedRequest);
                    LOGGER.debugf("Encrypted credential request: %s", encryptedRequest);
                } catch (Exception e) {
                    fail("Failed to create encrypted request: " + e.getMessage());
                    return;
                }

                // Set Content-Type to application/jwt
                session.getContext().getHttpRequest().getHttpHeaders().getRequestHeaders()
                        .putSingle(HttpHeaders.CONTENT_TYPE, APPLICATION_JWT);
                LOGGER.debugf("Content-Type set to: %s", APPLICATION_JWT);

                // Test with encrypted request
                Response response;
                try {
                    response = issuerEndpoint.requestCredential(encryptedRequest);
                } catch (BadRequestException e) {
                    ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                    fail("Unexpected BadRequestException: error=" + error.getError() + ", description=" + error.getErrorDescription());
                    return;
                } catch (Exception e) {
                    fail("Unexpected exception during request: " + e.getClass().getName() + ": " + e.getMessage());
                    return;
                }

                assertEquals("Encrypted request should be processed successfully",
                        200, response.getStatus());
                LOGGER.debugf("Response status: %d, entity: %s", response.getStatus(), response.getEntity());

                // Verify response
                CredentialResponse credentialResponse = (CredentialResponse) response.getEntity();
                LOGGER.debugf("Credential response: %s", credentialResponse);
                assertNotNull("Response should contain credentials", credentialResponse.getCredentials());
                assertFalse("Credentials should not be empty", credentialResponse.getCredentials().isEmpty());

            } catch (Exception e) {
                fail("Test failed with exception: " + e.getClass().getName() + ": " + e.getMessage());
            }
        });
    }

    @Test
    public void testEncryptedCredentialRequestWithCompression() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            try {
                // Enable request encryption and compression
                RealmModel realm = session.getContext().getRealm();
                realm.setAttribute("oid4vci.encryption.required", "true");
                realm.setAttribute("oid4vci.request.enc.algorithms", "A256GCM");
                realm.setAttribute("oid4vci.request.zip.algorithms", "DEF");

                AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                // Create credential request
                CredentialRequest credentialRequest = new CredentialRequest()
                        .setCredentialConfigurationId(jwtTypeCredentialClientScope.getName());

                String requestJson = JsonSerialization.writeValueAsString(credentialRequest);

                KeyManager keyManager = session.keys();
                KeyWrapper encryptionKey = keyManager.getKeysStream(realm)
                        .filter(key -> KeyUse.ENC.equals(key.getUse()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No encryption key found"));

                // Encrypt the request with compression
                String encryptedRequest = createEncryptedCredentialRequestWithCompression(requestJson, encryptionKey);

                // Test with encrypted and compressed request
                Response response = issuerEndpoint.requestCredential(encryptedRequest);
                assertEquals("Encrypted compressed request should be processed successfully",
                        200, response.getStatus());

                // Verify response
                CredentialResponse credentialResponse = (CredentialResponse) response.getEntity();
                assertNotNull("Response should contain credentials", credentialResponse.getCredentials());
                assertFalse("Credentials should not be empty", credentialResponse.getCredentials().isEmpty());

            } catch (Exception e) {
                fail("Test failed with exception: " + e.getMessage());
            }
        });
    }

    @Test
    public void testInvalidEncryptionAlgorithm() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            try {
                RealmModel realm = session.getContext().getRealm();
                realm.setAttribute("oid4vci.request.enc.algorithms", "A256GCM");

                AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                // Create credential request
                CredentialRequest credentialRequest = new CredentialRequest()
                        .setCredentialConfigurationId(jwtTypeCredentialClientScope.getName());

                String requestJson = JsonSerialization.writeValueAsString(credentialRequest);

                KeyManager keyManager = session.keys();
                KeyWrapper encryptionKey = keyManager.getKeysStream(realm)
                        .filter(key -> KeyUse.ENC.equals(key.getUse()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No encryption key found"));

                // Create JWE with unsupported encryption algorithm
                String encryptedRequest = createEncryptedCredentialRequestWithInvalidAlg(requestJson, encryptionKey);

                // Set Content-Type to application/jwt
                session.getContext().getHttpRequest().getHttpHeaders().getRequestHeaders()
                        .putSingle(HttpHeaders.CONTENT_TYPE, APPLICATION_JWT);

                // Test with invalid encryption algorithm
                try {
                    Response response = issuerEndpoint.requestCredential(encryptedRequest);
                    fail("Expected BadRequestException for invalid enc");
                    assertEquals("Unexpected response status: " + response.getStatus(), 400, response.getStatus());
                } catch (BadRequestException e) {
                    ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                    assertEquals(INVALID_ENCRYPTION_PARAMETERS, error.getError());
                    assertTrue("Error should mention unsupported algorithm",
                            error.getErrorDescription().contains("Unsupported enc algorithm"));
                }

            } catch (Exception e) {
                if (!(e instanceof BadRequestException)) {
                    fail("Unexpected exception: " + e.getMessage());
                }
            }
        });
    }

    @Test
    public void testRequestCredentialWithIncompleteEncryptionParams() throws Throwable {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            // Missing enc parameter
            JWK jwk = JWKParser.create().parse("{\"kty\":\"RSA\",\"n\":\"test-n\",\"e\":\"AQAB\"}").getJwk();
            CredentialRequest credentialRequest = new CredentialRequest()
                    .setFormat(Format.JWT_VC)
                    .setCredentialIdentifier("test-credential")
                    .setCredentialResponseEncryption(
                            new CredentialResponseEncryption()
                                    .setJwk(jwk));

            String credentialRequestPayload;
            credentialRequestPayload = JsonSerialization.writeValueAsString(credentialRequest);

            try {
                issuerEndpoint.requestCredential(credentialRequestPayload);
                Assert.fail("Expected BadRequestException due to missing encryption parameter 'zip'  and 'enc'");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue("Error message should specify missing parameters",
                        error.getErrorDescription().contains("Missing required encryption parameters: zip, enc"));
            }
        });
    }

    @Test
    public void testCredentialIssuanceWithEncryption() throws Exception {
        // Integration test for the full credential issuance flow with encryption
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
                                    .setEnc(A256GCM)
                                    .setJwk(jwk));

                    try (Response response = credentialTarget.request()
                            .header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken)
                            .post(Entity.json(credentialRequest))) {

                        assertEquals(200, response.getStatus());
                        assertEquals("application/jwt", response.getMediaType().toString());

                        String encryptedResponse = response.readEntity(String.class);
                        CredentialResponse decryptedResponse;
                        try {
                            decryptedResponse = decryptJweResponse(encryptedResponse, privateKey);
                        } catch (IOException | JWEException e) {
                            fail("Failed to decrypt JWE response: " + e.getMessage());
                            return;
                        }

                        // Verify the decrypted payload
                        JsonWebToken jsonWebToken;
                        try {
                            jsonWebToken = TokenVerifier.create(
                                    (String) decryptedResponse.getCredentials().get(0).getCredential(),
                                    JsonWebToken.class
                            ).getToken();
                        } catch (VerificationException e) {
                            fail("Failed to verify JWT: " + e.getMessage());
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
    public void testRequestCredentialWithUnsupportedResponseEncryption() {
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
                                    .setEnc("A128GCM")
                                    .setJwk(jwk));

            String requestPayload;
            try {
                requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
            } catch (JsonProcessingException e) {
                fail("Failed to serialize CredentialRequest: " + e.getMessage());
                return;
            }

             try {
                 issuerEndpoint.requestCredential(requestPayload);
                 fail("Expected BadRequestException due to unsupported encryption algorithm");
             } catch (BadRequestException e) {
                 ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                 assertEquals(INVALID_ENCRYPTION_PARAMETERS, error.getError());
                 assertTrue(error.getErrorDescription().contains("Unsupported content encryption algorithm"));
             }
        });
    }

    @Test
    public void testRequestCredentialWithUnsupportedResponseCompression() {
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
                fail("Failed to serialize CredentialRequest: " + e.getMessage());
                return;
            }

            try {
                issuerEndpoint.requestCredential(requestPayload);
                fail("Expected BadRequestException due to unsupported compression algorithm");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(INVALID_ENCRYPTION_PARAMETERS, error.getError());
            }
        });
    }

    @Test
    public void testRequestCredentialWithInvalidJWK() throws Throwable {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            // Invalid JWK (missing modulus)
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
                assertEquals(INVALID_ENCRYPTION_PARAMETERS, error.getError());
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
                fail("Failed to serialize CredentialRequest: " + e.getMessage());
                return;
            }

            try {
                issuerEndpoint.requestCredential(requestPayload);
                fail("Expected BadRequestException due to wrong JWK key type");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue(error.getErrorDescription().contains("JWK"));
            }
        });
    }

    @Test
    public void testRequestCredentialEncryptionRequiredButMissing() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            RealmModel realm = session.getContext().getRealm();
            realm.setAttribute("oid4vci.encryption.required", "true");
            realm.setAttribute("oid4vci.request.enc.algorithms", "A256GCM");
            realm.setAttribute("oid4vci.request.zip.algorithms", "DEF");

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
                    fail("Failed to serialize CredentialRequest: " + e.getMessage());
                    return;
                }

                try {
                    issuerEndpoint.requestCredential(requestPayload);
                    Assert.fail("Expected BadRequestException due to missing encryption when required");
                } catch (BadRequestException e) {
                    ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                    assertEquals(INVALID_ENCRYPTION_PARAMETERS, error.getError());
                    assertEquals("Encryption is required by the Credential Issuer, but the request is not a JWE.", error.getErrorDescription());
                }
            } finally {
                // Clean up realm attributes
                realm.removeAttribute("oid4vci.encryption.required");
                realm.removeAttribute("oid4vci.request.enc.algorithms");
                realm.removeAttribute("oid4vci.request.zip.algorithms");
            }
        });
    }

    @Test
    public void testRequestCredentialWithMissingResponseEncryptionWhenRequired() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        testingClient.server(TEST_REALM_NAME).run(session -> {
            RealmModel realm = session.getContext().getRealm();
            realm.setAttribute("credential_response_encryption.encryption_required", "true");

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
                    fail("Failed to serialize CredentialRequest: " + e.getMessage());
                    return;
                }

                try {
                    issuerEndpoint.requestCredential(requestPayload);
                    fail("Expected BadRequestException due to missing response encryption when required");
                } catch (BadRequestException e) {
                    ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                    assertEquals(INVALID_ENCRYPTION_PARAMETERS, error.getError());
                    assertEquals("Encryption is required by the Credential Issuer, but the request is not a JWE.", error.getErrorDescription());
                }
            } finally {
                realm.removeAttribute("credential_response_encryption.encryption_required");
            }
        });
    }
}

