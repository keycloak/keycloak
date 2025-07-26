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

package org.keycloak.tests.admin.realm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.*;
import org.keycloak.tests.utils.Assert;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import jakarta.ws.rs.NotFoundException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.keycloak.models.Constants;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class RealmRolesTest {

    @InjectRealm(config = RealmRolesRealmConf.class)
    ManagedRealm managedRealm;

    @InjectClient(ref = "client-a")
    ManagedClient clientA;

    @InjectAdminEvents
    AdminEvents adminEvents;

    @Test
    public void getRole() {
        RoleRepresentation role = managedRealm.admin().roles().get("role-a").toRepresentation();
        assertNotNull(role);
        assertEquals("role-a", role.getName());
        assertEquals("Role A", role.getDescription());
        assertEquals(Map.of("role-a-attr-key1", List.of("role-a-attr-val1")), role.getAttributes());
        assertFalse(role.isComposite());
    }

    @Test
    public void createRoleWithSameName() {
        Assertions.assertThrows(ClientErrorException.class, () -> {
            managedRealm.admin().roles().create(RoleConfigBuilder.create().name("role-a").build());
        });
    }

    @Test
    public void updateRole() {
        RoleRepresentation role = managedRealm.admin().roles().get("role-a").toRepresentation();

        role.setName("role-a-new");
        role.setDescription("Role A New");
        Map<String, List<String>> newAttributes = Collections.singletonMap("attrKeyNew", Collections.singletonList("attrValueNew"));
        role.setAttributes(newAttributes);

        managedRealm.admin().roles().get("role-a").update(role);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.roleResourcePath("role-a"), role, ResourceType.REALM_ROLE);

        role = managedRealm.admin().roles().get("role-a-new").toRepresentation();

        assertNotNull(role);
        assertEquals("role-a-new", role.getName());
        assertEquals("Role A New", role.getDescription());
        assertEquals(newAttributes, role.getAttributes());
        assertFalse(role.isComposite());
    }

    @Test
    public void deleteRole() {
        assertNotNull(managedRealm.admin().roles().get("role-a"));
        managedRealm.admin().roles().deleteRole("role-a");
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.roleResourcePath("role-a"), ResourceType.REALM_ROLE);

        try {
            managedRealm.admin().roles().get("role-a").toRepresentation();
            fail("Expected 404");
        } catch (NotFoundException e) {
            // expected
        }
    }

    @Test
    public void composites() {
        assertFalse(managedRealm.admin().roles().get("role-a").toRepresentation().isComposite());
        assertEquals(0, managedRealm.admin().roles().get("role-a").getRoleComposites().size());

        List<RoleRepresentation> l = new LinkedList<>();
        l.add(RoleConfigBuilder.create().id(managedRealm.admin().roles().get("role-b").toRepresentation().getId()).build());
        l.add(RoleConfigBuilder.create().id(managedRealm.admin().roles().get("role-c").toRepresentation().getId()).build());
        managedRealm.admin().roles().get("role-a").addComposites(l);

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourceCompositesPath("role-a"), l, ResourceType.REALM_ROLE);

        Set<RoleRepresentation> composites = managedRealm.admin().roles().get("role-a").getRoleComposites();

        assertTrue(managedRealm.admin().roles().get("role-a").toRepresentation().isComposite());
        Assert.assertNames(composites, "role-b", "role-c");

        Set<RoleRepresentation> realmComposites = managedRealm.admin().roles().get("role-a").getRealmRoleComposites();
        Assert.assertNames(realmComposites, "role-b");

        Set<RoleRepresentation> clientComposites = managedRealm.admin().roles().get("role-a").getClientRoleComposites(clientA.getId());
        Assert.assertNames(clientComposites, "role-c");

        managedRealm.admin().roles().get("role-a").deleteComposites(l);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.roleResourceCompositesPath("role-a"), l, ResourceType.REALM_ROLE);

        assertFalse(managedRealm.admin().roles().get("role-a").toRepresentation().isComposite());
        assertEquals(0, managedRealm.admin().roles().get("role-a").getRoleComposites().size());
    }

    /**
     * KEYCLOAK-2035 Verifies that Users assigned to Role are being properly retrieved as members in API endpoint for role membership
     */
    @Test
    public void testUsersInRole() {
        RoleResource role = managedRealm.admin().roles().get("role-with-users");

        List<UserRepresentation> users = managedRealm.admin().users().search("test-role-member");
        assertEquals(1, users.size());
        UserResource user = managedRealm.admin().users().get(users.get(0).getId());
        UserRepresentation userRep = user.toRepresentation();

        RoleResource roleResource = managedRealm.admin().roles().get(role.toRepresentation().getName());
        List<RoleRepresentation> rolesToAdd = new LinkedList<>();
        rolesToAdd.add(roleResource.toRepresentation());
        managedRealm.admin().users().get(userRep.getId()).roles().realmLevel().add(rolesToAdd);

        roleResource = managedRealm.admin().roles().get(role.toRepresentation().getName());
        assertEquals(Collections.singletonList("test-role-member"), extractUsernames(roleResource.getUserMembers()));
    }

    private static List<String> extractUsernames(Collection<UserRepresentation> users) {
        return users.stream().map(UserRepresentation::getUsername).collect(Collectors.toList());
    }

    /**
     * KEYCLOAK-2035  Verifies that Role with no users assigned is being properly retrieved without members in API endpoint for role membership
     */
    @Test
    public void testUsersNotInRole() {
        RoleResource role = managedRealm.admin().roles().get("role-without-users");

        role = managedRealm.admin().roles().get(role.toRepresentation().getName());
        assertThat(role.getUserMembers(), is(empty()));
    }


    /**
     * KEYCLOAK-4978 Verifies that Groups assigned to Role are being properly retrieved as members in API endpoint for role membership
     */
    @Test
    public void testGroupsInRole() {
        RoleResource role = managedRealm.admin().roles().get("role-with-users");

        List<GroupRepresentation> groups = managedRealm.admin().groups().groups();
        GroupRepresentation groupRep = groups.stream().filter(g -> g.getPath().equals("/test-role-group")).findFirst().get();

        RoleResource roleResource = managedRealm.admin().roles().get(role.toRepresentation().getName());
        List<RoleRepresentation> rolesToAdd = new LinkedList<>();
        rolesToAdd.add(roleResource.toRepresentation());
        managedRealm.admin().groups().group(groupRep.getId()).roles().realmLevel().add(rolesToAdd);

        roleResource = managedRealm.admin().roles().get(role.toRepresentation().getName());

        Set<GroupRepresentation> groupsInRole = roleResource.getRoleGroupMembers();
        assertTrue(groupsInRole.stream().anyMatch(g -> g.getPath().equals("/test-role-group")));
    }

    /**
     * KEYCLOAK-4978  Verifies that Role with no users assigned is being properly retrieved without groups in API endpoint for role membership
     */
    @Test
    public void testGroupsNotInRole() {
        RoleResource role = managedRealm.admin().roles().get("role-without-users");

        role = managedRealm.admin().roles().get(role.toRepresentation().getName());

        Set<GroupRepresentation> groupsInRole = role.getRoleGroupMembers();
        assertTrue(groupsInRole.isEmpty());
    }

    /**
     * KEYCLOAK-2035 Verifies that Role Membership is ok after user removal
     */
    @Test
    public void roleMembershipAfterUserRemoval() {
        RoleResource role = managedRealm.admin().roles().get("role-with-users");

        List<UserRepresentation> users = managedRealm.admin().users().search("test-role-member", null, null, null, null, null);
        assertEquals(1, users.size());
        UserResource user = managedRealm.admin().users().get(users.get(0).getId());
        UserRepresentation userRep = user.toRepresentation();

        RoleResource roleResource = managedRealm.admin().roles().get(role.toRepresentation().getName());
        List<RoleRepresentation> rolesToAdd = new LinkedList<>();
        rolesToAdd.add(roleResource.toRepresentation());
        managedRealm.admin().users().get(userRep.getId()).roles().realmLevel().add(rolesToAdd);

        roleResource = managedRealm.admin().roles().get(role.toRepresentation().getName());
        assertEquals(Collections.singletonList("test-role-member"), extractUsernames(roleResource.getUserMembers()));

        managedRealm.admin().users().delete(userRep.getId());
        assertThat(roleResource.getUserMembers(), is(empty()));
    }

    @Test
    public void testRoleMembershipWithPagination() {
        RoleResource role = managedRealm.admin().roles().get("role-with-users");

        // Add a second user
        UserRepresentation userRep2 = new UserRepresentation();
        userRep2.setUsername("test-role-member2");
        userRep2.setEmail("test-role-member2@test-role-member.com");
        userRep2.setRequiredActions(Collections.<String>emptyList());
        userRep2.setEnabled(true);
        managedRealm.admin().users().create(userRep2);

        List<UserRepresentation> users = managedRealm.admin().users().search("test-role-member", null, null, null, null, null);
        assertThat(users, hasSize(2));
        for (UserRepresentation userRepFromList : users) {
            UserResource user = managedRealm.admin().users().get(userRepFromList.getId());
            UserRepresentation userRep = user.toRepresentation();

            RoleResource roleResource = managedRealm.admin().roles().get(role.toRepresentation().getName());
            List<RoleRepresentation> rolesToAdd = new LinkedList<>();
            rolesToAdd.add(roleResource.toRepresentation());
            managedRealm.admin().users().get(userRep.getId()).roles().realmLevel().add(rolesToAdd);
        }

        RoleResource roleResource = managedRealm.admin().roles().get(role.toRepresentation().getName());

        List<UserRepresentation> roleUserMembers = roleResource.getUserMembers(0, 1);
        assertEquals(Collections.singletonList("test-role-member"), extractUsernames(roleUserMembers));
        Assertions.assertNotNull(roleUserMembers.get(0).getNotBefore(), "Not in full representation");

        roleUserMembers = roleResource.getUserMembers(true, 1, 1);
        assertThat(roleUserMembers, hasSize(1));
        assertEquals(Collections.singletonList("test-role-member2"), extractUsernames(roleUserMembers));
        Assertions.assertNull(roleUserMembers.get(0).getNotBefore(), "Not in brief representation");

        roleUserMembers = roleResource.getUserMembers(true, 2, 1);
        assertThat(roleUserMembers, is(empty()));
    }

    // issue #9587
    @Test
    public void testSearchForRealmRoles() {
        managedRealm.admin().roles().list("role-", true).forEach(role -> assertThat("There is client role '" + role.getName() + "' among realm roles.", role.getClientRole(), is(false)));
    }

    @Test
    public void testSearchForRoles() {

        for(int i = 0; i<15; i++) {
            String roleName = "testrole"+i;
            RoleRepresentation role = makeRole(roleName);
            managedRealm.admin().roles().create(role);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleName), role, ResourceType.REALM_ROLE);
        }

        String roleNameA = "abcdefg";
        RoleRepresentation roleA = makeRole(roleNameA);
        managedRealm.admin().roles().create(roleA);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleNameA), roleA, ResourceType.REALM_ROLE);

        String roleNameB = "defghij";
        RoleRepresentation roleB = makeRole(roleNameB);
        managedRealm.admin().roles().create(roleB);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleNameB), roleB, ResourceType.REALM_ROLE);

        List<RoleRepresentation> resultSearch = managedRealm.admin().roles().list("defg", -1, -1);
        assertEquals(2,resultSearch.size());

        List<RoleRepresentation> resultSearch2 = managedRealm.admin().roles().list("testrole", -1, -1);
        assertEquals(15,resultSearch2.size());

        List<RoleRepresentation> resultSearchPagination = managedRealm.admin().roles().list("testrole", 1, 5);
        assertEquals(5,resultSearchPagination.size());
    }

    @Test
    public void testPaginationRoles() {

        for(int i = 0; i<15; i++) {
            String roleName = "role"+i;
            RoleRepresentation role = makeRole(roleName);
            managedRealm.admin().roles().create(role);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleName), role, ResourceType.REALM_ROLE);
        }

        List<RoleRepresentation> resultSearchPagination = managedRealm.admin().roles().list(1, 5);
        assertEquals(5,resultSearchPagination.size());

        List<RoleRepresentation> resultSearchPagination2 = managedRealm.admin().roles().list(5, 5);
        assertEquals(5,resultSearchPagination2.size());

        List<RoleRepresentation> resultSearchPagination3 = managedRealm.admin().roles().list(1, 5);
        assertEquals(5,resultSearchPagination3.size());

        List<RoleRepresentation> resultSearchPaginationIncoherentParams = managedRealm.admin().roles().list(1, null);
        assertTrue(resultSearchPaginationIncoherentParams.size() > 15);
    }

    @Test
    public void testPaginationRolesCache() {

        for(int i = 0; i<5; i++) {
            String roleName = "paginaterole"+i;
            RoleRepresentation role = makeRole(roleName);
            managedRealm.admin().roles().create(role);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleName), role, ResourceType.REALM_ROLE);
        }

        List<RoleRepresentation> resultBeforeAddingRoleToTestCache = managedRealm.admin().roles().list(1, 1000);

        // after a first call which init the cache, we add a new role to see if the result change

        RoleRepresentation role = makeRole("anewrole");
        managedRealm.admin().roles().create(role);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath("anewrole"), role, ResourceType.REALM_ROLE);

        List<RoleRepresentation> resultafterAddingRoleToTestCache = managedRealm.admin().roles().list(1, 1000);

        assertEquals(resultBeforeAddingRoleToTestCache.size()+1, resultafterAddingRoleToTestCache.size());
    }

    @Test
    public void getRolesWithFullRepresentation() {
        for(int i = 0; i<5; i++) {
            String roleName = "attributesrole"+i;
            RoleRepresentation role = makeRole(roleName);

            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("attribute1", Arrays.asList("value1","value2"));
            role.setAttributes(attributes);

            managedRealm.admin().roles().create(role);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleName), role, ResourceType.REALM_ROLE);
        }

        List<RoleRepresentation> roles = managedRealm.admin().roles().list("attributesrole", false);
        assertTrue(roles.get(0).getAttributes().containsKey("attribute1"));
    }

    @Test
    public void getRolesWithBriefRepresentation() {
        for(int i = 0; i<5; i++) {
            String roleName = "attributesrolebrief"+i;
            RoleRepresentation role = makeRole(roleName);

            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("attribute1", Arrays.asList("value1","value2"));
            role.setAttributes(attributes);

            managedRealm.admin().roles().create(role);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleName), role, ResourceType.REALM_ROLE);
        }

        List<RoleRepresentation> roles = managedRealm.admin().roles().list("attributesrolebrief", true);
        assertNull(roles.get(0).getAttributes());
    }

    @Test
    public void testDefaultRoles() {
        RoleResource defaultRole = managedRealm.admin().roles().get(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());

        UserRepresentation user = managedRealm.admin().users().search("test-role-member").get(0);

        UserResource userResource = managedRealm.admin().users().get(user.getId());
        assertThat(convertRolesToNames(userResource.roles().realmLevel().listAll()), hasItem(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName()));
        assertThat(convertRolesToNames(userResource.roles().realmLevel().listEffective()), allOf(
                hasItem(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName()),
                hasItem(Constants.OFFLINE_ACCESS_ROLE),
                hasItem(Constants.AUTHZ_UMA_AUTHORIZATION)
        ));

        defaultRole.addComposites(Collections.singletonList(managedRealm.admin().roles().get("role-a").toRepresentation()));

        userResource = managedRealm.admin().users().get(user.getId());
        assertThat(convertRolesToNames(userResource.roles().realmLevel().listAll()), allOf(
                hasItem(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName()),
                not(hasItem("role-a"))
        ));
        assertThat(convertRolesToNames(userResource.roles().realmLevel().listEffective()), allOf(
                hasItem(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName()),
                hasItem(Constants.OFFLINE_ACCESS_ROLE),
                hasItem(Constants.AUTHZ_UMA_AUTHORIZATION),
                hasItem("role-a")
        ));

        assertThat(userResource.roles().clientLevel(clientA.getId()).listAll(), empty());
        assertThat(userResource.roles().clientLevel(clientA.getId()).listEffective(), empty());

        defaultRole.addComposites(Collections.singletonList(managedRealm.admin().clients().get(clientA.getId()).roles().get("role-c").toRepresentation()));

        userResource = managedRealm.admin().users().get(user.getId());

        assertThat(userResource.roles().clientLevel(clientA.getId()).listAll(), empty());
        assertThat(convertRolesToNames(userResource.roles().clientLevel(clientA.getId()).listEffective()),
                hasItem("role-c")
        );
    }

    @Test
    public void testDeleteDefaultRole() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            managedRealm.admin().roles().deleteRole(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName());
        });
    }

    private RoleRepresentation makeRole(String name) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        return role;
    }

    private List<String> convertRolesToNames(List<RoleRepresentation> roles) {
        return roles.stream().map(RoleRepresentation::getName).collect(Collectors.toList());
    }

    private static class RealmRolesRealmConf implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder builder) {
            builder.addGroup("test-role-group").path("/test-role-group");
            builder.addUser("test-role-member").name("Test", "Role User").
                    email("test-role-member@test-role-member.com").emailVerified().requiredActions();
            builder.addClient("client-c").description("Client C");
            builder.addRole("role-a").description("Role A").attributes(Map.of("role-a-attr-key1", List.of("role-a-attr-val1")));
            builder.addRole("role-b").description("Role B");
            builder.addRole("role-with-users").description("Role with users");
            builder.addRole("role-without-users").description("role-without-users");

            return builder;
        }
    }
}
