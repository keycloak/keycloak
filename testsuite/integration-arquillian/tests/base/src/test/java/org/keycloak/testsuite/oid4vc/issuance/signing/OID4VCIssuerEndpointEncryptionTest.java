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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

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
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.testsuite.Assert;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.MediaType;

import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;
import org.junit.Test;

import static org.keycloak.jose.jwe.JWEConstants.A256GCM;
import static org.keycloak.protocol.oid4vc.model.ErrorType.INVALID_ENCRYPTION_PARAMETERS;
import static org.keycloak.utils.MediaType.APPLICATION_JWT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class for Credential Request and Response Encryption
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
                            .setCredentialIdentifier(scopeName)
                            .setCredentialResponseEncryption(
                                    new CredentialResponseEncryption()
                                            .setEnc(A256GCM)
                                            .setJwk(jwk));

                    String credentialRequestPayload = JsonSerialization.writeValueAsString(credentialRequest);

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
        String token = getBearerToken(oauth, client);
        testingClient.server(TEST_REALM_NAME).run(session -> {
            RealmModel realm = session.getContext().getRealm();
            realm.setAttribute("oid4vci.encryption.required", "true");
            realm.setAttribute("oid4vci.request.enc.algorithms", "A256GCM");

            try {
                AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                CredentialRequest credentialRequest = new CredentialRequest()
                        .setCredentialIdentifier("test-credential");

                String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

                try {
                    issuerEndpoint.requestCredential(requestPayload);
                    fail("Expected BadRequestException due to unencrypted request when encryption is required");
                } catch (BadRequestException e) {
                    ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                    assertEquals(INVALID_ENCRYPTION_PARAMETERS, error.getError());
                    assertEquals("Encryption is required but request is not a valid JWE: Not a JWE String", error.getErrorDescription());
                }
            } finally {
                realm.removeAttribute("oid4vci.encryption.required");
                realm.removeAttribute("oid4vci.request.enc.algorithms");
            }
        });
    }

    @Test
    public void testEncryptedCredentialRequest() {
        final String scopeName = jwtTypeCredentialClientScope.getName();
        String token = getBearerToken(oauth, client, scopeName);
        testingClient.server(TEST_REALM_NAME).run(session -> {
            try {
                // Enable request encryption requirement
                RealmModel realm = session.getContext().getRealm();
                realm.setAttribute("oid4vci.encryption.required", "true");
                realm.setAttribute("oid4vci.request.enc.algorithms", "A256GCM");

                AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                // Generate keys for request encryption
                KeyManager keyManager = session.keys();
                KeyWrapper encryptionKey = keyManager.getKeysStream(realm)
                        .filter(key -> KeyUse.ENC.equals(key.getUse()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No encryption key found"));

                // Generate keys for response encryption
                Map<String, Object> jwkPair = generateRsaJwkWithPrivateKey();
                JWK responseJwk = (JWK) jwkPair.get("jwk");
                PrivateKey responsePrivateKey = (PrivateKey) jwkPair.get("privateKey");

                CredentialRequest credentialRequest = new CredentialRequest()
                        .setCredentialIdentifier(scopeName)
                        .setCredentialResponseEncryption(
                                new CredentialResponseEncryption()
                                        .setEnc(A256GCM)
                                        .setJwk(responseJwk));

                String requestJson = JsonSerialization.writeValueAsString(credentialRequest);
                String encryptedRequest = createEncryptedCredentialRequest(requestJson, encryptionKey);

                Response response = issuerEndpoint.requestCredential(encryptedRequest);

                assertEquals("Encrypted request should be processed successfully",
                        200, response.getStatus());
                assertEquals("Response should be JWT type for encrypted responses",
                        APPLICATION_JWT, response.getMediaType().toString());

                // Decrypt and verify response
                String encryptedResponse = (String) response.getEntity();
                CredentialResponse decryptedResponse = decryptJweResponse(encryptedResponse, responsePrivateKey);

                assertNotNull("Decrypted response should contain a credential", decryptedResponse.getCredentials());
                JsonWebToken jsonWebToken = TokenVerifier.create(
                        (String) decryptedResponse.getCredentials().get(0).getCredential(),
                        JsonWebToken.class).getToken();
                assertNotNull("A valid credential string should have been responded", jsonWebToken);
                VerifiableCredential credential = JsonSerialization.mapper.convertValue(
                        jsonWebToken.getOtherClaims().get("vc"),
                        VerifiableCredential.class);
                assertTrue("The static claim should be set.",
                        credential.getCredentialSubject().getClaims().containsKey("scope-name"));

            } catch (Exception e) {
                fail("Test failed with exception: " + e.getClass().getName() + ": " + e.getMessage());
            }
        });
    }

    @Test
    public void testEncryptedCredentialRequestWithCompression() {
        final String scopeName = jwtTypeCredentialClientScope.getName();
        String token = getBearerToken(oauth, client, scopeName);
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


                // Generate keys for request encryption
                KeyManager keyManager = session.keys();
                KeyWrapper encryptionKey = keyManager.getKeysStream(realm)
                        .filter(key -> KeyUse.ENC.equals(key.getUse()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No encryption key found"));

                // Generate keys for response encryption
                Map<String, Object> jwkPair = generateRsaJwkWithPrivateKey();
                JWK responseJwk = (JWK) jwkPair.get("jwk");
                PrivateKey responsePrivateKey = (PrivateKey) jwkPair.get("privateKey");

                // Create credential request with response encryption parameters
                CredentialRequest credentialRequest = new CredentialRequest()
                        .setCredentialIdentifier(scopeName)
                        .setCredentialResponseEncryption(
                                new CredentialResponseEncryption()
                                        .setEnc(A256GCM)
                                        .setJwk(responseJwk));

                String requestJson = JsonSerialization.writeValueAsString(credentialRequest);

                // Encrypt the request with compression
                String encryptedRequest = createEncryptedCredentialRequestWithCompression(requestJson, encryptionKey);

                // Test with encrypted and compressed request
                Response response = issuerEndpoint.requestCredential(encryptedRequest);

                // Verify response
                assertEquals("Encrypted compressed request should be processed successfully",
                        200, response.getStatus());
                assertEquals("Response should be JWT type for encrypted responses",
                        MediaType.APPLICATION_JWT, response.getMediaType().toString());

                // Decrypt and verify the response
                String encryptedResponse = (String) response.getEntity();
                CredentialResponse decryptedResponse = decryptJweResponse(encryptedResponse, responsePrivateKey);

                assertNotNull("Decrypted response should contain a credential", decryptedResponse.getCredentials());
                JsonWebToken jsonWebToken = TokenVerifier.create(
                        (String) decryptedResponse.getCredentials().get(0).getCredential(),
                        JsonWebToken.class).getToken();
                assertNotNull("A valid credential string should have been responded", jsonWebToken);
                VerifiableCredential credential = JsonSerialization.mapper.convertValue(
                        jsonWebToken.getOtherClaims().get("vc"),
                        VerifiableCredential.class);
                assertTrue("The static claim should be set.",
                        credential.getCredentialSubject().getClaims().containsKey("scope-name"));

            } catch (Exception e) {
                LOGGER.error("Test failed", e);
                fail("Test failed with exception: " + e.getClass().getName() + ": " + e.getMessage());
            }
        });
    };

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
                    .setCredentialIdentifier("test-credential")
                    .setCredentialResponseEncryption(
                            new CredentialResponseEncryption()
                                    .setJwk(jwk));

            String credentialRequestPayload = JsonSerialization.writeValueAsString(credentialRequest);

            try {
                issuerEndpoint.requestCredential(credentialRequestPayload);
                Assert.fail("Expected BadRequestException due to missing encryption parameter 'enc'");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue("Error message should specify missing parameter 'enc'",
                        error.getErrorDescription().contains("Missing required parameters: enc"));
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
                    .setCredentialIdentifier("test-credential")
                    .setCredentialResponseEncryption(
                            new CredentialResponseEncryption()
                                    .setEnc("A128GCM")
                                    .setJwk(jwk));

            String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

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
                    .setCredentialIdentifier("test-credential")
                    .setCredentialResponseEncryption(
                            new CredentialResponseEncryption()
                                    .setEnc("A256GCM")
                                    .setZip("UNSUPPORTED-ZIP")
                                    .setJwk(jwk));

            String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

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
        final String scopeName = jwtTypeCredentialClientScope.getName();
        String token = getBearerToken(oauth, client, scopeName);
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            // Invalid JWK (missing modulus but WITH alg parameter)
            JWK jwk = JWKParser.create().parse("{\"kty\":\"RSA\",\"alg\":\"RSA-OAEP-256\",\"e\":\"AQAB\"}").getJwk();
            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialIdentifier(scopeName)
                    .setCredentialResponseEncryption(
                            new CredentialResponseEncryption()
                                    .setEnc("A256GCM")
                                    .setJwk(jwk));

            String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

            try {
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException due to invalid JWK missing modulus");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue("Error should mention invalid JWK. Actual: " + error.getErrorDescription(),
                        error.getErrorDescription().contains("Invalid JWK"));
            }
        });
    }

    @Test
    public void testRequestCredentialWithMissingResponseEncryptionWhenRequired() {
        String scopeName = jwtTypeCredentialClientScope.getName();
        String token = getBearerToken(oauth, client, scopeName);
        testingClient.server(TEST_REALM_NAME).run(session -> {
            RealmModel realm = session.getContext().getRealm();
            realm.setAttribute("oid4vci.encryption.required", "true");

            try {
                AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                CredentialRequest credentialRequest = new CredentialRequest()
                        .setCredentialIdentifier(scopeName);

                String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

                try {
                    issuerEndpoint.requestCredential(requestPayload);
                    fail("Expected BadRequestException due to missing request encryption when required");
                } catch (BadRequestException e) {
                    ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                    assertEquals(INVALID_ENCRYPTION_PARAMETERS, error.getError());
                    assertEquals("Encryption is required but request is not a valid JWE: Not a JWE String", error.getErrorDescription());
                }
            } finally {
                realm.removeAttribute("oid4vci.encryption.required");
            }
        });
    }
}
