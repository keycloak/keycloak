/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.FullNameMapper;
import org.keycloak.protocol.oidc.mappers.HardcodedRole;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;
import org.keycloak.protocol.saml.mappers.UserAttributeStatementMapper;
import org.keycloak.protocol.saml.mappers.UserPropertyAttributeStatementMapper;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ComponentTypeRepresentation;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientregistration.RegistrationAccessToken;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.services.clientregistration.policy.impl.ClientDisabledClientRegistrationPolicyFactory;
import org.keycloak.services.clientregistration.policy.impl.ClientScopesClientRegistrationPolicyFactory;
import org.keycloak.services.clientregistration.policy.impl.MaxClientsClientRegistrationPolicyFactory;
import org.keycloak.services.clientregistration.policy.impl.ProtocolMappersClientRegistrationPolicyFactory;
import org.keycloak.services.clientregistration.policy.impl.TrustedHostClientRegistrationPolicyFactory;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertTrue;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientRegistrationPoliciesTest extends AbstractClientRegistrationTest {

    private static final String PRIVATE_KEY = "MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=";
    private static final String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        testRealms.get(0).setId(REALM_NAME);
        testRealms.get(0).setPrivateKey(PRIVATE_KEY);
        testRealms.get(0).setPublicKey(PUBLIC_KEY);
    }

    @After
    @Override
    public void after() throws Exception {
        super.after();

        // Default setup of trustedHostPolicy
        ComponentRepresentation trustedHostPolicy = findPolicyByProviderAndAuth(TrustedHostClientRegistrationPolicyFactory.PROVIDER_ID, getPolicyAnon());
        trustedHostPolicy.getConfig().putSingle(TrustedHostClientRegistrationPolicyFactory.HOST_SENDING_REGISTRATION_REQUEST_MUST_MATCH, "true");
        trustedHostPolicy.getConfig().putSingle(TrustedHostClientRegistrationPolicyFactory.CLIENT_URIS_MUST_MATCH, "true");
        trustedHostPolicy.getConfig().put(TrustedHostClientRegistrationPolicyFactory.TRUSTED_HOSTS, Collections.emptyList());
        realmResource().components().component(trustedHostPolicy.getId()).update(trustedHostPolicy);
    }

    private RealmResource realmResource() {
        return adminClient.realm(REALM_NAME);
    }

    private ClientRepresentation createRep(String clientId) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setSecret("test-secret");
        return client;
    }

    private OIDCClientRepresentation createRepOidc() {
        return createRepOidc("http://localhost:8080/foo", "http://localhost:8080/foo");
    }

    private OIDCClientRepresentation createRepOidc(String clientBaseUri, String clientRedirectUri) {
        OIDCClientRepresentation client = new OIDCClientRepresentation();
        client.setClientName("RegistrationAccessTokenTest");
        client.setClientUri(clientBaseUri);
        client.setRedirectUris(Collections.singletonList(clientRedirectUri));
        return client;
    }

    public OIDCClientRepresentation create() throws ClientRegistrationException {
        OIDCClientRepresentation client = createRepOidc();

        OIDCClientRepresentation response = reg.oidc().create(client);

        reg.auth(Auth.token(response));

        return response;
    }

    private void assertOidcFail(ClientRegOp operation, OIDCClientRepresentation client, int expectedStatusCode) {
        assertOidcFail(operation, client, expectedStatusCode, null);
    }

    private void assertOidcFail(ClientRegOp operation, OIDCClientRepresentation client, int expectedStatusCode, String expectedErrorContains) {
        try {
            switch (operation) {
                case CREATE: reg.oidc().create(client);
                    break;
                case UPDATE: reg.oidc().update(client);
                    break;
                case DELETE: reg.oidc().delete(client);
                    break;
            }

            Assert.fail("Not expected to successfuly run operation " + operation.toString() + " on client");
        } catch (ClientRegistrationException expected) {
            HttpErrorException httpEx = (HttpErrorException) expected.getCause();
            Assert.assertEquals(expectedStatusCode, httpEx.getStatusLine().getStatusCode());
            if (expectedErrorContains != null) {
                assertTrue("Error response doesn't contain expected text. The error response text is: " + httpEx.getErrorResponse(), httpEx.getErrorResponse().contains(expectedErrorContains));
            }
        }
    }

    private void assertFail(ClientRegOp operation, ClientRepresentation client, int expectedStatusCode, String expectedErrorContains) {
        try {
            switch (operation) {
                case CREATE: reg.create(client);
                    break;
                case UPDATE: reg.update(client);
                    break;
                case DELETE: reg.delete(client);
                    break;
            }

            Assert.fail("Not expected to successfuly run operation " + operation.toString() + " on client");
        } catch (ClientRegistrationException expected) {
            HttpErrorException httpEx = (HttpErrorException) expected.getCause();
            Assert.assertEquals(expectedStatusCode, httpEx.getStatusLine().getStatusCode());
            if (expectedErrorContains != null) {
                assertTrue("Error response doesn't contain expected text. The error response text is: " + httpEx.getErrorResponse(), httpEx.getErrorResponse().contains(expectedErrorContains));
            }
        }
    }


    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE) // We would need to do domain name -> ip address to set trusted host
    public void testAnonCreateWithTrustedHost() throws Exception {
        // Failed to create client (untrusted host)
        OIDCClientRepresentation client = createRepOidc("http://root", "http://redirect");
        assertOidcFail(ClientRegOp.CREATE, client, 403, "Host not trusted");

        // Should still fail (bad redirect_uri)
        setTrustedHost("localhost");
        assertOidcFail(ClientRegOp.CREATE, client, 403, "URL doesn't match");

        // Should still fail (bad base_uri)
        client.setRedirectUris(Collections.singletonList("http://localhost:8080/foo"));
        assertOidcFail(ClientRegOp.CREATE, client, 403, "URL doesn't match");

        // Success create client
        client.setClientUri("http://localhost:8080/foo");
        OIDCClientRepresentation oidcClientRep = reg.oidc().create(client);


        // Test registration access token
        assertRegAccessToken(oidcClientRep.getRegistrationAccessToken(), RegistrationAuth.ANONYMOUS);
    }


    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE) // We would need to do domain name -> ip address to set trusted host
    public void testAnonUpdateWithTrustedHost() throws Exception {
        setTrustedHost("localhost");
        OIDCClientRepresentation client = create();

        // Fail update client
        client.setRedirectUris(Collections.singletonList("http://bad:8080/foo"));
        assertOidcFail(ClientRegOp.UPDATE, client, 403, "URL doesn't match");

        // Should be fine now
        client.setRedirectUris(Collections.singletonList("http://localhost:8080/foo"));
        reg.oidc().update(client);
    }


    @Test
    public void testRedirectUriWithDomain() throws Exception {
        // Change the policy to avoid checking hosts
        ComponentRepresentation trustedHostPolicyRep = findPolicyByProviderAndAuth(TrustedHostClientRegistrationPolicyFactory.PROVIDER_ID, getPolicyAnon());
        trustedHostPolicyRep.getConfig().putSingle(TrustedHostClientRegistrationPolicyFactory.HOST_SENDING_REGISTRATION_REQUEST_MUST_MATCH, "false");

        // Configure some trusted host and domain
        trustedHostPolicyRep.getConfig().put(TrustedHostClientRegistrationPolicyFactory.TRUSTED_HOSTS, Arrays.asList("www.host.com", "*.example.com"));
        realmResource().components().component(trustedHostPolicyRep.getId()).update(trustedHostPolicyRep);

        // Verify client can be created with the redirectUri from trusted host and domain
        OIDCClientRepresentation oidcClientRep = createRepOidc("http://www.host.com", "http://www.example.com");
        reg.oidc().create(oidcClientRep);

        // Remove domain from the config
        trustedHostPolicyRep.getConfig().put(TrustedHostClientRegistrationPolicyFactory.TRUSTED_HOSTS, Arrays.asList("www.host.com", "www1.example.com"));
        realmResource().components().component(trustedHostPolicyRep.getId()).update(trustedHostPolicyRep);

        // Check new client can't be created anymore
        oidcClientRep = createRepOidc("http://www.host.com", "http://www.example.com");
        assertOidcFail(ClientRegOp.CREATE, oidcClientRep, 403, "URL doesn't match");
    }




    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE) // We would need to do domain name -> ip address to set trusted host
    public void testAnonConsentRequired() throws Exception {
        setTrustedHost("localhost");
        OIDCClientRepresentation client = create();

        // Assert new client has consent required
        String clientId = client.getClientId();
        ClientRepresentation clientRep = ApiUtil.findClientByClientId(realmResource(), clientId).toRepresentation();
        Assert.assertTrue(clientRep.isConsentRequired());

        // Try update with disabled consent required. Should fail
        clientRep.setConsentRequired(false);
        assertFail(ClientRegOp.UPDATE, clientRep, 403, "Not permitted to update consentRequired to false");

        // Try update with enabled consent required. Should pass
        clientRep.setConsentRequired(true);
        reg.update(clientRep);
    }


    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE) // We would need to do domain name -> ip address to set trusted host
    public void testAnonFullScopeAllowed() throws Exception {
        setTrustedHost("localhost");
        OIDCClientRepresentation client = create();

        // Assert new client has fullScopeAllowed disabled
        String clientId = client.getClientId();
        ClientRepresentation clientRep = ApiUtil.findClientByClientId(realmResource(), clientId).toRepresentation();
        Assert.assertFalse(clientRep.isFullScopeAllowed());

        // Try update with disabled consent required. Should fail
        clientRep.setFullScopeAllowed(true);
        assertFail(ClientRegOp.UPDATE, clientRep, 403, "Not permitted to enable fullScopeAllowed");

        // Try update with enabled consent required. Should pass
        clientRep.setFullScopeAllowed(false);
        reg.update(clientRep);
    }


    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE) // We would need to do domain name -> ip address to set trusted host
    public void testClientDisabledPolicy() throws Exception {
        setTrustedHost("localhost");

        // Assert new client is enabled
        OIDCClientRepresentation client = create();
        String clientId = client.getClientId();
        ClientRepresentation clientRep = ApiUtil.findClientByClientId(realmResource(), clientId).toRepresentation();
        Assert.assertTrue(clientRep.isEnabled());

        // Add client-disabled policy
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName("Clients disabled");
        rep.setParentId(adminClient.realm(REALM_NAME).toRepresentation().getId());
        rep.setProviderId(ClientDisabledClientRegistrationPolicyFactory.PROVIDER_ID);
        rep.setProviderType(ClientRegistrationPolicy.class.getName());
        rep.setSubType(getPolicyAnon());
        Response response = realmResource().components().add(rep);
        String policyId = ApiUtil.getCreatedId(response);
        response.close();

        // Assert new client is disabled
        client = create();
        clientId = client.getClientId();
        clientRep = ApiUtil.findClientByClientId(realmResource(), clientId).toRepresentation();
        Assert.assertFalse(clientRep.isEnabled());

        // Try enable client. Should fail
        clientRep.setEnabled(true);
        assertFail(ClientRegOp.UPDATE, clientRep, 403, "Not permitted to enable client");

        // Try update disabled client. Should pass
        clientRep.setEnabled(false);
        reg.update(clientRep);

        // Revert
        realmResource().components().component(policyId).remove();
    }


    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE) // We would need to do domain name -> ip address to set trusted host
    public void testMaxClientsPolicy() throws Exception {
        setTrustedHost("localhost");

        int clientsCount = realmResource().clients().findAll().size();
        int newClientsLimit = clientsCount + 1;

        // Allow to create one more client to current limit
        ComponentRepresentation maxClientsPolicyRep = findPolicyByProviderAndAuth(MaxClientsClientRegistrationPolicyFactory.PROVIDER_ID, getPolicyAnon());
        maxClientsPolicyRep.getConfig().putSingle(MaxClientsClientRegistrationPolicyFactory.MAX_CLIENTS, String.valueOf(newClientsLimit));
        realmResource().components().component(maxClientsPolicyRep.getId()).update(maxClientsPolicyRep);

        // I can register one new client
        OIDCClientRepresentation client = create();

        // I can't register more clients
        assertOidcFail(ClientRegOp.CREATE, createRepOidc(), 403, "It's allowed to have max " + newClientsLimit + " clients per realm");

        // Revert
        maxClientsPolicyRep.getConfig().putSingle(MaxClientsClientRegistrationPolicyFactory.MAX_CLIENTS, String.valueOf(10000));
        realmResource().components().component(maxClientsPolicyRep.getId()).update(maxClientsPolicyRep);
    }


    @Test
    public void testProviders() throws Exception {
        List<ComponentTypeRepresentation> reps = realmResource().clientRegistrationPolicy().getProviders();
        Map<String, ComponentTypeRepresentation> providersMap = reps.stream().collect(Collectors.toMap((ComponentTypeRepresentation rep) -> {
            return rep.getId();
        }, (ComponentTypeRepresentation rep) -> {
            return rep;
        }));

        // test that ProtocolMappersClientRegistrationPolicy provider contains available protocol mappers
        ComponentTypeRepresentation protMappersRep = providersMap.get(ProtocolMappersClientRegistrationPolicyFactory.PROVIDER_ID);
        List<String> availableMappers = getProviderConfigProperty(protMappersRep, ProtocolMappersClientRegistrationPolicyFactory.ALLOWED_PROTOCOL_MAPPER_TYPES);

        List<String> someExpectedMappers = Arrays.asList(UserAttributeStatementMapper.PROVIDER_ID,
                UserAttributeMapper.PROVIDER_ID,
                UserPropertyAttributeStatementMapper.PROVIDER_ID,
                UserPropertyMapper.PROVIDER_ID, HardcodedRole.PROVIDER_ID);
        availableMappers.containsAll(someExpectedMappers);

        // test that clientScope provider contains just the default client scopes
        ComponentTypeRepresentation clientScopeRep = providersMap.get(ClientScopesClientRegistrationPolicyFactory.PROVIDER_ID);
        List<String> clientScopes = getProviderConfigProperty(clientScopeRep, ClientScopesClientRegistrationPolicyFactory.ALLOWED_CLIENT_SCOPES);
        Assert.assertFalse(clientScopes.isEmpty());
        Assert.assertTrue(clientScopes.contains(OAuth2Constants.SCOPE_PROFILE));
        Assert.assertFalse(clientScopes.contains("foo"));
        Assert.assertFalse(clientScopes.contains("bar"));

        // Add some clientScopes
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("foo");
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response response = realmResource().clientScopes().create(clientScope);
        String fooScopeId = ApiUtil.getCreatedId(response);
        response.close();

        clientScope = new ClientScopeRepresentation();
        clientScope.setName("bar");
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        response = realmResource().clientScopes().create(clientScope);
        String barScopeId = ApiUtil.getCreatedId(response);
        response.close();

        // send request again and test that clientScope provider contains added client scopes
        reps = realmResource().clientRegistrationPolicy().getProviders();
        clientScopeRep = reps.stream().filter((ComponentTypeRepresentation rep1) -> {

            return rep1.getId().equals(ClientScopesClientRegistrationPolicyFactory.PROVIDER_ID);

        }).findFirst().get();

        clientScopes = getProviderConfigProperty(clientScopeRep, ClientScopesClientRegistrationPolicyFactory.ALLOWED_CLIENT_SCOPES);
        Assert.assertTrue(clientScopes.contains("foo"));
        Assert.assertTrue(clientScopes.contains("bar"));

        // Revert client scopes
        realmResource().clientScopes().get(fooScopeId).remove();
        realmResource().clientScopes().get(barScopeId).remove();
    }

    private List<String> getProviderConfigProperty(ComponentTypeRepresentation provider, String expectedConfigPropName) {
        Assert.assertNotNull(provider);

        List<ConfigPropertyRepresentation> list = provider.getProperties();

        list = list.stream().filter((ConfigPropertyRepresentation rep) -> {

            return rep.getName().equals(expectedConfigPropName);

        }).collect(Collectors.toList());

        Assert.assertEquals(list.size(), 1);
        ConfigPropertyRepresentation allowedProtocolMappers = list.get(0);

        Assert.assertEquals(allowedProtocolMappers.getName(), expectedConfigPropName);
        return allowedProtocolMappers.getOptions();
    }


    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE) // We would need to do domain name -> ip address to set trusted host
    public void testClientScopesPolicy() throws Exception {
        setTrustedHost("localhost");

        // Add some clientScope through Admin REST
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("foo");
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response response = realmResource().clientScopes().create(clientScope);
        String clientScopeId = ApiUtil.getCreatedId(response);
        response.close();

        // I can't register new client with this scope
        ClientRepresentation clientRep = createRep("test-app");
        clientRep.setDefaultClientScopes(Collections.singletonList("foo"));
        assertFail(ClientRegOp.CREATE, clientRep, 403, "Not permitted to use specified clientScope");

        // Register client without scope - should success
        clientRep.setDefaultClientScopes(null);
        ClientRepresentation registeredClient = reg.create(clientRep);
        reg.auth(Auth.token(registeredClient));

        // Try to update client with scope - should fail
        registeredClient.setDefaultClientScopes(Collections.singletonList("foo"));
        assertFail(ClientRegOp.UPDATE, registeredClient, 403, "Not permitted to use specified clientScope");

        // Update client with the clientScope via Admin REST
        ClientResource client = ApiUtil.findClientByClientId(realmResource(), "test-app");
        client.addDefaultClientScope(clientScopeId);

        // Now the update via clientRegistration is permitted too as scope was already set
        reg.update(registeredClient);

        // Revert client scope
        realmResource().clients().get(client.toRepresentation().getId()).remove();
        realmResource().clientScopes().get(clientScopeId).remove();
    }


    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE) // We would need to do domain name -> ip address to set trusted host
    public void testClientScopesPolicyWithPermittedScope() throws Exception {
        setTrustedHost("localhost");

        // Add some clientScope through Admin REST
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("foo");
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response response = realmResource().clientScopes().create(clientScope);
        String clientScopeId = ApiUtil.getCreatedId(response);
        response.close();

        // I can't register new client with this scope
        ClientRepresentation clientRep = createRep("test-app");
        clientRep.setDefaultClientScopes(Collections.singletonList("foo"));
        assertFail(ClientRegOp.CREATE, clientRep, 403, "Not permitted to use specified clientScope");

        // Update the policy to allow the "foo" scope
        ComponentRepresentation clientScopesPolicyRep = findPolicyByProviderAndAuth(ClientScopesClientRegistrationPolicyFactory.PROVIDER_ID, getPolicyAnon());
        clientScopesPolicyRep.getConfig().putSingle(ClientScopesClientRegistrationPolicyFactory.ALLOWED_CLIENT_SCOPES, "foo");
        realmResource().components().component(clientScopesPolicyRep.getId()).update(clientScopesPolicyRep);

        // Check that I can register client now
        ClientRepresentation registeredClient = reg.create(clientRep);
        Assert.assertNotNull(registeredClient.getRegistrationAccessToken());

        // Revert client scope
        ApiUtil.findClientResourceByClientId(realmResource(), "test-app").remove();
        realmResource().clientScopes().get(clientScopeId).remove();
    }


    // PROTOCOL MAPPERS

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE) // We would need to do domain name -> ip address to set trusted host
    public void testProtocolMappersCreate() throws Exception {
        setTrustedHost("localhost");

        // Try to add client with some "hardcoded role" mapper. Should fail
        ClientRepresentation clientRep = createRep("test-app");
        clientRep.setProtocolMappers(Collections.singletonList(createHardcodedMapperRep()));
        assertFail(ClientRegOp.CREATE, clientRep, 403, "ProtocolMapper type not allowed");

        // Try the same authenticated. Should still fail.
        ClientInitialAccessPresentation token = adminClient.realm(REALM_NAME).clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));
        assertFail(ClientRegOp.CREATE, clientRep, 403, "ProtocolMapper type not allowed");

        // Update the "authenticated" policy and allow hardcoded role mapper
        ComponentRepresentation protocolMapperPolicyRep = findPolicyByProviderAndAuth(ProtocolMappersClientRegistrationPolicyFactory.PROVIDER_ID, getPolicyAuth());
        protocolMapperPolicyRep.getConfig().add(ProtocolMappersClientRegistrationPolicyFactory.ALLOWED_PROTOCOL_MAPPER_TYPES, HardcodedRole.PROVIDER_ID);
        realmResource().components().component(protocolMapperPolicyRep.getId()).update(protocolMapperPolicyRep);

        // Check authenticated registration is permitted
        ClientRepresentation registeredClient = reg.create(clientRep);
        Assert.assertNotNull(registeredClient.getRegistrationAccessToken());

        // Check "anonymous" registration still fails
        clientRep = createRep("test-app-2");
        clientRep.setProtocolMappers(Collections.singletonList(createHardcodedMapperRep()));
        reg.auth(null);
        assertFail(ClientRegOp.CREATE, clientRep, 403, "ProtocolMapper type not allowed");

        // Revert policy change
        ApiUtil.findClientResourceByClientId(realmResource(), "test-app").remove();
        protocolMapperPolicyRep.getConfig().remove(ProtocolMappersClientRegistrationPolicyFactory.ALLOWED_PROTOCOL_MAPPER_TYPES, HardcodedRole.PROVIDER_ID);
        realmResource().components().component(protocolMapperPolicyRep.getId()).update(protocolMapperPolicyRep);
    }


    private ProtocolMapperRepresentation createHardcodedMapperRep() {
        ProtocolMapperRepresentation protocolMapper = new ProtocolMapperRepresentation();
        protocolMapper.setName("Hardcoded foo role");
        protocolMapper.setProtocolMapper(HardcodedRole.PROVIDER_ID);
        protocolMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        protocolMapper.getConfig().put(HardcodedRole.ROLE_CONFIG, "foo-role");
        return protocolMapper;
    }


    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE) // We would need to do domain name -> ip address to set trusted host
    public void testProtocolMappersUpdate() throws Exception {
        setTrustedHost("localhost");

        // Check I can add client with allowed protocolMappers
        ProtocolMapperRepresentation protocolMapper = new ProtocolMapperRepresentation();
        protocolMapper.setName("Full name");
        protocolMapper.setProtocolMapper(FullNameMapper.PROVIDER_ID);
        protocolMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        ClientRepresentation clientRep = createRep("test-app");
        clientRep.setProtocolMappers(Collections.singletonList(protocolMapper));

        ClientRepresentation registeredClient = reg.create(clientRep);
        reg.auth(Auth.token(registeredClient));

        // Add some disallowed protocolMapper
        registeredClient.getProtocolMappers().add(createHardcodedMapperRep());

        // Check I can't update client because of protocolMapper
        assertFail(ClientRegOp.UPDATE, registeredClient, 403, "ProtocolMapper type not allowed");

        // Remove "bad" protocolMapper
        registeredClient.getProtocolMappers().removeIf((ProtocolMapperRepresentation mapper) -> {
            return mapper.getProtocolMapper().equals(HardcodedRole.PROVIDER_ID);
        });

        // Check I can update client now
        reg.update(registeredClient);

        // Revert client
        ApiUtil.findClientResourceByClientId(realmResource(), "test-app").remove();
    }


    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE) // We would need to do domain name -> ip address to set trusted host
    public void testProtocolMappersConsentRequired() throws Exception {
        setTrustedHost("localhost");

        // Register client and assert it doesn't have builtin protocol mappers
        ClientRepresentation clientRep = createRep("test-app");
        ClientRepresentation registeredClient = reg.create(clientRep);

        Assert.assertNull(registeredClient.getProtocolMappers());

        // Revert
        ApiUtil.findClientResourceByClientId(realmResource(), "test-app").remove();
    }


    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE) // We would need to do domain name -> ip address to set trusted host
    public void testProtocolMappersRemoveBuiltins() throws Exception {
        setTrustedHost("localhost");

        // Change policy to allow hardcoded mapper

        ComponentRepresentation protocolMapperPolicyRep = findPolicyByProviderAndAuth(ProtocolMappersClientRegistrationPolicyFactory.PROVIDER_ID, getPolicyAnon());
        protocolMapperPolicyRep.getConfig().add(ProtocolMappersClientRegistrationPolicyFactory.ALLOWED_PROTOCOL_MAPPER_TYPES, HardcodedRole.PROVIDER_ID);
        realmResource().components().component(protocolMapperPolicyRep.getId()).update(protocolMapperPolicyRep);

        // Create client with hardcoded mapper
        ClientRepresentation clientRep = createRep("test-app");
        clientRep.setProtocolMappers(Collections.singletonList(createHardcodedMapperRep()));
        ClientRepresentation registeredClient = reg.create(clientRep);

        Assert.assertEquals(1, registeredClient.getProtocolMappers().size());
        ProtocolMapperRepresentation hardcodedMapper = registeredClient.getProtocolMappers().get(0);

        // Revert
        ApiUtil.findClientResourceByClientId(realmResource(), "test-app").remove();
        protocolMapperPolicyRep.getConfig().remove(ProtocolMappersClientRegistrationPolicyFactory.ALLOWED_PROTOCOL_MAPPER_TYPES, HardcodedRole.PROVIDER_ID);
        realmResource().components().component(protocolMapperPolicyRep.getId()).update(protocolMapperPolicyRep);
    }

    // HELPER METHODS


    private String getPolicyAnon() {
        return ClientRegistrationPolicyManager.getComponentTypeKey(RegistrationAuth.ANONYMOUS);
    }

    private String getPolicyAuth() {
        return ClientRegistrationPolicyManager.getComponentTypeKey(RegistrationAuth.AUTHENTICATED);
    }

    private ComponentRepresentation findPolicyByProviderAndAuth(String providerId, String authType) {
        // Change the policy to avoid checking hosts
        String parentId = realmResource().toRepresentation().getId();
        List<ComponentRepresentation> reps = realmResource().components().query(parentId, ClientRegistrationPolicy.class.getName());
        for (ComponentRepresentation rep : reps) {
            if (rep.getSubType().equals(authType) && rep.getProviderId().equals(providerId)) {
                return rep;
            }
        }
        return null;
    }

    private void setTrustedHost(String hostname) {
        ComponentRepresentation trustedHostRep = findPolicyByProviderAndAuth(TrustedHostClientRegistrationPolicyFactory.PROVIDER_ID, getPolicyAnon());
        trustedHostRep.getConfig().putSingle(TrustedHostClientRegistrationPolicyFactory.TRUSTED_HOSTS, hostname);
        realmResource().components().component(trustedHostRep.getId()).update(trustedHostRep);
    }

    private void assertRegAccessToken(String registrationAccessToken, RegistrationAuth expectedRegAuth) throws Exception {
        byte[] content = new JWSInput(registrationAccessToken).getContent();
        RegistrationAccessToken regAccessToken = JsonSerialization.readValue(content, RegistrationAccessToken.class);
        Assert.assertEquals(regAccessToken.getRegistrationAuth(), expectedRegAuth.toString().toLowerCase());
    }

    private enum ClientRegOp {
        CREATE, READ, UPDATE, DELETE
    }

}
