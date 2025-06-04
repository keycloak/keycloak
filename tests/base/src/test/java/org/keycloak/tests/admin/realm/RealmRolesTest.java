package org.keycloak.tests.admin.realm;

import jakarta.ws.rs.ClientErrorException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.testsuite.util.RoleBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;


@KeycloakIntegrationTest
public class RealmRolesTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectClient
    ManagedClient managedClient;

    @InjectAdminEvents
    AdminEvents adminEvents;

    private final String roleNameA = "role-a";
    private final String roleNameWithUsers = "role-with-users";
    private final String roleNameWithoutUsers = "role-without-users";
    private static final Map<String, List<String>> ROLE_A_ATTRIBUTES =
            Collections.singletonMap("role-a-attr-key1", Collections.singletonList("role-a-attr-val1"));

    @BeforeEach
    public void before() {
        managedRealm.admin().roles().create(RoleBuilder.create().name(roleNameWithUsers).description("Role with users").build());
        managedRealm.admin().roles().create(RoleBuilder.create().name(roleNameWithoutUsers).description("role-without-users").build());

        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("test-role-member");
        userRep.setEmail("test-role-member@test-role-member.com");
        userRep.setRequiredActions(Collections.<String>emptyList());
        userRep.setEnabled(true);
        managedRealm.admin().users().create(userRep);

        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("test-role-group");
        groupRep.setPath("/test-role-group");
        managedRealm.admin().groups().add(groupRep);

        /*managedRealm.admin().roles().create(RoleBuilder.create()
                .name(roleNameA)
                .description("Role A")
                .attributes(ROLE_A_ATTRIBUTES)
                .build()
        );*/
    }

    @Test //already in RoleByIdResourceTest
    public void getRole() {

    }

    @Test //already in RoleByIdResourceTest
    public void updateRole() {

    }

    @Test //already in of RoleByIdResourceTest
    public void deleteRole() {

    }

    @Test //already in RoleByIdResourceTest
    public void composites() {

    }

    @Test
    public void createRoleWithSameName() {
        //add role A
        //Assertions.assertThrowsExactly(ClientErrorException.class, () -> managedRealm.admin().roles().create(RoleBuilder.create().name(roleNameA).build()));
    }

    /**
     * KEYCLOAK-2035 Verifies that Users assigned to Role are being properly retrieved as members in API endpoint for role membership
     */
    @Test
    public void testUsersInRole() {
        RoleResource role = managedRealm.admin().roles().get(roleNameWithUsers);

        List<UserRepresentation> users = managedRealm.admin().users().search("test-role-member");
        Assertions.assertEquals(1, users.size());
        UserResource user = managedRealm.admin().users().get(users.get(0).getId());
        UserRepresentation userRep = user.toRepresentation();

        RoleResource roleResource = managedRealm.admin().roles().get(role.toRepresentation().getName());
        List<RoleRepresentation> rolesToAdd = new LinkedList<>();
        rolesToAdd.add(roleResource.toRepresentation());
        managedRealm.admin().users().get(userRep.getId()).roles().realmLevel().add(rolesToAdd);

        roleResource = managedRealm.admin().roles().get(role.toRepresentation().getName());
        Assertions.assertEquals(Collections.singletonList("test-role-member"), extractUsernames(roleResource.getUserMembers()));
    }

    private static List<String> extractUsernames(Collection<UserRepresentation> users) {
        return users.stream().map(UserRepresentation::getUsername).collect(Collectors.toList());
    }

    /**
     * KEYCLOAK-2035  Verifies that Role with no users assigned is being properly retrieved without members in API endpoint for role membership
     */
    @Test
    public void testUsersNotInRole() {
        RoleResource role = managedRealm.admin().roles().get(roleNameWithoutUsers);

        role = managedRealm.admin().roles().get(role.toRepresentation().getName());
        assertThat(role.getUserMembers(), is(empty()));
    }

    /**
     * KEYCLOAK-4978 Verifies that Groups assigned to Role are being properly retrieved as members in API endpoint for role membership
     */
    @Test
    public void testGroupsInRole() {
        RoleResource role = managedRealm.admin().roles().get(roleNameWithUsers);

        List<GroupRepresentation> groups = managedRealm.admin().groups().groups();
        GroupRepresentation groupRep = groups.stream().filter(g -> g.getPath().equals("/test-role-group")).findFirst().get();

        List<RoleRepresentation> rolesToAdd = new LinkedList<>();
        rolesToAdd.add(role.toRepresentation());
        managedRealm.admin().groups().group(groupRep.getId()).roles().realmLevel().add(rolesToAdd);

        Set<GroupRepresentation> groupsInRole = managedRealm.admin().roles().get(roleNameWithUsers).getRoleGroupMembers();
        Assertions.assertTrue(groupsInRole.stream().filter(g -> g.getPath().equals("/test-role-group")).findFirst().isPresent());
    }

    /**
     * KEYCLOAK-4978  Verifies that Role with no users assigned is being properly retrieved without groups in API endpoint for role membership
     */
    @Test
    public void testGroupsNotInRole() {
        RoleResource role = managedRealm.admin().roles().get(roleNameWithoutUsers);

        Set<GroupRepresentation> groupsInRole = role.getRoleGroupMembers();
        Assertions.assertTrue(groupsInRole.isEmpty());
    }

    /**
     * KEYCLOAK-2035 Verifies that Role Membership is ok after user removal
     */
    @Test
    public void roleMembershipAfterUserRemoval() {
        RoleResource role = managedRealm.admin().roles().get(roleNameWithUsers);

        List<UserRepresentation> users = managedRealm.admin().users().search("test-role-member", null, null, null, null, null);
        Assertions.assertEquals(1, users.size());
        UserResource user = managedRealm.admin().users().get(users.get(0).getId());
        UserRepresentation userRep = user.toRepresentation();

        RoleResource roleResource = managedRealm.admin().roles().get(role.toRepresentation().getName());
        List<RoleRepresentation> rolesToAdd = new LinkedList<>();
        rolesToAdd.add(roleResource.toRepresentation());
        managedRealm.admin().users().get(userRep.getId()).roles().realmLevel().add(rolesToAdd);

        roleResource = managedRealm.admin().roles().get(role.toRepresentation().getName());
        Assertions.assertEquals(Collections.singletonList("test-role-member"), extractUsernames(roleResource.getUserMembers()));

        managedRealm.admin().users().delete(userRep.getId());
        assertThat(roleResource.getUserMembers(), is(empty()));
    }

    @Test
    public void testRoleMembershipWithPagination() {
        RoleResource role = managedRealm.admin().roles().get(roleNameWithUsers);

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

            List<RoleRepresentation> rolesToAdd = new LinkedList<>();
            rolesToAdd.add(role.toRepresentation());
            managedRealm.admin().users().get(userRep.getId()).roles().realmLevel().add(rolesToAdd);
        }

        List<UserRepresentation> roleUserMembers = role.getUserMembers(0, 1);
        Assertions.assertEquals(Collections.singletonList("test-role-member"), extractUsernames(roleUserMembers));
        Assertions.assertNotNull(roleUserMembers.get(0).getNotBefore()); //"Not in full representation"

        roleUserMembers = role.getUserMembers(true, 1, 1);
        assertThat(roleUserMembers, hasSize(1));
        Assertions.assertEquals(Collections.singletonList("test-role-member2"), extractUsernames(roleUserMembers));
        Assertions.assertNull(roleUserMembers.get(0).getNotBefore()); //"Not in brief representation"

        roleUserMembers = role.getUserMembers(true, 2, 1);
        assertThat(roleUserMembers, is(empty()));
    }

    @Test
    public void testSearchForRealmRoles() {
        managedRealm.admin().roles().list("role-", true).stream().forEach(role -> assertThat("There is client role '" + role.getName() + "' among realm roles.", role.getClientRole(), is(false)));
    }

    @Test
    public void testSearchForRoles() {
        adminEvents.clear(); //TODO: should clear or remove beforeEach?

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
        Assertions.assertEquals(2,resultSearch.size());

        List<RoleRepresentation> resultSearch2 = managedRealm.admin().roles().list("testrole", -1, -1);
        Assertions.assertEquals(15,resultSearch2.size());

        List<RoleRepresentation> resultSearchPagination = managedRealm.admin().roles().list("testrole", 1, 5);
        Assertions.assertEquals(5, resultSearchPagination.size());
    }

    private RoleRepresentation makeRole(String name) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        return role;
    }

    @Test
    public void testPaginationRoles() {
        adminEvents.clear(); //TODO: should clear or remove beforeEach?

        for(int i = 0; i<15; i++) {
            String roleName = "role"+i;
            RoleRepresentation role = makeRole(roleName);
            managedRealm.admin().roles().create(role);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleName), role, ResourceType.REALM_ROLE);
        }

        List<RoleRepresentation> resultSearchPagination = managedRealm.admin().roles().list(1, 5);
        Assertions.assertEquals(5, resultSearchPagination.size());

        List<RoleRepresentation> resultSearchPagination2 = managedRealm.admin().roles().list(5, 5);
        Assertions.assertEquals(5,resultSearchPagination2.size());

        List<RoleRepresentation> resultSearchPagination3 = managedRealm.admin().roles().list(1, 5);
        Assertions.assertEquals(5,resultSearchPagination3.size());

        List<RoleRepresentation> resultSearchPaginationIncoherentParams = managedRealm.admin().roles().list(1, null);
        Assertions.assertTrue(resultSearchPaginationIncoherentParams.size() > 15);
    }

    @Test
    public void testPaginationRolesCache() {
        adminEvents.clear(); //TODO: should clear or remove beforeEach?

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

        Assertions.assertEquals(resultBeforeAddingRoleToTestCache.size()+1, resultafterAddingRoleToTestCache.size());
    }

    @Test
    public void getRolesWithFullRepresentation() {
        adminEvents.clear(); //TODO: should clear or remove beforeEach?

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
        Assertions.assertTrue(roles.get(0).getAttributes().containsKey("attribute1"));
    }

    @Test
    public void getRolesWithBriefRepresentation() {
        adminEvents.clear(); //TODO: should clear or remove beforeEach?

        for(int i = 0; i<5; i++) {
            String roleName = "attributesrolebrief"+i;
            RoleRepresentation role = makeRole(roleName);

            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("attribute1", Arrays.asList("value1","value2"));
            role.setAttributes(attributes);

            managedRealm.admin().roles().create(role);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath(roleName), role, ResourceType.REALM_ROLE);
        }

        List<RoleRepresentation> roles =  managedRealm.admin().roles().list("attributesrolebrief", true);
        Assertions.assertNull(roles.get(0).getAttributes());
    }

    @Test
    public void testDefaultRoles() {
        //TODO: add role A, Constants?
        /*adminEvents.clear(); //TODO: should clear or remove beforeEach?
        String REALM_NAME = managedRealm.getName();

        List<RoleRepresentation> defaultRoles = managedRealm.admin().roles().list();

        RoleResource defaultRole =  managedRealm.admin().roles().get(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + REALM_NAME);

        UserRepresentation user =  managedRealm.admin().users().search("test-role-member").get(0);

        UserResource userResource = managedRealm.admin().users().get(user.getId());
        assertThat(convertRolesToNames(userResource.roles().realmLevel().listAll()), hasItem(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + REALM_NAME));
        assertThat(convertRolesToNames(userResource.roles().realmLevel().listEffective()), allOf(
                hasItem(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + REALM_NAME),
                hasItem(Constants.OFFLINE_ACCESS_ROLE),
                hasItem(Constants.AUTHZ_UMA_AUTHORIZATION)
        ));

        defaultRole.addComposites(Collections.singletonList( managedRealm.admin().roles().get("role-a").toRepresentation()));

        userResource =  managedRealm.admin().users().get(user.getId());
        assertThat(convertRolesToNames(userResource.roles().realmLevel().listAll()), allOf(
                hasItem(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + REALM_NAME),
                not(hasItem("role-a"))
        ));
        assertThat(convertRolesToNames(userResource.roles().realmLevel().listEffective()), allOf(
                hasItem(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + REALM_NAME),
                hasItem(Constants.OFFLINE_ACCESS_ROLE),
                hasItem(Constants.AUTHZ_UMA_AUTHORIZATION),
                hasItem("role-a")
        ));

        assertThat(userResource.roles().clientLevel( managedClient.getClientId()).listAll(), empty());
        assertThat(userResource.roles().clientLevel( managedClient.getClientId()).listEffective(), empty());

        defaultRole.addComposites(Collections.singletonList(managedRealm.admin().clients().get( managedClient.getClientId()).roles().get("role-c").toRepresentation()));

        userResource =  managedRealm.admin().users().get(user.getId());

        assertThat(userResource.roles().clientLevel( managedClient.getClientId()).listAll(), empty());
        assertThat(convertRolesToNames(userResource.roles().clientLevel( managedClient.getClientId()).listEffective()),
                hasItem("role-c")
        );*/
    }

    @Test //(expected = BadRequestException.class)
    public void testDeleteDefaultRole() {

    }

    private List<String> convertRolesToNames(List<RoleRepresentation> roles) {
        return roles.stream().map(RoleRepresentation::getName).collect(Collectors.toList());
    }
}
