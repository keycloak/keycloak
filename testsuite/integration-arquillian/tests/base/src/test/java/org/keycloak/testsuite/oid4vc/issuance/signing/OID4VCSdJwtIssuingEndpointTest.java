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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.TokenVerifier;
import org.keycloak.VCFormat;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailResponse;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.JwtCredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.SdJwtCredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.keybinding.CNonceHandler;
import org.keycloak.protocol.oid4vc.issuance.keybinding.JwtCNonceHandler;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCGeneratedIdMapper;
import org.keycloak.protocol.oid4vc.model.Claim;
import org.keycloak.protocol.oid4vc.model.Claims;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.OAuth2Constants.OPENID_CREDENTIAL;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_SUBJECT_ID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Endpoint test with sd-jwt specific config.
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class OID4VCSdJwtIssuingEndpointTest extends OID4VCIssuerEndpointTest {

    @Test
    public void testRequestTestCredential() {
        String scopeName = sdJwtTypeCredentialClientScope.getName();
        String credConfigId = sdJwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);
        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetailResponse> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

        final String clientScopeString = toJsonString(sdJwtTypeCredentialClientScope);

        testingClient
                .server(TEST_REALM_NAME)
                .run(session -> {
                    ClientScopeRepresentation clientScope = fromJsonString(clientScopeString,
                            ClientScopeRepresentation.class);
                    testRequestTestCredential(session, clientScope, token, null, credentialIdentifier);
                });
    }

    @Test
    public void testRequestTestCredentialWithKeybinding() {
        String cNonce = getCNonce();
        String scopeName = sdJwtTypeCredentialClientScope.getName();
        String credConfigId = sdJwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);
        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetailResponse> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

        final String clientScopeString = toJsonString(sdJwtTypeCredentialClientScope);

        testingClient
                .server(TEST_REALM_NAME)
                .run((session -> {
                    Proofs proof = new Proofs()
                            .setJwt(List.of(generateJwtProof(getCredentialIssuer(session), cNonce)));

                    ClientScopeRepresentation clientScope = fromJsonString(clientScopeString,
                            ClientScopeRepresentation.class);

                    SdJwtVP sdJwtVP = testRequestTestCredential(session, clientScope, token, proof, credentialIdentifier);
                    assertNotNull("A cnf claim must be attached to the credential", sdJwtVP.getCnfClaim());
                }));
    }

    @Test
    public void testRequestTestCredentialWithInvalidKeybinding() throws Throwable {
        String cNonce = getCNonce();
        String scopeName = sdJwtTypeCredentialClientScope.getName();
        String credConfigId = sdJwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);
        CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

        String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
        String token = tokenResponse.getAccessToken();
        List<OID4VCAuthorizationDetailResponse> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

        final String clientScopeString = toJsonString(sdJwtTypeCredentialClientScope);

        try {
            withCausePropagation(() -> {
                testingClient.server(TEST_REALM_NAME).run((session -> {
                    Proofs proof = new Proofs()
                            .setJwt(List.of(generateInvalidJwtProof(getCredentialIssuer(session), cNonce)));

                    ClientScopeRepresentation clientScope = fromJsonString(clientScopeString,
                            ClientScopeRepresentation.class);

                    testRequestTestCredential(session, clientScope, token, proof, credentialIdentifier);
                }));
            });
            Assert.fail("Should have thrown an exception");
        } catch (BadRequestException ex) {
            Assert.assertEquals("Could not validate provided proof", ex.getMessage());
        }
    }

    @Test
    public void testProofOfPossessionWithMissingAudience() throws Throwable {
        try {
            String scopeName = sdJwtTypeCredentialClientScope.getName();
            String credConfigId = sdJwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);
            CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
            OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
            authDetail.setType(OPENID_CREDENTIAL);
            authDetail.setCredentialConfigurationId(credConfigId);
            authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

            String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
            org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
            String token = tokenResponse.getAccessToken();
            List<OID4VCAuthorizationDetailResponse> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
            String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

            final String clientScopeString = toJsonString(sdJwtTypeCredentialClientScope);

            withCausePropagation(() -> testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        CNonceHandler cNonceHandler = session.getProvider(CNonceHandler.class);
                        final String nonceEndpoint = OID4VCIssuerWellKnownProvider.getNonceEndpoint(session.getContext());
                        // creates a cNonce with missing data
                        String cNonce = cNonceHandler.buildCNonce(null,
                                                                  Map.of(JwtCNonceHandler.SOURCE_ENDPOINT,
                                                                         nonceEndpoint));
                        Proofs proof = new Proofs().setJwt(List.of(generateJwtProof(getCredentialIssuer(session), cNonce)));

                        ClientScopeRepresentation clientScope = fromJsonString(clientScopeString,
                                ClientScopeRepresentation.class);
                        testRequestTestCredential(session, clientScope, token, proof, credentialIdentifier);
                    })));
            Assert.fail("Should have thrown an exception");
        } catch (BadRequestException ex) {
            Assert.assertEquals("""
                                        c_nonce: expected 'aud' to be equal to \
                                        '[https://localhost:8543/auth/realms/test/protocol/oid4vc/credential]' but \
                                        actual value was '[]'""",
                    ExceptionUtils.getRootCause(ex).getMessage());
            Assert.assertEquals("Could not validate provided proof", ex.getMessage());
        }
    }

    @Test
    public void testProofOfPossessionWithIllegalSourceEndpoint() throws Throwable {
        try {
            String scopeName = sdJwtTypeCredentialClientScope.getName();
            String credConfigId = sdJwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);
            CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
            OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
            authDetail.setType(OPENID_CREDENTIAL);
            authDetail.setCredentialConfigurationId(credConfigId);
            authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

            String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
            org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
            String token = tokenResponse.getAccessToken();
            List<OID4VCAuthorizationDetailResponse> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
            String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

            final String clientScopeString = toJsonString(sdJwtTypeCredentialClientScope);

            withCausePropagation(() -> testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        CNonceHandler cNonceHandler = session.getProvider(CNonceHandler.class);
                        final String credentialsEndpoint = //
                                OID4VCIssuerWellKnownProvider.getCredentialsEndpoint(session.getContext());
                        // creates a cNonce with missing data
                        String cNonce = cNonceHandler.buildCNonce(List.of(credentialsEndpoint), null);
                        Proofs proof = new Proofs().setJwt(List.of(generateJwtProof(getCredentialIssuer(session), cNonce)));

                        ClientScopeRepresentation clientScope = fromJsonString(clientScopeString,
                                ClientScopeRepresentation.class);
                        testRequestTestCredential(session, clientScope, token, proof, credentialIdentifier);
                    })));
            Assert.fail("Should have thrown an exception");
        } catch (BadRequestException ex) {
            Assert.assertEquals("""
                                        c_nonce: expected 'source_endpoint' to be equal to \
                                        'https://localhost:8543/auth/realms/test/protocol/oid4vc/nonce' but \
                                        actual value was 'null'""",
                    ExceptionUtils.getRootCause(ex).getMessage());
            Assert.assertEquals("Could not validate provided proof", ex.getMessage());
        }
    }

    @Test
    public void testProofOfPossessionWithExpiredState() throws Throwable {
        try {
            String scopeName = sdJwtTypeCredentialClientScope.getName();
            String credConfigId = sdJwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);
            CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
            OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
            authDetail.setType(OPENID_CREDENTIAL);
            authDetail.setCredentialConfigurationId(credConfigId);
            authDetail.setLocations(List.of(credentialIssuer.getCredentialIssuer()));

            String authCode = getAuthorizationCode(oauth, client, "john", scopeName);
            org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = getBearerToken(oauth, authCode, authDetail);
            String token = tokenResponse.getAccessToken();
            List<OID4VCAuthorizationDetailResponse> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
            String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

            final String clientScopeString = toJsonString(sdJwtTypeCredentialClientScope);

            withCausePropagation(() -> testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        CNonceHandler cNonceHandler = session.getProvider(CNonceHandler.class);
                        final String credentialsEndpoint = //
                                OID4VCIssuerWellKnownProvider.getCredentialsEndpoint(session.getContext());
                        final String nonceEndpoint = OID4VCIssuerWellKnownProvider.getNonceEndpoint(session.getContext());
                        try {
                            // make the exp-value negative to set the exp-time in the past
                            session.getContext().getRealm().setAttribute(OID4VCIConstants.C_NONCE_LIFETIME_IN_SECONDS, -1);
                            String cNonce = cNonceHandler.buildCNonce(List.of(credentialsEndpoint),
                                                                      Map.of(JwtCNonceHandler.SOURCE_ENDPOINT, nonceEndpoint));
                            Proofs proof = new Proofs().setJwt(List.of(generateJwtProof(getCredentialIssuer(session), cNonce)));

                            ClientScopeRepresentation clientScope = fromJsonString(clientScopeString,
                                    ClientScopeRepresentation.class);
                            testRequestTestCredential(session, clientScope, token, proof, credentialIdentifier);
                        } finally {
                            // make sure other tests are not affected by the changed realm-attribute
                            session.getContext().getRealm().removeAttribute(OID4VCIConstants.C_NONCE_LIFETIME_IN_SECONDS);
                        }
                    })));
            Assert.fail("Should have thrown an exception");
        } catch (BadRequestException ex) {
            String message = ExceptionUtils.getRootCause(ex).getMessage();
            Assert.assertTrue(String.format("Message '%s' should match regular expression", message),
                    message.matches("c_nonce not valid: \\d+\\(exp\\) < \\d+\\(now\\)"));
            Assert.assertEquals("Could not validate provided proof", ex.getMessage());
        }
    }

    protected static String getCredentialIssuer(KeycloakSession session) {
        return OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
    }

    private static SdJwtVP testRequestTestCredential(KeycloakSession session, ClientScopeRepresentation clientScope,

                                                     String token, Proofs proof, String credentialIdentifier)
            throws VerificationException, IOException {

        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
        authenticator.setTokenString(token);
        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

        CredentialRequest credentialRequest = new CredentialRequest()
                .setCredentialIdentifier(credentialIdentifier)
                .setProofs(proof);

        String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

        Response credentialResponse = issuerEndpoint.requestCredential(requestPayload);
        assertEquals("The credential request should be answered successfully.",

                HttpStatus.SC_OK,
                credentialResponse.getStatus());
        assertNotNull("A credential should be responded.", credentialResponse.getEntity());
        CredentialResponse credentialResponseVO = JsonSerialization.mapper.convertValue(credentialResponse.getEntity(),
                CredentialResponse.class);
        new TestCredentialResponseHandler(sdJwtCredentialVct).handleCredentialResponse(credentialResponseVO,
                clientScope);

        // Get the credential from the credentials array
        return SdJwtVP.of(credentialResponseVO.getCredentials().get(0).getCredential().toString());
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

        ClientScopeRepresentation clientScope = sdJwtTypeCredentialClientScope;
        String token = getBearerToken(oauth, client, clientScope.getName());

        // 1. Retrieving the credential-offer-uri
        final String credentialConfigurationId = clientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);
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
        List<OID4VCAuthorizationDetailResponse> authDetailsResponse = accessTokenResponse.getOid4vcAuthorizationDetails();
        assertNotNull("authorization_details should be present in the response", authDetailsResponse);
        assertFalse("authorization_details should not be empty", authDetailsResponse.isEmpty());
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);
        assertNotNull("Credential identifier should be present", credentialIdentifier);

        final String vct = clientScope.getAttributes().get(CredentialScopeModel.VCT);

        // 6. Get the credential using credential_identifier (required when authorization_details are present)
        credentialsOffer.getCredentialConfigurationIds().stream()
                .map(offeredCredentialId -> credentialIssuer.getCredentialsSupported().get(offeredCredentialId))
                .forEach(supportedCredential -> {
                    try {
                        requestCredentialWithIdentifier(theToken,
                                credentialIssuer.getCredentialEndpoint(),
                                credentialIdentifier,
                                new TestCredentialResponseHandler(vct),
                                sdJwtTypeCredentialClientScope);
                    } catch (IOException e) {
                        fail("Was not able to get the credential.");
                    } catch (VerificationException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * This is testing the configuration exposed by OID4VCIssuerWellKnownProvider based on the client and signing config setup here.
     */
    @Test
    public void testGetSdJwtConfigFromMetadata() {
        final String scopeName = sdJwtTypeCredentialClientScope.getName();
        final String credentialConfigurationId = sdJwtTypeCredentialClientScope.getAttributes()
                .get(CredentialScopeModel.CONFIGURATION_ID);
        final String verifiableCredentialType = sdJwtTypeCredentialClientScope.getAttributes()
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

                    assertTrue("The sd-jwt-credential should be supported.",
                            credentialIssuer.getCredentialsSupported().containsKey(credentialConfigurationId));

                    SupportedCredentialConfiguration jwtVcConfig =
                            credentialIssuer.getCredentialsSupported().get(credentialConfigurationId);
                    assertEquals("The sd-jwt-credential should offer type test-credential",
                            scopeName,
                            jwtVcConfig.getScope());
                    assertEquals("The sd-jwt-credential should be offered in the jwt_vc format.",
                            VCFormat.SD_JWT_VC,
                            jwtVcConfig.getFormat());

                    assertNotNull("The sd-jwt-credential can optionally provide a claims claim.",
                                  credentialIssuer.getCredentialsSupported().get(credentialConfigurationId)
                                          .getCredentialMetadata() != null ?
                                          credentialIssuer.getCredentialsSupported().get(credentialConfigurationId)
                                                  .getCredentialMetadata().getClaims() : null);

                    Claims jwtVcClaims = jwtVcConfig.getCredentialMetadata() != null ? jwtVcConfig.getCredentialMetadata().getClaims() : null;
                    assertNotNull("The sd-jwt-credential can optionally provide a claims claim.",
                            jwtVcClaims);

                    assertEquals(7,  jwtVcClaims.size());
                    {
                        Claim claim = jwtVcClaims.get(0);
                        assertEquals("id claim is present", CLAIM_NAME_SUBJECT_ID, claim.getPath().get(0));
                        assertFalse("id claim not mandatory.", claim.isMandatory());
                        assertNull("id has no display value", claim.getDisplay());
                    }
                    {
                        Claim claim = jwtVcClaims.get(1);
                        assertEquals("email claim is present", "email", claim.getPath().get(0));
                        assertFalse("email claim not mandatory.", claim.isMandatory());
                        assertNull("email has no display value", claim.getDisplay());
                    }
                    {
                        Claim claim = jwtVcClaims.get(2);
                        assertEquals("firstName claim is present", "firstName", claim.getPath().get(0));
                        assertFalse("firstName claim not mandatory.", claim.isMandatory());
                        assertNull("firstName has no display value", claim.getDisplay());
                    }
                    {
                        Claim claim = jwtVcClaims.get(3);
                        assertEquals("lastName claim is present", "lastName", claim.getPath().get(0));
                        assertFalse("lastName claim not mandatory.", claim.isMandatory());
                        assertNull("lastName has no display value", claim.getDisplay());
                    }
                    {
                        Claim claim = jwtVcClaims.get(4);
                        assertEquals("address.street_address claim is nested.",
                                2,
                                claim.getPath().size());
                        assertEquals("address.street_address claim has correct parent claim name.",
                                "address",
                                claim.getPath().get(0));
                        assertEquals("address.street_address claim has correct nested claim name.",
                                "street_address",
                                claim.getPath().get(1));
                        assertFalse("address.street_address claim is not mandatory.",
                                claim.isMandatory());
                        assertNull("address.street_address claim has no display value",
                                claim.getDisplay());
                    }
                    {
                        Claim claim = jwtVcClaims.get(5);
                        assertEquals("address.locality claim is nested.",
                                2,
                                claim.getPath().size());
                        assertEquals("address.locality claim has correct parent claim name.",
                                "address",
                                claim.getPath().get(0));
                        assertEquals("address.locality claim has correct nested claim name.",
                                "locality",
                                claim.getPath().get(1));
                        assertFalse("address.locality claim is not mandatory.",
                                claim.isMandatory());
                        assertNull("address.locality claim has no display value",
                                claim.getDisplay());
                    }
                    {
                        Claim claim = jwtVcClaims.get(6);
                        assertEquals("scope-name claim is present", "scope-name", claim.getPath().get(0));
                        assertFalse("scope-name claim not mandatory.", claim.isMandatory());
                        assertNull("scope-name has no display value", claim.getDisplay());
                    }

                    assertEquals("The sd-jwt-credential should offer vct",
                            verifiableCredentialType,
                            credentialIssuer.getCredentialsSupported().get(credentialConfigurationId).getVct());

                    // We are offering key binding only for identity credential
                    assertTrue("The sd-jwt-credential should contain a cryptographic binding method supported named jwk",
                            credentialIssuer.getCredentialsSupported().get(credentialConfigurationId)
                                    .getCryptographicBindingMethodsSupported()
                                    .contains(CredentialScopeModel.CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT));
                    assertTrue("The sd-jwt-credential should contain a credential signing algorithm named ES256",
                            credentialIssuer.getCredentialsSupported().get(credentialConfigurationId)
                                    .getCredentialSigningAlgValuesSupported().contains("ES256"));
                    assertTrue("The sd-jwt-credential should support a proof of type jwt with signing algorithm ES256",
                            credentialIssuer.getCredentialsSupported()
                                    .get(credentialConfigurationId)
                                    .getProofTypesSupported()
                                    .getSupportedProofTypes()
                                    .get("jwt")
                                    .getSigningAlgorithmsSupported()
                                    .contains("ES256"));
                    assertEquals("The sd-jwt-credential should display as Test Credential",
                                 credentialConfigurationId,
                                 credentialIssuer.getCredentialsSupported().get(credentialConfigurationId)
                                         .getCredentialMetadata() != null &&
                                         credentialIssuer.getCredentialsSupported().get(credentialConfigurationId)
                                                 .getCredentialMetadata().getDisplay() != null ?
                                         credentialIssuer.getCredentialsSupported().get(credentialConfigurationId)
                                                 .getCredentialMetadata().getDisplay().get(0).getName() : null);
                }));
    }

    protected static OID4VCIssuerEndpoint prepareIssuerEndpoint(KeycloakSession session,
                                                                AppAuthManager.BearerTokenAuthenticator authenticator) {
        JwtCredentialBuilder testJwtCredentialBuilder = new JwtCredentialBuilder(new StaticTimeProvider(5), session);
        SdJwtCredentialBuilder testSdJwtCredentialBuilder = new SdJwtCredentialBuilder();

        return new OID4VCIssuerEndpoint(
                session,
                Map.of(
                        testSdJwtCredentialBuilder.getSupportedFormat(), testSdJwtCredentialBuilder,
                        testJwtCredentialBuilder.getSupportedFormat(), testJwtCredentialBuilder
                ),
                authenticator,
                TIME_PROVIDER,
                30);
    }

    private static final String JTI_KEY = "jti";

    public static ProtocolMapperRepresentation getJtiGeneratedIdMapper() {
        ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
        protocolMapperRepresentation.setName("generated-id-mapper");
        protocolMapperRepresentation.setProtocol(OID4VCIConstants.OID4VC_PROTOCOL);
        protocolMapperRepresentation.setId(UUID.randomUUID().toString());
        protocolMapperRepresentation.setProtocolMapper("oid4vc-generated-id-mapper");
        protocolMapperRepresentation.setConfig(Map.of(
                OID4VCGeneratedIdMapper.CLAIM_NAME, JTI_KEY
        ));
        return protocolMapperRepresentation;
    }

    public static ClientScopeModel createCredentialScope(KeycloakSession session) {
        RealmModel realmModel = session.getContext().getRealm();
        ClientScopeModel credentialScope = session.clientScopes()
                .addClientScope(realmModel, jwtTypeCredentialScopeName);
        credentialScope.setAttribute(CredentialScopeModel.CREDENTIAL_IDENTIFIER,
                jwtTypeCredentialScopeName);
        credentialScope.setProtocol(OID4VCIConstants.OID4VC_PROTOCOL);
        return credentialScope;
    }

    @Override
    protected ComponentExportRepresentation getKeyProvider() {
        return getEcKeyProvider();
    }

    static class TestCredentialResponseHandler extends CredentialResponseHandler {
        final String vct;

        TestCredentialResponseHandler(String vct) {
            this.vct = vct;
        }

        @Override
        protected void handleCredentialResponse(CredentialResponse credentialResponse, ClientScopeRepresentation clientScope) throws VerificationException {
            // SDJWT have a special format.
            SdJwtVP sdJwtVP = SdJwtVP.of(credentialResponse.getCredentials().get(0).getCredential().toString());
            JsonWebToken jsonWebToken = TokenVerifier.create(sdJwtVP.getIssuerSignedJWT().getJws(), JsonWebToken.class).getToken();

            assertNotNull("A valid credential string should have been responded", jsonWebToken);
            assertNotNull("The credentials should include the id claim", jsonWebToken.getId());
            assertNotNull("The credentials should be included at the vct-claim.", jsonWebToken.getOtherClaims().get("vct"));
            assertEquals("The credentials should be included at the vct-claim.", vct, jsonWebToken.getOtherClaims().get("vct").toString());

            Map<String, JsonNode> disclosureMap = sdJwtVP.getDisclosures().values().stream()
                    .map(disclosure -> {
                        try {
                            JsonNode jsonNode = JsonSerialization.mapper.readTree(Base64Url.decode(disclosure));
                            return Map.entry(jsonNode.get(1).asText(), jsonNode); // Create a Map.Entry
                        } catch (IOException e) {
                            throw new RuntimeException(e); // Re-throw as unchecked exception
                        }
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            assertFalse("Only mappers supported for the requested type should have been evaluated.", disclosureMap.containsKey("given_name"));
            assertTrue("The credentials should include the firstName claim.", disclosureMap.containsKey("firstName"));
            assertEquals("firstName claim incorrectly mapped.", "John", disclosureMap.get("firstName").get(2).asText());
            assertTrue("The credentials should include the lastName claim.", disclosureMap.containsKey("lastName"));
            assertEquals("lastName claim incorrectly mapped.", "Doe", disclosureMap.get("lastName").get(2).asText());

            assertThat("address is parent claim for nested claims", disclosureMap.get("address").get(2), instanceOf(ObjectNode.class));
            ObjectNode nestedAddressClaim = (ObjectNode) disclosureMap.get("address").get(2);
            assertEquals("address contains two nested claims", 2, nestedAddressClaim.size());
            assertEquals("street_address mapped correctly", "221B Baker Street", nestedAddressClaim.get("street_address").asText());
            assertEquals("locality mapped correctly", "London", nestedAddressClaim.get("locality").asText());

            assertTrue("The credentials should include the scope-name claim.",
                    disclosureMap.containsKey("scope-name"));
            assertEquals("The credentials should include the scope-name claims correct value.",
                    clientScope.getName(),
                    disclosureMap.get("scope-name").get(2).textValue());
            assertTrue("The credentials should include the email claim.", disclosureMap.containsKey("email"));
            assertEquals("email claim incorrectly mapped.", "john@email.cz", disclosureMap.get("email").get(2).asText());

            assertNotNull("Test credential shall include an iat claim.", jsonWebToken.getIat());
            assertNotNull("Test credential shall include an nbf claim.", jsonWebToken.getNbf());

        }
    }
}
