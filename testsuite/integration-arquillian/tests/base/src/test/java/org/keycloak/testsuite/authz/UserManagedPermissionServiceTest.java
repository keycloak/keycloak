/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.authz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.authorization.model.Policy.FilterOption.OWNER;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.NotFoundException;

import org.junit.Test;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.resource.AuthorizationResource;
import org.keycloak.authorization.client.resource.PolicyResource;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.PermissionResponse;
import org.keycloak.representations.idm.authorization.PermissionTicketRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.UmaPermissionRepresentation;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.GroupBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.RolesBuilder;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class UserManagedPermissionServiceTest extends AbstractResourceServerTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create().name(REALM_NAME)
                .roles(RolesBuilder.create()
                        .realmRole(RoleBuilder.create().name("uma_authorization").build())
                        .realmRole(RoleBuilder.create().name("uma_protection").build())
                        .realmRole(RoleBuilder.create().name("role_a").build())
                        .realmRole(RoleBuilder.create().name("role_b").build())
                        .realmRole(RoleBuilder.create().name("role_c").build())
                        .realmRole(RoleBuilder.create().name("role_d").build())
                )
                .group(GroupBuilder.create().name("group_a")
                        .subGroups(Arrays.asList(GroupBuilder.create().name("group_b").build()))
                        .build())
                .group(GroupBuilder.create().name("group_c").build())
                .group(GroupBuilder.create().name("group_remove").build())
                .user(UserBuilder.create().username("marta").password("password")
                        .addRoles("uma_authorization", "uma_protection")
                        .role("resource-server-test", "uma_protection"))
                .user(UserBuilder.create().username("alice").password("password")
                        .addRoles("uma_authorization", "uma_protection")
                        .role("resource-server-test", "uma_protection"))
                .user(UserBuilder.create().username("kolo").password("password")
                        .addRoles("role_a")
                        .addGroups("group_a"))
                .client(ClientBuilder.create().clientId("resource-server-test")
                        .secret("secret")
                        .authorizationServicesEnabled(true)
                        .redirectUris("http://localhost/resource-server-test")
                        .defaultRoles("uma_protection")
                        .directAccessGrants()
                        .serviceAccountsEnabled(true))
                .client(ClientBuilder.create().clientId("client-a")
                        .redirectUris("http://localhost/resource-server-test")
                        .publicClient())
                .build());
    }

    private void testCreate() {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setName("Resource A");
        resource.setOwnerManagedAccess(true);
        resource.setOwner("marta");
        resource.addScope("Scope A", "Scope B", "Scope C");

        resource = getAuthzClient().protection().resource().create(resource);

        UmaPermissionRepresentation newPermission = new UmaPermissionRepresentation();

        newPermission.setName("Custom User-Managed Permission");
        newPermission.setDescription("Users from specific roles are allowed to access");
        newPermission.addScope("Scope A", "Scope B", "Scope C");
        newPermission.addRole("role_a", "role_b", "role_c", "role_d");
        newPermission.addGroup("/group_a", "/group_a/group_b", "/group_c");
        newPermission.addClient("client-a", "resource-server-test");
        
        if (Profile.isFeatureEnabled(Profile.Feature.UPLOAD_SCRIPTS)) {
            newPermission.setCondition("$evaluation.grant()");
        }

        newPermission.addUser("kolo");

        ProtectionResource protection = getAuthzClient().protection("marta", "password");

        UmaPermissionRepresentation permission = protection.policy(resource.getId()).create(newPermission);

        assertEquals(newPermission.getName(), permission.getName());
        assertEquals(newPermission.getDescription(), permission.getDescription());
        assertNotNull(permission.getScopes());
        assertTrue(permission.getScopes().containsAll(newPermission.getScopes()));
        assertNotNull(permission.getRoles());
        assertTrue(permission.getRoles().containsAll(newPermission.getRoles()));
        assertNotNull(permission.getGroups());
        assertTrue(permission.getGroups().containsAll(newPermission.getGroups()));
        assertNotNull(permission.getClients());
        assertTrue(permission.getClients().containsAll(newPermission.getClients()));
        assertEquals(newPermission.getCondition(), permission.getCondition());
        assertNotNull(permission.getUsers());
        assertTrue(permission.getUsers().containsAll(newPermission.getUsers()));
    }

    @Test
    public void testCreateDeprecatedFeaturesEnabled() {
        testCreate();
    }

    @Test
    @DisableFeature(value = Profile.Feature.UPLOAD_SCRIPTS, skipRestart = true)
    public void testCreateDeprecatedFeaturesDisabled() {
        testCreate();
    }

    private void testUpdate() {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setName("Resource A");
        resource.setOwnerManagedAccess(true);
        resource.setOwner("marta");
        resource.addScope("Scope A", "Scope B", "Scope C");

        resource = getAuthzClient().protection().resource().create(resource);

        UmaPermissionRepresentation permission = new UmaPermissionRepresentation();

        permission.setName("Custom User-Managed Permission");
        permission.setDescription("Users from specific roles are allowed to access");
        permission.addScope("Scope A");
        permission.addRole("role_a");

        ProtectionResource protection = getAuthzClient().protection("marta", "password");

        permission = protection.policy(resource.getId()).create(permission);

        assertEquals(1, getAssociatedPolicies(permission).size());

        permission.setName("Changed");
        permission.setDescription("Changed");

        protection.policy(resource.getId()).update(permission);

        UmaPermissionRepresentation updated = protection.policy(resource.getId()).findById(permission.getId());

        assertEquals(permission.getName(), updated.getName());
        assertEquals(permission.getDescription(), updated.getDescription());

        permission.removeRole("role_a");
        permission.addRole("role_b", "role_c");

        protection.policy(resource.getId()).update(permission);
        assertEquals(1, getAssociatedPolicies(permission).size());
        updated = protection.policy(resource.getId()).findById(permission.getId());

        assertTrue(permission.getRoles().containsAll(updated.getRoles()));

        permission.addRole("role_d");

        protection.policy(resource.getId()).update(permission);
        assertEquals(1, getAssociatedPolicies(permission).size());
        updated = protection.policy(resource.getId()).findById(permission.getId());

        assertTrue(permission.getRoles().containsAll(updated.getRoles()));

        permission.addGroup("/group_a/group_b");

        protection.policy(resource.getId()).update(permission);
        assertEquals(2, getAssociatedPolicies(permission).size());
        updated = protection.policy(resource.getId()).findById(permission.getId());

        assertTrue(permission.getGroups().containsAll(updated.getGroups()));

        permission.addGroup("/group_a");

        protection.policy(resource.getId()).update(permission);
        assertEquals(2, getAssociatedPolicies(permission).size());
        updated = protection.policy(resource.getId()).findById(permission.getId());

        assertTrue(permission.getGroups().containsAll(updated.getGroups()));

        permission.removeGroup("/group_a/group_b");
        permission.addGroup("/group_c");

        protection.policy(resource.getId()).update(permission);
        assertEquals(2, getAssociatedPolicies(permission).size());
        updated = protection.policy(resource.getId()).findById(permission.getId());

        assertTrue(permission.getGroups().containsAll(updated.getGroups()));

        permission.addClient("client-a");

        protection.policy(resource.getId()).update(permission);
        assertEquals(3, getAssociatedPolicies(permission).size());
        updated = protection.policy(resource.getId()).findById(permission.getId());

        assertTrue(permission.getClients().containsAll(updated.getClients()));

        permission.addClient("resource-server-test");

        protection.policy(resource.getId()).update(permission);
        assertEquals(3, getAssociatedPolicies(permission).size());
        updated = protection.policy(resource.getId()).findById(permission.getId());

        assertTrue(permission.getClients().containsAll(updated.getClients()));

        permission.removeClient("client-a");

        protection.policy(resource.getId()).update(permission);
        assertEquals(3, getAssociatedPolicies(permission).size());
        updated = protection.policy(resource.getId()).findById(permission.getId());

        assertTrue(permission.getClients().containsAll(updated.getClients()));

        if (Profile.isFeatureEnabled(Profile.Feature.UPLOAD_SCRIPTS)) {
            permission.setCondition("$evaluation.grant()");

            protection.policy(resource.getId()).update(permission);
            assertEquals(4, getAssociatedPolicies(permission).size());
            updated = protection.policy(resource.getId()).findById(permission.getId());

            assertEquals(permission.getCondition(), updated.getCondition());
        }

        permission.addUser("alice");

        protection.policy(resource.getId()).update(permission);
        
        int expectedPolicies = Profile.isFeatureEnabled(Profile.Feature.UPLOAD_SCRIPTS) ? 5 : 4;
        
        assertEquals(expectedPolicies, getAssociatedPolicies(permission).size());
        updated = protection.policy(resource.getId()).findById(permission.getId());
        assertEquals(1, updated.getUsers().size());
        assertEquals(permission.getUsers(), updated.getUsers());

        permission.addUser("kolo");

        protection.policy(resource.getId()).update(permission);
        assertEquals(expectedPolicies, getAssociatedPolicies(permission).size());
        updated = protection.policy(resource.getId()).findById(permission.getId());
        assertEquals(2, updated.getUsers().size());
        assertEquals(permission.getUsers(), updated.getUsers());

        permission.removeUser("alice");

        protection.policy(resource.getId()).update(permission);
        assertEquals(expectedPolicies, getAssociatedPolicies(permission).size());
        updated = protection.policy(resource.getId()).findById(permission.getId());
        assertEquals(1, updated.getUsers().size());
        assertEquals(permission.getUsers(), updated.getUsers());

        permission.setUsers(null);

        protection.policy(resource.getId()).update(permission);
        assertEquals(--expectedPolicies, getAssociatedPolicies(permission).size());
        updated = protection.policy(resource.getId()).findById(permission.getId());

        assertEquals(permission.getUsers(), updated.getUsers());

        if (Profile.isFeatureEnabled(Profile.Feature.UPLOAD_SCRIPTS)) {
            permission.setCondition(null);

            protection.policy(resource.getId()).update(permission);
            assertEquals(--expectedPolicies, getAssociatedPolicies(permission).size());
            updated = protection.policy(resource.getId()).findById(permission.getId());

            assertEquals(permission.getCondition(), updated.getCondition());
        };

        permission.setRoles(null);

        protection.policy(resource.getId()).update(permission);
        assertEquals(--expectedPolicies, getAssociatedPolicies(permission).size());
        updated = protection.policy(resource.getId()).findById(permission.getId());

        assertEquals(permission.getRoles(), updated.getRoles());

        permission.setClients(null);

        protection.policy(resource.getId()).update(permission);
        assertEquals(--expectedPolicies, getAssociatedPolicies(permission).size());
        updated = protection.policy(resource.getId()).findById(permission.getId());

        assertEquals(permission.getClients(), updated.getClients());

        permission.setGroups(null);

        try {
            protection.policy(resource.getId()).update(permission);
            assertEquals(1, getAssociatedPolicies(permission).size());
            fail("Permission must be removed because the last associated policy was removed");
        } catch (NotFoundException ignore) {

        } catch (Exception e) {
            fail("Expected not found");
        }
    }
    
    @Test
    public void testUpdateDeprecatedFeaturesEnabled() {
        testUpdate();
    }

    @Test
    @DisableFeature(value = Profile.Feature.UPLOAD_SCRIPTS, skipRestart = true)
    public void testUpdateDeprecatedFeaturesDisabled() {
        testUpdate();
    }
    
    @Test
    @DisableFeature(value = Profile.Feature.UPLOAD_SCRIPTS, skipRestart = true)
    public void testUploadScriptDisabled() {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setName("Resource A");
        resource.setOwnerManagedAccess(true);
        resource.setOwner("marta");
        resource.addScope("Scope A", "Scope B", "Scope C");

        resource = getAuthzClient().protection().resource().create(resource);

        UmaPermissionRepresentation newPermission = new UmaPermissionRepresentation();

        newPermission.setName("Custom User-Managed Permission");
        newPermission.setDescription("Users from specific roles are allowed to access");
        newPermission.setCondition("$evaluation.grant()");

        ProtectionResource protection = getAuthzClient().protection("marta", "password");

        try {
            protection.policy(resource.getId()).create(newPermission);
            fail("Should fail because upload scripts is disabled");
        } catch (Exception ignore) {
            
        }
        
        newPermission.setCondition(null);

        UmaPermissionRepresentation representation = protection.policy(resource.getId()).create(newPermission);
        
        representation.setCondition("$evaluation.grant();");

        try {
            protection.policy(resource.getId()).update(newPermission);
            fail("Should fail because upload scripts is disabled");
        } catch (Exception ignore) {

        }
    }

    @Test
    public void testUserManagedPermission() {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setName("Resource A");
        resource.setOwnerManagedAccess(true);
        resource.setOwner("marta");
        resource.addScope("Scope A", "Scope B", "Scope C");

        resource = getAuthzClient().protection().resource().create(resource);

        UmaPermissionRepresentation permission = new UmaPermissionRepresentation();

        permission.setName("Custom User-Managed Permission");
        permission.setDescription("Users from specific roles are allowed to access");
        permission.addScope("Scope A");
        permission.addRole("role_a");

        ProtectionResource protection = getAuthzClient().protection("marta", "password");

        permission = protection.policy(resource.getId()).create(permission);

        AuthorizationResource authorization = getAuthzClient().authorization("kolo", "password");

        AuthorizationRequest request = new AuthorizationRequest();

        request.addPermission(resource.getId(), "Scope A");

        AuthorizationResponse authzResponse = authorization.authorize(request);

        assertNotNull(authzResponse);

        permission.removeRole("role_a");
        permission.addRole("role_b");

        protection.policy(resource.getId()).update(permission);

        try {
            authorization.authorize(request);
            fail("User should not have permission");
        } catch (Exception e) {
            assertTrue(AuthorizationDeniedException.class.isInstance(e));
        }

        try {
            getAuthzClient().authorization("alice", "password").authorize(request);
            fail("User should not have permission");
        } catch (Exception e) {
            assertTrue(AuthorizationDeniedException.class.isInstance(e));
        }

        permission.addRole("role_a");

        protection.policy(resource.getId()).update(permission);

        authzResponse = authorization.authorize(request);

        assertNotNull(authzResponse);

        protection.policy(resource.getId()).delete(permission.getId());

        try {
            authorization.authorize(request);
            fail("User should not have permission");
        } catch (Exception e) {
            assertTrue(AuthorizationDeniedException.class.isInstance(e));
        }

        try {
            getAuthzClient().protection("marta", "password").policy(resource.getId()).findById(permission.getId());
            fail("Permission must not exist");
        } catch (Exception e) {
            assertEquals(404, HttpResponseException.class.cast(e.getCause()).getStatusCode());
        }

        // create a user based permission, where only selected users are allowed access to the resource.
        permission = new UmaPermissionRepresentation();
        permission.setName("Custom User-Managed Permission");
        permission.setDescription("Specific users are allowed access to the resource");
        permission.addScope("Scope A");
        permission.addUser("alice");
        protection.policy(resource.getId()).create(permission);

        // alice should be able to access the resource with the updated permission.
        authzResponse = getAuthzClient().authorization("alice", "password").authorize(request);
        assertNotNull(authzResponse);

        // kolo shouldn't be able to access the resource with the updated permission.
        try {
            authorization.authorize(request);
            fail("User should not have permission to access the protected resource");
        } catch(Exception e) {
            assertTrue(AuthorizationDeniedException.class.isInstance(e));
        }

    }

    @Test
    public void testPermissionInAdditionToUserGrantedPermission() {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setName("Resource A");
        resource.setOwnerManagedAccess(true);
        resource.setOwner("marta");
        resource.addScope("Scope A", "Scope B", "Scope C");

        resource = getAuthzClient().protection().resource().create(resource);

        PermissionResponse ticketResponse = getAuthzClient().protection().permission().create(new PermissionRequest(resource.getId(), "Scope A"));

        AuthorizationRequest request = new AuthorizationRequest();

        request.setTicket(ticketResponse.getTicket());

        try {
            getAuthzClient().authorization("kolo", "password").authorize(request);
            fail("User should not have permission");
        } catch (Exception e) {
            assertTrue(AuthorizationDeniedException.class.isInstance(e));
            assertTrue(e.getMessage().contains("request_submitted"));
        }

        List<PermissionTicketRepresentation> tickets = getAuthzClient().protection().permission().findByResource(resource.getId());

        assertEquals(1, tickets.size());

        PermissionTicketRepresentation ticket = tickets.get(0);

        ticket.setGranted(true);

        getAuthzClient().protection().permission().update(ticket);

        AuthorizationResponse authzResponse = getAuthzClient().authorization("kolo", "password").authorize(request);

        assertNotNull(authzResponse);

        UmaPermissionRepresentation permission = new UmaPermissionRepresentation();

        permission.setName("Custom User-Managed Permission");
        permission.addScope("Scope A");
        permission.addRole("role_a");

        ProtectionResource protection = getAuthzClient().protection("marta", "password");

        permission = protection.policy(resource.getId()).create(permission);

        getAuthzClient().authorization("kolo", "password").authorize(request);

        ticket.setGranted(false);

        getAuthzClient().protection().permission().update(ticket);

        getAuthzClient().authorization("kolo", "password").authorize(request);

        permission = getAuthzClient().protection("marta", "password").policy(resource.getId()).findById(permission.getId());

        assertNotNull(permission);

        permission.removeRole("role_a");
        permission.addRole("role_b");

        getAuthzClient().protection("marta", "password").policy(resource.getId()).update(permission);

        try {
            getAuthzClient().authorization("kolo", "password").authorize(request);
            fail("User should not have permission");
        } catch (Exception e) {
            assertTrue(AuthorizationDeniedException.class.isInstance(e));
        }

        request = new AuthorizationRequest();

        request.addPermission(resource.getId());

        try {
            getAuthzClient().authorization("kolo", "password").authorize(request);
            fail("User should not have permission");
        } catch (Exception e) {
            assertTrue(AuthorizationDeniedException.class.isInstance(e));
        }

        getAuthzClient().protection("marta", "password").policy(resource.getId()).delete(permission.getId());

        try {
            getAuthzClient().authorization("kolo", "password").authorize(request);
            fail("User should not have permission");
        } catch (Exception e) {
            assertTrue(AuthorizationDeniedException.class.isInstance(e));
        }
    }

    @Test
    public void testPermissionWithoutScopes() {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setName(UUID.randomUUID().toString());
        resource.setOwner("marta");
        resource.setOwnerManagedAccess(true);
        resource.addScope("Scope A", "Scope B", "Scope C");

        ProtectionResource protection = getAuthzClient().protection();

        resource = protection.resource().create(resource);

        UmaPermissionRepresentation permission = new UmaPermissionRepresentation();

        permission.setName("Custom User-Managed Policy");
        permission.addRole("role_a");

        PolicyResource policy = getAuthzClient().protection("marta", "password").policy(resource.getId());

        permission = policy.create(permission);

        assertEquals(3, permission.getScopes().size());
        assertTrue(Arrays.asList("Scope A", "Scope B", "Scope C").containsAll(permission.getScopes()));

        permission = policy.findById(permission.getId());

        assertTrue(Arrays.asList("Scope A", "Scope B", "Scope C").containsAll(permission.getScopes()));
        assertEquals(3, permission.getScopes().size());

        permission.removeScope("Scope B");

        policy.update(permission);
        permission = policy.findById(permission.getId());

        assertEquals(2, permission.getScopes().size());
        assertTrue(Arrays.asList("Scope A", "Scope C").containsAll(permission.getScopes()));
    }

    @Test
    public void testOnlyResourceOwnerCanManagePolicies() {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setName(UUID.randomUUID().toString());
        resource.setOwner("marta");
        resource.addScope("Scope A", "Scope B", "Scope C");

        ProtectionResource protection = getAuthzClient().protection();

        resource = protection.resource().create(resource);

        try {
            getAuthzClient().protection("alice", "password").policy(resource.getId()).create(new UmaPermissionRepresentation());
            fail("Error expected");
        } catch (Exception e) {
            assertTrue(HttpResponseException.class.cast(e.getCause()).toString().contains("Only resource owner can access policies for resource"));
        }
    }

    @Test
    public void testOnlyResourcesWithOwnerManagedAccess() {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setName(UUID.randomUUID().toString());
        resource.setOwner("marta");
        resource.addScope("Scope A", "Scope B", "Scope C");

        ProtectionResource protection = getAuthzClient().protection();

        resource = protection.resource().create(resource);

        try {
            getAuthzClient().protection("marta", "password").policy(resource.getId()).create(new UmaPermissionRepresentation());
            fail("Error expected");
        } catch (Exception e) {
            assertTrue(HttpResponseException.class.cast(e.getCause()).toString().contains("Only resources with owner managed accessed can have policies"));
        }
    }

    @Test
    public void testOwnerAccess() {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setName(UUID.randomUUID().toString());
        resource.setOwner("marta");
        resource.addScope("Scope A", "Scope B", "Scope C");
        resource.setOwnerManagedAccess(true);

        ProtectionResource protection = getAuthzClient().protection();

        resource = protection.resource().create(resource);

        UmaPermissionRepresentation rep = null;
        
        try {
            rep = new UmaPermissionRepresentation();
            
            rep.setName("test");
            rep.addRole("role_b");
            
            rep = getAuthzClient().protection("marta", "password").policy(resource.getId()).create(rep);
        } catch (Exception e) {
            assertTrue(HttpResponseException.class.cast(e.getCause()).toString().contains("Only resources with owner managed accessed can have policies"));
        }

        AuthorizationResource authorization = getAuthzClient().authorization("marta", "password");

        AuthorizationRequest request = new AuthorizationRequest();
        
        request.addPermission(resource.getId(), "Scope A");

        AuthorizationResponse authorize = authorization.authorize(request);
        
        assertNotNull(authorize);

        try {
            getAuthzClient().authorization("kolo", "password").authorize(request);
            fail("User should not have permission");
        } catch (Exception e) {
            assertTrue(AuthorizationDeniedException.class.isInstance(e));
        }

        rep.addRole("role_a");
        
        getAuthzClient().protection("marta", "password").policy(resource.getId()).update(rep);

        authorization = getAuthzClient().authorization("kolo", "password");

        assertNotNull(authorization.authorize(request));
    }

    @Test
    public void testFindPermission() {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setName(UUID.randomUUID().toString());
        resource.setOwner("marta");
        resource.setOwnerManagedAccess(true);
        resource.addScope("Scope A", "Scope B", "Scope C");

        ProtectionResource protection = getAuthzClient().protection();

        resource = protection.resource().create(resource);

        PolicyResource policy = getAuthzClient().protection("marta", "password").policy(resource.getId());

        for (int i = 0; i < 10; i++) {
            UmaPermissionRepresentation permission = new UmaPermissionRepresentation();

            permission.setName("Custom User-Managed Policy " + i);
            permission.addRole("role_a");

            policy.create(permission);
        }

        assertEquals(10, policy.find(null, null, null, null).size());

        List<UmaPermissionRepresentation> byId = policy.find("Custom User-Managed Policy 8", null, null, null);

        assertEquals(1, byId.size());
        assertEquals(byId.get(0).getId(), policy.findById(byId.get(0).getId()).getId());
        assertEquals(10, policy.find(null, "Scope A", null, null).size());
        assertEquals(5, policy.find(null, null, -1, 5).size());
        assertEquals(2, policy.find(null, null, -1, 2).size());
    }

    @Test
    public void testGrantRequestedScopesOnly() {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setName(UUID.randomUUID().toString());
        resource.setOwnerManagedAccess(true);
        resource.setOwner("marta");
        resource.addScope("view", "delete");

        ProtectionResource protection = getAuthzClient().protection("marta", "password");

        resource = protection.resource().create(resource);

        UmaPermissionRepresentation permission = new UmaPermissionRepresentation();

        permission.setName("Custom User-Managed Permission");
        permission.addScope("view");
        permission.addUser("kolo");

        permission = protection.policy(resource.getId()).create(permission);

        AuthorizationRequest request = new AuthorizationRequest();

        request.addPermission(resource.getId(), "view");

        AuthorizationResponse response = getAuthzClient().authorization("kolo", "password").authorize(request);
        AccessToken rpt = toAccessToken(response.getToken());
        Collection<Permission> permissions = rpt.getAuthorization().getPermissions();

        assertPermissions(permissions, resource.getId(), "view");

        assertTrue(permissions.isEmpty());

        request = new AuthorizationRequest();

        request.addPermission(resource.getId(), "delete");

        try {
            getAuthzClient().authorization("kolo", "password").authorize(request);
            fail("User should not have permission");
        } catch (Exception e) {
            assertTrue(AuthorizationDeniedException.class.isInstance(e));
        }

        request = new AuthorizationRequest();

        request.addPermission(resource.getId(), "delete");

        try {
            getAuthzClient().authorization("kolo", "password").authorize(request);
            fail("User should not have permission");
        } catch (Exception e) {
            assertTrue(AuthorizationDeniedException.class.isInstance(e));
        }

        request = new AuthorizationRequest();

        request.addPermission(resource.getId());

        response = getAuthzClient().authorization("kolo", "password").authorize(request);
        rpt = toAccessToken(response.getToken());
        permissions = rpt.getAuthorization().getPermissions();

        assertPermissions(permissions, resource.getId(), "view");

        assertTrue(permissions.isEmpty());
    }

    @Test
    public void testDoNotGrantPermissionWhenObtainAllEntitlements() {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setName("Resource A");
        resource.setOwnerManagedAccess(true);
        resource.setOwner("marta");
        resource.addScope("Scope A", "Scope B", "Scope C");

        resource = getAuthzClient().protection().resource().create(resource);

        UmaPermissionRepresentation permission = new UmaPermissionRepresentation();

        permission.setName("Custom User-Managed Permission");
        permission.addScope("Scope A", "Scope B");
        permission.addUser("kolo");

        ProtectionResource protection = getAuthzClient().protection("marta", "password");

        protection.policy(resource.getId()).create(permission);

        AuthorizationResource authorization = getAuthzClient().authorization("kolo", "password");

        AuthorizationRequest request = new AuthorizationRequest();

        request.addPermission(resource.getId(), "Scope A", "Scope B");

        AuthorizationResponse authzResponse = authorization.authorize(request);
        assertNotNull(authzResponse);

        AccessToken token = toAccessToken(authzResponse.getToken());
        assertNotNull(token.getAuthorization());

        Collection<Permission> permissions = token.getAuthorization().getPermissions();
        assertEquals(1, permissions.size());

        assertTrue(permissions
                .iterator().next().getScopes().containsAll(Arrays.asList("Scope A", "Scope B")));

        try {
            // policy engine does not evaluate custom policies when obtaining all entitlements
            getAuthzClient().authorization("kolo", "password").authorize();
            fail("User should not have permission");
        } catch (Exception e) {
            assertTrue(AuthorizationDeniedException.class.isInstance(e));
        }
    }

    @Test
    public void testRemovePoliciesOnResourceDelete() {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setName("Resource A");
        resource.setOwnerManagedAccess(true);
        resource.setOwner("marta");
        resource.addScope("Scope A", "Scope B", "Scope C");

        resource = getAuthzClient().protection().resource().create(resource);

        UmaPermissionRepresentation newPermission = new UmaPermissionRepresentation();

        newPermission.setName("Custom User-Managed Permission");
        newPermission.setDescription("Users from specific roles are allowed to access");
        newPermission.addScope("Scope A", "Scope B", "Scope C");
        newPermission.addRole("role_a", "role_b", "role_c", "role_d");
        newPermission.addGroup("/group_a", "/group_a/group_b", "/group_c");
        newPermission.addClient("client-a", "resource-server-test");

        if (Profile.isFeatureEnabled(Profile.Feature.UPLOAD_SCRIPTS)) {
            newPermission.setCondition("$evaluation.grant()");
        }

        newPermission.addUser("kolo");

        ProtectionResource protection = getAuthzClient().protection("marta", "password");

        protection.policy(resource.getId()).create(newPermission);

        getTestingClient().server().run((RunOnServer) UserManagedPermissionServiceTest::testRemovePoliciesOnResourceDelete);
    }

    private static void testRemovePoliciesOnResourceDelete(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("authz-test");
        ClientModel client = realm.getClientByClientId("resource-server-test");
        AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);
        UserModel user = session.users().getUserByUsername(realm, "marta");
        Map<Policy.FilterOption, String[]> filters = new HashMap<>();

        filters.put(Policy.FilterOption.TYPE, new String[] {"uma"});
        filters.put(OWNER, new String[] {user.getId()});

        List<Policy> policies = provider.getStoreFactory().getPolicyStore()
                .findByResourceServer(filters, client.getId(), -1, -1);
        assertEquals(1, policies.size());

        Policy policy = policies.get(0);
        assertFalse(policy.getResources().isEmpty());

        Resource resource = policy.getResources().iterator().next();
        assertEquals("Resource A", resource.getName());

        provider.getStoreFactory().getResourceStore().delete(resource.getId());

        filters = new HashMap<>();

        filters.put(OWNER, new String[] {user.getId()});
        policies = provider.getStoreFactory().getPolicyStore()
                .findByResourceServer(filters, client.getId(), -1, -1);
        assertTrue(policies.isEmpty());
    }

    @Test
    public void testRemovePoliciesOnGroupDelete() {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setName("Resource A");
        resource.setOwnerManagedAccess(true);
        resource.setOwner("marta");
        resource.addScope("Scope A", "Scope B", "Scope C");

        resource = getAuthzClient().protection().resource().create(resource);

        UmaPermissionRepresentation newPermission = new UmaPermissionRepresentation();

        newPermission.setName("Custom User-Managed Permission");
        newPermission.addGroup("/group_remove");

        ProtectionResource protection = getAuthzClient().protection("marta", "password");

        protection.policy(resource.getId()).create(newPermission);

        getTestingClient().server().run((RunOnServer) UserManagedPermissionServiceTest::testRemovePoliciesOnGroupDelete);
    }

    private static void testRemovePoliciesOnGroupDelete(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("authz-test");
        ClientModel client = realm.getClientByClientId("resource-server-test");
        AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);
        UserModel user = session.users().getUserByUsername(realm, "marta");
        Map<Policy.FilterOption, String[]> filters = new HashMap<>();

        filters.put(Policy.FilterOption.TYPE, new String[] {"uma"});
        filters.put(OWNER, new String[] {user.getId()});

        List<Policy> policies = provider.getStoreFactory().getPolicyStore()
                .findByResourceServer(filters, client.getId(), -1, -1);
        assertEquals(1, policies.size());

        Policy policy = policies.get(0);
        assertFalse(policy.getResources().isEmpty());

        Resource resource = policy.getResources().iterator().next();
        assertEquals("Resource A", resource.getName());

        realm.removeGroup(realm.searchForGroupByNameStream("group_remove", -1, -1).findAny().get());

        filters = new HashMap<>();

        filters.put(OWNER, new String[] {user.getId()});

        policies = provider.getStoreFactory().getPolicyStore()
                .findByResourceServer(filters, client.getId(), -1, -1);
        assertTrue(policies.isEmpty());
    }

    private List<PolicyRepresentation> getAssociatedPolicies(UmaPermissionRepresentation permission) {
        return getClient(getRealm()).authorization().policies().policy(permission.getId()).associatedPolicies();
    }

}
