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

package org.keycloak.tests.admin.authz.fgap;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ClientScopePolicyRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.RegexPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.TimePolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.CLIENTS;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLES;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLES_COMPOSITE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@KeycloakIntegrationTest
public class ClientResourceTypeEvaluationTest extends AbstractPermissionTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @InjectClient(ref = "realmClient")
    ManagedClient realmClient;

    private final String clientsType = AdminPermissionsSchema.CLIENTS.getType();

    @Test
    public void testUnsupportedPolicyTypes() {
        assertSupportForPolicyType("resource", () -> getPermissionsResource(adminPermissionsClient).resource().create(new ResourcePermissionRepresentation()), false);
    }

    @Test
    public void testSupportedPolicyTypes() {
        assertSupportForPolicyType("scope", () -> getPermissionsResource(adminPermissionsClient).scope().create(PermissionBuilder.create()
                .resourceType(AdminPermissionsSchema.USERS.getType())
                .scopes(AdminPermissionsSchema.USERS.getScopes())
                .build()), true);
        assertSupportForPolicyType("user", () -> getPolicies().user().create(new UserPolicyRepresentation()), true);
        assertSupportForPolicyType("client", () -> getPolicies().client().create(new ClientPolicyRepresentation()), true);
        assertSupportForPolicyType("group", () -> getPolicies().group().create(new GroupPolicyRepresentation()), true);
        assertSupportForPolicyType("role", () -> getPolicies().role().create(new RolePolicyRepresentation()), true);
        assertSupportForPolicyType("aggregate", () -> getPolicies().aggregate().create(new AggregatePolicyRepresentation()), true);
        assertSupportForPolicyType("client-scope", () -> getPolicies().clientScope().create(new ClientScopePolicyRepresentation()), true);
        assertSupportForPolicyType("js", () -> getPolicies().js().create(new JSPolicyRepresentation()), true);
        assertSupportForPolicyType("regex", () -> getPolicies().regex().create(new RegexPolicyRepresentation()), true);
        assertSupportForPolicyType("time", () -> getPolicies().time().create(new TimePolicyRepresentation()), true);
    }

    private void assertSupportForPolicyType(String type, Supplier<Response> operation, boolean supported) {
        try (Response response = operation.get()) {
            assertPolicyEndpointResponse(type, supported, response);
        }

        PolicyRepresentation representation = new PolicyRepresentation();

        representation.setType(type);

        try (Response response = getPolicies().create(representation)) {
            assertPolicyEndpointResponse(type, supported, response);
        }
    }

    private void assertPolicyEndpointResponse(String type, boolean supported, Response response) {
        assertThat("Policy type [" + type + "] should be " + (supported ? "supported" : "unsupported"), Status.BAD_REQUEST.equals(Status.fromStatusCode(response.getStatus())), not(supported));
        assertThat("Policy type [" + type + "] should be " + (supported ? "supported" : "unsupported"), response.readEntity(String.class).contains("Policy type not supported by feature"), not(supported));
    }

    @Test
    public void testManageOnlyOneClient() {
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        ClientResource clientResource = realmAdminClient.realm(realm.getName()).clients().get(myclient.getId());

        // the following operations should fail as the permission wasn't granted yet
        try {
            clientResource.toRepresentation();
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
        try {
            myclient.setName("somethingNew");
            clientResource.update(myclient);
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
        try {
            ClientScopeRepresentation clientScopeRep = clientResource.getDefaultClientScopes().get(1);
            clientResource.removeDefaultClientScope(clientScopeRep.getId());
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, myclient.getId(), clientsType, Set.of(VIEW, MANAGE), onlyMyAdminUserPolicy);

        // the caller can view myclient
        clientResource.toRepresentation();

        // the caller can list myclient
        List<ClientRepresentation> allClients = realmAdminClient.realm(realm.getName()).clients().findAll();
        assertThat(allClients, hasSize(1));

        // can update myclient
        myclient.setName("somethingNew");
        clientResource.update(myclient);

        // can view client scopes
        List<ClientScopeRepresentation> defaultClientScopes = clientResource.getDefaultClientScopes();
        assertThat(defaultClientScopes, not(empty()));

        // can remove a default client scope
        ClientScopeRepresentation clientScopeRep = defaultClientScopes.get(1);
        clientResource.removeDefaultClientScope(clientScopeRep.getId());

        // can add an optional client scope
        clientResource.addOptionalClientScope(clientScopeRep.getId());

        // can't update a different client
        ClientRepresentation realmClientRep = realm.admin().clients().get(realmClient.getId()).toRepresentation();
        realmClientRep.setName("somethingNew");
        try {
            realmAdminClient.realm(realm.getName()).clients().get(realmClient.getId()).update(realmClientRep);
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
    }

    @Test
    public void testManageAllClients() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        ClientRepresentation newClient = new ClientRepresentation();
        newClient.setClientId("newClient");
        newClient.setProtocol("openid-connect");

        // the following operations should fail as the permission wasn't granted yet
        try (Response response = realmAdminClient.realm(realm.getName()).clients().create(newClient)) {
            Assertions.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        List<ClientRepresentation> found = realmAdminClient.realm(realm.getName()).clients().findAll();
        assertThat(found, empty());

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, clientsType, onlyMyAdminUserPolicy, Set.of(VIEW, MANAGE));

        // can create a new client
        realmAdminClient.realm(realm.getName()).clients().create(newClient).close();
        found = realmAdminClient.realm(realm.getName()).clients().findByClientId("newClient");
        assertThat(found, hasSize(1));

        // can delete
        realmAdminClient.realm(realm.getName()).clients().get(found.get(0).getId()).remove();
        found = realmAdminClient.realm(realm.getName()).clients().findByClientId("newClient");
        assertThat(found, empty());

        // can list & view all clients
        found = realmAdminClient.realm(realm.getName()).clients().findAll();
        assertThat(found, not(empty()));
    }

    @Test
    public void testClientScopeEvaluation() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        ClientRepresentation newClient = new ClientRepresentation();
        newClient.setClientId("newClient");
        newClient.setProtocol("openid-connect");

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, clientsType, onlyMyAdminUserPolicy, Set.of(VIEW, MANAGE));

        realmAdminClient.realm(realm.getName()).clients().create(newClient).close();
        List<ClientRepresentation> found = realmAdminClient.realm(realm.getName()).clients().findByClientId("newClient");
        assertThat(found, hasSize(1));

        UserRepresentation user = UserBuilder.create()
                .username(KeycloakModelUtils.generateId())
                .build();
        try (Response response = realm.admin().users().create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }

        ClientResource clientApi = realmAdminClient.realm(realm.getName()).clients().get(found.get(0).getId());

        try {
            clientApi.clientScopesEvaluate().generateAccessToken("openid", user.getId(), null);
            fail("no permissions to view the user.");
        } catch (ForbiddenException e) {
            assertEquals("You have no access to this user", e.getResponse().readEntity(OAuth2ErrorRepresentation.class).getError());
        }

        createPermission(adminPermissionsClient, user.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(VIEW), onlyMyAdminUserPolicy);
        clientApi.clientScopesEvaluate().generateAccessToken("openid", user.getId(), null);
    }

    @Test
    public void testViewAllClients() {
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        // the following operations should fail as the permission wasn't granted yet
        try {
            realmAdminClient.realm(realm.getName()).clients().get(myclient.getId()).getProtocolMappers().getMappers();
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        List<ClientRepresentation> found = realmAdminClient.realm(realm.getName()).clients().findAll(true);
        assertThat(found, empty());


        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, clientsType, onlyMyAdminUserPolicy, Set.of(VIEW));

        // can list and view all clients
        found = realmAdminClient.realm(realm.getName()).clients().findAll(true);
        assertThat(found, not(empty()));

        // can't create a protocol mapper
        ProtocolMapperRepresentation protocolMapperRep = new ProtocolMapperRepresentation();
        protocolMapperRep.setName("my-protocol-mapper");
        protocolMapperRep.setProtocol("openid-connect");
        protocolMapperRep.setProtocolMapper("oidc-hardcoded-claim-mapper");
        try (Response response = realmAdminClient.realm(realm.getName()).clients().get(myclient.getId()).getProtocolMappers().createMapper(protocolMapperRep)) {
            Assertions.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        // can get protocol mappers
        assertThat(realmAdminClient.realm(realm.getName()).clients().get(myclient.getId()).getProtocolMappers().getMappers(), empty());

        // can't create a new client
        try (Response response = realmAdminClient.realm(realm.getName()).clients().create(null)) {
            Assertions.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testCreateClientsRequireManageScope() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        ClientRepresentation newClient = new ClientRepresentation();

        newClient.setClientId(KeycloakModelUtils.generateId());

        // can't create a new client
        try (Response response = realmAdminClient.realm(realm.getName()).clients().create(newClient)) {
            Assertions.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        List<ClientRepresentation> found = realmAdminClient.realm(realm.getName()).clients().findAll(true);
        assertThat(found, empty());


        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        ScopePermissionRepresentation allPermission = createAllPermission(adminPermissionsClient, clientsType, onlyMyAdminUserPolicy, CLIENTS.getScopes().stream().filter(Predicate.not(MANAGE::equals)).collect(Collectors.toSet()));

        // can't create a new client
        try (Response response = realmAdminClient.realm(realm.getName()).clients().create(newClient)) {
            Assertions.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        // grants manage access
        allPermission = getScopePermissionsResource(adminPermissionsClient).findByName(allPermission.getName());
        allPermission.setScopes(Set.of(VIEW, MANAGE));
        getScopePermissionsResource(adminPermissionsClient).findById(allPermission.getId()).update(allPermission);

        // create clients is permission is granted to all clients
        try (Response response = realmAdminClient.realm(realm.getName()).clients().create(newClient)) {
            Assertions.assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }

        found = realmAdminClient.realm(realm.getName()).clients().findByClientId(newClient.getClientId());
        assertThat(found, not(empty()));
    }

    @Test
    public void testMapRolesOnlyOneClient() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);

        // create a role
        RoleRepresentation role = new RoleRepresentation();
        role.setName("myclient-role");
        role.setClientRole(true);
        realm.admin().clients().get(myclient.getId()).roles().create(role);
        role = realm.admin().clients().get(myclient.getId()).roles().get("myclient-role").toRepresentation();

        // the following operations should fail as the permission wasn't granted yet
        try {
            realmAdminClient.realm(realm.getName()).users().get(myadmin.getId()).roles().clientLevel(myclient.getId()).add(List.of(role));
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, myadmin.getId(), AdminPermissionsSchema.USERS_RESOURCE_TYPE, Set.of(MAP_ROLES), onlyMyAdminUserPolicy);
        createPermission(adminPermissionsClient, myclient.getId(), clientsType, Set.of(MAP_ROLES), onlyMyAdminUserPolicy);

        // now those should pass
        realmAdminClient.realm(realm.getName()).users().get(myadmin.getId()).roles().clientLevel(myclient.getId()).add(List.of(role));
    }

    @Test
    public void testMapCompositesToAnotherClientRole() {
        // to add a client role ('myclient') 'roleA' as a composite role of 'roleB' (client role of 'realmClient' client)
        // it is required to have permission to manage the `realmClient` and to map-roles-composite of the `myclient`
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);

        // create two roles, each for seprate client
        RoleRepresentation roleA = new RoleRepresentation();
        roleA.setName("roleA");
        roleA.setClientRole(true);
        realm.admin().clients().get(myclient.getId()).roles().create(roleA);
        roleA = realm.admin().clients().get(myclient.getId()).roles().get("roleA").toRepresentation();

        RoleRepresentation roleB = new RoleRepresentation();
        roleB.setName("roleB");
        roleB.setClientRole(true);
        realm.admin().clients().get(realmClient.getId()).roles().create(roleB);

        // the following operations should fail as the permission wasn't granted yet
        try {
            realmAdminClient.realm(realm.getName()).clients().get(realmClient.getId()).roles().get("roleB").addComposites(List.of(roleA));
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());

        createPermission(adminPermissionsClient, myclient.getId(), clientsType, Set.of(MAP_ROLES_COMPOSITE), onlyMyAdminUserPolicy);

        // the following operations should fail as the permission to manage the realmClient is missing
        try {
            realmAdminClient.realm(realm.getName()).clients().get(realmClient.getId()).roles().get("roleB").addComposites(List.of(roleA));
            fail("Expected exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        createPermission(adminPermissionsClient, realmClient.getId(), clientsType, Set.of(MANAGE), onlyMyAdminUserPolicy);

        // now those should pass
        realmAdminClient.realm(realm.getName()).clients().get(realmClient.getId()).roles().get("roleB").addComposites(List.of(roleA));
    }

    @Test
    public void testEvaluateAllResourcePermissionsForSpecificResourcePermission() {
        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);
        UserRepresentation adminUser = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin", adminUser.getId());
        ScopePermissionRepresentation allResourcesPermission = createAllPermission(adminPermissionsClient, clientsType, allowPolicy, Set.of(MANAGE, MAP_ROLES));
        // all resource permissions grants manage scope
        ClientsResource clients = realmAdminClient.realm(realm.getName()).clients();
        clients.get(myclient.getId()).update(myclient);

        ScopePermissionRepresentation resourcePermission = createPermission(adminPermissionsClient, myclient.getId(), clientsType, Set.of(MANAGE), allowPolicy);
        // both all and specific resource permission grants manage scope
        clients.get(myclient.getId()).update(myclient);

        allResourcesPermission = getScopePermissionsResource(adminPermissionsClient).findByName(allResourcesPermission.getName());
        allResourcesPermission.setScopes(Set.of(MAP_ROLES));
        getScopePermissionsResource(adminPermissionsClient).findById(allResourcesPermission.getId()).update(allResourcesPermission);
        // all resource permission does not have the manage scope but the scope is granted by the resource permission
        clients.get(myclient.getId()).update(myclient);

        resourcePermission = getScopePermissionsResource(adminPermissionsClient).findByName(resourcePermission.getName());
        resourcePermission.setScopes(Set.of(MAP_ROLES));
        getScopePermissionsResource(adminPermissionsClient).findById(resourcePermission.getId()).update(resourcePermission);
        try {
            // neither the all and specific resource permission grants access to the manage scope
            clients.get(myclient.getId()).update(myclient);
            Assertions.fail("Expected Exception wasn't thrown.");
        } catch (ForbiddenException expected) {}

        allResourcesPermission.setScopes(Set.of(MANAGE));
        getScopePermissionsResource(adminPermissionsClient).findById(allResourcesPermission.getId()).update(allResourcesPermission);
        // all resource permission grants access again to manage
        clients.get(myclient.getId()).update(myclient);

        UserPolicyRepresentation notAllowPolicy = createUserPolicy(Logic.NEGATIVE, realm, adminPermissionsClient, "Not My Admin", adminUser.getId());
        createPermission(adminPermissionsClient, myclient.getId(), clientsType, Set.of(MANAGE), notAllowPolicy);
        try {
            // a specific resource permission that explicitly negates access to the manage scope denies access to the scope
            clients.get(myclient.getId()).update(myclient);
            Assertions.fail("Expected Exception wasn't thrown.");
        } catch (ForbiddenException expected) {}

        resourcePermission = getScopePermissionsResource(adminPermissionsClient).findByName(resourcePermission.getName());
        resourcePermission.setScopes(Set.of(MAP_ROLES, MANAGE));
        getScopePermissionsResource(adminPermissionsClient).findById(resourcePermission.getId()).update(resourcePermission);
        try {
            // the specific resource permission that explicitly negates access to the manage scope denies access to the scope
            // even though there is another resource permission that grants access to the scope - conflict resolution denies by default
            clients.get(myclient.getId()).update(myclient);
            Assertions.fail("Expected Exception wasn't thrown.");
        } catch (ForbiddenException expected) {}
    }

    @Test
    public void testManageClientWithAuthorizationSettings() {
        ClientRepresentation myResourceServer = realm.admin().clients().findByClientId("myresourceserver").get(0);
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        ClientResource clientResource = realmAdminClient.realm(realm.getName()).clients().get(myResourceServer.getId());
        UserPolicyRepresentation onlyMyAdminUserPolicy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, myResourceServer.getId(), clientsType, Set.of(VIEW, MANAGE), onlyMyAdminUserPolicy);

        // can update myResourceServer because manage also implies managing authorization service settings
        myResourceServer.setName("somethingNew");
        clientResource.update(myResourceServer);
    }
}
