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
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections4.map.HashedMap;
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
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.signing.SdJwtSigningService;
import org.keycloak.protocol.oid4vc.model.CredentialConfigId;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.VerifiableCredentialType;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        String vct = "https://credentials.example.com/test-credential";
        testingClient
                .server(TEST_REALM_NAME)
                .run((session -> {
                    AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                    authenticator.setTokenString(token);
                    OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                    CredentialRequest credentialRequest = new CredentialRequest()
                            .setFormat(Format.SD_JWT_VC)
                            .setVct(vct);
                    Response credentialResponse = issuerEndpoint.requestCredential(credentialRequest);
                    assertEquals("The credential request should be answered successfully.", HttpStatus.SC_OK, credentialResponse.getStatus());
                    assertNotNull("A credential should be responded.", credentialResponse.getEntity());
                    CredentialResponse credentialResponseVO = JsonSerialization.mapper.convertValue(credentialResponse.getEntity(), CredentialResponse.class);
                    new TestCredentialResponseHandler(vct).handleCredentialResponse(credentialResponseVO);
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
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
        postPreAuthorizedCode.setEntity(formEntity);
        OAuthClient.AccessTokenResponse accessTokenResponse = new OAuthClient.AccessTokenResponse(httpClient.execute(postPreAuthorizedCode));
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
        String expectedAuthorizationServer = expectedIssuer;
        testingClient
                .server(TEST_REALM_NAME)
                .run((session -> {
                    OID4VCIssuerWellKnownProvider oid4VCIssuerWellKnownProvider = new OID4VCIssuerWellKnownProvider(session);
                    Object issuerConfig = oid4VCIssuerWellKnownProvider.getConfig();
                    assertTrue("Valid credential-issuer metadata should be returned.", issuerConfig instanceof CredentialIssuer);
                    CredentialIssuer credentialIssuer = (CredentialIssuer) issuerConfig;
                    assertEquals("The correct issuer should be included.", expectedIssuer, credentialIssuer.getCredentialIssuer());
                    assertEquals("The correct credentials endpoint should be included.", expectedCredentialsEndpoint, credentialIssuer.getCredentialEndpoint());
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
                    assertTrue("The IdentityCredential should contain a credential signing algorithm named ES384", credentialIssuer.getCredentialsSupported().get("IdentityCredential").getCredentialSigningAlgValuesSupported().contains("ES384"));
                    assertEquals("The IdentityCredential should display as Test Credential", "Identity Credential", credentialIssuer.getCredentialsSupported().get("IdentityCredential").getDisplay().get(0).getName());
                    assertTrue("The IdentityCredential should support a proof of type jwt with signing algorithm ES256", credentialIssuer.getCredentialsSupported().get("IdentityCredential").getProofTypesSupported().getJwt().getProofSigningAlgValuesSupported().contains("ES256"));
                }));
    }


    protected static OID4VCIssuerEndpoint prepareIssuerEndpoint(KeycloakSession session, AppAuthManager.BearerTokenAuthenticator authenticator) {
        SdJwtSigningService testCredentialSigningService = new SdJwtSigningService(
                session,
                JsonSerialization.mapper,
                getKeyFromSession(session).getKid(),
                Algorithm.ES256,
                Format.SD_JWT_VC,
                "sha-256",
                "did:web:issuer.org",
                2,
                List.of("iat", "nbf"),
                Optional.empty(),
                VerifiableCredentialType.from("https://credentials.example.com/test-credential"),
                CredentialConfigId.from("test-credential"));

        SdJwtSigningService identityCredentialSigningService = new SdJwtSigningService(
                session,
                JsonSerialization.mapper,
                getKeyFromSession(session).getKid(),
                Algorithm.ES256,
                Format.SD_JWT_VC,
                "sha-256",
                "did:web:issuer.org",
                0,
                List.of("given_name", "iat", "nbf"),
                Optional.empty(),
                VerifiableCredentialType.from("https://credentials.example.com/identity_credential"),
                CredentialConfigId.from("IdentityCredential"));

        return new OID4VCIssuerEndpoint(
                session,
                "did:web:issuer.org",
                Map.of(
                        testCredentialSigningService.locator(), testCredentialSigningService,
                        identityCredentialSigningService.locator(), identityCredentialSigningService
                        ),
                authenticator,
                new ObjectMapper(),
                TIME_PROVIDER,
                30,
                true);
    }

    private ComponentExportRepresentation getIdCredentialSigningProvider() {
        ComponentExportRepresentation componentExportRepresentation = new ComponentExportRepresentation();
        componentExportRepresentation.setName("sd-jwt-signing_identity_credential");
        componentExportRepresentation.setId(UUID.randomUUID().toString());
        componentExportRepresentation.setProviderId(Format.SD_JWT_VC);

        componentExportRepresentation.setConfig(new MultivaluedHashMap<>(
                Map.of(
                        "algorithmType", List.of("ES256"),
                        "tokenType", List.of(Format.SD_JWT_VC),
                        "issuerDid", List.of(TEST_DID.toString()),
                        "hashAlgorithm", List.of("sha-256"),
                        "decoys", List.of("0"),
                        "vct", List.of("https://credentials.example.com/identity_credential"),
                        "visibleClaims", List.of("iat,nbf"),
                        "vcConfigId", List.of("IdentityCredential")
                )
        ));
        return componentExportRepresentation;
    }

    private ComponentExportRepresentation getTestCredentialSigningProvider() {
        ComponentExportRepresentation componentExportRepresentation = new ComponentExportRepresentation();
        componentExportRepresentation.setName("sd-jwt-signing_test-credential");
        componentExportRepresentation.setId(UUID.randomUUID().toString());
        componentExportRepresentation.setProviderId(Format.SD_JWT_VC);

        componentExportRepresentation.setConfig(new MultivaluedHashMap<>(
                Map.of(
                        "algorithmType", List.of("ES256"),
                        "tokenType", List.of(Format.SD_JWT_VC),
                        "issuerDid", List.of(TEST_DID.toString()),
                        "hashAlgorithm", List.of("sha-256"),
                        "decoys", List.of("2"),
                        "vct", List.of("https://credentials.example.com/test-credential"),
                        "visibleClaims", List.of("iat,nbf"),
                        "vcConfigId", List.of("test-credential")
                )
        ));
        return componentExportRepresentation;
    }

    @Override
    protected ClientRepresentation getTestClient(String clientId) {
        ClientRepresentation clientRepresentation = new ClientRepresentation();
        clientRepresentation.setClientId(clientId);
        clientRepresentation.setProtocol(OID4VCLoginProtocolFactory.PROTOCOL_ID);
        clientRepresentation.setEnabled(true);
        Map<String, String> testCredentialAttributes = Map.of(
                "vc.test-credential.expiry_in_s", "1800",
                "vc.test-credential.format", Format.SD_JWT_VC,
                "vc.test-credential.scope", "test-credential",
                "vc.test-credential.claims", "{ \"firstName\": {\"mandatory\": false, \"display\": [{\"name\": \"First Name\", \"locale\": \"en-US\"}, {\"name\": \"名前\", \"locale\": \"ja-JP\"}]}, \"lastName\": {\"mandatory\": false}, \"email\": {\"mandatory\": false} }",
                "vc.test-credential.vct", "https://credentials.example.com/test-credential",
                "vc.test-credential.credential_signing_alg_values_supported", "ES256,ES384",
                "vc.test-credential.display.0", "{\n  \"name\": \"Test Credential\"\n}"
        );
        Map<String, String> identityCredentialAttributes = Map.of(
                "vc.IdentityCredential.expiry_in_s", "31536000",
                "vc.IdentityCredential.format", Format.SD_JWT_VC,
                "vc.IdentityCredential.scope", "identity_credential",
                "vc.IdentityCredential.vct", "https://credentials.example.com/identity_credential",
                "vc.IdentityCredential.cryptographic_binding_methods_supported", "jwk",
                "vc.IdentityCredential.credential_signing_alg_values_supported", "ES256,ES384",
                "vc.IdentityCredential.claims", "{\"given_name\":{\"display\":[{\"name\":\"الاسم الشخصي\",\"locale\":\"ar\"},{\"name\":\"Vorname\",\"locale\":\"de\"},{\"name\":\"Given Name\",\"locale\":\"en\"},{\"name\":\"Nombre\",\"locale\":\"es\"},{\"name\":\"نام\",\"locale\":\"fa\"},{\"name\":\"Etunimi\",\"locale\":\"fi\"},{\"name\":\"Prénom\",\"locale\":\"fr\"},{\"name\":\"पहचानी गई नाम\",\"locale\":\"hi\"},{\"name\":\"Nome\",\"locale\":\"it\"},{\"name\":\"名\",\"locale\":\"ja\"},{\"name\":\"Овог нэр\",\"locale\":\"mn\"},{\"name\":\"Voornaam\",\"locale\":\"nl\"},{\"name\":\"Nome Próprio\",\"locale\":\"pt\"},{\"name\":\"Förnamn\",\"locale\":\"sv\"},{\"name\":\"مسلمان نام\",\"locale\":\"ur\"}]},\"family_name\":{\"display\":[{\"name\":\"اسم العائلة\",\"locale\":\"ar\"},{\"name\":\"Nachname\",\"locale\":\"de\"},{\"name\":\"Family Name\",\"locale\":\"en\"},{\"name\":\"Apellido\",\"locale\":\"es\"},{\"name\":\"نام خانوادگی\",\"locale\":\"fa\"},{\"name\":\"Sukunimi\",\"locale\":\"fi\"},{\"name\":\"Nom de famille\",\"locale\":\"fr\"},{\"name\":\"परिवार का नाम\",\"locale\":\"hi\"},{\"name\":\"Cognome\",\"locale\":\"it\"},{\"name\":\"姓\",\"locale\":\"ja\"},{\"name\":\"өөрийн нэр\",\"locale\":\"mn\"},{\"name\":\"Achternaam\",\"locale\":\"nl\"},{\"name\":\"Sobrenome\",\"locale\":\"pt\"},{\"name\":\"Efternamn\",\"locale\":\"sv\"},{\"name\":\"خاندانی نام\",\"locale\":\"ur\"}]},\"birthdate\":{\"display\":[{\"name\":\"تاريخ الميلاد\",\"locale\":\"ar\"},{\"name\":\"Geburtsdatum\",\"locale\":\"de\"},{\"name\":\"Date of Birth\",\"locale\":\"en\"},{\"name\":\"Fecha de Nacimiento\",\"locale\":\"es\"},{\"name\":\"تاریخ تولد\",\"locale\":\"fa\"},{\"name\":\"Syntymäaika\",\"locale\":\"fi\"},{\"name\":\"Date de naissance\",\"locale\":\"fr\"},{\"name\":\"जन्म की तारीख\",\"locale\":\"hi\"},{\"name\":\"Data di nascita\",\"locale\":\"it\"},{\"name\":\"生年月日\",\"locale\":\"ja\"},{\"name\":\"төрсөн өдөр\",\"locale\":\"mn\"},{\"name\":\"Geboortedatum\",\"locale\":\"nl\"},{\"name\":\"Data de Nascimento\",\"locale\":\"pt\"},{\"name\":\"Födelsedatum\",\"locale\":\"sv\"},{\"name\":\"تاریخ پیدائش\",\"locale\":\"ur\"}]}}",
                "vc.IdentityCredential.display.0", "{\"name\": \"Identity Credential\"}",
                "vc.IdentityCredential.proof_types_supported", "{\"jwt\":{\"proof_signing_alg_values_supported\":[\"ES256\"]}}"
        );
        HashedMap<String, String> allAttributes = new HashedMap<>();
        allAttributes.putAll(testCredentialAttributes);
        allAttributes.putAll(identityCredentialAttributes);
        clientRepresentation.setAttributes(allAttributes);
        clientRepresentation.setProtocolMappers(
                List.of(
                        getRoleMapper(clientId, "test-credential"),
                        getUserAttributeMapper("email", "email", "test-credential"),
                        getUserAttributeMapper("firstName", "firstName", "test-credential"),
                        getUserAttributeMapper("lastName", "lastName", "test-credential"),
                        getIdMapper("test-credential"),
                        getStaticClaimMapper("test-credential", "test-credential"),
                        getIssuedAtTimeMapper(null, ChronoUnit.HOURS.name(), "COMPUTE","test-credential"),
                        getIssuedAtTimeMapper("nbf", null, "COMPUTE","test-credential"),

                        getUserAttributeMapper("given_name", "firstName", "identity_credential"),
                        getUserAttributeMapper("family_name", "lastName", "identity_credential"),
                        getIssuedAtTimeMapper(null, ChronoUnit.MINUTES.name(), "COMPUTE","identity_credential"),
                        getIssuedAtTimeMapper("nbf", ChronoUnit.SECONDS.name(), "COMPUTE","identity_credential")
                )
        );
        return clientRepresentation;
    }

    protected ComponentExportRepresentation getKeyProvider(){
        return getEcKeyProvider();
    }

    @Override
    protected List<ComponentExportRepresentation> getSigningProviders() {
        return List.of(getIdCredentialSigningProvider(), getTestCredentialSigningProvider());
    }

    static class TestCredentialResponseHandler extends CredentialResponseHandler {
        final String vct;
        TestCredentialResponseHandler(String vct){
            this.vct = vct;
        }
        @Override
        protected void handleCredentialResponse(CredentialResponse credentialResponse) throws VerificationException {
            // SDJWT have a special format.
            SdJwtVP sdJwtVP = SdJwtVP.of(credentialResponse.getCredential().toString());
            JsonWebToken jsonWebToken = TokenVerifier.create(sdJwtVP.getIssuerSignedJWT().toJws(), JsonWebToken.class).getToken();

            assertNotNull("A valid credential string should have been responded", jsonWebToken);
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
            assertEquals("firstName claim incorrectly mapped.", disclosureMap.get("firstName").get(2).asText(), "John");
            assertTrue("The credentials should include the lastName claim.", disclosureMap.containsKey("lastName"));
            assertEquals("lastName claim incorrectly mapped.", disclosureMap.get("lastName").get(2).asText(), "Doe");
            assertTrue("The credentials should include the roles claim.", disclosureMap.containsKey("roles"));
            assertTrue("The credentials should include the id claim", disclosureMap.containsKey("id"));
            assertTrue("The credentials should include the test-credential claim.", disclosureMap.containsKey("test-credential"));
            assertTrue("lastName claim incorrectly mapped.", disclosureMap.get("test-credential").get(2).asBoolean());
            assertTrue("The credentials should include the email claim.", disclosureMap.containsKey("email"));
            assertEquals("email claim incorrectly mapped.", disclosureMap.get("email").get(2).asText(), "john@email.cz");

            assertNotNull("Test credential shall include an iat claim.", jsonWebToken.getIat());
            assertNotNull("Test credential shall include an nbf claim.", jsonWebToken.getNbf());        }
    }
}

