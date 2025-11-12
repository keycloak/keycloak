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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
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

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
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
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.JwtProof;
import org.keycloak.protocol.oid4vc.model.OfferUriType;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCode;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedGrant;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.OID4VCConstants.CREDENTIAL_SUBJECT;

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
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);

            OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CorsErrorResponseException exception = Assert.assertThrows(CorsErrorResponseException.class, () ->
                    oid4VCIssuerEndpoint.getCredentialOfferURI("inexistent-id", OfferUriType.URI, 0, 0)
            );
            assertEquals("Should return BAD_REQUEST", Response.Status.BAD_REQUEST.getStatusCode(),
                    exception.getResponse().getStatus());
        }));
    }

    @Test
    public void testGetCredentialOfferUriUnauthorized() {
        testingClient.server(TEST_REALM_NAME).run((session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(null);
            OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CorsErrorResponseException exception = Assert.assertThrows(CorsErrorResponseException.class, () ->
                    oid4VCIssuerEndpoint.getCredentialOfferURI("test-credential", OfferUriType.URI, 0, 0)
            );
            assertEquals("Should return BAD_REQUEST", Response.Status.BAD_REQUEST.getStatusCode(),
                    exception.getResponse().getStatus());
        }));
    }

    @Test
    public void testGetCredentialOfferUriInvalidToken() {
        testingClient.server(TEST_REALM_NAME).run((session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString("invalid-token");
            OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CorsErrorResponseException exception = Assert.assertThrows(CorsErrorResponseException.class, () ->
                    oid4VCIssuerEndpoint.getCredentialOfferURI("test-credential", OfferUriType.URI, 0, 0)
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
                AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(
                        session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                Response response = oid4VCIssuerEndpoint.getCredentialOfferURI(credentialConfigurationId,
                        OfferUriType.URI,
                        0,
                        0);

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
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
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
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
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
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
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
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                        authenticator.setTokenString(token);
                        String sessionCode = prepareSessionCode(session, authenticator, "invalidNote");
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        issuerEndpoint.getCredentialOffer(sessionCode);
                    }));
        });
    }

    @Test
    public void testGetCredentialOffer() {
        String token = getBearerToken(oauth);
        testingClient
                .server(TEST_REALM_NAME)
                .run((session) -> {
                    AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                    authenticator.setTokenString(token);
                    CredentialsOffer credentialsOffer = new CredentialsOffer()
                            .setCredentialIssuer("the-issuer")
                            .setGrants(new PreAuthorizedGrant().setPreAuthorizedCode(new PreAuthorizedCode().setPreAuthorizedCode("the-code")))
                            .setCredentialConfigurationIds(List.of("credential-configuration-id"));

                    String sessionCode = prepareSessionCode(session, authenticator, JsonSerialization.writeValueAsString(credentialsOffer));
                    // The cache transactions need to be committed explicitly in the test. Without that, the OAuth2Code will only be committed to
                    // the cache after .run((session)-> ...)
                    session.getTransactionManager().commit();
                    OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                    Response credentialOfferResponse = issuerEndpoint.getCredentialOffer(sessionCode);
                    assertEquals("The offer should have been returned.", HttpStatus.SC_OK, credentialOfferResponse.getStatus());
                    Object credentialOfferEntity = credentialOfferResponse.getEntity();
                    assertNotNull("An actual offer should be in the response.", credentialOfferEntity);

                    CredentialsOffer retrievedCredentialsOffer = JsonSerialization.mapper.convertValue(credentialOfferEntity, CredentialsOffer.class);
                    assertEquals("The offer should be the one prepared with for the session.", credentialsOffer, retrievedCredentialsOffer);
                });
    }

// ----- requestCredential

    @Test
    public void testRequestCredentialUnauthorized() {
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
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
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
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
        String token = getBearerToken(oauth, client, scopeName);

        try {
            withCausePropagation(() -> {
                testingClient.server(TEST_REALM_NAME).run((session -> {
                    AppAuthManager.BearerTokenAuthenticator authenticator =
                            new AppAuthManager.BearerTokenAuthenticator(session);
                    authenticator.setTokenString(token);

                    // Prepare the issue endpoint with no credential builders.
                    OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator, Map.of());

                    CredentialRequest credentialRequest =
                            new CredentialRequest().setCredentialConfigurationId(credentialConfigurationId);

                    String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
                    issuerEndpoint.requestCredential(requestPayload);
                }));
            });
            Assert.fail("Should have thrown an exception");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof BadRequestException);
            Assert.assertEquals("No credential builder found for format jwt_vc", e.getMessage());
        }
    }

    @Test(expected = BadRequestException.class)
    public void testRequestCredentialUnsupportedCredential() throws Throwable {
        String token = getBearerToken(oauth);
        withCausePropagation(() -> {
            testingClient.server(TEST_REALM_NAME).run(session -> {
                AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
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
        final String scopeName = jwtTypeCredentialClientScope.getName();
        String token = getBearerToken(oauth, client, scopeName);
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialIdentifier(scopeName);

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
    public void testRequestCredentialWithConfigurationIdNotSet() {
        final String scopeName = minimalJwtTypeCredentialClientScope.getName();
        String token = getBearerToken(oauth, client, scopeName);
        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialIdentifier(scopeName);

            String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

            Response credentialResponse = issuerEndpoint.requestCredential(requestPayload);
            assertEquals("The credential request should be answered successfully.",
                    HttpStatus.SC_OK, credentialResponse.getStatus());
            assertNotNull("A credential should be responded.", credentialResponse.getEntity());
            CredentialResponse credentialResponseVO = JsonSerialization.mapper
                    .convertValue(credentialResponse.getEntity(), CredentialResponse.class);
            String credentialString = (String) credentialResponseVO.getCredentials().get(0).getCredential();
            SdJwtVP sdJwtVP = SdJwtVP.of(credentialString);
            assertNotNull("A valid credential string should have been responded", sdJwtVP);
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
        HttpGet getCredentialOfferURI = new HttpGet(getBasePath(TEST_REALM_NAME)
                + "credential-offer-uri?credential_configuration_id="
                + credentialConfigurationId);
        getCredentialOfferURI.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        CloseableHttpResponse credentialOfferURIResponse = httpClient.execute(getCredentialOfferURI);

        assertEquals("A valid offer uri should be returned",
                HttpStatus.SC_OK,
                credentialOfferURIResponse.getStatusLine().getStatusCode());
        String s = IOUtils.toString(credentialOfferURIResponse.getEntity().getContent(), StandardCharsets.UTF_8);
        CredentialOfferURI credentialOfferURI = JsonSerialization.readValue(s, CredentialOfferURI.class);

        // 2. Using the uri to get the actual credential offer
        HttpGet getCredentialOffer = new HttpGet(credentialOfferURI.getIssuer() + "/" + credentialOfferURI.getNonce());
        CloseableHttpResponse credentialOfferResponse = httpClient.execute(getCredentialOffer);

        assertEquals("A valid offer should be returned", HttpStatus.SC_OK, credentialOfferResponse.getStatusLine().getStatusCode());
        s = IOUtils.toString(credentialOfferResponse.getEntity().getContent(), StandardCharsets.UTF_8);
        CredentialsOffer credentialsOffer = JsonSerialization.readValue(s, CredentialsOffer.class);

        // 3. Get the issuer metadata
        HttpGet getIssuerMetadata = new HttpGet(credentialsOffer.getCredentialIssuer() + "/.well-known/openid-credential-issuer");
        CloseableHttpResponse issuerMetadataResponse = httpClient.execute(getIssuerMetadata);
        assertEquals(HttpStatus.SC_OK, issuerMetadataResponse.getStatusLine().getStatusCode());
        s = IOUtils.toString(issuerMetadataResponse.getEntity().getContent(), StandardCharsets.UTF_8);
        CredentialIssuer credentialIssuer = JsonSerialization.readValue(s, CredentialIssuer.class);

        assertEquals("We only expect one authorization server.", 1, credentialIssuer.getAuthorizationServers().size());

        // 4. Get the openid-configuration
        HttpGet getOpenidConfiguration = new HttpGet(credentialIssuer.getAuthorizationServers().get(0) + "/.well-known/openid-configuration");
        CloseableHttpResponse openidConfigResponse = httpClient.execute(getOpenidConfiguration);
        assertEquals(HttpStatus.SC_OK, openidConfigResponse.getStatusLine().getStatusCode());
        s = IOUtils.toString(openidConfigResponse.getEntity().getContent(), StandardCharsets.UTF_8);
        OIDCConfigurationRepresentation openidConfig = JsonSerialization.readValue(s, OIDCConfigurationRepresentation.class);

        assertNotNull("A token endpoint should be included.", openidConfig.getTokenEndpoint());
        assertTrue("The pre-authorized code should be supported.", openidConfig.getGrantTypesSupported().contains(PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));

        // 5. Get an access token for the pre-authorized code
        HttpPost postPreAuthorizedCode = new HttpPost(openidConfig.getTokenEndpoint());
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE));
        parameters.add(new BasicNameValuePair(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        postPreAuthorizedCode.setEntity(formEntity);
        AccessTokenResponse accessTokenResponse = new AccessTokenResponse(httpClient.execute(postPreAuthorizedCode));
        assertEquals(HttpStatus.SC_OK, accessTokenResponse.getStatusCode());
        String theToken = accessTokenResponse.getAccessToken();

        // 6. Get the credential
        credentialsOffer.getCredentialConfigurationIds().stream()
                .map(offeredCredentialId -> credentialIssuer.getCredentialsSupported().get(offeredCredentialId))
                .forEach(supportedCredential -> {
                    try {
                        requestCredential(theToken,
                                credentialIssuer.getCredentialEndpoint(),
                                supportedCredential,
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
    public void testCredentialIssuanceWithAuthZCodeWithScopeMatched() {
        BiFunction<String, String, String> getAccessToken = (testClientId, testScope) -> {
            return getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        };

        Consumer<Map<String, Object>> sendCredentialRequest = m -> {
            String accessToken = (String) m.get("accessToken");
            WebTarget credentialTarget = (WebTarget) m.get("credentialTarget");
            CredentialRequest credentialRequest = (CredentialRequest) m.get("credentialRequest");
            assertEquals("Credential configuration id should match",
                    jwtTypeCredentialClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID),
                    credentialRequest.getCredentialConfigurationId());

            try (Response response = credentialTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken)
                    .post(Entity.json(credentialRequest))) {
                if (response.getStatus() != 200) {
                    String errorBody = response.readEntity(String.class);
                    System.out.println("Error Response: " + errorBody);
                }
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
        };

        testCredentialIssuanceWithAuthZCodeFlow(jwtTypeCredentialClientScope, getAccessToken, sendCredentialRequest);
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
                assertTrue("Error response should mention UNKNOWN_CREDENTIAL_CONFIGURATION or scope",
                        errorJson.contains("UNKNOWN_CREDENTIAL_CONFIGURATION") || errorJson.contains("scope"));
            }
        };

        testCredentialIssuanceWithAuthZCodeFlow(sdJwtTypeCredentialClientScope, getAccessToken, sendCredentialRequest);
    }

    @Test
    public void testRequestCredentialWithNotificationId() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        final String scopeName = jwtTypeCredentialClientScope.getName();

        testingClient.server(TEST_REALM_NAME).run((session) -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialIdentifier(scopeName);

            String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

            // First credential request
            Response response1 = issuerEndpoint.requestCredential(requestPayload);
            assertEquals("The credential request should be successful.", HttpStatus.SC_OK, response1.getStatus());
            CredentialResponse credentialResponse1 = JsonSerialization.mapper.convertValue(
                    response1.getEntity(), CredentialResponse.class);
            assertNotNull("Credential response should not be null", credentialResponse1);
            assertNotNull("Credential should be present", credentialResponse1.getCredentials());
            assertNotNull("Notification ID should be present", credentialResponse1.getNotificationId());
            assertFalse("Notification ID should not be empty", credentialResponse1.getNotificationId().isEmpty());
            // Second credential request
            Response response2 = issuerEndpoint.requestCredential(requestPayload);
            assertEquals("The second credential request should be successful.", HttpStatus.SC_OK, response2.getStatus());
            CredentialResponse credentialResponse2 = JsonSerialization.mapper.convertValue(
                    response2.getEntity(), CredentialResponse.class);
            assertNotEquals("Notification IDs should be unique",
                    credentialResponse1.getNotificationId(), credentialResponse2.getNotificationId());
        });
    }

    /**
     * This is testing the multiple credential issuance flow in a single call with proofs
     */
    @Test
    public void testRequestMultipleCredentialsWithProofs() {
        final String scopeName = jwtTypeCredentialClientScope.getName();
        String token = getBearerToken(oauth, client, scopeName);
        String cNonce = getCNonce();

        testingClient.server(TEST_REALM_NAME).run(session -> {
            try {
                AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                String issuer = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());

                String jwtProof1 = generateJwtProof(issuer, cNonce);
                String jwtProof2 = generateJwtProof(issuer, cNonce);
                Proofs proofs = new Proofs().setJwt(Arrays.asList(jwtProof1, jwtProof2));


                CredentialRequest request = new CredentialRequest()
                        .setCredentialIdentifier(scopeName)
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

                assertNotNull("Notification ID should be present", credentialResponse.getNotificationId());
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
                            Format.JWT_VC,
                            jwtVcConfig.getFormat());

                    Claims jwtVcClaims = jwtVcConfig.getCredentialMetadata() != null ? jwtVcConfig.getCredentialMetadata().getClaims() : null;
                    assertNotNull("The jwt_vc-credential can optionally provide a claims claim.",
                            jwtVcClaims);

                    assertEquals(5, jwtVcClaims.size());
                    {
                        Claim claim = jwtVcClaims.get(0);
                        assertEquals("The jwt_vc-credential claim credentialSubject.given_name is present.",
                                CREDENTIAL_SUBJECT,
                                claim.getPath().get(0));
                        assertEquals("The jwt_vc-credential claim credentialSubject.given_name is present.",
                                "given_name",
                                claim.getPath().get(1));
                        assertFalse("The jwt_vc-credential claim credentialSubject.given_name is not mandatory.",
                                claim.isMandatory());
                        assertNotNull("The jwt_vc-credential claim credentialSubject.given_name has display configured",
                                claim.getDisplay());
                        assertEquals(15, claim.getDisplay().size());
                        for (ClaimDisplay givenNameDisplay : claim.getDisplay()) {
                            assertNotNull(givenNameDisplay.getName());
                            assertNotNull(givenNameDisplay.getLocale());
                        }
                    }
                    {
                        Claim claim = jwtVcClaims.get(1);
                        assertEquals("The jwt_vc-credential claim credentialSubject.family_name is present.",
                                CREDENTIAL_SUBJECT,
                                claim.getPath().get(0));
                        assertEquals("The jwt_vc-credential claim credentialSubject.family_name is present.",
                                "family_name",
                                claim.getPath().get(1));
                        assertFalse("The jwt_vc-credential claim credentialSubject.family_name is not mandatory.",
                                claim.isMandatory());
                        assertNotNull("The jwt_vc-credential claim credentialSubject.family_name has display configured",
                                claim.getDisplay());
                        assertEquals(15, claim.getDisplay().size());
                        for (ClaimDisplay familyNameDisplay : claim.getDisplay()) {
                            assertNotNull(familyNameDisplay.getName());
                            assertNotNull(familyNameDisplay.getLocale());
                        }
                    }
                    {
                        Claim claim = jwtVcClaims.get(2);
                        assertEquals("The jwt_vc-credential claim credentialSubject.birthdate is present.",
                                CREDENTIAL_SUBJECT,
                                claim.getPath().get(0));
                        assertEquals("The jwt_vc-credential claim credentialSubject.birthdate is present.",
                                "birthdate",
                                claim.getPath().get(1));
                        assertFalse("The jwt_vc-credential claim credentialSubject.birthdate is not mandatory.",
                                claim.isMandatory());
                        assertNotNull("The jwt_vc-credential claim credentialSubject.birthdate has display configured",
                                claim.getDisplay());
                        assertEquals(15, claim.getDisplay().size());
                        for (ClaimDisplay birthDateDisplay : claim.getDisplay()) {
                            assertNotNull(birthDateDisplay.getName());
                            assertNotNull(birthDateDisplay.getLocale());
                        }
                    }
                    {
                        Claim claim = jwtVcClaims.get(3);
                        assertEquals("The jwt_vc-credential claim credentialSubject.email is present.",
                                CREDENTIAL_SUBJECT,
                                claim.getPath().get(0));
                        assertEquals("The jwt_vc-credential claim credentialSubject.email is present.",
                                "email",
                                claim.getPath().get(1));
                        assertFalse("The jwt_vc-credential claim credentialSubject.email is not mandatory.",
                                claim.isMandatory());
                        assertNotNull("The jwt_vc-credential claim credentialSubject.email has display configured",
                                claim.getDisplay());
                        assertEquals(15, claim.getDisplay().size());
                        for (ClaimDisplay birthDateDisplay : claim.getDisplay()) {
                            assertNotNull(birthDateDisplay.getName());
                            assertNotNull(birthDateDisplay.getLocale());
                        }
                    }
                    {
                        Claim claim = jwtVcClaims.get(4);
                        assertEquals("The jwt_vc-credential claim credentialSubject.scope-name is present.",
                                CREDENTIAL_SUBJECT,
                                claim.getPath().get(0));
                        assertEquals("The jwt_vc-credential claim credentialSubject.scope-name is present.",
                                "scope-name",
                                claim.getPath().get(1));
                        assertFalse("The jwt_vc-credential claim credentialSubject.scope-name is not mandatory.",
                                claim.isMandatory());
                        assertNull("The jwt_vc-credential claim credentialSubject.scope-name has no display configured",
                                claim.getDisplay());
                    }

                    assertEquals("The jwt_vc-credential should offer vct",
                            verifiableCredentialType,
                            jwtVcConfig.getVct());

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
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
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
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            // Create a credential request with a non-existent configuration ID
            // This will test the unknown_credential_configuration error
            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialConfigurationId("unknown-configuration-id");

            String requestPayload = JsonSerialization.writeValueAsString(credentialRequest);

            try {
                issuerEndpoint.requestCredential(requestPayload);
                Assert.fail("Expected BadRequestException due to unknown credential configuration");
            } catch (BadRequestException e) {
                ErrorResponse error = (ErrorResponse) e.getResponse().getEntity();
                assertEquals(ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION, error.getError());
            }
        });
    }

    /**
     * Test that unknown_credential_configuration error is returned when a credential configuration exists
     * but there is no credential builder registered for its format.
     */
    @Test
    public void testRequestCredentialWhenNoCredentialBuilderForFormat() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());

        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            // Prepare endpoint with no credential builders to simulate missing builder for the configured format
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator, Map.of());

            // Use the known configuration id for the JWT VC test scope
            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialConfigurationId(jwtTypeCredentialConfigurationIdName);

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
        String token = getBearerToken(oauth, client, jwtTypeCredentialClientScope.getName());
        final String credentialConfigurationId = jwtTypeCredentialClientScope.getAttributes()
                .get(CredentialScopeModel.CONFIGURATION_ID);

        testingClient.server(TEST_REALM_NAME).run(session -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            // Test 1: Create a request with single proof field - should be converted to proofs array
            CredentialRequest requestWithProof = new CredentialRequest()
                    .setCredentialConfigurationId(credentialConfigurationId);

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
                    .setCredentialConfigurationId(credentialConfigurationId);

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
}
