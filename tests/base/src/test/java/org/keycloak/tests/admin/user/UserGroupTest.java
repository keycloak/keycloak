package org.keycloak.tests.admin.user;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.realm.GroupConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Test;

import static org.keycloak.tests.utils.Assert.assertNames;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class UserGroupTest extends AbstractUserTest {

    @Test
    public void testGetGroupsForUserFullRepresentation() {
        String userName = "averagejoe";
        String groupName = "groupWithAttribute";
        Map<String, List<String>> attributes = new HashMap<String, List<String>>();
        attributes.put("attribute1", Arrays.asList("attribute1","attribute2"));

        UserRepresentation userRepresentation = UserConfigBuilder.create()
                .username(userName).name("average", "joe").password("password")
                .email("joe@average.com").emailVerified(true).build();

        GroupRepresentation groupRepresentation = GroupConfigBuilder.create().name(groupName).setAttributes(attributes).build();

        String userId = createUser(userRepresentation);
        String groupId = createGroup(groupRepresentation).getId();

        UserResource user = managedRealm.admin().users().get(userId);
        user.joinGroup(groupId);

        List<GroupRepresentation> userGroups = user.groups(0, 100, false);

        assertFalse(userGroups.isEmpty());
        assertTrue(userGroups.get(0).getAttributes().containsKey("attribute1"));
    }

    @Test
    public void testGetSearchedGroupsForUserFullRepresentation() {
        String userName = "averagejoe";
        String groupName1 = "group1WithAttribute";
        String groupName2 = "group2WithAttribute";
        Map<String, List<String>> attributes1 = new HashMap<String, List<String>>();
        attributes1.put("attribute1", Arrays.asList("attribute1"));
        Map<String, List<String>> attributes2 = new HashMap<String, List<String>>();
        attributes2.put("attribute2", Arrays.asList("attribute2"));

        UserRepresentation userRepresentation = UserConfigBuilder.create()
                .username(userName).name("average", "joe").password("password")
                .email("joe@average.com").emailVerified(true).build();

        GroupRepresentation groupRepresentation = GroupConfigBuilder.create().name(groupName1).setAttributes(attributes1).build();
        GroupRepresentation groupRepresentation2 = GroupConfigBuilder.create().name(groupName2).setAttributes(attributes2).build();

        String userId = createUser(userRepresentation);

        String group1Id = createGroup(groupRepresentation).getId();
        String group2Id = createGroup(groupRepresentation2).getId();

        UserResource user = managedRealm.admin().users().get(userId);
        user.joinGroup(group1Id);
        user.joinGroup(group2Id);

        List<GroupRepresentation> userGroups = user.groups("group2", false);
        assertFalse(userGroups.isEmpty());
        assertTrue(userGroups.stream().collect(Collectors.toMap(GroupRepresentation::getName, Function.identity())).get(groupName2).getAttributes().containsKey("attribute2"));

        userGroups = user.groups("group3", false);
        assertTrue(userGroups.isEmpty());
    }

    @Test
    public void groupMembershipPaginated() {
        String userId = createUser(UserConfigBuilder.create().username("user-a").build());

        for (int i = 1; i <= 10; i++) {
            GroupRepresentation group = new GroupRepresentation();
            group.setName("group-" + i);
            String groupId = createGroup(group).getId();
            managedRealm.admin().users().get(userId).joinGroup(groupId);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userGroupPath(userId, groupId), group, ResourceType.GROUP_MEMBERSHIP);
        }

        List<GroupRepresentation> groups = managedRealm.admin().users().get(userId).groups(5, 6);
        assertEquals(groups.size(), 5);
        assertNames(groups, "group-5","group-6","group-7","group-8","group-9");
    }

    @Test
    public void groupMembershipSearch() {
        String userId = createUser(UserConfigBuilder.create().username("user-b").build());

        for (int i = 1; i <= 10; i++) {
            GroupRepresentation group = new GroupRepresentation();
            group.setName("group-" + i);
            String groupId = createGroup(group).getId();
            managedRealm.admin().users().get(userId).joinGroup(groupId);
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userGroupPath(userId, groupId), group, ResourceType.GROUP_MEMBERSHIP);
        }

        List<GroupRepresentation> groups = managedRealm.admin().users().get(userId).groups("-3", 0, 10);
        assertThat(managedRealm.admin().users().get(userId).groupsCount("-3").get("count"), is(1L));
        assertEquals(1, groups.size());
        assertNames(groups, "group-3");

        List<GroupRepresentation> groups2 = managedRealm.admin().users().get(userId).groups("1", 0, 10);
        assertThat(managedRealm.admin().users().get(userId).groupsCount("1").get("count"), is(2L));
        assertEquals(2, groups2.size());
        assertNames(groups2, "group-1", "group-10");

        List<GroupRepresentation> groups3 = managedRealm.admin().users().get(userId).groups("1", 2, 10);
        assertEquals(0, groups3.size());

        List<GroupRepresentation> groups4 = managedRealm.admin().users().get(userId).groups("gr", 2, 10);
        assertThat(managedRealm.admin().users().get(userId).groupsCount("gr").get("count"), is(10L));
        assertEquals(8, groups4.size());

        List<GroupRepresentation> groups5 = managedRealm.admin().users().get(userId).groups("Gr", 2, 10);
        assertEquals(8, groups5.size());
    }

    @Test
    public void createUserWithGroups() {
        String username = "user-with-groups";
        String groupToBeAdded = "test-group";

        createGroup(GroupConfigBuilder.create().name(groupToBeAdded).build());

        UserRepresentation build = UserConfigBuilder.create()
                .username(username)
                .groups(groupToBeAdded)
                .build();

        //when
        String userId = createUser(build);
        List<GroupRepresentation> obtainedGroups = managedRealm.admin().users().get(userId).groups();

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

        UserRepresentation userRepresentation = UserConfigBuilder.create().username(username).build();

        GroupRepresentation subGroupRep = GroupConfigBuilder.create().name(subGroupName).build();
        GroupRepresentation parentGroupRep = GroupConfigBuilder.create().name(parentGroupName).subGroups(subGroupRep).build();

        String userId = createUser(userRepresentation);

        String subGroupId = createGroup(subGroupRep).getId();
        String parentGroupId = createGroup(parentGroupRep).getId();

        UserResource user = managedRealm.admin().users().get(userId);

        //when
        user.joinGroup(subGroupId);
        List<GroupRepresentation> obtainedGroups = managedRealm.admin().users().get(userId).groups();

        //then
        assertEquals(1, obtainedGroups.size());
        assertEquals(subGroupName, obtainedGroups.get(0).getName());

        //when
        user.joinGroup(parentGroupId);
        obtainedGroups = managedRealm.admin().users().get(userId).groups();

        //then
        assertEquals(2, obtainedGroups.size());
        assertEquals(parentGroupName, obtainedGroups.get(0).getName());
        assertEquals(subGroupName, obtainedGroups.get(1).getName());
    }

    @Test
    public void joinSubGroupAfterParentGroup() {
        String username = "user-with-sub-and-parent-group";
        String parentGroupName = "parent-group";
        String subGroupName = "sub-group";

        UserRepresentation userRepresentation = UserConfigBuilder.create().username(username).build();
        GroupRepresentation subGroupRep = GroupConfigBuilder.create().name(subGroupName).build();
        GroupRepresentation parentGroupRep = GroupConfigBuilder.create().name(parentGroupName).subGroups(subGroupRep).build();

        String userId = createUser(userRepresentation);
        String subGroupId = createGroup(subGroupRep).getId();
        String parentGroupId = createGroup(parentGroupRep).getId();

        UserResource user = managedRealm.admin().users().get(userId);

        //when
        user.joinGroup(parentGroupId);
        List<GroupRepresentation> obtainedGroups = managedRealm.admin().users().get(userId).groups();

        //then
        assertEquals(1, obtainedGroups.size());
        assertEquals(parentGroupName, obtainedGroups.get(0).getName());

        //when
        user.joinGroup(subGroupId);
        obtainedGroups = managedRealm.admin().users().get(userId).groups();

        //then
        assertEquals(2, obtainedGroups.size());
        assertEquals(parentGroupName, obtainedGroups.get(0).getName());
        assertEquals(subGroupName, obtainedGroups.get(1).getName());
    }

    private GroupRepresentation createGroup(GroupRepresentation group) {
        final String groupId;

        try (Response response = managedRealm.admin().groups().add(group)) {
            groupId = ApiUtil.getCreatedId(response);
        }

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.groupPath(groupId), group, ResourceType.GROUP);

        group.setId(groupId);
        return group;
    }
}
