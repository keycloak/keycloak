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
package org.keycloak.tests.oidc;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.representations.MTLSEndpointAliases;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.clientregistration.ClientRegistrationService;
import org.keycloak.services.clientregistration.oidc.OIDCClientRegistrationProviderFactory;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.common.BasicRealmWithUserConfig;
import org.keycloak.tests.utils.Assert;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;

import static org.keycloak.utils.MediaType.APPLICATION_JWKS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.MethodName.class)
public abstract class AbstractWellKnownProviderTest {

    @InjectRealm(config = BasicRealmWithUserConfig.class)
    ManagedRealm managedRealm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectKeycloakUrls
    KeycloakUrls urls;

    @InjectSimpleHttp
    SimpleHttp http;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    protected abstract String getWellKnownProviderId();

    @Test
    public void testDiscovery() {
        try (Client client = ClientBuilder.newClient()) {
            OIDCConfigurationRepresentation oidcConfig = getOIDCDiscoveryRepresentation(client, urls.getBase());

            // URIs are filled
            assertEquals(oidcConfig.getAuthorizationEndpoint(), OIDCLoginProtocolService.authUrl(UriBuilder.fromUri(urls.getBase())).build(managedRealm.getName()).toString());
            assertEquals(oidcConfig.getTokenEndpoint(), oauth.getEndpoints().getToken());
            assertEquals(oidcConfig.getUserinfoEndpoint(), OIDCLoginProtocolService.userInfoUrl(UriBuilder.fromUri(urls.getBase())).build(managedRealm.getName()).toString());
            assertEquals(oidcConfig.getJwksUri(), oauth.getEndpoints().getJwks());

            String registrationUri = UriBuilder
                    .fromUri(urls.getBase())
                    .path(RealmsResource.class)
                    .path(RealmsResource.class, "getClientsService")
                    .path(ClientRegistrationService.class, "provider")
                    .build(managedRealm.getName(), OIDCClientRegistrationProviderFactory.ID)
                    .toString();
            assertEquals(oidcConfig.getRegistrationEndpoint(), registrationUri);

            // Support standard + implicit + hybrid flow
            assertContains(oidcConfig.getResponseTypesSupported(), OAuth2Constants.CODE, OIDCResponseType.ID_TOKEN, "id_token token", "code id_token", "code token", "code id_token token");
            assertEquals(10, oidcConfig.getGrantTypesSupported().size());
            assertContains(oidcConfig.getGrantTypesSupported(), OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.CLIENT_CREDENTIALS, OAuth2Constants.IMPLICIT,
                    OAuth2Constants.PASSWORD, OAuth2Constants.REFRESH_TOKEN, OAuth2Constants.DEVICE_CODE_GRANT_TYPE,
                    OAuth2Constants.JWT_AUTHORIZATION_GRANT, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE, OAuth2Constants.UMA_GRANT_TYPE, OAuth2Constants.CIBA_GRANT_TYPE);
            assertContains(oidcConfig.getResponseModesSupported(), "query", "fragment", "form_post", "jwt", "query.jwt", "fragment.jwt", "form_post.jwt");

            Assert.assertNames(oidcConfig.getSubjectTypesSupported(), "pairwise", "public");

            // Signature algorithms
            Assert.assertNames(oidcConfig.getIdTokenSigningAlgValuesSupported(), Algorithm.PS256, Algorithm.PS384, Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256, Algorithm.ES384, Algorithm.ES512, Algorithm.HS256, Algorithm.HS384, Algorithm.HS512, Algorithm.EdDSA);
            Assert.assertNames(oidcConfig.getUserInfoSigningAlgValuesSupported(), "none", Algorithm.PS256, Algorithm.PS384, Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256, Algorithm.ES384, Algorithm.ES512, Algorithm.HS256, Algorithm.HS384, Algorithm.HS512, Algorithm.EdDSA);
            Assert.assertNames(oidcConfig.getRequestObjectSigningAlgValuesSupported(), "none", Algorithm.PS256, Algorithm.PS384, Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256, Algorithm.ES384, Algorithm.ES512, Algorithm.HS256, Algorithm.HS384, Algorithm.HS512, Algorithm.EdDSA);
            Assert.assertNames(oidcConfig.getAuthorizationSigningAlgValuesSupported(), Algorithm.PS256, Algorithm.PS384, Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256, Algorithm.ES384, Algorithm.ES512, Algorithm.HS256, Algorithm.HS384, Algorithm.HS512, Algorithm.EdDSA);

            // request object encryption algorithms
            Assert.assertNames(oidcConfig.getRequestObjectEncryptionAlgValuesSupported(), JWEConstants.ECDH_ES, JWEConstants.ECDH_ES_A128KW, JWEConstants.ECDH_ES_A192KW, JWEConstants.ECDH_ES_A256KW, JWEConstants.RSA_OAEP, JWEConstants.RSA_OAEP_256, JWEConstants.RSA1_5);
            Assert.assertNames(oidcConfig.getRequestObjectEncryptionEncValuesSupported(), JWEConstants.A256GCM, JWEConstants.A192GCM, JWEConstants.A128GCM, JWEConstants.A128CBC_HS256, JWEConstants.A192CBC_HS384, JWEConstants.A256CBC_HS512);

            // Encryption algorithms
            Assert.assertNames(oidcConfig.getIdTokenEncryptionAlgValuesSupported(), JWEConstants.ECDH_ES, JWEConstants.ECDH_ES_A128KW, JWEConstants.ECDH_ES_A192KW, JWEConstants.ECDH_ES_A256KW, JWEConstants.RSA1_5, JWEConstants.RSA_OAEP, JWEConstants.RSA_OAEP_256);
            Assert.assertNames(oidcConfig.getIdTokenEncryptionEncValuesSupported(), JWEConstants.A128CBC_HS256, JWEConstants.A128GCM, JWEConstants.A192CBC_HS384, JWEConstants.A192GCM, JWEConstants.A256CBC_HS512, JWEConstants.A256GCM);
            Assert.assertNames(oidcConfig.getAuthorizationEncryptionAlgValuesSupported(), JWEConstants.ECDH_ES, JWEConstants.ECDH_ES_A128KW, JWEConstants.ECDH_ES_A192KW, JWEConstants.ECDH_ES_A256KW, JWEConstants.RSA1_5, JWEConstants.RSA_OAEP, JWEConstants.RSA_OAEP_256);
            Assert.assertNames(oidcConfig.getAuthorizationEncryptionEncValuesSupported(), JWEConstants.A128CBC_HS256, JWEConstants.A128GCM, JWEConstants.A192CBC_HS384, JWEConstants.A192GCM, JWEConstants.A256CBC_HS512, JWEConstants.A256GCM);
            Assert.assertNames(oidcConfig.getUserInfoEncryptionAlgValuesSupported(), JWEConstants.ECDH_ES, JWEConstants.ECDH_ES_A128KW, JWEConstants.ECDH_ES_A192KW, JWEConstants.ECDH_ES_A256KW, JWEConstants.RSA1_5, JWEConstants.RSA_OAEP, JWEConstants.RSA_OAEP_256);
            Assert.assertNames(oidcConfig.getUserInfoEncryptionEncValuesSupported(), JWEConstants.A128CBC_HS256, JWEConstants.A128GCM, JWEConstants.A192CBC_HS384, JWEConstants.A192GCM, JWEConstants.A256CBC_HS512, JWEConstants.A256GCM);

            // Client authentication
            Assert.assertNames(oidcConfig.getTokenEndpointAuthMethodsSupported(), "client_secret_basic", "client_secret_post", "private_key_jwt", "client_secret_jwt", "tls_client_auth");
            Assert.assertNames(oidcConfig.getTokenEndpointAuthSigningAlgValuesSupported(), Algorithm.PS256, Algorithm.PS384, Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256, Algorithm.ES384, Algorithm.ES512, Algorithm.HS256, Algorithm.HS384, Algorithm.HS512, Algorithm.EdDSA);
            Assert.assertNames(oidcConfig.getIntrospectionEndpointAuthSigningAlgValuesSupported(), Algorithm.PS256,
                    Algorithm.PS384, Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256,
                    Algorithm.ES384, Algorithm.ES512, Algorithm.HS256, Algorithm.HS384, Algorithm.HS512, Algorithm.EdDSA);

            // Claims
            assertContains(oidcConfig.getClaimsSupported(), "iss", IDToken.SUBJECT, IDToken.AUD, "exp", "iat", IDToken.AUTH_TIME, IDToken.NAME, IDToken.GIVEN_NAME, IDToken.FAMILY_NAME, IDToken.PREFERRED_USERNAME, IDToken.EMAIL, IDToken.ACR, IDToken.AZP, "nonce");
            Assert.assertNames(oidcConfig.getClaimTypesSupported(), "normal");
            Assertions.assertTrue(oidcConfig.getClaimsParameterSupported());

            // Scopes supported
            assertScopesSupportedMatchesWithRealm(oidcConfig);

            // Request and Request_Uri
            Assertions.assertTrue(oidcConfig.getRequestParameterSupported());
            Assertions.assertTrue(oidcConfig.getRequestUriParameterSupported());
            Assertions.assertTrue(oidcConfig.getRequireRequestUriRegistration());

            // KEYCLOAK-7451 OAuth Authorization Server Metadata for Proof Key for Code Exchange
            // PKCE support
            Assert.assertNames(oidcConfig.getCodeChallengeMethodsSupported(), OAuth2Constants.PKCE_METHOD_PLAIN, OAuth2Constants.PKCE_METHOD_S256);

            // KEYCLOAK-6771 Certificate Bound Token
            // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-6.2
            Assertions.assertTrue(oidcConfig.getTlsClientCertificateBoundAccessTokens());
            MTLSEndpointAliases mtlsEndpointAliases = oidcConfig.getMtlsEndpointAliases();
            Assertions.assertEquals(oidcConfig.getTokenEndpoint(), mtlsEndpointAliases.getTokenEndpoint());
            Assertions.assertEquals(oidcConfig.getRevocationEndpoint(), mtlsEndpointAliases.getRevocationEndpoint());

            // CIBA
            assertEquals(oidcConfig.getBackchannelAuthenticationEndpoint(), oauth.getEndpoints().getBackchannelAuthentication());
            assertContains(oidcConfig.getGrantTypesSupported(), OAuth2Constants.CIBA_GRANT_TYPE);
            Assert.assertNames(oidcConfig.getBackchannelTokenDeliveryModesSupported(), "poll", "ping");
            Assert.assertNames(oidcConfig.getBackchannelAuthenticationRequestSigningAlgValuesSupported(), Algorithm.PS256, Algorithm.PS384, Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256, Algorithm.ES384, Algorithm.ES512, Algorithm.EdDSA);

            Assertions.assertTrue(oidcConfig.getBackchannelLogoutSupported());
            Assertions.assertTrue(oidcConfig.getBackchannelLogoutSessionSupported());

            // Token Revocation
            assertEquals(oidcConfig.getRevocationEndpoint(), oauth.getEndpoints().getRevocation());
            Assert.assertNames(oidcConfig.getRevocationEndpointAuthMethodsSupported(), "client_secret_basic",
                    "client_secret_post", "private_key_jwt", "client_secret_jwt", "tls_client_auth");
            Assert.assertNames(oidcConfig.getRevocationEndpointAuthSigningAlgValuesSupported(), Algorithm.PS256,
                    Algorithm.PS384, Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256,
                    Algorithm.ES384, Algorithm.ES512, Algorithm.HS256, Algorithm.HS384, Algorithm.HS512, Algorithm.EdDSA);

            assertEquals(oidcConfig.getDeviceAuthorizationEndpoint(), oauth.getEndpoints().getDeviceAuthorization());

            // Pushed Authorization Request (PAR)
            assertEquals(oauth.getEndpoints().getPushedAuthorizationRequest(), oidcConfig.getPushedAuthorizationRequestEndpoint());
            assertEquals(Boolean.FALSE, oidcConfig.getRequirePushedAuthorizationRequests());

            // frontchannel logout
            assertTrue(oidcConfig.getFrontChannelLogoutSessionSupported());
            assertTrue(oidcConfig.getFrontChannelLogoutSupported());
        }
    }

    @Test
    public void testHttpDiscovery() {
        try (Client client = ClientBuilder.newClient()) {
            OIDCConfigurationRepresentation oidcConfig = getOIDCDiscoveryRepresentation(client, UriBuilder.fromUri(urls.getBase()).scheme("http").build().toString());

            Assertions.assertNotNull(oidcConfig.getJwksUri());

            // Token Revocation
            Assertions.assertNotNull(oidcConfig.getRevocationEndpoint());
            Assertions.assertNotNull(oidcConfig.getRevocationEndpointAuthMethodsSupported());
            Assertions.assertNotNull(oidcConfig.getRevocationEndpointAuthSigningAlgValuesSupported());
        }
    }

    @Test
    public void testIssuerMatches() throws Exception {
        AuthorizationEndpointResponse authzResp = oauth.doLogin(BasicRealmWithUserConfig.USERNAME, BasicRealmWithUserConfig.PASSWORD);
        AccessTokenResponse response = oauth.doAccessTokenRequest(authzResp.getCode());
        assertEquals(200, response.getStatusCode());
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        try (Client client = ClientBuilder.newClient()) {
            OIDCConfigurationRepresentation oidcConfig = getOIDCDiscoveryRepresentation(client, urls.getBase());

            // assert issuer matches
            assertEquals(idToken.getIssuer(), oidcConfig.getIssuer());
        }
    }

    @Test
    public void corsTest() {
        URI uri = getOIDCDiscoveryUri(UriBuilder.fromUri(urls.getBase()));
        try (Client client = ClientBuilder.newClient()) {
            for (String origin : List.of("http://somehost", "null")) {
                try (Response response = client.target(uri).request().header(Cors.ORIGIN_HEADER, origin).get()) {
                    assertEquals(origin, response.getHeaders().getFirst(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
                }
            }
        }
    }

    @Test
    public void certs() throws IOException {
        registerEcKeyProvider();

        OIDCConfigurationRepresentation representation = http.doGet(managedRealm.getBaseUrl() + "/.well-known/openid-configuration").asJson(OIDCConfigurationRepresentation.class);
        String jwksUri = representation.getJwksUri();

        JSONWebKeySet jsonWebKeySet = http.doGet(jwksUri).asJson(JSONWebKeySet.class);
        assertEquals(3, jsonWebKeySet.getKeys().length);
    }

    @Test
    public void certsWithJwks() throws IOException {
        registerEcKeyProvider();

        OIDCConfigurationRepresentation representation = http.doGet(managedRealm.getBaseUrl() + "/.well-known/openid-configuration").asJson(OIDCConfigurationRepresentation.class);
        String jwksUri = representation.getJwksUri();

        try (SimpleHttpResponse response = http.doGet(jwksUri).header(ACCEPT, APPLICATION_JWKS).asResponse()) {
            assertEquals(APPLICATION_JWKS, response.getFirstHeader(CONTENT_TYPE));
        }

        // Test HEAD method works (Issue 41537)
        try (SimpleHttpResponse response = http.doHead(jwksUri).header(ACCEPT, APPLICATION_JWKS).asResponse()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertEquals(APPLICATION_JWKS, response.getFirstHeader(CONTENT_TYPE));
        }
    }

    @Test
    public void testIntrospectionEndpointClaim() throws IOException {
        try (Client client = ClientBuilder.newClient()) {
            ObjectNode oidcConfig = JsonSerialization
                    .readValue(getOIDCDiscoveryConfiguration(client, urls.getBase()), ObjectNode.class);
            assertEquals(oidcConfig.get("introspection_endpoint").asText(),
                    getOIDCDiscoveryRepresentation(client, urls.getBase()).getIntrospectionEndpoint());
        }
    }

    @Test
    public void testAcrValuesSupported() throws IOException {
        try (Client client = ClientBuilder.newClient()) {
            // Default values when no "acr-to-loa" mapping and no authentication flow configured
            OIDCConfigurationRepresentation oidcConfig = getOIDCDiscoveryRepresentation(client, urls.getBase());
            Assert.assertNames(oidcConfig.getAcrValuesSupported(), "0", "1");

            RealmRepresentation originalRealm = managedRealm.admin().toRepresentation();
            managedRealm.cleanup().add(realm -> realm.update(originalRealm));
            managedRealm.cleanup().add(realm -> realm.flows().getFlows().stream()
                    .filter(flow -> "discovery-step-up".equals(flow.getAlias()))
                    .forEach(flow -> realm.flows().deleteFlow(flow.getId())));

            // Update authentication flow and see it uses "acr" values from it
            configureStepUpFlow();
            oidcConfig = getOIDCDiscoveryRepresentation(client, urls.getBase());
            Assert.assertNames(oidcConfig.getAcrValuesSupported(), "0", "1", "2", "3");

            // Configure "ACR-To-Loa" mapping and check it has both configured values and numbers from authentication flow
            RealmResource testRealm = managedRealm.admin();
            RealmRepresentation realmRep = testRealm.toRepresentation();
            Map<String, Integer> acrToLoa = new HashMap<>();
            acrToLoa.put("poor", 0);
            acrToLoa.put("silver", 1);
            acrToLoa.put("gold", 2);
            String acrToLoaAttr = JsonSerialization.writeValueAsString(acrToLoa);
            realmRep.getAttributes().put(Constants.ACR_LOA_MAP, acrToLoaAttr);
            testRealm.update(realmRep);

            oidcConfig = getOIDCDiscoveryRepresentation(client, urls.getBase());
            Assert.assertNames(oidcConfig.getAcrValuesSupported(), "poor", "silver", "gold", "0", "1", "2", "3");

            // Use mappings even with values not included in the authentication flow
            acrToLoa = new HashMap<>();
            acrToLoa.put("poor", 0);
            acrToLoa.put("silver", 1);
            acrToLoa.put("gold", 2);
            acrToLoa.put("platinum", 3);
            acrToLoa.put("diamond", 4);
            acrToLoaAttr = JsonSerialization.writeValueAsString(acrToLoa);
            realmRep.getAttributes().put(Constants.ACR_LOA_MAP, acrToLoaAttr);
            testRealm.update(realmRep);

            oidcConfig = getOIDCDiscoveryRepresentation(client, urls.getBase());
            Assert.assertNames(oidcConfig.getAcrValuesSupported(), "poor", "silver", "gold", "platinum", "diamond", "0", "1", "2", "3");

        }
    }

    @Test
    public void testDpopSigningAlgValuesSupportedWithDpop() throws IOException {
        try (Client client = ClientBuilder.newClient()) {
            OIDCConfigurationRepresentation oidcConfig = getOIDCDiscoveryRepresentation(client, urls.getBase());

            // DPoP
            Assert.assertNames(oidcConfig.getDpopSigningAlgValuesSupported(), Algorithm.PS256, Algorithm.PS384, Algorithm.PS512,
                    Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256, Algorithm.ES384, Algorithm.ES512, Algorithm.EdDSA);
        }
    }

    @Test
    public void testDefaultProviderCustomizations() throws IOException {
        try (Client client = ClientBuilder.newClient()) {
            OIDCConfigurationRepresentation oidcConfig = getOIDCDiscoveryRepresentation(client, urls.getBase());

            // Exact names already tested in OIDC
            assertScopesSupportedMatchesWithRealm(oidcConfig);

            //create 2 client scope - one with hideFromOpenIDProviderMetadata equal to true
            ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
            clientScope.setName("show-scope");
            clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            try (Response response = managedRealm.admin().clientScopes().create(clientScope)) {
                String id = ApiUtil.getCreatedId(response);
                managedRealm.cleanup().add(realm -> realm.clientScopes().get(id).remove());
            }

            ClientScopeRepresentation clientScope2 = new ClientScopeRepresentation();
            clientScope2.setName("hidden-scope");
            clientScope2.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            Map<String,String> attributes = new HashMap<>();
            attributes.put(ClientScopeModel.INCLUDE_IN_OPENID_PROVIDER_METADATA,"false");
            clientScope2.setAttributes(attributes);
            try (Response response = managedRealm.admin().clientScopes().create(clientScope2)) {
                String id = ApiUtil.getCreatedId(response);
                managedRealm.cleanup().add(realm -> realm.clientScopes().get(id).remove());
            }

            List<String> expectedScopeList = Stream.of(OAuth2Constants.SCOPE_OPENID, OAuth2Constants.OFFLINE_ACCESS,
                    OAuth2Constants.SCOPE_PROFILE, OAuth2Constants.SCOPE_EMAIL, OAuth2Constants.SCOPE_PHONE, OAuth2Constants.SCOPE_ADDRESS, OIDCLoginProtocolFactory.ACR_SCOPE, OIDCLoginProtocolFactory.BASIC_SCOPE,
                    OIDCLoginProtocolFactory.ROLES_SCOPE, OIDCLoginProtocolFactory.WEB_ORIGINS_SCOPE, OIDCLoginProtocolFactory.MICROPROFILE_JWT_SCOPE, OAuth2Constants.ORGANIZATION,
                    ServiceAccountConstants.SERVICE_ACCOUNT_SCOPE, "show-scope").collect(Collectors.toList());
            oidcConfig = getOIDCDiscoveryRepresentation(client, urls.getBase());
            assertScopesSupportedMatchesWithRealm(oidcConfig, expectedScopeList);
        }
    }

    private void assertScopesSupportedMatchesWithRealm(OIDCConfigurationRepresentation oidcConfig, List<String> expectedScopeList) {
        Assert.assertNames(oidcConfig.getScopesSupported(), expectedScopeList.toArray(new String[expectedScopeList.size()]) );
    }

    protected void assertScopesSupportedMatchesWithRealm(OIDCConfigurationRepresentation oidcConfig) {
        Assert.assertNames(oidcConfig.getScopesSupported(), OAuth2Constants.SCOPE_OPENID, OAuth2Constants.OFFLINE_ACCESS,
                OAuth2Constants.SCOPE_PROFILE, OAuth2Constants.SCOPE_EMAIL, OAuth2Constants.SCOPE_PHONE, OAuth2Constants.SCOPE_ADDRESS, OIDCLoginProtocolFactory.ACR_SCOPE, OIDCLoginProtocolFactory.BASIC_SCOPE,
                OIDCLoginProtocolFactory.ROLES_SCOPE, OIDCLoginProtocolFactory.WEB_ORIGINS_SCOPE, OIDCLoginProtocolFactory.MICROPROFILE_JWT_SCOPE, OAuth2Constants.ORGANIZATION,
                ServiceAccountConstants.SERVICE_ACCOUNT_SCOPE);
    }

    protected OIDCConfigurationRepresentation getOIDCDiscoveryRepresentation(Client client, String uriTemplate) {
        try {
            return JsonSerialization.readValue(getOIDCDiscoveryConfiguration(client, uriTemplate), OIDCConfigurationRepresentation.class);
        } catch (IOException cause) {
            throw new RuntimeException("Failed to parse OIDC configuration", cause);
        }
    }

    protected URI getOIDCDiscoveryUri(UriBuilder builder) {
        return RealmsResource.wellKnownProviderUrl(builder).build(managedRealm.getName(), this.getWellKnownProviderId());
    }

    private String getOIDCDiscoveryConfiguration(Client client, String uriTemplate) {
        UriBuilder builder = UriBuilder.fromUri(uriTemplate);
        URI oidcDiscoveryUri = getOIDCDiscoveryUri(builder);
        WebTarget oidcDiscoveryTarget = client.target(oidcDiscoveryUri);

        try (Response response = oidcDiscoveryTarget.request().get()) {
            assertEquals("no-cache, must-revalidate, no-transform, no-store", response.getHeaders().getFirst("Cache-Control"));
            return response.readEntity(String.class);
        }
    }

    private void registerEcKeyProvider() {
        ComponentRepresentation key = new ComponentRepresentation();
        key.setName("valid");
        key.setParentId(managedRealm.getId());
        key.setProviderId("ecdsa-generated");
        key.setProviderType(KeyProvider.class.getName());
        key.setConfig(new MultivaluedHashMap<>());
        key.getConfig().putSingle("priority", Long.toString(System.currentTimeMillis()));
        key.getConfig().putSingle("ecdsaEllipticCurveKey", "P-256");
        try (Response response = managedRealm.admin().components().add(key)) {
            String id = ApiUtil.getCreatedId(response);
            managedRealm.cleanup().add(realm -> realm.components().component(id).remove());
        }
    }

    private void configureStepUpFlow() {
        // Discovery reads the LoA conditions; no authentication is performed with this flow.
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow("discovery-step-up")
                .inForms(forms -> {
                    forms.clear();
                    for (int level = 1; level <= 3; level++) {
                        String loa = Integer.toString(level);
                        forms.addSubFlowExecution("discovery-level-" + loa, AuthenticationFlow.BASIC_FLOW,
                                Requirement.CONDITIONAL, subFlow -> subFlow.addAuthenticatorExecution(
                                        Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID, config -> {
                                            config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, loa);
                                            config.getConfig().put(ConditionalLoaAuthenticator.MAX_AGE,
                                                    loa.equals("1") ? Integer.toString(ConditionalLoaAuthenticator.DEFAULT_MAX_AGE) : "0");
                                        }));
                    }
                }).defineAsBrowserFlow());
    }

    private void assertContains(List<String> actual, String... expected) {
        for (String exp : expected) {
            Assertions.assertTrue(actual.contains(exp));
        }
    }
}
