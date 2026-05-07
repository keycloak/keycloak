package org.keycloak.tests.client;

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



import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.events.Errors;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.util.JsonSerialization;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;

import static org.keycloak.services.clientregistration.ErrorCodes.INVALID_CLIENT_METADATA;
import static org.keycloak.services.clientregistration.ErrorCodes.INVALID_REDIRECT_URI;
import static org.keycloak.utils.MediaType.APPLICATION_JSON;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class ClientRegistrationTest extends AbstractClientRegistrationTest {


    @InjectRealm(config = ClientRegistrationTest.ClientRegistrationRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectRealm(ref = "master", attachTo = "master", lifecycle = LifeCycle.METHOD)
    ManagedRealm masterRealm;

    @Test
    @DatabaseTest
    public void registerClientAsAdmin() throws ClientRegistrationException{
        ClientRepresentation createdClient = buildAndVerifyManageClients(authManageClients(oauth.clientRegistration()));

        ClientRepresentation adminClient = realm.admin()
                .clients()
                .get(createdClient.getId())
                .toRepresentation();
        assertEquals(CLIENT_ID, adminClient.getClientId());
    }

    @Test
    public void registerClientAsAdminWithCreateOnly() throws ClientRegistrationException {
        ClientRepresentation createdClient = buildAndVerifyManageClients(authManageClients(oauth.clientRegistration()));
        assertEquals(CLIENT_ID, createdClient.getClientId());
    }

    // KEYCLOAK-5907
    @Test
    public void withServiceAccount() throws ClientRegistrationException {
        ClientRegistration registration = oauth.clientRegistration();
        authManageClients(registration);
        ClientRepresentation clientRep = getClientRepresentationForTestClient();
        clientRep.setServiceAccountsEnabled(true);

        ClientRepresentation rep = registration.create(clientRep);
        assertNotNull(rep);
        assertEquals(CLIENT_ID, rep.getClientId());

        UserRepresentation serviceAccountUser = realm.admin().clients().get(rep.getId()).getServiceAccountUser();

        assertNotNull(serviceAccountUser);

        realm.admin().clients().delete(rep.getId());

        try {
            realm.admin().users().get(serviceAccountUser.getId()).toRepresentation();
            fail("Expected NotFoundException");
        } catch (NotFoundException ignored) {
        }
    }

    @Test
    @DatabaseTest
    public void updateServiceAccount() throws Exception {
        ClientRegistration clientRegistration = oauth.clientRegistration();
        final ClientRepresentation createdClient = buildAndVerifyManageClients(authManageClients(clientRegistration));

        ClientRepresentation client = realm.admin().clients().get(createdClient.getId()).toRepresentation();
        assertFalse(client.isServiceAccountsEnabled());
        assertTrue(realm.admin().users().search(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + client.getClientId(), true).isEmpty());
        client.setServiceAccountsEnabled(true);
        client = clientRegistration.update(client);
        assertTrue(client.isServiceAccountsEnabled());
        assertFalse(realm.admin().users().search(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + client.getClientId(), true).isEmpty());

        client.setServiceAccountsEnabled(false);
        client = clientRegistration.update(client);
        assertFalse(client.isServiceAccountsEnabled());
        assertTrue(realm.admin().users().search(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + client.getClientId(), true).isEmpty());
    }

    @Test
    @DatabaseTest
    public void registerClientInMasterRealm() throws Exception {
        ClientRegistration masterReg = ClientRegistration.create().url(keycloakUrls.getBase(), "master").build();

        String token = oauth.realm("master").client(Constants.ADMIN_CLI_CLIENT_ID).doPasswordGrantRequest( "admin", "admin").getAccessToken();
        masterReg.auth(Auth.token(token));

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);

        ClientRepresentation createdClient = masterReg.create(client);
        assertNotNull(createdClient);
    }

    @Test
    public void registerClientWithoutProtocol() throws ClientRegistrationException {
        ClientRepresentation clientRepresentation = buildAndVerifyManageClients(authCreateClients(oauth.clientRegistration()));

        assertEquals(CLIENT_ID, clientRepresentation.getClientId());
        assertEquals("openid-connect", clientRepresentation.getProtocol());

    }

    /**
     * OID4VC protocol is not valid for clients. It can only be used for ClientScopes.
     * Attempting to create a client with protocol "oid4vc" should be rejected.
     */
    @Test
    public void registerOid4vcClientShouldBeRejected() {
        authManageClients(oauth.clientRegistration());

        ClientRepresentation client = getClientRepresentationForTestClient();
        client.setProtocol("oid4vc");

        try(Response response = realm.admin().clients().create(client)) {
            Assertions.assertEquals(400, response.getStatus() , "Creating a client with OID4VC protocol should be rejected as it is not a valid protocol for clients.");
        }
    }

    @Test
    public void registerClientAsAdminWithNoAccess() {
        try {
            buildAndVerifyManageClients(authNoAccess(oauth.clientRegistration()));
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    @DatabaseTest
    public void registerClientUsingRevokedToken() throws Exception {
        ClientRegistration reg = oauth.clientRegistration();
        reg.auth(Auth.token(getToken("manage-clients", "password")));

        ClientRepresentation myclient = new ClientRepresentation();

        myclient.setClientId("myclient");
        myclient.setServiceAccountsEnabled(true);
        myclient.setSecret("password");
        myclient.setDirectAccessGrantsEnabled(true);

        reg.create(myclient);

        oauth.client("myclient");
        String bearerToken = getToken("myclient", "password", "manage-clients", "password");
        assertTrue(oauth.tokenRevocationRequest(bearerToken).accessToken().send().isSuccess());

        try {
            reg.auth(Auth.token(bearerToken));

            ClientRepresentation clientRep = getClientRepresentationForTestClient();
            clientRep.setServiceAccountsEnabled(true);

            reg.create(clientRep);
        } catch (ClientRegistrationException cre) {
            HttpErrorException cause = (HttpErrorException) cre.getCause();
            assertEquals(401, cause.getStatusLine().getStatusCode());
            OAuth2ErrorRepresentation error = cause.toErrorRepresentation();
            assertEquals(Errors.INVALID_TOKEN, error.getError());
            assertEquals("Failed decode token", error.getErrorDescription());
        }
    }

    @Test
    @DatabaseTest
    public void registerClientWithNonAsciiChars() throws ClientRegistrationException {
        ClientRegistration registration =  oauth.clientRegistration();
        authCreateClients(registration);
        ClientRepresentation client = getClientRepresentationForTestClient();
        String name = "Cli\u00EBnt";
        client.setName(name);

        ClientRepresentation createdClient = registration.create(client);
        assertEquals(name, createdClient.getName());

        authManageClients(registration);
    }

    @Test
    @DatabaseTest
    public void clientWithDefaultRoles() throws ClientRegistrationException {
        ClientRegistration registration = oauth.clientRegistration();
        authCreateClients(registration);
        ClientRepresentation client = getClientRepresentationForTestClient();
        client.setDefaultRoles(new String[]{"test-default-role"});

        ClientRepresentation createdClient = registration.create(client);
        assertArrayEquals(new String[]{"test-default-role"}, createdClient.getDefaultRoles());

        authManageClients(registration);
        ClientRepresentation obtainedClient = registration.get(CLIENT_ID);
        assertArrayEquals(new String[]{"test-default-role"}, obtainedClient.getDefaultRoles());

        client.setDefaultRoles(new String[]{"test-default-role1","test-default-role2"});
        ClientRepresentation updatedClient = registration.update(client);
        assertThat(Arrays.asList(updatedClient.getDefaultRoles()), containsInAnyOrder("test-default-role1", "test-default-role2"));
    }

    @Test
    @DatabaseTest
    public void updateClientScopes() throws ClientRegistrationException {
        ClientRegistration registration = oauth.clientRegistration();
        authManageClients(registration);
        ClientRepresentation client = getClientRepresentationForTestClient();
        ArrayList<String> optionalClientScopes = new ArrayList<>(List.of("address"));
        client.setOptionalClientScopes(optionalClientScopes);
        ClientRepresentation createdClient = registration.create(client);
        Set<String> requestedClientScopes = new HashSet<>(optionalClientScopes);
        Set<String> registeredClientScopes = new HashSet<>(createdClient.getOptionalClientScopes());
        assertEquals(requestedClientScopes, registeredClientScopes);
        assertTrue(CollectionUtil.collectionEquals(createdClient.getDefaultClientScopes(), Set.of("basic")));

        authManageClients(registration);
        ClientRepresentation obtainedClient = registration.get(CLIENT_ID);
        registeredClientScopes = new HashSet<>(obtainedClient.getOptionalClientScopes());
        assertEquals(requestedClientScopes, registeredClientScopes);
        assertTrue(CollectionUtil.collectionEquals(obtainedClient.getDefaultClientScopes(), Set.of("basic")));


        optionalClientScopes = new ArrayList<>(List.of("address", "phone"));
        obtainedClient.setOptionalClientScopes(optionalClientScopes);
        ClientRepresentation updatedClient = registration.update(obtainedClient);
        requestedClientScopes = new HashSet<>(optionalClientScopes);
        registeredClientScopes = new HashSet<>(updatedClient.getOptionalClientScopes());
        assertEquals(requestedClientScopes, registeredClientScopes);
        assertTrue(CollectionUtil.collectionEquals(updatedClient.getDefaultClientScopes(), Set.of("basic")));
    }

    @Test
    public void testInvalidUrlClientValidation() {
        testClientUriValidation("Root URL is not a valid URL",
                "Base URL is not a valid URL",
                "Backchannel logout URL is not a valid URL",
                null,
                "invalid", "myapp://some-fake-app");
    }

    @Test
    public void testIllegalSchemeClientValidation() {
        testClientUriValidation("Root URL uses an illegal scheme",
                "Base URL uses an illegal scheme",
                "Backchannel logout URL uses an illegal scheme",
                "A redirect URI uses an illegal scheme",
                "data:text/html;base64,PHNjcmlwdD5jb25maXJtKGRvY3VtZW50LmRvbWFpbik7PC9zY3JpcHQ+",
                "javascript:confirm(document.domain)/*"
        );
    }

    // KEYCLOAK-3421
    @Test
    public void testFragmentProhibitedClientValidation() {
        testClientUriValidation("Root URL must not contain an URL fragment",
                null,
                null,
                "Redirect URIs must not contain an URI fragment",
                "http://redhat.com/abcd#someFragment"
        );
    }

    @Test
    public void testSamlSpecificUrls() throws ClientRegistrationException {
        testSamlSpecificUrls(true, "javascript:alert('TEST')", "data:text/html;base64,PHNjcmlwdD5jb25maXJtKGRvY3VtZW50LmRvbWFpbik7PC9zY3JpcHQ+");
        testSamlSpecificUrls(false, "javascript:alert('TEST')", "data:text/html;base64,PHNjcmlwdD5jb25maXJtKGRvY3VtZW50LmRvbWFpbik7PC9zY3JpcHQ+");
    }

    @Test
    @DatabaseTest
    public void testUpdateAuthorizationSettings() throws ClientRegistrationException {
        ClientRegistration clientRegistration =  oauth.clientRegistration();
        authManageClients(clientRegistration);
        ClientRepresentation clientRep = getClientRepresentationForTestClient();
        clientRep.setAuthorizationServicesEnabled(true);

        ClientRepresentation rep = clientRegistration.create(clientRep);
        rep = realm.admin().clients().get(rep.getId()).toRepresentation();

        assertTrue(rep.getAuthorizationServicesEnabled());

        ResourceServerRepresentation authzSettings = new ResourceServerRepresentation();

        authzSettings.setAllowRemoteResourceManagement(false);
        authzSettings.setResources(List.of(new ResourceRepresentation("foo", "scope-a", "scope-b")));

        PolicyRepresentation permission = new PolicyRepresentation();

        permission.setName(KeycloakModelUtils.generateId());
        permission.setType("resource");
        permission.setResources(Collections.singleton("foo"));

        authzSettings.setPolicies(List.of(permission));

        rep.setAuthorizationSettings(authzSettings);

        clientRegistration.update(rep);
        authzSettings = realm.admin().clients().get(rep.getId()).authorization().exportSettings();

        assertFalse(authzSettings.getResources().isEmpty());
        assertFalse(authzSettings.getScopes().isEmpty());
        assertFalse(authzSettings.getPolicies().isEmpty());
    }

    @Test
    public void getClientAsAdmin() throws ClientRegistrationException {
        ClientRegistration registration = oauth.clientRegistration();
        setupClientAsAdmin(registration);
        ClientRepresentation rep = registration.get(CLIENT_ID);
        assertNotNull(rep);
    }

    @Test
    public void getClientAsAdminWithCreateOnly() throws ClientRegistrationException {
        ClientRegistration registration = oauth.clientRegistration();
        setupClientAsAdmin(registration);
        authCreateClients(registration);
        try {
            registration.get(CLIENT_ID);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void getClientAsAdminWithNoAccess() throws ClientRegistrationException {
        ClientRegistration registration = oauth.clientRegistration();
        setupClientAsAdmin(registration);
        authNoAccess(registration);
        try {
            registration.get(CLIENT_ID);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void getClientNotFound() throws ClientRegistrationException {
        ClientRegistration clientRegistration = oauth.clientRegistration();
        authManageClients(clientRegistration);
        Assertions.assertNull(clientRegistration.get("invalid"));
    }

    @Test
    public void getClientNotFoundNoAccess() {
        ClientRegistration registration = oauth.clientRegistration();
        authNoAccess(registration);
        try {
            registration.get("invalid");
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    @DatabaseTest
    public void updateClientAsAdmin() throws ClientRegistrationException {
        ClientRegistration registration = oauth.clientRegistration();
        setupClientAsAdmin(registration);
        authManageClients(registration);
        updateClient(registration);
    }

    @Test
    @DatabaseTest
    public void updateClientSecret() throws ClientRegistrationException {
        ClientRegistration clientRegistration = oauth.clientRegistration();
        ClientRepresentation client = buildAndVerifyManageClients(authManageClients(clientRegistration));

        assertNotNull(client.getSecret());

        client.setSecret("mysecret");
        clientRegistration.update(client);

        ClientRepresentation updatedClient = clientRegistration.get(CLIENT_ID);

        assertEquals("mysecret", updatedClient.getSecret());
    }

    @Test
    @DatabaseTest
    public void addClientProtcolMappers() throws ClientRegistrationException {
        ClientRegistration clientRegistration = oauth.clientRegistration();
        buildAndVerifyManageClients(authManageClients(clientRegistration));

        ClientRepresentation client = clientRegistration.get(CLIENT_ID);

        addProtocolMapper(client, "mapperA");
        clientRegistration.update(client);

        ClientRepresentation updatedClient = clientRegistration.get(CLIENT_ID);
        assertThat("Adding protocolMapper failed", updatedClient.getProtocolMappers().size(), is(1));
    }

    @Test
    @DatabaseTest
    public void removeClientProtocolMappers() throws ClientRegistrationException {
        ClientRegistration clientRegistration = oauth.clientRegistration();
        authManageClients(clientRegistration);

        ClientRepresentation initialClient = getClientRepresentationForTestClient();
        addProtocolMapper(initialClient, "mapperA");
        clientRegistration.create(initialClient);
        ClientRepresentation client = clientRegistration.get(CLIENT_ID);
        client.setProtocolMappers(new ArrayList<>());
        clientRegistration.update(client);

        ClientRepresentation updatedClient = clientRegistration.get(CLIENT_ID);
        assertThat("Removing protocolMapper failed", updatedClient.getProtocolMappers(), nullValue());
    }

    @Test
    @DatabaseTest
    public void updateClientProtocolMappers() throws ClientRegistrationException {
        ClientRegistration clientRegistration = oauth.clientRegistration();
        authManageClients(clientRegistration);

        ClientRepresentation initialClient = getClientRepresentationForTestClient();
        addProtocolMapper(initialClient, "mapperA");
        clientRegistration.create(initialClient);
        ClientRepresentation client = clientRegistration.get(CLIENT_ID);
        client.getProtocolMappers().get(0).getConfig().put("claim.name", "updatedClaimName");
        clientRegistration.update(client);

        ClientRepresentation updatedClient = clientRegistration.get(CLIENT_ID);
        assertThat("Updating protocolMapper failed", updatedClient.getProtocolMappers().get(0).getConfig().get("claim.name"), is("updatedClaimName"));
    }

    @Test
    public void updateClientAsAdminWithCreateOnly() {
        ClientRegistration clientRegistration = oauth.clientRegistration();
        authCreateClients(clientRegistration);
        try {
            updateClient(clientRegistration);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void updateClientAsAdminWithNoAccess() {
        ClientRegistration clientRegistration = oauth.clientRegistration();
        authNoAccess(clientRegistration);
        try {
            updateClient(clientRegistration);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    @DatabaseTest
    public void updateClientNotFound() {
        ClientRegistration clientRegistration = oauth.clientRegistration();
        authManageClients(clientRegistration);
        try {
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId("invalid");

            clientRegistration.update(client);

            fail("Expected 404");
        } catch (ClientRegistrationException e) {
            assertEquals(404, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    @DatabaseTest
    public void updateClientWithNonAsciiChars() throws ClientRegistrationException {
        ClientRegistration clientRegistration = oauth.clientRegistration();
        authCreateClients(clientRegistration);
        clientRegistration.create(getClientRepresentationForTestClient());

        authManageClients(clientRegistration);
        ClientRepresentation client = clientRegistration.get(CLIENT_ID);
        String name = "Cli\u00EBnt";
        client.setName(name);

        ClientRepresentation updatedClient = clientRegistration.update(client);
        assertEquals(name, updatedClient.getName());
    }

    @Test
    @DatabaseTest
    public void deleteClientAsAdmin() throws ClientRegistrationException {
        ClientRegistration clientRegistration = oauth.clientRegistration();
        authCreateClients(clientRegistration);
        clientRegistration.create(getClientRepresentationForTestClient());

        authManageClients(clientRegistration);
    }

    @Test
    public void deleteClientAsAdminWithCreateOnly() throws ClientRegistrationException {
        ClientRegistration clientRegistration = oauth.clientRegistration();
        authManageClients(clientRegistration);
        ClientRepresentation client = clientRegistration.create(getClientRepresentationForTestClient());
        try {
            authCreateClients(clientRegistration);
            clientRegistration.delete(CLIENT_ID);
            realm.admin().clients().get(client.getId()).toRepresentation();
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void deleteClientAsAdminWithNoAccess() throws ClientRegistrationException {
        ClientRegistration clientRegistration = oauth.clientRegistration();
        authManageClients(clientRegistration);
        ClientRepresentation client = clientRegistration.create(getClientRepresentationForTestClient());
        try {
            authNoAccess(clientRegistration);
            clientRegistration.delete(CLIENT_ID);
            realm.admin().clients().get(client.getId()).toRepresentation();
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    @DatabaseTest
    public void registerClientAsAdminWithScope() throws ClientRegistrationException {
        ClientRegistration clientRegistration = oauth.clientRegistration();
        authManageClients(clientRegistration);
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);
        ArrayList<String> optionalClientScopes = new ArrayList<>(Arrays.asList("address","phone"));
        client.setOptionalClientScopes(optionalClientScopes);

        ClientRepresentation createdClient = clientRegistration.create(client);
        assertEquals(CLIENT_ID, createdClient.getClientId());
        client = realm.admin().clients().get(createdClient.getId()).toRepresentation();
        assertEquals(CLIENT_ID, client.getClientId());

        Set<String> requestedClientScopes = new HashSet<>(optionalClientScopes);
        Set<String> registeredClientScopes = new HashSet<>(client.getOptionalClientScopes());
        assertEquals(requestedClientScopes, registeredClientScopes);
        assertTrue(CollectionUtil.collectionEquals(client.getDefaultClientScopes(), Set.of("basic")));
    }

    @Test
    @DatabaseTest
    public void registerClientAsAdminWithoutScope() throws ClientRegistrationException {
        ClientRegistration clientRegistration = oauth.clientRegistration();
        Set<String> realmDefaultClientScopes = realm.admin().getDefaultDefaultClientScopes().stream()
                .filter(scope -> Objects.equals(scope.getProtocol(), OIDCLoginProtocol.LOGIN_PROTOCOL))
                .map(ClientScopeRepresentation::getName).collect(Collectors.toSet());
        Set<String> realmOptionalClientScopes = realm.admin().getDefaultOptionalClientScopes().stream()
                .filter(scope -> Objects.equals(scope.getProtocol(), OIDCLoginProtocol.LOGIN_PROTOCOL))
                .map(ClientScopeRepresentation::getName).collect(Collectors.toSet());

        authManageClients(clientRegistration);
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);

        ClientRepresentation createdClient = clientRegistration.create(client);
        assertEquals(CLIENT_ID, createdClient.getClientId());
        client = realm.admin().clients().get(createdClient.getId()).toRepresentation();
        assertEquals(CLIENT_ID, client.getClientId());

        assertEquals(realmDefaultClientScopes, new HashSet<>(client.getDefaultClientScopes()));
        assertEquals(realmOptionalClientScopes, new HashSet<>(client.getOptionalClientScopes()));
    }

    @Test
    public void registerClientAsAdminWithNotDefinedScope() {
        ClientRegistration clientRegistration = oauth.clientRegistration();
        authManageClients(clientRegistration);
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);
        client.setOptionalClientScopes(new ArrayList<>(Arrays.asList("notdefinedscope","phone")));
        try {
            clientRegistration.create(client);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void registerClientWithWrongCharacters() throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(keycloakUrls.getBaseBuilder().path("/realms/master/clients-registrations/openid-connect").build());
            post.setEntity(new StringEntity("{\"<img src=alert(1)>\":1}"));
            post.setHeader("Content-Type", APPLICATION_JSON);

            try (CloseableHttpResponse response = client.execute(post)) {
                assertThat(response.getStatusLine().getStatusCode(),is(400));

                Header header = response.getFirstHeader("Content-Type");
                assertThat(header, notNullValue());

                // Verify the Content-Type is not text/html
                assertThat(Arrays.stream(header.getElements())
                        .map(HeaderElement::getName)
                        .filter(Objects::nonNull)
                        .anyMatch(f -> f.equals(APPLICATION_JSON)), is(true));

                // The alert is not executed
                assertThat(EntityUtils.toString(response.getEntity()), CoreMatchers.containsString("Unrecognized field \\\"<img src=alert(1)>\\\""));
            }
        }
    }

    @Test
    @DatabaseTest
    public void registerMultipleClients() {

        int concurrentThreads = 5;
        int iterations = 10;
        int initialTokenCounts = 2;

        ClientInitialAccessCreatePresentation clientInitialAccessCreatePresentation = new ClientInitialAccessCreatePresentation();
        clientInitialAccessCreatePresentation.setCount(initialTokenCounts);
        clientInitialAccessCreatePresentation.setExpiration(10000);
        ClientInitialAccessPresentation response = realm.admin().clientInitialAccess().create(clientInitialAccessCreatePresentation);

        ExecutorService threadPool = Executors.newFixedThreadPool(concurrentThreads);
        AtomicInteger createdCount = new AtomicInteger();
        try {
            Collection<Callable<Void>> futures = new LinkedList<>();
            for (int i = 0; i < iterations; i ++) {
                final int j = i;

                Callable<Void> f = () -> {
                    ClientRegistration client = oauth.clientRegistration().create().url(keycloakUrls.getBase(), REALM_NAME).build();
                    client.auth(Auth.token(response));
                    ClientRepresentation rep = new ClientRepresentation();
                    rep.setClientId("test-" + j);
                    rep = client.create(rep);
                    if(rep.getId() != null && rep.getClientId().equals("test-" + j)) {
                        createdCount.getAndIncrement();
                    }
                    return null;
                };
                futures.add(f);
            }
            threadPool.invokeAll(futures);

        } catch (Exception ex) {
            fail(ex.getMessage());
        }

        //controls the number of uses of the initial access token
        assertEquals(initialTokenCounts, createdCount.get());
    }

    @Test
    @DatabaseTest
    public void registerWithLightweightAccessTokenAndTransientSession() throws Exception {

        String TEST_ADMIN_CLIENT = "test-admin-client";
        ClientRepresentation testAdminClient = new ClientRepresentation();
        testAdminClient.setClientId(TEST_ADMIN_CLIENT);
        testAdminClient.setSecret(TEST_ADMIN_CLIENT);
        testAdminClient.setServiceAccountsEnabled(true);
        testAdminClient.setAttributes(new HashMap<>());
        testAdminClient.getAttributes().put(Constants.USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED, Boolean.TRUE.toString());

        Response response = masterRealm.admin().clients().create(testAdminClient);
        testAdminClient = masterRealm.admin().clients().get(ApiUtil.getCreatedId(response)).toRepresentation();

        UserRepresentation serviceAccountUser = masterRealm.admin().clients().get(testAdminClient.getId()).getServiceAccountUser();
        RoleRepresentation adminRole = masterRealm.admin().roles().get("admin").toRepresentation();
        masterRealm.admin().users().get(serviceAccountUser.getId()).roles().realmLevel().add(List.of(adminRole));

        oauth.client(TEST_ADMIN_CLIENT, TEST_ADMIN_CLIENT);
        oauth.realm("master");
        String token = oauth.doClientCredentialsGrantAccessTokenRequest().getAccessToken();
        ClientRegistration masterReg = ClientRegistration.create().url(keycloakUrls.getBase(), "master").build();
        masterReg.auth(Auth.token(token));

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("test-lightweight-client");
        client.setSecret(CLIENT_SECRET);
        ClientRepresentation createdClient = masterReg.create(client);
        assertNotNull(createdClient);

        masterRealm.admin().clients().get(testAdminClient.getId()).remove();
        masterRealm.admin().clients().get(createdClient.getId()).remove();
    }
    // ========== Realm Configuration ==========

    public static class ClientRegistrationRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name(REALM_NAME)
                    .clients(ClientBuilder.create("myclient-test")
                            .publicClient(true)
                            .directAccessGrantsEnabled(true));

            UserBuilder manage_client_user_builder =  UserBuilder.create()
                    .username("manage-clients")
                    .name("manage", "clients")
                    .enabled(true)
                    .password("password")
                    .email("manage-clients@test.com")
                    .emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_CLIENTS);

            UserBuilder create_client_user_builder =  UserBuilder.create()
                    .username("create-clients")
                    .name("create", "clients")
                    .enabled(true)
                    .password("password")
                    .email("create-clients@test.com")
                    .emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.CREATE_CLIENT);

            UserBuilder no_access_user_builder =   UserBuilder.create()
                    .username("no-access")
                    .name("no", "access")
                    .enabled(true)
                    .password("password")
                    .email("no-access@test.com")
                    .emailVerified(true);
            realm.users(manage_client_user_builder, create_client_user_builder, no_access_user_builder);

            return realm;
        }
    }

    // ========== Helper Methods ==========

    private ClientRepresentation getClientRepresentationForTestClient() {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);
        return client;
    }

    private void testClientUriValidation(String expectedRootUrlError, String expectedBaseUrlError, String expectedBackchannelLogoutUrlError, String expectedRedirectUrisError, String... testUrls) {
        testClientUriValidation(true, expectedRootUrlError, expectedBaseUrlError, expectedBackchannelLogoutUrlError, expectedRedirectUrisError, testUrls);
        testClientUriValidation(false, expectedRootUrlError, expectedBaseUrlError, expectedBackchannelLogoutUrlError, expectedRedirectUrisError, testUrls);
    }

    private void testClientUriValidation(boolean register, String expectedRootUrlError, String expectedBaseUrlError, String expectedBackchannelLogoutUrlError, String expectedRedirectUrisError, String... testUrls) {
        ClientRepresentation rep;
        ClientRegistration registration = oauth.clientRegistration();
        if (register) {
            authCreateClients(registration);
            rep = getClientRepresentationForTestClient();
        }
        else {
            try {
                setupClientAsAdmin(registration);
                rep = registration.get(CLIENT_ID);
            }
            catch (ClientRegistrationException e) {
                throw new RuntimeException(e);
            }
        }

        for (String testUrl : testUrls) {
            if (expectedRootUrlError != null) {
                rep.setRootUrl(testUrl);
                registerOrUpdateClientExpectingValidationErrors(rep, register, false, registration, expectedRootUrlError);
            }
            rep.setRootUrl(null);

            if (expectedBaseUrlError != null) {
                rep.setBaseUrl(testUrl);
                registerOrUpdateClientExpectingValidationErrors(rep, register, false, registration, expectedBaseUrlError);
            }
            rep.setBaseUrl(null);

            if (expectedBackchannelLogoutUrlError != null) {
                OIDCAdvancedConfigWrapper.fromClientRepresentation(rep).setBackchannelLogoutUrl(testUrl);
                registerOrUpdateClientExpectingValidationErrors(rep, register, false, registration, expectedBackchannelLogoutUrlError);
            }
            OIDCAdvancedConfigWrapper.fromClientRepresentation(rep).setBackchannelLogoutUrl(null);

            if (expectedRedirectUrisError != null) {
                rep.setRedirectUris(Collections.singletonList(testUrl));
                registerOrUpdateClientExpectingValidationErrors(rep, register, true, registration, expectedRedirectUrisError);
            }
            rep.setRedirectUris(null);

            if (expectedRootUrlError != null) rep.setRootUrl(testUrl);
            if (expectedBaseUrlError != null) rep.setBaseUrl(testUrl);
            if (expectedRedirectUrisError != null) rep.setRedirectUris(Collections.singletonList(testUrl));
            registerOrUpdateClientExpectingValidationErrors(rep, register, expectedRedirectUrisError != null, registration,expectedRootUrlError, expectedBaseUrlError,expectedRedirectUrisError);

            rep.setRootUrl(null);
            rep.setBaseUrl(null);
            rep.setRedirectUris(null);
        }
    }

    private void setupClientAsAdmin(ClientRegistration clientRegistration) throws ClientRegistrationException {
        buildAndVerifyManageClients(authManageClients(clientRegistration));
    }

    private void registerOrUpdateClientExpectingValidationErrors(ClientRepresentation rep, boolean register, boolean redirectUris, ClientRegistration registration, String... expectedErrors) {
        HttpErrorException errorException = null;

        try {
            if (register) {
                registration.create(rep);
            }
            else {
                registration.update(rep);
            }
            Assertions.fail("Expected exception");
        }
        catch (ClientRegistrationException e) {
            errorException = (HttpErrorException) e.getCause();
        }

        expectedErrors = Arrays.stream(expectedErrors).filter(Objects::nonNull).toArray(String[]::new);

        assertEquals(400, errorException.getStatusLine().getStatusCode());
        OAuth2ErrorRepresentation errorRep;
        try {
            errorRep = JsonSerialization.readValue(errorException.getErrorResponse(), OAuth2ErrorRepresentation.class);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<String> actualErrors = asList(errorRep.getErrorDescription().split("; "));
        assertThat(actualErrors, containsInAnyOrder(expectedErrors));
        Assertions.assertEquals(redirectUris ? INVALID_REDIRECT_URI : INVALID_CLIENT_METADATA, errorRep.getError());
    }

    private void testSamlSpecificUrls(boolean register, String... testUrls) throws ClientRegistrationException {
        ClientRepresentation rep = getClientRepresentationForTestClient();
        ClientRegistration registration = oauth.clientRegistration();
        rep.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        if (register) {
            authCreateClients(registration);
        } else {
            rep = buildAndVerifyManageClients(authManageClients(registration));
            rep.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        }
        rep.setAttributes(new HashMap<>());

        Map<String, String> attrs = Map.of(
                SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, "Assertion Consumer Service POST Binding URL",
                SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE, "Assertion Consumer Service Redirect Binding URL",
                SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE, "Artifact Binding URL",
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "Logout Service POST Binding URL",
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE, "Logout Service ARTIFACT Binding URL",
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "Logout Service Redirect Binding URL",
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_ATTRIBUTE, "Logout Service SOAP Binding URL",
                SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE, "Artifact Resolution Service");

        for (String testUrl : testUrls) {
            // admin url
            rep.setAdminUrl(testUrl);
            registerOrUpdateClientExpectingValidationErrors(rep, register, false, registration, "Master SAML Processing URL uses an illegal scheme");
            rep.setAdminUrl(null);
            // attributes
            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                rep.getAttributes().put(entry.getKey(), testUrl);
                registerOrUpdateClientExpectingValidationErrors(rep, register, false, registration, entry.getValue() + " uses an illegal scheme");
                rep.getAttributes().remove(entry.getKey());
            }
        }
    }

    private void updateClient(ClientRegistration clientRegistration) throws ClientRegistrationException {
        ClientRepresentation client = clientRegistration.get(CLIENT_ID);
        client.setRedirectUris(Collections.singletonList("http://localhost:8080/app"));

        clientRegistration.update(client);

        ClientRepresentation updatedClient = clientRegistration.get(CLIENT_ID);

        assertEquals(1, updatedClient.getRedirectUris().size());
        assertEquals("http://localhost:8080/app", updatedClient.getRedirectUris().get(0));
    }

    private void addProtocolMapper(ClientRepresentation client, String mapperName) {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName(mapperName);
        mapper.setProtocol("openid-connect");
        mapper.setProtocolMapper("oidc-usermodel-attribute-mapper");
        mapper.getConfig().put("userinfo.token.claim", "true");
        mapper.getConfig().put("user.attribute", "someAttribute");
        mapper.getConfig().put("id.token.claim", "true");
        mapper.getConfig().put("access.token.claim", "true");
        mapper.getConfig().put("claim.name", "someClaimName");
        mapper.getConfig().put("jsonType.label", "long");

        client.setProtocolMappers(new ArrayList<>());
        client.getProtocolMappers().add(mapper);
    }

    private ClientRepresentation buildAndVerifyManageClients(ClientRegistration oauth) throws ClientRegistrationException {
        ClientRegistration clientRegistration = oauth;

        // Register the client
        ClientRepresentation client = getClientRepresentationForTestClient();
        ClientRepresentation createdClient = clientRegistration.create(client);

        // Verify client was created
        assertNotNull(createdClient);
        assertEquals(CLIENT_ID, createdClient.getClientId());
        return createdClient;
    }

}
