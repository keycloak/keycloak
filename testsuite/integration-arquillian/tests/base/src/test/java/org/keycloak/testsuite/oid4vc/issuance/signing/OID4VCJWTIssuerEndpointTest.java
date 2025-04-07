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

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.OfferUriType;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCode;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedGrant;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test from org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCIssuerEndpointTest
 */
public class OID4VCJWTIssuerEndpointTest extends OID4VCIssuerEndpointTest {

    // ----- getCredentialOfferUri

    @Test(expected = BadRequestException.class)
    public void testGetCredentialOfferUriUnsupportedCredential() throws Throwable {
        String token = getBearerToken(oauth);
        withCausePropagation(() -> testingClient.server(TEST_REALM_NAME)
                .run((session -> {
                    AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                    authenticator.setTokenString(token);

                    OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                    oid4VCIssuerEndpoint.getCredentialOfferURI("inexistent-id", OfferUriType.URI, 0, 0);
                })));

    }

    @Test(expected = BadRequestException.class)
    public void testGetCredentialOfferUriUnauthorized() throws Throwable {
        withCausePropagation(() -> testingClient.server(TEST_REALM_NAME)
                .run((session -> {
                    AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                    authenticator.setTokenString(null);
                    OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                    oid4VCIssuerEndpoint.getCredentialOfferURI("test-credential", OfferUriType.URI, 0, 0);
                })));
    }

    @Test(expected = BadRequestException.class)
    public void testGetCredentialOfferUriInvalidToken() throws Throwable {
        withCausePropagation(() -> testingClient.server(TEST_REALM_NAME)
                .run((session -> {
                    AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                    authenticator.setTokenString("invalid-token");
                    OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                    Response response = oid4VCIssuerEndpoint
                            .getCredentialOfferURI("test-credential", OfferUriType.URI, 0, 0);
                    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
                })));
    }

    @Test
    public void testGetCredentialOfferURI() {
        String token = getBearerToken(oauth);
        testingClient
                .server(TEST_REALM_NAME)
                .run((session) -> {
                    try {
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                        authenticator.setTokenString(token);
                        OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                        Response response = oid4VCIssuerEndpoint.getCredentialOfferURI("test-credential", OfferUriType.URI, 0, 0);

                        assertEquals("An offer uri should have been returned.", HttpStatus.SC_OK, response.getStatus());
                        CredentialOfferURI credentialOfferURI = JsonSerialization.mapper.convertValue(response.getEntity(), CredentialOfferURI.class);
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
                    // the cache transactions need to be commited explicitly in the test. Without that, the OAuth2Code will only be commited to
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

    @Test(expected = BadRequestException.class)
    public void testRequestCredentialUnauthorized() throws Throwable {
        withCausePropagation(() -> {
            testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                        authenticator.setTokenString(null);
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        Response response = issuerEndpoint.requestCredential(new CredentialRequest()
                                .setFormat(Format.JWT_VC)
                                .setCredentialIdentifier("test-credential"));
                        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
                    }));
        });
    }

    @Test(expected = BadRequestException.class)
    public void testRequestCredentialInvalidToken() throws Throwable {
        withCausePropagation(() -> {
            testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                        authenticator.setTokenString("token");
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        issuerEndpoint.requestCredential(new CredentialRequest()
                                .setFormat(Format.JWT_VC)
                                .setCredentialIdentifier("test-credential"));
                    }));
        });
    }

    @Test(expected = BadRequestException.class)
    public void testRequestCredentialUnsupportedFormat() throws Throwable {
        String token = getBearerToken(oauth);
        withCausePropagation(() -> {
            testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                        authenticator.setTokenString(token);
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        issuerEndpoint.requestCredential(new CredentialRequest()
                                .setFormat(Format.SD_JWT_VC)
                                .setCredentialIdentifier("test-credential"));
                    }));
        });
    }

    @Test(expected = BadRequestException.class)
    public void testRequestCredentialNoMatchingCredentialBuilder() throws Throwable {
        String token = getBearerToken(oauth);
        withCausePropagation(() ->
            testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                        authenticator.setTokenString(token);

                        // Prepare the issue endpoint with no credential builders.
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator, Map.of());

                        issuerEndpoint.requestCredential(new CredentialRequest()
                                .setFormat(Format.JWT_VC)
                                .setCredentialIdentifier("test-credential"));
                    }))
        );
    }

    @Test(expected = BadRequestException.class)
    public void testRequestCredentialUnsupportedCredential() throws Throwable {
        String token = getBearerToken(oauth);
        withCausePropagation(() -> {
            testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                        authenticator.setTokenString(token);
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        issuerEndpoint.requestCredential(new CredentialRequest()
                                .setFormat(Format.JWT_VC)
                                .setCredentialIdentifier("no-such-credential"));
                    }));
        });
    }

    @Test
    public void testRequestCredential() {
        String token = getBearerToken(oauth);
        testingClient
                .server(TEST_REALM_NAME)
                .run((session -> {
                    AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                    authenticator.setTokenString(token);
                    OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                    CredentialRequest credentialRequest = new CredentialRequest()
                            .setFormat(Format.JWT_VC)
                            .setCredentialIdentifier("test-credential");
                    Response credentialResponse = issuerEndpoint.requestCredential(credentialRequest);
                    assertEquals("The credential request should be answered successfully.", HttpStatus.SC_OK, credentialResponse.getStatus());
                    assertNotNull("A credential should be responded.", credentialResponse.getEntity());
                    CredentialResponse credentialResponseVO = JsonSerialization.mapper.convertValue(credentialResponse.getEntity(), CredentialResponse.class);
                    JsonWebToken jsonWebToken = TokenVerifier.create((String) credentialResponseVO.getCredential(), JsonWebToken.class).getToken();

                    assertNotNull("A valid credential string should have been responded", jsonWebToken);
                    assertNotNull("The credentials should be included at the vc-claim.", jsonWebToken.getOtherClaims().get("vc"));
                    VerifiableCredential credential = JsonSerialization.mapper.convertValue(jsonWebToken.getOtherClaims().get("vc"), VerifiableCredential.class);
                    assertTrue("The static claim should be set.", credential.getCredentialSubject().getClaims().containsKey("VerifiableCredential"));
                    assertFalse("Only mappers supported for the requested type should have been evaluated.", credential.getCredentialSubject().getClaims().containsKey("AnotherCredentialType"));
                }));
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

        String token = getBearerToken(oauth);

        // 1. Retrieving the credential-offer-uri
        HttpGet getCredentialOfferURI = new HttpGet(getBasePath(TEST_REALM_NAME) + "credential-offer-uri?credential_configuration_id=test-credential");
        getCredentialOfferURI.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        CloseableHttpResponse credentialOfferURIResponse = httpClient.execute(getCredentialOfferURI);

        assertEquals("A valid offer uri should be returned", HttpStatus.SC_OK, credentialOfferURIResponse.getStatusLine().getStatusCode());
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
                        requestOffer(theToken, credentialIssuer.getCredentialEndpoint(), supportedCredential, new CredentialResponseHandler());
                    } catch (IOException e) {
                        fail("Was not able to get the credential.");
                    } catch (VerificationException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    public void testCredentialIssuanceWithAuthZCodeWithScopeMatched() throws Exception {
        testCredentialIssuanceWithAuthZCodeFlow((testClientId, testScope) -> getBearerToken(oauth.clientId(testClientId).openid(false).scope(testScope)),
                m -> {
                    String accessToken = (String) m.get("accessToken");
                    WebTarget credentialTarget = (WebTarget) m.get("credentialTarget");
                    CredentialRequest credentialRequest = (CredentialRequest) m.get("credentialRequest");

                    try (Response response = credentialTarget.request().header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken).post(Entity.json(credentialRequest))) {
                        CredentialResponse credentialResponse = JsonSerialization.readValue(response.readEntity(String.class), CredentialResponse.class);

                        assertEquals(200, response.getStatus());
                        JsonWebToken jsonWebToken = TokenVerifier.create((String) credentialResponse.getCredential(), JsonWebToken.class).getToken();
                        assertEquals("did:web:test.org", jsonWebToken.getIssuer());

                        VerifiableCredential credential = JsonSerialization.mapper.convertValue(jsonWebToken.getOtherClaims().get("vc"), VerifiableCredential.class);
                        assertEquals(TEST_TYPES, credential.getType());
                        assertEquals(TEST_DID, credential.getIssuer());
                        assertEquals("john@email.cz", credential.getCredentialSubject().getClaims().get("email"));
                    } catch (IOException | VerificationException e) {
                        Assert.fail();
                    }
                });
    }

    @Test
    public void testCredentialIssuanceWithAuthZCodeWithScopeUnmatched() throws Exception {
        testCredentialIssuanceWithAuthZCodeFlow((testClientId, testScope) -> getBearerToken(oauth.clientId(testClientId).openid(false).scope("email")), // set registered different scope
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
        testCredentialIssuanceWithAuthZCodeFlow((testClientId, testScope) -> getBearerToken(oauth.clientId(testClientId).openid(false).scope(null)), // no scope
                m -> {
                    String accessToken = (String) m.get("accessToken");
                    WebTarget credentialTarget = (WebTarget) m.get("credentialTarget");
                    CredentialRequest credentialRequest = (CredentialRequest) m.get("credentialRequest");

                    try (Response response = credentialTarget.request().header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken).post(Entity.json(credentialRequest))) {
                        assertEquals(400, response.getStatus());
                    }
                });
    }

}
