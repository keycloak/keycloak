/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.oid4vc;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

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
import org.keycloak.VCFormat;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferState;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage;
import org.keycloak.protocol.oid4vc.issuance.keybinding.AttestationValidatorUtil;
import org.keycloak.protocol.oid4vc.model.Claim;
import org.keycloak.protocol.oid4vc.model.ClaimDisplay;
import org.keycloak.protocol.oid4vc.model.Claims;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.ErrorResponse;
import org.keycloak.protocol.oid4vc.model.ErrorType;
import org.keycloak.protocol.oid4vc.model.JwtProof;
import org.keycloak.protocol.oid4vc.model.KeyAttestationJwtBody;
import org.keycloak.protocol.oid4vc.model.NonceResponse;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCodeGrant;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.AppAuthManager.BearerTokenAuthenticator;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.CREDENTIAL_SUBJECT;
import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;
import static org.keycloak.OID4VCConstants.SDJWT_DELIMITER;
import static org.keycloak.tests.oid4vc.OID4VCProofTestUtils.generateJwtProof;
import static org.keycloak.tests.oid4vc.OID4VCProofTestUtils.generateJwtProofWithClaims;
import static org.keycloak.tests.oid4vc.OID4VCProofTestUtils.generateJwtProofWithKidNoAttestation;
import static org.keycloak.tests.oid4vc.OID4VCProofTestUtils.jwtProofs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCJWTIssuerEndpointTest extends OID4VCIssuerEndpointTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @AfterEach
    public void logout() {
        AccountHelper.logout(testRealm.admin(), "john");
    }

    @Test
    void testGetCredentialOfferUriUnsupportedCredential() {
        String token = getBearerToken(oauth);

        runOnServer.run(session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);

            OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CorsErrorResponseException exception = assertThrows(
                    CorsErrorResponseException.class,
                    () -> oid4VCIssuerEndpoint.createCredentialOffer("inexistent-id")
            );
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus(), "Should return BAD_REQUEST");
        });
    }

    @Test
    void testGetCredentialOfferUriUnauthorized() {
        runOnServer.run(session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString(null);
            OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CorsErrorResponseException exception = assertThrows(
                    CorsErrorResponseException.class,
                    () -> oid4VCIssuerEndpoint.createCredentialOffer("test-credential", true, "john")
            );
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus(), "Should return BAD_REQUEST");
        });
    }

    @Test
    void testGetCredentialOfferUriInvalidToken() {
        runOnServer.run(session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString("invalid-token");
            OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CorsErrorResponseException exception = assertThrows(
                    CorsErrorResponseException.class,
                    () -> oid4VCIssuerEndpoint.createCredentialOffer("test-credential", true, "john")
            );
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus(), "Should return BAD_REQUEST");
        });
    }

    @Test
    public void testGetCredentialOfferURI() {
        final String scopeName = jwtTypeCredentialScope.getName();
        final String credentialConfigurationId = jwtTypeCredentialScope.getAttributes()
                .get(CredentialScopeModel.VC_CONFIGURATION_ID);

        String token = getBearerToken(oauth, client, scopeName);

        runOnServer.run(session -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                Response response = oid4VCIssuerEndpoint.createCredentialOffer(credentialConfigurationId);

                assertEquals(HttpStatus.SC_OK, response.getStatus(), "An offer uri should have been returned.");

                CredentialOfferURI credentialOfferURI = JsonSerialization.mapper.convertValue(
                        response.getEntity(),
                        CredentialOfferURI.class
                );

                assertNotNull(credentialOfferURI.getNonce(), "A nonce should be included.");
                assertNotNull(credentialOfferURI.getIssuer(), "The issuer uri should be provided.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testGetCredentialOfferUnauthorized() {
        assertThrows(BadRequestException.class, () ->
                withCausePropagation(() -> runOnServer.run(session -> {
                    BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                    authenticator.setTokenString(null);
                    OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                    Response response = issuerEndpoint.getCredentialOffer("nonce");
                    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
                }))
        );
    }

    @Test
    public void testGetCredentialOfferWithoutNonce() {
        String token = getBearerToken(oauth);
        assertThrows(BadRequestException.class, () ->
                withCausePropagation(() -> runOnServer.run(session -> {
                    BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                    authenticator.setTokenString(token);
                    OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                    issuerEndpoint.getCredentialOffer(null);
                }))
        );
    }

    @Test
    public void testGetCredentialOfferWithoutAPreparedOffer() {
        String token = getBearerToken(oauth);
        assertThrows(BadRequestException.class, () ->
                withCausePropagation(() -> runOnServer.run(session -> {
                    BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                    authenticator.setTokenString(token);
                    OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                    issuerEndpoint.getCredentialOffer("unpreparedNonce");
                }))
        );
    }

    @Test
    public void testGetCredentialOfferWithABrokenNote() {
        String token = getBearerToken(oauth);
        assertThrows(BadRequestException.class, () ->
                withCausePropagation(() -> runOnServer.run(session -> {
                    BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                    authenticator.setTokenString(token);
                    String nonce = prepareSessionCode(session, authenticator, "invalidNote").key();
                    OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                    issuerEndpoint.getCredentialOffer(nonce);
                }))
        );
    }

    @Test
    public void testGetCredentialOffer() {
        String token = getBearerToken(oauth);

        runOnServer.run(session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);

            CredentialsOffer credOffer = new CredentialsOffer()
                    .setCredentialIssuer("the-issuer")
                    .addGrant(new PreAuthorizedCodeGrant().setPreAuthorizedCode("the-code"))
                    .setCredentialConfigurationIds(List.of("credential-configuration-id"));

            CredentialOfferStorage offerStorage = session.getProvider(CredentialOfferStorage.class);
            CredentialOfferState offerState = new CredentialOfferState(credOffer, null, null, Time.currentTime() + 60, null);
            offerStorage.putOfferState(offerState);

            // The cache transactions need to be committed explicitly in the test.
            // Without that, the OAuth2Code will only be committed to the cache after .run((session)-> ...)
            session.getTransactionManager().commit();

            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
            Response credentialOfferResponse = issuerEndpoint.getCredentialOffer(offerState.getNonce());

            assertEquals(HttpStatus.SC_OK, credentialOfferResponse.getStatus(), "The offer should have been returned.");
            Object credentialOfferEntity = credentialOfferResponse.getEntity();
            assertNotNull(credentialOfferEntity, "An actual offer should be in the response.");

            CredentialsOffer retrievedCredentialsOffer = JsonSerialization.mapper.convertValue(credentialOfferEntity, CredentialsOffer.class);
            assertEquals(credOffer, retrievedCredentialsOffer, "The offer should be the one prepared with for the session.");
        });
    }

    // ----- requestCredential

    @Test
    public void testRequestCredentialUnauthorized() {
        runOnServer.run(session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString(null);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialIdentifier("test-credential");

            String requestPayload;
            try {
                requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            CorsErrorResponseException exception = assertThrows(
                    CorsErrorResponseException.class,
                    () -> issuerEndpoint.requestCredential(requestPayload)
            );
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus(), "Should return BAD_REQUEST");
        });
    }

    @Test
    public void testRequestCredentialInvalidToken() {
        runOnServer.run(session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString("token");
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialIdentifier("test-credential");

            String requestPayload;
            try {
                requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            CorsErrorResponseException exception = assertThrows(
                    CorsErrorResponseException.class,
                    () -> issuerEndpoint.requestCredential(requestPayload)
            );
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus(), "Should return BAD_REQUEST");
        });
    }

    @Test
    public void testRequestCredentialNoMatchingCredentialBuilder() throws Throwable {
        final String credentialConfigurationId = jwtTypeCredentialScope.getAttributes()
                .get(CredentialScopeModel.VC_CONFIGURATION_ID);
        final String scopeName = jwtTypeCredentialScope.getName();

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credentialConfigurationId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);

        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);
        String cNonce = getCNonce();
        String credentialIssuerId = credentialIssuer.getCredentialIssuer();

        try {
            withCausePropagation(() -> runOnServer.run(session -> {
                try {
                    BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                    authenticator.setTokenString(token);

                    // Prepare the issue endpoint with no credential builders.
                    OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator, Map.of());
                    Proofs proofs = jwtProofs(credentialIssuerId, cNonce);

                    CredentialRequest credentialRequest = new CredentialRequest()
                            .setCredentialIdentifier(credentialIdentifier)
                            .setProofs(proofs);

                    String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
                    issuerEndpoint.requestCredential(requestPayload);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
            fail("Should have thrown an exception");
        } catch (Exception e) {
            assertInstanceOf(BadRequestException.class, e);
            assertEquals("No credential builder found for format jwt_vc_json", e.getMessage());
        }
    }

    @Test
    public void testRequestCredentialUnsupportedCredential() {
        String token = getBearerToken(oauth);

        assertThrows(BadRequestException.class, () ->
                withCausePropagation(() -> runOnServer.run(session -> {
                    try {
                        BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                        authenticator.setTokenString(token);

                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        CredentialRequest credentialRequest = new CredentialRequest()
                                .setCredentialIdentifier("no-such-credential");

                        String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
                        issuerEndpoint.requestCredential(requestPayload);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }))
        );
    }

    @Test
    public void testRequestCredential() {
        String scopeName = jwtTypeCredentialScope.getName();
        String credConfigId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);

        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();

        assertNotNull(authDetailsResponse, "authorization_details should be present in the response");
        assertFalse(authDetailsResponse.isEmpty(), "authorization_details should not be empty");

        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);
        assertNotNull(credentialIdentifier, "credential_identifier should be present");

        String cNonce = getCNonce();
        Proofs proofs = jwtProofs(credentialIssuer.getCredentialIssuer(), cNonce);

        CredentialResponse credentialResponseVO = oauth.oid4vc()
                .credentialRequest()
                .bearerToken(token)
                .credentialIdentifier(credentialIdentifier)
                .proofs(proofs)
                .send()
                .getCredentialResponse();

        JsonWebToken jsonWebToken;
        try {
            jsonWebToken = TokenVerifier.create((String) credentialResponseVO.getCredentials().get(0).getCredential(), JsonWebToken.class).getToken();
        } catch (VerificationException e) {
            fail("Failed to verify JWT: " + e.getMessage());
            return;
        }

        assertNotNull(jsonWebToken, "A valid credential string should have been responded");
        assertNotNull(jsonWebToken.getOtherClaims().get("vc"), "The credentials should be included at the vc-claim.");

        VerifiableCredential credential = JsonSerialization.mapper.convertValue(
                jsonWebToken.getOtherClaims().get("vc"),
                VerifiableCredential.class
        );

        assertTrue(credential.getCredentialSubject().getClaims().containsKey("scope-name"), "The static claim should be set.");
        assertEquals(scopeName, credential.getCredentialSubject().getClaims().get("scope-name"), "The static claim should be set.");
        assertFalse(credential.getCredentialSubject().getClaims().containsKey("AnotherCredentialType"), "Only mappers supported for the requested type should have been evaluated.");
    }

    @Test
    public void testRequestCredentialWithNeitherIdSet() {
        final String scopeName = minimalJwtTypeCredentialScope.getName();
        String token = getBearerToken(oauth, client, scopeName);

        runOnServer.run(session -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                CredentialRequest credentialRequest = new CredentialRequest();

                try {
                    String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
                    issuerEndpoint.requestCredential(requestPayload);
                    fail("Expected BadRequestException due to missing credential identifier or configuration id");
                } catch (BadRequestException e) {
                    ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                    assertEquals(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue(), error.getError());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testCredentialIssuance() {
        AccessTokenResponse accessTokenResponse = getBearerTokenCodeFlow(oauth, client, "john", jwtTypeCredentialScope.getName());
        assertEquals(HttpStatus.SC_OK, accessTokenResponse.getStatusCode());
        String token = accessTokenResponse.getAccessToken();
        assertNotNull(token, "Access token should be present");

        // Extract credential_identifier from authorization_details in token response
        List<OID4VCAuthorizationDetail> authDetailsResponse = accessTokenResponse.getOID4VCAuthorizationDetails();
        assertNotNull(authDetailsResponse, "authorization_details should be present in the response");
        assertFalse(authDetailsResponse.isEmpty(), "authorization_details should not be empty");

        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);
        assertNotNull(credentialIdentifier, "Credential identifier should be present");

        // 1. Retrieving the credential-offer-uri
        final String credentialConfigurationId = jwtTypeCredentialScope.getAttributes()
                .get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialOfferURI credOfferUri = oauth.oid4vc()
                .credentialOfferUriRequest(credentialConfigurationId)
                .preAuthorized(false)
                .targetUser("john")
                .bearerToken(token)
                .send()
                .getCredentialOfferURI();

        assertNotNull(credOfferUri, "A valid offer uri should be returned");

        // 2. Using the uri to get the actual credential offer
        CredentialOfferResponse credentialOfferResponse = oauth.oid4vc()
                .doCredentialOfferRequest(credOfferUri);
        CredentialsOffer credentialsOffer = credentialOfferResponse.getCredentialsOffer();

        assertNotNull(credentialsOffer, "A valid offer should be returned");
        List<String> credConfigIds = credentialsOffer.getCredentialConfigurationIds();
        assertEquals(1, credConfigIds.size(), "We only expect one credential_configuration_id");

        // 3. Get the issuer metadata
        CredentialIssuer credentialIssuer = oauth.oid4vc()
                .issuerMetadataRequest()
                .endpoint(credentialsOffer.getIssuerMetadataUrl())
                .send()
                .getMetadata();

        assertNotNull(credentialIssuer, "Issuer metadata should be returned");
        assertEquals(1, credentialIssuer.getAuthorizationServers().size(), "We only expect one authorization server.");

        // 4. Get the openid-configuration
        OIDCConfigurationRepresentation openidConfig = oauth.wellknownRequest()
                .url(credentialIssuer.getAuthorizationServers().get(0))
                .send()
                .getOidcConfiguration();

        assertNotNull(openidConfig.getTokenEndpoint(), "A token endpoint should be included.");
        assertFalse(openidConfig.getGrantTypesSupported().contains(PreAuthorizedCodeGrant.PRE_AUTH_GRANT_TYPE), "The pre-authorized code should not be supported.");

        // 5. Get the credential using credential_identifier (required when authorization_details are present)
        credConfigIds.stream()
                .map(offeredCredentialId -> credentialIssuer.getCredentialsSupported().get(offeredCredentialId))
                .map(credConfigId -> credentialIssuer.getCredentialsSupported().get(credConfigId))
                .forEach(supportedCredential -> {
                    String cNonce = getCNonce();
                    Proofs proofs = jwtProofs(credentialIssuer.getCredentialIssuer(), cNonce);
                    CredentialResponse credentialResponseVO = oauth.oid4vc()
                            .credentialRequest()
                            .bearerToken(token)
                            .credentialIdentifier(credentialIdentifier)
                            .proofs(proofs)
                            .send()
                            .getCredentialResponse();

                    JsonWebToken jsonWebToken;
                    try {
                        jsonWebToken = TokenVerifier.create((String) credentialResponseVO.getCredentials().get(0).getCredential(), JsonWebToken.class).getToken();
                    } catch (VerificationException e) {
                        throw new RuntimeException(e);
                    }

                    assertNotNull(jsonWebToken, "A valid credential string should have been responded");
                    assertNotNull(jsonWebToken.getOtherClaims().get("vc"), "The credentials should be included at the vc-claim.");

                    VerifiableCredential credential = JsonSerialization.mapper.convertValue(
                            jsonWebToken.getOtherClaims().get("vc"),
                            VerifiableCredential.class
                    );

                    assertTrue(credential.getCredentialSubject().getClaims().containsKey("scope-name"), "The static claim should be set.");
                    assertEquals(jwtTypeCredentialScope.getName(), credential.getCredentialSubject().getClaims().get("scope-name"), "The static claim should be set.");
                    assertFalse(credential.getCredentialSubject().getClaims().containsKey("AnotherCredentialType"), "Only mappers supported for the requested type should have been evaluated.");
                });
    }

    @Test
    public void testCredentialIssuanceWithAuthZCodeWithScopeMatched() throws Exception {
        String scopeName = jwtTypeCredentialScope.getName();
        String credentialConfigurationId = jwtTypeCredentialScope.getAttributes()
                .get(CredentialScopeModel.VC_CONFIGURATION_ID);
        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authCode).send();
        assertTrue(accessTokenResponse.isSuccess(), "Access token request should succeed");
        assertNotNull(accessTokenResponse.getAccessToken(), "Access token should be present");

        List<OID4VCAuthorizationDetail> authDetailsResponse = accessTokenResponse.getOID4VCAuthorizationDetails();
        assertNotNull(authDetailsResponse, "authorization_details should be present in the token response");
        assertFalse(authDetailsResponse.isEmpty(), "authorization_details should not be empty");

        OID4VCAuthorizationDetail firstAuthorizationDetail = authDetailsResponse.get(0);
        assertEquals(credentialConfigurationId, firstAuthorizationDetail.getCredentialConfigurationId(), "credential_configuration_id should match requested scope");
        assertNotNull(firstAuthorizationDetail.getCredentialIdentifiers(), "credential_identifiers should be present");
        assertFalse(firstAuthorizationDetail.getCredentialIdentifiers().isEmpty(), "credential_identifiers should not be empty");

        String credentialIdentifier = firstAuthorizationDetail.getCredentialIdentifiers().get(0);

        String cNonce = getCNonce();
        Proofs proofs = jwtProofs(credentialIssuer.getCredentialIssuer(), cNonce);
        CredentialResponse credentialResponse = oauth.oid4vc()
                .credentialRequest()
                .bearerToken(accessTokenResponse.getAccessToken())
                .credentialIdentifier(credentialIdentifier)
                .proofs(proofs)
                .send()
                .getCredentialResponse();
        assertNotNull(credentialResponse, "Credential request should return a response");
        assertNotNull(credentialResponse.getCredentials(), "Credential response should contain credentials");
    }

    @Test
    public void testCredentialIssuanceWithAuthZCodeWithScopeUnmatched() {
        testCredentialIssuanceWithAuthZCodeFlow(
                sdJwtTypeCredentialScope,
                (testScope) -> getBearerToken(oauth.openid(false).scope("email")), // set registered different scope
                m -> {
                    String accessToken = (String) m.get("accessToken");
                    WebTarget credentialTarget = (WebTarget) m.get("credentialTarget");
                    CredentialRequest credentialRequest = (CredentialRequest) m.get("credentialRequest");

                    try (Response response = credentialTarget.request()
                            .header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken)
                            .post(Entity.json(credentialRequest))) {
                        assertEquals(400, response.getStatus());
                    }
                }
        );
    }

    @Test
    public void testCredentialIssuanceWithAuthZCodeSWithoutScope() {
        testCredentialIssuanceWithAuthZCodeFlow(
                sdJwtTypeCredentialScope,
                (testScope) -> getBearerToken(oauth.openid(false).scope(null)), // no scope
                m -> {
                    String accessToken = (String) m.get("accessToken");
                    WebTarget credentialTarget = (WebTarget) m.get("credentialTarget");
                    CredentialRequest credentialRequest = (CredentialRequest) m.get("credentialRequest");

                    try (Response response = credentialTarget.request()
                            .header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken)
                            .post(Entity.json(credentialRequest))) {
                        assertEquals(400, response.getStatus());
                    }
                }
        );
    }

    /**
     * When the token contains authorization_details, the credential_identifier from the request must match that in
     * the authorization_details from the AccessTokenResponse and in the AccessToken JWT.
     */
    @Test
    public void testCredentialIssuanceWithScopeUnmatched() {
        Function<String, String> getAccessToken = (testScope) ->
                getBearerToken(oauth, client, jwtTypeCredentialScope.getName());

        Consumer<Map<String, Object>> sendCredentialRequest = m -> {
            String accessToken = (String) m.get("accessToken");
            WebTarget credentialTarget = (WebTarget) m.get("credentialTarget");
            CredentialRequest credentialRequest = (CredentialRequest) m.get("credentialRequest");

            try (Response response = credentialTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken)
                    .post(Entity.json(credentialRequest))) {

                assertEquals(400, response.getStatus());
                String errorMessage = response.readEntity(String.class);

                assertTrue(errorMessage.contains("unknown_credential_identifier"));
                assertTrue(errorMessage.contains("credential_identifier must match one from the authorization_details in the access token"));
            }
        };

        testCredentialIssuanceWithAuthZCodeFlow(sdJwtTypeCredentialScope, getAccessToken, sendCredentialRequest,
                (cid) -> new CredentialRequest().setCredentialIdentifier("sd-jwt-credential"));
    }

    @Test
    public void testRequestMultipleCredentialsWithProofs() {
        final String scopeName = jwtTypeCredentialScope.getName();
        String credConfigId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);

        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

        String cNonce = getCNonce();

        runOnServer.run(session -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                String issuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());

                String jwtProof1 = generateJwtProof(issuer, cNonce);
                String jwtProof2 = generateJwtProof(issuer, cNonce);
                Proofs proofs = new Proofs().setJwt(Arrays.asList(jwtProof1, jwtProof2));

                CredentialRequest request = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier)
                        .setProofs(proofs);

                OID4VCIssuerEndpoint endpoint = prepareIssuerEndpoint(session, authenticator);
                String requestPayload = JsonSerialization.writeValueAsString(request);

                Response response = endpoint.requestCredential(requestPayload);
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Response status should be OK");

                CredentialResponse credentialResponse = JsonSerialization.mapper
                        .convertValue(response.getEntity(), CredentialResponse.class);

                assertNotNull(credentialResponse, "Credential response should not be null");
                assertNotNull(credentialResponse.getCredentials(), "Credentials array should not be null");
                assertEquals(2, credentialResponse.getCredentials().size(), "Should return 2 credentials due to two proofs");

                // Validate each credential
                for (CredentialResponse.Credential credential : credentialResponse.getCredentials()) {
                    assertNotNull(credential.getCredential(), "Credential should not be null");
                    JsonWebToken jsonWebToken;
                    try {
                        jsonWebToken = TokenVerifier.create((String) credential.getCredential(), JsonWebToken.class).getToken();
                    } catch (VerificationException e) {
                        fail("Failed to verify JWT: " + e.getMessage());
                        return;
                    }

                    assertNotNull(jsonWebToken, "A valid credential string should be returned");
                    assertNotNull(jsonWebToken.getOtherClaims().get("vc"), "The credentials should include the vc claim");

                    VerifiableCredential vc = JsonSerialization.mapper.convertValue(
                            jsonWebToken.getOtherClaims().get("vc"),
                            VerifiableCredential.class
                    );

                    assertTrue(vc.getCredentialSubject().getClaims().containsKey("scope-name"), "The scope-name claim should be set");
                    assertEquals(scopeName, vc.getCredentialSubject().getClaims().get("scope-name"), "The scope-name claim should match the scope");
                    assertTrue(vc.getCredentialSubject().getClaims().containsKey("given_name"), "The given_name claim should be set");
                    assertEquals("John", vc.getCredentialSubject().getClaims().get("given_name"), "The given_name claim should be John");
                    assertTrue(vc.getCredentialSubject().getClaims().containsKey("email"), "The email claim should be set");
                    assertEquals("john@email.cz", vc.getCredentialSubject().getClaims().get("email"), "The email claim should be john@email.cz");
                    assertFalse(vc.getCredentialSubject().getClaims().containsKey("AnotherCredentialType"), "Only supported mappers should be evaluated");
                }
            } catch (Exception e) {
                throw new RuntimeException("Test failed due to: " + e.getMessage(), e);
            }
        });
    }

    @Test
    public void testRequestCredentialWithKidProofWithoutKeyAttestation() {
        final String scopeName = jwtTypeCredentialScope.getName();
        String credConfigId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        String credentialIdentifier = tokenResponse.getOID4VCAuthorizationDetails().get(0).getCredentialIdentifiers().get(0);
        String cNonce = getCNonce();

        runOnServer.run(session -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                String issuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
                String kidOnlyJwtProof = generateJwtProofWithKidNoAttestation(issuer, cNonce);

                CredentialRequest request = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier)
                        .setProofs(new Proofs().setJwt(List.of(kidOnlyJwtProof)));
                String requestPayload = JsonSerialization.writeValueAsString(request);

                OID4VCIssuerEndpoint endpoint = prepareIssuerEndpoint(session, authenticator);
                ErrorResponseException ex = assertThrows(ErrorResponseException.class,
                        () -> endpoint.requestCredential(requestPayload));
                assertEquals(ErrorType.INVALID_PROOF.getValue(), ex.getError());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testRequestCredentialWithJwkKeyAttestationAccepted() {
        Map<String, String> requestContext = prepareJwtCredentialRequestContext();
        String token = requestContext.get("token");
        String credentialIdentifier = requestContext.get("credentialIdentifier");
        String cNonce = requestContext.get("cNonce");

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            String previousTrustedKeys = realm.getAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR);
            try {
                KeyWrapper attestationSigner = OID4VCProofTestUtils.newEcSigningKey("endpoint-attestation-jwk");
                JWK trustedAttestationJwk = JWKBuilder.create().ec(attestationSigner.getPublicKey());
                trustedAttestationJwk.setKeyId(attestationSigner.getKid());
                trustedAttestationJwk.setAlgorithm(attestationSigner.getAlgorithm());
                realm.setAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR,
                        JsonSerialization.writeValueAsString(List.of(trustedAttestationJwk)));

                KeyWrapper proofKey = OID4VCProofTestUtils.newEcSigningKey("endpoint-proof-jwk");
                JWK proofJwk = JWKBuilder.create().ec(proofKey.getPublicKey());
                proofJwk.setKeyId(proofKey.getKid());
                proofJwk.setAlgorithm(proofKey.getAlgorithm());
                String attestationJwt = OID4VCProofTestUtils.generateAttestationProof(
                        attestationSigner, cNonce, List.of(proofJwk), List.of("iso_18045_high"),
                        List.of("iso_18045_high"), null);

                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                String issuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
                String jwtProof = generateJwtProofWithEmbeddedAttestation(
                        proofKey, attestationJwt, cNonce, issuer, false);

                CredentialRequest request = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier)
                        .setProofs(new Proofs().setJwt(List.of(jwtProof)));
                String requestPayload = JsonSerialization.writeValueAsString(request);

                OID4VCIssuerEndpoint endpoint = prepareIssuerEndpoint(session, authenticator);
                Response response = endpoint.requestCredential(requestPayload);
                assertSingleCredentialResponse(response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (previousTrustedKeys != null) {
                    realm.setAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR, previousTrustedKeys);
                } else {
                    realm.removeAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR);
                }
            }
        });
    }

    @Test
    public void testRequestCredentialWithKidKeyAttestationAccepted() {
        Map<String, String> requestContext = prepareJwtCredentialRequestContext();
        String token = requestContext.get("token");
        String credentialIdentifier = requestContext.get("credentialIdentifier");
        String cNonce = requestContext.get("cNonce");

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            String previousTrustedKeys = realm.getAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR);
            try {
                KeyWrapper attestationSigner = OID4VCProofTestUtils.newEcSigningKey("endpoint-attestation-kid");
                JWK trustedAttestationJwk = JWKBuilder.create().ec(attestationSigner.getPublicKey());
                trustedAttestationJwk.setKeyId(attestationSigner.getKid());
                trustedAttestationJwk.setAlgorithm(attestationSigner.getAlgorithm());
                realm.setAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR,
                        JsonSerialization.writeValueAsString(List.of(trustedAttestationJwk)));

                KeyWrapper proofKey = OID4VCProofTestUtils.newEcSigningKey("endpoint-proof-kid");
                JWK proofJwk = JWKBuilder.create().ec(proofKey.getPublicKey());
                proofJwk.setKeyId(proofKey.getKid());
                proofJwk.setAlgorithm(proofKey.getAlgorithm());
                String attestationJwt = OID4VCProofTestUtils.generateAttestationProof(
                        attestationSigner, cNonce, List.of(proofJwk), List.of("iso_18045_high"),
                        List.of("iso_18045_high"), null);

                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                String issuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
                String jwtProof = generateJwtProofWithEmbeddedAttestation(
                        proofKey, attestationJwt, cNonce, issuer, true);

                CredentialRequest request = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier)
                        .setProofs(new Proofs().setJwt(List.of(jwtProof)));
                String requestPayload = JsonSerialization.writeValueAsString(request);

                OID4VCIssuerEndpoint endpoint = prepareIssuerEndpoint(session, authenticator);
                Response response = endpoint.requestCredential(requestPayload);
                assertSingleCredentialResponse(response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (previousTrustedKeys != null) {
                    realm.setAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR, previousTrustedKeys);
                } else {
                    realm.removeAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR);
                }
            }
        });
    }

    @Test
    public void testRequestCredentialWithAttestationProofAccepted() {
        Map<String, String> requestContext = prepareJwtCredentialRequestContext();
        String token = requestContext.get("token");
        String credentialIdentifier = requestContext.get("credentialIdentifier");
        String cNonce = requestContext.get("cNonce");

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            String previousTrustedKeys = realm.getAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR);
            try {
                KeyWrapper attestationSigner = OID4VCProofTestUtils.newEcSigningKey("endpoint-attestation-proof-type");
                JWK trustedAttestationJwk = JWKBuilder.create().ec(attestationSigner.getPublicKey());
                trustedAttestationJwk.setKeyId(attestationSigner.getKid());
                trustedAttestationJwk.setAlgorithm(attestationSigner.getAlgorithm());
                realm.setAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR,
                        JsonSerialization.writeValueAsString(List.of(trustedAttestationJwk)));

                KeyWrapper proofKey = OID4VCProofTestUtils.newEcSigningKey("endpoint-proof-attestation-proof-type");
                JWK proofJwk = JWKBuilder.create().ec(proofKey.getPublicKey());
                proofJwk.setKeyId(proofKey.getKid());
                proofJwk.setAlgorithm(proofKey.getAlgorithm());
                String attestationJwt = OID4VCProofTestUtils.generateAttestationProof(
                        attestationSigner, cNonce, List.of(proofJwk), List.of("iso_18045_high"),
                        List.of("iso_18045_high"), null);

                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);

                CredentialRequest request = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier)
                        .setProofs(new Proofs().setAttestation(List.of(attestationJwt)));
                String requestPayload = JsonSerialization.writeValueAsString(request);

                OID4VCIssuerEndpoint endpoint = prepareIssuerEndpoint(session, authenticator);
                Response response = endpoint.requestCredential(requestPayload);
                assertSingleCredentialResponse(response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (previousTrustedKeys != null) {
                    realm.setAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR, previousTrustedKeys);
                } else {
                    realm.removeAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR);
                }
            }
        });
    }

    @Test
    public void testRequestCredentialWithKeyAttestationMismatchedProofKeyRejected() {
        Map<String, String> requestContext = prepareJwtCredentialRequestContext();
        String token = requestContext.get("token");
        String credentialIdentifier = requestContext.get("credentialIdentifier");
        String cNonce = requestContext.get("cNonce");

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            String previousTrustedKeys = realm.getAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR);
            try {
                KeyWrapper attestationSigner = OID4VCProofTestUtils.newEcSigningKey("endpoint-attestation-mismatch");
                JWK trustedAttestationJwk = JWKBuilder.create().ec(attestationSigner.getPublicKey());
                trustedAttestationJwk.setKeyId(attestationSigner.getKid());
                trustedAttestationJwk.setAlgorithm(attestationSigner.getAlgorithm());
                realm.setAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR,
                        JsonSerialization.writeValueAsString(List.of(trustedAttestationJwk)));

                KeyWrapper proofKey = OID4VCProofTestUtils.newEcSigningKey("endpoint-proof-used");
                KeyWrapper otherKey = OID4VCProofTestUtils.newEcSigningKey("endpoint-proof-attested");
                JWK otherJwk = JWKBuilder.create().ec(otherKey.getPublicKey());
                otherJwk.setKeyId(otherKey.getKid());
                otherJwk.setAlgorithm(otherKey.getAlgorithm());
                String attestationJwt = OID4VCProofTestUtils.generateAttestationProof(
                        attestationSigner, cNonce, List.of(otherJwk), List.of("iso_18045_high"),
                        List.of("iso_18045_high"), null);

                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                String issuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
                String jwtProof = generateJwtProofWithEmbeddedAttestation(
                        proofKey, attestationJwt, cNonce, issuer, false);

                CredentialRequest request = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier)
                        .setProofs(new Proofs().setJwt(List.of(jwtProof)));
                String requestPayload = JsonSerialization.writeValueAsString(request);

                OID4VCIssuerEndpoint endpoint = prepareIssuerEndpoint(session, authenticator);
                ErrorResponseException ex = assertThrows(ErrorResponseException.class,
                        () -> endpoint.requestCredential(requestPayload));
                assertEquals(ErrorType.INVALID_PROOF.getValue(), ex.getError());
                assertTrue(ex.getErrorDescription().contains("attested_keys"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (previousTrustedKeys != null) {
                    realm.setAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR, previousTrustedKeys);
                } else {
                    realm.removeAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR);
                }
            }
        });
    }

    @Test
    public void testRequestCredentialWithKeyAttestationMissingExpRejected() {
        Map<String, String> requestContext = prepareJwtCredentialRequestContext();
        String token = requestContext.get("token");
        String credentialIdentifier = requestContext.get("credentialIdentifier");
        String cNonce = requestContext.get("cNonce");

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            String previousTrustedKeys = realm.getAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR);
            try {
                KeyWrapper attestationSigner = OID4VCProofTestUtils.newEcSigningKey("endpoint-attestation-no-exp");
                JWK trustedAttestationJwk = JWKBuilder.create().ec(attestationSigner.getPublicKey());
                trustedAttestationJwk.setKeyId(attestationSigner.getKid());
                trustedAttestationJwk.setAlgorithm(attestationSigner.getAlgorithm());
                realm.setAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR,
                        JsonSerialization.writeValueAsString(List.of(trustedAttestationJwk)));

                KeyWrapper proofKey = OID4VCProofTestUtils.newEcSigningKey("endpoint-proof-no-exp");
                JWK proofJwk = JWKBuilder.create().ec(proofKey.getPublicKey());
                proofJwk.setKeyId(proofKey.getKid());
                proofJwk.setAlgorithm(proofKey.getAlgorithm());
                String attestationJwtWithoutExp = generateAttestationProofWithoutExp(
                        attestationSigner, cNonce, List.of(proofJwk));

                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                String issuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
                String jwtProof = generateJwtProofWithEmbeddedAttestation(
                        proofKey, attestationJwtWithoutExp, cNonce, issuer, false);

                CredentialRequest request = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier)
                        .setProofs(new Proofs().setJwt(List.of(jwtProof)));
                String requestPayload = JsonSerialization.writeValueAsString(request);

                OID4VCIssuerEndpoint endpoint = prepareIssuerEndpoint(session, authenticator);
                ErrorResponseException ex = assertThrows(ErrorResponseException.class,
                        () -> endpoint.requestCredential(requestPayload));
                assertEquals(ErrorType.INVALID_PROOF.getValue(), ex.getError());
                assertTrue(ex.getErrorDescription().contains("Missing 'exp' claim"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (previousTrustedKeys != null) {
                    realm.setAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR, previousTrustedKeys);
                } else {
                    realm.removeAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR);
                }
            }
        });
    }

    @Test
    public void testRequestCredentialWithHs256JwtProofRejected() {
        final String scopeName = jwtTypeCredentialScope.getName();
        String credConfigId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        String credentialIdentifier = tokenResponse.getOID4VCAuthorizationDetails().get(0).getCredentialIdentifiers().get(0);
        String cNonce = getCNonce();

        runOnServer.run(session -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                String issuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
                String validJwtProof = generateJwtProof(issuer, cNonce);
                String hs256JwtProof = withModifiedHeaderClaim(validJwtProof, "alg", "HS256");

                CredentialRequest request = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier)
                        .setProofs(new Proofs().setJwt(List.of(hs256JwtProof)));
                String requestPayload = JsonSerialization.writeValueAsString(request);

                OID4VCIssuerEndpoint endpoint = prepareIssuerEndpoint(session, authenticator);
                ErrorResponseException ex = assertThrows(ErrorResponseException.class,
                        () -> endpoint.requestCredential(requestPayload));
                assertEquals(ErrorType.INVALID_PROOF.getValue(), ex.getError());
                assertTrue(ex.getErrorDescription().contains("Proof signature algorithm not supported"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testRequestCredentialWithPrivateJwkMaterialInHeaderRejected() {
        final String scopeName = jwtTypeCredentialScope.getName();
        String credConfigId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        String credentialIdentifier = tokenResponse.getOID4VCAuthorizationDetails().get(0).getCredentialIdentifiers().get(0);
        String cNonce = getCNonce();

        runOnServer.run(session -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                String issuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
                String validJwtProof = generateJwtProof(issuer, cNonce);
                String proofWithPrivateJwkMaterial = withPrivateJwkMaterialInHeader(validJwtProof);

                CredentialRequest request = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier)
                        .setProofs(new Proofs().setJwt(List.of(proofWithPrivateJwkMaterial)));
                String requestPayload = JsonSerialization.writeValueAsString(request);

                OID4VCIssuerEndpoint endpoint = prepareIssuerEndpoint(session, authenticator);
                ErrorResponseException ex = assertThrows(ErrorResponseException.class,
                        () -> endpoint.requestCredential(requestPayload));
                assertEquals(ErrorType.INVALID_PROOF.getValue(), ex.getError());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testRequestCredentialWithMissingIssuerInClientBoundFlowAllowed() {
        final String scopeName = jwtTypeCredentialScope.getName();
        String credConfigId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        String credentialIdentifier = tokenResponse.getOID4VCAuthorizationDetails().get(0).getCredentialIdentifiers().get(0);
        String cNonce = getCNonce();

        runOnServer.run(session -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                String issuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
                String missingIssuerProof = generateJwtProofWithClaims(List.of(issuer), cNonce, null, null, null, null);

                CredentialRequest request = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier)
                        .setProofs(new Proofs().setJwt(List.of(missingIssuerProof)));
                String requestPayload = JsonSerialization.writeValueAsString(request);

                OID4VCIssuerEndpoint endpoint = prepareIssuerEndpoint(session, authenticator);
                Response response = endpoint.requestCredential(requestPayload);
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Response status should be OK");
                CredentialResponse credentialResponse = JsonSerialization.mapper
                        .convertValue(response.getEntity(), CredentialResponse.class);
                assertNotNull(credentialResponse, "Credential response should not be null");
                assertNotNull(credentialResponse.getCredentials(), "Credentials should not be null");
                assertEquals(1, credentialResponse.getCredentials().size(), "Expected exactly one credential");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testRequestCredentialWithWrongIssuerInClientBoundFlowRejected() {
        final String scopeName = jwtTypeCredentialScope.getName();
        String credConfigId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        String credentialIdentifier = tokenResponse.getOID4VCAuthorizationDetails().get(0).getCredentialIdentifiers().get(0);
        String cNonce = getCNonce();

        runOnServer.run(session -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                String issuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
                String wrongIssuerProof = generateJwtProofWithClaims(
                        List.of(issuer), cNonce, "wrong-client-id", null, null, null);

                CredentialRequest request = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier)
                        .setProofs(new Proofs().setJwt(List.of(wrongIssuerProof)));
                String requestPayload = JsonSerialization.writeValueAsString(request);

                OID4VCIssuerEndpoint endpoint = prepareIssuerEndpoint(session, authenticator);
                ErrorResponseException ex = assertThrows(ErrorResponseException.class,
                        () -> endpoint.requestCredential(requestPayload));
                assertEquals(ErrorType.INVALID_PROOF.getValue(), ex.getError());
                assertTrue(ex.getErrorDescription().contains("Issuer claim must be the client_id"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testRequestCredentialWithMultipleAudiencesRejected() {
        final String scopeName = jwtTypeCredentialScope.getName();
        String credConfigId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        String credentialIdentifier = tokenResponse.getOID4VCAuthorizationDetails().get(0).getCredentialIdentifiers().get(0);
        String cNonce = getCNonce();

        runOnServer.run(session -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                String issuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
                String multiAudProof = generateJwtProofWithClaims(
                        List.of(issuer, "https://unrelated.example"),
                        cNonce,
                        OID4VCI_CLIENT_ID,
                        null,
                        null,
                        null);

                CredentialRequest request = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier)
                        .setProofs(new Proofs().setJwt(List.of(multiAudProof)));
                String requestPayload = JsonSerialization.writeValueAsString(request);

                OID4VCIssuerEndpoint endpoint = prepareIssuerEndpoint(session, authenticator);
                ErrorResponseException ex = assertThrows(ErrorResponseException.class,
                        () -> endpoint.requestCredential(requestPayload));
                assertEquals(ErrorType.INVALID_PROOF.getValue(), ex.getError());
                assertTrue(ex.getErrorDescription().contains("Audience claim must be single value"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testRequestCredentialWithFutureIatRejected() {
        final String scopeName = jwtTypeCredentialScope.getName();
        String credConfigId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        String credentialIdentifier = tokenResponse.getOID4VCAuthorizationDetails().get(0).getCredentialIdentifiers().get(0);
        String cNonce = getCNonce();

        runOnServer.run(session -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                String issuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
                long now = System.currentTimeMillis() / 1000L;
                String futureIatProof = generateJwtProofWithClaims(
                        List.of(issuer),
                        cNonce,
                        OID4VCI_CLIENT_ID,
                        now + 120,
                        null,
                        null);

                CredentialRequest request = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier)
                        .setProofs(new Proofs().setJwt(List.of(futureIatProof)));
                String requestPayload = JsonSerialization.writeValueAsString(request);

                OID4VCIssuerEndpoint endpoint = prepareIssuerEndpoint(session, authenticator);
                ErrorResponseException ex = assertThrows(ErrorResponseException.class,
                        () -> endpoint.requestCredential(requestPayload));
                assertEquals(ErrorType.INVALID_PROOF.getValue(), ex.getError());
                assertTrue(ex.getErrorDescription().contains("Proof iat is in the future"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testRequestCredentialWithExpiredExpRejected() {
        final String scopeName = jwtTypeCredentialScope.getName();
        String credConfigId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        String credentialIdentifier = tokenResponse.getOID4VCAuthorizationDetails().get(0).getCredentialIdentifiers().get(0);
        String cNonce = getCNonce();

        runOnServer.run(session -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                String issuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
                long now = System.currentTimeMillis() / 1000L;
                String expiredExpProof = generateJwtProofWithClaims(
                        List.of(issuer),
                        cNonce,
                        OID4VCI_CLIENT_ID,
                        now,
                        now - 1,
                        null);

                CredentialRequest request = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier)
                        .setProofs(new Proofs().setJwt(List.of(expiredExpProof)));
                String requestPayload = JsonSerialization.writeValueAsString(request);

                OID4VCIssuerEndpoint endpoint = prepareIssuerEndpoint(session, authenticator);
                ErrorResponseException ex = assertThrows(ErrorResponseException.class,
                        () -> endpoint.requestCredential(requestPayload));
                assertEquals(ErrorType.INVALID_PROOF.getValue(), ex.getError());
                assertTrue(ex.getErrorDescription().contains("Proof has expired"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testRequestCredentialWithFutureNbfRejected() {
        final String scopeName = jwtTypeCredentialScope.getName();
        String credConfigId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        String credentialIdentifier = tokenResponse.getOID4VCAuthorizationDetails().get(0).getCredentialIdentifiers().get(0);
        String cNonce = getCNonce();

        runOnServer.run(session -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                String issuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
                long now = System.currentTimeMillis() / 1000L;
                String futureNbfProof = generateJwtProofWithClaims(
                        List.of(issuer),
                        cNonce,
                        OID4VCI_CLIENT_ID,
                        now,
                        null,
                        now + 120);

                CredentialRequest request = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier)
                        .setProofs(new Proofs().setJwt(List.of(futureNbfProof)));
                String requestPayload = JsonSerialization.writeValueAsString(request);

                OID4VCIssuerEndpoint endpoint = prepareIssuerEndpoint(session, authenticator);
                ErrorResponseException ex = assertThrows(ErrorResponseException.class,
                        () -> endpoint.requestCredential(requestPayload));
                assertEquals(ErrorType.INVALID_PROOF.getValue(), ex.getError());
                assertTrue(ex.getErrorDescription().contains("Proof is not yet valid"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testRequestCredentialWithTrustChainHeaderRejected() {
        final String scopeName = jwtTypeCredentialScope.getName();
        String credConfigId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        String credentialIdentifier = tokenResponse.getOID4VCAuthorizationDetails().get(0).getCredentialIdentifiers().get(0);
        String cNonce = getCNonce();

        runOnServer.run(session -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                String issuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
                String validJwtProof = generateJwtProof(issuer, cNonce);
                String trustChainProof = withModifiedHeaderClaim(validJwtProof, "trust_chain", List.of("dummy-trust-chain-entry"));

                CredentialRequest request = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier)
                        .setProofs(new Proofs().setJwt(List.of(trustChainProof)));
                String requestPayload = JsonSerialization.writeValueAsString(request);

                OID4VCIssuerEndpoint endpoint = prepareIssuerEndpoint(session, authenticator);
                ErrorResponseException ex = assertThrows(ErrorResponseException.class,
                        () -> endpoint.requestCredential(requestPayload));
                assertEquals(ErrorType.INVALID_PROOF.getValue(), ex.getError());
                assertTrue(ex.getErrorDescription().contains("trust_chain"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testRequestCredentialWithKidAndJwkHeadersRejected() {
        final String scopeName = jwtTypeCredentialScope.getName();
        String credConfigId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        String credentialIdentifier = tokenResponse.getOID4VCAuthorizationDetails().get(0).getCredentialIdentifiers().get(0);
        String cNonce = getCNonce();

        runOnServer.run(session -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                String issuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
                String validJwtProof = generateJwtProof(issuer, cNonce);
                String kidAndJwkProof = withModifiedHeaderClaim(validJwtProof, "kid", "some-kid");

                CredentialRequest request = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier)
                        .setProofs(new Proofs().setJwt(List.of(kidAndJwkProof)));
                String requestPayload = JsonSerialization.writeValueAsString(request);

                OID4VCIssuerEndpoint endpoint = prepareIssuerEndpoint(session, authenticator);
                ErrorResponseException ex = assertThrows(ErrorResponseException.class,
                        () -> endpoint.requestCredential(requestPayload));
                assertEquals(ErrorType.INVALID_PROOF.getValue(), ex.getError());
                assertTrue(ex.getErrorDescription().contains("mutually exclusive"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testGetJwtVcConfigFromMetadata() {
        final String scopeName = jwtTypeCredentialScope.getName();
        final String credentialConfigurationId = jwtTypeCredentialScope.getAttributes()
                .get(CredentialScopeModel.VC_CONFIGURATION_ID);

        String expectedIssuer = keycloakUrls.getBase() + "/realms/" + testRealm.getName();
        String expectedCredentialsEndpoint = expectedIssuer + "/protocol/oid4vc/credential";
        String expectedNonceEndpoint = expectedIssuer + "/protocol/oid4vc/" + OID4VCIssuerEndpoint.NONCE_PATH;
        final String expectedAuthorizationServer = expectedIssuer;

        runOnServer.run(session -> {
            OID4VCIssuerWellKnownProvider oid4VCIssuerWellKnownProvider = new OID4VCIssuerWellKnownProvider(session);
            CredentialIssuer credentialIssuer = oid4VCIssuerWellKnownProvider.getIssuerMetadata();

            assertEquals(expectedIssuer, credentialIssuer.getCredentialIssuer(), "The correct issuer should be included.");
            assertEquals(expectedCredentialsEndpoint, credentialIssuer.getCredentialEndpoint(), "The correct credentials endpoint should be included.");
            assertEquals(expectedNonceEndpoint, credentialIssuer.getNonceEndpoint(), "The correct nonce endpoint should be included.");
            assertEquals(1, credentialIssuer.getAuthorizationServers().size(), "Since the authorization server is equal to the issuer, just 1 should be returned.");
            assertEquals(expectedAuthorizationServer, credentialIssuer.getAuthorizationServers().get(0), "The expected server should have been returned.");
            assertTrue(credentialIssuer.getCredentialsSupported().containsKey(credentialConfigurationId), "The jwt_vc-credential should be supported.");

            SupportedCredentialConfiguration jwtVcConfig = credentialIssuer.getCredentialsSupported().get(credentialConfigurationId);
            assertEquals(scopeName, jwtVcConfig.getScope(), "The jwt_vc-credential should offer type test-credential");
            assertEquals(VCFormat.JWT_VC, jwtVcConfig.getFormat(), "The jwt_vc-credential should be offered in the jwt_vc format.");

            Claims jwtVcClaims = jwtVcConfig.getCredentialMetadata() != null ? jwtVcConfig.getCredentialMetadata().getClaims() : null;
            assertNotNull(jwtVcClaims, "The jwt_vc-credential can optionally provide a claims claim.");
            assertEquals(8, jwtVcClaims.size());

            {
                Claim claim = jwtVcClaims.get(0);
                assertEquals(CREDENTIAL_SUBJECT, claim.getPath().get(0), "Has credentialSubject.id");
                assertEquals("id", claim.getPath().get(1), "credentialSubject.id mapped correctly");
                assertFalse(claim.isMandatory(), "credentialSubject.id is not mandatory");
                assertNull(claim.getDisplay(), "credentialSubject.id has no display");
            }
            {
                Claim claim = jwtVcClaims.get(1);
                assertEquals(CREDENTIAL_SUBJECT, claim.getPath().get(0), "Has credentialSubject.given_name");
                assertEquals("given_name", claim.getPath().get(1), "credentialSubject.given_name mapped correctly");
                assertFalse(claim.isMandatory(), "credentialSubject.given_name is not mandatory");
                assertNotNull(claim.getDisplay(), "credentialSubject.given_name has display");
                assertEquals(15, claim.getDisplay().size());
                for (ClaimDisplay givenNameDisplay : claim.getDisplay()) {
                    assertNotNull(givenNameDisplay.getName());
                    assertNotNull(givenNameDisplay.getLocale());
                }
            }
            {
                Claim claim = jwtVcClaims.get(2);
                assertEquals(CREDENTIAL_SUBJECT, claim.getPath().get(0), "Has credentialSubject.family_name");
                assertEquals("family_name", claim.getPath().get(1), "credentialSubject.family_name mapped correctly");
                assertFalse(claim.isMandatory(), "credentialSubject.family_name is not mandatory");
                assertNotNull(claim.getDisplay(), "credentialSubject.family_name has display");
                assertEquals(15, claim.getDisplay().size());
                for (ClaimDisplay familyNameDisplay : claim.getDisplay()) {
                    assertNotNull(familyNameDisplay.getName());
                    assertNotNull(familyNameDisplay.getLocale());
                }
            }
            {
                Claim claim = jwtVcClaims.get(3);
                assertEquals(CREDENTIAL_SUBJECT, claim.getPath().get(0), "Has credentialSubject.birthdate");
                assertEquals("birthdate", claim.getPath().get(1), "credentialSubject.birthdate mapped correctly");
                assertFalse(claim.isMandatory(), "credentialSubject.birthdate is not mandatory");
                assertNotNull(claim.getDisplay(), "credentialSubject.birthdate has display");
                assertEquals(15, claim.getDisplay().size());
                for (ClaimDisplay birthDateDisplay : claim.getDisplay()) {
                    assertNotNull(birthDateDisplay.getName());
                    assertNotNull(birthDateDisplay.getLocale());
                }
            }
            {
                Claim claim = jwtVcClaims.get(4);
                assertEquals(CREDENTIAL_SUBJECT, claim.getPath().get(0), "Has credentialSubject.email");
                assertEquals("email", claim.getPath().get(1), "credentialSubject.email mapped correctly");
                assertFalse(claim.isMandatory(), "credentialSubject.email is not mandatory");
                assertNotNull(claim.getDisplay(), "credentialSubject.email has display");
                assertEquals(15, claim.getDisplay().size());
                for (ClaimDisplay birthDateDisplay : claim.getDisplay()) {
                    assertNotNull(birthDateDisplay.getName());
                    assertNotNull(birthDateDisplay.getLocale());
                }
            }
            {
                Claim claim = jwtVcClaims.get(5);
                assertEquals(CREDENTIAL_SUBJECT, claim.getPath().get(0), "Has credentialSubject.address_locality");
                assertEquals("address", claim.getPath().get(1), "credentialSubject.address.locality mapped correctly (parent claim in path)");
                assertEquals("locality", claim.getPath().get(2), "credentialSubject.address.locality mapped correctly (nested claim in path)");
                assertFalse(claim.isMandatory(), "credentialSubject.address.locality is not mandatory");
                assertNull(claim.getDisplay(), "credentialSubject.address.locality has no display");
            }
            {
                Claim claim = jwtVcClaims.get(6);
                assertEquals(CREDENTIAL_SUBJECT, claim.getPath().get(0), "Has credentialSubject.address.street_address");
                assertEquals("address", claim.getPath().get(1), "credentialSubject.address.street_address mapped correctly (parent claim in path)");
                assertEquals("street_address", claim.getPath().get(2), "credentialSubject.address.street_address mapped correctly (nested claim in path)");
                assertFalse(claim.isMandatory(), "credentialSubject.address.street_address is not mandatory");
                assertNull(claim.getDisplay(), "credentialSubject.address.street_address has no display");
            }
            {
                Claim claim = jwtVcClaims.get(7);
                assertEquals(CREDENTIAL_SUBJECT, claim.getPath().get(0), "Has credentialSubject.scope-name");
                assertEquals("scope-name", claim.getPath().get(1), "credentialSubject.scope-name mapped correctly");
                assertFalse(claim.isMandatory(), "credentialSubject.scope-name is not mandatory");
                assertNull(claim.getDisplay(), "credentialSubject.scope-name has no display");
            }

            assertNotNull(jwtVcConfig.getCredentialDefinition(), "The jwt_vc-credential should offer credential_definition");
            assertNull(jwtVcConfig.getVct(), "JWT_VC credentials should not have vct");

            assertTrue(jwtVcConfig.getCryptographicBindingMethodsSupported().contains(CredentialScopeModel.CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT),
                    "The jwt_vc-credential should contain a cryptographic binding method supported named jwk");
            assertTrue(jwtVcConfig.getCredentialSigningAlgValuesSupported().contains("RS256"),
                    "The jwt_vc-credential should contain a credential signing algorithm named RS256");

            assertTrue(credentialIssuer.getCredentialsSupported()
                            .get(credentialConfigurationId)
                            .getProofTypesSupported()
                            .getSupportedProofTypes()
                            .get("jwt")
                            .getSigningAlgorithmsSupported()
                            .contains("RS256"),
                    "The jwt_vc-credential should support a proof of type jwt with signing algorithm RS256");

            String expectedDisplay = (jwtVcConfig.getCredentialMetadata() != null && jwtVcConfig.getCredentialMetadata().getDisplay() != null)
                    ? jwtVcConfig.getCredentialMetadata().getDisplay().get(0).getName()
                    : null;
            assertEquals(credentialConfigurationId, expectedDisplay, "The jwt_vc-credential should display as Test Credential");
        });
    }

    @Test
    public void testRequestCredentialWithUnknownCredentialIdentifier() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialScope.getName());

        runOnServer.run(session -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                CredentialRequest credentialRequest = new CredentialRequest()
                        .setCredentialIdentifier("unknown-credential-identifier");

                String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

                try {
                    issuerEndpoint.requestCredential(requestPayload);
                    fail("Expected BadRequestException due to unknown credential identifier");
                } catch (BadRequestException e) {
                    ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                    assertEquals(ErrorType.UNKNOWN_CREDENTIAL_IDENTIFIER.getValue(), error.getError());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testRequestCredentialWithCredentialConfigurationId() {

        String scopeName = jwtTypeCredentialScope.getName();
        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode);

        List<OID4VCAuthorizationDetail> authDetails = tokenResponse.getOID4VCAuthorizationDetails();
        assertEquals(1, authDetails.size(), "Expected one OID4VCAuthorizationDetail");

        // Server now requires credential_identifier when authorization_details are present,
        // so this request is treated as an invalid credential request.
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> oauth.oid4vc().credentialRequest()
                .credentialConfigurationId(authDetails.get(0).getCredentialConfigurationId())
                .bearerToken(tokenResponse.getAccessToken())
                .send().getCredentialResponse());
        assertTrue(ex.getMessage().contains("Credential must be requested by credential identifier from authorization_details"), ex.getMessage());
    }

    @Test
    public void testRequestCredentialWhenNoCredentialBuilderForFormat() {
        String scopeName = jwtTypeCredentialScope.getName();
        String credConfigId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);

        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();

        assertNotNull(authDetailsResponse, "authorization_details should be present in the response");
        assertFalse(authDetailsResponse.isEmpty(), "authorization_details should not be empty");

        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);
        assertNotNull(credentialIdentifier, "credential_identifier should be present");
        String cNonce = getCNonce();
        String credentialIssuerId = credentialIssuer.getCredentialIssuer();

        runOnServer.run(session -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator, Map.of());
                Proofs proofs = jwtProofs(credentialIssuerId, cNonce);

                CredentialRequest credentialRequest = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier)
                        .setProofs(proofs);

                String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

                try {
                    issuerEndpoint.requestCredential(requestPayload);
                    fail("Expected BadRequestException due to missing credential builder for format");
                } catch (BadRequestException e) {
                    ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                    assertEquals(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION.getValue(), error.getError());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testProofToProofsConversion() {
        final String scopeName = jwtTypeCredentialScope.getName();
        final String credentialConfigurationId = jwtTypeCredentialScope.getAttributes()
                .get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credentialConfigurationId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);

        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

        runOnServer.run(session -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                // Test 1: Create a request with single proof field - should be converted to proofs array
                CredentialRequest requestWithProof = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier);

                JwtProof singleProof = new JwtProof()
                        .setJwt("dummy-jwt")
                        .setProofType("jwt");
                requestWithProof.setProof(singleProof);

                String requestPayload = JsonSerialization.writeValueAsString(requestWithProof);

                try {
                    issuerEndpoint.requestCredential(requestPayload);
                    fail();
                } catch (ErrorResponseException ex) {
                    assertEquals(ErrorType.INVALID_PROOF.getValue(), ex.getError());
                    assertEquals("Could not validate JWT proof", ex.getErrorDescription());
                }

                // Test 2: Create a request with both proof and proofs fields - should fail validation
                CredentialRequest requestWithBoth = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier);

                requestWithBoth.setProof(singleProof);

                Proofs proofsArray = new Proofs();
                proofsArray.setJwt(List.of("dummy-jwt"));
                requestWithBoth.setProofs(proofsArray);

                String bothFieldsPayload = JsonSerialization.writeValueAsString(requestWithBoth);

                try {
                    issuerEndpoint.requestCredential(bothFieldsPayload);
                    fail("Expected BadRequestException when both proof and proofs are provided");
                } catch (BadRequestException e) {
                    int statusCode = e.getResponse().getStatus();
                    assertEquals(400, statusCode, "Expected HTTP 400 Bad Request");
                    ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                    assertEquals(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue(), error.getError());
                    assertEquals("Both 'proof' and 'proofs' must not be present at the same time", error.getErrorDescription());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testCredentialRequestWithOptionalClientScope() {
        ClientScopeRepresentation optionalScope = createOptionalClientScope(
                "optional-jwt-credential",
                ISSUER_DID.toString(),
                "optional-jwt-credential-config-id",
                null, null,
                VCFormat.JWT_VC,
                null, null
        );

        optionalScope = registerOptionalClientScope(optionalScope);
        ClientRepresentation testClient = testRealm.admin().clients().findByClientId(OID4VCI_CLIENT_ID).get(0);
        testRealm.admin().clients().get(testClient.getId()).addOptionalClientScope(optionalScope.getId());

        final String scopeName = optionalScope.getName();
        final String configId = optionalScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(configId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);

        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

        runOnServer.run(session -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                CredentialRequest credentialRequest = new CredentialRequest()
                        .setCredentialIdentifier(credentialIdentifier);

                String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
                Response credentialResponse = issuerEndpoint.requestCredential(requestPayload);

                assertEquals(HttpStatus.SC_OK, credentialResponse.getStatus(), "The credential request should succeed for Optional client scope");
                assertNotNull(credentialResponse.getEntity(), "A credential should be returned");

                CredentialResponse credentialResponseVO = JsonSerialization.mapper
                        .convertValue(credentialResponse.getEntity(), CredentialResponse.class);

                assertNotNull(credentialResponseVO.getCredentials(), "Credentials array should not be null");
                assertFalse(credentialResponseVO.getCredentials().isEmpty(), "Credentials array should not be empty");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testCannotAssignOid4vciScopeAsDefaultToClient() {
        ClientScopeRepresentation oid4vciScope = createOptionalClientScope(
                "test-oid4vci-scope",
                ISSUER_DID.toString(),
                "test-oid4vci-config-id",
                null, null,
                VCFormat.JWT_VC,
                null, null
        );

        oid4vciScope = registerOptionalClientScope(oid4vciScope);
        ClientRepresentation testClient = testRealm.admin().clients().findByClientId(OID4VCI_CLIENT_ID).get(0);
        ClientResource clientResource = testRealm.admin().clients().get(testClient.getId());

        try {
            clientResource.addDefaultClientScope(oid4vciScope.getId());
            fail("Expected BadRequestException when trying to assign OID4VCI scope as Default");
        } catch (BadRequestException e) {
            OAuth2ErrorRepresentation error = e.getResponse().readEntity(OAuth2ErrorRepresentation.class);
            assertEquals("OID4VCI client scopes cannot be assigned as Default scopes. Only Optional scope assignment is supported.",
                    error.getErrorDescription());
        }
    }

    @Test
    public void testCannotAssignOid4vciScopeAsDefaultToRealm() {
        ClientScopeRepresentation oid4vciScope = createOptionalClientScope(
                "test-oid4vci-realm-scope",
                ISSUER_DID.toString(),
                "test-oid4vci-realm-config-id",
                null, null,
                VCFormat.JWT_VC,
                null, null
        );

        oid4vciScope = registerOptionalClientScope(oid4vciScope);

        try {
            testRealm.admin().addDefaultDefaultClientScope(oid4vciScope.getId());
            fail("Expected BadRequestException when trying to assign OID4VCI scope as realm Default");
        } catch (BadRequestException e) {
            OAuth2ErrorRepresentation error = e.getResponse().readEntity(OAuth2ErrorRepresentation.class);
            assertEquals("OID4VCI client scopes cannot be assigned as Default scopes. Only Optional scope assignment is supported.",
                    error.getErrorDescription());
        }
    }

    @Test
    public void testCannotAssignOid4vciScopeWhenRealmDisabled() {
        ClientScopeRepresentation oid4vciScope = createOptionalClientScope(
                "test-oid4vci-disabled-scope",
                ISSUER_DID.toString(),
                "test-oid4vci-disabled-config-id",
                null, null,
                VCFormat.JWT_VC,
                null, null
        );

        oid4vciScope = registerOptionalClientScope(oid4vciScope);

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            realm.setVerifiableCredentialsEnabled(false);
        });

        try {
            ClientRepresentation testClient = testRealm.admin().clients().findByClientId(OID4VCI_CLIENT_ID).get(0);
            ClientResource clientResource = testRealm.admin().clients().get(testClient.getId());

            try {
                clientResource.addOptionalClientScope(oid4vciScope.getId());
                fail("Expected BadRequestException when OID4VCI is disabled at realm level");
            } catch (BadRequestException e) {
                OAuth2ErrorRepresentation error = e.getResponse().readEntity(OAuth2ErrorRepresentation.class);
                assertEquals("OID4VCI client scopes cannot be assigned when Verifiable Credentials is disabled for the realm",
                        error.getErrorDescription());
            }
        } finally {
            runOnServer.run(session -> {
                RealmModel realm = session.getContext().getRealm();
                realm.setVerifiableCredentialsEnabled(true);
            });
        }
    }

    @Test
    public void testCredentialOfferDifferentNoncesIndependent() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialScope.getName());
        final String credentialConfigurationId = jwtTypeCredentialScope.getAttributes()
                .get(CredentialScopeModel.VC_CONFIGURATION_ID);

        // 1. Create first credential offer
        CredentialOfferURI credentialOfferURI1 = oauth.oid4vc()
                .credentialOfferUriRequest(credentialConfigurationId)
                .preAuthorized(false)
                .targetUser("john")
                .bearerToken(token)
                .send()
                .getCredentialOfferURI();

        assertNotNull(credentialOfferURI1, "First credential offer URI should not be null");
        String nonce1 = credentialOfferURI1.getNonce();
        assertNotNull(nonce1, "First nonce should not be null");

        // 2. Create second credential offer (should have different nonce)
        CredentialOfferURI credentialOfferURI2 = oauth.oid4vc()
                .credentialOfferUriRequest(credentialConfigurationId)
                .preAuthorized(false)
                .targetUser("john")
                .bearerToken(token)
                .send()
                .getCredentialOfferURI();

        assertNotNull(credentialOfferURI2, "Second credential offer URI should not be null");
        String nonce2 = credentialOfferURI2.getNonce();
        assertNotNull(nonce2, "Second nonce should not be null");
        assertNotEquals(nonce1, nonce2, "Nonces should be different for different offers");

        // 3. Access first offer - should succeed
        CredentialsOffer credentialsOffer1 = oauth.oid4vc()
                .credentialOfferRequest(nonce1)
                .bearerToken(token)
                .send()
                .getCredentialsOffer();
        assertNotNull(credentialsOffer1, "First credential offer should not be null");

        // 4. Access second offer - should also succeed (different nonce, independent state)
        CredentialsOffer credentialsOffer2 = oauth.oid4vc()
                .credentialOfferRequest(nonce2)
                .bearerToken(token)
                .send()
                .getCredentialsOffer();
        assertNotNull(credentialsOffer2, "Second credential offer should not be null");
    }

    @Test
    public void testCredentialIssuerSigningDefaultKeyAndAlg() {
        KeysMetadataRepresentation keyMetadata = testRealm.admin().keys().getKeyMetadata();

        String kid = "";
        String alg = "";
        jwtTypeCredentialScope.getAttributes().put(CredentialScopeModel.VC_SIGNING_KEY_ID, kid);
        jwtTypeCredentialScope.getAttributes().put(CredentialScopeModel.VC_SIGNING_ALG, alg);
        testRealm.admin().clientScopes().get(jwtTypeCredentialScope.getId()).update(jwtTypeCredentialScope);
        testCredentialIssuanceSigningConfiguration(keyMetadata.getActive().get(Constants.DEFAULT_SIGNATURE_ALGORITHM), Constants.DEFAULT_SIGNATURE_ALGORITHM);
    }

    @Test
    public void testCredentialIssuerSigningSpecificAlg() {
        String kid = "";
        String alg = Algorithm.ES256;
        jwtTypeCredentialScope.getAttributes().put(CredentialScopeModel.VC_SIGNING_KEY_ID, kid);
        jwtTypeCredentialScope.getAttributes().put(CredentialScopeModel.VC_SIGNING_ALG, alg);
        testRealm.admin().clientScopes().get(jwtTypeCredentialScope.getId()).update(jwtTypeCredentialScope);
        testCredentialIssuanceSigningConfiguration(null, Algorithm.ES256);
    }

    @Test
    public void testCredentialIssuerSigningSpecificKey() {
        KeysMetadataRepresentation keyMetadata = testRealm.admin().keys().getKeyMetadata();

        String kid = keyMetadata.getActive().get(Constants.DEFAULT_SIGNATURE_ALGORITHM);
        String alg = Constants.DEFAULT_SIGNATURE_ALGORITHM;
        jwtTypeCredentialScope.getAttributes().put(CredentialScopeModel.VC_SIGNING_KEY_ID, kid);
        jwtTypeCredentialScope.getAttributes().put(CredentialScopeModel.VC_SIGNING_ALG, alg);
        testRealm.admin().clientScopes().get(jwtTypeCredentialScope.getId()).update(jwtTypeCredentialScope);
        testCredentialIssuanceSigningConfiguration(keyMetadata.getActive().get(Constants.DEFAULT_SIGNATURE_ALGORITHM), Constants.DEFAULT_SIGNATURE_ALGORITHM);
    }

    @Test
    public void testCredentialIssuerUsesDefaultTokenJwsTypeForJwtFormatWhenUnset() {
        assertDefaultTokenJwsTypeWhenUnset(
                jwtTypeCredentialScope,
                VCFormat.JWT_VC,
                CredentialScopeModel.VC_BUILD_CONFIG_TOKEN_JWS_TYPE_DEFAULT_JWT_VC
        );
    }

    @Test
    public void testCredentialIssuerUsesDefaultTokenJwsTypeForSdJwtFormatWhenUnset() {
        assertDefaultTokenJwsTypeWhenUnset(
                sdJwtTypeCredentialScope,
                VCFormat.SD_JWT_VC,
                CredentialScopeModel.VC_BUILD_CONFIG_TOKEN_JWS_TYPE_DEFAULT_SD_JWT_VC
        );
    }

    @Test
    public void testCredentialIssuerUsesDefaultTokenJwsTypeForJwtFormatWhenBlank() {
        assertDefaultTokenJwsTypeWhenBlank(
                jwtTypeCredentialScope,
                VCFormat.JWT_VC,
                CredentialScopeModel.VC_BUILD_CONFIG_TOKEN_JWS_TYPE_DEFAULT_JWT_VC
        );
    }

    @Test
    public void testCredentialIssuerUsesDefaultTokenJwsTypeForSdJwtFormatWhenBlank() {
        assertDefaultTokenJwsTypeWhenBlank(
                sdJwtTypeCredentialScope,
                VCFormat.SD_JWT_VC,
                CredentialScopeModel.VC_BUILD_CONFIG_TOKEN_JWS_TYPE_DEFAULT_SD_JWT_VC
        );
    }

    public void testCredentialIssuanceSigningConfiguration(String kid, String alg) {
        String scopeName = jwtTypeCredentialScope.getName();
        String credConfigId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);
        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        testCredentialIssuanceWithAuthZCodeFlow(jwtTypeCredentialScope,
                (testScope) -> {
                    String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
                    return getBearerToken(oauth, authCode, authDetail).getAccessToken();
                },
                m -> {
                    String accessToken = (String) m.get("accessToken");
                    WebTarget credentialTarget = (WebTarget) m.get("credentialTarget");
                    CredentialRequest credentialRequest = (CredentialRequest) m.get("credentialRequest");
                    String issuer = getRealmPath(testRealm.getName());
                    String cNonce = getCNonce();
                    Proofs proofs = jwtProofs(issuer, cNonce);
                    credentialRequest.setProofs(proofs);

                    try (Response response = credentialTarget.request()
                            .header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken)
                            .post(Entity.json(credentialRequest))) {
                        assertEquals(200, response.getStatus());
                        CredentialResponse credentialResponse = JsonSerialization.readValue(response.readEntity(String.class),
                                CredentialResponse.class);
                        JWSHeader jwsHeader = TokenVerifier.create((String) credentialResponse.getCredentials().get(0).getCredential(),
                                JsonWebToken.class).getHeader();

                        if (kid != null) {
                            assertEquals(jwsHeader.getKeyId(), kid);
                        }
                        assertEquals(jwsHeader.getRawAlgorithm(), alg);

                    } catch (VerificationException | IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void assertDefaultTokenJwsTypeWhenUnset(
            ClientScopeRepresentation clientScope,
            String expectedFormat,
            String expectedTyp
    ) {
        String originalTyp = clientScope.getAttributes().remove(CredentialScopeModel.VC_BUILD_CONFIG_TOKEN_JWS_TYPE);
        testRealm.admin().clientScopes().get(clientScope.getId()).update(clientScope);

        try {
            String actualTyp = issueCredentialAndGetTyp(clientScope, expectedFormat);
            assertEquals(expectedTyp, actualTyp, "Expected typ default for format " + expectedFormat);
        } finally {
            if (originalTyp == null) {
                clientScope.getAttributes().remove(CredentialScopeModel.VC_BUILD_CONFIG_TOKEN_JWS_TYPE);
            } else {
                clientScope.getAttributes().put(CredentialScopeModel.VC_BUILD_CONFIG_TOKEN_JWS_TYPE, originalTyp);
            }
            testRealm.admin().clientScopes().get(clientScope.getId()).update(clientScope);
        }
    }

    private void assertDefaultTokenJwsTypeWhenBlank(
            ClientScopeRepresentation clientScope,
            String expectedFormat,
            String expectedTyp
    ) {
        String originalTyp = clientScope.getAttributes().get(CredentialScopeModel.VC_BUILD_CONFIG_TOKEN_JWS_TYPE);
        clientScope.getAttributes().put(CredentialScopeModel.VC_BUILD_CONFIG_TOKEN_JWS_TYPE, " ");
        testRealm.admin().clientScopes().get(clientScope.getId()).update(clientScope);

        try {
            String actualTyp = issueCredentialAndGetTyp(clientScope, expectedFormat);
            assertEquals(expectedTyp, actualTyp, "Expected typ default for format " + expectedFormat);
        } finally {
            if (originalTyp == null) {
                clientScope.getAttributes().remove(CredentialScopeModel.VC_BUILD_CONFIG_TOKEN_JWS_TYPE);
            } else {
                clientScope.getAttributes().put(CredentialScopeModel.VC_BUILD_CONFIG_TOKEN_JWS_TYPE, originalTyp);
            }
            testRealm.admin().clientScopes().get(clientScope.getId()).update(clientScope);
        }
    }

    private String issueCredentialAndGetTyp(ClientScopeRepresentation clientScope, String expectedFormat) {
        AtomicReference<String> typRef = new AtomicReference<>();

        testCredentialIssuanceWithAuthZCodeFlow(
                clientScope,
                (testScope) -> getBearerToken(oauth, client, testScope),
                m -> {
                    String accessToken = (String) m.get("accessToken");
                    WebTarget credentialTarget = (WebTarget) m.get("credentialTarget");
                    CredentialRequest credentialRequest = (CredentialRequest) m.get("credentialRequest");
                    String issuer = getRealmPath(testRealm.getName());
                    String cNonce = getCNonce();
                    credentialRequest.setProofs(jwtProofs(issuer, cNonce));

                    try (Response response = credentialTarget.request()
                            .header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken)
                            .post(Entity.json(credentialRequest))) {
                        assertEquals(200, response.getStatus(), "Credential request should succeed");
                        CredentialResponse credentialResponse = JsonSerialization.readValue(
                                response.readEntity(String.class),
                                CredentialResponse.class
                        );
                        String credentialValue = (String) credentialResponse.getCredentials().get(0).getCredential();
                        if (VCFormat.SD_JWT_VC.equals(expectedFormat)) {
                            credentialValue = credentialValue.split(SDJWT_DELIMITER)[0];
                        }
                        JWSHeader header = TokenVerifier.create(credentialValue, JsonWebToken.class).getHeader();
                        typRef.set(header.getType());
                    } catch (VerificationException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        return typRef.get();
    }

    private String getCNonce() {
        UriBuilder builder = UriBuilder.fromUri(keycloakUrls.getBase());
        URI oid4vcUri = RealmsResource.protocolUrl(builder)
                .build(testRealm.getName(), OID4VCLoginProtocolFactory.PROTOCOL_ID);
        String nonceUrl = String.format("%s/%s", oid4vcUri.toString(), OID4VCIssuerEndpoint.NONCE_PATH);

        String nonceResponseString;

        // request cNonce
        try (Client restClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
            WebTarget nonceTarget = restClient.target(nonceUrl);
            Invocation.Builder nonceInvocationBuilder = nonceTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, null)
                    .header(HttpHeaders.COOKIE, null);

            try (Response response = nonceInvocationBuilder.post(null)) {
                assertEquals(HttpStatus.SC_OK, response.getStatus());
                assertTrue(response.getMediaType().toString().startsWith(MediaType.APPLICATION_JSON_TYPE.toString()));

                nonceResponseString = parseResponse(response);
                assertNotNull(nonceResponseString);
                assertEquals("no-store", response.getHeaderString(HttpHeaders.CACHE_CONTROL));
            }
        }

        NonceResponse nonceResponse;
        try {
            nonceResponse = JsonSerialization.readValue(nonceResponseString, NonceResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return nonceResponse.getNonce();
    }

    private static String parseResponse(Response response) {
        try {
            return response.readEntity(String.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String withModifiedHeaderClaim(String jwt, String claim, Object value) {
        try {
            String[] parts = jwt.split("\\.");
            Map<String, Object> header = JsonSerialization.readValue(Base64Url.decode(parts[0]), new TypeReference<>() {
            });
            header.put(claim, value);
            parts[0] = Base64Url.encode(JsonSerialization.writeValueAsString(header).getBytes(StandardCharsets.UTF_8));
            return String.join(".", parts);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String withPrivateJwkMaterialInHeader(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            Map<String, Object> header = JsonSerialization.readValue(Base64Url.decode(parts[0]), new TypeReference<>() {
            });
            Map<String, Object> jwk = JsonSerialization.mapper.convertValue(header.get("jwk"), new TypeReference<>() {
            });
            jwk.put("d", "fake-private-material");
            header.put("jwk", jwk);
            parts[0] = Base64Url.encode(JsonSerialization.writeValueAsString(header).getBytes(StandardCharsets.UTF_8));
            return String.join(".", parts);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateJwtProofWithEmbeddedAttestation(
            KeyWrapper proofKey,
            String attestationJwt,
            String cNonce,
            String audience,
            boolean useKidHeader
    ) {
        try {
            JWK proofJwk = JWKBuilder.create().ec(proofKey.getPublicKey());
            proofJwk.setKeyId(proofKey.getKid());
            proofJwk.setAlgorithm(proofKey.getAlgorithm());

            AccessToken token = new AccessToken();
            token.addAudience(audience);
            token.setNonce(cNonce);
            token.issuedNow();

            Map<String, Object> header = Map.of(
                    "alg", proofKey.getAlgorithm(),
                    "typ", "openid4vci-proof+jwt",
                    "key_attestation", attestationJwt,
                    useKidHeader ? "kid" : "jwk",
                    useKidHeader ? proofKey.getKid() : proofJwk
            );

            return new JWSBuilder() {
                @Override
                protected String encodeHeader(String sigAlgName) {
                    try {
                        return Base64Url.encode(JsonSerialization.writeValueAsBytes(header));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to encode JWT proof header", e);
                    }
                }
            }.jsonContent(token).sign(new ECDSASignatureSignerContext(proofKey));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT proof with key_attestation", e);
        }
    }

    private static String generateAttestationProofWithoutExp(KeyWrapper attestationKey, String nonce, List<JWK> attestedKeys) {
        KeyAttestationJwtBody body = new KeyAttestationJwtBody();
        body.setIat(System.currentTimeMillis() / 1000L);
        body.setNonce(nonce);
        body.setAttestedKeys(attestedKeys);
        body.setKeyStorage(List.of("iso_18045_high"));
        body.setUserAuthentication(List.of("iso_18045_high"));

        return new JWSBuilder()
                .type(AttestationValidatorUtil.ATTESTATION_JWT_TYP)
                .kid(attestationKey.getKid())
                .jsonContent(body)
                .sign(new ECDSASignatureSignerContext(attestationKey));
    }

    private Map<String, String> prepareJwtCredentialRequestContext() {
        String scopeName = jwtTypeCredentialScope.getName();
        String credentialConfigurationId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);
        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credentialConfigurationId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String credentialIdentifier = tokenResponse.getOID4VCAuthorizationDetails().get(0).getCredentialIdentifiers().get(0);
        return Map.of(
                "token", tokenResponse.getAccessToken(),
                "credentialIdentifier", credentialIdentifier,
                "cNonce", getCNonce()
        );
    }

    private static void assertSingleCredentialResponse(Response response) {
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Response status should be OK");
        CredentialResponse credentialResponse = JsonSerialization.mapper
                .convertValue(response.getEntity(), CredentialResponse.class);
        assertNotNull(credentialResponse);
        assertNotNull(credentialResponse.getCredentials());
        assertEquals(1, credentialResponse.getCredentials().size());
    }

}
