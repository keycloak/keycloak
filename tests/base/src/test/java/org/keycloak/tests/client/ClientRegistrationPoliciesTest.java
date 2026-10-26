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

package org.keycloak.tests.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.CibaConfig;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.FullNameMapper;
import org.keycloak.protocol.oidc.mappers.HardcodedRole;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.UserAttributeStatementMapper;
import org.keycloak.protocol.saml.mappers.UserPropertyAttributeStatementMapper;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ComponentTypeRepresentation;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
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
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
public class ClientRegistrationPoliciesTest extends AbstractClientRegistrationTest {

    @AfterEach
    public void after() throws Exception {

        // Default setup of trustedHostPolicy
        ComponentRepresentation trustedHostPolicy = findPolicyByProviderAndAuth(TrustedHostClientRegistrationPolicyFactory.PROVIDER_ID, getPolicyAnon());
        trustedHostPolicy.getConfig().putSingle(TrustedHostClientRegistrationPolicyFactory.HOST_SENDING_REGISTRATION_REQUEST_MUST_MATCH, "true");
        trustedHostPolicy.getConfig().putSingle(TrustedHostClientRegistrationPolicyFactory.CLIENT_URIS_MUST_MATCH, "true");
        trustedHostPolicy.getConfig().put(TrustedHostClientRegistrationPolicyFactory.TRUSTED_HOSTS, Collections.emptyList());
        managedRealm.admin().components().component(trustedHostPolicy.getId()).update(trustedHostPolicy);
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

            Assertions.fail("Not expected to successfuly run operation " + operation.toString() + " on client");
        } catch (ClientRegistrationException expected) {
            HttpErrorException httpEx = (HttpErrorException) expected.getCause();
            Assertions.assertEquals(expectedStatusCode, httpEx.getStatusLine().getStatusCode());
            if (expectedErrorContains != null) {
                assertTrue(httpEx.getErrorResponse().contains(expectedErrorContains), "Error response doesn't contain expected text. The error response text is: " + httpEx.getErrorResponse());
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

            Assertions.fail("Not expected to successfuly run operation " + operation.toString() + " on client");
        } catch (ClientRegistrationException expected) {
            HttpErrorException httpEx = (HttpErrorException) expected.getCause();
            Assertions.assertEquals(expectedStatusCode, httpEx.getStatusLine().getStatusCode());
            if (expectedErrorContains != null) {
                assertTrue(httpEx.getErrorResponse().contains(expectedErrorContains), "Error response doesn't contain expected text. The error response text is: " + httpEx.getErrorResponse());
            }
        }
    }


    @Test
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
    public void testAnonCreateWithInvalidRedirectScheme() throws Exception {
        setTrustedHost("localhost");
        OIDCClientRepresentation client = createRepOidc();

        client.setRedirectUris(Collections.singletonList("invalid://bad.host"));
        assertOidcFail(ClientRegOp.CREATE, client, 403, "URI doesn't match");

        client.setRedirectUris(Collections.singletonList("invalid://localhost"));
        OIDCClientRepresentation oidcClientRep = reg.oidc().create(client);

        assertRegAccessToken(oidcClientRep.getRegistrationAccessToken(), RegistrationAuth.ANONYMOUS);
    }


    @Test
    public void testAnonUpdateWithTrustedHost() throws Exception {
        setTrustedHost("localhost");
        OIDCClientRepresentation client = create();

        // Fail update client
        client.setRedirectUris(Collections.singletonList("http://bad:8080/foo"));
        assertOidcFail(ClientRegOp.UPDATE, client, 403, "URI doesn't match");

        // Should be fine now
        client.setRedirectUris(Collections.singletonList("http://localhost:8080/foo"));
        reg.oidc().update(client);
    }



    @Test
    public void testAnonCreateWithClientUris() throws Exception {
        setTrustedHost("localhost");

        assertOidcCreateFail(c -> c.setPostLogoutRedirectUris(List.of("http://untrusted.host/logout")), "URI doesn't match");
        assertOidcCreateFail(c -> c.setRequestUris(List.of("http://untrusted.host/request")), "URI doesn't match");
        assertOidcCreateFail(c -> c.setBackchannelLogoutUri("http://untrusted.host/backchannel-logout"), "URL doesn't match");
        assertOidcCreateFail(c -> c.setFrontChannelLogoutUri("http://untrusted.host/frontchannel-logout"), "URL doesn't match");
        assertOidcCreateFail(c -> c.setLogoUri("http://untrusted.host/logo"), "URL doesn't match");
        assertOidcCreateFail(c -> c.setPolicyUri("http://untrusted.host/policy"), "URL doesn't match");
        assertOidcCreateFail(c -> c.setTosUri("http://untrusted.host/tos"), "URL doesn't match");
        assertOidcCreateFail(c -> c.setJwksUri("http://untrusted.host/jwks"), "URL doesn't match");
        assertOidcCreateFail(c -> c.setSectorIdentifierUri("http://untrusted.host/sector"), "URL doesn't match");
        assertOidcCreateFail(c -> c.setBackchannelClientNotificationEndpoint("http://untrusted.host/ciba"), "URL doesn't match");

        // A protocol-relative URI carries its own authority, so it must not be resolved against the
        // root URL. The front-channel logout iframe renders it as is, reaching the untrusted host
        assertOidcCreateFail(c -> c.setFrontChannelLogoutUri("//untrusted.host/frontchannel-logout"), "URL doesn't match");

        // Trusted URIs are accepted, "+" just refers to the redirect URIs
        OIDCClientRepresentation client = createRepOidc();
        client.setPostLogoutRedirectUris(Collections.singletonList("+"));
        client.setRequestUris(List.of("http://localhost:8080/request"));
        client.setBackchannelLogoutUri("http://localhost:8080/backchannel-logout");
        client.setFrontChannelLogoutUri("http://localhost:8080/frontchannel-logout");
        OIDCClientRepresentation oidcClientRep = reg.oidc().create(client);
        reg.auth(Auth.token(oidcClientRep));

        // Fail update as well
        oidcClientRep.setPostLogoutRedirectUris(List.of("http://untrusted.host/logout"));
        assertOidcFail(ClientRegOp.UPDATE, oidcClientRep, 403, "URI doesn't match");
        Assertions.assertEquals("+", managedRealm.admin().clients().findByClientId(oidcClientRep.getClientId())
                .get(0).getAttributes().get(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS));

        oidcClientRep.setPostLogoutRedirectUris(List.of("http://localhost:8080/logout"));
        reg.oidc().update(oidcClientRep);
    }


    @Test
    public void testAnonCreateWithClientUrisDefaultProvider() throws Exception {
        setTrustedHost("localhost");

        // The default provider sets the attributes directly, without going through the OIDC converter
        ClientRepresentation client = createRep("client-uris-client");
        client.setRootUrl("http://localhost:8080");

        client.setAttributes(Map.of(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+##http://untrusted.host/logout"));
        assertFail(ClientRegOp.CREATE, client, 403, "URI doesn't match");

        // The authority of a protocol-relative URI must not be hidden behind the trusted root URL
        client.setAttributes(Map.of(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, "//untrusted.host/frontchannel-logout"));
        assertFail(ClientRegOp.CREATE, client, 403, "URL doesn't match");

        client.setAttributes(Map.of(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+##http://localhost:8080/logout"));
        ClientRepresentation registered = reg.create(client);
        reg.auth(Auth.token(registered));
        for (String value : List.of("+##http://untrusted.host/logout", "*", "http://localhost/logout##http://untrusted.host/logout")) {
            registered.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, value);
            assertFail(ClientRegOp.UPDATE, registered, 403, "URI doesn't match");
        }
        for (String rootUrl : List.of("", "http://localhost:8080")) {
            registered.setRootUrl(rootUrl);
            registered.setAttributes(Map.of(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, "//untrusted.host/logout"));
            assertFail(ClientRegOp.UPDATE, registered, 403, "URL doesn't match");
        }
    }

    @Test
    public void testTrustedHostsUriCheckCanStillBeDisabled() throws Exception {
        setTrustedHost("localhost");
        ComponentRepresentation policy = findPolicyByProviderAndAuth(TrustedHostClientRegistrationPolicyFactory.PROVIDER_ID, getPolicyAnon());
        policy.getConfig().putSingle(TrustedHostClientRegistrationPolicyFactory.CLIENT_URIS_MUST_MATCH, "false");
        managedRealm.admin().components().component(policy.getId()).update(policy);

        ClientRepresentation client = createRep("uri-check-disabled");
        client.setAttributes(Map.of(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "https://external.example/logout"));
        ClientRepresentation registered = reg.create(client);
        reg.auth(Auth.token(registered));
        registered.getAttributes().put(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "https://external.example/updated");
        Assertions.assertEquals("https://external.example/updated", reg.update(registered).getAttributes().get(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS));
    }

    @Test
    public void testTrustedHostsPreservesDoubleSlashRedirectPaths() throws Exception {
        setTrustedHost("localhost");
        ClientRepresentation client = createRep("double-slash-paths");
        client.setRootUrl("https://localhost");
        client.setRedirectUris(List.of("//callback/path"));
        client.setAttributes(Map.of(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "//logout/path"));
        ClientRepresentation registered = reg.create(client);
        reg.auth(Auth.token(registered));
        ClientRepresentation updated = reg.update(registered);
        Assertions.assertEquals(List.of("//callback/path"), updated.getRedirectUris());
        Assertions.assertEquals("//logout/path", updated.getAttributes().get(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS));
    }


    private void assertOidcCreateFail(Consumer<OIDCClientRepresentation> uriSetter, String expectedErrorContains) {
        OIDCClientRepresentation client = createRepOidc();
        uriSetter.accept(client);
        assertOidcFail(ClientRegOp.CREATE, client, 403, expectedErrorContains);
    }

    @Test
    public void testTrustedHostsPreservesSpecialValuesAndAllowsAdditionalHosts() throws Exception {
        setTrustedHost("localhost");
        ClientRepresentation client = createRep("special-uri-values");
        client.setWebOrigins(List.of("+", "*", "null", "localhost:8080"));
        client.setAttributes(Map.of(
                OIDCConfigAttributes.LOGO_URI, "data:image/png;base64,aW1hZ2U=",
                OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+##-"));
        ClientRepresentation registered = reg.create(client);
        reg.auth(Auth.token(registered));
        registered = reg.update(registered);
        reg.auth(Auth.token(registered));

        registered.getAttributes().put(OIDCConfigAttributes.LOGO_URI, "https://assets.example.org/logo.png");
        assertFail(ClientRegOp.UPDATE, registered, 403, "URL doesn't match");

        ComponentRepresentation policy = findPolicyByProviderAndAuth(TrustedHostClientRegistrationPolicyFactory.PROVIDER_ID, getPolicyAnon());
        policy.getConfig().put(TrustedHostClientRegistrationPolicyFactory.TRUSTED_HOSTS, List.of("localhost", "assets.example.org"));
        managedRealm.admin().components().component(policy.getId()).update(policy);
        ClientRepresentation updated = reg.update(registered);
        Assertions.assertEquals("https://assets.example.org/logo.png", updated.getAttributes().get(OIDCConfigAttributes.LOGO_URI));
    }

    @Test
    public void testTrustedHostsValidatesSamlAttributesOnPartialUpdate() throws Exception {
        setTrustedHost("localhost");
        ClientRepresentation client = createRep("partial-saml-uris");
        client.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        client.setRedirectUris(List.of("https://localhost/callback"));
        ClientRepresentation registered = reg.create(client);
        reg.auth(Auth.token(registered));

        for (String attribute : List.of(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE,
                SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL)) {
            ClientRepresentation update = new ClientRepresentation();
            update.setClientId(registered.getClientId());
            update.setAttributes(Map.of(attribute, "https://untrusted.host/endpoint"));
            assertFail(ClientRegOp.UPDATE, update, 403, "URL doesn't match");
            Assertions.assertNotEquals("https://untrusted.host/endpoint",
                    managedRealm.admin().clients().get(registered.getId()).toRepresentation().getAttributes().get(attribute));

            update.setAttributes(Map.of(attribute, "https://localhost/endpoint"));
            ClientRepresentation updated = reg.update(update);
            reg.auth(Auth.token(updated));
            Assertions.assertEquals(SamlProtocol.LOGIN_PROTOCOL, updated.getProtocol());
            Assertions.assertEquals("https://localhost/endpoint", updated.getAttributes().get(attribute));
        }
    }

    @Test
    public void testTrustedHostsValidatesMetadataOnCreateAndUpdate() throws Exception {
        setTrustedHost("localhost");
        for (String attribute : List.of(OIDCConfigAttributes.LOGO_URI, OIDCConfigAttributes.POLICY_URI,
                OIDCConfigAttributes.TOS_URI, OIDCConfigAttributes.JWKS_URL, OIDCConfigAttributes.SECTOR_IDENTIFIER_URI,
                CibaConfig.CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT,
                SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL)) {
            reg.auth(null);
            ClientRepresentation client = createRep("uri-" + attribute);
            client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            client.setAttributes(Map.of(attribute, "https://untrusted.host/endpoint"));
            assertFail(ClientRegOp.CREATE, client, 403, "URL doesn't match");
            client.setAttributes(Map.of(attribute, "https://localhost/endpoint"));
            ClientRepresentation registered = reg.create(client);
            reg.auth(Auth.token(registered));
            registered.getAttributes().put(attribute, "https://another.host/updated");
            assertFail(ClientRegOp.UPDATE, registered, 403, "URL doesn't match");
            Assertions.assertEquals("https://localhost/endpoint",
                    managedRealm.admin().clients().get(registered.getId()).toRepresentation().getAttributes().get(attribute));
        }
    }


    @Test
    public void testRedirectUriWithDomain() throws Exception {
        // Change the policy to avoid checking hosts
        ComponentRepresentation trustedHostPolicyRep = findPolicyByProviderAndAuth(TrustedHostClientRegistrationPolicyFactory.PROVIDER_ID, getPolicyAnon());
        trustedHostPolicyRep.getConfig().putSingle(TrustedHostClientRegistrationPolicyFactory.HOST_SENDING_REGISTRATION_REQUEST_MUST_MATCH, "false");

        // Configure some trusted host and domain
        trustedHostPolicyRep.getConfig().put(TrustedHostClientRegistrationPolicyFactory.TRUSTED_HOSTS, Arrays.asList("www.host.com", "*.example.com"));
        managedRealm.admin().components().component(trustedHostPolicyRep.getId()).update(trustedHostPolicyRep);

        // Verify client can be created with the redirectUri from trusted host and domain
        OIDCClientRepresentation oidcClientRep = createRepOidc("http://www.host.com", "http://www.example.com");
        reg.oidc().create(oidcClientRep);

        // Remove domain from the config
        trustedHostPolicyRep.getConfig().put(TrustedHostClientRegistrationPolicyFactory.TRUSTED_HOSTS, Arrays.asList("www.host.com", "www1.example.com"));
        managedRealm.admin().components().component(trustedHostPolicyRep.getId()).update(trustedHostPolicyRep);

        // Check new client can't be created anymore
        oidcClientRep = createRepOidc("http://www.host.com", "http://www.example.com");
        assertOidcFail(ClientRegOp.CREATE, oidcClientRep, 403, "URI doesn't match");
    }




    @Test
    public void testAnonConsentRequired() throws Exception {
        setTrustedHost("localhost");
        OIDCClientRepresentation client = create();

        // Assert new client has consent required
        String clientId = client.getClientId();
        ClientRepresentation clientRep = AdminApiUtil.findClientByClientId(managedRealm.admin(), clientId).toRepresentation();
        Assertions.assertTrue(clientRep.isConsentRequired());

        // Try update with disabled consent required. Should fail
        clientRep.setConsentRequired(false);
        assertFail(ClientRegOp.UPDATE, clientRep, 403, "Not permitted to update consentRequired to false");

        // Try update with enabled consent required. Should pass
        clientRep.setConsentRequired(true);
        reg.update(clientRep);
    }


    @Test
    public void testAnonFullScopeAllowed() throws Exception {
        setTrustedHost("localhost");
        OIDCClientRepresentation client = create();

        // Assert new client has fullScopeAllowed disabled
        String clientId = client.getClientId();
        ClientRepresentation clientRep = AdminApiUtil.findClientByClientId(managedRealm.admin(), clientId).toRepresentation();
        Assertions.assertFalse(clientRep.isFullScopeAllowed());

        // Try update with disabled consent required. Should fail
        clientRep.setFullScopeAllowed(true);
        assertFail(ClientRegOp.UPDATE, clientRep, 403, "Not permitted to enable fullScopeAllowed");

        // Try update with enabled consent required. Should pass
        clientRep.setFullScopeAllowed(false);
        reg.update(clientRep);
    }


    @Test
    public void testClientDisabledPolicy() throws Exception {
        setTrustedHost("localhost");

        // Assert new client is enabled
        OIDCClientRepresentation client = create();
        String clientId = client.getClientId();
        ClientRepresentation clientRep = AdminApiUtil.findClientByClientId(managedRealm.admin(), clientId).toRepresentation();
        Assertions.assertTrue(clientRep.isEnabled());

        // Add client-disabled policy
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName("Clients disabled");
        rep.setParentId(adminClient.realm(REALM_NAME).toRepresentation().getId());
        rep.setProviderId(ClientDisabledClientRegistrationPolicyFactory.PROVIDER_ID);
        rep.setProviderType(ClientRegistrationPolicy.class.getName());
        rep.setSubType(getPolicyAnon());
        Response response = managedRealm.admin().components().add(rep);
        String policyId = ApiUtil.getCreatedId(response);
        response.close();

        // Assert new client is disabled
        client = create();
        clientId = client.getClientId();
        clientRep = AdminApiUtil.findClientByClientId(managedRealm.admin(), clientId).toRepresentation();
        Assertions.assertFalse(clientRep.isEnabled());

        // Try enable client. Should fail
        clientRep.setEnabled(true);
        assertFail(ClientRegOp.UPDATE, clientRep, 401, "Not authorized to update client");

        // Revert
        managedRealm.admin().components().component(policyId).remove();
    }


    @Test
    public void testMaxClientsPolicy() throws Exception {
        setTrustedHost("localhost");

        int clientsCount = managedRealm.admin().clients().findAll().size();
        int newClientsLimit = clientsCount + 1;

        // Allow to create one more client to current limit
        ComponentRepresentation maxClientsPolicyRep = findPolicyByProviderAndAuth(MaxClientsClientRegistrationPolicyFactory.PROVIDER_ID, getPolicyAnon());
        maxClientsPolicyRep.getConfig().putSingle(MaxClientsClientRegistrationPolicyFactory.MAX_CLIENTS, String.valueOf(newClientsLimit));
        managedRealm.admin().components().component(maxClientsPolicyRep.getId()).update(maxClientsPolicyRep);

        // I can register one new client
        OIDCClientRepresentation client = create();

        // I can't register more clients
        assertOidcFail(ClientRegOp.CREATE, createRepOidc(), 403, "It's allowed to have max " + newClientsLimit + " clients per realm");

        // Revert
        maxClientsPolicyRep.getConfig().putSingle(MaxClientsClientRegistrationPolicyFactory.MAX_CLIENTS, String.valueOf(10000));
        managedRealm.admin().components().component(maxClientsPolicyRep.getId()).update(maxClientsPolicyRep);
    }


    @Test
    public void testProviders() throws Exception {
        List<ComponentTypeRepresentation> reps = managedRealm.admin().clientRegistrationPolicy().getProviders();
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
        Assertions.assertTrue(availableMappers.containsAll(someExpectedMappers));

        // test that clientScope provider contains just the default client scopes
        ComponentTypeRepresentation clientScopeRep = providersMap.get(ClientScopesClientRegistrationPolicyFactory.PROVIDER_ID);
        List<String> clientScopes = getProviderConfigProperty(clientScopeRep, ClientScopesClientRegistrationPolicyFactory.ALLOWED_CLIENT_SCOPES);
        Assertions.assertFalse(clientScopes.isEmpty());
        Assertions.assertTrue(clientScopes.contains(OAuth2Constants.SCOPE_PROFILE));
        Assertions.assertTrue(clientScopes.contains(OAuth2Constants.SCOPE_OPENID));
        Assertions.assertFalse(clientScopes.contains("foo"));
        Assertions.assertFalse(clientScopes.contains("bar"));

        // Add some clientScopes
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("foo");
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response response = managedRealm.admin().clientScopes().create(clientScope);
        String fooScopeId = ApiUtil.getCreatedId(response);
        response.close();

        clientScope = new ClientScopeRepresentation();
        clientScope.setName("bar");
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        response = managedRealm.admin().clientScopes().create(clientScope);
        String barScopeId = ApiUtil.getCreatedId(response);
        response.close();

        // send request again and test that clientScope provider contains added client scopes
        reps = managedRealm.admin().clientRegistrationPolicy().getProviders();
        clientScopeRep = reps.stream().filter((ComponentTypeRepresentation rep1) -> {

            return rep1.getId().equals(ClientScopesClientRegistrationPolicyFactory.PROVIDER_ID);

        }).findFirst().get();

        clientScopes = getProviderConfigProperty(clientScopeRep, ClientScopesClientRegistrationPolicyFactory.ALLOWED_CLIENT_SCOPES);
        Assertions.assertTrue(clientScopes.contains("foo"));
        Assertions.assertTrue(clientScopes.contains("bar"));

        // Revert client scopes
        managedRealm.admin().clientScopes().get(fooScopeId).remove();
        managedRealm.admin().clientScopes().get(barScopeId).remove();
    }

    private List<String> getProviderConfigProperty(ComponentTypeRepresentation provider, String expectedConfigPropName) {
        Assertions.assertNotNull(provider);

        List<ConfigPropertyRepresentation> list = provider.getProperties();

        list = list.stream().filter((ConfigPropertyRepresentation rep) -> {

            return rep.getName().equals(expectedConfigPropName);

        }).collect(Collectors.toList());

        Assertions.assertEquals(list.size(), 1);
        ConfigPropertyRepresentation allowedProtocolMappers = list.get(0);

        Assertions.assertEquals(allowedProtocolMappers.getName(), expectedConfigPropName);
        return allowedProtocolMappers.getOptions();
    }


    @Test
    public void testClientScopesPolicy() throws Exception {
        setTrustedHost("localhost");
        String clientId = uniqueClientId("test-app");

        // Add some clientScope through Admin REST
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("foo");
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response response = managedRealm.admin().clientScopes().create(clientScope);
        String clientScopeId = ApiUtil.getCreatedId(response);
        response.close();

        // I can't register new client with this scope
        ClientRepresentation clientRep = createRep(clientId);
        clientRep.setDefaultClientScopes(Collections.singletonList("foo"));
        assertFail(ClientRegOp.CREATE, clientRep, 403, "Not permitted to use specified clientScope");

        // Register client without scope - should success
        ClientRepresentation clientWithoutScopes = createRep(clientId);
        ClientRepresentation registeredClient = reg.create(clientWithoutScopes);
        reg.auth(Auth.token(registeredClient));

        // Try to update client with scope - should fail
        registeredClient.setDefaultClientScopes(Collections.singletonList("foo"));
        assertFail(ClientRegOp.UPDATE, registeredClient, 403, "Not permitted to use specified clientScope");

        // Update client with the clientScope via Admin REST
        ClientResource client = AdminApiUtil.findClientByClientId(managedRealm.admin(), clientId);
        client.addDefaultClientScope(clientScopeId);

        // Now the update via clientRegistration is permitted too as scope was already set
        reg.update(registeredClient);

        // Revert client scope
        managedRealm.admin().clients().get(client.toRepresentation().getId()).remove();
        managedRealm.admin().clientScopes().get(clientScopeId).remove();
    }


    @Test
    public void testClientScopesPolicyWithPermittedScope() throws Exception {
        setTrustedHost("localhost");
        String clientId = uniqueClientId("test-app");

        // Add some clientScope through Admin REST
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("foo");
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response response = managedRealm.admin().clientScopes().create(clientScope);
        String clientScopeId = ApiUtil.getCreatedId(response);
        response.close();

        // I can't register new client with this scope
        ClientRepresentation clientRep = createRep(clientId);
        clientRep.setDefaultClientScopes(Collections.singletonList("foo"));
        assertFail(ClientRegOp.CREATE, clientRep, 403, "Not permitted to use specified clientScope");

        // Update the policy to allow the "foo" scope
        ComponentRepresentation clientScopesPolicyRep = findPolicyByProviderAndAuth(ClientScopesClientRegistrationPolicyFactory.PROVIDER_ID, getPolicyAnon());
        List<String> originalAllowedClientScopes = clientScopesPolicyRep.getConfig().get(ClientScopesClientRegistrationPolicyFactory.ALLOWED_CLIENT_SCOPES);
        List<String> originalAllowedClientScopesCopy = originalAllowedClientScopes == null ? null : new ArrayList<>(originalAllowedClientScopes);

        String registeredClientId = null;
        try {
            clientScopesPolicyRep.getConfig().putSingle(ClientScopesClientRegistrationPolicyFactory.ALLOWED_CLIENT_SCOPES, "foo");
            managedRealm.admin().components().component(clientScopesPolicyRep.getId()).update(clientScopesPolicyRep);

            // Check that I can register client now
            ClientRepresentation registeredClient = reg.create(clientRep);
            Assertions.assertNotNull(registeredClient.getRegistrationAccessToken());
            registeredClientId = registeredClient.getId();
        } finally {
            if (registeredClientId != null) {
                managedRealm.admin().clients().get(registeredClientId).remove();
            }
            if (originalAllowedClientScopesCopy == null) {
                clientScopesPolicyRep.getConfig().remove(ClientScopesClientRegistrationPolicyFactory.ALLOWED_CLIENT_SCOPES);
            } else {
                clientScopesPolicyRep.getConfig().put(ClientScopesClientRegistrationPolicyFactory.ALLOWED_CLIENT_SCOPES, originalAllowedClientScopesCopy);
            }
            managedRealm.admin().components().component(clientScopesPolicyRep.getId()).update(clientScopesPolicyRep);
            managedRealm.admin().clientScopes().get(clientScopeId).remove();
        }
    }


    // PROTOCOL MAPPERS

    @Test
    public void testProtocolMappersCreate() throws Exception {
        setTrustedHost("localhost");
        String clientId = uniqueClientId("test-app");
        String secondClientId = uniqueClientId("test-app-2");

        // Try to add client with some "hardcoded role" mapper. Should fail
        ClientRepresentation clientRep = createRep(clientId);
        clientRep.setProtocolMappers(Collections.singletonList(createHardcodedMapperRep()));
        assertFail(ClientRegOp.CREATE, clientRep, 403, "ProtocolMapper type not allowed");

        // Try the same authenticated. Should still fail.
        ClientInitialAccessPresentation token = adminClient.realm(REALM_NAME).clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));
        assertFail(ClientRegOp.CREATE, clientRep, 403, "ProtocolMapper type not allowed");

        // Update the "authenticated" policy and allow hardcoded role mapper
        ComponentRepresentation protocolMapperPolicyRep = findPolicyByProviderAndAuth(ProtocolMappersClientRegistrationPolicyFactory.PROVIDER_ID, getPolicyAuth());
        protocolMapperPolicyRep.getConfig().add(ProtocolMappersClientRegistrationPolicyFactory.ALLOWED_PROTOCOL_MAPPER_TYPES, HardcodedRole.PROVIDER_ID);
        managedRealm.admin().components().component(protocolMapperPolicyRep.getId()).update(protocolMapperPolicyRep);

        // Check authenticated registration is permitted
        ClientRepresentation registeredClient = reg.create(clientRep);
        String clientUuid = registeredClient.getId();
        managedRealm.cleanup().add(r -> r.clients().get(clientUuid).remove());
        Assertions.assertNotNull(registeredClient.getRegistrationAccessToken());

        // Check "anonymous" registration still fails
        clientRep = createRep(secondClientId);
        clientRep.setProtocolMappers(Collections.singletonList(createHardcodedMapperRep()));
        reg.auth(null);
        assertFail(ClientRegOp.CREATE, clientRep, 403, "ProtocolMapper type not allowed");

        // Revert policy change
        protocolMapperPolicyRep.getConfig().get(ProtocolMappersClientRegistrationPolicyFactory.ALLOWED_PROTOCOL_MAPPER_TYPES).remove(HardcodedRole.PROVIDER_ID);
        managedRealm.admin().components().component(protocolMapperPolicyRep.getId()).update(protocolMapperPolicyRep);
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
    public void testProtocolMappersUpdate() throws Exception {
        setTrustedHost("localhost");
        String clientId = uniqueClientId("test-app");

        // Check I can add client with allowed protocolMappers
        ProtocolMapperRepresentation protocolMapper = new ProtocolMapperRepresentation();
        protocolMapper.setName("Full name");
        protocolMapper.setProtocolMapper(FullNameMapper.PROVIDER_ID);
        protocolMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        ClientRepresentation clientRep = createRep(clientId);
        clientRep.setProtocolMappers(Collections.singletonList(protocolMapper));

        ClientRepresentation registeredClient = reg.create(clientRep);
        String clientUuid = registeredClient.getId();
        managedRealm.cleanup().add(r -> r.clients().get(clientUuid).remove());
        reg.auth(Auth.token(registeredClient));

        // Add some disallowed protocolMapper
        registeredClient.getProtocolMappers().add(createHardcodedMapperRep());

        // Check I can't update client because of protocolMapper
        assertFail(ClientRegOp.UPDATE, registeredClient, 403, "Missing id for mapper named 'Hardcoded foo role'");

        // Remove "bad" protocolMapper
        registeredClient.getProtocolMappers().removeIf((ProtocolMapperRepresentation mapper) -> {
            return mapper.getProtocolMapper().equals(HardcodedRole.PROVIDER_ID);
        });

        // Check I can update client now
        reg.update(registeredClient);
    }

    @Test
    public void passingAnAlreadyAssignedProtocolMapperWithDisallowedTypeDoesNotFail() throws Exception {
        setTrustedHost("localhost");
        String clientId = uniqueClientId("test-app");

        ClientRepresentation clientRep = createRep(clientId);
        // Create the client with a client registration disallowed protocolMapper
        clientRep.setProtocolMappers(List.of(createHardcodedMapperRep()));
        Response adminClientCreationResponse = managedRealm.admin().clients().create(clientRep);
        String clientUuid = ApiUtil.getCreatedId(adminClientCreationResponse);
        managedRealm.cleanup().add(r -> r.clients().get(clientUuid).remove());
        adminClientCreationResponse.close();

        ClientResource clientResource = managedRealm.admin().clients().get(clientUuid);

        reg.auth(Auth.token(clientResource.regenerateRegistrationAccessToken()));
        // Check the client can be updated with a representation keeping the disallowed protocolMapper
        reg.update(clientResource.toRepresentation());
    }

    @Test
    public void passingNewProtocolMapperOfDisallowedTypeInAdditionToAnAlreadyAssignedOneOfTheSameTypeFails() {
        setTrustedHost("localhost");
        String clientId = uniqueClientId("test-app");

        ClientRepresentation clientRep = createRep(clientId);
        // Create the client with a client registration disallowed protocolMapper
        clientRep.setProtocolMappers(List.of(createHardcodedMapperRep()));
        Response adminClientCreationResponse = managedRealm.admin().clients().create(clientRep);
        String clientUuid = ApiUtil.getCreatedId(adminClientCreationResponse);
        managedRealm.cleanup().add(r -> r.clients().get(clientUuid).remove());
        adminClientCreationResponse.close();

        ClientResource clientResource = managedRealm.admin().clients().get(clientUuid);

        reg.auth(Auth.token(clientResource.regenerateRegistrationAccessToken()));

        ClientRepresentation representation = clientResource.toRepresentation();
        representation.getProtocolMappers().add(createHardcodedMapperRep());
        assertFail(ClientRegOp.UPDATE, representation, 403, "Missing id for mapper named 'Hardcoded foo role'");
    }

    @Test
    public void alteringAnAlreadyAssignedProtocolMappersConfigWithDisallowedTypeFails() {
        setTrustedHost("localhost");
        String clientId = uniqueClientId("test-app");

        ClientRepresentation clientRep = createRep(clientId);
        // Create the client with a client registration disallowed protocolMapper
        clientRep.setProtocolMappers(List.of(createHardcodedMapperRep()));
        Response adminClientCreationResponse = managedRealm.admin().clients().create(clientRep);
        String clientUuid = ApiUtil.getCreatedId(adminClientCreationResponse);
        managedRealm.cleanup().add(r -> r.clients().get(clientUuid).remove());
        adminClientCreationResponse.close();

        ClientResource clientResource = managedRealm.admin().clients().get(clientUuid);

        reg.auth(Auth.token(clientResource.regenerateRegistrationAccessToken()));
        // Check the client can be updated with a representation keeping the disallowed protocolMapper
        ClientRepresentation representation = clientResource.toRepresentation();
        representation.getProtocolMappers().forEach(mapper -> mapper.getConfig().put("foo", "bar"));
        assertFail(ClientRegOp.UPDATE, representation, 403, "ProtocolMapper type not allowed");
    }

    @Test
    public void alteringAnAlreadyAssignedProtocolMappersIdWithDisallowedTypeFails() {
        setTrustedHost("localhost");
        String clientId = uniqueClientId("test-app");

        ClientRepresentation clientRep = createRep(clientId);
        // Create the client with a client registration disallowed protocolMapper
        clientRep.setProtocolMappers(List.of(createHardcodedMapperRep()));
        Response adminClientCreationResponse = managedRealm.admin().clients().create(clientRep);
        String clientUuid = ApiUtil.getCreatedId(adminClientCreationResponse);
        managedRealm.cleanup().add(r -> r.clients().get(clientUuid).remove());
        adminClientCreationResponse.close();

        ClientResource clientResource = managedRealm.admin().clients().get(clientUuid);

        reg.auth(Auth.token(clientResource.regenerateRegistrationAccessToken()));
        ClientRepresentation representation = clientResource.toRepresentation();
        representation.getProtocolMappers().forEach(mapper -> mapper.setId(UUID.randomUUID().toString()));
        assertFail(ClientRegOp.UPDATE, representation, 403, "No existing mapper model found for id");
    }

    @Test
    public void testProtocolMappersConsentRequired() throws Exception {
        setTrustedHost("localhost");
        String clientId = uniqueClientId("test-app");

        // Register client and assert it doesn't have builtin protocol mappers
        ClientRepresentation clientRep = createRep(clientId);
        ClientRepresentation registeredClient = reg.create(clientRep);
        String clientUuid = registeredClient.getId();
        managedRealm.cleanup().add(r -> r.clients().get(clientUuid).remove());

        Assertions.assertNull(registeredClient.getProtocolMappers());
    }


    @Test
    public void testProtocolMappersRemoveBuiltins() throws Exception {
        setTrustedHost("localhost");
        String clientId = uniqueClientId("test-app");

        // Change policy to allow hardcoded mapper

        ComponentRepresentation protocolMapperPolicyRep = findPolicyByProviderAndAuth(ProtocolMappersClientRegistrationPolicyFactory.PROVIDER_ID, getPolicyAnon());
        protocolMapperPolicyRep.getConfig().add(ProtocolMappersClientRegistrationPolicyFactory.ALLOWED_PROTOCOL_MAPPER_TYPES, HardcodedRole.PROVIDER_ID);
        managedRealm.admin().components().component(protocolMapperPolicyRep.getId()).update(protocolMapperPolicyRep);

        // Create client with hardcoded mapper
        ClientRepresentation clientRep = createRep(clientId);
        clientRep.setProtocolMappers(Collections.singletonList(createHardcodedMapperRep()));
        ClientRepresentation registeredClient = reg.create(clientRep);
        String clientUuid = registeredClient.getId();
        managedRealm.cleanup().add(r -> r.clients().get(clientUuid).remove());

        Assertions.assertEquals(1, registeredClient.getProtocolMappers().size());
        ProtocolMapperRepresentation hardcodedMapper = registeredClient.getProtocolMappers().get(0);

        // Revert
        protocolMapperPolicyRep.getConfig().get(ProtocolMappersClientRegistrationPolicyFactory.ALLOWED_PROTOCOL_MAPPER_TYPES).remove(HardcodedRole.PROVIDER_ID);
        managedRealm.admin().components().component(protocolMapperPolicyRep.getId()).update(protocolMapperPolicyRep);
    }

    @Test
    public void testRegisterFakeClientScope() {
        //create
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName("Clients scopes not allowed");
        rep.setParentId(adminClient.realm(REALM_NAME).toRepresentation().getId());
        rep.setProviderId(ClientScopesClientRegistrationPolicyFactory.PROVIDER_ID);
        rep.setProviderType(ClientRegistrationPolicy.class.getName());
        rep.setSubType(getPolicyAnon());
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle(ClientScopesClientRegistrationPolicyFactory.ALLOWED_CLIENT_SCOPES, "foo1");
        Response response = managedRealm.admin().components().add(rep);
        ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
        Assertions.assertEquals("Client scopes not allowed: [foo1]", error.getErrorMessage());

        //update
        ComponentRepresentation clientScopesPolicyRep = findPolicyByProviderAndAuth(ClientScopesClientRegistrationPolicyFactory.PROVIDER_ID, getPolicyAnon());
        clientScopesPolicyRep.getConfig().putSingle(ClientScopesClientRegistrationPolicyFactory.ALLOWED_CLIENT_SCOPES, "foo2");
        BadRequestException e = Assertions.assertThrows(BadRequestException.class,
                () -> managedRealm.admin().components().component(clientScopesPolicyRep.getId()).update(clientScopesPolicyRep));
        error = e.getResponse().readEntity(ErrorRepresentation.class);
        Assertions.assertEquals("Client scopes not allowed: [foo2]", error.getErrorMessage());
    }

    @Test
    public void testMapperTypeSwap() {
        setTrustedHost("localhost");
        String clientId = uniqueClientId("swap-test-client");

        ProtocolMapperRepresentation allowedMapper = new ProtocolMapperRepresentation();
        allowedMapper.setName("swap-mapper");
        allowedMapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);
        allowedMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        allowedMapper.getConfig().put(HardcodedRole.ROLE_CONFIG, "realm-management.manage-users");
        allowedMapper.getConfig().put("user.attribute", "attr");
        allowedMapper.getConfig().put("claim.name", "claim");

        ClientRepresentation clientRep = createRep(clientId);
        clientRep.setProtocolMappers(List.of(allowedMapper));
        Response adminClientCreationResponse = managedRealm.admin().clients().create(clientRep);
        String clientTechnicalId = ApiUtil.getCreatedId(adminClientCreationResponse);
        adminClientCreationResponse.close();

        try {
            ClientResource clientResource = managedRealm.admin().clients().get(clientTechnicalId);
            reg.auth(Auth.token(clientResource.regenerateRegistrationAccessToken()));
            ClientRepresentation representation = clientResource.toRepresentation();

            // Swap the mapper type from allowed
            representation.getProtocolMappers().forEach(mapper -> {
                if ("swap-mapper".equals(mapper.getName())) {
                    mapper.setProtocolMapper(HardcodedRole.PROVIDER_ID);
                }
            });

            // The type-swap MUST be rejected with 403
            assertFail(ClientRegOp.UPDATE, representation, 403, "ProtocolMapper type not allowed");
        } finally {
            managedRealm.admin().clients().get(clientTechnicalId).remove();
        }
    }

    // HELPER METHODS

    private String uniqueClientId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private String getPolicyAnon() {
        return ClientRegistrationPolicyManager.getComponentTypeKey(RegistrationAuth.ANONYMOUS);
    }

    private String getPolicyAuth() {
        return ClientRegistrationPolicyManager.getComponentTypeKey(RegistrationAuth.AUTHENTICATED);
    }

    private ComponentRepresentation findPolicyByProviderAndAuth(String providerId, String authType) {
        // Change the policy to avoid checking hosts
        String parentId = managedRealm.admin().toRepresentation().getId();
        List<ComponentRepresentation> reps = managedRealm.admin().components().query(parentId, ClientRegistrationPolicy.class.getName());
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
        managedRealm.admin().components().component(trustedHostRep.getId()).update(trustedHostRep);
    }

    private void assertRegAccessToken(String registrationAccessToken, RegistrationAuth expectedRegAuth) throws Exception {
        byte[] content = new JWSInput(registrationAccessToken).getContent();
        RegistrationAccessToken regAccessToken = JsonSerialization.readValue(content, RegistrationAccessToken.class);
        Assertions.assertEquals(regAccessToken.getRegistrationAuth(), expectedRegAuth.toString().toLowerCase());
    }

    private enum ClientRegOp {
        CREATE, READ, UPDATE, DELETE
    }

}
