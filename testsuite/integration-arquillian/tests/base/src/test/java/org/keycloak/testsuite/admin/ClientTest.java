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

package org.keycloak.testsuite.admin;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.models.Constants.defaultClients;

import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.common.util.Time;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.adapters.action.TestAvailabilityAction;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.CredentialBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientTest extends AbstractAdminTest {

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void getClients() {
        Assert.assertNames(realm.clients().findAll(), "account", "account-console", "realm-management", "security-admin-console", "broker", Constants.ADMIN_CLI_CLIENT_ID);
    }

    private ClientRepresentation createClient() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("my-app");
        rep.setDescription("my-app description");
        rep.setEnabled(true);
        rep.setPublicClient(true);
        Response response = realm.clients().create(rep);
        response.close();
        String id = ApiUtil.getCreatedId(response);
        getCleanup().addClientUuid(id);
        ClientRepresentation found = ApiUtil.findClientResourceByClientId(realm, "my-app").toRepresentation();

        assertEquals("my-app", found.getClientId());
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientResourcePath(id), rep, ResourceType.CLIENT);

        rep.setId(id);

        return rep;
    }
    
    private ClientRepresentation createClientNonPublic() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("my-app");
        rep.setDescription("my-app description");
        rep.setEnabled(true);
        rep.setPublicClient(false);
        Response response = realm.clients().create(rep);
        response.close();
        String id = ApiUtil.getCreatedId(response);
        getCleanup().addClientUuid(id);
        ClientRepresentation found = ApiUtil.findClientResourceByClientId(realm, "my-app").toRepresentation();

        assertEquals("my-app", found.getClientId());
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientResourcePath(id), rep, ResourceType.CLIENT);

        rep.setId(id);

        return rep;
    }
    
    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void createClientVerifyWithSecret() {
        String id = createClientNonPublic().getId();

        ClientResource client = realm.clients().get(id);
        assertNotNull(client);
        assertNotNull(client.toRepresentation().getSecret());
        Assert.assertNames(realm.clients().findAll(), "account", "account-console", "realm-management", "security-admin-console", "broker", "my-app", Constants.ADMIN_CLI_CLIENT_ID);
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void createClientVerify() {
        String id = createClient().getId();

        ClientResource client = realm.clients().get(id);
        assertNotNull(client);
        assertNull(client.toRepresentation().getSecret());
        Assert.assertNames(realm.clients().findAll(), "account", "account-console", "realm-management", "security-admin-console", "broker", "my-app", Constants.ADMIN_CLI_CLIENT_ID);
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

    private void testClientUriValidation(String expectedRootUrlError, String expectedBaseUrlError, String expectedBackchannelLogoutUrlError, String expectedRedirectUrisError, String... testUrls) {
        testClientUriValidation(false, expectedRootUrlError, expectedBaseUrlError, expectedBackchannelLogoutUrlError, expectedRedirectUrisError, testUrls);
        testClientUriValidation(true, expectedRootUrlError, expectedBaseUrlError, expectedBackchannelLogoutUrlError, expectedRedirectUrisError, testUrls);
    }

    private void testClientUriValidation(boolean create, String expectedRootUrlError, String expectedBaseUrlError, String expectedBackchannelLogoutUrlError, String expectedRedirectUrisError, String... testUrls) {
        ClientRepresentation rep;
        if (create) {
            rep = new ClientRepresentation();
            rep.setClientId("my-app2");
            rep.setEnabled(true);
        }
        else {
            rep = createClient();
        }

        for (String testUrl : testUrls) {
            if (expectedRootUrlError != null) {
                rep.setRootUrl(testUrl);
                createOrUpdateClientExpectingValidationErrors(rep, create, expectedRootUrlError);
            }
            rep.setRootUrl(null);

            if (expectedBaseUrlError != null) {
                rep.setBaseUrl(testUrl);
                createOrUpdateClientExpectingValidationErrors(rep, create, expectedBaseUrlError);
            }
            rep.setBaseUrl(null);

            if (expectedBackchannelLogoutUrlError != null) {
                OIDCAdvancedConfigWrapper.fromClientRepresentation(rep).setBackchannelLogoutUrl(testUrl);
                createOrUpdateClientExpectingValidationErrors(rep, create, expectedBackchannelLogoutUrlError);
            }
            OIDCAdvancedConfigWrapper.fromClientRepresentation(rep).setBackchannelLogoutUrl(null);

            if (expectedRedirectUrisError != null) {
                rep.setRedirectUris(Collections.singletonList(testUrl));
                createOrUpdateClientExpectingValidationErrors(rep, create, expectedRedirectUrisError);
            }
            rep.setRedirectUris(null);

            if (expectedRootUrlError != null) rep.setRootUrl(testUrl);
            if (expectedBaseUrlError != null) rep.setBaseUrl(testUrl);
            if (expectedRedirectUrisError != null) rep.setRedirectUris(Collections.singletonList(testUrl));
            createOrUpdateClientExpectingValidationErrors(rep, create, expectedRootUrlError, expectedBaseUrlError, expectedRedirectUrisError);

            rep.setRootUrl(null);
            rep.setBaseUrl(null);
            rep.setRedirectUris(null);
        }
    }

    private void createOrUpdateClientExpectingValidationErrors(ClientRepresentation rep, boolean create, String... expectedErrors) {
        Response response = null;
        if (create) {
            response = realm.clients().create(rep);
        }
        else {
            try {
                realm.clients().get(rep.getId()).update(rep);
                fail("Expected exception");
            }
            catch (BadRequestException e) {
                response = e.getResponse();
            }
        }

        expectedErrors = Arrays.stream(expectedErrors).filter(Objects::nonNull).toArray(String[]::new);

        assertEquals(response.getStatus(), 400);
        OAuth2ErrorRepresentation errorRep = response.readEntity(OAuth2ErrorRepresentation.class);
        List<String> actualErrors = asList(errorRep.getErrorDescription().split("; "));
        assertThat(actualErrors, containsInAnyOrder(expectedErrors));
        assertEquals("invalid_input", errorRep.getError());
    }

    @Test
    public void removeClient() {
        String id = createClient().getId();

        assertNotNull(ApiUtil.findClientByClientId(realm, "my-app"));
        realm.clients().get(id).remove();
        assertNull(ApiUtil.findClientResourceByClientId(realm, "my-app"));
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.clientResourcePath(id), ResourceType.CLIENT);
    }

    @Test
    public void removeInternalClientExpectingBadRequestException() {
        final String testRealmClientId = ApiUtil.findClientByClientId(realmsResouce().realm("master"), "test-realm")
                .toRepresentation().getId();

        assertThrows(BadRequestException.class,
                () -> realmsResouce().realm("master").clients().get(testRealmClientId).remove());

        defaultClients.forEach(defaultClient -> {
            final String defaultClientId = ApiUtil.findClientByClientId(realm, defaultClient)
                    .toRepresentation().getId();

            assertThrows(BadRequestException.class,
                    () -> realm.clients().get(defaultClientId).remove());
        });
    }

    @Test
    public void getClientRepresentation() {
        String id = createClient().getId();

        ClientRepresentation rep = realm.clients().get(id).toRepresentation();
        assertEquals(id, rep.getId());
        assertEquals("my-app", rep.getClientId());
        assertTrue(rep.isEnabled());
    }

    /**
     * See <a href="https://issues.jboss.org/browse/KEYCLOAK-1918">KEYCLOAK-1918</a>
     */
    @Test
    public void getClientDescription() {
        String id = createClient().getId();

        ClientRepresentation rep = realm.clients().get(id).toRepresentation();
        assertEquals(id, rep.getId());
        assertEquals("my-app description", rep.getDescription());
    }

    @Test
    public void getClientSessions() throws Exception {
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");
        assertEquals(200, response.getStatusCode());

        OAuthClient.AuthorizationEndpointResponse codeResponse = oauth.doLogin("test-user@localhost", "password");

        OAuthClient.AccessTokenResponse response2 = oauth.doAccessTokenRequest(codeResponse.getCode(), "password");
        assertEquals(200, response2.getStatusCode());

        ClientResource app = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");

        assertEquals(2, (long) app.getApplicationSessionCount().get("count"));

        List<UserSessionRepresentation> userSessions = app.getUserSessions(0, 100);
        assertEquals(2, userSessions.size());
        assertEquals(1, userSessions.get(0).getClients().size());
    }

    @Test
    public void getAllClients() {
        List<ClientRepresentation> allClients = realm.clients().findAll();
        assertNotNull(allClients);
        assertFalse(allClients.isEmpty());
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void getAllClientsSearchAndPagination() {
        Set<String> ids = new HashSet<>();
        try {
            for (int i = 1; i <= 10; i++) {
                ClientRepresentation c = ClientBuilder.create().clientId("ccx-" + (i < 10 ? "0" + i : i)).build();
                Response response = realm.clients().create(c);
                ids.add(ApiUtil.getCreatedId(response));
                response.close();
            }

            assertPaginatedClients(1, 10, realm.clients().findAll("ccx-", null, true, 0, 100));
            assertPaginatedClients(1, 5, realm.clients().findAll("ccx-", null, true, 0, 5));
            assertPaginatedClients(6, 10, realm.clients().findAll("ccx-", null, true, 5, 5));
        } finally {
            ids.stream().forEach(id -> realm.clients().get(id).remove());
        }
    }

    private void assertPaginatedClients(int start, int end, List<ClientRepresentation> actual) {
        List<String> expected = new LinkedList<>();
        for (int i = start; i <= end; i++) {
            expected.add("ccx-" + (i < 10 ? "0" + i : i));
        }
        List<String> a = actual.stream().map(rep -> rep.getClientId()).collect(Collectors.toList());
        assertThat(a, is(expected));

    }

    @Test
    public void getClientById() {
        createClient();
        ClientRepresentation rep = ApiUtil.findClientResourceByClientId(realm, "my-app").toRepresentation();
        ClientRepresentation gotById = realm.clients().get(rep.getId()).toRepresentation();
        assertClient(rep, gotById);
    }

    @Test
    // KEYCLOAK-1110
    public void deleteDefaultRole() {
        ClientRepresentation rep = createClient();
        String id = rep.getId();

        RoleRepresentation role = new RoleRepresentation("test", "test", false);
        realm.clients().get(id).roles().create(role);

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(id, "test"), role, ResourceType.CLIENT_ROLE);

        role = realm.clients().get(id).roles().get("test").toRepresentation();

        realm.roles().get(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + REALM_NAME).addComposites(Collections.singletonList(role));

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.roleResourceCompositesPath(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + REALM_NAME), Collections.singletonList(role), ResourceType.REALM_ROLE);

        assertThat(realm.roles().get(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + REALM_NAME).getRoleComposites().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()), 
                hasItem(role.getName()));

        realm.clients().get(id).roles().deleteRole("test");

        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.clientRoleResourcePath(id, "test"), ResourceType.CLIENT_ROLE);

        assertThat(realm.roles().get(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + REALM_NAME).getRoleComposites().stream().map(RoleRepresentation::getName).collect(Collectors.toSet()), 
                not(hasItem(role)));
    }

    @Test
    public void testProtocolMappers() {
        String clientDbId = createClient().getId();
        ProtocolMappersResource mappersResource = ApiUtil.findClientByClientId(realm, "my-app").getProtocolMappers();

        protocolMappersTest(clientDbId, mappersResource);
    }

    @Test
    public void updateClient() {
        ClientRepresentation client = createClient();

        ClientRepresentation newClient = new ClientRepresentation();
        newClient.setId(client.getId());
        newClient.setClientId(client.getClientId());
        newClient.setBaseUrl("http://baseurl");

        realm.clients().get(client.getId()).update(newClient);

        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.clientResourcePath(client.getId()), newClient, ResourceType.CLIENT);

        ClientRepresentation storedClient = realm.clients().get(client.getId()).toRepresentation();

        assertClient(client, storedClient);

        newClient.setSecret("new-secret");

        realm.clients().get(client.getId()).update(newClient);

        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.clientResourcePath(client.getId()), newClient, ResourceType.CLIENT);

        storedClient = realm.clients().get(client.getId()).toRepresentation();
        assertClient(client, storedClient);

        storedClient.getAttributes().put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, "");

        realm.clients().get(storedClient.getId()).update(storedClient);
        storedClient = realm.clients().get(client.getId()).toRepresentation();

        assertFalse(storedClient.getAttributes().containsKey(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL));
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void serviceAccount() {
        Response response = realm.clients().create(ClientBuilder.create().clientId("serviceClient").serviceAccount().build());
        String id = ApiUtil.getCreatedId(response);
        getCleanup().addClientUuid(id);
        response.close();
        UserRepresentation userRep = realm.clients().get(id).getServiceAccountUser();
        assertEquals("service-account-serviceclient", userRep.getUsername());
        // KEYCLOAK-11197 service accounts are no longer created with a placeholder e-mail.
        assertNull(userRep.getEmail());
    }

    @Test
    public void pushRevocation() {
        testingClient.testApp().clearAdminActions();

        ClientRepresentation client = createAppClient();
        String id = client.getId();

        realm.clients().get(id).pushRevocation();

        PushNotBeforeAction pushNotBefore = testingClient.testApp().getAdminPushNotBefore();
        assertEquals(client.getNotBefore().intValue(), pushNotBefore.getNotBefore());

        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, AdminEventPaths.clientPushRevocationPath(id), ResourceType.CLIENT);
    }

    private ClientRepresentation createAppClient() {
        String redirectUri = oauth.getRedirectUri().replace("/master/", "/" + REALM_NAME + "/");

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("test-app");
        client.setAdminUrl(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/app/admin");
        client.setRedirectUris(Collections.singletonList(redirectUri));
        client.setSecret("secret");
        client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        int notBefore = Time.currentTime() - 60;
        client.setNotBefore(notBefore);

        Response response = realm.clients().create(client);
        String id = ApiUtil.getCreatedId(response);
        getCleanup().addClientUuid(id);
        response.close();

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientResourcePath(id), client, ResourceType.CLIENT);

        client.setId(id);
        return client;
    }

    @Test (expected = BadRequestException.class)
    public void testAddNodeWithReservedCharacter() {
        testingClient.testApp().clearAdminActions();

        ClientRepresentation client = createAppClient();
        String id = client.getId();

        realm.clients().get(id).registerNode(Collections.singletonMap("node", "foo#"));
    }
    
    @Test
    public void nodes() {
        testingClient.testApp().clearAdminActions();

        ClientRepresentation client = createAppClient();
        String id = client.getId();

        String myhost = suiteContext.getAuthServerInfo().getContextRoot().getHost();
        realm.clients().get(id).registerNode(Collections.singletonMap("node", myhost));
        realm.clients().get(id).registerNode(Collections.singletonMap("node", "invalid"));

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientNodePath(id, myhost), ResourceType.CLUSTER_NODE);
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientNodePath(id, "invalid"), ResourceType.CLUSTER_NODE);

        GlobalRequestResult result = realm.clients().get(id).testNodesAvailable();
        assertEquals(1, result.getSuccessRequests().size());
        assertEquals(1, result.getFailedRequests().size());

        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, AdminEventPaths.clientTestNodesAvailablePath(id), result, ResourceType.CLUSTER_NODE);

        TestAvailabilityAction testAvailable = testingClient.testApp().getTestAvailable();
        assertEquals("test-app", testAvailable.getResource());

        assertEquals(2, realm.clients().get(id).toRepresentation().getRegisteredNodes().size());

        realm.clients().get(id).unregisterNode("invalid");

        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.clientNodePath(id, "invalid"), ResourceType.CLUSTER_NODE);

        assertEquals(1, realm.clients().get(id).toRepresentation().getRegisteredNodes().size());
    }

    @Test
    public void offlineUserSessions() throws IOException {
        ClientRepresentation client = createAppClient();
        String id = client.getId();

        Response response = realm.users().create(UserBuilder.create().username("testuser").build());
        String userId = ApiUtil.getCreatedId(response);
        response.close();

        realm.users().get(userId).resetPassword(CredentialBuilder.create().password("password").build());

        Map<String, Long> offlineSessionCount = realm.clients().get(id).getOfflineSessionCount();
        assertEquals(new Long(0), offlineSessionCount.get("count"));

        List<UserSessionRepresentation> userSessions = realm.users().get(userId).getOfflineSessions(id);
        assertEquals("There should be no offline sessions", 0, userSessions.size());

        oauth.realm(REALM_NAME);
        oauth.redirectUri(client.getRedirectUris().get(0));
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.doLogin("testuser", "password");
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(oauth.getCurrentQuery().get("code"), "secret");
        assertEquals(200, accessTokenResponse.getStatusCode());

        offlineSessionCount = realm.clients().get(id).getOfflineSessionCount();
        assertEquals(new Long(1), offlineSessionCount.get("count"));

        List<UserSessionRepresentation> offlineUserSessions = realm.clients().get(id).getOfflineUserSessions(0, 100);
        assertEquals(1, offlineUserSessions.size());
        assertEquals("testuser", offlineUserSessions.get(0).getUsername());
        org.hamcrest.MatcherAssert.assertThat(offlineUserSessions.get(0).getLastAccess(),
            allOf(greaterThan(Time.currentTimeMillis() - 10000L), lessThan(Time.currentTimeMillis())));

        userSessions = realm.users().get(userId).getOfflineSessions(id);
        assertEquals("There should be one offline session", 1, userSessions.size());
        assertOfflineSession(offlineUserSessions.get(0), userSessions.get(0));
    }

    private void assertOfflineSession(UserSessionRepresentation expected, UserSessionRepresentation actual) {
        assertEquals("id", expected.getId(), actual.getId());
        assertEquals("userId", expected.getUserId(), actual.getUserId());
        assertEquals("userName", expected.getUsername(), actual.getUsername());
        assertEquals("clients", expected.getClients(), actual.getClients());
    }

    @Test
    public void scopes() {
        Response response = realm.clients().create(ClientBuilder.create().clientId("client").fullScopeEnabled(false).build());
        String id = ApiUtil.getCreatedId(response);
        getCleanup().addClientUuid(id);
        response.close();

        assertAdminEvents.poll();

        RoleMappingResource scopesResource = realm.clients().get(id).getScopeMappings();

        RoleRepresentation roleRep1 = createRealmRole("realm-composite");
        RoleRepresentation roleRep2 = createRealmRole("realm-child");

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.roleResourcePath("realm-composite"), roleRep1, ResourceType.REALM_ROLE);
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.roleResourcePath("realm-child"), roleRep2, ResourceType.REALM_ROLE);

        roleRep1 = realm.roles().get("realm-composite").toRepresentation();
        roleRep2 = realm.roles().get("realm-child").toRepresentation();

        realm.roles().get("realm-composite").addComposites(Collections.singletonList(roleRep2));

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.roleResourceCompositesPath("realm-composite"), Collections.singletonList(roleRep2), ResourceType.REALM_ROLE);

        String accountMgmtId = realm.clients().findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation viewAccountRoleRep = realm.clients().get(accountMgmtId).roles().get(AccountRoles.VIEW_PROFILE).toRepresentation();

        scopesResource.realmLevel().add(Collections.singletonList(roleRep1));
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientScopeMappingsRealmLevelPath(id), Collections.singletonList(roleRep1), ResourceType.REALM_SCOPE_MAPPING);

        scopesResource.clientLevel(accountMgmtId).add(Collections.singletonList(viewAccountRoleRep));
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientScopeMappingsClientLevelPath(id, accountMgmtId), Collections.singletonList(viewAccountRoleRep), ResourceType.CLIENT_SCOPE_MAPPING);

        Assert.assertNames(scopesResource.realmLevel().listAll(), "realm-composite");
        Assert.assertNames(scopesResource.realmLevel().listEffective(), "realm-composite", "realm-child");
        Assert.assertNames(scopesResource.realmLevel().listAvailable(), "realm-child", "offline_access",
                Constants.AUTHZ_UMA_AUTHORIZATION, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + REALM_NAME);

        Assert.assertNames(scopesResource.clientLevel(accountMgmtId).listAll(), AccountRoles.VIEW_PROFILE);
        Assert.assertNames(scopesResource.clientLevel(accountMgmtId).listEffective(), AccountRoles.VIEW_PROFILE);

        Assert.assertNames(scopesResource.clientLevel(accountMgmtId).listAvailable(), AccountRoles.MANAGE_ACCOUNT, AccountRoles.MANAGE_ACCOUNT_LINKS, AccountRoles.VIEW_APPLICATIONS, AccountRoles.VIEW_CONSENT, AccountRoles.MANAGE_CONSENT, AccountRoles.DELETE_ACCOUNT);

        Assert.assertNames(scopesResource.getAll().getRealmMappings(), "realm-composite");
        Assert.assertNames(scopesResource.getAll().getClientMappings().get(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).getMappings(), AccountRoles.VIEW_PROFILE);

        scopesResource.realmLevel().remove(Collections.singletonList(roleRep1));
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.clientScopeMappingsRealmLevelPath(id), Collections.singletonList(roleRep1), ResourceType.REALM_SCOPE_MAPPING);

        scopesResource.clientLevel(accountMgmtId).remove(Collections.singletonList(viewAccountRoleRep));
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.clientScopeMappingsClientLevelPath(id, accountMgmtId), Collections.singletonList(viewAccountRoleRep), ResourceType.CLIENT_SCOPE_MAPPING);

        Assert.assertNames(scopesResource.realmLevel().listAll());
        Assert.assertNames(scopesResource.realmLevel().listEffective());
        Assert.assertNames(scopesResource.realmLevel().listAvailable(), "offline_access", Constants.AUTHZ_UMA_AUTHORIZATION, "realm-composite", "realm-child", Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + REALM_NAME);
        Assert.assertNames(scopesResource.clientLevel(accountMgmtId).listAll());

        Assert.assertNames(scopesResource.clientLevel(accountMgmtId).listAvailable(), AccountRoles.VIEW_PROFILE, AccountRoles.MANAGE_ACCOUNT, AccountRoles.MANAGE_ACCOUNT_LINKS, AccountRoles.VIEW_APPLICATIONS, AccountRoles.VIEW_CONSENT, AccountRoles.MANAGE_CONSENT, AccountRoles.DELETE_ACCOUNT);

        Assert.assertNames(scopesResource.clientLevel(accountMgmtId).listEffective());
    }

    /**
     * Test for KEYCLOAK-10603.
     */
    @Test
    public void rolesCanBeAddedToScopeEvenWhenTheyAreAlreadyIndirectlyAssigned() {
        Response response =
                realm.clients().create(ClientBuilder.create().clientId("test-client").fullScopeEnabled(false).build());
        String testedClientUuid = ApiUtil.getCreatedId(response);
        getCleanup().addClientUuid(testedClientUuid);
        response.close();

        createRealmRole("realm-composite");
        createRealmRole("realm-child");
        realm.roles().get("realm-composite")
                .addComposites(Collections.singletonList(realm.roles().get("realm-child").toRepresentation()));

        response = realm.clients().create(ClientBuilder.create().clientId("role-container-client").build());
        String roleContainerClientUuid = ApiUtil.getCreatedId(response);
        getCleanup().addClientUuid(roleContainerClientUuid);
        response.close();

        RoleRepresentation clientCompositeRole = RoleBuilder.create().name("client-composite").build();
        realm.clients().get(roleContainerClientUuid).roles().create(clientCompositeRole);
        realm.clients().get(roleContainerClientUuid).roles().create(RoleBuilder.create().name("client-child").build());
        realm.clients().get(roleContainerClientUuid).roles().get("client-composite").addComposites(Collections
                .singletonList(
                        realm.clients().get(roleContainerClientUuid).roles().get("client-child").toRepresentation()));

        // Make indirect assignments: assign composite roles
        RoleMappingResource scopesResource = realm.clients().get(testedClientUuid).getScopeMappings();
        scopesResource.realmLevel()
                .add(Collections.singletonList(realm.roles().get("realm-composite").toRepresentation()));
        scopesResource.clientLevel(roleContainerClientUuid).add(Collections
                .singletonList(realm.clients().get(roleContainerClientUuid).roles().get("client-composite")
                        .toRepresentation()));

        // check state before making the direct assignments
        Assert.assertNames(scopesResource.realmLevel().listAll(), "realm-composite");
        Assert.assertNames(scopesResource.realmLevel().listAvailable(), "realm-child", "offline_access",
                Constants.AUTHZ_UMA_AUTHORIZATION, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + REALM_NAME);
        Assert.assertNames(scopesResource.realmLevel().listEffective(), "realm-composite", "realm-child");

        Assert.assertNames(scopesResource.clientLevel(roleContainerClientUuid).listAll(), "client-composite");
        Assert.assertNames(scopesResource.clientLevel(roleContainerClientUuid).listAvailable(), "client-child");
        Assert.assertNames(scopesResource.clientLevel(roleContainerClientUuid).listEffective(), "client-composite",
                "client-child");

        // Make direct assignments for roles which are already indirectly assigned
        scopesResource.realmLevel().add(Collections.singletonList(realm.roles().get("realm-child").toRepresentation()));
        scopesResource.clientLevel(roleContainerClientUuid).add(Collections
                .singletonList(
                        realm.clients().get(roleContainerClientUuid).roles().get("client-child").toRepresentation()));

        // List realm roles
        Assert.assertNames(scopesResource.realmLevel().listAll(), "realm-composite", "realm-child");
        Assert.assertNames(scopesResource.realmLevel().listAvailable(), "offline_access",
                Constants.AUTHZ_UMA_AUTHORIZATION, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + REALM_NAME);
        Assert.assertNames(scopesResource.realmLevel().listEffective(), "realm-composite", "realm-child");

        // List client roles
        Assert.assertNames(scopesResource.clientLevel(roleContainerClientUuid).listAll(), "client-composite",
                "client-child");
        Assert.assertNames(scopesResource.clientLevel(roleContainerClientUuid).listAvailable());
        Assert.assertNames(scopesResource.clientLevel(roleContainerClientUuid).listEffective(), "client-composite",
                "client-child");
    }

    @Test
    public void scopesRoleRemoval() {
        // clientA to test scope mappins
        Response response = realm.clients().create(ClientBuilder.create().clientId("clientA").fullScopeEnabled(false).build());
        String idA = ApiUtil.getCreatedId(response);
        getCleanup().addClientUuid(idA);
        response.close();
        assertAdminEvents.poll();

        // clientB to create a client role for clientA
        response = realm.clients().create(ClientBuilder.create().clientId("clientB").fullScopeEnabled(false).build());
        String idB = ApiUtil.getCreatedId(response);
        getCleanup().addClientUuid(idB);
        response.close();
        assertAdminEvents.poll();

        RoleMappingResource scopesResource = realm.clients().get(idA).getScopeMappings();

        // create a realm role and a role in clientB
        RoleRepresentation realmRoleRep = createRealmRole("realm-role");
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.roleResourcePath(realmRoleRep.getName()), realmRoleRep, ResourceType.REALM_ROLE);
        RoleRepresentation clientBRoleRep = RoleBuilder.create().name("clientB-role").build();
        realm.clients().get(idB).roles().create(clientBRoleRep);
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(idB, clientBRoleRep.getName()), clientBRoleRep, ResourceType.CLIENT_ROLE);

        // assing to clientA both roles to the scope mappings
        realmRoleRep = realm.roles().get(realmRoleRep.getName()).toRepresentation();
        clientBRoleRep = realm.clients().get(idB).roles().get(clientBRoleRep.getName()).toRepresentation();
        scopesResource.realmLevel().add(Collections.singletonList(realmRoleRep));
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientScopeMappingsRealmLevelPath(idA), Collections.singletonList(realmRoleRep), ResourceType.REALM_SCOPE_MAPPING);
        scopesResource.clientLevel(idB).add(Collections.singletonList(clientBRoleRep));
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientScopeMappingsClientLevelPath(idA, idB), Collections.singletonList(clientBRoleRep), ResourceType.CLIENT_SCOPE_MAPPING);

        // assert the roles are there
        Assert.assertNames(scopesResource.realmLevel().listAll(), realmRoleRep.getName());
        Assert.assertNames(scopesResource.clientLevel(idB).listAll(), clientBRoleRep.getName());

        // delete realm role and check everything is refreshed ok
        realm.roles().deleteRole(realmRoleRep.getName());
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.roleResourcePath(realmRoleRep.getName()), ResourceType.REALM_ROLE);
        Assert.assertNames(scopesResource.realmLevel().listAll());
        Assert.assertNames(scopesResource.clientLevel(idB).listAll(), clientBRoleRep.getName());

        // delete client role and check everything is refreshed ok
        realm.clients().get(idB).roles().deleteRole(clientBRoleRep.getName());
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.clientRoleResourcePath(idB, clientBRoleRep.getName()), ResourceType.CLIENT_ROLE);
        Assert.assertNames(scopesResource.realmLevel().listAll());
        Assert.assertNames(scopesResource.clientLevel(idB).listAll());
    }

    public void protocolMappersTest(String clientDbId, ProtocolMappersResource mappersResource) {
        // assert default mappers found
        List<ProtocolMapperRepresentation> protocolMappers = mappersResource.getMappers();

        String emailMapperId = null;
        String usernameMapperId = null;
        String fooMapperId = null;
        for (ProtocolMapperRepresentation mapper : protocolMappers) {
            if (mapper.getName().equals(OIDCLoginProtocolFactory.EMAIL)) {
                emailMapperId = mapper.getId();
            } else if (mapper.getName().equals(OIDCLoginProtocolFactory.USERNAME)) {
                usernameMapperId = mapper.getId();
            } else if (mapper.getName().equals("foo")) {
                fooMapperId = mapper.getId();
            }
        }

        // Builtin mappers are not here
        assertNull(emailMapperId);
        assertNull(usernameMapperId);

        assertNull(fooMapperId);

        // Create foo mapper
        ProtocolMapperRepresentation fooMapper = new ProtocolMapperRepresentation();
        fooMapper.setName("foo");
        fooMapper.setProtocol("openid-connect");
        fooMapper.setProtocolMapper("oidc-hardcoded-claim-mapper");
        Response response = mappersResource.createMapper(fooMapper);
        String location = response.getLocation().toString();
        fooMapperId = location.substring(location.lastIndexOf("/") + 1);
        response.close();

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientProtocolMapperPath(clientDbId, fooMapperId), fooMapper, ResourceType.PROTOCOL_MAPPER);

        fooMapper = mappersResource.getMapperById(fooMapperId);
        assertEquals(fooMapper.getName(), "foo");

        // Update foo mapper
        mappersResource.update(fooMapperId, fooMapper);

        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.clientProtocolMapperPath(clientDbId, fooMapperId), fooMapper, ResourceType.PROTOCOL_MAPPER);

        fooMapper = mappersResource.getMapperById(fooMapperId);

        // Remove foo mapper
        mappersResource.delete(fooMapperId);
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.clientProtocolMapperPath(clientDbId, fooMapperId), ResourceType.PROTOCOL_MAPPER);
        try {
            mappersResource.getMapperById(fooMapperId);
            fail("Not expected to find deleted mapper");
        } catch (NotFoundException nfe) {
        }
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void updateClientWithProtocolMapper() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("my-app");

        ProtocolMapperRepresentation fooMapper = new ProtocolMapperRepresentation();
        fooMapper.setName("foo");
        fooMapper.setProtocol("openid-connect");
        fooMapper.setProtocolMapper("oidc-hardcoded-claim-mapper");
        rep.setProtocolMappers(Collections.singletonList(fooMapper));

        Response response = realm.clients().create(rep);
        response.close();
        String id = ApiUtil.getCreatedId(response);
        getCleanup().addClientUuid(id);

        ClientResource clientResource = realm.clients().get(id);
        assertNotNull(clientResource);
        ClientRepresentation client = clientResource.toRepresentation();
        List<ProtocolMapperRepresentation> protocolMappers = client.getProtocolMappers();
        assertEquals(1, protocolMappers.size());
        ProtocolMapperRepresentation mapper = protocolMappers.get(0);
        assertEquals("foo", mapper.getName());

        ClientRepresentation newClient = new ClientRepresentation();
        newClient.setId(client.getId());
        newClient.setClientId(client.getClientId());

        ProtocolMapperRepresentation barMapper = new ProtocolMapperRepresentation();
        barMapper.setName("bar");
        barMapper.setProtocol("openid-connect");
        barMapper.setProtocolMapper("oidc-hardcoded-role-mapper");
        protocolMappers.add(barMapper);
        newClient.setProtocolMappers(protocolMappers);

        realm.clients().get(client.getId()).update(newClient);

        ClientRepresentation storedClient = realm.clients().get(client.getId()).toRepresentation();
        assertClient(client, storedClient);
    }

    public static void assertClient(ClientRepresentation client, ClientRepresentation storedClient) {
        if (client.getClientId() != null) Assert.assertEquals(client.getClientId(), storedClient.getClientId());
        if (client.getName() != null) Assert.assertEquals(client.getName(), storedClient.getName());
        if (client.isEnabled() != null) Assert.assertEquals(client.isEnabled(), storedClient.isEnabled());
        if (client.isAlwaysDisplayInConsole() != null) Assert.assertEquals(client.isAlwaysDisplayInConsole(), storedClient.isAlwaysDisplayInConsole());
        if (client.isBearerOnly() != null) Assert.assertEquals(client.isBearerOnly(), storedClient.isBearerOnly());
        if (client.isPublicClient() != null) Assert.assertEquals(client.isPublicClient(), storedClient.isPublicClient());
        if (client.isFullScopeAllowed() != null) Assert.assertEquals(client.isFullScopeAllowed(), storedClient.isFullScopeAllowed());
        if (client.getRootUrl() != null) Assert.assertEquals(client.getRootUrl(), storedClient.getRootUrl());
        if (client.getAdminUrl() != null) Assert.assertEquals(client.getAdminUrl(), storedClient.getAdminUrl());
        if (client.getBaseUrl() != null) Assert.assertEquals(client.getBaseUrl(), storedClient.getBaseUrl());
        if (client.isSurrogateAuthRequired() != null) Assert.assertEquals(client.isSurrogateAuthRequired(), storedClient.isSurrogateAuthRequired());
        if (client.getClientAuthenticatorType() != null) Assert.assertEquals(client.getClientAuthenticatorType(), storedClient.getClientAuthenticatorType());

        if (client.getNotBefore() != null) {
            Assert.assertEquals(client.getNotBefore(), storedClient.getNotBefore());
        }
        if (client.getDefaultRoles() != null) {
            Set<String> set = new HashSet<String>();
            for (String val : client.getDefaultRoles()) {
                set.add(val);
            }
            Set<String> storedSet = new HashSet<String>();
            for (String val : storedClient.getDefaultRoles()) {
                storedSet.add(val);
            }

            Assert.assertEquals(set, storedSet);
        }

        List<String> redirectUris = client.getRedirectUris();
        if (redirectUris != null) {
            Set<String> set = new HashSet<String>();
            for (String val : client.getRedirectUris()) {
                set.add(val);
            }
            Set<String> storedSet = new HashSet<String>();
            for (String val : storedClient.getRedirectUris()) {
                storedSet.add(val);
            }

            Assert.assertEquals(set, storedSet);
        }

        List<String> webOrigins = client.getWebOrigins();
        if (webOrigins != null) {
            Set<String> set = new HashSet<String>();
            for (String val : client.getWebOrigins()) {
                set.add(val);
            }
            Set<String> storedSet = new HashSet<String>();
            for (String val : storedClient.getWebOrigins()) {
                storedSet.add(val);
            }

            Assert.assertEquals(set, storedSet);
        }

        List<ProtocolMapperRepresentation> protocolMappers = client.getProtocolMappers();
        if(protocolMappers != null){
            Set<String> set = protocolMappers.stream()
                    .map(ProtocolMapperRepresentation::getName)
                    .collect(Collectors.toSet());
            Set<String> storedSet = storedClient.getProtocolMappers().stream()
                    .map(ProtocolMapperRepresentation::getName)
                    .collect(Collectors.toSet());

            Assert.assertEquals(set, storedSet);
        }
    }

    private RoleRepresentation createRealmRole(String roleName) {
        RoleRepresentation role = RoleBuilder.create().name(roleName).build();
        realm.roles().create(role);

        String createdId = realm.roles().get(role.getName()).toRepresentation().getId();
        getCleanup().addRoleId(createdId);

        return role;
    }
}
