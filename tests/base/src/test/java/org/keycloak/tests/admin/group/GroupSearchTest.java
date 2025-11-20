package org.keycloak.tests.admin.group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.realm.GroupConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import com.google.common.collect.Comparators;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class GroupSearchTest extends AbstractGroupTest {

    @InjectRealm(config = GroupSearchTestRealmConfig.class)
    ManagedRealm managedRealm;

    private static final String ATTR_ORG_NAME = "org";
    private static final String ATTR_QUOTES_NAME = "test \"123\"";
    private static final String ATTR_QUOTES_VAL = "field=\"blah blah\"";

    @Test
    public void querySubGroups() {
        // create a parent group
        GroupRepresentation parentGroup = GroupConfigBuilder.create()
                .name("parentGroup")
                .attribute(ATTR_ORG_NAME, "parentOrg")
                .build();
        String groupUuid = createGroup(managedRealm, parentGroup);
        GroupResource parentGroupResource = managedRealm.admin().groups().group(groupUuid);

        // create a subgroups in the parent
        for (int i = 1; i <= 5; i++) {
            GroupRepresentation testGroup = GroupConfigBuilder.create()
                    .name("kcgroup-" + i)
                    .attribute(ATTR_ORG_NAME, "kcgroup-" + i)
                    .attribute(ATTR_QUOTES_NAME, ATTR_QUOTES_VAL)
                    .build();
            addSubGroup(managedRealm, parentGroup, testGroup);

            if (i == 2) {
                GroupRepresentation subGroup = GroupConfigBuilder.create()
                        .name("kcsubgroup-" + i)
                        .build();

                addSubGroup(managedRealm, testGroup, subGroup);
            }
        }
        for (int i = 1; i <= 3; i++) {
            GroupRepresentation testGroup = GroupConfigBuilder.create()
                    .name("testgroup-" + i)
                    .build();
            addSubGroup(managedRealm, parentGroup, testGroup);
        }

        // search for subgroups filtering by name - all groups with 'kc' in the name.
        List<GroupRepresentation> subGroups = parentGroupResource.getSubGroups("kc", false, 0, 10, true);
        assertThat(subGroups, hasSize(5));
        for (int i = 1; i <= 5; i++) {
            // subgroups should be ordered by name.
            assertThat(subGroups.get(i - 1).getName(), is(equalTo("kcgroup-" + i)));
            assertThat(subGroups.get(i - 1).getAttributes(), is(nullValue())); // brief rep - no attributes should be returned in subgroups.
        }

        // search for subgroups filtering by name - all groups with 'test' in the name.
        subGroups = parentGroupResource.getSubGroups("test", false, 0, 10, true);
        assertThat(subGroups, hasSize(3));
        for (int i = 1; i <= 3; i++) {
            assertThat(subGroups.get(i - 1).getName(), is(equalTo("testgroup-" + i)));
        }

        // search for subgroups filtering by name - all groups with 'gro' in the name.
        subGroups = parentGroupResource.getSubGroups("gro", false, 0, 10, true);
        assertThat(subGroups, hasSize(8));

        // search using a string that matches none of the subgroups.
        subGroups = parentGroupResource.getSubGroups("nonexistent", false, 0, 10, false);
        assertThat(subGroups, is(empty()));

        // exact search with full representation - only one subgroup should be returned.
        subGroups = parentGroupResource.getSubGroups("kcgroup-2", true, 0, 10, false);
        assertThat(subGroups, hasSize(1));
        assertThat(subGroups.get(0).getName(), is(equalTo("kcgroup-2")));
        assertThat(subGroups.get(0).getSubGroupCount(), is(1L));
        // attributes should be present in the returned subgroup.
        Map<String, List<String>> attributes = subGroups.get(0).getAttributes();
        assertThat(attributes, not(anEmptyMap()));
        assertThat(attributes.keySet(), hasSize(2));
        assertThat(attributes.keySet(), containsInAnyOrder(ATTR_ORG_NAME, ATTR_QUOTES_NAME));

        subGroups = parentGroupResource.getSubGroups("kcgroup-2", true, 0, 10, false, false);
        assertThat(subGroups, hasSize(1));
        assertThat(subGroups.get(0).getName(), is(equalTo("kcgroup-2")));
        assertThat(subGroups.get(0).getSubGroupCount(), is(nullValue()));

        subGroups = managedRealm.admin().groups().groups("kcgroup-2", true, 0, 1, true);
        Assertions.assertEquals(1, subGroups.size());
        assertThat(subGroups.get(0).getName(), is(equalTo(parentGroup.getName())));
        Assertions.assertEquals(1, subGroups.get(0).getSubGroups().size());
        assertThat(subGroups.get(0).getSubGroups().get(0).getName(), is(equalTo("kcgroup-2")));
        assertThat(subGroups.get(0).getSubGroups().get(0).getSubGroupCount(), is(1L));

        subGroups = managedRealm.admin().groups().groups("kcgroup-2", true, 0, 1, true, false);
        Assertions.assertEquals(1, subGroups.size());
        assertThat(subGroups.get(0).getName(), is(equalTo(parentGroup.getName())));
        Assertions.assertEquals(1, subGroups.get(0).getSubGroups().size());
        assertThat(subGroups.get(0).getSubGroups().get(0).getName(), is(equalTo("kcgroup-2")));
        assertThat(subGroups.get(0).getSubGroups().get(0).getSubGroupCount(), is(nullValue()));
    }

    /**
     * Groups search with query returns unwanted groups
     *
     * @link https://issues.redhat.com/browse/KEYCLOAK-18380
     */
    @Test
    public void searchForGroupsShouldOnlyReturnMatchingElementsOrIntermediatePaths() {

        /*
         * /g1/g1.1-gugu
         * /g1/g1.2-test1234
         * /g2-test1234
         * /g3/g3.1-test1234/g3.1.1
         */
        String needle = "test1234";
        GroupRepresentation g1 = GroupConfigBuilder.create().name("g1").build();
        GroupRepresentation g1_1 = GroupConfigBuilder.create().name("g1.1-bubu").build();
        GroupRepresentation g1_2 = GroupConfigBuilder.create().name("g1.2-" + needle).build();
        GroupRepresentation g2 = GroupConfigBuilder.create().name("g2-" + needle).build();
        GroupRepresentation g3 = GroupConfigBuilder.create().name("g3").build();
        GroupRepresentation g3_1 = GroupConfigBuilder.create().name("g3.1-" + needle).build();
        GroupRepresentation g3_1_1 = GroupConfigBuilder.create().name("g3.1.1").build();

        createGroup(managedRealm, g1);
        createGroup(managedRealm, g2);
        createGroup(managedRealm, g3);
        addSubGroup(managedRealm, g1, g1_1);
        addSubGroup(managedRealm, g1, g1_2);
        addSubGroup(managedRealm, g3, g3_1);
        addSubGroup(managedRealm, g3_1, g3_1_1);

        RealmResource realm = managedRealm.admin();
        // we search for "test1234" and expect only /g1/g1.2-test1234, /g2-test1234 and /g3/g3.1-test1234 as a result
        List<GroupRepresentation> result = realm.groups().groups(needle, 0, 100);

        assertEquals(3, result.size());
        assertEquals("g1", result.get(0).getName());
        assertEquals(1, result.get(0).getSubGroups().size());
        assertEquals("g1.2-" + needle, result.get(0).getSubGroups().get(0).getName());
        assertEquals("g2-" + needle, result.get(1).getName());
        assertEquals("g3", result.get(2).getName());
        assertEquals(1, result.get(2).getSubGroups().size());
        assertEquals("g3.1-" + needle, result.get(2).getSubGroups().get(0).getName());
    }

    @Test
    public void searchGroupsByName() {
        createGroup(managedRealm, GroupConfigBuilder.create().name("group-name-1").build());
        createGroup(managedRealm, GroupConfigBuilder.create().name("group-name-2").build());

        GroupsResource groupsResource = managedRealm.admin().groups();
        List<GroupRepresentation> groups;

        // Search containing name success
        groups = groupsResource.groups("group-name", false, 0, 20, false);
        assertEquals(2, groups.size());
        // Search exact name success
        groups = groupsResource.groups("group-name-1", true, 0, 20, false);
        assertEquals(1, groups.size());
        // Search exact name failure
        groups = groupsResource.groups("group-name", true, 0, 20, false);
        assertTrue(groups.isEmpty());
    }

    @Test
    public void searchAndCountGroups() {
        String firstGroupId = "";

        RealmResource realm = managedRealm.admin();

        // Add 20 new groups with known names
        for (int i = 0; i < 20; i++) {
            String groupId = createGroup(managedRealm, GroupConfigBuilder.create().name("group" + i).build());
            if (i == 0) {
                firstGroupId = groupId;
            }
        }

        // Get groups by search and pagination
        List<GroupRepresentation> allGroups = realm.groups().groups();
        assertEquals(20, allGroups.size());

        List<GroupRepresentation> slice = realm.groups().groups(0, 7);
        assertEquals(7, slice.size());

        slice = realm.groups().groups(null, 7);
        assertEquals(7, slice.size());

        slice = realm.groups().groups(10, null);
        assertEquals(10, slice.size());

        slice = realm.groups().groups(5, 7);
        assertEquals(7, slice.size());

        slice = realm.groups().groups(15, 7);
        assertEquals(5, slice.size());

        List<GroupRepresentation> search = realm.groups().groups("group1", 0, 20);
        assertEquals(11, search.size());
        for (GroupRepresentation group : search) {
            assertTrue(group.getName().contains("group1"));
        }

        List<GroupRepresentation> noResultSearch = realm.groups().groups("abcd", 0, 20);
        assertEquals(0, noResultSearch.size());

        // Count
        assertEquals(Long.valueOf(allGroups.size()), realm.groups().count().get("count"));
        assertEquals(Long.valueOf(search.size()), realm.groups().count("group1").get("count"));
        assertEquals(Long.valueOf(noResultSearch.size()), realm.groups().count("abcd").get("count"));

        // Add a subgroup for onlyTopLevel flag testing
        GroupRepresentation level2Group = new GroupRepresentation();
        level2Group.setName("group1111");
        Response response = realm.groups().group(firstGroupId).subGroup(level2Group);
        response.close();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.groupSubgroupsPath(firstGroupId), level2Group, ResourceType.GROUP);

        assertEquals(Long.valueOf(allGroups.size()), realm.groups().count(true).get("count"));
        assertEquals(Long.valueOf(allGroups.size() + 1), realm.groups().count(false).get("count"));
        //add another subgroup
        GroupRepresentation level2Group2 = new GroupRepresentation();
        level2Group2.setName("group111111");
        realm.groups().group(firstGroupId).subGroup(level2Group2);
        //search and count for group with string group11 -> return 2 top level group, group11 and group0 having subgroups group1111 and group111111
        search = realm.groups().groups("group11", 0, 10);
        assertEquals(2, search.size());
        GroupRepresentation group0 = search.stream().filter(group -> "group0".equals(group.getName())).findAny().orElseGet(null);
        assertNotNull(group0);
        assertEquals(2, group0.getSubGroups().size());
        assertThat(group0.getSubGroups().stream().map(GroupRepresentation::getName).collect(Collectors.toList()), Matchers.containsInAnyOrder("group1111", "group111111"));
        assertEquals(countLeafGroups(search), realm.groups().count("group11").get("count"));
    }

    private Long countLeafGroups(List<GroupRepresentation> search) {
        long counter = 0;
        for (GroupRepresentation group : search) {
            if (group.getSubGroups().isEmpty()) {
                counter += 1;
                continue;
            }
            counter += countLeafGroups(group.getSubGroups());
        }
        return counter;
    }

    @Test
    public void orderGroupsByName() {
        // Create two pages worth of groups in a random order
        List<GroupRepresentation> testGroups = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            GroupRepresentation group = new GroupRepresentation();
            group.setName("group" + i);
            testGroups.add(group);
        }

        Collections.shuffle(testGroups);

        for (GroupRepresentation group : testGroups) {
            createGroup(managedRealm, group);
        }

        // Groups should be ordered by name
        Comparator<GroupRepresentation> compareByName = Comparator.comparing(GroupRepresentation::getName);

        GroupsResource groups = managedRealm.admin().groups();
        // Assert that all groups are returned in order
        List<GroupRepresentation> allGroups = groups.groups(0, 100);
        assertEquals(40, allGroups.size());
        assertTrue(Comparators.isInStrictOrder(allGroups, compareByName));

        // Assert that pagination results are returned in order
        List<GroupRepresentation> firstPage = groups.groups(0, 20);
        assertEquals(20, firstPage.size());
        assertTrue(Comparators.isInStrictOrder(firstPage, compareByName));

        List<GroupRepresentation> secondPage = groups.groups(20, 20);
        assertEquals(20, secondPage.size());
        assertTrue(Comparators.isInStrictOrder(secondPage, compareByName));

        // Check that the ordering of groups across multiple pages is correct
        // Since the individual pages are ordered it is sufficient to compare the
        // last group from the first page to the first group of the second page
        GroupRepresentation lastGroupOnFirstPage = firstPage.get(firstPage.size() - 1);
        GroupRepresentation firstGroupOnSecondPage = secondPage.get(0);
        assertTrue(compareByName.compare(lastGroupOnFirstPage, firstGroupOnSecondPage) < 0);
    }

    /**
     * Verifies that the group search works the same across group provider implementations for hierarchies
     *
     * @link https://issues.jboss.org/browse/KEYCLOAK-18390
     */
    @Test
    public void searchGroupsOnGroupHierarchies() {
        final RealmResource realm = managedRealm.admin();

        final String searchFor = UUID.randomUUID().toString();

        final GroupRepresentation g1 = new GroupRepresentation();
        g1.setName("g1");
        final GroupRepresentation g1_1 = new GroupRepresentation();
        g1_1.setName("g1.1-" + searchFor);

        String g1Id = createGroup(managedRealm, g1);
        addSubGroup(managedRealm, g1, g1_1);

        final List<GroupRepresentation> searchResultGroups = realm.groups().groups(searchFor, 0, 10);

        assertFalse(searchResultGroups.isEmpty());
        assertEquals(g1Id, searchResultGroups.get(0).getId());
        assertEquals(g1.getName(), searchResultGroups.get(0).getName());

        List<GroupRepresentation> searchResultSubGroups = searchResultGroups.get(0).getSubGroups();
        assertEquals(g1_1.getId(), searchResultSubGroups.get(0).getId());
        assertEquals(g1_1.getName(), searchResultSubGroups.get(0).getName());
    }

    public void testParentAndChildGroup(String parentName, String childName) {
        RealmResource realm = managedRealm.admin();
        GroupRepresentation parentGroup = new GroupRepresentation();
        parentGroup.setName(parentName);
        createGroup(managedRealm, parentGroup);
        GroupRepresentation childGroup = new GroupRepresentation();
        childGroup.setName(childName);
        Response response = realm.groups().group(parentGroup.getId()).subGroup(childGroup);
        childGroup.setId(ApiUtil.getCreatedId(response));

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.CREATE)
                .resourcePath(AdminEventPaths.groupSubgroupsPath(parentGroup.getId()))
                .representation(childGroup)
                .resourceType(ResourceType.GROUP);

        List<GroupRepresentation> groupsFound = realm.groups().groups(parentGroup.getName(), true, 0, 1, true);
        Assertions.assertEquals(1, groupsFound.size());
        Assertions.assertEquals(parentGroup.getId(), groupsFound.get(0).getId());
        parentGroup = groupsFound.get(0);
        Assertions.assertEquals(0, parentGroup.getSubGroups().size());
        Assertions.assertEquals(KeycloakModelUtils.buildGroupPath(GroupProvider.DEFAULT_ESCAPE_SLASHES, parentName),
                parentGroup.getPath());

        groupsFound = realm.groups().groups(childGroup.getName(), true, 0, 1, true);
        Assertions.assertEquals(1, groupsFound.size());
        Assertions.assertEquals(parentGroup.getId(), groupsFound.get(0).getId());
        Assertions.assertEquals(1, groupsFound.get(0).getSubGroups().size());
        Assertions.assertEquals(childGroup.getId(), groupsFound.get(0).getSubGroups().get(0).getId());
        childGroup = groupsFound.get(0).getSubGroups().get(0);
        Assertions.assertEquals(KeycloakModelUtils.normalizeGroupPath(
                        KeycloakModelUtils.buildGroupPath(GroupProvider.DEFAULT_ESCAPE_SLASHES, parentName, childName)),
                childGroup.getPath());

        GroupRepresentation groupFound = realm.getGroupByPath(parentGroup.getPath());
        Assertions.assertNotNull(groupFound);
        Assertions.assertEquals(parentGroup.getId(), groupFound.getId());
        groupFound = realm.getGroupByPath(childGroup.getPath());
        Assertions.assertNotNull(groupFound);
        Assertions.assertEquals(childGroup.getId(), groupFound.getId());

        realm.groups().group(childGroup.getId()).remove();
    }

    @Test
    public void testGroupsWithSpaces() {
        testParentAndChildGroup("parent space", "child space");
    }

    @Test
    public void testGroupsWithSlashes() {
        testParentAndChildGroup("parent/slash", "child/slash");
    }

    private static class GroupSearchTestRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm.eventsEnabled(true);
        }
    }

}
