package org.keycloak.tests.admin.user;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.tests.utils.admin.ApiUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@KeycloakIntegrationTest
public class UserGroupTest extends AbstractUserTest {

    @Test
    public void testGetGroupsForUserFullRepresentation() {
        RealmResource realm = adminClient.realms().realm("test");

        String userName = "averagejoe";
        String groupName = "groupWithAttribute";
        Map<String, List<String>> attributes = new HashMap<String, List<String>>();
        attributes.put("attribute1", Arrays.asList("attribute1","attribute2"));

        UserRepresentation userRepresentation = UserBuilder
                .edit(createUserRepresentation(userName, "joe@average.com", "average", "joe", true))
                .addPassword("password")
                .build();

        try (Creator<UserResource> u = Creator.create(realm, userRepresentation);
             Creator<GroupResource> g = Creator.create(realm, GroupBuilder.create().name(groupName).attributes(attributes).build())) {

            String groupId = g.id();
            UserResource user = u.resource();
            user.joinGroup(groupId);

            List<GroupRepresentation> userGroups = user.groups(0, 100, false);

            assertFalse(userGroups.isEmpty());
            assertTrue(userGroups.get(0).getAttributes().containsKey("attribute1"));
        }
    }

    @Test
    public void testGetSearchedGroupsForUserFullRepresentation() {
        RealmResource realm = adminClient.realms().realm("test");

        String userName = "averagejoe";
        String groupName1 = "group1WithAttribute";
        String groupName2 = "group2WithAttribute";
        Map<String, List<String>> attributes1 = new HashMap<String, List<String>>();
        attributes1.put("attribute1", Arrays.asList("attribute1"));
        Map<String, List<String>> attributes2 = new HashMap<String, List<String>>();
        attributes2.put("attribute2", Arrays.asList("attribute2"));

        UserRepresentation userRepresentation = UserBuilder
                .edit(createUserRepresentation(userName, "joe@average.com", "average", "joe", true))
                .addPassword("password")
                .build();

        try (Creator<UserResource> u = Creator.create(realm, userRepresentation);
             Creator<GroupResource> g1 = Creator.create(realm, GroupBuilder.create().name(groupName1).attributes(attributes1).build());
             Creator<GroupResource> g2 = Creator.create(realm, GroupBuilder.create().name(groupName2).attributes(attributes2).build())) {

            String group1Id = g1.id();
            String group2Id = g2.id();
            UserResource user = u.resource();
            user.joinGroup(group1Id);
            user.joinGroup(group2Id);

            List<GroupRepresentation> userGroups = user.groups("group2", false);
            assertFalse(userGroups.isEmpty());
            assertTrue(userGroups.stream().collect(Collectors.toMap(GroupRepresentation::getName, Function.identity())).get(groupName2).getAttributes().containsKey("attribute2"));

            userGroups = user.groups("group3", false);
            assertTrue(userGroups.isEmpty());
        }
    }

    @Test
    public void groupMembershipPaginated() {
        String userId = createUser(UserBuilder.create().username("user-a").build());

        for (int i = 1; i <= 10; i++) {
            GroupRepresentation group = new GroupRepresentation();
            group.setName("group-" + i);
            String groupId = createGroup(realm, group).getId();
            realm.users().get(userId).joinGroup(groupId);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userGroupPath(userId, groupId), group, ResourceType.GROUP_MEMBERSHIP);
        }

        List<GroupRepresentation> groups = realm.users().get(userId).groups(5, 6);
        assertEquals(groups.size(), 5);
        assertNames(groups, "group-5","group-6","group-7","group-8","group-9");
    }

    @Test
    public void groupMembershipSearch() {
        String userId = createUser(UserBuilder.create().username("user-b").build());

        for (int i = 1; i <= 10; i++) {
            GroupRepresentation group = new GroupRepresentation();
            group.setName("group-" + i);
            String groupId = createGroup(realm, group).getId();
            realm.users().get(userId).joinGroup(groupId);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userGroupPath(userId, groupId), group, ResourceType.GROUP_MEMBERSHIP);
        }

        List<GroupRepresentation> groups = realm.users().get(userId).groups("-3", 0, 10);
        assertThat(realm.users().get(userId).groupsCount("-3").get("count"), is(1L));
        assertEquals(1, groups.size());
        assertNames(groups, "group-3");

        List<GroupRepresentation> groups2 = realm.users().get(userId).groups("1", 0, 10);
        assertThat(realm.users().get(userId).groupsCount("1").get("count"), is(2L));
        assertEquals(2, groups2.size());
        assertNames(groups2, "group-1", "group-10");

        List<GroupRepresentation> groups3 = realm.users().get(userId).groups("1", 2, 10);
        assertEquals(0, groups3.size());

        List<GroupRepresentation> groups4 = realm.users().get(userId).groups("gr", 2, 10);
        assertThat(realm.users().get(userId).groupsCount("gr").get("count"), is(10L));
        assertEquals(8, groups4.size());

        List<GroupRepresentation> groups5 = realm.users().get(userId).groups("Gr", 2, 10);
        assertEquals(8, groups5.size());
    }

    @Test
    public void createUserWithGroups() {
        String username = "user-with-groups";
        String groupToBeAdded = "test-group";

        createGroup(realm, GroupBuilder.create().name(groupToBeAdded).build());

        UserRepresentation build = UserBuilder.create()
                .username(username)
                .addGroups(groupToBeAdded)
                .build();

        //when
        String userId = createUser(build);
        List<GroupRepresentation> obtainedGroups = realm.users().get(userId).groups();

        //then
        assertEquals(1, obtainedGroups.size());
        assertEquals(groupToBeAdded, obtainedGroups.get(0).getName());
    }

    /**
     * Test for #9482
     */
    @Test
    public void joinParentGroupAfterSubGroup() {
        String username = "user-with-sub-and-parent-group";
        String parentGroupName = "parent-group";
        String subGroupName = "sub-group";

        UserRepresentation userRepresentation = UserBuilder.create().username(username).build();

        GroupRepresentation subGroupRep = GroupBuilder.create().name(subGroupName).build();
        GroupRepresentation parentGroupRep = GroupBuilder.create().name(parentGroupName).subGroups(List.of(subGroupRep)).build();

        try (Creator<UserResource> u = Creator.create(realm, userRepresentation);
             Creator<GroupResource> subgroup = Creator.create(realm, subGroupRep);
             Creator<GroupResource> parentGroup = Creator.create(realm, parentGroupRep)) {

            UserResource user = u.resource();

            //when
            user.joinGroup(subgroup.id());
            List<GroupRepresentation> obtainedGroups = realm.users().get(u.id()).groups();

            //then
            assertEquals(1, obtainedGroups.size());
            assertEquals(subGroupName, obtainedGroups.get(0).getName());

            //when
            user.joinGroup(parentGroup.id());
            obtainedGroups = realm.users().get(u.id()).groups();

            //then
            assertEquals(2, obtainedGroups.size());
            assertEquals(parentGroupName, obtainedGroups.get(0).getName());
            assertEquals(subGroupName, obtainedGroups.get(1).getName());
        }
    }

    @Test
    public void joinSubGroupAfterParentGroup() {
        String username = "user-with-sub-and-parent-group";
        String parentGroupName = "parent-group";
        String subGroupName = "sub-group";

        UserRepresentation userRepresentation = UserBuilder.create().username(username).build();
        GroupRepresentation subGroupRep = GroupBuilder.create().name(subGroupName).build();
        GroupRepresentation parentGroupRep = GroupBuilder.create().name(parentGroupName).subGroups(List.of(subGroupRep)).build();

        try (Creator<UserResource> u = Creator.create(realm, userRepresentation);
             Creator<GroupResource> subgroup = Creator.create(realm, subGroupRep);
             Creator<GroupResource> parentGroup = Creator.create(realm, parentGroupRep)) {

            UserResource user = u.resource();

            //when
            user.joinGroup(parentGroup.id());
            List<GroupRepresentation> obtainedGroups = realm.users().get(u.id()).groups();

            //then
            assertEquals(1, obtainedGroups.size());
            assertEquals(parentGroupName, obtainedGroups.get(0).getName());

            //when
            user.joinGroup(subgroup.id());
            obtainedGroups = realm.users().get(u.id()).groups();

            //then
            assertEquals(2, obtainedGroups.size());
            assertEquals(parentGroupName, obtainedGroups.get(0).getName());
            assertEquals(subGroupName, obtainedGroups.get(1).getName());
        }
    }

    private GroupRepresentation createGroup(RealmResource realm, GroupRepresentation group) {
        final String groupId;
        try (Response response = realm.groups().add(group)) {
            groupId = ApiUtil.getCreatedId(response);
            getCleanup().addGroupId(groupId);
        }

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.groupPath(groupId), group, ResourceType.GROUP);

        // Set ID to the original rep
        group.setId(groupId);
        return group;
    }
}
