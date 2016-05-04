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

import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.common.util.Time;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.adapters.action.TestAvailabilityAction;
import org.keycloak.representations.idm.*;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.services.resources.admin.ScopeMappedResource;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.CredentialBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientTest extends AbstractAdminTest {

    @Test
    public void getClients() {
        Assert.assertNames(realm.clients().findAll(), "account", "realm-management", "security-admin-console", "broker", Constants.ADMIN_CLI_CLIENT_ID);
    }

    private ClientRepresentation createClient() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("my-app");
        rep.setDescription("my-app description");
        rep.setEnabled(true);
        Response response = realm.clients().create(rep);
        response.close();
        String id = ApiUtil.getCreatedId(response);
        rep.setId(id);
        return rep;
    }

    @Test
    public void createClientVerify() {
        String id = createClient().getId();

        assertNotNull(realm.clients().get(id));
        Assert.assertNames(realm.clients().findAll(), "account", "realm-management", "security-admin-console", "broker", "my-app", Constants.ADMIN_CLI_CLIENT_ID);
    }

    @Test
    public void removeClient() {
        String id = createClient().getId();

        realm.clients().get(id).remove();
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

        OAuthClient.AuthorizationCodeResponse codeResponse = oauth.doLogin("test-user@localhost", "password");

        OAuthClient.AccessTokenResponse response2 = oauth.doAccessTokenRequest(codeResponse.getCode(), "password");
        assertEquals(200, response2.getStatusCode());

        ClientResource app = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");

        assertEquals(2, (long) app.getApplicationSessionCount().get("count"));

        List<UserSessionRepresentation> userSessions = app.getUserSessions(0, 100);
        assertEquals(2, userSessions.size());
        assertEquals(1, userSessions.get(0).getClients().size());
    }

    @Test
    // KEYCLOAK-1110
    public void deleteDefaultRole() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("my-app");
        rep.setEnabled(true);
        Response response = realm.clients().create(rep);
        response.close();
        String id = ApiUtil.getCreatedId(response);

        RoleRepresentation role = new RoleRepresentation("test", "test", false);
        realm.clients().get(id).roles().create(role);

        rep = realm.clients().get(id).toRepresentation();
        rep.setDefaultRoles(new String[] { "test" });
        realm.clients().get(id).update(rep);

        assertArrayEquals(new String[] { "test" }, realm.clients().get(id).toRepresentation().getDefaultRoles());

        realm.clients().get(id).roles().deleteRole("test");

        assertNull(realm.clients().get(id).toRepresentation().getDefaultRoles());
    }

    @Test
    public void testProtocolMappers() {
        createClient();
        ProtocolMappersResource mappersResource = ApiUtil.findClientByClientId(realm, "my-app").getProtocolMappers();

        protocolMappersTest(mappersResource);
    }

    @Test
    public void updateClient() {
        ClientRepresentation client = createClient();

        ClientRepresentation newClient = new ClientRepresentation();
        newClient.setId(client.getId());
        newClient.setClientId(client.getClientId());
        newClient.setBaseUrl("http://baseurl");

        realm.clients().get(client.getId()).update(newClient);

        ClientRepresentation storedClient = realm.clients().get(client.getId()).toRepresentation();

        assertClient(client, storedClient);

        newClient.setSecret("new-secret");

        realm.clients().get(client.getId()).update(newClient);

        storedClient = realm.clients().get(client.getId()).toRepresentation();
        assertClient(client, storedClient);
    }

    @Test
    public void serviceAccount() {
        Response response = realm.clients().create(ClientBuilder.create().clientId("serviceClient").serviceAccount().build());
        String id = ApiUtil.getCreatedId(response);
        UserRepresentation userRep = realm.clients().get(id).getServiceAccountUser();
        assertEquals("service-account-serviceclient", userRep.getUsername());
    }

    @Test
    public void pushRevocation() {
        testingClient.testApp().clearAdminActions();

        String redirectUri = oauth.getRedirectUri().replace("/master/", "/" + REALM_NAME + "/");

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("test-app");
        client.setAdminUrl(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/app/admin");
        client.setRedirectUris(Collections.singletonList(redirectUri));
        client.setSecret("secret");

        int notBefore = Time.currentTime() - 60;

        client.setNotBefore(notBefore);
        Response response = realm.clients().create(client);
        String id = ApiUtil.getCreatedId(response);
        response.close();

        realm.clients().get(id).pushRevocation();

        PushNotBeforeAction pushNotBefore = testingClient.testApp().getAdminPushNotBefore();
        assertEquals(notBefore, pushNotBefore.getNotBefore());
    }

    @Test
    public void nodes() {
        testingClient.testApp().clearAdminActions();

        String redirectUri = oauth.getRedirectUri().replace("/master/", "/" + REALM_NAME + "/");

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("test-app");
        client.setAdminUrl(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/app/admin");
        client.setRedirectUris(Collections.singletonList(redirectUri));
        client.setSecret("secret");

        Response response = realm.clients().create(client);
        String id = ApiUtil.getCreatedId(response);
        response.close();

        realm.clients().get(id).registerNode(Collections.singletonMap("node", suiteContext.getAuthServerInfo().getContextRoot().getHost()));
        realm.clients().get(id).registerNode(Collections.singletonMap("node", "invalid"));

        GlobalRequestResult result = realm.clients().get(id).testNodesAvailable();
        assertEquals(1, result.getSuccessRequests().size());
        assertEquals(1, result.getFailedRequests().size());

        TestAvailabilityAction testAvailable = testingClient.testApp().getTestAvailable();
        assertEquals("test-app", testAvailable.getResource());

        assertEquals(2, realm.clients().get(id).toRepresentation().getRegisteredNodes().size());

        realm.clients().get(id).unregisterNode("invalid");

        assertEquals(1, realm.clients().get(id).toRepresentation().getRegisteredNodes().size());
    }

    @Test
    public void offlineUserSessions() throws IOException {
        String redirectUri = oauth.getRedirectUri().replace("/master/", "/" + REALM_NAME + "/");

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("test-app");
        client.setAdminUrl(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/master/app/admin");
        client.setRedirectUris(Collections.singletonList(redirectUri));
        client.setSecret("secret");

        Response response = realm.clients().create(client);
        String id = ApiUtil.getCreatedId(response);
        response.close();

        response = realm.users().create(UserBuilder.create().username("testuser").build());
        String userId = ApiUtil.getCreatedId(response);
        response.close();

        realm.users().get(userId).resetPassword(CredentialBuilder.create().password("password").build());

        Map<String, Long> offlineSessionCount = realm.clients().get(id).getOfflineSessionCount();
        assertEquals(new Long(0), offlineSessionCount.get("count"));

        oauth.realm(REALM_NAME);
        oauth.redirectUri(redirectUri);
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.doLogin("testuser", "password");
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(oauth.getCurrentQuery().get("code"), "secret");
        assertEquals(200, accessTokenResponse.getStatusCode());

        offlineSessionCount = realm.clients().get(id).getOfflineSessionCount();
        assertEquals(new Long(1), offlineSessionCount.get("count"));

        List<UserSessionRepresentation> offlineUserSessions = realm.clients().get(id).getOfflineUserSessions(0, 100);
        assertEquals(1, offlineUserSessions.size());
        assertEquals("testuser", offlineUserSessions.get(0).getUsername());
    }

    @Test
    public void scopes() {
        Response response = realm.clients().create(ClientBuilder.create().clientId("client").fullScopeEnabled(false).build());
        String id = ApiUtil.getCreatedId(response);
        response.close();

        RoleMappingResource scopesResource = realm.clients().get(id).getScopeMappings();

        realm.roles().create(RoleBuilder.create().name("role1").build());
        realm.roles().create(RoleBuilder.create().name("role2").build());

        RoleRepresentation roleRep1 = realm.roles().get("role1").toRepresentation();
        RoleRepresentation roleRep2 = realm.roles().get("role2").toRepresentation();

        realm.roles().get("role1").addComposites(Collections.singletonList(roleRep2));

        String accountMgmtId = realm.clients().findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation viewAccountRoleRep = realm.clients().get(accountMgmtId).roles().get(AccountRoles.VIEW_PROFILE).toRepresentation();

        scopesResource.realmLevel().add(Collections.singletonList(roleRep1));
        scopesResource.clientLevel(accountMgmtId).add(Collections.singletonList(viewAccountRoleRep));

        Assert.assertNames(scopesResource.realmLevel().listAll(), "role1");
        Assert.assertNames(scopesResource.realmLevel().listEffective(), "role1", "role2");
        Assert.assertNames(scopesResource.realmLevel().listAvailable(), "offline_access");

        Assert.assertNames(scopesResource.clientLevel(accountMgmtId).listAll(), AccountRoles.VIEW_PROFILE);
        Assert.assertNames(scopesResource.clientLevel(accountMgmtId).listEffective(), AccountRoles.VIEW_PROFILE);
        Assert.assertNames(scopesResource.clientLevel(accountMgmtId).listAvailable(), AccountRoles.MANAGE_ACCOUNT);

        Assert.assertNames(scopesResource.getAll().getRealmMappings(), "role1");
        Assert.assertNames(scopesResource.getAll().getClientMappings().get(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).getMappings(), AccountRoles.VIEW_PROFILE);

        scopesResource.realmLevel().remove(Collections.singletonList(roleRep1));
        scopesResource.clientLevel(accountMgmtId).remove(Collections.singletonList(viewAccountRoleRep));

        Assert.assertNames(scopesResource.realmLevel().listAll());
        Assert.assertNames(scopesResource.realmLevel().listEffective());
        Assert.assertNames(scopesResource.realmLevel().listAvailable(), "offline_access", "role1", "role2");
        Assert.assertNames(scopesResource.clientLevel(accountMgmtId).listAll());
        Assert.assertNames(scopesResource.clientLevel(accountMgmtId).listAvailable(), AccountRoles.VIEW_PROFILE, AccountRoles.MANAGE_ACCOUNT);
        Assert.assertNames(scopesResource.clientLevel(accountMgmtId).listEffective());
    }

    public static void protocolMappersTest(ProtocolMappersResource mappersResource) {
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

        assertNotNull(emailMapperId);
        assertNotNull(usernameMapperId);
        assertNull(fooMapperId);

        // Create foo mapper
        ProtocolMapperRepresentation fooMapper = new ProtocolMapperRepresentation();
        fooMapper.setName("foo");
        fooMapper.setProtocol("fooProtocol");
        fooMapper.setProtocolMapper("fooMapper");
        fooMapper.setConsentRequired(true);
        Response response = mappersResource.createMapper(fooMapper);
        String location = response.getLocation().toString();
        fooMapperId = location.substring(location.lastIndexOf("/") + 1);
        response.close();

        fooMapper = mappersResource.getMapperById(fooMapperId);
        assertEquals(fooMapper.getName(), "foo");

        // Update foo mapper
        fooMapper.setProtocolMapper("foo-mapper-updated");
        mappersResource.update(fooMapperId, fooMapper);

        fooMapper = mappersResource.getMapperById(fooMapperId);
        assertEquals(fooMapper.getProtocolMapper(), "foo-mapper-updated");

        // Remove foo mapper
        mappersResource.delete(fooMapperId);
        try {
            mappersResource.getMapperById(fooMapperId);
            fail("Not expected to find deleted mapper");
        } catch (NotFoundException nfe) {
        }
    }

    public static void assertClient(ClientRepresentation client, ClientRepresentation storedClient) {
        if (client.getClientId() != null) Assert.assertEquals(client.getClientId(), storedClient.getClientId());
        if (client.getName() != null) Assert.assertEquals(client.getName(), storedClient.getName());
        if (client.isEnabled() != null) Assert.assertEquals(client.isEnabled(), storedClient.isEnabled());
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
    }

}
