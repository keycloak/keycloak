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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.models.oid4vci.Oid4vcProtocolMapperModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailsResponse;
import org.keycloak.protocol.oid4vc.model.AuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.ClaimsDescription;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.ErrorType;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.OAuth2Constants.OPENID_CREDENTIAL;
import static org.keycloak.models.oid4vci.CredentialScopeModel.SIGNING_ALG;
import static org.keycloak.models.oid4vci.CredentialScopeModel.SIGNING_KEY_ID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Base class for authorization code flow tests with authorization details and claims validation.
 * Contains common test logic that can be reused by JWT and SD-JWT specific test classes.
 *
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public abstract class OID4VCAuthorizationCodeFlowTestBase extends OID4VCIssuerEndpointTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    /**
     * Test context for OID4VC tests
     */
    protected static class Oid4vcTestContext {
        public CredentialIssuer credentialIssuer;
        public OIDCConfigurationRepresentation openidConfig;
    }

    /**
     * Get the credential format (jwt_vc or sd_jwt_vc)
     */
    protected abstract String getCredentialFormat();

    /**
     * Get the credential client scope
     */
    protected abstract ClientScopeRepresentation getCredentialClientScope();

    /**
     * Get the expected claim path for the credential format
     */
    protected abstract String getExpectedClaimPath();

    /**
     * Get the name of the protocol mapper for firstName
     */
    protected abstract String getFirstNameProtocolMapperName();

    /**
     * Prepare OID4VC test context by fetching issuer metadata and credential offer
     */
    protected Oid4vcTestContext prepareOid4vcTestContext() throws Exception {
        Oid4vcTestContext ctx = new Oid4vcTestContext();

        // Get credential issuer metadata
        HttpGet getCredentialIssuer = new HttpGet(getRealmMetadataPath(TEST_REALM_NAME));
        try (CloseableHttpResponse response = httpClient.execute(getCredentialIssuer)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            ctx.credentialIssuer = JsonSerialization.readValue(s, CredentialIssuer.class);
        }

        // Get OpenID configuration
        HttpGet getOpenidConfiguration = new HttpGet(ctx.credentialIssuer.getAuthorizationServers().get(0) + "/.well-known/openid-configuration");
        try (CloseableHttpResponse response = httpClient.execute(getOpenidConfiguration)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            ctx.openidConfig = JsonSerialization.readValue(s, OIDCConfigurationRepresentation.class);
        }

        return ctx;
    }

    // Test for the whole authorization_code flow with the credentialRequest using credential_configuration_id
    @Test
    public void testCompleteFlowWithClaimsValidationAuthorizationCode_credentialRequestWithConfigurationId() throws Exception {
        BiFunction<String, String, CredentialRequest> credRequestSupplier = (credentialConfigurationId, credentialIdentifier) -> {
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setCredentialConfigurationId(credentialConfigurationId);
            return credentialRequest;
        };

        testCompleteFlowWithClaimsValidationAuthorizationCode(credRequestSupplier);
    }

    // Test for the whole authorization_code flow with the credentialRequest using credential_identifier
    @Test
    public void testCompleteFlowWithClaimsValidationAuthorizationCode_credentialRequestWithCredentialIdentifier() throws Exception {
        BiFunction<String, String, CredentialRequest> credRequestSupplier = (credentialConfigurationId, credentialIdentifier) -> {
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setCredentialIdentifier(credentialIdentifier);
            return credentialRequest;
        };

        testCompleteFlowWithClaimsValidationAuthorizationCode(credRequestSupplier);
    }

    // Test for the authorization_code flow with "mandatory" claim specified in the "authorization_details" parameter
    @Test
    public void testCompleteFlow_mandatoryClaimsInAuthzDetailsParameter() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();
        BiFunction<String, String, CredentialRequest> credRequestSupplier = (credentialConfigurationId, credentialIdentifier) -> {
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setCredentialIdentifier(credentialIdentifier);
            return credentialRequest;
        };

        // 1 - Update user to have missing "lastName" (mandatory attribute)
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "john");
        UserRepresentation userRep = user.toRepresentation();
        // NOTE: Need to call both "setLastName" and set attributes to be able to set last name as null
        userRep.setAttributes(Collections.emptyMap());
        userRep.setLastName(null);
        user.update(userRep);

        // 2 - Test the flow. Credential request should fail due the missing "lastName"
        // Perform authorization code flow to get authorization code
        AccessTokenResponse tokenResponse = authzCodeFlow(ctx);
        String credentialIdentifier = assertTokenResponse(tokenResponse);
        String credentialConfigurationId = getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);

        // Clear events before credential request
        events.clear();

        // Request the actual credential using the identifier
        HttpPost postCredential = getCredentialRequest(ctx, credRequestSupplier, tokenResponse, credentialConfigurationId, credentialIdentifier);

        try (CloseableHttpResponse credentialResponse = httpClient.execute(postCredential)) {
            assertErrorCredentialResponse_mandatoryClaimsMissing(credentialResponse);
            
            // Verify VERIFIABLE_CREDENTIAL_REQUEST_ERROR event was fired with details about missing mandatory claim
            events.expect(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                    .client(client.getClientId())
                    .user(AssertEvents.isUUID())
                    .session(AssertEvents.isSessionId())
                    .error(Errors.INVALID_REQUEST)
                    .detail(Details.REASON, Matchers.containsString("The requested claims are not available in the user profile"))
                    .assertEvent();
        }

        // 3 - Update user to add "lastName"
        userRep.setLastName("Doe");
        user.update(userRep);

        // 4 - Test the credential-request again. Should be OK now
        try (CloseableHttpResponse credentialResponse = httpClient.execute(postCredential)) {
            assertSuccessfulCredentialResponse(credentialResponse);
        }
    }


    // Test for the authorization_code flow with "mandatory" claim specified in the "authorization_details" parameter as well as
    // mandatory claims in the protocol mappers configuration
    @Test
    public void testCompleteFlow_mandatoryClaimsInAuthzDetailsParameterAndProtocolMappersConfig() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();
        BiFunction<String, String, CredentialRequest> credRequestSupplier = (credentialConfigurationId, credentialIdentifier) -> {
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setCredentialIdentifier(credentialIdentifier);
            return credentialRequest;
        };

        // 1 - Update "firstName" protocol mapper to be mandatory
        ClientScopeResource clientScopeResource = ApiUtil.findClientScopeByName(testRealm(), getCredentialClientScope().getName());
        assertNotNull(clientScopeResource);
        ProtocolMapperRepresentation protocolMapper = clientScopeResource.getProtocolMappers().getMappers()
                .stream()
                .filter(protMapper -> getFirstNameProtocolMapperName().equals(protMapper.getName()))
                .findFirst()
                .orElseThrow((() -> new RuntimeException("Not found protocol mapper with name 'firstName-mapper'.")));
        protocolMapper.getConfig().put(Oid4vcProtocolMapperModel.MANDATORY, "true");
        clientScopeResource.getProtocolMappers().update(protocolMapper.getId(), protocolMapper);

        try {
            // 2 - Update user to have missing "lastName" (mandatory attribute by authorization_details parameter) and "firstName" (mandatory attribute by protocol mapper)
            UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "john");
            UserRepresentation userRep = user.toRepresentation();
            // NOTE: Need to call both "setLastName" and set attributes to be able to set last name as null
            userRep.setAttributes(Collections.emptyMap());
            userRep.setFirstName(null);
            userRep.setLastName(null);
            user.update(userRep);

            // 2 - Test the flow. Credential request should fail due the missing "lastName"
            // Perform authorization code flow to get authorization code
            AccessTokenResponse tokenResponse = authzCodeFlow(ctx);
            String credentialIdentifier = assertTokenResponse(tokenResponse);
            String credentialConfigurationId = getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);

            // Clear events before credential request
            events.clear();

            // Request the actual credential using the identifier
            HttpPost postCredential = getCredentialRequest(ctx, credRequestSupplier, tokenResponse, credentialConfigurationId, credentialIdentifier);

            try (CloseableHttpResponse credentialResponse = httpClient.execute(postCredential)) {
                assertErrorCredentialResponse_mandatoryClaimsMissing(credentialResponse);
                
                // Verify VERIFIABLE_CREDENTIAL_REQUEST_ERROR event was fired with details about missing mandatory claim
                events.expect(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                        .client(client.getClientId())
                        .user(AssertEvents.isUUID())
                        .session(AssertEvents.isSessionId())
                        .error(Errors.INVALID_REQUEST)
                        .detail(Details.REASON, Matchers.containsString("The requested claims are not available in the user profile"))
                        .assertEvent();
            }

            // 3 - Update user to add "lastName", but keep "firstName" missing. Credential request should still fail
            userRep.setLastName("Doe");
            userRep.setFirstName(null);
            user.update(userRep);

            // Clear events before credential request
            events.clear();

            try (CloseableHttpResponse credentialResponse = httpClient.execute(postCredential)) {
                assertErrorCredentialResponse_mandatoryClaimsMissing(credentialResponse);
                
                // Verify VERIFIABLE_CREDENTIAL_REQUEST_ERROR event was fired
                events.expect(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                        .client(client.getClientId())
                        .user(AssertEvents.isUUID())
                        .session(AssertEvents.isSessionId())
                        .error(Errors.INVALID_REQUEST)
                        .detail(Details.REASON, Matchers.containsString("The requested claims are not available in the user profile"))
                        .assertEvent();
            }

            // 4 - Update user to add "firstName", but missing "lastName"
            userRep.setLastName(null);
            userRep.setFirstName("John");
            user.update(userRep);

            // Clear events before credential request
            events.clear();

            try (CloseableHttpResponse credentialResponse = httpClient.execute(postCredential)) {
                assertErrorCredentialResponse_mandatoryClaimsMissing(credentialResponse);
                
                // Verify VERIFIABLE_CREDENTIAL_REQUEST_ERROR event was fired
                events.expect(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                        .client(client.getClientId())
                        .user(AssertEvents.isUUID())
                        .session(AssertEvents.isSessionId())
                        .error(Errors.INVALID_REQUEST)
                        .detail(Details.REASON, Matchers.containsString("The requested claims are not available in the user profile"))
                        .assertEvent();
            }

            // 5 - Update user to both "firstName" and "lastName". Credential request should be successful
            userRep.setLastName("Doe");
            userRep.setFirstName("John");
            user.update(userRep);

            try (CloseableHttpResponse credentialResponse = httpClient.execute(postCredential)) {
                assertSuccessfulCredentialResponse(credentialResponse);
            }
        } finally {
            // 6 - Revert protocol mapper config
            protocolMapper.getConfig().put(Oid4vcProtocolMapperModel.MANDATORY, "false");
            clientScopeResource.getProtocolMappers().update(protocolMapper.getId(), protocolMapper);
        }
    }

    @Test
    public void testCompleteFlowWithSigningAlgorithmAndKeyIdConfigured() throws Exception {
        BiFunction<String, String, CredentialRequest> credRequestSupplier = (credentialConfigurationId, credentialIdentifier) -> {
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setCredentialIdentifier(credentialIdentifier);
            return credentialRequest;
        };

        ClientScopeResource clientScope = ApiUtil.findClientScopeByName(testRealm(), getCredentialClientScope().getName());
        ClientScopeRepresentation clientScopeRep = clientScope.toRepresentation();
        Map<String, String> origAttributes = new HashMap<>(clientScopeRep.getAttributes());

        try {
            // 1 - Configure signature algorithm, but not keyId. Make sure that credential signed with the target algorithm
            clientScopeRep.getAttributes().put(SIGNING_ALG, Algorithm.ES512);
            clientScopeRep.getAttributes().put(SIGNING_KEY_ID, null);
            clientScope.update(clientScopeRep);

            Object credentialObj = testCompleteFlowWithClaimsValidationAuthorizationCode(credRequestSupplier);
            JWSHeader jwsHeader = verifyCredentialSignature(credentialObj, Algorithm.ES512);
            String es512keyId = jwsHeader.getKeyId();
            logoutUser("john");

            // 2 - Configure signature algorithm, and keyId with blank value "" (just to simulate what admin console was doing when clientScope was saved).
            // Make sure that credential signed with the target algorithm and keyId is not considered
            clientScopeRep.getAttributes().put(SIGNING_ALG, Algorithm.EdDSA);
            clientScopeRep.getAttributes().put(SIGNING_KEY_ID, "");
            clientScope.update(clientScopeRep);

            credentialObj = testCompleteFlowWithClaimsValidationAuthorizationCode(credRequestSupplier);
            verifyCredentialSignature(credentialObj, Algorithm.EdDSA);
            logoutUser("john");

            // 3 - Configure signature algorithm, and keyId with some value. Make sure that
            // credential signed with the target algorithm and keyId as expected
            clientScopeRep.getAttributes().put(SIGNING_ALG, Algorithm.ES512);
            clientScopeRep.getAttributes().put(SIGNING_KEY_ID, es512keyId);
            clientScope.update(clientScopeRep);

            credentialObj = testCompleteFlowWithClaimsValidationAuthorizationCode(credRequestSupplier);
            JWSHeader newJWSHeader = verifyCredentialSignature(credentialObj, Algorithm.ES512);
            assertEquals(es512keyId, newJWSHeader.getKeyId());
            logoutUser("john");

            // 4 - Configure different signature algorithm not matching with key specified by keyId. Error is expected
            clientScopeRep.getAttributes().put(SIGNING_ALG, Algorithm.EdDSA);
            clientScopeRep.getAttributes().put(SIGNING_KEY_ID, es512keyId);
            clientScope.update(clientScopeRep);

            Oid4vcTestContext ctx = prepareOid4vcTestContext();
            AccessTokenResponse tokenResponse = authzCodeFlow(ctx);
            String credentialIdentifier = assertTokenResponse(tokenResponse);
            String credentialConfigurationId = getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);

            // Clear events before credential request
            events.clear();

            HttpPost postCredential = getCredentialRequest(ctx, credRequestSupplier, tokenResponse, credentialConfigurationId, credentialIdentifier);

            try (CloseableHttpResponse credentialResponse = httpClient.execute(postCredential)) {
                String expectedError = "Signing of credential failed: No key for id '" + es512keyId + "' and algorithm 'EdDSA' available.";
                assertErrorCredentialResponse(credentialResponse, ErrorType.INVALID_CREDENTIAL_REQUEST.name(), expectedError);

                // Verify VERIFIABLE_CREDENTIAL_REQUEST_ERROR event was fired with details about missing mandatory claim
                events.expect(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                        .client(client.getClientId())
                        .user(AssertEvents.isUUID())
                        .session(AssertEvents.isSessionId())
                        .error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue())
                        .detail(Details.REASON, expectedError)
                        .assertEvent();
            }

        } finally {
            // Revert clientScope config
            clientScopeRep.setAttributes(origAttributes);
            clientScope.update(clientScopeRep);
        }
    }

    // Return VC credential object
    private Object testCompleteFlowWithClaimsValidationAuthorizationCode(BiFunction<String, String, CredentialRequest> credentialRequestSupplier) throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Perform authorization code flow to get authorization code
        AccessTokenResponse tokenResponse = authzCodeFlow(ctx);
        String credentialIdentifier = assertTokenResponse(tokenResponse);
        String credentialConfigurationId = getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);

        events.clear();

        // Request the actual credential using the identifier
        HttpPost postCredential = getCredentialRequest(ctx, credentialRequestSupplier, tokenResponse, credentialConfigurationId, credentialIdentifier);

        try (CloseableHttpResponse credentialResponse = httpClient.execute(postCredential)) {
            Object credential = assertSuccessfulCredentialResponse(credentialResponse);

            // Verify event
            events.expect(EventType.VERIFIABLE_CREDENTIAL_REQUEST)
                    .client(client.getClientId())
                    .user(AssertEvents.isUUID())
                    .session(AssertEvents.isSessionId())
                    .detail(Details.USERNAME, "john")
                    .detail(Details.CREDENTIAL_TYPE, credentialConfigurationId)
                    .assertEvent();

            return credential;
        }
    }

    // Successful authorization_code flow
    private AccessTokenResponse authzCodeFlow(Oid4vcTestContext ctx) throws Exception {
        // Perform authorization code flow to get authorization code
        oauth.client(client.getClientId());
        oauth.scope(getCredentialClientScope().getName()); // Add the credential scope
        oauth.loginForm().doLogin("john", "password");

        String code = oauth.parseLoginResponse().getCode();
        assertNotNull("Authorization code should not be null", code);

        // Create authorization details with claims for token exchange
        ClaimsDescription claim = new ClaimsDescription();

        // Construct claim path based on credential format
        List<Object> claimPath;
        if ("sd_jwt_vc".equals(getCredentialFormat())) {
            claimPath = Collections.singletonList(getExpectedClaimPath());
        } else {
            claimPath = Arrays.asList("credentialSubject", getExpectedClaimPath());
        }
        claim.setPath(claimPath);
        claim.setMandatory(true);

        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setClaims(List.of(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<AuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        // Exchange authorization code for tokens with authorization_details
        HttpPost postToken = new HttpPost(ctx.openidConfig.getTokenEndpoint());
        List<NameValuePair> tokenParameters = new LinkedList<>();
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, oauth.getClientId()));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_SECRET, "password"));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.AUTHORIZATION_DETAILS, authDetailsJson));
        UrlEncodedFormEntity tokenFormEntity = new UrlEncodedFormEntity(tokenParameters, StandardCharsets.UTF_8);
        postToken.setEntity(tokenFormEntity);

        try (CloseableHttpResponse tokenHttpResponse = httpClient.execute(postToken)) {
            assertEquals(HttpStatus.SC_OK, tokenHttpResponse.getStatusLine().getStatusCode());
            String tokenResponseBody = IOUtils.toString(tokenHttpResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            return JsonSerialization.readValue(tokenResponseBody, AccessTokenResponse.class);
        }
    }

    // Test successful token response. Returns "Credential identifier" of the VC credential
    private String assertTokenResponse(AccessTokenResponse tokenResponse) throws Exception {
        // Extract authorization_details from token response
        List<OID4VCAuthorizationDetailsResponse> authDetailsResponse = parseAuthorizationDetails(JsonSerialization.writeValueAsString(tokenResponse));
        assertNotNull("authorization_details should be present in the response", authDetailsResponse);
        assertEquals(1, authDetailsResponse.size());

        OID4VCAuthorizationDetailsResponse authDetailResponse = authDetailsResponse.get(0);
        assertNotNull("Credential identifiers should be present", authDetailResponse.getCredentialIdentifiers());
        assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());

        String credentialConfigurationId = authDetailResponse.getCredentialConfigurationId();
        assertNotNull("Credential configuration id should not be null", credentialConfigurationId);

        List<String> credentialIdentifiers = authDetailResponse.getCredentialIdentifiers();
        assertNotNull("Credential identifiers should not be null", credentialIdentifiers);
        assertEquals("Credential identifiers expected to have 1 item. It had " + credentialIdentifiers.size() + " with value " + credentialIdentifiers,
                1, credentialIdentifiers.size());
        return credentialIdentifiers.get(0);
    }

    private HttpPost getCredentialRequest(Oid4vcTestContext ctx, BiFunction<String, String, CredentialRequest> credentialRequestSupplier, AccessTokenResponse tokenResponse,
                                          String credentialConfigurationId, String credentialIdentifier) throws Exception {
        // Request the actual credential using the identifier
        HttpPost postCredential = new HttpPost(ctx.credentialIssuer.getCredentialEndpoint());
        postCredential.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.getToken());
        postCredential.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        CredentialRequest credentialRequest = credentialRequestSupplier.apply(credentialConfigurationId, credentialIdentifier);

        String requestBody = JsonSerialization.writeValueAsString(credentialRequest);
        postCredential.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

        return postCredential;
    }

    // Test successful credential response and returns credential object
    private Object assertSuccessfulCredentialResponse(CloseableHttpResponse credentialResponse) throws Exception {
        assertEquals(HttpStatus.SC_OK, credentialResponse.getStatusLine().getStatusCode());
        String responseBody = IOUtils.toString(credentialResponse.getEntity().getContent(), StandardCharsets.UTF_8);

        // Parse the credential response
        CredentialResponse parsedResponse = JsonSerialization.readValue(responseBody, CredentialResponse.class);
        assertNotNull("Credential response should not be null", parsedResponse);
        assertNotNull("Credentials should be present", parsedResponse.getCredentials());
        assertEquals("Should have exactly one credential", 1, parsedResponse.getCredentials().size());

        // Verify that the issued credential contains the requested claims AND may contain additional claims
        CredentialResponse.Credential credentialWrapper = parsedResponse.getCredentials().get(0);
        assertNotNull("Credential wrapper should not be null", credentialWrapper);

        // The credential is stored as Object, so we need to cast it
        Object credentialObj = credentialWrapper.getCredential();
        assertNotNull("Credential object should not be null", credentialObj);

        // Verify the credential structure based on formatfix-authorization_details-processing
        verifyCredentialStructure(credentialObj);

        return credentialObj;
    }

    private void assertErrorCredentialResponse_mandatoryClaimsMissing(CloseableHttpResponse credentialResponse) throws Exception {
        assertEquals(HttpStatus.SC_BAD_REQUEST, credentialResponse.getStatusLine().getStatusCode());
        String responseBody = IOUtils.toString(credentialResponse.getEntity().getContent(), StandardCharsets.UTF_8);
        OAuth2ErrorRepresentation error = JsonSerialization.readValue(responseBody, OAuth2ErrorRepresentation.class);
        assertEquals("Credential issuance failed: No elements selected after processing claims path pointer. The requested claims are not available in the user profile.", error.getError());
    }

    private void assertErrorCredentialResponse(CloseableHttpResponse credentialResponse, String expectedError, String expectedErrorDescription) throws Exception {
        assertEquals(HttpStatus.SC_BAD_REQUEST, credentialResponse.getStatusLine().getStatusCode());
        String responseBody = IOUtils.toString(credentialResponse.getEntity().getContent(), StandardCharsets.UTF_8);
        OAuth2ErrorRepresentation error = JsonSerialization.readValue(responseBody, OAuth2ErrorRepresentation.class);
        assertEquals(expectedError, error.getError());
        assertEquals(expectedErrorDescription, error.getErrorDescription());
    }

    /**
     * Verify the credential structure based on the format.
     * Subclasses can override this to provide format-specific verification.
     */
    protected void verifyCredentialStructure(Object credentialObj) {
        // Default implementation - subclasses should override
        assertNotNull("Credential object should not be null", credentialObj);
    }

    /**
     * Verify credential signature on VC credential is of expected algorithm and optionally expected keyId.
     *
     * @param vcCredential Verifiable credential
     * @param expectedSignatureAlgorithm expected signature algorithm of the VC credential
     * @return JWS header used for the VC credential. Can be used for further checks in the tests
     */
    protected abstract JWSHeader verifyCredentialSignature(Object vcCredential, String expectedSignatureAlgorithm) throws Exception;

    /**
     * Parse authorization details from the token response.
     */
    protected List<OID4VCAuthorizationDetailsResponse> parseAuthorizationDetails(String responseBody) {
        try {
            // Parse the JSON response to extract authorization_details
            Map<String, Object> responseMap = JsonSerialization.readValue(responseBody, new TypeReference<>() {
            });
            Object authDetailsObj = responseMap.get(OAuth2Constants.AUTHORIZATION_DETAILS);

            if (authDetailsObj == null) {
                return Collections.emptyList();
            }

            // Convert to list of OID4VCAuthorizationDetailsResponse
            return JsonSerialization.readValue(JsonSerialization.writeValueAsString(authDetailsObj),
                    new TypeReference<>() {
                    });
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse authorization_details from response", e);
        }
    }
}
