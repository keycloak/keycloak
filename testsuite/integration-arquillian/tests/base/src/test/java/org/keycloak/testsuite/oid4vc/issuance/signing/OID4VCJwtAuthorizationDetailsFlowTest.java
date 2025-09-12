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
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailsResponse;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.util.JsonSerialization;
import org.keycloak.models.oid4vci.CredentialScopeModel;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailsProcessor.OPENID_CREDENTIAL_TYPE;
import static org.keycloak.protocol.oid4vc.model.Format.JWT_VC;

public class OID4VCJwtAuthorizationDetailsFlowTest extends OID4VCIssuerEndpointTest {

    private static class Oid4vcTestContext {
        CredentialsOffer credentialsOffer;
        CredentialIssuer credentialIssuer;
        OIDCConfigurationRepresentation openidConfig;
    }

    private Oid4vcTestContext prepareOid4vcTestContext(String token) throws Exception {
        Oid4vcTestContext ctx = new Oid4vcTestContext();

        String credentialConfigurationId = jwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);
        HttpGet getCredentialOfferURI = new HttpGet(getBasePath(TEST_REALM_NAME) + "credential-offer-uri?credential_configuration_id=" + credentialConfigurationId);
        getCredentialOfferURI.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        CredentialOfferURI credentialOfferURI;
        try (CloseableHttpResponse response = httpClient.execute(getCredentialOfferURI)) {
            int status = response.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_OK) {
                String body = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            }
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
    public void testPreAuthorizedCodeWithAuthorizationDetailsFormat() throws Exception {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setFormat(JWT_VC);
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
            assertEquals(JWT_VC, authDetailResponse.getFormat());
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
    public void testPreAuthorizedCodeWithInvalidAuthorizationDetails() throws Exception {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId(jwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setFormat(JWT_VC); // Invalid: format should not be combined with credential_configuration_id
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
            assertTrue("Error message should mention authorization_details processing error",
                    responseBody.contains("Error when processing authorization_details"));
        }
    }

    @Test
    public void testAuthorizationCodeWithAuthorizationDetailsFormat() throws Exception {
        // Simulate the authorization code flow for JWT VC with valid authorization_details
        String testClientId = client.getClientId();
        String testScope = jwtTypeCredentialClientScope.getName();
        oauth.clientId(testClientId)
                .scope(testScope)
                .openid(false);

        // Get authorization code
        org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse authResponse = oauth.doLogin("john", "password");
        String authorizationCode = authResponse.getCode();
        assertNotNull("Authorization code should be present", authorizationCode);

        // Get token endpoint from .well-known
        java.net.URI oid4vciDiscoveryUri = org.keycloak.services.resources.RealmsResource.wellKnownProviderUrl(
                        jakarta.ws.rs.core.UriBuilder.fromUri(org.keycloak.testsuite.util.oauth.OAuthClient.AUTH_SERVER_ROOT))
                .build(TEST_REALM_NAME, org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProviderFactory.PROVIDER_ID);
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
        authDetail.setLocations(Collections.singletonList(credentialIssuer.getCredentialIssuer()));

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
            List<OID4VCAuthorizationDetailsResponse> authDetailsResponse = parseAuthorizationDetails(tokenResponseBody);
            assertEquals(1, authDetailsResponse.size());
            OID4VCAuthorizationDetailsResponse authDetailResponse = authDetailsResponse.get(0);
            assertEquals(OPENID_CREDENTIAL_TYPE, authDetailResponse.getType());
            assertEquals(JWT_VC, authDetailResponse.getFormat());
            assertNotNull(authDetailResponse.getCredentialIdentifiers());
            assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());
            String formatIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
            assertNotNull("Identifier should not be null", formatIdentifier);
            assertFalse("Identifier should not be empty", formatIdentifier.isEmpty());
            try {
                UUID.fromString(formatIdentifier);
            } catch (IllegalArgumentException e) {
                fail("Identifier should be a valid UUID, but was: " + formatIdentifier);
            }
        }
    }

    @Test
    public void testAuthorizationCodeWithInvalidAuthorizationDetails() throws Exception {
        // Simulate the authorization code flow for JWT VC with invalid authorization_details
        String testClientId = client.getClientId();
        String testScope = jwtTypeCredentialClientScope.getName();
        oauth.clientId(testClientId)
                .scope(testScope)
                .openid(false);

        // Get authorization code
        org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse authResponse = oauth.doLogin("john", "password");
        String authorizationCode = authResponse.getCode();
        assertNotNull("Authorization code should be present", authorizationCode);

        // Get token endpoint from .well-known
        java.net.URI oid4vciDiscoveryUri = org.keycloak.services.resources.RealmsResource.wellKnownProviderUrl(
                        jakarta.ws.rs.core.UriBuilder.fromUri(org.keycloak.testsuite.util.oauth.OAuthClient.AUTH_SERVER_ROOT))
                .build(TEST_REALM_NAME, org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProviderFactory.PROVIDER_ID);
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
        authDetail.setCredentialConfigurationId(jwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setFormat(JWT_VC); // Invalid: format should not be combined with credential_configuration_id
        authDetail.setLocations(Collections.singletonList(credentialIssuer.getCredentialIssuer()));

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
            assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusLine().getStatusCode());
            assertTrue("Error message should mention authorization_details processing error",
                    tokenResponseBody.contains("Error when processing authorization_details"));
        }
    }
}
