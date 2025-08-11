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
import org.junit.Before;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.DeflaterOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.jose.jwe.JWEConstants.A256GCM;

/**
 * Test class for Credential Request and Response Encryption as per spec
 *
 * @author Bertrand Ogen
 */
public class OID4VCIssuerEndpointEncryptionTest extends OID4VCIssuerEndpointTest {

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
                                            .setEnc("A256GCM")
                                            .setJwk(jwk));

                    String credentialRequestPayload;
                    credentialRequestPayload = JsonSerialization.writeValueAsString(credentialRequest);

                    Response credentialResponse = issuerEndpoint.requestCredential(credentialRequestPayload);

                    assertEquals("The credential request should be answered successfully.",
                            HttpStatus.SC_OK, credentialResponse.getStatus());
                    assertEquals("Response should be JWT type for encrypted responses",
                            org.keycloak.utils.MediaType.APPLICATION_JWT, credentialResponse.getMediaType().toString());

                    String encryptedResponse = (String) credentialResponse.getEntity();
                    CredentialResponse decryptedResponse;
                    try {
                        decryptedResponse = decryptJweResponse(encryptedResponse, privateKey);
                    } catch (IOException | JWEException e) {
                        Assert.fail("Failed to decrypt JWE response: " + e.getMessage());
                        return;
                    }

                    // Verify the decrypted payload
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
                }));
    }

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
                Assert.fail("Expected BadRequestException due to missing encryption parameters 'alg, enc'");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue("Error message should specify missing parameters",
                        error.getErrorDescription().contains("Missing required encryption parameters: alg, enc"));
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
                            Assert.fail("Failed to decrypt JWE response: " + e.getMessage());
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
                Assert.fail("Failed to serialize CredentialRequest: " + e.getMessage());
                return;
            }

            try {
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException due to unsupported encryption algorithm");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue(error.getErrorDescription().contains("Unsupported encryption parameters: alg=RSA-OAEP-256, enc=A128GCM"));
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
                Assert.fail("Failed to serialize CredentialRequest: " + e.getMessage());
                return;
            }

            try {
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException due to unsupported compression algorithm");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue(error.getErrorDescription().contains("Unsupported compression parameter: zip=UNSUPPORTED-ZIP"));
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
            try {
                requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
            } catch (JsonProcessingException e) {
                Assert.fail("Failed to serialize CredentialRequest: " + e.getMessage());
                return;
            }

            try {
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException due to invalid JWK missing modulus");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertTrue(error.getErrorDescription().contains("Failed to encrypt response"));
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
                assertTrue(error.getErrorDescription().contains("Failed to encrypt response"));
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
                    Assert.fail("Failed to serialize CredentialRequest: " + e.getMessage());
                    return;
                }

                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException due to missing response encryption when required");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_ENCRYPTION_PARAMETERS, error.getError());
                assertEquals("Response encryption is required by the Credential Issuer, but no encryption parameters were provided.",
                        error.getErrorDescription());
            } finally {
                realm.removeAttribute("credential_response_encryption.encryption_required");
            }
        });
    }
}

