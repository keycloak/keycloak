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

package org.keycloak.tests.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleByIdResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RoleBuilder;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.tests.utils.Assert;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class RoleByIdResourceTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectClient(lifecycle = LifeCycle.METHOD)
    ManagedClient managedClient;

    @InjectAdminEvents
    AdminEvents adminEvents;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    private RoleByIdResource resource;

    private final String roleNameA = "role-a";
    private final String roleNameB = "role-b";
    private final String roleNameC = "role-c";
    private final String roleNameD = "role-d";
    private final String roleNameCompositeWihD = "composite-role-with-d";
    private Map<String, String> roleIds = new HashMap<>();

    @BeforeEach
    public void before() {
        managedRealm.admin().roles().create(RoleBuilder.create().name(roleNameA).description("Role A").build());
        managedRealm.admin().roles().create(RoleBuilder.create().name(roleNameB).description("Role B").build());
        // add a role that is a composite role
        RoleRepresentation roleD = RoleBuilder.create().name(roleNameD).description("Role D").build();
        managedRealm.admin().roles().create(roleD);
        managedRealm.admin().roles().create(RoleBuilder.create()
                .name(roleNameCompositeWihD)
                .description("Composite Role with Role D")
                .composite(true)
                .realmComposite(roleD.getName())
                .build()
        );

        managedRealm.admin().clients().get(managedClient.getId()).roles().create(RoleBuilder.create().name(roleNameC).description("Role C").build());

        managedRealm.admin().roles().list()
                .forEach(r -> roleIds.put(r.getName(), r.getId()));

        managedRealm.admin().clients().get(managedClient.getId()).roles().list()
                .forEach(r -> roleIds.put(r.getName(), r.getId()));

        resource = managedRealm.admin().rolesById();
        adminEvents.skipAll(); // Tested in RealmRolesTest already
    }

    @Test
    public void getRole() {
        RoleRepresentation role = resource.getRole(roleIds.get(roleNameA));
        assertNotNull(role);
        assertEquals(roleNameA, role.getName());
        assertEquals("Role A", role.getDescription());
        assertFalse(role.isComposite());
    }

    @Test
    @DatabaseTest
    public void updateRole() {
        RoleRepresentation role = resource.getRole(roleIds.get(roleNameA));

        role.setName("role-a-new");
        role.setDescription("Role A New");

        resource.updateRole(roleIds.get(roleNameA), role);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.roleByIdResourcePath(roleIds.get(roleNameA)), role, ResourceType.REALM_ROLE);

        role = resource.getRole(roleIds.get(roleNameA));

        assertNotNull(role);
        assertEquals("role-a-new", role.getName());
        assertEquals("Role A New", role.getDescription());
        assertFalse(role.isComposite());
    }

    @Test
    @DatabaseTest
    public void deleteRole() {
        assertNotNull(resource.getRole(roleIds.get(roleNameA)));
        resource.deleteRole(roleIds.get(roleNameA));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.roleByIdResourcePath(roleIds.get(roleNameA)), ResourceType.REALM_ROLE);

        try {
            resource.getRole(roleIds.get(roleNameA));
            fail("Expected 404");
        } catch (NotFoundException e) {
            // Ignore
        }
    }

    @Test
    @DatabaseTest
    public void composites() {
        assertFalse(resource.getRole(roleIds.get(roleNameA)).isComposite());
        assertEquals(0, resource.getRoleComposites(roleIds.get(roleNameA)).size());

        List<RoleRepresentation> l = new LinkedList<>();
        l.add(RoleBuilder.create().id(roleIds.get(roleNameB)).build());
        l.add(RoleBuilder.create().id(roleIds.get(roleNameC)).build());
        resource.addComposites(roleIds.get(roleNameA), l);

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleByIdResourceCompositesPath(roleIds.get(roleNameA)), l, ResourceType.REALM_ROLE);

        Set<RoleRepresentation> composites = resource.getRoleComposites(roleIds.get(roleNameA));

        assertTrue(resource.getRole(roleIds.get(roleNameA)).isComposite());
        Assert.assertNames(composites, roleNameB, roleNameC);

        Set<RoleRepresentation> realmComposites = resource.getRealmRoleComposites(roleIds.get(roleNameA));
        Assert.assertNames(realmComposites, roleNameB);

        Set<RoleRepresentation> clientComposites = resource.getClientRoleComposites(roleIds.get(roleNameA), managedClient.getId());
        Assert.assertNames(clientComposites, roleNameC);

        composites = resource.searchRoleComposites(roleIds.get(roleNameA), null, null, null);
        Assert.assertNames(composites, roleNameB, roleNameC);

        composites = resource.searchRoleComposites(roleIds.get(roleNameA), "b", null, null);
        Assert.assertNames(composites, roleNameB);

        composites = resource.searchRoleComposites(roleIds.get(roleNameA), null, 0, 0);
        assertThat(composites, is(empty()));

        composites = resource.searchRoleComposites(roleIds.get(roleNameA), null, 0, 1);
        Assert.assertNames(composites, roleNameB);

        composites = resource.searchRoleComposites(roleIds.get(roleNameA), null, 1, 1);
        Assert.assertNames(composites, roleNameC);

        resource.deleteComposites(roleIds.get(roleNameA), l);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.roleByIdResourceCompositesPath(roleIds.get(roleNameA)), l, ResourceType.REALM_ROLE);

        assertFalse(resource.getRole(roleIds.get(roleNameA)).isComposite());
        assertEquals(0, resource.getRoleComposites(roleIds.get(roleNameA)).size());

    }

    /**
     * see KEYCLOAK-12754
     */
    @Test
    public void createNewMixedRealmCompositeRole() {
        RoleRepresentation newRoleComp = RoleBuilder.create()
                .name("role-mixed-comp")
                .composite(true)
                .realmComposite(roleNameA)
                .realmComposite(roleNameCompositeWihD)
                .clientComposite(managedClient.getClientId(), roleNameC).build();
        managedRealm.admin().roles().create(newRoleComp);

        RoleRepresentation roleMixedComp = managedRealm.admin().roles().get(newRoleComp.getName()).toRepresentation();
        assertTrue(roleMixedComp.isComposite());

        Predicate<RoleRepresentation> isClientRole = RoleRepresentation::getClientRole;

        Set<RoleRepresentation> roleComposites = resource.getRoleComposites(roleMixedComp.getId());
        Set<RoleRepresentation> containedRealmRoles = roleComposites.stream().filter(isClientRole.negate()).collect(Collectors.toSet());
        assertFalse(containedRealmRoles.isEmpty());
        assertTrue(containedRealmRoles.stream().anyMatch(r -> r.getName().equals(roleNameA)));
        assertTrue(containedRealmRoles.stream().anyMatch(r -> r.getName().equals(roleNameCompositeWihD)));

        Set<RoleRepresentation> containedClientRoles = roleComposites.stream().filter(isClientRole).collect(Collectors.toSet());
        assertFalse(containedClientRoles.isEmpty());
        assertTrue(containedClientRoles.stream().anyMatch(r -> r.getContainerId().equals(managedClient.getId()) && r.getName().equals(roleNameC)));

        // check that there are no unexpected roles contained
        int expectedCompositeCount = newRoleComp.getComposites().getRealm().size() + newRoleComp.getComposites().getClient().size();
        assertEquals(expectedCompositeCount, roleComposites.size());
    }

    /**
     * see KEYCLOAK-12754
     */
    @Test
    public void createNewMixedRealmCompositeRoleWithUnknownRealmRoleShouldThrow() {
        String unknownRealmRole = "realm-role-unknown";
        RoleRepresentation newRoleComp = RoleBuilder.create()
                .name("role-broken-comp1")
                .composite(true)
                .realmComposite(unknownRealmRole)
                .clientComposite(managedRealm.getId(), roleNameC)
                .build();

        Assertions.assertThrowsExactly(NotFoundException.class, () -> managedRealm.admin().roles().create(newRoleComp));
    }

    /**
     * see KEYCLOAK-12754
     */
    @Test
    public void createNewMixedRealmCompositeRoleWithUnknownClientRoleShouldThrow() {
        String unknownClientRole = "client-role-unknown";
        RoleRepresentation newRoleComp = RoleBuilder.create()
                .name("role-broken-comp2")
                .composite(true)
                .realmComposite(roleNameA)
                .clientComposite(managedClient.getClientId(), unknownClientRole)
                .build();

        Assertions.assertThrowsExactly(NotFoundException.class, () -> managedRealm.admin().roles().create(newRoleComp));
    }

    @Test
    public void attributes() {
        for (String id : roleIds.values()) {
            RoleRepresentation role = resource.getRole(id);
            assertNotNull(role.getAttributes());
            assertTrue(role.getAttributes().isEmpty());

            // update the role with attributes
            Map<String, List<String>> attributes = new HashMap<>();
            List<String> attributeValues = new ArrayList<>();
            attributeValues.add("value1");
            attributes.put("key1", attributeValues);
            attributeValues = new ArrayList<>();
            attributeValues.add("value2.1");
            attributeValues.add("value2.2");
            attributes.put("key2", attributeValues);
            role.setAttributes(attributes);

            resource.updateRole(id, role);
            role = resource.getRole(id);
            assertNotNull(role);
            Map<String, List<String>> roleAttributes = role.getAttributes();
            assertNotNull(roleAttributes);

            Assert.assertRoleAttributes(attributes, roleAttributes);


            // delete an attribute
            attributes.remove("key2");
            role.setAttributes(attributes);
            resource.updateRole(id, role);
            role = resource.getRole(id);
            assertNotNull(role);
            roleAttributes = role.getAttributes();
            assertNotNull(roleAttributes);

            Assert.assertRoleAttributes(attributes, roleAttributes);
        }
    }

    @Test
    public void deleteDefaultRole() {
        RoleRepresentation role = managedRealm.admin().roles().get(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName()).toRepresentation();
        Assertions.assertThrowsExactly(BadRequestException.class, () -> resource.deleteRole(role.getId()));
    }

    /**
     * see #37320
     */
    @Test
    public void renameRoleToNamePreviouslyCached() {
        String roleName = "realm-role-new-" + new Random().nextInt();
        RoleRepresentation newRoleRepresentation = RoleBuilder.create()
                .name(roleName)
                .build();
        managedRealm.admin().roles().create(newRoleRepresentation);
        RoleRepresentation roleRepresentation = managedRealm.admin().roles().get(roleName).toRepresentation();

        String newRoleName = "realm-role-renamed-" + new Random().nextInt();
        cacheMissingRoleName(newRoleName);

        RoleRepresentation updatedRoleRepresentation = RoleBuilder.create()
                .id(roleRepresentation.getId())
                .name(newRoleName)
                .build();
        resource.updateRole(roleRepresentation.getId(), updatedRoleRepresentation);

        try {
            managedRealm.admin().roles().get(newRoleName).toRepresentation();
        } catch (NotFoundException e) {
            fail("Role is incorrectly cached");
        }
    }

    /**
     * see #37320
     */
    @Test
    public void createRolePreviouslyCached() {
        String roleName = "realm-role-new-" + new Random().nextInt();
        RoleRepresentation roleRepresentation = RoleBuilder.create()
                .name(roleName)
                .build();

        cacheMissingRoleName(roleName);

        managedRealm.admin().roles().create(roleRepresentation);

        try {
            managedRealm.admin().roles().get(roleName).toRepresentation();
        } catch (NotFoundException e) {
            fail("Role is incorrectly cached");
        }
    }

    private void cacheMissingRoleName(String missingRoleName) {
        assertThrows(NotFoundException.class, () -> managedRealm.admin().roles().get(missingRoleName).toRepresentation());
    }

    /**
     * see #49427
     */
    @Test
    @DatabaseTest
    public void testRenameProtectedRoleBlockedForDelegatedAdmin() {
        createDelegatedAdminUser("delegated-admin", "password", AdminRoles.MANAGE_CLIENTS);

        ClientRepresentation rmClient = managedRealm.admin().clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation realmAdminRole = managedRealm.admin().clients()
                .get(rmClient.getId()).roles().get(AdminRoles.REALM_ADMIN).toRepresentation();

        try (Keycloak delegatedClient = adminClientFactory.create()
                .realm(managedRealm.getName())
                .username("delegated-admin")
                .password("password")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build()) {

            RoleRepresentation renamed = new RoleRepresentation();
            renamed.setId(realmAdminRole.getId());
            renamed.setName("temp-role");

            Assertions.assertThrows(ForbiddenException.class, () ->
                    delegatedClient.realm(managedRealm.getName()).rolesById()
                            .updateRole(realmAdminRole.getId(), renamed));
        }
    }

    /**
     * see #49427
     */
    @Test
    @DatabaseTest
    public void testRenamingToProtectedRoleNameBlockedForDelegatedAdmin() {
        createDelegatedAdminUser("delegated-admin2", "password", AdminRoles.MANAGE_CLIENTS);

        String attackerClientId = "attacker-client-" + new Random().nextInt(10000);
        try (Keycloak delegatedClient = adminClientFactory.create()
                .realm(managedRealm.getName())
                .username("delegated-admin2")
                .password("password")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build()) {

            // Create a client and a role the delegated admin owns
            Response resp = delegatedClient.realm(managedRealm.getName()).clients()
                    .create(clientRepresentation(attackerClientId));
            String attackerClientUuid = resp.getLocation().getPath().replaceAll(".*/", "");
            resp.close();

            delegatedClient.realm(managedRealm.getName()).clients()
                    .get(attackerClientUuid).roles().create(roleRepresentation("innocent-role"));

            RoleRepresentation innocentRole = delegatedClient.realm(managedRealm.getName()).clients()
                    .get(attackerClientUuid).roles().get("innocent-role").toRepresentation();

            // Attempt to rename innocent-role → realm-admin
            RoleRepresentation renamed = new RoleRepresentation();
            renamed.setId(innocentRole.getId());
            renamed.setName(AdminRoles.REALM_ADMIN);

            Assertions.assertThrows(ForbiddenException.class, () ->
                    delegatedClient.realm(managedRealm.getName()).rolesById()
                            .updateRole(innocentRole.getId(), renamed));
        }
    }

    /**
     * see #49427 — named-role endpoint (PUT /roles/{role-name}) must also be guarded.
     */
    @Test
    @DatabaseTest
    public void testRenameProtectedRoleBlockedViaNamedRoleEndpoint() {
        createDelegatedAdminUser("delegated-admin3", "password", AdminRoles.MANAGE_CLIENTS);

        ClientRepresentation rmClient = managedRealm.admin().clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);

        try (Keycloak delegatedClient = adminClientFactory.create()
                .realm(managedRealm.getName())
                .username("delegated-admin3")
                .password("password")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build()) {

            RoleRepresentation renamed = new RoleRepresentation();
            renamed.setName("temp-role");

            Assertions.assertThrows(ForbiddenException.class, () ->
                    delegatedClient.realm(managedRealm.getName()).clients()
                            .get(rmClient.getId()).roles().get(AdminRoles.REALM_ADMIN)
                            .update(renamed));
        }
    }

    /**
     * see #49427
    */
    @Test
    @DatabaseTest
    public void testTOCTOURenameCompositeAttackPrevented() {
        createDelegatedAdminUser("toctou-attacker", "password", AdminRoles.MANAGE_CLIENTS);

        ClientRepresentation rmClient = managedRealm.admin().clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation realmAdminRole = managedRealm.admin().clients()
                .get(rmClient.getId()).roles().get(AdminRoles.REALM_ADMIN).toRepresentation();

        try (Keycloak delegatedClient = adminClientFactory.create()
                .realm(managedRealm.getName())
                .username("toctou-attacker")
                .password("password")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build()) {

            // Step 1: rename realm-admin → temp-role must be blocked
            RoleRepresentation renamed = new RoleRepresentation();
            renamed.setId(realmAdminRole.getId());
            renamed.setName("temp-role");

            Assertions.assertThrows(ForbiddenException.class, () ->
                    delegatedClient.realm(managedRealm.getName()).rolesById()
                            .updateRole(realmAdminRole.getId(), renamed));

            // Confirm realm-admin name is unchanged
            RoleRepresentation unchanged = managedRealm.admin().clients()
                    .get(rmClient.getId()).roles().get(AdminRoles.REALM_ADMIN).toRepresentation();
            assertEquals(AdminRoles.REALM_ADMIN, unchanged.getName());
        }
    }

    private void createDelegatedAdminUser(String username, String password, String clientRoleName) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setFirstName("Delegated");
        user.setLastName("Admin");
        user.setEmail(username + "@test.local");

        Response resp = managedRealm.admin().users().create(user);
        String userId = resp.getLocation().getPath().replaceAll(".*/", "");
        resp.close();

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(password);
        cred.setTemporary(false);
        managedRealm.admin().users().get(userId).resetPassword(cred);

        ClientRepresentation rmClient = managedRealm.admin().clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation role = managedRealm.admin().clients()
                .get(rmClient.getId()).roles().get(clientRoleName).toRepresentation();
        managedRealm.admin().users().get(userId).roles()
                .clientLevel(rmClient.getId()).add(Collections.singletonList(role));

        // query-clients is required so the delegated admin can look up clients
        RoleRepresentation queryClients = managedRealm.admin().clients()
                .get(rmClient.getId()).roles().get(AdminRoles.QUERY_CLIENTS).toRepresentation();
        managedRealm.admin().users().get(userId).roles()
                .clientLevel(rmClient.getId()).add(Collections.singletonList(queryClients));
    }

    private ClientRepresentation clientRepresentation(String clientId) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setEnabled(true);
        return client;
    }

    private RoleRepresentation roleRepresentation(String name) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        return role;
    }
}
