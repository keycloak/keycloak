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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ScopePermissionResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
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

@KeycloakIntegrationTest
public class UserResourceTypePermissionTest extends AbstractPermissionTest {

    @InjectUser(ref = "alice", lifecycle = LifeCycle.METHOD)
    ManagedUser userAlice;

    @InjectUser(ref = "bob")
    ManagedUser userBob;

    @BeforeEach
    public void onBefore() {
        for (int i = 0; i < 3; i++) {
            UserPolicyRepresentation policy = new UserPolicyRepresentation();

            policy.setName("User Policy " + i);

            client.admin().authorization().policies().user().create(policy).close();
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
        assertEquals(1, permission.resources().size());
        assertEquals(AdminPermissionsSchema.USERS_RESOURCE_TYPE, permission.resources().get(0).getDisplayName());
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
        assertEquals(3, permission.associatedPolicies().size());
        assertEquals(1, permission.resources().size());
        assertEquals(userAlice.getUsername(), permission.resources().get(0).getDisplayName());

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

        //check the resource was removed from policies
        UserPolicyRepresentation userPolicy = getPolicies().user().findByName("Only Alice or Bob User Policy");
        assertThat(userPolicy, notNullValue());
        assertThat(userPolicy.getUsers(), not(contains(userAlice.getId())));

        UserPolicyRepresentation userPolicy1 = getPolicies().user().findByName("Only Alice User Policy");
        assertThat(userPolicy1, notNullValue());
        assertThat(userPolicy1.getUsers(), empty());

        //there should be 1 permission left
        permissions = getScopePermissionsResource(client).findAll(null, null, null, null, null);
        assertThat(permissions, hasSize(1));
        assertThat(getPolicies().policy(permissions.get(0).getId()).resources().stream().map(ResourceRepresentation::getName).collect(Collectors.toList()), not(hasItem(userAlice.getId())));
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
