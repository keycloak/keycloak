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
package org.keycloak.testsuite.oid4vc.issuance.signing;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.TokenVerifier;
import org.keycloak.VCFormat;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;
import org.keycloak.models.RealmModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage.CredentialOfferState;
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
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCode;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedGrant;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.managers.AppAuthManager.BearerTokenAuthenticator;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;
import org.keycloak.util.JsonSerialization;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.OID4VCConstants.CREDENTIAL_SUBJECT;
import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test from org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCIssuerEndpointTest
 */
public class OID4VCJWTIssuerEndpointTest extends OID4VCIssuerEndpointTest {

    // ----- getCredentialOfferUri

    @Test
    public void testGetCredentialOfferUriUnsupportedCredential() {
        String token = getBearerToken(oauth);
        testingClient.server(TEST_REALM_NAME).run((session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);

            OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CorsErrorResponseException exception = Assert.assertThrows(CorsErrorResponseException.class, () ->
                    oid4VCIssuerEndpoint.getCredentialOfferURI("inexistent-id")
            );
            assertEquals("Should return BAD_REQUEST", Response.Status.BAD_REQUEST.getStatusCode(),
                    exception.getResponse().getStatus());
        }));
    }

    @Test
    public void testGetCredentialOfferUriUnauthorized() {
        testingClient.server(TEST_REALM_NAME).run((session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString(null);
            OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CorsErrorResponseException exception = Assert.assertThrows(CorsErrorResponseException.class, () ->
                    oid4VCIssuerEndpoint.getCredentialOfferURI("test-credential", true, "john")
            );
            assertEquals("Should return BAD_REQUEST", Response.Status.BAD_REQUEST.getStatusCode(),
                    exception.getResponse().getStatus());
        }));
    }

    @Test
    public void testGetCredentialOfferUriInvalidToken() {
        testingClient.server(TEST_REALM_NAME).run((session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString("invalid-token");
            OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CorsErrorResponseException exception = Assert.assertThrows(CorsErrorResponseException.class, () ->
                    oid4VCIssuerEndpoint.getCredentialOfferURI("test-credential", true, "john")
            );
            assertEquals("Should return BAD_REQUEST", Response.Status.BAD_REQUEST.getStatusCode(),
                    exception.getResponse().getStatus());
        }));
    }

    @Test
    public void testGetCredentialOfferURI() {
        final String scopeName = jwtTypeCredentialClientScope.getName();
        final String credentialConfigurationId = jwtTypeCredentialClientScope.getAttributes()
                .get(CredentialScopeModel.CONFIGURATION_ID);
        String token = getBearerToken(oauth, client, scopeName);

        testingClient.server(TEST_REALM_NAME).run((session) -> {
            try {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                Response response = oid4VCIssuerEndpoint.getCredentialOfferURI(credentialConfigurationId);

                assertEquals("An offer uri should have been returned.", HttpStatus.SC_OK, response.getStatus());
                CredentialOfferURI credentialOfferURI = JsonSerialization.mapper.convertValue(response.getEntity(),
                        CredentialOfferURI.class);
                assertNotNull("A nonce should be included.", credentialOfferURI.getNonce());
                assertNotNull("The issuer uri should be provided.", credentialOfferURI.getIssuer());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // ----- getCredentialOffer

    @Test(expected = BadRequestException.class)
    public void testGetCredentialOfferUnauthorized() throws Throwable {
        withCausePropagation(() -> {
            testingClient
                    .server(TEST_REALM_NAME)
                    .run((session) -> {
                        BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                        authenticator.setTokenString(null);
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        Response response = issuerEndpoint.getCredentialOffer("nonce");
                        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
                    });
        });
    }

    @Test(expected = BadRequestException.class)
    public void testGetCredentialOfferWithoutNonce() throws Throwable {
        String token = getBearerToken(oauth);
        withCausePropagation(() -> {
            testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                        authenticator.setTokenString(token);
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        issuerEndpoint.getCredentialOffer(null);
                    }));
        });
    }

    @Test(expected = BadRequestException.class)
    public void testGetCredentialOfferWithoutAPreparedOffer() throws Throwable {
        String token = getBearerToken(oauth);
        withCausePropagation(() -> {
            testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                        authenticator.setTokenString(token);
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        issuerEndpoint.getCredentialOffer("unpreparedNonce");
                    }));
        });
    }

    @Test(expected = BadRequestException.class)
    public void testGetCredentialOfferWithABrokenNote() throws Throwable {
        String token = getBearerToken(oauth);
        withCausePropagation(() -> {
            testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                        authenticator.setTokenString(token);
                        String nonce = prepareSessionCode(session, authenticator, "invalidNote").key();
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        issuerEndpoint.getCredentialOffer(nonce);
                    }));
        });
    }

    @Test
    public void testGetCredentialOffer() {
        String token = getBearerToken(oauth);
        testingClient
                .server(TEST_REALM_NAME)
                .run((session) -> {
                    BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                    authenticator.setTokenString(token);
                    CredentialsOffer credOffer = new CredentialsOffer()
                            .setCredentialIssuer("the-issuer")
                            .setGrants(new PreAuthorizedGrant().setPreAuthorizedCode(new PreAuthorizedCode().setPreAuthorizedCode("the-code")))
                            .setCredentialConfigurationIds(List.of("credential-configuration-id"));

                    CredentialOfferStorage offerStorage = session.getProvider(CredentialOfferStorage.class);
                    CredentialOfferState offerState = new CredentialOfferState(credOffer, null, null, Time.currentTime() + 60);
                    offerStorage.putOfferState(session, offerState);

                    // The cache transactions need to be committed explicitly in the test. Without that, the OAuth2Code will only be committed to
                    // the cache after .run((session)-> ...)
                    session.getTransactionManager().commit();
                    OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                    Response credentialOfferResponse = issuerEndpoint.getCredentialOffer(offerState.getNonce());
                    assertEquals("The offer should have been returned.", HttpStatus.SC_OK, credentialOfferResponse.getStatus());
                    Object credentialOfferEntity = credentialOfferResponse.getEntity();
                    assertNotNull("An actual offer should be in the response.", credentialOfferEntity);

                    CredentialsOffer retrievedCredentialsOffer = JsonSerialization.mapper.convertValue(credentialOfferEntity, CredentialsOffer.class);
                    assertEquals("The offer should be the one prepared with for the session.", credOffer, retrievedCredentialsOffer);
                });
    }

// ----- requestCredential

    @Test
    public void testRequestCredentialUnauthorized() {
        testingClient.server(TEST_REALM_NAME).run(session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString(null);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialIdentifier("test-credential");

            String requestPayload;
            requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

            CorsErrorResponseException exception = Assert.assertThrows(CorsErrorResponseException.class, () ->
                    issuerEndpoint.requestCredential(requestPayload)
            );
            assertEquals("Should return BAD_REQUEST", Response.Status.BAD_REQUEST.getStatusCode(),
                    exception.getResponse().getStatus());
        });
    }

    @Test
    public void testRequestCredentialInvalidToken() {
        testingClient.server(TEST_REALM_NAME).run(session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString("token");
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialIdentifier("test-credential");

            String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

            CorsErrorResponseException exception = Assert.assertThrows(CorsErrorResponseException.class, () ->
                    issuerEndpoint.requestCredential(requestPayload)
            );
            assertEquals("Should return BAD_REQUEST", Response.Status.BAD_REQUEST.getStatusCode(),
                    exception.getResponse().getStatus());
        });
    }

    @Test
    public void testRequestCredentialNoMatchingCredentialBuilder() throws Throwable {
        final String credentialConfigurationId = jwtTypeCredentialClientScope.getAttributes()
                .get(CredentialScopeModel.CONFIGURATION_ID);
        final String scopeName = jwtTypeCredentialClientScope.getName();

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credentialConfigurationId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

        try {
            withCausePropagation(() -> {
                testingClient.server(TEST_REALM_NAME).run((session -> {
                    BearerTokenAuthenticator authenticator =
                            new BearerTokenAuthenticator(session);
                    authenticator.setTokenString(token);

                    // Prepare the issue endpoint with no credential builders.
                    OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator, Map.of());

                    CredentialRequest credentialRequest =
                            new CredentialRequest().setCredentialIdentifier(credentialIdentifier);

                    String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
                    issuerEndpoint.requestCredential(requestPayload);
                }));
            });
            Assert.fail("Should have thrown an exception");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof BadRequestException);
            Assert.assertEquals("No credential builder found for format jwt_vc_json", e.getMessage());
        }
    }

    @Test(expected = BadRequestException.class)
    public void testRequestCredentialUnsupportedCredential() throws Throwable {
        String token = getBearerToken(oauth);
        withCausePropagation(() -> {
            testingClient.server(TEST_REALM_NAME).run(session -> {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                CredentialRequest credentialRequest = new CredentialRequest()
                        .setCredentialIdentifier("no-such-credential");

                String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

                issuerEndpoint.requestCredential(requestPayload);
            });
        });
    }

    @Test
    public void testRequestCredential() {
        String scopeName = jwtTypeCredentialClientScope.getName();
        String credConfigId = jwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);
        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        assertNotNull("authorization_details should be present in the response", authDetailsResponse);
        assertFalse("authorization_details should not be empty", authDetailsResponse.isEmpty());
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);
        assertNotNull("credential_identifier should be present", credentialIdentifier);

        testingClient.server(TEST_REALM_NAME).run(session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialIdentifier(credentialIdentifier);

            String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

            Response credentialResponse = issuerEndpoint.requestCredential(requestPayload);
            assertEquals("The credential request should be answered successfully.",
                    HttpStatus.SC_OK, credentialResponse.getStatus());
            assertNotNull("A credential should be responded.", credentialResponse.getEntity());
            CredentialResponse credentialResponseVO = JsonSerialization.mapper
                    .convertValue(credentialResponse.getEntity(), CredentialResponse.class);
            JsonWebToken jsonWebToken;
            try {
                jsonWebToken = TokenVerifier.create((String) credentialResponseVO.getCredentials().get(0).getCredential(),
                        JsonWebToken.class).getToken();
            } catch (VerificationException e) {
                Assert.fail("Failed to verify JWT: " + e.getMessage());
                return;
            }
            assertNotNull("A valid credential string should have been responded", jsonWebToken);
            assertNotNull("The credentials should be included at the vc-claim.",
                    jsonWebToken.getOtherClaims().get("vc"));
            VerifiableCredential credential = JsonSerialization.mapper.convertValue(
                    jsonWebToken.getOtherClaims().get("vc"), VerifiableCredential.class);
            assertTrue("The static claim should be set.",
                    credential.getCredentialSubject().getClaims().containsKey("scope-name"));
            assertEquals("The static claim should be set.",
                    scopeName, credential.getCredentialSubject().getClaims().get("scope-name"));
            assertFalse("Only mappers supported for the requested type should have been evaluated.",
                    credential.getCredentialSubject().getClaims().containsKey("AnotherCredentialType"));
        });
    }

    @Test
    public void testRequestCredentialWithNeitherIdSet() {
        final String scopeName = minimalJwtTypeCredentialClientScope.getName();
        String token = getBearerToken(oauth, client, scopeName);
        testingClient.server(TEST_REALM_NAME).run(session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
            CredentialRequest credentialRequest = new CredentialRequest();
            try {
                String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException due to missing credential identifier or configuration id");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_CREDENTIAL_REQUEST, error.getError());
            }
        });
    }

    // Tests the complete flow from
    // 1. Retrieving the credential-offer-uri
    // 2. Using the uri to get the actual credential offer
    // 3. Get the issuer metadata
    // 4. Get the openid-configuration
    // 5. Get an access token for the pre-authorized code
    // 6. Get the credential
    @Test
    public void testCredentialIssuance() throws Exception {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());

        // 1. Retrieving the credential-offer-uri
        final String credentialConfigurationId = jwtTypeCredentialClientScope.getAttributes()
                .get(CredentialScopeModel.CONFIGURATION_ID);
        CredentialOfferURI credentialOfferURI = oauth.oid4vc()
                .credentialOfferUriRequest(credentialConfigurationId)
                .preAuthorized(true)
                .username("john")
                .bearerToken(token)
                .send()
                .getCredentialOfferURI();

        assertNotNull("A valid offer uri should be returned", credentialOfferURI);

        // 2. Using the uri to get the actual credential offer
        CredentialsOffer credentialsOffer = oauth.oid4vc()
                .credentialOfferRequest(credentialOfferURI)
                .bearerToken(token)
                .send()
                .getCredentialsOffer();

        assertNotNull("A valid offer should be returned", credentialsOffer);

        // 3. Get the issuer metadata
        CredentialIssuer credentialIssuer = oauth.oid4vc()
                .issuerMetadataRequest()
                .endpoint(credentialsOffer.getIssuerMetadataUrl())
                .send()
                .getMetadata();

        assertNotNull("Issuer metadata should be returned", credentialIssuer);
        assertEquals("We only expect one authorization server.", 1, credentialIssuer.getAuthorizationServers().size());

        // 4. Get the openid-configuration
        OIDCConfigurationRepresentation openidConfig = oauth
                .wellknownRequest()
                .url(credentialIssuer.getAuthorizationServers().get(0))
                .send()
                .getOidcConfiguration();

        assertNotNull("A token endpoint should be included.", openidConfig.getTokenEndpoint());
        assertTrue("The pre-authorized code should be supported.", openidConfig.getGrantTypesSupported().contains(PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));

        // 5. Get an access token for the pre-authorized code
        AccessTokenResponse accessTokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode())
                .endpoint(openidConfig.getTokenEndpoint())
                .send();

        assertEquals(HttpStatus.SC_OK, accessTokenResponse.getStatusCode());
        String theToken = accessTokenResponse.getAccessToken();
        assertNotNull("Access token should be present", theToken);

        // Extract credential_identifier from authorization_details in token response
        List<OID4VCAuthorizationDetail> authDetailsResponse = accessTokenResponse.getOid4vcAuthorizationDetails();
        assertNotNull("authorization_details should be present in the response", authDetailsResponse);
        assertFalse("authorization_details should not be empty", authDetailsResponse.isEmpty());
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);
        assertNotNull("Credential identifier should be present", credentialIdentifier);

        // 6. Get the credential using credential_identifier (required when authorization_details are present)
        credentialsOffer.getCredentialConfigurationIds().stream()
                .map(offeredCredentialId -> credentialIssuer.getCredentialsSupported().get(offeredCredentialId))
                .forEach(supportedCredential -> {
                    try {
                        requestCredentialWithIdentifier(theToken,
                                credentialIssuer.getCredentialEndpoint(),
                                credentialIdentifier,
                                new CredentialResponseHandler(),
                                jwtTypeCredentialClientScope);
                    } catch (IOException e) {
                        fail("Was not able to get the credential.");
                    } catch (VerificationException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    public void testCredentialIssuanceWithAuthZCodeWithScopeMatched() throws Exception {
        String scopeName = jwtTypeCredentialClientScope.getName();
        String credConfigId = jwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);
        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        testCredentialIssuanceWithAuthZCodeFlow(jwtTypeCredentialClientScope,
                (testClientId, testScope) -> {
                    String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
                    return getBearerToken(oauth, authCode, authDetail).getAccessToken();
                },
                m -> {
                    String accessToken = (String) m.get("accessToken");
                    WebTarget credentialTarget = (WebTarget) m.get("credentialTarget");
                    CredentialRequest credentialRequest = (CredentialRequest) m.get("credentialRequest");

                    try (Response response = credentialTarget.request()
                            .header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken)
                            .post(Entity.json(credentialRequest))) {
                        assertEquals(200, response.getStatus());
                        CredentialResponse credentialResponse = JsonSerialization.readValue(response.readEntity(String.class),
                                CredentialResponse.class);

                        JsonWebToken jsonWebToken = TokenVerifier.create((String) credentialResponse.getCredentials().get(0).getCredential(),
                                JsonWebToken.class).getToken();
                        assertEquals(TEST_DID.toString(), jsonWebToken.getIssuer());

                        VerifiableCredential credential = JsonSerialization.mapper.convertValue(jsonWebToken.getOtherClaims()
                                        .get("vc"),
                                VerifiableCredential.class);
                        assertEquals(List.of(jwtTypeCredentialClientScope.getName()), credential.getType());
                        assertEquals(TEST_DID, credential.getIssuer());
                        assertEquals("john@email.cz", credential.getCredentialSubject().getClaims().get("email"));
                    } catch (VerificationException | IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    public void testCredentialIssuanceWithAuthZCodeWithScopeUnmatched() throws Exception {
        testCredentialIssuanceWithAuthZCodeFlow(sdJwtTypeCredentialClientScope, (testClientId, testScope) ->
                        getBearerToken(oauth.clientId(testClientId).openid(false).scope("email")),// set registered different scope
                m -> {
                    String accessToken = (String) m.get("accessToken");
                    WebTarget credentialTarget = (WebTarget) m.get("credentialTarget");
                    CredentialRequest credentialRequest = (CredentialRequest) m.get("credentialRequest");

                    try (Response response = credentialTarget.request().header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken).post(Entity.json(credentialRequest))) {
                        assertEquals(400, response.getStatus());
                    }
                });
    }

    @Test
    public void testCredentialIssuanceWithAuthZCodeSWithoutScope() throws Exception {
        testCredentialIssuanceWithAuthZCodeFlow(sdJwtTypeCredentialClientScope,
                (testClientId, testScope) -> getBearerToken(oauth.clientId(testClientId).openid(false).scope(null)),// no scope
                m -> {
                    String accessToken = (String) m.get("accessToken");
                    WebTarget credentialTarget = (WebTarget) m.get("credentialTarget");
                    CredentialRequest credentialRequest = (CredentialRequest) m.get("credentialRequest");

                    try (Response response = credentialTarget.request().header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken).post(Entity.json(credentialRequest))) {
                        assertEquals(400, response.getStatus());
                    }
                });
    }

    /**
     * The accessToken references the scope "test-credential" but we ask for the credential "VerifiableCredential"
     * in the CredentialRequest
     */
    @Test
    public void testCredentialIssuanceWithScopeUnmatched() {
        BiFunction<String, String, String> getAccessToken = (testClientId, testScope) -> {
            return getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        };

        Consumer<Map<String, Object>> sendCredentialRequest = m -> {
            String accessToken = (String) m.get("accessToken");
            WebTarget credentialTarget = (WebTarget) m.get("credentialTarget");
            CredentialRequest credentialRequest = (CredentialRequest) m.get("credentialRequest");

            try (Response response = credentialTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken)
                    .post(Entity.json(credentialRequest))) {
                assertEquals(400, response.getStatus());
                String errorJson = response.readEntity(String.class);
                assertNotNull("Error response should not be null", errorJson);
                assertTrue("Error response should mention INVALID_CREDENTIAL_REQUEST or scope",
                        errorJson.contains("INVALID_CREDENTIAL_REQUEST") || errorJson.contains("scope"));
            }
        };

        testCredentialIssuanceWithAuthZCodeFlow(sdJwtTypeCredentialClientScope, getAccessToken, sendCredentialRequest);
    }

    /**
     * This is testing the multiple credential issuance flow in a single call with proofs
     */
    @Test
    public void testRequestMultipleCredentialsWithProofs() {
        final String scopeName = jwtTypeCredentialClientScope.getName();
        String credConfigId = jwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

        String cNonce = getCNonce();

        testingClient.server(TEST_REALM_NAME).run(session -> {
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
                assertEquals("Response status should be OK", Response.Status.OK.getStatusCode(), response.getStatus());

                CredentialResponse credentialResponse = JsonSerialization.mapper
                        .convertValue(response.getEntity(), CredentialResponse.class);
                assertNotNull("Credential response should not be null", credentialResponse);
                assertNotNull("Credentials array should not be null", credentialResponse.getCredentials());
                assertEquals("Should return 2 credentials due to two proofs", 2, credentialResponse.getCredentials().size());

                // Validate each credential
                for (CredentialResponse.Credential credential : credentialResponse.getCredentials()) {
                    assertNotNull("Credential should not be null", credential.getCredential());
                    JsonWebToken jsonWebToken;
                    try {
                        jsonWebToken = TokenVerifier.create((String) credential.getCredential(), JsonWebToken.class).getToken();
                    } catch (VerificationException e) {
                        Assert.fail("Failed to verify JWT: " + e.getMessage());
                        return;
                    }
                    assertNotNull("A valid credential string should be returned", jsonWebToken);
                    assertNotNull("The credentials should include the vc claim", jsonWebToken.getOtherClaims().get("vc"));

                    VerifiableCredential vc = JsonSerialization.mapper.convertValue(
                            jsonWebToken.getOtherClaims().get("vc"), VerifiableCredential.class);
                    assertTrue("The scope-name claim should be set",
                            vc.getCredentialSubject().getClaims().containsKey("scope-name"));
                    assertEquals("The scope-name claim should match the scope",
                            scopeName, vc.getCredentialSubject().getClaims().get("scope-name"));
                    assertTrue("The given_name claim should be set",
                            vc.getCredentialSubject().getClaims().containsKey("given_name"));
                    assertEquals("The given_name claim should be John",
                            "John", vc.getCredentialSubject().getClaims().get("given_name"));
                    assertTrue("The email claim should be set",
                            vc.getCredentialSubject().getClaims().containsKey("email"));
                    assertEquals("The email claim should be john@email.cz",
                            "john@email.cz", vc.getCredentialSubject().getClaims().get("email"));
                    assertFalse("Only supported mappers should be evaluated",
                            vc.getCredentialSubject().getClaims().containsKey("AnotherCredentialType"));
                }
            } catch (Exception e) {
                throw new RuntimeException("Test failed due to: " + e.getMessage(), e);
            }
        });
    }

    /**
     * This is testing the configuration exposed by OID4VCIssuerWellKnownProvider based on the client and signing config setup here.
     */
    @Test
    public void testGetJwtVcConfigFromMetadata() {
        final String scopeName = jwtTypeCredentialClientScope.getName();
        final String credentialConfigurationId = jwtTypeCredentialClientScope.getAttributes()
                .get(CredentialScopeModel.CONFIGURATION_ID);
        final String verifiableCredentialType = jwtTypeCredentialClientScope.getAttributes()
                .get(CredentialScopeModel.VCT);
        String expectedIssuer = suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/" + TEST_REALM_NAME;
        String expectedCredentialsEndpoint = expectedIssuer + "/protocol/oid4vc/credential";
        String expectedNonceEndpoint = expectedIssuer + "/protocol/oid4vc/" + OID4VCIssuerEndpoint.NONCE_PATH;
        final String expectedAuthorizationServer = expectedIssuer;
        testingClient
                .server(TEST_REALM_NAME)
                .run((session -> {
                    OID4VCIssuerWellKnownProvider oid4VCIssuerWellKnownProvider = new OID4VCIssuerWellKnownProvider(session);
                    CredentialIssuer credentialIssuer = oid4VCIssuerWellKnownProvider.getIssuerMetadata();
                    assertEquals("The correct issuer should be included.", expectedIssuer, credentialIssuer.getCredentialIssuer());
                    assertEquals("The correct credentials endpoint should be included.", expectedCredentialsEndpoint, credentialIssuer.getCredentialEndpoint());
                    assertEquals("The correct nonce endpoint should be included.",
                            expectedNonceEndpoint,
                            credentialIssuer.getNonceEndpoint());
                    assertEquals("Since the authorization server is equal to the issuer, just 1 should be returned.", 1, credentialIssuer.getAuthorizationServers().size());
                    assertEquals("The expected server should have been returned.", expectedAuthorizationServer, credentialIssuer.getAuthorizationServers().get(0));

                    assertTrue("The jwt_vc-credential should be supported.",
                            credentialIssuer.getCredentialsSupported()
                                    .containsKey(credentialConfigurationId));

                    SupportedCredentialConfiguration jwtVcConfig =
                            credentialIssuer.getCredentialsSupported().get(credentialConfigurationId);
                    assertEquals("The jwt_vc-credential should offer type test-credential",
                            scopeName,
                            jwtVcConfig.getScope());
                    assertEquals("The jwt_vc-credential should be offered in the jwt_vc format.",
                            VCFormat.JWT_VC,
                            jwtVcConfig.getFormat());

                    Claims jwtVcClaims = jwtVcConfig.getCredentialMetadata() != null ? jwtVcConfig.getCredentialMetadata().getClaims() : null;
                    assertNotNull("The jwt_vc-credential can optionally provide a claims claim.",
                            jwtVcClaims);

                    assertEquals(8, jwtVcClaims.size());
                    {
                        Claim claim = jwtVcClaims.get(0);
                        assertEquals("Has credentialSubject.id", CREDENTIAL_SUBJECT, claim.getPath().get(0));
                        assertEquals("credentialSubject.id mapped correctly","id", claim.getPath().get(1));
                        assertFalse("credentialSubject.id is not mandatory", claim.isMandatory());
                        assertNull("credentialSubject.id has no display", claim.getDisplay());
                    }
                    {
                        Claim claim = jwtVcClaims.get(1);
                        assertEquals("Has credentialSubject.given_name", CREDENTIAL_SUBJECT, claim.getPath().get(0));
                        assertEquals("credentialSubject.given_name mapped correctly","given_name", claim.getPath().get(1));
                        assertFalse("credentialSubject.given_name is not mandatory", claim.isMandatory());
                        assertNotNull("credentialSubject.given_name has display", claim.getDisplay());
                        assertEquals(15, claim.getDisplay().size());
                        for (ClaimDisplay givenNameDisplay : claim.getDisplay()) {
                            assertNotNull(givenNameDisplay.getName());
                            assertNotNull(givenNameDisplay.getLocale());
                        }
                    }
                    {
                        Claim claim = jwtVcClaims.get(2);
                        assertEquals("Has credentialSubject.family_name", CREDENTIAL_SUBJECT, claim.getPath().get(0));
                        assertEquals("credentialSubject.family_name mapped correctly","family_name", claim.getPath().get(1));
                        assertFalse("credentialSubject.family_name is not mandatory", claim.isMandatory());
                        assertNotNull("credentialSubject.family_name has display", claim.getDisplay());
                        assertEquals(15, claim.getDisplay().size());
                        for (ClaimDisplay familyNameDisplay : claim.getDisplay()) {
                            assertNotNull(familyNameDisplay.getName());
                            assertNotNull(familyNameDisplay.getLocale());
                        }
                    }
                    {
                        Claim claim = jwtVcClaims.get(3);
                        assertEquals("Has credentialSubject.birthdate", CREDENTIAL_SUBJECT, claim.getPath().get(0));
                        assertEquals("credentialSubject.birthdate mapped correctly","birthdate", claim.getPath().get(1));
                        assertFalse("credentialSubject.birthdate is not mandatory", claim.isMandatory());
                        assertNotNull("credentialSubject.birthdate has display", claim.getDisplay());
                        assertEquals(15, claim.getDisplay().size());
                        for (ClaimDisplay birthDateDisplay : claim.getDisplay()) {
                            assertNotNull(birthDateDisplay.getName());
                            assertNotNull(birthDateDisplay.getLocale());
                        }
                    }
                    {
                        Claim claim = jwtVcClaims.get(4);
                        assertEquals("Has credentialSubject.email", CREDENTIAL_SUBJECT, claim.getPath().get(0));
                        assertEquals("credentialSubject.email mapped correctly","email", claim.getPath().get(1));
                        assertFalse("credentialSubject.email is not mandatory", claim.isMandatory());
                        assertNotNull("credentialSubject.email has display", claim.getDisplay());
                        assertEquals(15, claim.getDisplay().size());
                        for (ClaimDisplay birthDateDisplay : claim.getDisplay()) {
                            assertNotNull(birthDateDisplay.getName());
                            assertNotNull(birthDateDisplay.getLocale());
                        }
                    }
                    {
                        Claim claim = jwtVcClaims.get(5);
                        assertEquals("Has credentialSubject.address_locality", CREDENTIAL_SUBJECT, claim.getPath().get(0));
                        assertEquals(
                                "credentialSubject.address.locality mapped correctly (parent claim in path)",
                                "address",
                                claim.getPath().get(1)
                        );
                        assertEquals(
                                "credentialSubject.address.locality mapped correctly (nested claim in path)",
                                "locality",
                                claim.getPath().get(2)
                        );
                        assertFalse("credentialSubject.address.locality is not mandatory", claim.isMandatory());
                        assertNull("credentialSubject.address.locality has no display", claim.getDisplay());
                    }
                    {
                        Claim claim = jwtVcClaims.get(6);
                        assertEquals("Has credentialSubject.address.street_address", CREDENTIAL_SUBJECT, claim.getPath().get(0));
                        assertEquals(
                                "credentialSubject.address.street_address mapped correctly (parent claim in path)",
                                "address",
                                claim.getPath().get(1)
                        );
                        assertEquals(
                                "credentialSubject.address.street_address mapped correctly (nested claim in path)",
                                "street_address",
                                claim.getPath().get(2)
                        );
                        assertFalse("credentialSubject.address.street_address is not mandatory", claim.isMandatory());
                        assertNull("credentialSubject.address.street_address has no display", claim.getDisplay());
                    }
                    {
                        Claim claim = jwtVcClaims.get(7);
                        assertEquals("Has credentialSubject.scope-name", CREDENTIAL_SUBJECT, claim.getPath().get(0));
                        assertEquals("credentialSubject.scope-name mapped correctly","scope-name", claim.getPath().get(1));
                        assertFalse("credentialSubject.scope-name is not mandatory", claim.isMandatory());
                        assertNull("credentialSubject.scope-name has no display", claim.getDisplay());
                    }

                    assertNotNull("The jwt_vc-credential should offer credential_definition",
                            jwtVcConfig.getCredentialDefinition());
                    assertNull("JWT_VC credentials should not have vct", jwtVcConfig.getVct());

                    // We are offering key binding only for identity credential
                    assertTrue("The jwt_vc-credential should contain a cryptographic binding method supported named jwk",
                            jwtVcConfig.getCryptographicBindingMethodsSupported()
                                    .contains(CredentialScopeModel.CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT));
                    assertTrue("The jwt_vc-credential should contain a credential signing algorithm named RS256",
                            jwtVcConfig.getCredentialSigningAlgValuesSupported().contains("RS256"));
                    assertTrue("The jwt_vc-credential should support a proof of type jwt with signing algorithm RS256",
                            credentialIssuer.getCredentialsSupported()
                                    .get(credentialConfigurationId)
                                    .getProofTypesSupported()
                                    .getSupportedProofTypes()
                                    .get("jwt")
                                    .getSigningAlgorithmsSupported()
                                    .contains("RS256"));
                    assertEquals("The jwt_vc-credential should display as Test Credential",
                            credentialConfigurationId,
                            jwtVcConfig.getCredentialMetadata() != null && jwtVcConfig.getCredentialMetadata().getDisplay() != null ?
                                    jwtVcConfig.getCredentialMetadata().getDisplay().get(0).getName() : null);
                }));
    }


    /**
     * Test that unknown_credential_identifier error is returned when credential_identifier is not recognized
     */
    @Test
    public void testRequestCredentialWithUnknownCredentialIdentifier() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());

        testingClient.server(TEST_REALM_NAME).run(session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialIdentifier("unknown-credential-identifier");

            String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

            try {
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException due to unknown credential identifier");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.UNKNOWN_CREDENTIAL_IDENTIFIER, error.getError());
            }
        });
    }

    /**
     * Test that unknown_credential_configuration error is returned when the requested credential_configuration_id does not exist
     */
    @Test
    public void testRequestCredentialWithUnknownCredentialConfigurationId() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());

        testingClient.server(TEST_REALM_NAME).run(session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            // Create a credential request with a non-existent configuration ID
            // This will test the invalid_credential_request error when no authorization_details present
            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialConfigurationId("unknown-configuration-id");

            String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

            try {
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException due to unknown credential configuration");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_CREDENTIAL_REQUEST, error.getError());
            }
        });
    }

    /**
     * Test that unknown_credential_configuration error is returned when a credential configuration exists
     * but there is no credential builder registered for its format.
     */
    @Test
    public void testRequestCredentialWhenNoCredentialBuilderForFormat() {
        String scopeName = jwtTypeCredentialClientScope.getName();
        String credConfigId = jwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);
        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        assertNotNull("authorization_details should be present in the response", authDetailsResponse);
        assertFalse("authorization_details should not be empty", authDetailsResponse.isEmpty());
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);
        assertNotNull("credential_identifier should be present", credentialIdentifier);

        testingClient.server(TEST_REALM_NAME).run(session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            // Prepare endpoint with no credential builders to simulate missing builder for the configured format
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator, Map.of());

            // Use the credential identifier from the token
            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialIdentifier(credentialIdentifier);

            String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

            try {
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException due to missing credential builder for format");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION, error.getError());
            }
        });
    }

    /**
     * Test that verifies the conversion from 'proof' (singular) to 'proofs' (array) works correctly.
     * This test ensures backward compatibility with clients that send 'proof' instead of 'proofs'.
     */
    @Test
    public void testProofToProofsConversion() throws Exception {
        final String scopeName = jwtTypeCredentialClientScope.getName();
        final String credentialConfigurationId = jwtTypeCredentialClientScope.getAttributes()
                .get(CredentialScopeModel.CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credentialConfigurationId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

        testingClient.server(TEST_REALM_NAME).run(session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            // Test 1: Create a request with single proof field - should be converted to proofs array
            CredentialRequest requestWithProof = new CredentialRequest()
                    .setCredentialIdentifier(credentialIdentifier);

            // Create a single proof object
            JwtProof singleProof = new JwtProof()
                    .setJwt("dummy-jwt")
                    .setProofType("jwt");
            requestWithProof.setProof(singleProof);

            String requestPayload = JsonSerialization.writeValueAsString(requestWithProof);

            try {
                // This should work because the conversion happens in validateRequestEncryption
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail();
            } catch (Exception e) {
                // We expect JWT validation to fail, but the conversion should have worked
                assertTrue("Error should be related to JWT validation, not conversion",
                        e.getMessage().contains("Could not validate provided proof"));
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
                Assert.fail("Expected BadRequestException when both proof and proofs are provided");
            } catch (BadRequestException e) {
                int statusCode = e.getResponse().getStatus();
                assertEquals("Expected HTTP 400 Bad Request", 400, statusCode);
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.INVALID_CREDENTIAL_REQUEST, error.getError());
                assertEquals("Both 'proof' and 'proofs' must not be present at the same time",
                        error.getErrorDescription());
            }
        });
    }

    /**
     * Test that credential requests work when the client scope is assigned as Optional.
     */
    @Test
    public void testCredentialRequestWithOptionalClientScope() {
        ClientScopeRepresentation optionalScope = registerOptionalClientScope(
                "optional-jwt-credential",
                TEST_DID.toString(),
                "optional-jwt-credential-config-id",
                null, null,
                VCFormat.JWT_VC,
                null, null);
        
        ClientRepresentation testClient = testRealm().clients().findByClientId(client.getClientId()).get(0);
        testRealm().clients().get(testClient.getId()).addOptionalClientScope(optionalScope.getId());

        // Extract serializable data before lambda
        final String scopeName = optionalScope.getName();
        final String configId = optionalScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);

        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(configId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

        testingClient.server(TEST_REALM_NAME).run(session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialIdentifier(credentialIdentifier);

            String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
            Response credentialResponse = issuerEndpoint.requestCredential(requestPayload);

            assertEquals("The credential request should succeed for Optional client scope",
                    HttpStatus.SC_OK, credentialResponse.getStatus());
            assertNotNull("A credential should be returned", credentialResponse.getEntity());

            CredentialResponse credentialResponseVO = JsonSerialization.mapper
                    .convertValue(credentialResponse.getEntity(), CredentialResponse.class);

            assertNotNull("Credentials array should not be null", credentialResponseVO.getCredentials());
            assertFalse("Credentials array should not be empty", credentialResponseVO.getCredentials().isEmpty());
        });
    }

    /**
     * Test that OID4VCI client scopes cannot be assigned as Default to a client.
     */
    @Test
    public void testCannotAssignOid4vciScopeAsDefaultToClient() {
        ClientScopeRepresentation oid4vciScope = registerOptionalClientScope(
                "test-oid4vci-scope",
                TEST_DID.toString(),
                "test-oid4vci-config-id",
                null, null,
                VCFormat.JWT_VC,
                null, null);
        
        ClientRepresentation testClient = testRealm().clients().findByClientId(client.getClientId()).get(0);
        ClientResource clientResource = testRealm().clients().get(testClient.getId());
        
        try {
            clientResource.addDefaultClientScope(oid4vciScope.getId());
            Assert.fail("Expected BadRequestException when trying to assign OID4VCI scope as Default");
        } catch (BadRequestException e) {
            OAuth2ErrorRepresentation error = e.getResponse().readEntity(OAuth2ErrorRepresentation.class);
            assertEquals("OID4VCI client scopes cannot be assigned as Default scopes. Only Optional scope assignment is supported.",
                    error.getErrorDescription());
        }
    }

    @Test
    public void testCannotAssignOid4vciScopeAsDefaultToRealm() {
        ClientScopeRepresentation oid4vciScope = registerOptionalClientScope(
                "test-oid4vci-realm-scope",
                TEST_DID.toString(),
                "test-oid4vci-realm-config-id",
                null, null,
                VCFormat.JWT_VC,
                null, null);
        
        try {
            testRealm().addDefaultDefaultClientScope(oid4vciScope.getId());
            Assert.fail("Expected BadRequestException when trying to assign OID4VCI scope as realm Default");
        } catch (BadRequestException e) {
            OAuth2ErrorRepresentation error = e.getResponse().readEntity(OAuth2ErrorRepresentation.class);
            assertEquals("OID4VCI client scopes cannot be assigned as Default scopes. Only Optional scope assignment is supported.",
                    error.getErrorDescription());
        }
    }

    /**
     * Test that OID4VCI client scopes cannot be assigned even as Optional when OID4VCI is disabled at realm level.
     */
    @Test
    public void testCannotAssignOid4vciScopeWhenRealmDisabled() {
        ClientScopeRepresentation oid4vciScope = registerOptionalClientScope(
                "test-oid4vci-disabled-scope",
                TEST_DID.toString(),
                "test-oid4vci-disabled-config-id",
                null, null,
                VCFormat.JWT_VC,
                null, null);

        testingClient.server(TEST_REALM_NAME).run(session -> {
            RealmModel realm = session.getContext().getRealm();
            realm.setVerifiableCredentialsEnabled(false);
        });

        try {
            ClientRepresentation testClient = testRealm().clients().findByClientId(client.getClientId()).get(0);
            ClientResource clientResource = testRealm().clients().get(testClient.getId());

            try {
                clientResource.addOptionalClientScope(oid4vciScope.getId());
                Assert.fail("Expected BadRequestException when OID4VCI is disabled at realm level");
            } catch (BadRequestException e) {
                OAuth2ErrorRepresentation error = e.getResponse().readEntity(OAuth2ErrorRepresentation.class);
                assertEquals("OID4VCI client scopes cannot be assigned when Verifiable Credentials is disabled for the realm",
                        error.getErrorDescription());
            }
        } finally {
            testingClient.server(TEST_REALM_NAME).run(session -> {
                RealmModel realm = session.getContext().getRealm();
                realm.setVerifiableCredentialsEnabled(true);
            });
        }
    }

    /**
     * Test that credential offer by reference can only be accessed once (replay protection).
     * This ensures that the credential offer URL with a nonce can only be triggered once,
     * preventing multiple retrievals of the same pre-authorized code.
     */
    @Test
    public void testCredentialOfferReplayProtection() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        final String credentialConfigurationId = jwtTypeCredentialClientScope.getAttributes()
                .get(CredentialScopeModel.CONFIGURATION_ID);

        // 1. Retrieving the credential-offer-uri
        CredentialOfferURI credentialOfferURI = oauth.oid4vc()
                .credentialOfferUriRequest(credentialConfigurationId)
                .preAuthorized(true)
                .username("john")
                .bearerToken(token)
                .send()
                .getCredentialOfferURI();
        assertNotNull("Credential offer URI should not be null", credentialOfferURI);

        String nonce = credentialOfferURI.getNonce();
        assertNotNull("Nonce should not be null", nonce);

        // 2. First access to the credential offer URL - should succeed
        CredentialsOffer credentialsOffer = oauth.oid4vc()
                .credentialOfferRequest(nonce)
                .bearerToken(token)
                .send()
                .getCredentialsOffer();
        assertNotNull("Credential offer should not be null", credentialsOffer);
        assertNotNull("Pre-authorized grant should be present", credentialsOffer.getGrants());
        String preAuthorizedCode = credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode();
        assertNotNull("Pre-authorized code value should not be null", preAuthorizedCode);

        // 3. Second access to the same credential offer URL - should fail with replay protection error
        CredentialOfferResponse response = oauth.oid4vc()
                .credentialOfferRequest(nonce)
                .bearerToken(token)
                .send();

        assertEquals("Second access to credential offer should fail with 400 Bad Request",
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals("Error type should be INVALID_CREDENTIAL_OFFER_REQUEST",
                ErrorType.INVALID_CREDENTIAL_OFFER_REQUEST.name(),
                response.getError());
        assertTrue("Error description should mention that offer is not found or already consumed",
                response.getErrorDescription().contains("not found") || response.getErrorDescription().contains("already consumed"));
    }

    /**
     * Test that different nonces work independently (each offer has its own state).
     * This verifies that replay protection is per-nonce and doesn't affect other offers.
     */
    @Test
    public void testCredentialOfferDifferentNoncesIndependent() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        final String credentialConfigurationId = jwtTypeCredentialClientScope.getAttributes()
                .get(CredentialScopeModel.CONFIGURATION_ID);

        // 1. Create first credential offer
        CredentialOfferURI credentialOfferURI1 = oauth.oid4vc()
                .credentialOfferUriRequest(credentialConfigurationId)
                .preAuthorized(true)
                .username("john")
                .bearerToken(token)
                .send()
                .getCredentialOfferURI();
        assertNotNull("First credential offer URI should not be null", credentialOfferURI1);
        String nonce1 = credentialOfferURI1.getNonce();
        assertNotNull("First nonce should not be null", nonce1);

        // 2. Create second credential offer (should have different nonce)
        CredentialOfferURI credentialOfferURI2 = oauth.oid4vc()
                .credentialOfferUriRequest(credentialConfigurationId)
                .preAuthorized(true)
                .username("john")
                .bearerToken(token)
                .send()
                .getCredentialOfferURI();
        assertNotNull("Second credential offer URI should not be null", credentialOfferURI2);
        String nonce2 = credentialOfferURI2.getNonce();
        assertNotNull("Second nonce should not be null", nonce2);
        assertNotEquals("Nonces should be different for different offers", nonce1, nonce2);

        // 3. Access first offer - should succeed
        CredentialsOffer credentialsOffer1 = oauth.oid4vc()
                .credentialOfferRequest(nonce1)
                .bearerToken(token)
                .send()
                .getCredentialsOffer();
        assertNotNull("First credential offer should not be null", credentialsOffer1);

        // 4. Access second offer - should also succeed (different nonce, independent state)
        CredentialsOffer credentialsOffer2 = oauth.oid4vc()
                .credentialOfferRequest(nonce2)
                .bearerToken(token)
                .send()
                .getCredentialsOffer();
        assertNotNull("Second credential offer should not be null", credentialsOffer2);

        // 5. Verify that accessing first offer again fails (replay protection per-nonce)
        CredentialOfferResponse response1 = oauth.oid4vc()
                .credentialOfferRequest(nonce1)
                .bearerToken(token)
                .send();

        assertEquals("First offer should fail on second access (replay protection)",
                Response.Status.BAD_REQUEST.getStatusCode(), response1.getStatusCode());
        assertEquals("Error type should be INVALID_CREDENTIAL_OFFER_REQUEST",
                ErrorType.INVALID_CREDENTIAL_OFFER_REQUEST.name(),
                response1.getError());

        // 6. Verify that accessing second offer again also fails (replay protection per-nonce)
        CredentialOfferResponse response2 = oauth.oid4vc()
                .credentialOfferRequest(nonce2)
                .bearerToken(token)
                .send();

        assertEquals("Second offer should fail on second access (replay protection)",
                Response.Status.BAD_REQUEST.getStatusCode(), response2.getStatusCode());
        assertEquals("Error type should be INVALID_CREDENTIAL_OFFER_REQUEST",
                ErrorType.INVALID_CREDENTIAL_OFFER_REQUEST.name(),
                response2.getError());
    }

    /**
     * Test that removing the nonce entry (for replay protection) does not invalidate
     * the Pre-Authorized Code. This verifies that the replay protection mechanism doesn't
     * interfere with the normal token request flow using the pre-authorized code.
     */
    @Test
    public void testPreAuthorizedCodeValidAfterOfferConsumed() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        final String credentialConfigurationId = jwtTypeCredentialClientScope.getAttributes()
                .get(CredentialScopeModel.CONFIGURATION_ID);

        // 1. Fetch the Offer URI
        CredentialOfferURI credentialOfferURI = oauth.oid4vc()
                .credentialOfferUriRequest(credentialConfigurationId)
                .preAuthorized(true)
                .username("john")
                .bearerToken(token)
                .send()
                .getCredentialOfferURI();
        assertNotNull("Credential offer URI should not be null", credentialOfferURI);
        String nonce = credentialOfferURI.getNonce();
        assertNotNull("Nonce should not be null", nonce);

        // 2. Fetch the Offer JSON (this removes the nonce entry for replay protection)
        CredentialsOffer credentialsOffer = oauth.oid4vc()
                .credentialOfferRequest(nonce)
                .bearerToken(token)
                .send()
                .getCredentialsOffer();
        assertNotNull("Credential offer should not be null", credentialsOffer);
        assertNotNull("Pre-authorized grant should be present", credentialsOffer.getGrants());
        String preAuthorizedCode = credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode();
        assertNotNull("Pre-authorized code value should not be null", preAuthorizedCode);

        // 3. Immediately perform the Token Request (Pre-Authorized Code Grant) using the valid code
        CredentialIssuer credentialIssuer = oauth.oid4vc()
                .issuerMetadataRequest()
                .endpoint(credentialsOffer.getIssuerMetadataUrl())
                .send()
                .getMetadata();
        OIDCConfigurationRepresentation openidConfig = oauth
                .wellknownRequest()
                .url(credentialIssuer.getAuthorizationServers().get(0))
                .send()
                .getOidcConfiguration();
        assertNotNull("Token endpoint should be present", openidConfig.getTokenEndpoint());

        AccessTokenResponse accessTokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(preAuthorizedCode)
                .send();
        assertEquals("Token request should succeed even after nonce is removed for replay protection",
                HttpStatus.SC_OK,
                accessTokenResponse.getStatusCode());
        assertNotNull("Access token should be present", accessTokenResponse.getAccessToken());
        assertFalse("Access token should not be empty", accessTokenResponse.getAccessToken().isEmpty());
    }
}
