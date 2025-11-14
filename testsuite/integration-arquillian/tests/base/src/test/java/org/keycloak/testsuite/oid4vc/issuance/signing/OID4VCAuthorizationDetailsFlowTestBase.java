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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.OAuth2Constants;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailsResponse;
import org.keycloak.protocol.oid4vc.model.AuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.ClaimsDescription;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
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
import org.junit.Test;

import static org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailsProcessor.OPENID_CREDENTIAL_TYPE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Base class for authorization details flow tests.
 * Contains common test logic that can be reused by JWT and SD-JWT specific test classes.
 *
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public abstract class OID4VCAuthorizationDetailsFlowTestBase extends OID4VCIssuerEndpointTest {

    protected static class Oid4vcTestContext {
        CredentialsOffer credentialsOffer;
        CredentialIssuer credentialIssuer;
        OIDCConfigurationRepresentation openidConfig;
    }

    /**
     * Get the credential format for this test implementation.
     *
     * @return the credential format (e.g., "jwt_vc", "sd_jwt_vc")
     */
    protected abstract String getCredentialFormat();

    /**
     * Get the credential client scope for this test implementation.
     *
     * @return the client scope model
     */
    protected abstract ClientScopeRepresentation getCredentialClientScope();

    /**
     * Get the expected claim path for this test implementation.
     *
     * @return the claim path as a string
     */
    protected abstract String getExpectedClaimPath();

    protected Oid4vcTestContext prepareOid4vcTestContext(String token) throws Exception {
        Oid4vcTestContext ctx = new Oid4vcTestContext();

        String credentialConfigurationId = getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);
        HttpGet getCredentialOfferURI = new HttpGet(getBasePath(TEST_REALM_NAME) + "credential-offer-uri?credential_configuration_id=" + credentialConfigurationId);
        getCredentialOfferURI.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        CredentialOfferURI credentialOfferURI;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOfferURI)) {
            int status = response.getStatusLine().getStatusCode();
            assertEquals(HttpStatus.SC_OK, status);
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialOfferURI = JsonSerialization.readValue(s, CredentialOfferURI.class);
        }

        HttpGet getCredentialOffer = new HttpGet(credentialOfferURI.getIssuer() + "/" + credentialOfferURI.getNonce());
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOffer)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            ctx.credentialsOffer = JsonSerialization.readValue(s, CredentialsOffer.class);
        }

        HttpGet getIssuerMetadata = new HttpGet(ctx.credentialsOffer.getCredentialIssuer() + "/.well-known/openid-credential-issuer");
        try (CloseableHttpResponse response = httpClient.execute(getIssuerMetadata)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            ctx.credentialIssuer = JsonSerialization.readValue(s, CredentialIssuer.class);
        }

        HttpGet getOpenidConfiguration = new HttpGet(ctx.credentialIssuer.getAuthorizationServers().get(0) + "/.well-known/openid-configuration");
        try (CloseableHttpResponse response = httpClient.execute(getOpenidConfiguration)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            ctx.openidConfig = JsonSerialization.readValue(s, OIDCConfigurationRepresentation.class);
        }

        return ctx;
    }

    @Test
    public void testPreAuthorizedCodeWithAuthorizationDetailsCredentialConfigurationId() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<AuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        HttpPost postPreAuthorizedCode = new HttpPost(ctx.openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        parameters.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        postPreAuthorizedCode.setEntity(formEntity);

        try (CloseableHttpResponse tokenResponse = httpClient.execute(postPreAuthorizedCode)) {
            assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusLine().getStatusCode());
            String responseBody = IOUtils.toString(tokenResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            List<OID4VCAuthorizationDetailsResponse> authDetailsResponse = parseAuthorizationDetails(responseBody);
            assertNotNull("authorization_details should be present in the response", authDetailsResponse);
            assertEquals(1, authDetailsResponse.size());
            OID4VCAuthorizationDetailsResponse authDetailResponse = authDetailsResponse.get(0);
            assertEquals(OPENID_CREDENTIAL_TYPE, authDetailResponse.getType());
            assertEquals(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID), authDetailResponse.getCredentialConfigurationId());
            assertNotNull(authDetailResponse.getCredentialIdentifiers());
            assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());
            String firstIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
            assertNotNull("Identifier should not be null", firstIdentifier);
            assertFalse("Identifier should not be empty", firstIdentifier.isEmpty());
            try {
                UUID.fromString(firstIdentifier);
            } catch (IllegalArgumentException e) {
                fail("Identifier should be a valid UUID, but was: " + firstIdentifier);
            }
        }
    }

    @Test
    public void testPreAuthorizedCodeWithAuthorizationDetailsAndClaims() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Create claims description for a claim that should be supported
        ClaimsDescription claim = new ClaimsDescription();

        // Construct claim path based on credential format
        List<Object> claimPath;
        if ("sd_jwt_vc".equals(getCredentialFormat())) {
            // SD-JWT doesn't use credentialSubject prefix
            claimPath = Arrays.asList(getExpectedClaimPath());
        } else {
            // JWT and other formats use credentialSubject prefix
            claimPath = Arrays.asList("credentialSubject", getExpectedClaimPath());
        }
        claim.setPath(claimPath);
        claim.setMandatory(true);

        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setClaims(Arrays.asList(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<AuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        HttpPost postPreAuthorizedCode = new HttpPost(ctx.openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        parameters.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        postPreAuthorizedCode.setEntity(formEntity);

        try (CloseableHttpResponse tokenResponse = httpClient.execute(postPreAuthorizedCode)) {
            assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusLine().getStatusCode());
            String responseBody = IOUtils.toString(tokenResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            List<OID4VCAuthorizationDetailsResponse> authDetailsResponse = parseAuthorizationDetails(responseBody);
            assertNotNull("authorization_details should be present in the response", authDetailsResponse);
            assertEquals(1, authDetailsResponse.size());
            OID4VCAuthorizationDetailsResponse authDetailResponse = authDetailsResponse.get(0);
            assertEquals(OPENID_CREDENTIAL_TYPE, authDetailResponse.getType());
            assertEquals(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID), authDetailResponse.getCredentialConfigurationId());
            assertNotNull(authDetailResponse.getClaims());
            assertEquals(1, authDetailResponse.getClaims().size());
            ClaimsDescription responseClaim = authDetailResponse.getClaims().get(0);

            List<Object> expectedClaimPath;
            if ("sd_jwt_vc".equals(getCredentialFormat())) {
                expectedClaimPath = Arrays.asList(getExpectedClaimPath());
            } else {
                expectedClaimPath = Arrays.asList("credentialSubject", getExpectedClaimPath());
            }
            assertEquals(expectedClaimPath, responseClaim.getPath());
            assertTrue(responseClaim.isMandatory());

            // Verify that credential identifiers are present
            assertNotNull(authDetailResponse.getCredentialIdentifiers());
            assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());
        }
    }

    @Test
    public void testPreAuthorizedCodeWithUnsupportedClaims() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Create claims description for a claim that should NOT be supported
        ClaimsDescription claim = new ClaimsDescription();
        claim.setPath(Arrays.asList("credentialSubject", "unsupportedClaim"));
        claim.setMandatory(false);

        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setClaims(Arrays.asList(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<AuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        HttpPost postPreAuthorizedCode = new HttpPost(ctx.openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        parameters.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        postPreAuthorizedCode.setEntity(formEntity);

        try (CloseableHttpResponse tokenResponse = httpClient.execute(postPreAuthorizedCode)) {
            // Should fail because the claim is not supported by the credential configuration
            assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusLine().getStatusCode());
            String responseBody = IOUtils.toString(tokenResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            assertTrue("Error message should indicate authorization_details processing error",
                    responseBody.contains("Error when processing authorization_details"));
        }
    }

    @Test
    public void testPreAuthorizedCodeWithMandatoryClaimMissing() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Create claims description for a mandatory claim
        ClaimsDescription claim = new ClaimsDescription();
        claim.setPath(Arrays.asList("credentialSubject", "mandatoryClaim"));
        claim.setMandatory(true);

        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setClaims(Arrays.asList(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<AuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        HttpPost postPreAuthorizedCode = new HttpPost(ctx.openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        parameters.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        postPreAuthorizedCode.setEntity(formEntity);

        try (CloseableHttpResponse tokenResponse = httpClient.execute(postPreAuthorizedCode)) {
            // Should fail because the mandatory claim is not supported
            assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusLine().getStatusCode());
            String responseBody = IOUtils.toString(tokenResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            assertTrue("Error message should indicate authorization_details processing error",
                    responseBody.contains("Error when processing authorization_details"));
        }
    }

    @Test
    public void testPreAuthorizedCodeWithComplexClaimsPath() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Create claims description with complex path
        ClaimsDescription claim = new ClaimsDescription();
        claim.setPath(Arrays.asList("credentialSubject", "address", "street"));
        claim.setMandatory(false);

        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setClaims(Arrays.asList(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<AuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        HttpPost postPreAuthorizedCode = new HttpPost(ctx.openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        parameters.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        postPreAuthorizedCode.setEntity(formEntity);

        try (CloseableHttpResponse tokenResponse = httpClient.execute(postPreAuthorizedCode)) {
            // Should fail if the complex path is not supported
            int statusCode = tokenResponse.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                String responseBody = IOUtils.toString(tokenResponse.getEntity().getContent(), StandardCharsets.UTF_8);
                assertTrue("Error message should indicate authorization_details processing error",
                        responseBody.contains("Error when processing authorization_details"));
            } else {
                // If it succeeds, verify the response structure
                assertEquals(HttpStatus.SC_OK, statusCode);
                String responseBody = IOUtils.toString(tokenResponse.getEntity().getContent(), StandardCharsets.UTF_8);
                List<OID4VCAuthorizationDetailsResponse> authDetailsResponse = parseAuthorizationDetails(responseBody);
                assertNotNull("authorization_details should be present in the response", authDetailsResponse);
                assertEquals(1, authDetailsResponse.size());
            }
        }
    }

    @Test
    public void testPreAuthorizedCodeWithInvalidAuthorizationDetails() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        // Missing credential_configuration_id - should fail
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<AuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        HttpPost postPreAuthorizedCode = new HttpPost(ctx.openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        parameters.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        postPreAuthorizedCode.setEntity(formEntity);

        try (CloseableHttpResponse tokenResponse = httpClient.execute(postPreAuthorizedCode)) {
            assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusLine().getStatusCode());
            String responseBody = IOUtils.toString(tokenResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            assertTrue("Error message should indicate authorization_details processing error",
                    responseBody.contains("Error when processing authorization_details"));
        }
    }

    @Test
    public void testPreAuthorizedCodeWithInvalidClaims() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Create claims description with invalid path
        ClaimsDescription claim = new ClaimsDescription();
        claim.setPath(null); // Invalid: null path
        claim.setMandatory(false);

        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setClaims(Arrays.asList(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<AuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        HttpPost postPreAuthorizedCode = new HttpPost(ctx.openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        parameters.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        postPreAuthorizedCode.setEntity(formEntity);

        try (CloseableHttpResponse tokenResponse = httpClient.execute(postPreAuthorizedCode)) {
            assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusLine().getStatusCode());
            String responseBody = IOUtils.toString(tokenResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            assertTrue("Error message should indicate authorization_details processing error",
                    responseBody.contains("Error when processing authorization_details"));
        }
    }

    @Test
    public void testPreAuthorizedCodeWithEmptyAuthorizationDetails() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Send empty authorization_details array - should fail
        String authDetailsJson = "[]";

        HttpPost postPreAuthorizedCode = new HttpPost(ctx.openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        parameters.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        postPreAuthorizedCode.setEntity(formEntity);

        try (CloseableHttpResponse tokenResponse = httpClient.execute(postPreAuthorizedCode)) {
            assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusLine().getStatusCode());
            String responseBody = IOUtils.toString(tokenResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            assertTrue("Error message should indicate authorization_details processing error",
                    responseBody.contains("Error when processing authorization_details"));
        }
    }

    @Test
    public void testPreAuthorizedCodeWithCredentialOfferBasedAuthorizationDetails() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Test Pre-Authorized Code Flow without authorization_details parameter
        // The system should generate authorization_details based on credential_configuration_ids from the credential offer

        HttpPost postPreAuthorizedCode = new HttpPost(ctx.openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        postPreAuthorizedCode.setEntity(formEntity);

        try (CloseableHttpResponse tokenResponse = httpClient.execute(postPreAuthorizedCode)) {
            assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusLine().getStatusCode());
            String responseBody = IOUtils.toString(tokenResponse.getEntity().getContent(), StandardCharsets.UTF_8);

            List<OID4VCAuthorizationDetailsResponse> authDetailsResponse = parseAuthorizationDetails(responseBody);
            assertNotNull("authorization_details should be present in the response", authDetailsResponse);
            assertEquals("Should have authorization_details for each credential configuration in the offer",
                    ctx.credentialsOffer.getCredentialConfigurationIds().size(), authDetailsResponse.size());

            // Verify each credential configuration from the offer has corresponding authorization_details
            for (int i = 0; i < ctx.credentialsOffer.getCredentialConfigurationIds().size(); i++) {
                String expectedConfigId = ctx.credentialsOffer.getCredentialConfigurationIds().get(i);
                OID4VCAuthorizationDetailsResponse authDetailResponse = authDetailsResponse.get(i);

                assertEquals(OPENID_CREDENTIAL_TYPE, authDetailResponse.getType());
                assertEquals("Credential configuration ID should match the one from the offer",
                        expectedConfigId, authDetailResponse.getCredentialConfigurationId());
                assertNotNull("Credential identifiers should be present", authDetailResponse.getCredentialIdentifiers());
                assertEquals("Should have exactly one credential identifier", 1, authDetailResponse.getCredentialIdentifiers().size());

                String credentialIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
                assertNotNull("Identifier should not be null", credentialIdentifier);
                assertFalse("Identifier should not be empty", credentialIdentifier.isEmpty());
                try {
                    UUID.fromString(credentialIdentifier);
                } catch (IllegalArgumentException e) {
                    fail("Identifier should be a valid UUID, but was: " + credentialIdentifier);
                }
            }
        }
    }

    @Test
    public void testCompleteFlowWithCredentialOfferBasedAuthorizationDetails() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Step 1: Request token without authorization_details parameter (no scope needed)
        HttpPost postPreAuthorizedCode = new HttpPost(ctx.openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        postPreAuthorizedCode.setEntity(formEntity);

        String credentialIdentifier = null;
        try (CloseableHttpResponse tokenResponse = httpClient.execute(postPreAuthorizedCode)) {
            assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusLine().getStatusCode());
            String responseBody = IOUtils.toString(tokenResponse.getEntity().getContent(), StandardCharsets.UTF_8);

            List<OID4VCAuthorizationDetailsResponse> authDetailsResponse = parseAuthorizationDetails(responseBody);
            assertNotNull("authorization_details should be present in the response", authDetailsResponse);
            assertEquals("Should have authorization_details for each credential configuration in the offer",
                    ctx.credentialsOffer.getCredentialConfigurationIds().size(), authDetailsResponse.size());

            // Use the first authorization detail for credential request
            OID4VCAuthorizationDetailsResponse authDetailResponse = authDetailsResponse.get(0);
            assertNotNull("Credential identifiers should be present", authDetailResponse.getCredentialIdentifiers());
            assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());

            credentialIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
            assertNotNull("Credential identifier should not be null", credentialIdentifier);
        }

        // Step 2: Request the actual credential using ONLY the identifier (no credential_configuration_id)
        // This tests that the mapping from credential identifier to credential configuration ID works correctly
        HttpPost postCredential = new HttpPost(ctx.credentialIssuer.getCredentialEndpoint());
        postCredential.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        postCredential.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setCredentialIdentifier(credentialIdentifier);

        String requestBody = JsonSerialization.writeValueAsString(credentialRequest);
        postCredential.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

        try (CloseableHttpResponse credentialResponse = httpClient.execute(postCredential)) {
            assertEquals(HttpStatus.SC_OK, credentialResponse.getStatusLine().getStatusCode());
            String responseBody = IOUtils.toString(credentialResponse.getEntity().getContent(), StandardCharsets.UTF_8);

            // Parse the credential response
            CredentialResponse parsedResponse = JsonSerialization.readValue(responseBody, CredentialResponse.class);
            assertNotNull("Credential response should not be null", parsedResponse);
            assertNotNull("Credentials should be present", parsedResponse.getCredentials());
            assertEquals("Should have exactly one credential", 1, parsedResponse.getCredentials().size());

            // Step 3: Verify that the issued credential structure is valid
            CredentialResponse.Credential credentialWrapper = parsedResponse.getCredentials().get(0);
            assertNotNull("Credential wrapper should not be null", credentialWrapper);

            // The credential is stored as Object, so we need to cast it
            Object credentialObj = credentialWrapper.getCredential();
            assertNotNull("Credential object should not be null", credentialObj);

            // Verify the credential structure based on format
            verifyCredentialStructure(credentialObj);
        }
    }

    @Test
    public void testMultipleCredentialConfigurationsFromOffer() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Verify that the credential offer has multiple configurations (if supported by the test setup)
        assertTrue("Credential offer should have at least one credential configuration",
                ctx.credentialsOffer.getCredentialConfigurationIds() != null &&
                        !ctx.credentialsOffer.getCredentialConfigurationIds().isEmpty());

        // Step 1: Request token without authorization_details parameter
        HttpPost postPreAuthorizedCode = new HttpPost(ctx.openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        postPreAuthorizedCode.setEntity(formEntity);

        try (CloseableHttpResponse tokenResponse = httpClient.execute(postPreAuthorizedCode)) {
            assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusLine().getStatusCode());
            String responseBody = IOUtils.toString(tokenResponse.getEntity().getContent(), StandardCharsets.UTF_8);

            List<OID4VCAuthorizationDetailsResponse> authDetailsResponse = parseAuthorizationDetails(responseBody);
            assertNotNull("authorization_details should be present in the response", authDetailsResponse);

            // Verify that we have authorization_details for each credential configuration in the offer
            assertEquals("Should have authorization_details for each credential configuration in the offer",
                    ctx.credentialsOffer.getCredentialConfigurationIds().size(), authDetailsResponse.size());

            // Verify each authorization detail
            for (int i = 0; i < authDetailsResponse.size(); i++) {
                OID4VCAuthorizationDetailsResponse authDetail = authDetailsResponse.get(i);
                String expectedConfigId = ctx.credentialsOffer.getCredentialConfigurationIds().get(i);

                // Verify structure
                assertEquals("Type should be openid_credential", OPENID_CREDENTIAL_TYPE, authDetail.getType());
                assertEquals("Credential configuration ID should match the one from the offer",
                        expectedConfigId, authDetail.getCredentialConfigurationId());
                assertNotNull("Credential identifiers should be present", authDetail.getCredentialIdentifiers());
                assertEquals("Should have exactly one credential identifier per configuration",
                        1, authDetail.getCredentialIdentifiers().size());

                // Verify identifier format
                String credentialIdentifier = authDetail.getCredentialIdentifiers().get(0);
                assertNotNull("Credential identifier should not be null", credentialIdentifier);
                assertFalse("Credential identifier should not be empty", credentialIdentifier.isEmpty());
                try {
                    UUID.fromString(credentialIdentifier);
                } catch (IllegalArgumentException e) {
                    fail("Credential identifier should be a valid UUID, but was: " + credentialIdentifier);
                }
            }

            // Verify that all credential identifiers are unique
            Set<String> allIdentifiers = authDetailsResponse.stream()
                    .flatMap(auth -> auth.getCredentialIdentifiers().stream())
                    .collect(Collectors.toSet());
            assertEquals("All credential identifiers should be unique",
                    authDetailsResponse.size(), allIdentifiers.size());
        }
    }

    @Test
    public void testCompleteFlowWithClaimsValidation() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Step 1: Request token with authorization details containing specific claims
        // This tests that requested claims are validated and present in the final credential
        ClaimsDescription claim = new ClaimsDescription();

        // Construct claim path based on credential format
        List<Object> claimPath;
        if ("sd_jwt_vc".equals(getCredentialFormat())) {
            claimPath = Arrays.asList(getExpectedClaimPath());
        } else {
            claimPath = Arrays.asList("credentialSubject", getExpectedClaimPath());
        }
        claim.setPath(claimPath);
        claim.setMandatory(true);

        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setClaims(Arrays.asList(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<AuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        HttpPost postPreAuthorizedCode = new HttpPost(ctx.openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        parameters.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        postPreAuthorizedCode.setEntity(formEntity);

        String credentialIdentifier;
        try (CloseableHttpResponse tokenResponse = httpClient.execute(postPreAuthorizedCode)) {
            assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusLine().getStatusCode());
            String responseBody = IOUtils.toString(tokenResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            List<OID4VCAuthorizationDetailsResponse> authDetailsResponse = parseAuthorizationDetails(responseBody);
            assertNotNull("authorization_details should be present in the response", authDetailsResponse);
            assertEquals(1, authDetailsResponse.size());

            OID4VCAuthorizationDetailsResponse authDetailResponse = authDetailsResponse.get(0);
            assertNotNull("Credential identifiers should be present", authDetailResponse.getCredentialIdentifiers());
            assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());

            credentialIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
            assertNotNull("Credential identifier should not be null", credentialIdentifier);
        }

        // Step 2: Request the actual credential using the identifier
        HttpPost postCredential = new HttpPost(ctx.credentialIssuer.getCredentialEndpoint());
        postCredential.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        postCredential.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setCredentialIdentifier(credentialIdentifier);

        String requestBody = JsonSerialization.writeValueAsString(credentialRequest);
        postCredential.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

        try (CloseableHttpResponse credentialResponse = httpClient.execute(postCredential)) {
            assertEquals(HttpStatus.SC_OK, credentialResponse.getStatusLine().getStatusCode());
            String responseBody = IOUtils.toString(credentialResponse.getEntity().getContent(), StandardCharsets.UTF_8);

            // Parse the credential response
            CredentialResponse parsedResponse = JsonSerialization.readValue(responseBody, CredentialResponse.class);
            assertNotNull("Credential response should not be null", parsedResponse);
            assertNotNull("Credentials should be present", parsedResponse.getCredentials());
            assertEquals("Should have exactly one credential", 1, parsedResponse.getCredentials().size());

            // Step 3: Verify that the issued credential contains the requested claims AND may contain additional claims
            CredentialResponse.Credential credentialWrapper = parsedResponse.getCredentials().get(0);
            assertNotNull("Credential wrapper should not be null", credentialWrapper);

            // The credential is stored as Object, so we need to cast it
            Object credentialObj = credentialWrapper.getCredential();
            assertNotNull("Credential object should not be null", credentialObj);

            // Verify the credential structure based on format
            verifyCredentialStructure(credentialObj);
        }
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
     * Parse authorization details from the token response.
     */
    protected List<OID4VCAuthorizationDetailsResponse> parseAuthorizationDetails(String responseBody) {
        try {
            // Parse the JSON response to extract authorization_details
            Map<String, Object> responseMap = JsonSerialization.readValue(responseBody, Map.class);
            Object authDetailsObj = responseMap.get("authorization_details");

            if (authDetailsObj == null) {
                return Collections.emptyList();
            }

            // Convert to list of OID4VCAuthorizationDetailsResponse
            return JsonSerialization.readValue(JsonSerialization.writeValueAsString(authDetailsObj),
                    new TypeReference<List<OID4VCAuthorizationDetailsResponse>>() {
                    });
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse authorization_details from response", e);
        }
    }
}
