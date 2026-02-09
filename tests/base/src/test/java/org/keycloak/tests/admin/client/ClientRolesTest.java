/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.tests.admin.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.ws.rs.ClientErrorException;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RoleByIdResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RoleConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.Assert;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@KeycloakIntegrationTest
public class ClientRolesTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectAdminEvents
    AdminEvents adminEvents;

    @InjectClient(config = ClientRolesClientConfig.class)
    ManagedClient managedClient;

    private ClientResource clientRsc;
    private String clientDbId;
    private RolesResource rolesRsc;

    @BeforeEach
    public void init() {
        clientDbId = managedClient.getId();
        clientRsc = managedClient.admin();
        rolesRsc = clientRsc.roles();
    }

    private RoleRepresentation makeRole(String name) {
        return RoleConfigBuilder.create()
                .name(name)
                .build();
    }

    private boolean hasRole(RolesResource rolesRsc, String name) {
        for (RoleRepresentation role : rolesRsc.list()) {
            if (role.getName().equals(name)) return true;
        }

        return false;
    }

    @Test
    public void testAddRole() {
        RoleRepresentation role1 = RoleConfigBuilder.create()
                .name("role1")
                .description("role1-description")
                .singleAttribute("role1-attr-key", "role1-attr-val")
                .build();
        rolesRsc.create(role1);
        managedClient.cleanup().add(c -> c.roles().deleteRole("role1"));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(clientDbId, "role1"), role1, ResourceType.CLIENT_ROLE);

        RoleRepresentation addedRole = rolesRsc.get(role1.getName()).toRepresentation();
        assertEquals(role1.getName(), addedRole.getName());
        assertEquals(role1.getDescription(), addedRole.getDescription());
        assertEquals(role1.getAttributes(), addedRole.getAttributes());
    }

    @Test
    public void createRoleWithSameName() {
        RoleRepresentation role = RoleConfigBuilder.create().name("role-a").build();
        rolesRsc.create(role);
        managedClient.cleanup().add(c -> c.roles().deleteRole("role-a"));
        assertThrows(ClientErrorException.class, () -> rolesRsc.create(role), "Client role with the same name is not allowed");
    }

    @Test
    public void createRoleWithNamePattern() {
        RoleRepresentation role = RoleConfigBuilder.create().name("role-a-{pattern}").build();
        rolesRsc.create(role);
        managedClient.cleanup().add(c -> c.roles().deleteRole("role-a-{pattern}"));
    }

    @Test
    public void testRemoveRole() {
        RoleRepresentation role2 = makeRole("role2");
        rolesRsc.create(role2);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(clientDbId, "role2"), role2, ResourceType.CLIENT_ROLE);

        rolesRsc.deleteRole("role2");
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.clientRoleResourcePath(clientDbId, "role2"), ResourceType.CLIENT_ROLE);

        assertFalse(hasRole(rolesRsc, "role2"));
    }

    @Test
    public void testComposites() {
        RoleRepresentation roleA = makeRole("role-a");
        rolesRsc.create(roleA);
        managedClient.cleanup().add(c -> c.roles().deleteRole("role-a"));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(clientDbId, "role-a"), roleA, ResourceType.CLIENT_ROLE);

        assertFalse(rolesRsc.get("role-a").toRepresentation().isComposite());
        assertEquals(0, rolesRsc.get("role-a").getRoleComposites().size());

        RoleRepresentation roleB = makeRole("role-b");
        rolesRsc.create(roleB);
        managedClient.cleanup().add(c -> c.roles().deleteRole("role-b"));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(clientDbId, "role-b"), roleB, ResourceType.CLIENT_ROLE);

        RoleRepresentation roleC = makeRole("role-c");
        managedRealm.admin().roles().create(roleC);
        managedRealm.cleanup().add(r -> r.roles().deleteRole("role-c"));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath("role-c"), roleC, ResourceType.REALM_ROLE);

        List<RoleRepresentation> l = new LinkedList<>();
        l.add(rolesRsc.get("role-b").toRepresentation());
        l.add(managedRealm.admin().roles().get("role-c").toRepresentation());
        rolesRsc.get("role-a").addComposites(l);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourceCompositesPath(clientDbId, "role-a"), l, ResourceType.CLIENT_ROLE);

        Set<RoleRepresentation> composites = rolesRsc.get("role-a").getRoleComposites();

        assertTrue(rolesRsc.get("role-a").toRepresentation().isComposite());
        Assert.assertNames(composites, "role-b", "role-c");

        Set<RoleRepresentation> realmComposites = rolesRsc.get("role-a").getRealmRoleComposites();
        Assert.assertNames(realmComposites, "role-c");

        Set<RoleRepresentation> clientComposites = rolesRsc.get("role-a").getClientRoleComposites(clientRsc.toRepresentation().getId());
        Assert.assertNames(clientComposites, "role-b");

        rolesRsc.get("role-a").deleteComposites(l);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.clientRoleResourceCompositesPath(clientDbId, "role-a"), l, ResourceType.CLIENT_ROLE);

        assertFalse(rolesRsc.get("role-a").toRepresentation().isComposite());
        assertEquals(0, rolesRsc.get("role-a").getRoleComposites().size());
    }


    @Test
    public void testCompositeRolesSearch() {
        // Create main-role we will work on
        RoleRepresentation mainRole = makeRole("main-role");
        rolesRsc.create(mainRole);
        managedClient.cleanup().add(c -> c.roles().deleteRole("main-role"));

        RoleResource mainRoleRsc = rolesRsc.get("main-role");
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(clientDbId, "main-role"), mainRole, ResourceType.CLIENT_ROLE);

        // Add composites
        List<RoleRepresentation> createdRoles = IntStream.range(0, 20)
                .boxed()
                .map(i -> makeRole("role" + i))
                .peek(rolesRsc::create)
                .peek(role -> managedClient.cleanup().add(c -> c.roles().deleteRole(role.getName())))
                .peek(role -> AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(clientDbId, role.getName()), role, ResourceType.CLIENT_ROLE))
                .map(role -> rolesRsc.get(role.getName()).toRepresentation())
                .collect(Collectors.toList());

        mainRoleRsc.addComposites(createdRoles);
        mainRole = mainRoleRsc.toRepresentation();
        RoleByIdResource roleByIdResource = managedRealm.admin().rolesById();

        // Search for all composites
        Set<RoleRepresentation> foundRoles = roleByIdResource.getRoleComposites(mainRole.getId());
        assertThat(foundRoles, hasSize(createdRoles.size()));

        // Search paginated composites
        foundRoles = roleByIdResource.searchRoleComposites(mainRole.getId(), null, 0, 10);
        assertThat(foundRoles, hasSize(10));

        // Search for composites by string role1 (should be role1, role10-role19) without pagination
        foundRoles = roleByIdResource.searchRoleComposites(mainRole.getId(), "role1", null, null);
        assertThat(foundRoles, hasSize(11));

        // Search for role1 with pagination
        foundRoles = roleByIdResource.searchRoleComposites(mainRole.getId(), "role1", 5, 5);
        assertThat(foundRoles, hasSize(5));
    }

    @Test
    public void usersInRole() {
        String clientID = managedClient.getId();

        // create test role on client
        String roleName = "test-role";
        RoleRepresentation role = makeRole(roleName);
        rolesRsc.create(role);
        managedClient.cleanup().add(c -> c.roles().deleteRole(roleName));
        assertTrue(hasRole(rolesRsc, roleName));
        List<RoleRepresentation> roleToAdd = Collections.singletonList(rolesRsc.get(roleName).toRepresentation());

        //create users and assign test role
        List<String> usernames = createUsernames(0, 10);
        usernames.forEach(username -> {
            UserRepresentation user = new UserRepresentation();
            user.setUsername(username);
            String userUuid = ApiUtil.getCreatedId(managedRealm.admin().users().create(user));
            managedRealm.cleanup().add(r -> r.users().delete(userUuid));
            managedRealm.admin().users().get(userUuid).roles().clientLevel(clientID).add(roleToAdd);
        });

        // check if users have test role assigned
        RoleResource roleResource = rolesRsc.get(roleName);
        List<UserRepresentation> usersInRole = roleResource.getUserMembers();
        assertEquals(usernames, extractUsernames(usersInRole));

        // pagination
        List<UserRepresentation> usersInRole1 = roleResource.getUserMembers(0, 5);
        assertEquals(createUsernames(0, 5), extractUsernames(usersInRole1));
        Assertions.assertNotNull(usersInRole1.get(0).getNotBefore(), "Not in full representation");
        List<UserRepresentation> usersInRole2 = roleResource.getUserMembers(true, 5, 10);
        assertEquals(createUsernames(5, 10), extractUsernames(usersInRole2));
        Assert.assertNull(usersInRole2.get(0).getNotBefore(), "Not in brief representation");
    }

    private static List<String> createUsernames(int startIndex, int endIndex) {
        List<String> usernames = new ArrayList<>();
        for (int i = startIndex; i < endIndex; i++) {
            String userName = "user" + i;
            usernames.add(userName);
        }
        return usernames;
    }

    private static List<String> extractUsernames(Collection<UserRepresentation> users) {
        return users.stream().map(UserRepresentation::getUsername).collect(Collectors.toList());
    }

    @Test
    public void testSearchForRoles() {
        for (int i = 0; i < 15; i++) {
            String roleName = "role" + i;
            RoleRepresentation role = makeRole(roleName);
            rolesRsc.create(role);
            managedClient.cleanup().add(c -> c.roles().deleteRole(roleName));
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(clientDbId, roleName), role, ResourceType.CLIENT_ROLE);
        }

        String roleNameA = "abcdef";
        RoleRepresentation roleA = makeRole(roleNameA);
        rolesRsc.create(roleA);
        managedClient.cleanup().add(c -> c.roles().deleteRole(roleNameA));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(clientDbId, roleNameA), roleA, ResourceType.CLIENT_ROLE);

        String roleNameB = "defghi";
        RoleRepresentation roleB = makeRole(roleNameB);
        rolesRsc.create(roleB);
        managedClient.cleanup().add(c -> c.roles().deleteRole(roleNameB));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(clientDbId, roleNameB), roleB, ResourceType.CLIENT_ROLE);

        List<RoleRepresentation> resultSearch = rolesRsc.list("def", -1, -1);
        assertEquals(2, resultSearch.size());

        List<RoleRepresentation> resultSearch2 = rolesRsc.list("role", -1, -1);
        assertEquals(15, resultSearch2.size());

        List<RoleRepresentation> resultSearchPagination = rolesRsc.list("role", 1, 5);
        assertEquals(5, resultSearchPagination.size());
    }

    @Test
    public void testPaginationRoles() {
        for (int i = 0; i < 15; i++) {
            String roleName = "role" + i;
            RoleRepresentation role = makeRole(roleName);
            rolesRsc.create(role);
            managedClient.cleanup().add(c -> c.roles().deleteRole(roleName));
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(clientDbId, roleName), role, ResourceType.CLIENT_ROLE);
        }

        List<RoleRepresentation> resultSearchWithoutPagination = rolesRsc.list();
        assertEquals(15, resultSearchWithoutPagination.size());

        List<RoleRepresentation> resultSearchPagination = rolesRsc.list(1, 5);
        assertEquals(5, resultSearchPagination.size());

        List<RoleRepresentation> resultSearchPaginationIncoherentParams = rolesRsc.list(1, null);
        assertTrue(resultSearchPaginationIncoherentParams.size() >= 15);
    }

    @Test
    public void testPaginationRolesCache() {
        for (int i = 0; i < 5; i++) {
            String roleName = "paginaterole" + i;
            RoleRepresentation role = makeRole(roleName);
            rolesRsc.create(role);
            managedClient.cleanup().add(c -> c.roles().deleteRole(roleName));
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(clientDbId, roleName), role, ResourceType.CLIENT_ROLE);
        }

        List<RoleRepresentation> resultBeforeAddingRoleToTestCache = rolesRsc.list(1, 1000);

        // after a first call which init the cache, we add a new role to see if the result change

        RoleRepresentation role = makeRole("anewrole");
        rolesRsc.create(role);
        managedClient.cleanup().add(c -> c.roles().deleteRole("anewrole"));
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(clientDbId, "anewrole"), role, ResourceType.CLIENT_ROLE);

        List<RoleRepresentation> resultAfterAddingRoleToTestCache = rolesRsc.list(1, 1000);

        assertEquals(resultBeforeAddingRoleToTestCache.size() + 1, resultAfterAddingRoleToTestCache.size());
    }

    @Test
    public void getRolesWithFullRepresentation() {
        for (int i = 0; i < 5; i++) {
            String roleName = "attributesrole" + i;
            RoleRepresentation role = RoleConfigBuilder.create()
                    .name(roleName)
                    .attributes(Map.of("attribute1", List.of("value1", "value2")))
                    .build();

            rolesRsc.create(role);
            managedClient.cleanup().add(c -> c.roles().deleteRole(roleName));
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(clientDbId, roleName), role, ResourceType.CLIENT_ROLE);
        }

        List<RoleRepresentation> roles = rolesRsc.list(false);
        roles.forEach(role -> assertTrue(role.getAttributes().containsKey("attribute1")));
    }

    @Test
    public void getRolesWithBriefRepresentation() {
        for (int i = 0; i < 5; i++) {
            String roleName = "attributesrole" + i;
            RoleRepresentation role = RoleConfigBuilder.create()
                    .name(roleName)
                    .attributes(Map.of("attribute1", List.of("value1", "value2")))
                    .build();

            rolesRsc.create(role);
            managedClient.cleanup().add(c -> c.roles().deleteRole(roleName));
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.clientRoleResourcePath(clientDbId, roleName), role, ResourceType.CLIENT_ROLE);
        }

        List<RoleRepresentation> roles = rolesRsc.list();
        roles.forEach(role -> assertNull(role.getAttributes()));
    }

    private static class ClientRolesClientConfig implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId("roleClient")
                    .name("roleClient")
                    .protocol("openid-connect");
        }
    }
}
