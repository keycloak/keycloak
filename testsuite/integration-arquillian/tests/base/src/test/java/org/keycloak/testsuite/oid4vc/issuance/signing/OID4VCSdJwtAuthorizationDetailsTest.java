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

import jakarta.ws.rs.core.HttpHeaders;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oid4vc.model.AuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.AuthorizationDetailResponse;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.util.JsonSerialization;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.keycloak.protocol.oid4vc.model.Format.SD_JWT_VC;
import static org.keycloak.protocol.oidc.grants.OAuth2GrantTypeBase.OPENID_CREDENTIAL_TYPE;

/**
 * Tests the handling of authorization_details and SD-JWT VC format in the OID4VC (OpenID for Verifiable Credentials) issuance flow.
 * <p>
 * Covers scenarios including:
 * <ul>
 *   <li>Pre-authorized code flow with authorization_details using SD-JWT VC format</li>
 *   <li>Validation of error handling for invalid authorization_details combinations</li>
 *   <li>Correct issuance and identifier validation for SD-JWT credentials</li>
 * </ul>
 * Ensures correct behavior for SD-JWT credential issuance, identifier management, and error responses in OID4VC flows.
 */
public class OID4VCSdJwtAuthorizationDetailsTest extends OID4VCSdJwtIssuingEndpointTest {

    @Test
    public void testPreAuthorizedCodeWithInvalidAuthorizationDetails() throws Exception {
        String token = getBearerToken(oauth);
        HttpGet getCredentialOfferURI = new HttpGet(getBasePath(TEST_REALM_NAME) + "credential-offer-uri?credential_configuration_id=test-credential");
        getCredentialOfferURI.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        CredentialOfferURI credentialOfferURI;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOfferURI)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialOfferURI = JsonSerialization.readValue(s, CredentialOfferURI.class);
        }
        HttpGet getCredentialOffer = new HttpGet(credentialOfferURI.getIssuer() + "/" + credentialOfferURI.getNonce());
        CredentialsOffer credentialsOffer;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOffer)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialsOffer = JsonSerialization.readValue(s, CredentialsOffer.class);
        }
        HttpGet getIssuerMetadata = new HttpGet(credentialsOffer.getCredentialIssuer() + "/.well-known/openid-credential-issuer");
        CredentialIssuer credentialIssuer;
        try (CloseableHttpResponse response = httpClient.execute(getIssuerMetadata)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialIssuer = JsonSerialization.readValue(s, CredentialIssuer.class);
        }
        HttpGet getOpenidConfiguration = new HttpGet(credentialIssuer.getAuthorizationServers().get(0) + "/.well-known/openid-configuration");
        OIDCConfigurationRepresentation openidConfig;
        try (CloseableHttpResponse response = httpClient.execute(getOpenidConfiguration)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            openidConfig = JsonSerialization.readValue(s, OIDCConfigurationRepresentation.class);
        }
        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId("test-credential");
        authDetail.setFormat(SD_JWT_VC); // Invalid: format should not be combined with credential_configuration_id
        List<AuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);
        HttpPost postPreAuthorizedCode = new HttpPost(openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        parameters.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        postPreAuthorizedCode.setEntity(formEntity);
        try (CloseableHttpResponse tokenResponse = httpClient.execute(postPreAuthorizedCode)) {
            assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testPreAuthorizedCodeWithAuthorizationDetailsFormat() throws Exception {
        String token = getBearerToken(oauth);
        HttpGet getCredentialOfferURI = new HttpGet(getBasePath(TEST_REALM_NAME) + "credential-offer-uri?credential_configuration_id=test-credential");
        getCredentialOfferURI.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        CredentialOfferURI credentialOfferURI;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOfferURI)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialOfferURI = JsonSerialization.readValue(s, CredentialOfferURI.class);
        }
        HttpGet getCredentialOffer = new HttpGet(credentialOfferURI.getIssuer() + "/" + credentialOfferURI.getNonce());
        CredentialsOffer credentialsOffer;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOffer)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialsOffer = JsonSerialization.readValue(s, CredentialsOffer.class);
        }
        HttpGet getIssuerMetadata = new HttpGet(credentialsOffer.getCredentialIssuer() + "/.well-known/openid-credential-issuer");
        CredentialIssuer credentialIssuer;
        try (CloseableHttpResponse response = httpClient.execute(getIssuerMetadata)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialIssuer = JsonSerialization.readValue(s, CredentialIssuer.class);
        }
        HttpGet getOpenidConfiguration = new HttpGet(credentialIssuer.getAuthorizationServers().get(0) + "/.well-known/openid-configuration");
        OIDCConfigurationRepresentation openidConfig;
        try (CloseableHttpResponse response = httpClient.execute(getOpenidConfiguration)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            openidConfig = JsonSerialization.readValue(s, OIDCConfigurationRepresentation.class);
        }
        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setFormat(SD_JWT_VC);
        if (credentialIssuer.getAuthorizationServers() != null && !credentialIssuer.getAuthorizationServers().isEmpty()) {
            authDetail.setLocations(Collections.singletonList(credentialIssuer.getCredentialIssuer()));
        }
        List<AuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);
        HttpPost postPreAuthorizedCode = new HttpPost(openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        parameters.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        postPreAuthorizedCode.setEntity(formEntity);
        try (CloseableHttpResponse tokenResponse = httpClient.execute(postPreAuthorizedCode)) {
            assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusLine().getStatusCode());
            String responseBody = IOUtils.toString(tokenResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            List<AuthorizationDetailResponse> authDetailsResponse = parseAuthorizationDetails(responseBody);
            assertNotNull("authorization_details should be present in the response", authDetailsResponse);
            assertEquals(1, authDetailsResponse.size());
            AuthorizationDetailResponse authDetailResponse = authDetailsResponse.get(0);
            assertEquals("openid_credential", authDetailResponse.getType());
            assertEquals(SD_JWT_VC, authDetailResponse.getFormat());
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
} 
