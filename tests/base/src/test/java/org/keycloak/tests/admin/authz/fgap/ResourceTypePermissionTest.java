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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ScopePermissionResource;
import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.authorization.AdminPermissionsSchema;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.util.ApiUtil;

@KeycloakIntegrationTest(config = KeycloakAdminPermissionsServerConfig.class)
public class ResourceTypePermissionTest extends AbstractPermissionTest {

    @InjectUser(ref = "alice", lifecycle = LifeCycle.METHOD)
    ManagedUser userAlice;

    @InjectUser(ref = "bob")
    ManagedUser userBob;

    @InjectClient(ref = "testClient")
    ManagedClient testClient;

    @InjectClient(ref = "testClient2", lifecycle = LifeCycle.METHOD)
    ManagedClient testClient2;

    @BeforeEach
    public void onBefore() {
        for (int i = 0; i < 3; i++) {
            UserPolicyRepresentation policy = new UserPolicyRepresentation();

            policy.setName("User Policy " + i);

            client.admin().authorization().policies().user().create(policy).close();
        }
    }

    @AfterEach
    public void onAfter() {
        ScopePermissionsResource permissions = getScopePermissionsResource(client);

        for (ScopePermissionRepresentation permission : permissions.findAll(null, null, null, -1, -1)) {
            permissions.findById(permission.getId()).remove();
        }
    }

    @Test
    public void testCreateResourceTypePermission() {
        ScopePermissionRepresentation expected = createAllUserPermission();
        List<ScopePermissionRepresentation> result = getScopePermissionsResource(client).findAll(null, null, null, -1, -1);
        assertEquals(1, result.size());
        ScopePermissionRepresentation permissionRep = result.get(0);
        ScopePermissionResource permission = getScopePermissionsResource(client).findById(permissionRep.getId());
        assertEquals(expected.getName(), permissionRep.getName());
        assertEquals(AdminPermissionsSchema.USERS.getScopes().size(), permission.scopes().size());
        assertEquals(3, permission.associatedPolicies().size());
    }

    @Test
    public void testCreateResourceObjectPermission() {
        ScopePermissionRepresentation expected = createUserPermission(userAlice);
        List<ScopePermissionRepresentation> result = getScopePermissionsResource(client).findAll(null, null, null, -1, -1);
        assertEquals(1, result.size());
        ScopePermissionRepresentation permissionRep = result.get(0);
        ScopePermissionResource permission = getScopePermissionsResource(client).findById(permissionRep.getId());
        assertEquals(expected.getName(), permissionRep.getName());
        assertEquals(AdminPermissionsSchema.USERS.getScopes().size(), permission.scopes().size());
        assertEquals(1, permission.resources().size());
        assertEquals(3, permission.associatedPolicies().size());
    }

    @Test
    public void testFindByResourceObject() {
        createUserPermission(userAlice, userBob);

        List<ScopePermissionRepresentation> existing = getScopePermissionsResource(client).findAll(null, null, userAlice.getId(), -1, -1);
        assertEquals(1, existing.size());
        existing = getScopePermissionsResource(client).findAll(null, null, userBob.getId(), -1, -1);
        assertEquals(1, existing.size());
    }

    @Test
    public void testDelete() {
        // only "all-resource" resources should be present
        List<ResourceRepresentation> resources = client.admin().authorization().resources().find(null, null, null, null, null, null, null);
        assertEquals(AdminPermissionsSchema.SCHEMA.getResourceTypes().entrySet().size(), resources.size());

        createUserPermission(userAlice);
        // resource for Alice should be created
        resources = client.admin().authorization().resources().find(null, null, null, null, null, null, null);
        assertEquals(1 + AdminPermissionsSchema.SCHEMA.getResourceTypes().entrySet().size(), resources.size());

        createUserPermission(userBob);
        // resource for Bob should be created
        resources = client.admin().authorization().resources().find(null, null, null, null, null, null, null);
        assertEquals(2 + AdminPermissionsSchema.SCHEMA.getResourceTypes().entrySet().size(), resources.size());

        List<ScopePermissionRepresentation> existing = getScopePermissionsResource(client).findAll(null, null, userAlice.getId(), -1, -1);
        assertEquals(1, existing.size());
        // remove permission for Alice
        getScopePermissionsResource(client).findById(existing.get(0).getId()).remove();
        existing = getScopePermissionsResource(client).findAll(null, null, userAlice.getId(), -1, -1);
        assertThat(existing, nullValue());

        existing = getScopePermissionsResource(client).findAll(null, null, userBob.getId(), -1, -1);
        assertEquals(1, existing.size());

        // remove permission for Bob
        getScopePermissionsResource(client).findById(existing.get(0).getId()).remove();

        //resources for both Alice and Bob should be deleted, there should be only "all-resource" resources
        resources = client.admin().authorization().resources().find(null, null, null, null, null, null, null);
        assertEquals(AdminPermissionsSchema.SCHEMA.getResourceTypes().entrySet().size(), resources.size());
    }

    @Test
    public void testUpdate() {
        createUserPermission(userAlice, userBob);

        List<ScopePermissionRepresentation> searchByResourceAlice = getScopePermissionsResource(client).findAll(null, null, userAlice.getId(), -1, -1);
        assertThat(searchByResourceAlice, hasSize(1));
        List<ScopePermissionRepresentation> searchByResourceBob = getScopePermissionsResource(client).findAll(null, null, userBob.getId(), -1, -1);
        assertThat(searchByResourceBob, hasSize(1));

        ScopePermissionRepresentation permission = searchByResourceAlice.get(0);

        // verify it's the same permission
        assertThat(permission.getId(), equalTo(searchByResourceBob.get(0).getId()));

        // get resources assiciated with the permission
        List<ResourceRepresentation> resources = getPolicies().policy(permission.getId()).resources();
        assertThat(resources, hasSize(2));

        // update permission so that contains single resource
        ResourceRepresentation toRemove = resources.get(1);
        resources.remove(toRemove);
        permission.setResources(resources.stream().map(ResourceRepresentation::getId).collect(Collectors.toSet()));
        getScopePermissionsResource(client).findById(permission.getId()).update(permission);

        //permission should have only single resource
        assertThat(getPolicies().policy(permission.getId()).resources(), hasSize(1));
        try {
            // resource removed from permission should be removed from server as it is not assigned to any permission
            client.admin().authorization().resources().resource(toRemove.getId()).toRepresentation();
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(NotFoundException.class));
        }
    }

    @Test
    public void testUpdatePermissionResources() {
        AuthorizationResource authorization = client.admin().authorization();
        ScopePermissionRepresentation representation = createAllUserPermission();
        representation = getScopePermissionsResource(client).findByName(representation.getName());
        assertThat(representation, notNullValue());
        List<ResourceRepresentation> resources = authorization.resources().resources();
        assertThat(resources.size(), is(AdminPermissionsSchema.SCHEMA.getResourceTypes().size()));
        ScopePermissionResource permission = authorization.permissions().scope().findById(representation.getId());
        List<ResourceRepresentation> permissionResources = permission.resources();
        assertThat(permissionResources.size(), is(1));
        assertThat(permissionResources.get(0).getName(), is(AdminPermissionsSchema.USERS.getType()));

        representation.setResources(Set.of(userAlice.getId()));
        permission.update(representation);
        resources = authorization.resources().resources();
        assertThat(resources.size(), is(AdminPermissionsSchema.SCHEMA.getResourceTypes().size() + 1));
        permissionResources = permission.resources();
        assertThat(permissionResources.size(), is(1));
        assertThat(permissionResources.get(0).getName(), is(userAlice.getId()));

        representation.setResources(Set.of());
        permission.update(representation);
        resources = authorization.resources().resources();
        assertThat(resources.size(), is(AdminPermissionsSchema.SCHEMA.getResourceTypes().size()));
        permissionResources = permission.resources();
        assertThat(permissionResources.size(), is(1));
        assertThat(permissionResources.get(0).getName(), is(AdminPermissionsSchema.USERS.getType()));

        representation.setResources(Set.of(userAlice.getId()));
        permission.update(representation);
        resources = authorization.resources().resources();
        assertThat(resources.size(), is(AdminPermissionsSchema.SCHEMA.getResourceTypes().size() + 1));
        permissionResources = permission.resources();
        assertThat(permissionResources.size(), is(1));
        assertThat(permissionResources.get(0).getName(), is(userAlice.getId()));

        createUserPermission(userAlice, userBob);

        representation.setResources(Set.of());
        permission.update(representation);
        resources = authorization.resources().resources();
        assertThat(resources.size(), is(AdminPermissionsSchema.SCHEMA.getResourceTypes().size() + 2));
        permissionResources = permission.resources();
        assertThat(permissionResources.size(), is(1));
        assertThat(permissionResources.get(0).getName(), is(AdminPermissionsSchema.USERS.getType()));
    }

    @Test
    public void testRemoveUser() {
        //create user policies
        createUserPolicy(realm, client, "Only Alice or Bob User Policy", userAlice.getId(), userBob.getId());
        createUserPolicy(realm, client, "Only Alice User Policy", userAlice.getId());

        //create user permissions
        createUserPermission(userAlice, userBob);
        createUserPermission(userAlice);

        List<PolicyRepresentation> policies = getPolicies().policies(null, "Only", "user", null, null, null, null, null, null, null);
        assertThat(policies, hasSize(2));
        assertThat(policies.get(0).getConfig().get("users"), containsString(userAlice.getId()));
        assertThat(policies.get(1).getConfig().get("users"), containsString(userAlice.getId()));

        List<ScopePermissionRepresentation> permissions = getScopePermissionsResource(client).findAll(null, null, null, null, null);
        assertThat(permissions, hasSize(2));
        assertThat(getPolicies().policy(permissions.get(0).getId()).resources().stream().map(ResourceRepresentation::getName).collect(Collectors.toList()), hasItem(userAlice.getId()));
        assertThat(getPolicies().policy(permissions.get(1).getId()).resources().stream().map(ResourceRepresentation::getName).collect(Collectors.toList()), hasItem(userAlice.getId()));

        //remove user
        realm.admin().users().get(userAlice.getId()).remove();

        //there should be 1 policy left
        policies = getPolicies().policies(null, "Only", "user", null, null, null, null, null, null, null);
        assertThat(policies, hasSize(1));
        assertThat(policies.get(0).getConfig().get("users"), not(containsString(userAlice.getId())));

        //there should be 1 permission left
        permissions = getScopePermissionsResource(client).findAll(null, null, null, null, null);
        assertThat(permissions, hasSize(1));
        assertThat(getPolicies().policy(permissions.get(0).getId()).resources().stream().map(ResourceRepresentation::getName).collect(Collectors.toList()), not(hasItem(userAlice.getId())));
    }

    @Test
    public void testRemoveClient() {
        //create client policies
        createClientPolicy(realm, client, "Only testClient or testClient2 Client Policy", testClient.getId(), testClient2.getId());
        createClientPolicy(realm, client, "Only testClient2 Client Policy", testClient2.getId());

        //create client permissions
        createClientPermission(testClient, testClient2);
        createClientPermission(testClient2);

        List<PolicyRepresentation> policies = getPolicies().policies(null, "Only", "client", null, null, null, null, null, null, null);
        assertThat(policies, hasSize(2));
        assertThat(policies.get(0).getConfig().get("clients"), containsString(testClient2.getId()));
        assertThat(policies.get(1).getConfig().get("clients"), containsString(testClient2.getId()));

        List<ScopePermissionRepresentation> permissions = getScopePermissionsResource(client).findAll(null, null, null, null, null);
        assertThat(permissions, hasSize(2));
        assertThat(getPolicies().policy(permissions.get(0).getId()).resources().stream().map(ResourceRepresentation::getName).collect(Collectors.toList()), hasItem(testClient2.getId()));
        assertThat(getPolicies().policy(permissions.get(1).getId()).resources().stream().map(ResourceRepresentation::getName).collect(Collectors.toList()), hasItem(testClient2.getId()));

        //remove client
        realm.admin().clients().get(testClient2.getId()).remove();

        //there should be 1 policy left
        policies = getPolicies().policies(null, "Only", "client", null, null, null, null, null, null, null);
        assertThat(policies, hasSize(1));
        assertThat(policies.get(0).getConfig().get("clients"), not(containsString(testClient2.getId())));

        //there should be 1 permission left
        permissions = getScopePermissionsResource(client).findAll(null, null, null, null, null);
        assertThat(permissions, hasSize(1));
        assertThat(getPolicies().policy(permissions.get(0).getId()).resources().stream().map(ResourceRepresentation::getName).collect(Collectors.toList()), not(hasItem(testClient2.getId())));
    }

    @Test
    public void testRemoveGroup() {
        //create groups
        GroupRepresentation topGroup = new GroupRepresentation();
        topGroup.setName("topGroup");
        try (Response response = realm.admin().groups().add(topGroup)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            topGroup.setId(ApiUtil.handleCreatedResponse(response));
            realm.cleanup().add(r -> r.groups().group(topGroup.getId()).remove());
        }
        GroupRepresentation topGroup1 = new GroupRepresentation();
        topGroup1.setName("topGroup1");
        try (Response response = realm.admin().groups().add(topGroup1)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            topGroup1.setId(ApiUtil.handleCreatedResponse(response));
        }

        //create group policies
        createGroupPolicy("Only topGroup or topGroup1 Group Policy", topGroup.getId(), topGroup1.getId());
        createGroupPolicy("Only topGroup1 Group Policy", topGroup1.getId());

        //create group permissions
        createGroupPermission(topGroup, topGroup1);
        createGroupPermission(topGroup1);
        
        List<PolicyRepresentation> policies = getPolicies().policies(null, "Only", "group", null, null, null, null, null, null, null);
        assertThat(policies, hasSize(2));
        assertThat(policies.get(0).getConfig().get("groups"), containsString(topGroup.getId()));
        assertThat(policies.get(1).getConfig().get("groups"), containsString(topGroup1.getId()));

        List<ScopePermissionRepresentation> permissions = getScopePermissionsResource(client).findAll(null, null, null, null, null);
        assertThat(permissions, hasSize(2));
        assertThat(getPolicies().policy(permissions.get(0).getId()).resources().stream().map(ResourceRepresentation::getName).collect(Collectors.toList()), hasItem(topGroup.getId()));
        assertThat(getPolicies().policy(permissions.get(1).getId()).resources().stream().map(ResourceRepresentation::getName).collect(Collectors.toList()), hasItem(topGroup1.getId()));

        //remove group
        realm.admin().groups().group(topGroup1.getId()).remove();

        //there should be 1 policy left
        policies = getPolicies().policies(null, "Only", "group", null, null, null, null, null, null, null);
        assertThat(policies, hasSize(1));
        assertThat(policies.get(0).getConfig().get("groups"), not(containsString(topGroup1.getId())));

        //there should be 1 permission left
        permissions = getScopePermissionsResource(client).findAll(null, null, null, null, null);
        assertThat(permissions, hasSize(1));
        assertThat(getPolicies().policy(permissions.get(0).getId()).resources().stream().map(ResourceRepresentation::getName).collect(Collectors.toList()), not(hasItem(topGroup1.getId())));
    }

    @Test
    public void testRemoveRole() {
        //create roles
        RoleRepresentation realmRole = new RoleRepresentation();
        realmRole.setName("realmRole");
        try {
            realm.admin().roles().create(realmRole);
            realmRole.setId(realm.admin().roles().get(realmRole.getName()).toRepresentation().getId());
        } finally {
            realm.cleanup().add(r -> r.roles().deleteRole(realmRole.getName()));
        }

        RoleRepresentation clientRole = new RoleRepresentation();
        clientRole.setName("clientRole");
        clientRole.setClientRole(Boolean.TRUE);
        clientRole.setContainerId(testClient.getId());
        realm.admin().roles().create(clientRole);
        clientRole.setId(realm.admin().roles().get(clientRole.getName()).toRepresentation().getId());

        //create role policies
        createRolePolicy("Only realmRole or clientRole Role Policy", realmRole.getId(), clientRole.getId());
        createRolePolicy("Only clientRole Role Policy", clientRole.getId());

        //create role permissions
        createRolePermission(realmRole, clientRole);
        createRolePermission(clientRole);
        
        List<PolicyRepresentation> policies = getPolicies().policies(null, "Only", "role", null, null, null, null, null, null, null);
        assertThat(policies, hasSize(2));
        assertThat(policies.get(0).getConfig().get("roles"), containsString(clientRole.getId()));
        assertThat(policies.get(1).getConfig().get("roles"), containsString(clientRole.getId()));

        List<ScopePermissionRepresentation> permissions = getScopePermissionsResource(client).findAll(null, null, null, null, null);
        assertThat(permissions, hasSize(2));
        assertThat(getPolicies().policy(permissions.get(0).getId()).resources().stream().map(ResourceRepresentation::getName).collect(Collectors.toList()), hasItem(clientRole.getId()));
        assertThat(getPolicies().policy(permissions.get(1).getId()).resources().stream().map(ResourceRepresentation::getName).collect(Collectors.toList()), hasItem(clientRole.getId()));

        //remove role
        realm.admin().roles().get(clientRole.getName()).remove();

        //there should be 1 policy left
        policies = getPolicies().policies(null, "Only", "role", null, null, null, null, null, null, null);
        assertThat(policies, hasSize(1));
        assertThat(policies.get(0).getConfig().get("roles"), not(containsString(clientRole.getId())));

        //there should be 1 permission left
        permissions = getScopePermissionsResource(client).findAll(null, null, null, null, null);
        assertThat(permissions, hasSize(1));
        assertThat(getPolicies().policy(permissions.get(0).getId()).resources().stream().map(ResourceRepresentation::getName).collect(Collectors.toList()), not(hasItem(clientRole.getId())));
    }

    private ScopePermissionRepresentation createUserPermission(ManagedUser... users) {
        ScopePermissionRepresentation permission = PermissionBuilder.create()
                .resourceType(AdminPermissionsSchema.USERS.getType())
                .resources(Arrays.stream(users).map(ManagedUser::getUsername).collect(Collectors.toSet()))
                .scopes(AdminPermissionsSchema.USERS.getScopes())
                .addPolicies(List.of("User Policy 0", "User Policy 1", "User Policy 2"))
                .build();

        createPermission(client, permission);

        return permission;
    }

    private ScopePermissionRepresentation createGroupPermission(GroupRepresentation... groups) {
        ScopePermissionRepresentation permission = PermissionBuilder.create()
                .resourceType(AdminPermissionsSchema.GROUPS.getType())
                .resources(Arrays.stream(groups).map(GroupRepresentation::getId).collect(Collectors.toSet()))
                .scopes(AdminPermissionsSchema.GROUPS.getScopes())
                .addPolicies(List.of("User Policy 0", "User Policy 1"))
                .build();

        createPermission(client, permission);

        return permission;
    }

    private ScopePermissionRepresentation createRolePermission(RoleRepresentation... roles) {
        ScopePermissionRepresentation permission = PermissionBuilder.create()
                .resourceType(AdminPermissionsSchema.ROLES.getType())
                .resources(Arrays.stream(roles).map(RoleRepresentation::getId).collect(Collectors.toSet()))
                .scopes(AdminPermissionsSchema.ROLES.getScopes())
                .addPolicies(List.of("User Policy 0", "User Policy 1"))
                .build();

        createPermission(client, permission);

        return permission;
    }

    private GroupPolicyRepresentation createGroupPolicy(String name, String... groupIds) {
        GroupPolicyRepresentation policy = new GroupPolicyRepresentation();
        policy.setName(name);
        policy.addGroup(groupIds);
        policy.setLogic(Logic.POSITIVE);
        try (Response response = client.admin().authorization().policies().group().create(policy)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            realm.cleanup().add(r -> {
                GroupPolicyRepresentation groupPolicy = client.admin().authorization().policies().group().findByName(name);
                if (groupPolicy != null) {
                    client.admin().authorization().policies().group().findById(groupPolicy.getId()).remove();
                }
            });
        }
        return policy;
    }

    private RolePolicyRepresentation createRolePolicy(String name, String... roleIds) {
        RolePolicyRepresentation policy = new RolePolicyRepresentation();
        policy.setName(name);
        for (String roleId : roleIds) {
            policy.addRole(roleId);
        }
        policy.setLogic(Logic.POSITIVE);
        try (Response response = client.admin().authorization().policies().role().create(policy)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            realm.cleanup().add(r -> {
                RolePolicyRepresentation rolePolicy = client.admin().authorization().policies().role().findByName(name);
                if (rolePolicy != null) {
                    client.admin().authorization().policies().role().findById(rolePolicy.getId()).remove();
                }
            });
        }
        return policy;
    }

    private ScopePermissionRepresentation createClientPermission(ManagedClient... clients) {
        ScopePermissionRepresentation permission = PermissionBuilder.create()
                .resourceType(AdminPermissionsSchema.CLIENTS.getType())
                .resources(Arrays.stream(clients).map(ManagedClient::getClientId).collect(Collectors.toSet()))
                .scopes(AdminPermissionsSchema.CLIENTS.getScopes())
                .addPolicies(List.of("User Policy 0", "User Policy 1"))
                .build();

        createPermission(client, permission);

        return permission;
    }

    private ScopePermissionRepresentation createAllUserPermission() {
        ScopePermissionRepresentation permission = PermissionBuilder.create()
                .resourceType(AdminPermissionsSchema.USERS.getType())
                .scopes(AdminPermissionsSchema.USERS.getScopes())
                .addPolicies(List.of("User Policy 0", "User Policy 1", "User Policy 2"))
                .build();

        createPermission(client, permission);

        return permission;
    }
}
