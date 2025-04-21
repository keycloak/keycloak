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

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.VerificationException;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProviderFactory;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.JwtCredentialBuilder;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.runonserver.RunOnServerException;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint.CREDENTIAL_OFFER_URI_CODE_SCOPE;
import static org.keycloak.testsuite.forms.PassThroughClientAuthenticator.clientId;

/**
 * Moved test to subclass. so we can reuse initialization code.
 */
public abstract class OID4VCIssuerEndpointTest extends OID4VCTest {

    protected static final TimeProvider TIME_PROVIDER = new OID4VCTest.StaticTimeProvider(1000);
    protected CloseableHttpClient httpClient;
    public static String verifiableCredentialScopeName = "VerifiableCredential";
    public static String testCredentialScopeName = "test-credential";


    @Before
    public void setup() {
        CryptoIntegration.init(this.getClass().getClassLoader());
        httpClient = HttpClientBuilder.create().build();
        ClientRepresentation client = testRealm().clients().findByClientId(clientId).get(0);

        // Register the optional client scopes
        String verifiableCredentialScopeId = registerOptionalClientScope(verifiableCredentialScopeName, client.getClientId());
        String testCredentialScopeId = registerOptionalClientScope(testCredentialScopeName, client.getClientId());

        // Assign the registered optional client scopes to the client
        assignOptionalClientScopeToClient(verifiableCredentialScopeId, client.getClientId());
        assignOptionalClientScopeToClient(testCredentialScopeId, client.getClientId());
    }


    protected String getBearerToken(OAuthClient oAuthClient) {
        AuthorizationEndpointResponse authorizationEndpointResponse = oAuthClient.doLogin("john", "password");
        return oAuthClient.doAccessTokenRequest(authorizationEndpointResponse.getCode()).getAccessToken();
    }

    private ClientResource findClientByClientId(RealmResource realm, String clientId) {
        for (ClientRepresentation c : realm.clients().findAll()) {
            if (clientId.equals(c.getClientId())) {
                return realm.clients().get(c.getId());
            }
        }
        return null;
    }

    private String registerOptionalClientScope(String scopeName, String clientId) {
        // Check if the client scope already exists
        List<ClientScopeRepresentation> existingScopes = testRealm().clientScopes().findAll();
        for (ClientScopeRepresentation existingScope : existingScopes) {
            if (existingScope.getName().equals(scopeName)) {
                return existingScope.getId(); // Reuse existing scope
            }
        }

        // Create a new ClientScope if not found
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName(scopeName);
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        Response res = testRealm().clientScopes().create(clientScope);
        String scopeId = ApiUtil.getCreatedId(res);
        getCleanup().addClientScopeId(scopeId); // Automatically removed when a test method is finished.
        res.close();

        // Add protocol mappers to the ClientScope
        addProtocolMappersToClientScope(scopeId, scopeName, clientId);

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
        String testClientId = clientId;

        // use supported values by Credential Issuer Metadata
        String testCredentialConfigurationId = "test-credential";
        String testScope = "VerifiableCredential";
        String testFormat = Format.JWT_VC;

        // register optional client scope
        String scopeId = registerOptionalClientScope(testScope, testClientId);

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
    protected void testCredentialIssuanceWithAuthZCodeFlow(BiFunction<String, String, String> f, Consumer<Map<String, Object>> c) throws Exception {
        testCredentialIssuanceWithAuthZCodeFlow(m -> {
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
            } catch (IOException e) {
                Assert.fail();
            }

        });
    }

    protected static String prepareSessionCode(KeycloakSession session, AppAuthManager.BearerTokenAuthenticator authenticator, String note) {
        AuthenticationManager.AuthResult authResult = authenticator.authenticate();
        UserSessionModel userSessionModel = authResult.getSession();
        AuthenticatedClientSessionModel authenticatedClientSessionModel = userSessionModel.getAuthenticatedClientSessionByClient(authResult.getClient().getId());
        String codeId = SecretGenerator.getInstance().randomString();
        String nonce = SecretGenerator.getInstance().randomString();
        OAuth2Code oAuth2Code = new OAuth2Code(codeId, Time.currentTime() + 6000, nonce, CREDENTIAL_OFFER_URI_CODE_SCOPE, null, null, null, null,
                authenticatedClientSessionModel.getUserSession().getId());

        String oauthCode = OAuth2CodeParser.persistCode(session, authenticatedClientSessionModel, oAuth2Code);

        authenticatedClientSessionModel.setNote(oauthCode, note);
        return oauthCode;
    }

    protected static OID4VCIssuerEndpoint prepareIssuerEndpoint(KeycloakSession session, AppAuthManager.BearerTokenAuthenticator authenticator) {
        JwtCredentialBuilder jwtCredentialBuilder = new JwtCredentialBuilder(
            TEST_DID.toString(), 
            new StaticTimeProvider(1000));

        return prepareIssuerEndpoint(
                session,
                authenticator,
                Map.of(jwtCredentialBuilder.getSupportedFormat(), jwtCredentialBuilder)
        );
    }

    protected static OID4VCIssuerEndpoint prepareIssuerEndpoint(
            KeycloakSession session,
            AppAuthManager.BearerTokenAuthenticator authenticator,
            Map<String, CredentialBuilder> credentialBuilders
    ) {
        return new OID4VCIssuerEndpoint(
                session,
                credentialBuilders,
                authenticator,
                TIME_PROVIDER,
                30,
                true);
    }

    protected String getBasePath(String realm) {
        return getRealmPath(realm) + "/protocol/oid4vc/";
    }

    private String getRealmPath(String realm) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/" + realm;
    }

    protected void requestOffer(String token, String credentialEndpoint, SupportedCredentialConfiguration offeredCredential, CredentialResponseHandler responseHandler) throws IOException, VerificationException {
        CredentialRequest request = new CredentialRequest();
        request.setFormat(offeredCredential.getFormat());
        request.setCredentialIdentifier(offeredCredential.getId());

        StringEntity stringEntity = new StringEntity(JsonSerialization.writeValueAsString(request), ContentType.APPLICATION_JSON);

        HttpPost postCredential = new HttpPost(credentialEndpoint);
        postCredential.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        postCredential.setEntity(stringEntity);
        CloseableHttpResponse credentialRequestResponse = httpClient.execute(postCredential);
        assertEquals(HttpStatus.SC_OK, credentialRequestResponse.getStatusLine().getStatusCode());
        String s = IOUtils.toString(credentialRequestResponse.getEntity().getContent(), StandardCharsets.UTF_8);
        CredentialResponse credentialResponse = JsonSerialization.readValue(s, CredentialResponse.class);

        // Use response handler to customize checks based on formats.
        responseHandler.handleCredentialResponse(credentialResponse);
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        if (testRealm.getComponents() == null) {
            testRealm.setComponents(new MultivaluedHashMap<>());
        }

        testRealm.getComponents().add("org.keycloak.keys.KeyProvider", getKeyProvider());
        testRealm.getComponents().addAll("org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBuilder", getCredentialBuilderProviders());

        // Find existing client representation
        ClientRepresentation existingClient = testRealm.getClients().stream()
                .filter(client -> client.getClientId().equals(clientId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Client with ID " + clientId + " not found in realm"));

        // Add role to existing client
        if (testRealm.getRoles() != null) {
            Map<String, List<RoleRepresentation>> clientRoles = testRealm.getRoles().getClient();
            clientRoles.merge(
                    existingClient.getClientId(),
                    List.of(getRoleRepresentation("testRole", existingClient.getClientId())),
                    (existingRoles, newRoles) -> {
                        List<RoleRepresentation> mergedRoles = new ArrayList<>(existingRoles);
                        mergedRoles.addAll(newRoles);
                        return mergedRoles;
                    }
            );
        } else {
            testRealm.getRoles()
                    .setClient(Map.of(existingClient.getClientId(), List.of(getRoleRepresentation("testRole", existingClient.getClientId()))));
        }
        if (testRealm.getUsers() != null) {
            testRealm.getUsers().add(getUserRepresentation(Map.of(existingClient.getClientId(), List.of("testRole"))));
        } else {
            testRealm.setUsers(List.of(getUserRepresentation(Map.of(existingClient.getClientId(), List.of("testRole")))));
        }

        if (testRealm.getAttributes() == null) {
            testRealm.setAttributes(new HashMap<>());
        }

        testRealm.getAttributes().put("issuerDid", TEST_DID.toString());
        testRealm.getAttributes().putAll(getCredentialDefinitionAttributes());
    }

    protected void withCausePropagation(Runnable r) throws Throwable {
        try {
            r.run();
        } catch (Exception e) {
            if (e instanceof RunOnServerException) {
                throw e.getCause();
            }
            throw e;
        }
    }

    protected ComponentExportRepresentation getKeyProvider() {
        return getRsaKeyProvider(RSA_KEY);
    }

    protected List<ComponentExportRepresentation> getCredentialBuilderProviders() {
        return List.of(getCredentialBuilderProvider(Format.JWT_VC));
    }

    protected Map<String, String> getCredentialDefinitionAttributes() {
        return getTestCredentialDefinitionAttributes();
    }

    protected static class CredentialResponseHandler {
        protected void handleCredentialResponse(CredentialResponse credentialResponse) throws VerificationException {
            assertNotNull("The credential should have been responded.", credentialResponse.getCredential());
            JsonWebToken jsonWebToken = TokenVerifier.create((String) credentialResponse.getCredential(), JsonWebToken.class).getToken();
            assertEquals("did:web:test.org", jsonWebToken.getIssuer());
            VerifiableCredential credential = JsonSerialization.mapper.convertValue(jsonWebToken.getOtherClaims().get("vc"), VerifiableCredential.class);
            assertEquals(List.of("VerifiableCredential"), credential.getType());
            assertEquals(URI.create("did:web:test.org"), credential.getIssuer());
            assertEquals("john@email.cz", credential.getCredentialSubject().getClaims().get("email"));
            assertTrue("The static claim should be set.", credential.getCredentialSubject().getClaims().containsKey("VerifiableCredential"));
            assertFalse("Only mappers supported for the requested type should have been evaluated.", credential.getCredentialSubject().getClaims().containsKey("AnotherCredentialType"));
        }
    }
}
