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

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.models.KeycloakSession;
import org.keycloak.oid4vci.Oid4VciConstants;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.SdJwtCredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.keybinding.CNonceHandler;
import org.keycloak.protocol.oid4vc.issuance.keybinding.JwtCNonceHandler;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCGeneratedIdMapper;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.JwtProof;
import org.keycloak.protocol.oid4vc.model.Proof;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
        String token = getBearerToken(oauth);
        testingClient
                .server(TEST_REALM_NAME)
                .run(session -> testRequestTestCredential(session, token, null));
    }

    @Test
    public void testRequestTestCredentialWithKeybinding() {
        String cNonce = getCNonce();
        String token = getBearerToken(oauth);
        testingClient.server(TEST_REALM_NAME)
                     .run((session -> {
                         JwtProof proof = new JwtProof()
                                .setJwt(generateJwtProof(getCredentialIssuer(session), cNonce));

                         SdJwtVP sdJwtVP = testRequestTestCredential(session, token, proof);
                         assertNotNull("A cnf claim must be attached to the credential", sdJwtVP.getCnfClaim());
                     }));
    }

    @Test
    public void testRequestTestCredentialWithInvalidKeybinding() throws Throwable {
        try {
            String cNonce = getCNonce();
            String token = getBearerToken(oauth);
            withCausePropagation(() -> testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        JwtProof proof = new JwtProof()
                                                 .setJwt(generateInvalidJwtProof(getCredentialIssuer(session), cNonce));

                       testRequestTestCredential(session, token, proof);
                    })));
            Assert.fail("Should have thrown an exception");
        } catch (BadRequestException ex) {
            Assert.assertEquals("Could not validate provided proof", ex.getMessage());
            Assert.assertEquals("Could not verify signature of provided proof", ex.getCause().getMessage());
        }
    }

    @Test
    public void testProofOfPossessionWithMissingAudience() throws Throwable {
        try {
            String token = getBearerToken(oauth);
            withCausePropagation(() -> testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        CNonceHandler cNonceHandler = session.getProvider(CNonceHandler.class);
                        final String nonceEndpoint = OID4VCIssuerWellKnownProvider.getNonceEndpoint(session.getContext());
                        // creates a cNonce with missing data
                        String cNonce = cNonceHandler.buildCNonce(null,
                                                                  Map.of(JwtCNonceHandler.SOURCE_ENDPOINT,
                                                                         nonceEndpoint));
                        Proof proof = new JwtProof().setJwt(generateJwtProof(getCredentialIssuer(session), cNonce));

                       testRequestTestCredential(session, token, proof);
                    })));
            Assert.fail("Should have thrown an exception");
        } catch (BadRequestException ex) {
            Assert.assertEquals("""
                                        c_nonce: expected 'aud' to be equal to \
                                        '[https://localhost:8543/auth/realms/test/protocol/oid4vc/credential]' but \
                                        actual value was '[]'""",
                                ExceptionUtils.getRootCause(ex).getMessage());
        }
    }

    @Test
    public void testProofOfPossessionWithIllegalSourceEndpoint() throws Throwable {
        try {
            String token = getBearerToken(oauth);
            withCausePropagation(() -> testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        CNonceHandler cNonceHandler = session.getProvider(CNonceHandler.class);
                        final String credentialsEndpoint = //
                                OID4VCIssuerWellKnownProvider.getCredentialsEndpoint(session.getContext());
                        // creates a cNonce with missing data
                        String cNonce = cNonceHandler.buildCNonce(List.of(credentialsEndpoint), null);
                        Proof proof = new JwtProof().setJwt(generateJwtProof(getCredentialIssuer(session), cNonce));

                       testRequestTestCredential(session, token, proof);
                    })));
            Assert.fail("Should have thrown an exception");
        } catch (BadRequestException ex) {
            Assert.assertEquals("""
                                        c_nonce: expected 'source_endpoint' to be equal to \
                                        'https://localhost:8543/auth/realms/test/protocol/oid4vc/nonce' but \
                                        actual value was 'null'""",
                                ExceptionUtils.getRootCause(ex).getMessage());
        }
    }

    @Test
    public void testProofOfPossessionWithExpiredState() throws Throwable {
        try {
            String token = getBearerToken(oauth);
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
                            Proof proof = new JwtProof().setJwt(generateJwtProof(getCredentialIssuer(session), cNonce));

                           testRequestTestCredential(session, token, proof);
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
        }
    }

    private static String getCredentialIssuer(KeycloakSession session) {
        return OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());
    }

    private static SdJwtVP testRequestTestCredential(KeycloakSession session, String token, Proof proof)
            throws VerificationException {
        String vct = "https://credentials.example.com/test-credential";

        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
        authenticator.setTokenString(token);
        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

        CredentialRequest credentialRequest = new CredentialRequest()
                .setFormat(Format.SD_JWT_VC)
                .setVct(vct)
                .setProof(proof);

        Response credentialResponse = issuerEndpoint.requestCredential(credentialRequest);
        assertEquals("The credential request should be answered successfully.", HttpStatus.SC_OK, credentialResponse.getStatus());
        assertNotNull("A credential should be responded.", credentialResponse.getEntity());
        CredentialResponse credentialResponseVO = JsonSerialization.mapper.convertValue(credentialResponse.getEntity(), CredentialResponse.class);
        new TestCredentialResponseHandler(vct).handleCredentialResponse(credentialResponseVO);

        return SdJwtVP.of(credentialResponseVO.getCredential().toString());
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

        final String vct = "https://credentials.example.com/test-credential";

        // 6. Get the credential
        credentialsOffer.getCredentialConfigurationIds().stream()
                .map(offeredCredentialId -> credentialIssuer.getCredentialsSupported().get(offeredCredentialId))
                .forEach(supportedCredential -> {
                    try {
                        requestOffer(theToken, credentialIssuer.getCredentialEndpoint(), supportedCredential, new TestCredentialResponseHandler(vct));
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
    public void getConfig() {
        String expectedIssuer = suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/" + TEST_REALM_NAME;
        String expectedCredentialsEndpoint = expectedIssuer + "/protocol/oid4vc/credential";
        String expectedNonceEndpoint = expectedIssuer + "/protocol/oid4vc/" + OID4VCIssuerEndpoint.NONCE_PATH;
        final String expectedAuthorizationServer = expectedIssuer;
        testingClient
                .server(TEST_REALM_NAME)
                .run((session -> {
                    OID4VCIssuerWellKnownProvider oid4VCIssuerWellKnownProvider = new OID4VCIssuerWellKnownProvider(session);
                    Object issuerConfig = oid4VCIssuerWellKnownProvider.getConfig();
                    assertTrue("Valid credential-issuer metadata should be returned.", issuerConfig instanceof CredentialIssuer);
                    CredentialIssuer credentialIssuer = (CredentialIssuer) issuerConfig;
                    assertEquals("The correct issuer should be included.", expectedIssuer, credentialIssuer.getCredentialIssuer());
                    assertEquals("The correct credentials endpoint should be included.", expectedCredentialsEndpoint, credentialIssuer.getCredentialEndpoint());
                    assertEquals("The correct nonce endpoint should be included.",
                                 expectedNonceEndpoint,
                                 credentialIssuer.getNonceEndpoint());
                    assertEquals("Since the authorization server is equal to the issuer, just 1 should be returned.", 1, credentialIssuer.getAuthorizationServers().size());
                    assertEquals("The expected server should have been returned.", expectedAuthorizationServer, credentialIssuer.getAuthorizationServers().get(0));
                    assertTrue("The test-credential should be supported.", credentialIssuer.getCredentialsSupported().containsKey("test-credential"));
                    assertEquals("The test-credential should offer type test-credential", "test-credential", credentialIssuer.getCredentialsSupported().get("test-credential").getScope());
                    assertEquals("The test-credential should be offered in the sd-jwt format.", Format.SD_JWT_VC, credentialIssuer.getCredentialsSupported().get("test-credential").getFormat());
                    assertNotNull("The test-credential can optionally provide a claims claim.", credentialIssuer.getCredentialsSupported().get("test-credential").getClaims());
                    assertNotNull("The test-credential claim firstName is present.", credentialIssuer.getCredentialsSupported().get("test-credential").getClaims().get("firstName"));
                    assertFalse("The test-credential claim firstName is not mandatory.", credentialIssuer.getCredentialsSupported().get("test-credential").getClaims().get("firstName").getMandatory());
                    assertEquals("The test-credential claim firstName shall be displayed as First Name", "First Name", credentialIssuer.getCredentialsSupported().get("test-credential").getClaims().get("firstName").getDisplay().get(0).getName());
                    assertEquals("The test-credential should offer vct VerifiableCredential", "https://credentials.example.com/test-credential", credentialIssuer.getCredentialsSupported().get("test-credential").getVct());

                    // We are offering key binding only for identity credential
                    assertTrue("The IdentityCredential should contain a cryptographic binding method supported named jwk", credentialIssuer.getCredentialsSupported().get("IdentityCredential").getCryptographicBindingMethodsSupported().contains("jwk"));
                    assertTrue("The IdentityCredential should contain a credential signing algorithm named ES256", credentialIssuer.getCredentialsSupported().get("IdentityCredential").getCredentialSigningAlgValuesSupported().contains("ES256"));
                    assertEquals("The IdentityCredential should display as Test Credential", "Identity Credential", credentialIssuer.getCredentialsSupported().get("IdentityCredential").getDisplay().get(0).getName());
                    assertTrue("The IdentityCredential should support a proof of type jwt with signing algorithm ES256", credentialIssuer.getCredentialsSupported().get("IdentityCredential").getProofTypesSupported().getJwt().getProofSigningAlgValuesSupported().contains("ES256"));
                }));
    }

    protected static OID4VCIssuerEndpoint prepareIssuerEndpoint(KeycloakSession session, AppAuthManager.BearerTokenAuthenticator authenticator) {
        String issuerDid = "did:web:issuer.org";
        SdJwtCredentialBuilder testSdJwtCredentialBuilder = new SdJwtCredentialBuilder(issuerDid);

        return new OID4VCIssuerEndpoint(
                session,
                Map.of(
                        testSdJwtCredentialBuilder.getSupportedFormat(), testSdJwtCredentialBuilder
                ),
                authenticator,
                TIME_PROVIDER,
                30,
                true);
    }

    private static final String JTI_KEY = "jti";

    public static ProtocolMapperRepresentation getJtiGeneratedIdMapper(String supportedCredentialTypes) {
        ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
        protocolMapperRepresentation.setName("generated-id-mapper");
        protocolMapperRepresentation.setProtocol("oid4vc");
        protocolMapperRepresentation.setId(UUID.randomUUID().toString());
        protocolMapperRepresentation.setProtocolMapper("oid4vc-generated-id-mapper");
        protocolMapperRepresentation.setConfig(Map.of(
                OID4VCGeneratedIdMapper.SUBJECT_PROPERTY_CONFIG_KEY, JTI_KEY,
                "supportedCredentialTypes", supportedCredentialTypes
        ));
        return protocolMapperRepresentation;
    }

    @Override
    protected ComponentExportRepresentation getKeyProvider() {
        return getEcKeyProvider();
    }

    @Override
    protected List<ComponentExportRepresentation> getCredentialBuilderProviders() {
        return List.of(getCredentialBuilderProvider(Format.SD_JWT_VC));
    }

    @Override
    protected Map<String, String> getCredentialDefinitionAttributes() {
        Map<String, String> testCredentialAttributes = Map.ofEntries(
                Map.entry("vc.test-credential.expiry_in_s", "1800"),
                Map.entry("vc.test-credential.format", Format.SD_JWT_VC),
                Map.entry("vc.test-credential.scope", "test-credential"),
                Map.entry("vc.test-credential.claims", "{ \"firstName\": {\"mandatory\": false, \"display\": [{\"name\": \"First Name\", \"locale\": \"en-US\"}, {\"name\": \"名前\", \"locale\": \"ja-JP\"}]}, \"lastName\": {\"mandatory\": false}, \"email\": {\"mandatory\": false} }"),
                Map.entry("vc.test-credential.vct", "https://credentials.example.com/test-credential"),
                Map.entry("vc.test-credential.credential_signing_alg_values_supported", "ES256,ES384"),
                Map.entry("vc.test-credential.display.0", "{\n  \"name\": \"Test Credential\"\n}"),
                Map.entry("vc.test-credential.cryptographic_binding_methods_supported", "jwk"),
                Map.entry("vc.test-credential.proof_types_supported", "{\"jwt\":{\"proof_signing_alg_values_supported\":[\"ES256\"]}}"),
                Map.entry("vc.test-credential.credential_build_config.token_jws_type", "example+sd-jwt"),
                Map.entry("vc.test-credential.credential_build_config.hash_algorithm", "sha-256"),
                Map.entry("vc.test-credential.credential_build_config.visible_claims", "iat,nbf,jti"),
                Map.entry("vc.test-credential.credential_build_config.decoys", "2"),
                Map.entry("vc.test-credential.credential_build_config.signing_algorithm", "ES256")
        );

        Map<String, String> identityCredentialAttributes = Map.ofEntries(
                Map.entry("vc.IdentityCredential.expiry_in_s", "31536000"),
                Map.entry("vc.IdentityCredential.format", Format.SD_JWT_VC),
                Map.entry("vc.IdentityCredential.scope", "identity_credential"),
                Map.entry("vc.IdentityCredential.vct", "https://credentials.example.com/identity_credential"),
                Map.entry("vc.IdentityCredential.cryptographic_binding_methods_supported", "jwk"),
                Map.entry("vc.IdentityCredential.credential_signing_alg_values_supported", "ES256,ES384"),
                Map.entry("vc.IdentityCredential.claims", "{\"given_name\":{\"display\":[{\"name\":\"الاسم الشخصي\",\"locale\":\"ar\"},{\"name\":\"Vorname\",\"locale\":\"de\"},{\"name\":\"Given Name\",\"locale\":\"en\"},{\"name\":\"Nombre\",\"locale\":\"es\"},{\"name\":\"نام\",\"locale\":\"fa\"},{\"name\":\"Etunimi\",\"locale\":\"fi\"},{\"name\":\"Prénom\",\"locale\":\"fr\"},{\"name\":\"पहचानी गई नाम\",\"locale\":\"hi\"},{\"name\":\"Nome\",\"locale\":\"it\"},{\"name\":\"名\",\"locale\":\"ja\"},{\"name\":\"Овог нэр\",\"locale\":\"mn\"},{\"name\":\"Voornaam\",\"locale\":\"nl\"},{\"name\":\"Nome Próprio\",\"locale\":\"pt\"},{\"name\":\"Förnamn\",\"locale\":\"sv\"},{\"name\":\"مسلمان نام\",\"locale\":\"ur\"}]},\"family_name\":{\"display\":[{\"name\":\"اسم العائلة\",\"locale\":\"ar\"},{\"name\":\"Nachname\",\"locale\":\"de\"},{\"name\":\"Family Name\",\"locale\":\"en\"},{\"name\":\"Apellido\",\"locale\":\"es\"},{\"name\":\"نام خانوادگی\",\"locale\":\"fa\"},{\"name\":\"Sukunimi\",\"locale\":\"fi\"},{\"name\":\"Nom de famille\",\"locale\":\"fr\"},{\"name\":\"परिवार का नाम\",\"locale\":\"hi\"},{\"name\":\"Cognome\",\"locale\":\"it\"},{\"name\":\"姓\",\"locale\":\"ja\"},{\"name\":\"өөрийн нэр\",\"locale\":\"mn\"},{\"name\":\"Achternaam\",\"locale\":\"nl\"},{\"name\":\"Sobrenome\",\"locale\":\"pt\"},{\"name\":\"Efternamn\",\"locale\":\"sv\"},{\"name\":\"خاندانی نام\",\"locale\":\"ur\"}]},\"birthdate\":{\"display\":[{\"name\":\"تاريخ الميلاد\",\"locale\":\"ar\"},{\"name\":\"Geburtsdatum\",\"locale\":\"de\"},{\"name\":\"Date of Birth\",\"locale\":\"en\"},{\"name\":\"Fecha de Nacimiento\",\"locale\":\"es\"},{\"name\":\"تاریخ تولد\",\"locale\":\"fa\"},{\"name\":\"Syntymäaika\",\"locale\":\"fi\"},{\"name\":\"Date de naissance\",\"locale\":\"fr\"},{\"name\":\"जन्म की तारीख\",\"locale\":\"hi\"},{\"name\":\"Data di nascita\",\"locale\":\"it\"},{\"name\":\"生年月日\",\"locale\":\"ja\"},{\"name\":\"төрсөн өдөр\",\"locale\":\"mn\"},{\"name\":\"Geboortedatum\",\"locale\":\"nl\"},{\"name\":\"Data de Nascimento\",\"locale\":\"pt\"},{\"name\":\"Födelsedatum\",\"locale\":\"sv\"},{\"name\":\"تاریخ پیدائش\",\"locale\":\"ur\"}]}}"),
                Map.entry("vc.IdentityCredential.display.0", "{\"name\": \"Identity Credential\"}"),
                Map.entry("vc.IdentityCredential.proof_types_supported", "{\"jwt\":{\"proof_signing_alg_values_supported\":[\"ES256\"]}}"),
                Map.entry("vc.IdentityCredential.credential_build_config.token_jws_type", "example+sd-jwt"),
                Map.entry("vc.IdentityCredential.credential_build_config.hash_algorithm", "sha-256"),
                Map.entry("vc.IdentityCredential.credential_build_config.visible_claims", "iat,nbf,jti"),
                Map.entry("vc.IdentityCredential.credential_build_config.decoys", "0"),
                Map.entry("vc.IdentityCredential.credential_build_config.signing_algorithm", "ES256")
        );

        HashedMap<String, String> allAttributes = new HashedMap<>();
        allAttributes.putAll(testCredentialAttributes);
        allAttributes.putAll(identityCredentialAttributes);

        return allAttributes;
    }

    static class TestCredentialResponseHandler extends CredentialResponseHandler {
        final String vct;

        TestCredentialResponseHandler(String vct) {
            this.vct = vct;
        }

        @Override
        protected void handleCredentialResponse(CredentialResponse credentialResponse) throws VerificationException {
            // SDJWT have a special format.
            SdJwtVP sdJwtVP = SdJwtVP.of(credentialResponse.getCredential().toString());
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
            assertTrue("The credentials should include the roles claim.", disclosureMap.containsKey("roles"));
            assertTrue("The credentials should include the test-credential claim.", disclosureMap.containsKey("test-credential"));
            assertTrue("lastName claim incorrectly mapped.", disclosureMap.get("test-credential").get(2).asBoolean());
            assertTrue("The credentials should include the email claim.", disclosureMap.containsKey("email"));
            assertEquals("email claim incorrectly mapped.", "john@email.cz", disclosureMap.get("email").get(2).asText());

            assertNotNull("Test credential shall include an iat claim.", jsonWebToken.getIat());
            assertNotNull("Test credential shall include an nbf claim.", jsonWebToken.getNbf());
        }
    }
}
