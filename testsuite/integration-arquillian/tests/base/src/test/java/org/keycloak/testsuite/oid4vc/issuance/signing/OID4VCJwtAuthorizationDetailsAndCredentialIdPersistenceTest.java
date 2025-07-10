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
import org.keycloak.common.VerificationException;
import org.keycloak.protocol.oid4vc.model.AuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.AuthorizationDetailResponse;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.URI;

import jakarta.ws.rs.core.UriBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.keycloak.protocol.oid4vc.model.Format.JWT_VC;
import static org.keycloak.protocol.oid4vc.model.Format.SD_JWT_VC;
import static org.keycloak.protocol.oidc.grants.OAuth2GrantTypeBase.OPENID_CREDENTIAL_TYPE;

import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProviderFactory;

/**
 * Tests the handling of authorization_details and credential_configuration_id in the OID4VC (OpenID for Verifiable Credentials) issuance flow.
 * <p>
 * Covers scenarios including:
 * <ul>
 *   <li>Pre-authorized code and authorization code flows with authorization_details</li>
 *   <li>Persistence and reuse of credential identifiers within a session</li>
 *   <li>Generation of new credential identifiers across sessions</li>
 *   <li>Validation of error handling for invalid authorization_details combinations</li>
 * </ul>
 * Ensures correct behavior for credential issuance, identifier management, and error responses in OID4VC flows.
 */
public class OID4VCJwtAuthorizationDetailsAndCredentialIdPersistenceTest extends OID4VCIssuerEndpointTest {

    @Test
    public void testPreAuthorizedCodeWithAuthorizationDetailsCredentialId() throws Exception {
        String token = getBearerToken(oauth);

        // 1. Retrieve the credential offer URI
        HttpGet getCredentialOfferURI = new HttpGet(getBasePath(TEST_REALM_NAME) + "credential-offer-uri?credential_configuration_id=test-credential");
        getCredentialOfferURI.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        CredentialOfferURI credentialOfferURI;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOfferURI)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialOfferURI = JsonSerialization.readValue(s, CredentialOfferURI.class);
        }

        // 2. Get the credential offer
        HttpGet getCredentialOffer = new HttpGet(credentialOfferURI.getIssuer() + "/" + credentialOfferURI.getNonce());
        CredentialsOffer credentialsOffer;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOffer)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialsOffer = JsonSerialization.readValue(s, CredentialsOffer.class);
        }

        // 3. Get the issuer metadata
        HttpGet getIssuerMetadata = new HttpGet(credentialsOffer.getCredentialIssuer() + "/.well-known/openid-credential-issuer");
        CredentialIssuer credentialIssuer;
        try (CloseableHttpResponse response = httpClient.execute(getIssuerMetadata)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialIssuer = JsonSerialization.readValue(s, CredentialIssuer.class);
        }

        // 4. Get the openid-configuration
        HttpGet getOpenidConfiguration = new HttpGet(credentialIssuer.getAuthorizationServers().get(0) + "/.well-known/openid-configuration");
        OIDCConfigurationRepresentation openidConfig;
        try (CloseableHttpResponse response = httpClient.execute(getOpenidConfiguration)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            openidConfig = JsonSerialization.readValue(s, OIDCConfigurationRepresentation.class);
        }

        // 5. Prepare authorization_details with credential_configuration_id
        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId("test-credential");
        if (credentialIssuer.getAuthorizationServers() != null && !credentialIssuer.getAuthorizationServers().isEmpty()) {
            authDetail.setLocations(Collections.singletonList(credentialIssuer.getCredentialIssuer()));
        }
        List<AuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        // 6. Get an access token with authorization_details
        HttpPost postPreAuthorizedCode = new HttpPost(openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        parameters.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        postPreAuthorizedCode.setEntity(formEntity);
        try (CloseableHttpResponse tokenResponse = httpClient.execute(postPreAuthorizedCode)) {
            assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusLine().getStatusCode());

            // 7. Read the response body
            String responseBody = IOUtils.toString(tokenResponse.getEntity().getContent(), StandardCharsets.UTF_8);

            // Parse authorization_details
            List<AuthorizationDetailResponse> authDetailsResponse = parseAuthorizationDetails(responseBody);
            assertNotNull("authorization_details should be present in the response", authDetailsResponse);
            assertEquals(1, authDetailsResponse.size());
            AuthorizationDetailResponse authDetailResponse = authDetailsResponse.get(0);
            assertEquals("openid_credential", authDetailResponse.getType());
            assertEquals("test-credential", authDetailResponse.getCredentialConfigurationId());
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

            // Extract access token
            String a_token = getAccessToken(responseBody);

            // 8. Request the credential
            credentialsOffer.getCredentialConfigurationIds().stream()
                    .map(offeredCredentialId -> credentialIssuer.getCredentialsSupported().get(offeredCredentialId))
                    .forEach(supportedCredential -> {
                        try {
                            requestOffer(a_token, credentialIssuer.getCredentialEndpoint(), supportedCredential, new CredentialResponseHandler());
                        } catch (IOException | VerificationException e) {
                            fail("Was not able to get the credential: " + e.getMessage());
                        }
                    });
        }
    }

    @Test
    public void testPreAuthorizedCodeWithInvalidAuthorizationDetails() throws Exception {
        String token = getBearerToken(oauth);

        // 1. Retrieve the credential offer URI
        HttpGet getCredentialOfferURI = new HttpGet(getBasePath(TEST_REALM_NAME) + "credential-offer-uri?credential_configuration_id=test-credential");
        getCredentialOfferURI.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        CredentialOfferURI credentialOfferURI;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOfferURI)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialOfferURI = JsonSerialization.readValue(s, CredentialOfferURI.class);
        }

        // 2. Get the credential offer
        HttpGet getCredentialOffer = new HttpGet(credentialOfferURI.getIssuer() + "/" + credentialOfferURI.getNonce());
        CredentialsOffer credentialsOffer;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOffer)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialsOffer = JsonSerialization.readValue(s, CredentialsOffer.class);
        }

        // 3. Get the issuer metadata
        HttpGet getIssuerMetadata = new HttpGet(credentialsOffer.getCredentialIssuer() + "/.well-known/openid-credential-issuer");
        CredentialIssuer credentialIssuer;
        try (CloseableHttpResponse response = httpClient.execute(getIssuerMetadata)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialIssuer = JsonSerialization.readValue(s, CredentialIssuer.class);
        }

        // 4. Get the openid-configuration
        HttpGet getOpenidConfiguration = new HttpGet(credentialIssuer.getAuthorizationServers().get(0) + "/.well-known/openid-configuration");
        OIDCConfigurationRepresentation openidConfig;
        try (CloseableHttpResponse response = httpClient.execute(getOpenidConfiguration)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            openidConfig = JsonSerialization.readValue(s, OIDCConfigurationRepresentation.class);
        }

        // 5. Prepare invalid authorization_details
        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId("test-credential");
        authDetail.setFormat(JWT_VC); // Invalid: format should not be combined with credential_configuration_id
        List<AuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        // 6. Attempt to get an access token with invalid authorization_details
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
    public void testAuthorizationCodeWithAuthorizationDetails() throws Exception {
        testCredentialIssuanceWithAuthZCodeFlow(
                (testClientId, testScope) -> {
                    try {
                        // Configure OAuth client
                        oauth.clientId(testClientId)
                                .scope(testScope)
                                .openid(false);

                        // Get authorization code
                        AuthorizationEndpointResponse authResponse = oauth.doLogin("john", "password");
                        String authorizationCode = authResponse.getCode();
                        assertNotNull("Authorization code should be present", authorizationCode);

                        // Get token endpoint from .well-known
                        UriBuilder builder = UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT);
                        URI oid4vciDiscoveryUri = RealmsResource.wellKnownProviderUrl(builder).build(TEST_REALM_NAME, OID4VCIssuerWellKnownProviderFactory.PROVIDER_ID);
                        HttpGet getIssuerMetadata = new HttpGet(oid4vciDiscoveryUri);
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

                        // Prepare authorization_details
                        AuthorizationDetail authDetail = new AuthorizationDetail();
                        authDetail.setType(OPENID_CREDENTIAL_TYPE);
                        authDetail.setCredentialConfigurationId("test-credential");
                        if (credentialIssuer.getAuthorizationServers() != null && !credentialIssuer.getAuthorizationServers().isEmpty()) {
                            authDetail.setLocations(Collections.singletonList(credentialIssuer.getCredentialIssuer()));
                        }
                        List<AuthorizationDetail> authDetails = List.of(authDetail);
                        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

                        // Exchange code for token with authorization_details
                        HttpPost tokenRequest = new HttpPost(openidConfig.getTokenEndpoint());
                        List<NameValuePair> parameters = new LinkedList<>();
                        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
                        parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, authorizationCode));
                        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, testClientId));
                        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_SECRET, "password"));
                        parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
                        parameters.add(new BasicNameValuePair("authorization_details", authDetailsJson));
                        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
                        tokenRequest.setEntity(formEntity);
                        try (CloseableHttpResponse tokenResponse = httpClient.execute(tokenRequest)) {
                            String tokenResponseBody = IOUtils.toString(tokenResponse.getEntity().getContent(), StandardCharsets.UTF_8);
                            assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusLine().getStatusCode());

                            // Parse authorization_details
                            List<AuthorizationDetailResponse> authDetailsResponse = parseAuthorizationDetails(tokenResponseBody);
                            assertEquals(1, authDetailsResponse.size());
                            AuthorizationDetailResponse authDetailResponse = authDetailsResponse.get(0);
                            assertEquals(OPENID_CREDENTIAL_TYPE, authDetailResponse.getType());
                            assertEquals("test-credential", authDetailResponse.getCredentialConfigurationId());
                            assertNotNull(authDetailResponse.getCredentialIdentifiers());
                            assertFalse(authDetailResponse.getCredentialIdentifiers().isEmpty());

                            // Extract access token
                            return getAccessToken(tokenResponseBody);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to process token request", e);
                    }
                },
                params -> {
                    try {
                        String accessToken = (String) params.get("accessToken");
                        CredentialRequest credentialRequest = (CredentialRequest) params.get("credentialRequest");
                        String credentialEndpoint = getBasePath(TEST_REALM_NAME) + "credential";
                        requestOffer(accessToken, credentialEndpoint, new SupportedCredentialConfiguration()
                                .setFormat(credentialRequest.getFormat())
                                .setId(credentialRequest.getCredentialIdentifier()), new CredentialResponseHandler());
                    } catch (IOException | VerificationException e) {
                        fail("Failed to request credential: " + e.getMessage());
                    }
                }
        );
    }

    @Test
    public void testAuthorizationCodeWithAuthorizationDetailsFormat() throws Exception {
        testCredentialIssuanceWithAuthZCodeFlow(
                (testClientId, testScope) -> {
                    try {
                        oauth.clientId(testClientId)
                                .scope(testScope)
                                .openid(false);

                        AuthorizationEndpointResponse authResponse = oauth.doLogin("john", "password");
                        String authorizationCode = authResponse.getCode();
                        assertNotNull("Authorization code should be present", authorizationCode);

                        UriBuilder builder = UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT);
                        URI oid4vciDiscoveryUri = RealmsResource.wellKnownProviderUrl(builder).build(TEST_REALM_NAME, OID4VCIssuerWellKnownProviderFactory.PROVIDER_ID);
                        HttpGet getIssuerMetadata = new HttpGet(oid4vciDiscoveryUri);
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
                        authDetail.setFormat(JWT_VC);
                        if (credentialIssuer.getAuthorizationServers() != null && !credentialIssuer.getAuthorizationServers().isEmpty()) {
                            authDetail.setLocations(Collections.singletonList(credentialIssuer.getCredentialIssuer()));
                        }
                        List<AuthorizationDetail> authDetails = List.of(authDetail);
                        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

                        HttpPost tokenRequest = new HttpPost(openidConfig.getTokenEndpoint());
                        List<NameValuePair> parameters = new LinkedList<>();
                        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
                        parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, authorizationCode));
                        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, testClientId));
                        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_SECRET, "password"));
                        parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
                        parameters.add(new BasicNameValuePair("authorization_details", authDetailsJson));
                        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
                        tokenRequest.setEntity(formEntity);
                        try (CloseableHttpResponse tokenResponse = httpClient.execute(tokenRequest)) {
                            String tokenResponseBody = IOUtils.toString(tokenResponse.getEntity().getContent(), StandardCharsets.UTF_8);
                            assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusLine().getStatusCode());

                            List<AuthorizationDetailResponse> authDetailsResponse = parseAuthorizationDetails(tokenResponseBody);
                            assertEquals(1, authDetailsResponse.size());
                            AuthorizationDetailResponse authDetailResponse = authDetailsResponse.get(0);
                            assertEquals(OPENID_CREDENTIAL_TYPE, authDetailResponse.getType());
                            assertEquals(JWT_VC, authDetailResponse.getFormat());
                            assertNotNull(authDetailResponse.getCredentialIdentifiers());
                            assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());
                            String formatIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);

                            return getAccessToken(tokenResponseBody);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to process token request", e);
                    }
                },
                params -> {
                    try {
                        String accessToken = (String) params.get("accessToken");
                        CredentialRequest credentialRequest = (CredentialRequest) params.get("credentialRequest");
                        String credentialEndpoint = getBasePath(TEST_REALM_NAME) + "credential";
                        requestOffer(accessToken, credentialEndpoint,
                                new SupportedCredentialConfiguration()
                                        .setFormat(credentialRequest.getFormat())
                                        .setId(credentialRequest.getCredentialIdentifier()),
                                new CredentialResponseHandler());
                    } catch (IOException | VerificationException e) {
                        fail("Failed to request credential: " + e.getMessage());
                    }
                }
        );
    }

    @Test
    public void testCredentialIdentifierPersistenceInSession() throws Exception {
        String token = getBearerToken(oauth);

        // First token request - should generate new identifier
        // Get a fresh credential offer URI for the first request
        HttpGet getCredentialOfferURI1 = new HttpGet(getBasePath(TEST_REALM_NAME) + "credential-offer-uri?credential_configuration_id=test-credential");
        getCredentialOfferURI1.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        CredentialOfferURI credentialOfferURI1;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOfferURI1)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialOfferURI1 = JsonSerialization.readValue(s, CredentialOfferURI.class);
        }

        // Get the credential offer for the first request
        HttpGet getCredentialOffer1 = new HttpGet(credentialOfferURI1.getIssuer() + "/" + credentialOfferURI1.getNonce());
        CredentialsOffer credentialsOffer1;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOffer1)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialsOffer1 = JsonSerialization.readValue(s, CredentialsOffer.class);
        }

        // Get the issuer metadata
        HttpGet getIssuerMetadata = new HttpGet(credentialsOffer1.getCredentialIssuer() + "/.well-known/openid-credential-issuer");
        CredentialIssuer credentialIssuer;
        try (CloseableHttpResponse response = httpClient.execute(getIssuerMetadata)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialIssuer = JsonSerialization.readValue(s, CredentialIssuer.class);
        }

        // Get the openid-configuration
        HttpGet getOpenidConfiguration = new HttpGet(credentialIssuer.getAuthorizationServers().get(0) + "/.well-known/openid-configuration");
        OIDCConfigurationRepresentation openidConfig;
        try (CloseableHttpResponse response = httpClient.execute(getOpenidConfiguration)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            openidConfig = JsonSerialization.readValue(s, OIDCConfigurationRepresentation.class);
        }

        // Prepare authorization_details with credential_configuration_id (AFTER credentialIssuer is available)
        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId("test-credential");
        if (credentialIssuer.getAuthorizationServers() != null && !credentialIssuer.getAuthorizationServers().isEmpty()) {
            authDetail.setLocations(Collections.singletonList(credentialIssuer.getCredentialIssuer()));
        }
        List<AuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        HttpPost postPreAuthorizedCode1 = new HttpPost(openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters1 = new LinkedList<>();
        parameters1.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters1.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, credentialsOffer1.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        parameters1.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        UrlEncodedFormEntity formEntity1 = new UrlEncodedFormEntity(parameters1, StandardCharsets.UTF_8);
        postPreAuthorizedCode1.setEntity(formEntity1);

        String firstIdentifier = null;
        try (CloseableHttpResponse tokenResponse1 = httpClient.execute(postPreAuthorizedCode1)) {
            assertEquals(HttpStatus.SC_OK, tokenResponse1.getStatusLine().getStatusCode());
            String responseBody1 = IOUtils.toString(tokenResponse1.getEntity().getContent(), StandardCharsets.UTF_8);

            List<AuthorizationDetailResponse> authDetailsResponse1 = parseAuthorizationDetails(responseBody1);
            assertEquals(1, authDetailsResponse1.size());
            AuthorizationDetailResponse authDetailResponse1 = authDetailsResponse1.get(0);
            assertEquals("test-credential", authDetailResponse1.getCredentialConfigurationId());
            assertNotNull(authDetailResponse1.getCredentialIdentifiers());
            assertEquals(1, authDetailResponse1.getCredentialIdentifiers().size());
            firstIdentifier = authDetailResponse1.getCredentialIdentifiers().get(0);
            assertNotNull("Identifier should not be null", firstIdentifier);
            assertFalse("Identifier should not be empty", firstIdentifier.isEmpty());
            try {
                UUID.fromString(firstIdentifier);
            } catch (IllegalArgumentException e) {
                fail("Identifier should be a valid UUID, but was: " + firstIdentifier);
            }
        }

        // Second token request with same credential_configuration_id - should reuse the same identifier
        // Get a fresh credential offer URI for the second request
        HttpGet getCredentialOfferURI2 = new HttpGet(getBasePath(TEST_REALM_NAME) + "credential-offer-uri?credential_configuration_id=test-credential");
        getCredentialOfferURI2.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        CredentialOfferURI credentialOfferURI2;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOfferURI2)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialOfferURI2 = JsonSerialization.readValue(s, CredentialOfferURI.class);
        }

        // Get the credential offer for the second request
        HttpGet getCredentialOffer2 = new HttpGet(credentialOfferURI2.getIssuer() + "/" + credentialOfferURI2.getNonce());
        CredentialsOffer credentialsOffer2;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOffer2)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialsOffer2 = JsonSerialization.readValue(s, CredentialsOffer.class);
        }

        // Get the issuer metadata for the second request
        getIssuerMetadata = new HttpGet(credentialsOffer2.getCredentialIssuer() + "/.well-known/openid-credential-issuer");
        try (CloseableHttpResponse response = httpClient.execute(getIssuerMetadata)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialIssuer = JsonSerialization.readValue(s, CredentialIssuer.class);
        }

        // Prepare authorization_details again for the second request
        authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId("test-credential");
        if (credentialIssuer.getAuthorizationServers() != null && !credentialIssuer.getAuthorizationServers().isEmpty()) {
            authDetail.setLocations(Collections.singletonList(credentialIssuer.getCredentialIssuer()));
        }
        authDetails = List.of(authDetail);
        authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        HttpPost postPreAuthorizedCode2 = new HttpPost(openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters2 = new LinkedList<>();
        parameters2.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters2.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, credentialsOffer2.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        parameters2.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        UrlEncodedFormEntity formEntity2 = new UrlEncodedFormEntity(parameters2, StandardCharsets.UTF_8);
        postPreAuthorizedCode2.setEntity(formEntity2);

        String secondIdentifier = null;
        try (CloseableHttpResponse tokenResponse2 = httpClient.execute(postPreAuthorizedCode2)) {
            assertEquals(HttpStatus.SC_OK, tokenResponse2.getStatusLine().getStatusCode());
            String responseBody2 = IOUtils.toString(tokenResponse2.getEntity().getContent(), StandardCharsets.UTF_8);

            List<AuthorizationDetailResponse> authDetailsResponse2 = parseAuthorizationDetails(responseBody2);
            assertEquals(1, authDetailsResponse2.size());
            AuthorizationDetailResponse authDetailResponse2 = authDetailsResponse2.get(0);
            assertEquals("test-credential", authDetailResponse2.getCredentialConfigurationId());
            assertNotNull(authDetailResponse2.getCredentialIdentifiers());
            assertEquals(1, authDetailResponse2.getCredentialIdentifiers().size());
            secondIdentifier = authDetailResponse2.getCredentialIdentifiers().get(0);
            assertNotNull("Identifier should not be null", secondIdentifier);
            assertFalse("Identifier should not be empty", secondIdentifier.isEmpty());
            try {
                UUID.fromString(secondIdentifier);
            } catch (IllegalArgumentException e) {
                fail("Identifier should be a valid UUID, but was: " + secondIdentifier);
            }
            // Should be the same identifier as the first request (same session)
            assertEquals("Credential identifiers should be the same within the same session", firstIdentifier, secondIdentifier);
        }

        // Third token request in a new session - should generate a different identifier
        deleteAllCookiesForRealm(TEST_REALM_NAME);
        String token2 = getBearerToken(oauth);
        HttpGet getCredentialOfferURI3 = new HttpGet(getBasePath(TEST_REALM_NAME) + "credential-offer-uri?credential_configuration_id=test-credential");
        getCredentialOfferURI3.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token2);
        CredentialOfferURI credentialOfferURI3;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOfferURI3)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialOfferURI3 = JsonSerialization.readValue(s, CredentialOfferURI.class);
        }
        HttpGet getCredentialOffer3 = new HttpGet(credentialOfferURI3.getIssuer() + "/" + credentialOfferURI3.getNonce());
        CredentialsOffer credentialsOffer3;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOffer3)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialsOffer3 = JsonSerialization.readValue(s, CredentialsOffer.class);
        }
        getIssuerMetadata = new HttpGet(credentialsOffer3.getCredentialIssuer() + "/.well-known/openid-credential-issuer");
        try (CloseableHttpResponse response = httpClient.execute(getIssuerMetadata)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialIssuer = JsonSerialization.readValue(s, CredentialIssuer.class);
        }
        getOpenidConfiguration = new HttpGet(credentialIssuer.getAuthorizationServers().get(0) + "/.well-known/openid-configuration");
        try (CloseableHttpResponse response = httpClient.execute(getOpenidConfiguration)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            openidConfig = JsonSerialization.readValue(s, OIDCConfigurationRepresentation.class);
        }
        authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId("test-credential");
        if (credentialIssuer.getAuthorizationServers() != null && !credentialIssuer.getAuthorizationServers().isEmpty()) {
            authDetail.setLocations(Collections.singletonList(credentialIssuer.getCredentialIssuer()));
        }
        authDetails = List.of(authDetail);
        authDetailsJson = JsonSerialization.writeValueAsString(authDetails);
        HttpPost postPreAuthorizedCode3 = new HttpPost(openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters3 = new LinkedList<>();
        parameters3.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters3.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, credentialsOffer3.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        parameters3.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        UrlEncodedFormEntity formEntity3 = new UrlEncodedFormEntity(parameters3, StandardCharsets.UTF_8);
        postPreAuthorizedCode3.setEntity(formEntity3);
        String thirdIdentifier = null;
        try (CloseableHttpResponse tokenResponse3 = httpClient.execute(postPreAuthorizedCode3)) {
            assertEquals(HttpStatus.SC_OK, tokenResponse3.getStatusLine().getStatusCode());
            String responseBody3 = IOUtils.toString(tokenResponse3.getEntity().getContent(), StandardCharsets.UTF_8);
            List<AuthorizationDetailResponse> authDetailsResponse3 = parseAuthorizationDetails(responseBody3);
            assertEquals(1, authDetailsResponse3.size());
            AuthorizationDetailResponse authDetailResponse3 = authDetailsResponse3.get(0);
            assertEquals("test-credential", authDetailResponse3.getCredentialConfigurationId());
            assertNotNull(authDetailResponse3.getCredentialIdentifiers());
            assertEquals(1, authDetailResponse3.getCredentialIdentifiers().size());
            thirdIdentifier = authDetailResponse3.getCredentialIdentifiers().get(0);
            assertNotNull("Identifier should not be null", thirdIdentifier);
            assertFalse("Identifier should not be empty", thirdIdentifier.isEmpty());
            try {
                UUID.fromString(thirdIdentifier);
            } catch (IllegalArgumentException e) {
                fail("Identifier should be a valid UUID, but was: " + thirdIdentifier);
            }
            // Should be different from the first identifier (different session)
            assertFalse("Different sessions should generate different identifiers for the same credential_configuration_id", firstIdentifier.equals(thirdIdentifier));
        }
    }

    @Test
    public void testCredentialIdentifierDifferentSessions() throws Exception {
        // Test that different sessions generate different identifiers for the same credential_configuration_id

        // 1. First session - get a token and make a request
        String token1 = getBearerToken(oauth);

        // Get credential offer URI for first session
        HttpGet getCredentialOfferURI1 = new HttpGet(getBasePath(TEST_REALM_NAME) + "credential-offer-uri?credential_configuration_id=test-credential");
        getCredentialOfferURI1.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token1);
        CredentialOfferURI credentialOfferURI1;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOfferURI1)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialOfferURI1 = JsonSerialization.readValue(s, CredentialOfferURI.class);
        }

        // Get credential offer for first session
        HttpGet getCredentialOffer1 = new HttpGet(credentialOfferURI1.getIssuer() + "/" + credentialOfferURI1.getNonce());
        CredentialsOffer credentialsOffer1;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOffer1)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialsOffer1 = JsonSerialization.readValue(s, CredentialsOffer.class);
        }

        // Get issuer metadata and openid configuration
        HttpGet getIssuerMetadata = new HttpGet(credentialsOffer1.getCredentialIssuer() + "/.well-known/openid-credential-issuer");
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

        // Prepare authorization_details with credential_configuration_id (AFTER credentialIssuer is available)
        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId("test-credential");
        if (credentialIssuer.getAuthorizationServers() != null && !credentialIssuer.getAuthorizationServers().isEmpty()) {
            authDetail.setLocations(Collections.singletonList(credentialIssuer.getCredentialIssuer()));
        }
        List<AuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        // Make token request for first session
        HttpPost postPreAuthorizedCode1 = new HttpPost(openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters1 = new LinkedList<>();
        parameters1.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters1.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, credentialsOffer1.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        parameters1.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        UrlEncodedFormEntity formEntity1 = new UrlEncodedFormEntity(parameters1, StandardCharsets.UTF_8);
        postPreAuthorizedCode1.setEntity(formEntity1);

        String firstSessionIdentifier = null;
        try (CloseableHttpResponse tokenResponse1 = httpClient.execute(postPreAuthorizedCode1)) {
            assertEquals(HttpStatus.SC_OK, tokenResponse1.getStatusLine().getStatusCode());
            String responseBody1 = IOUtils.toString(tokenResponse1.getEntity().getContent(), StandardCharsets.UTF_8);

            List<AuthorizationDetailResponse> authDetailsResponse1 = parseAuthorizationDetails(responseBody1);
            assertEquals(1, authDetailsResponse1.size());
            AuthorizationDetailResponse authDetailResponse1 = authDetailsResponse1.get(0);
            assertEquals("test-credential", authDetailResponse1.getCredentialConfigurationId());
            assertNotNull(authDetailResponse1.getCredentialIdentifiers());
            assertEquals(1, authDetailResponse1.getCredentialIdentifiers().size());
            firstSessionIdentifier = authDetailResponse1.getCredentialIdentifiers().get(0);
            assertNotNull("Identifier should not be null", firstSessionIdentifier);
            assertFalse("Identifier should not be empty", firstSessionIdentifier.isEmpty());
            try {
                UUID.fromString(firstSessionIdentifier);
            } catch (IllegalArgumentException e) {
                fail("Identifier should be a valid UUID, but was: " + firstSessionIdentifier);
            }
        }

        // 2. Second session - get a new token and make a request
        // Clear cookies to ensure a new session
        deleteAllCookiesForRealm(TEST_REALM_NAME);
        String token2 = getBearerToken(oauth);

        // Get credential offer URI for second session
        HttpGet getCredentialOfferURI2 = new HttpGet(getBasePath(TEST_REALM_NAME) + "credential-offer-uri?credential_configuration_id=test-credential");
        getCredentialOfferURI2.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token2);
        CredentialOfferURI credentialOfferURI2;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOfferURI2)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialOfferURI2 = JsonSerialization.readValue(s, CredentialOfferURI.class);
        }

        // Get credential offer for second session
        HttpGet getCredentialOffer2 = new HttpGet(credentialOfferURI2.getIssuer() + "/" + credentialOfferURI2.getNonce());
        CredentialsOffer credentialsOffer2;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOffer2)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialsOffer2 = JsonSerialization.readValue(s, CredentialsOffer.class);
        }

        // Get issuer metadata and openid configuration for second session
        getIssuerMetadata = new HttpGet(credentialsOffer2.getCredentialIssuer() + "/.well-known/openid-credential-issuer");
        try (CloseableHttpResponse response = httpClient.execute(getIssuerMetadata)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialIssuer = JsonSerialization.readValue(s, CredentialIssuer.class);
        }
        getOpenidConfiguration = new HttpGet(credentialIssuer.getAuthorizationServers().get(0) + "/.well-known/openid-configuration");
        try (CloseableHttpResponse response = httpClient.execute(getOpenidConfiguration)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            openidConfig = JsonSerialization.readValue(s, OIDCConfigurationRepresentation.class);
        }

        // Prepare authorization_details again for the second session
        authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId("test-credential");
        if (credentialIssuer.getAuthorizationServers() != null && !credentialIssuer.getAuthorizationServers().isEmpty()) {
            authDetail.setLocations(Collections.singletonList(credentialIssuer.getCredentialIssuer()));
        }
        authDetails = List.of(authDetail);
        authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        // Make token request for second session
        HttpPost postPreAuthorizedCode2 = new HttpPost(openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters2 = new LinkedList<>();
        parameters2.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters2.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, credentialsOffer2.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        parameters2.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        UrlEncodedFormEntity formEntity2 = new UrlEncodedFormEntity(parameters2, StandardCharsets.UTF_8);
        postPreAuthorizedCode2.setEntity(formEntity2);

        try (CloseableHttpResponse tokenResponse2 = httpClient.execute(postPreAuthorizedCode2)) {
            assertEquals(HttpStatus.SC_OK, tokenResponse2.getStatusLine().getStatusCode());
            String responseBody2 = IOUtils.toString(tokenResponse2.getEntity().getContent(), StandardCharsets.UTF_8);

            List<AuthorizationDetailResponse> authDetailsResponse2 = parseAuthorizationDetails(responseBody2);
            assertEquals(1, authDetailsResponse2.size());
            AuthorizationDetailResponse authDetailResponse2 = authDetailsResponse2.get(0);
            assertEquals("test-credential", authDetailResponse2.getCredentialConfigurationId());
            assertNotNull(authDetailResponse2.getCredentialIdentifiers());
            assertEquals(1, authDetailResponse2.getCredentialIdentifiers().size());
            String secondSessionIdentifier = authDetailResponse2.getCredentialIdentifiers().get(0);

            // Should be different from the first session identifier
            assertFalse("Different sessions should generate different identifiers for the same credential_configuration_id",
                    firstSessionIdentifier.equals(secondSessionIdentifier));
            assertNotNull("Second session identifier should not be null", secondSessionIdentifier);
            assertFalse("Second session identifier should not be empty", secondSessionIdentifier.isEmpty());
            try {
                UUID.fromString(secondSessionIdentifier);
            } catch (IllegalArgumentException e) {
                fail("Second session identifier should be a valid UUID, but was: " + secondSessionIdentifier);
            }
        }
    }
}
