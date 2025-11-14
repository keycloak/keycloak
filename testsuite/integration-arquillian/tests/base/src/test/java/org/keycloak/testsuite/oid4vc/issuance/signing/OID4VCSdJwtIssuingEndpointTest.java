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
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.constants.Oid4VciConstants;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
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
import org.keycloak.protocol.oid4vc.model.Format;
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
import org.testcontainers.shaded.org.apache.commons.lang3.exception.ExceptionUtils;

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
        String token = getBearerToken(oauth, client, sdJwtTypeCredentialClientScope.getName());

        final String clientScopeString = toJsonString(sdJwtTypeCredentialClientScope);

        testingClient
                .server(TEST_REALM_NAME)
                .run(session -> {
                    ClientScopeRepresentation clientScope = fromJsonString(clientScopeString,
                            ClientScopeRepresentation.class);
                    testRequestTestCredential(session, clientScope, token, null);
                });
    }

    @Test
    public void testRequestTestCredentialWithKeybinding() {
        String cNonce = getCNonce();
        String token = getBearerToken(oauth, client, sdJwtTypeCredentialClientScope.getName());

        final String clientScopeString = toJsonString(sdJwtTypeCredentialClientScope);

        testingClient
                .server(TEST_REALM_NAME)
                .run((session -> {
                    Proofs proof = new Proofs()
                            .setJwt(List.of(generateJwtProof(getCredentialIssuer(session), cNonce)));

                    ClientScopeRepresentation clientScope = fromJsonString(clientScopeString,
                            ClientScopeRepresentation.class);

                    SdJwtVP sdJwtVP = testRequestTestCredential(session, clientScope, token, proof);
                    assertNotNull("A cnf claim must be attached to the credential", sdJwtVP.getCnfClaim());
                }));
    }

    @Test
    public void testRequestTestCredentialWithInvalidKeybinding() throws Throwable {
        String cNonce = getCNonce();
        String token = getBearerToken(oauth, client, sdJwtTypeCredentialClientScope.getName());

        final String clientScopeString = toJsonString(sdJwtTypeCredentialClientScope);

        try {
            withCausePropagation(() -> {
                testingClient.server(TEST_REALM_NAME).run((session -> {
                    Proofs proof = new Proofs()
                            .setJwt(List.of(generateInvalidJwtProof(getCredentialIssuer(session), cNonce)));

                    ClientScopeRepresentation clientScope = fromJsonString(clientScopeString,
                            ClientScopeRepresentation.class);

                    testRequestTestCredential(session, clientScope, token, proof);
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
            String token = getBearerToken(oauth, client, sdJwtTypeCredentialClientScope.getName());
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
                        testRequestTestCredential(session, clientScope, token, proof);
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
            String token = getBearerToken(oauth, client, sdJwtTypeCredentialClientScope.getName());
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
                        testRequestTestCredential(session, clientScope, token, proof);
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
            String token = getBearerToken(oauth, client, sdJwtTypeCredentialClientScope.getName());
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
                            session.getContext().getRealm().setAttribute(Oid4VciConstants.C_NONCE_LIFETIME_IN_SECONDS, -1);
                            String cNonce = cNonceHandler.buildCNonce(List.of(credentialsEndpoint),
                                                                      Map.of(JwtCNonceHandler.SOURCE_ENDPOINT, nonceEndpoint));
                            Proofs proof = new Proofs().setJwt(List.of(generateJwtProof(getCredentialIssuer(session), cNonce)));

                            ClientScopeRepresentation clientScope = fromJsonString(clientScopeString,
                                    ClientScopeRepresentation.class);
                            testRequestTestCredential(session, clientScope, token, proof);
                        } finally {
                            // make sure other tests are not affected by the changed realm-attribute
                            session.getContext().getRealm().removeAttribute(Oid4VciConstants.C_NONCE_LIFETIME_IN_SECONDS);
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

                                                     String token, Proofs proof)
            throws VerificationException, IOException {

        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
        authenticator.setTokenString(token);
        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

        final String credentialConfigurationId = clientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);
        CredentialRequest credentialRequest = new CredentialRequest()
                .setCredentialConfigurationId(credentialConfigurationId)
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
        HttpGet getCredentialOfferURI = new HttpGet(getBasePath(TEST_REALM_NAME) +
                "credential-offer-uri?credential_configuration_id=" +
                credentialConfigurationId);
        getCredentialOfferURI.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        CloseableHttpResponse credentialOfferURIResponse = httpClient.execute(getCredentialOfferURI);

        assertEquals("A valid offer uri should be returned", HttpStatus.SC_OK, credentialOfferURIResponse.getStatusLine().getStatusCode());
        String s = IOUtils.toString(credentialOfferURIResponse.getEntity().getContent(), StandardCharsets.UTF_8);
        CredentialOfferURI credentialOfferURI = JsonSerialization.readValue(s, CredentialOfferURI.class);

        // 2. Using the uri to get the actual credential offer
        HttpGet getCredentialOffer = new HttpGet(credentialOfferURI.getIssuer() + "/" + credentialOfferURI.getNonce());
        getCredentialOffer.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
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

        final String vct = clientScope.getAttributes().get(CredentialScopeModel.VCT);

        // 6. Get the credential
        credentialsOffer.getCredentialConfigurationIds().stream()
                .map(offeredCredentialId -> credentialIssuer.getCredentialsSupported().get(offeredCredentialId))
                .forEach(supportedCredential -> {
                    try {
                        requestCredential(theToken,
                                credentialIssuer.getCredentialEndpoint(),
                                supportedCredential,
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
                            Format.SD_JWT_VC,
                            jwtVcConfig.getFormat());

                    assertNotNull("The sd-jwt-credential can optionally provide a claims claim.",
                                  credentialIssuer.getCredentialsSupported().get(credentialConfigurationId)
                                          .getCredentialMetadata() != null ?
                                          credentialIssuer.getCredentialsSupported().get(credentialConfigurationId)
                                                  .getCredentialMetadata().getClaims() : null);

                    Claims jwtVcClaims = jwtVcConfig.getCredentialMetadata() != null ? jwtVcConfig.getCredentialMetadata().getClaims() : null;
                    assertNotNull("The sd-jwt-credential can optionally provide a claims claim.",
                            jwtVcClaims);

                    assertEquals(4,  jwtVcClaims.size());
                    {
                        Claim claim = jwtVcClaims.get(0);
                        assertEquals("The sd-jwt-credential claim email is present.",
                                "email",
                                claim.getPath().get(0));
                        assertFalse("The sd-jwt-credential claim email is not mandatory.",
                                claim.isMandatory());
                        assertNull("The sd-jwt-credential claim email has no display configured",
                                claim.getDisplay());
                    }
                    {
                        Claim claim = jwtVcClaims.get(1);
                        assertEquals("The sd-jwt-credential claim firstName is present.",
                                "firstName",
                                claim.getPath().get(0));
                        assertFalse("The sd-jwt-credential claim firstName is not mandatory.",
                                claim.isMandatory());
                        assertNull("The sd-jwt-credential claim firstName has no display configured",
                                claim.getDisplay());
                    }
                    {
                        Claim claim = jwtVcClaims.get(2);
                        assertEquals("The sd-jwt-credential claim lastName is present.",
                                "lastName",
                                claim.getPath().get(0));
                        assertFalse("The sd-jwt-credential claim lastName is not mandatory.",
                                claim.isMandatory());
                        assertNull("The sd-jwt-credential claim lastName has no display configured",
                                claim.getDisplay());
                    }
                    {
                        Claim claim = jwtVcClaims.get(3);
                        assertEquals("The sd-jwt-credential claim scope-name is present.",
                                "scope-name",
                                claim.getPath().get(0));
                        assertFalse("The sd-jwt-credential claim scope-name is not mandatory.",
                                claim.isMandatory());
                        assertNull("The sd-jwt-credential claim scope-name has no display configured",
                                claim.getDisplay());
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
        JwtCredentialBuilder testJwtCredentialBuilder = new JwtCredentialBuilder(new StaticTimeProvider(5));
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
        protocolMapperRepresentation.setProtocol(Oid4VciConstants.OID4VC_PROTOCOL);
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
        credentialScope.setProtocol(Oid4VciConstants.OID4VC_PROTOCOL);
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
            JsonWebToken jsonWebToken = TokenVerifier.create(sdJwtVP.getIssuerSignedJWT().toJws(), JsonWebToken.class).getToken();

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
