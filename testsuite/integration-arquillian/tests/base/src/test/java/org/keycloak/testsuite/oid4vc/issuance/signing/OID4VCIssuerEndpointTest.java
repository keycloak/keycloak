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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.VerificationException;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProviderFactory;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.issuance.signing.JwtSigningService;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.OfferUriType;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedGrant;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.runonserver.RunOnServerException;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OID4VCIssuerEndpointTest extends OID4VCTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TimeProvider TIME_PROVIDER = new OID4VCTest.StaticTimeProvider(1000);
    private CloseableHttpClient httpClient;


    @Before
    public void setup() {
        CryptoIntegration.init(this.getClass().getClassLoader());
        httpClient = HttpClientBuilder.create().build();
    }


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
                    oid4VCIssuerEndpoint.getCredentialOfferURI("test-credential", OfferUriType.URI, 0, 0);
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
                        CredentialOfferURI credentialOfferURI = new ObjectMapper().convertValue(response.getEntity(), CredentialOfferURI.class);
                        assertNotNull("A nonce should be included.", credentialOfferURI.getNonce());
                        assertNotNull("The issuer uri should be provided.", credentialOfferURI.getIssuer());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

    }

    private static String getBearerToken(OAuthClient oAuthClient) {
        OAuthClient.AuthorizationEndpointResponse authorizationEndpointResponse = oAuthClient.doLogin("john", "password");
        return oAuthClient.doAccessTokenRequest(authorizationEndpointResponse.getCode(), "password").getAccessToken();
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
                        issuerEndpoint.getCredentialOffer("nonce");
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
                        String nonce = prepareNonce(authenticator, "invalidNote");
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        issuerEndpoint.getCredentialOffer(nonce);
                    }));
        });
    }

    @Test
    public void testGetCredentialOffer() {
        String token = getBearerToken(oauth);
        String rootURL = suiteContext.getAuthServerInfo().getContextRoot().toString();
        testingClient
                .server(TEST_REALM_NAME)
                .run((session) -> {
                    AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                    authenticator.setTokenString(token);

                    SupportedCredentialConfiguration supportedCredentialConfiguration = new SupportedCredentialConfiguration()
                            .setId("test-credential")
                            .setScope("VerifiableCredential")
                            .setFormat(Format.JWT_VC);
                    String nonce = prepareNonce(authenticator, OBJECT_MAPPER.writeValueAsString(supportedCredentialConfiguration));

                    OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                    Response credentialOfferResponse = issuerEndpoint.getCredentialOffer(nonce);
                    assertEquals("The offer should have been returned.", HttpStatus.SC_OK, credentialOfferResponse.getStatus());
                    Object credentialOfferEntity = credentialOfferResponse.getEntity();
                    assertNotNull("An actual offer should be in the response.", credentialOfferEntity);

                    CredentialsOffer credentialsOffer = OBJECT_MAPPER.convertValue(credentialOfferEntity, CredentialsOffer.class);
                    assertNotNull("Credentials should have been offered.", credentialsOffer.getCredentialConfigurationIds());
                    assertFalse("Credentials should have been offered.", credentialsOffer.getCredentialConfigurationIds().isEmpty());
                    List<String> supportedCredentials = credentialsOffer.getCredentialConfigurationIds();
                    assertEquals("Exactly one credential should have been returned.", 1, supportedCredentials.size());
                    String offeredCredentialId = supportedCredentials.get(0);
                    assertEquals("The credential should be as defined in the note.", supportedCredentialConfiguration.getId(), offeredCredentialId);

                    PreAuthorizedGrant grant = credentialsOffer.getGrants();
                    assertNotNull("The grant should be included.", grant);
                    assertNotNull("The grant should contain the pre-authorized code.", grant.getPreAuthorizedCode());
                    assertNotNull("The actual pre-authorized code should be included.", grant
                            .getPreAuthorizedCode()
                            .getPreAuthorizedCode());

                    assertEquals("The correct issuer should be included.", rootURL + "/auth/realms/" + TEST_REALM_NAME, credentialsOffer.getCredentialIssuer());
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
                        issuerEndpoint.requestCredential(new CredentialRequest()
                                .setFormat(Format.JWT_VC)
                                .setCredentialIdentifier("test-credential"));
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
        ObjectMapper objectMapper = new ObjectMapper();
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
                    CredentialResponse credentialResponseVO = OBJECT_MAPPER.convertValue(credentialResponse.getEntity(), CredentialResponse.class);
                    JsonWebToken jsonWebToken = TokenVerifier.create((String) credentialResponseVO.getCredential(), JsonWebToken.class).getToken();

                    assertNotNull("A valid credential string should have been responded", jsonWebToken);
                    assertNotNull("The credentials should be included at the vc-claim.", jsonWebToken.getOtherClaims().get("vc"));
                    VerifiableCredential credential = objectMapper.convertValue(jsonWebToken.getOtherClaims().get("vc"), VerifiableCredential.class);
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
        parameters.add(new BasicNameValuePair("pre-authorized_code", credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode()));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
        postPreAuthorizedCode.setEntity(formEntity);
        OAuthClient.AccessTokenResponse accessTokenResponse = new OAuthClient.AccessTokenResponse(httpClient.execute(postPreAuthorizedCode));
        assertEquals(HttpStatus.SC_OK, accessTokenResponse.getStatusCode());
        String theToken = accessTokenResponse.getAccessToken();

        // 6. Get the credential
        credentialsOffer.getCredentialConfigurationIds().stream()
                .map(offeredCredentialId -> credentialIssuer.getCredentialsSupported().get(offeredCredentialId))
                .forEach(supportedCredential -> {
                    try {
                        requestOffer(theToken, credentialIssuer.getCredentialEndpoint(), supportedCredential);
                    } catch (IOException e) {
                        fail("Was not able to get the credential.");
                    } catch (VerificationException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private ClientResource findClientByClientId(RealmResource realm, String clientId) {
        for (ClientRepresentation c : realm.clients().findAll()) {
            if (clientId.equals(c.getClientId())) {
                return realm.clients().get(c.getId());
            }
        }
        return null;
    }

    private String registerOptionalClientScope(String scopeName) {
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName(scopeName);
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response res = testRealm().clientScopes().create(clientScope);
        String scopeId = ApiUtil.getCreatedId(res);
        getCleanup().addClientScopeId(scopeId); // automatically removed when a test method is finished.
        res.close();
        return scopeId;
    }

    private void assignOptionalClientScopeToClient(String scopeId, String clientId) {
        ClientResource clientResource = findClientByClientId(testRealm(), clientId);
        clientResource.addOptionalClientScope(scopeId);
    }

    private void addCredentialConfigurationIdToClient(String clientId, String credentialConfigurationId, String format, String scope) {
        ClientRepresentation clientRepresentation = adminClient.realm(TEST_REALM_NAME).clients().findByClientId(clientId).get(0);
        ClientResource clientResource = adminClient.realm(TEST_REALM_NAME).clients().get(clientRepresentation.getId());

        clientRepresentation.setAttributes(Map.of(
                "vc." + credentialConfigurationId + ".format", format,
                "vc." + credentialConfigurationId + ".scope", scope));
        clientRepresentation.setProtocolMappers(
                List.of(
                        getRoleMapper(clientId),
                        getEmailMapper(),
                        getIdMapper(),
                        getStaticClaimMapper(scope),
                        getStaticClaimMapper("AnotherCredentialType")
                )
        );

        clientResource.update(clientRepresentation);
    }

    private void removeCredentialConfigurationIdToClient(String clientId) {
        ClientRepresentation clientRepresentation = adminClient.realm(TEST_REALM_NAME).clients().findByClientId(clientId).get(0);
        ClientResource clientResource = adminClient.realm(TEST_REALM_NAME).clients().get(clientRepresentation.getId());
        clientRepresentation.setAttributes(Map.of());
        clientResource.update(clientRepresentation);
    }

    private void logoutUser(String clientId, String username) {
        UserResource user = ApiUtil.findUserByUsernameId(adminClient.realm(TEST_REALM_NAME), username);
        user.logout();
    }

    private void testCredentialIssuanceWithAuthZCodeFlow(Consumer<Map<String, String>> c) throws Exception {
        // use pre-registered client for this test class whose clientId is "test-app" defined in testrealm.json
        String testClientId = "test-app";

        // use supported values by Credential Issuer Metadata
        String testCredentialConfigurationId = "test-credential";
        String testScope = "VerifiableCredential";
        String testFormat = Format.JWT_VC.toString();

        // register optional client scope
        String scopeId = registerOptionalClientScope(testScope);

        // assign registered optional client scope
        assignOptionalClientScopeToClient(scopeId, testClientId); // pre-registered client for this test class

        // add credential configuration id to a client as client attributes
        addCredentialConfigurationIdToClient(testClientId, testCredentialConfigurationId, testFormat, testScope);

        c.accept(Map.of(
                "clientId", testClientId,
                "credentialConfigurationId", testCredentialConfigurationId,
                "scope", testScope,
                "format", testFormat)
        );
        // clean-up
        logoutUser(testClientId, "john");
        removeCredentialConfigurationIdToClient(testClientId);
        oauth.clientId(null);
    }

    // Tests the AuthZCode complete flow without scope from
    // 1. Get authorization code without scope specified by wallet
    // 2. Using the code to get access token
    // 3. Get the credential configuration id from issuer metadata at .wellKnown
    // 4. With the access token, get the credential
    private void testCredentialIssuanceWithAuthZCodeFlow(BiFunction<String, String, String> f, Consumer<Map<String, Object>> c) throws Exception {
        testCredentialIssuanceWithAuthZCodeFlow(m->{
            String testClientId = m.get("clientId");
            String testScope = m.get("scope");
            String testFormat = m.get("format");
            String testCredentialConfigurationId = m.get("credentialConfigurationId");

            try (Client client = AdminClientUtil.createResteasyClient()) {
                UriBuilder builder = UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT);
                URI oid4vciDiscoveryUri = RealmsResource.wellKnownProviderUrl(builder).build(TEST_REALM_NAME, OID4VCIssuerWellKnownProviderFactory.PROVIDER_ID);
                WebTarget oid4vciDiscoveryTarget = client.target(oid4vciDiscoveryUri);

                // 1. Get authoriZation code without scope specified by wallet
                // 2. Using the code to get accesstoken
                String token = f.apply(testClientId, testScope);

                // 3. Get the credential configuration id from issuer metadata at .wellKnown
                try (Response discoveryResponse = oid4vciDiscoveryTarget.request().get()) {
                    CredentialIssuer oid4vciIssuerConfig = JsonSerialization.readValue(discoveryResponse.readEntity(String.class), CredentialIssuer.class);
                    assertEquals(200, discoveryResponse.getStatus());
                    assertEquals(getRealmPath(TEST_REALM_NAME), oid4vciIssuerConfig.getCredentialIssuer());
                    assertEquals(getBasePath(TEST_REALM_NAME) + "credential", oid4vciIssuerConfig.getCredentialEndpoint());

                    // 4. With the access token, get the credential
                    try (Client clientForCredentialRequest = AdminClientUtil.createResteasyClient()) {
                        UriBuilder credentialUriBuilder = UriBuilder.fromUri(oid4vciIssuerConfig.getCredentialEndpoint());
                        URI credentialUri = credentialUriBuilder.build();
                        WebTarget credentialTarget = clientForCredentialRequest.target(credentialUri);

                        CredentialRequest request = new CredentialRequest();
                        request.setFormat(oid4vciIssuerConfig.getCredentialsSupported().get(testCredentialConfigurationId).getFormat());
                        request.setCredentialIdentifier(oid4vciIssuerConfig.getCredentialsSupported().get(testCredentialConfigurationId).getId());

                        assertEquals(testFormat, oid4vciIssuerConfig.getCredentialsSupported().get(testCredentialConfigurationId).getFormat().toString());
                        assertEquals(testCredentialConfigurationId, oid4vciIssuerConfig.getCredentialsSupported().get(testCredentialConfigurationId).getId());

                        c.accept(Map.of(
                                "accessToken", token,
                                "credentialTarget", credentialTarget,
                                "credentialRequest", request
                                ));
                    }
                }
            } catch (IOException  e) {
                Assert.fail();
            }

        });
    }

    @Test
    public void testCredentialIssuanceWithAuthZCodeWithScopeMatched() throws Exception {
        testCredentialIssuanceWithAuthZCodeFlow((testClientId, testScope) -> getBearerToken(oauth.clientId(testClientId).openid(false).scope(testScope)),
                m -> {
                    String accessToken = (String)m.get("accessToken");
                    WebTarget credentialTarget = (WebTarget)m.get("credentialTarget");
                    CredentialRequest credentialRequest = (CredentialRequest)m.get("credentialRequest");

                    try (Response response = credentialTarget.request().header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken).post(Entity.json(credentialRequest))) {
                        CredentialResponse credentialResponse = JsonSerialization.readValue(response.readEntity(String.class),CredentialResponse.class);

                        assertEquals(200, response.getStatus());
                        JsonWebToken jsonWebToken = TokenVerifier.create((String) credentialResponse.getCredential(), JsonWebToken.class).getToken();
                        assertEquals("did:web:test.org", jsonWebToken.getIssuer());

                        VerifiableCredential credential = new ObjectMapper().convertValue(jsonWebToken.getOtherClaims().get("vc"), VerifiableCredential.class);
                        assertEquals(TEST_TYPES, credential.getType());
                        assertEquals(TEST_DID, credential.getIssuer());
                        assertEquals("john@email.cz", credential.getCredentialSubject().getClaims().get("email"));
                    } catch (IOException | VerificationException  e) {
                        Assert.fail();
                    }
                });
    }

    @Test
    public void testCredentialIssuanceWithAuthZCodeWithScopeUnmatched() throws Exception {
        testCredentialIssuanceWithAuthZCodeFlow((testClientId, testScope) -> getBearerToken(oauth.clientId(testClientId).openid(false).scope("email")), // set registered different scope
                m -> {
                    String accessToken = (String)m.get("accessToken");
                    WebTarget credentialTarget = (WebTarget)m.get("credentialTarget");
                    CredentialRequest credentialRequest = (CredentialRequest)m.get("credentialRequest");

                    try (Response response = credentialTarget.request().header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken).post(Entity.json(credentialRequest))) {
                        assertEquals(400, response.getStatus());
                    }
                });
    }

    @Test
    public void testCredentialIssuanceWithAuthZCodeSWithoutScope() throws Exception {
        testCredentialIssuanceWithAuthZCodeFlow((testClientId, testScope) -> getBearerToken(oauth.clientId(testClientId).openid(false).scope(null)), // no scope
                m -> {
                    String accessToken = (String)m.get("accessToken");
                    WebTarget credentialTarget = (WebTarget)m.get("credentialTarget");
                    CredentialRequest credentialRequest = (CredentialRequest)m.get("credentialRequest");

                    try (Response response = credentialTarget.request().header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken).post(Entity.json(credentialRequest))) {
                        assertEquals(400, response.getStatus());
                    }
                });
    }

    private static String prepareNonce(AppAuthManager.BearerTokenAuthenticator authenticator, String note) {
        String nonce = SecretGenerator.getInstance().randomString();
        AuthenticationManager.AuthResult authResult = authenticator.authenticate();
        UserSessionModel userSessionModel = authResult.getSession();
        userSessionModel.getAuthenticatedClientSessionByClient(authResult.getClient().getId()).setNote(nonce, note);
        return nonce;
    }

    private static OID4VCIssuerEndpoint prepareIssuerEndpoint(KeycloakSession session, AppAuthManager.BearerTokenAuthenticator authenticator) {
        JwtSigningService jwtSigningService = new JwtSigningService(
                session,
                getKeyFromSession(session).getKid(),
                Algorithm.RS256,
                "JWT",
                "did:web:issuer.org",
                TIME_PROVIDER);
        return new OID4VCIssuerEndpoint(
                session,
                "did:web:issuer.org",
                Map.of(Format.JWT_VC.name(), jwtSigningService),
                authenticator,
                new ObjectMapper(),
                TIME_PROVIDER,
                30,
                true);
    }

    private String getBasePath(String realm) {
        return getRealmPath(realm) + "/protocol/oid4vc/";
    }

    private String getRealmPath(String realm){
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/" + realm;
    }

    private void requestOffer(String token, String credentialEndpoint, SupportedCredentialConfiguration offeredCredential) throws IOException, VerificationException {
        CredentialRequest request = new CredentialRequest();
        request.setFormat(offeredCredential.getFormat());
        request.setCredentialIdentifier(offeredCredential.getId());

        StringEntity stringEntity = new StringEntity(OBJECT_MAPPER.writeValueAsString(request), ContentType.APPLICATION_JSON);

        HttpPost postCredential = new HttpPost(credentialEndpoint);
        postCredential.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        postCredential.setEntity(stringEntity);
        CloseableHttpResponse credentialRequestResponse = httpClient.execute(postCredential);
        assertEquals(HttpStatus.SC_OK, credentialRequestResponse.getStatusLine().getStatusCode());
        String s = IOUtils.toString(credentialRequestResponse.getEntity().getContent(), StandardCharsets.UTF_8);
        CredentialResponse credentialResponse = JsonSerialization.readValue(s, CredentialResponse.class);

        assertNotNull("The credential should have been responded.", credentialResponse.getCredential());
        JsonWebToken jsonWebToken = TokenVerifier.create((String) credentialResponse.getCredential(), JsonWebToken.class).getToken();
        assertEquals("did:web:test.org", jsonWebToken.getIssuer());
        VerifiableCredential credential = new ObjectMapper().convertValue(jsonWebToken.getOtherClaims().get("vc"), VerifiableCredential.class);
        assertEquals(List.of("VerifiableCredential"), credential.getType());
        assertEquals(URI.create("did:web:test.org"), credential.getIssuer());
        assertEquals("john@email.cz", credential.getCredentialSubject().getClaims().get("email"));
        assertTrue("The static claim should be set.", credential.getCredentialSubject().getClaims().containsKey("VerifiableCredential"));
        assertFalse("Only mappers supported for the requested type should have been evaluated.", credential.getCredentialSubject().getClaims().containsKey("AnotherCredentialType"));
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        if (testRealm.getComponents() != null) {
            testRealm.getComponents().add("org.keycloak.keys.KeyProvider", getRsaKeyProvider(RSA_KEY));
            testRealm.getComponents().add("org.keycloak.protocol.oid4vc.issuance.signing.VerifiableCredentialsSigningService", getJwtSigningProvider(RSA_KEY));
        } else {
            testRealm.setComponents(new MultivaluedHashMap<>(
                    Map.of("org.keycloak.keys.KeyProvider", List.of(getRsaKeyProvider(RSA_KEY)),
                            "org.keycloak.protocol.oid4vc.issuance.signing.VerifiableCredentialsSigningService", List.of(getJwtSigningProvider(RSA_KEY))
                    )));
        }
        ClientRepresentation clientRepresentation = getTestClient("did:web:test.org");
        if (testRealm.getClients() != null) {
            testRealm.getClients().add(clientRepresentation);
        } else {
            testRealm.setClients(List.of(clientRepresentation));
        }
        if (testRealm.getRoles() != null) {
            testRealm.getRoles().getClient()
                    .put(clientRepresentation.getClientId(), List.of(getRoleRepresentation("testRole", clientRepresentation.getClientId())));
        } else {
            testRealm.getRoles()
                    .setClient(Map.of(clientRepresentation.getClientId(), List.of(getRoleRepresentation("testRole", clientRepresentation.getClientId()))));
        }
        if (testRealm.getUsers() != null) {
            testRealm.getUsers().add(getUserRepresentation(Map.of(clientRepresentation.getClientId(), List.of("testRole"))));
        } else {
            testRealm.setUsers(List.of(getUserRepresentation(Map.of(clientRepresentation.getClientId(), List.of("testRole")))));
        }
        if (testRealm.getAttributes() != null) {
            testRealm.getAttributes().put("issuerDid", TEST_DID.toString());
        } else {
            testRealm.setAttributes(Map.of("issuerDid", TEST_DID.toString()));
        }
    }

    private void withCausePropagation(Runnable r) throws Throwable {
        try {
            r.run();
        } catch (Exception e) {
            if (e instanceof RunOnServerException) {
                throw e.getCause();
            }
            throw e;
        }
    }

}

