package org.keycloak.tests.admin.group;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@KeycloakIntegrationTest(config = GroupAttributeSearchTest.GroupSearchServerConfig.class)
public class GroupAttributeSearchTest {

    @InjectRealm
    ManagedRealm managedRealm;

    private static final String GROUP1 = "group1";
    private static final String GROUP2 = "group2";
    private static final String GROUP3 = "group3";

    private static final String PARENT_GROUP = "parentGroup";

    private static final String CHILD_GROUP = "childGroup";

    private static final String ATTR_ORG_NAME = "org";
    private static final String ATTR_ORG_VAL = "Test_\"organisation\"";
    private static final String ATTR_URL_NAME = "url";
    private static final String ATTR_URL_VAL = "https://foo.bar/clflds";
    private static final String ATTR_FILTERED_NAME = "filtered";
    private static final String ATTR_FILTERED_VAL = "does_not_matter";

    private static final String ATTR_QUOTES_NAME = "test \"123\"";

    private static final String ATTR_QUOTES_NAME_ESCAPED = "\"test \\\"123\\\"\"";
    private static final String ATTR_QUOTES_VAL = "field=\"blah blah\"";
    private static final String ATTR_QUOTES_VAL_ESCAPED = "\"field=\\\"blah blah\\\"\"";

    private GroupRepresentation group1;
    private GroupRepresentation group2;
    private GroupRepresentation group3;
    private GroupRepresentation parentGroup;
    private GroupRepresentation childGroup;
    private GroupRepresentation secondChildGroup;

    @BeforeEach
    public void init() {
        group1 = new GroupRepresentation();
        group2 = new GroupRepresentation();
        group3 = new GroupRepresentation();
        parentGroup = new GroupRepresentation();
        childGroup = new GroupRepresentation();
        secondChildGroup = new GroupRepresentation();

        group1.setAttributes(Map.of(
                ATTR_ORG_NAME, List.of(ATTR_ORG_VAL),
                ATTR_URL_NAME, List.of(ATTR_URL_VAL)
        ));


        group2.setAttributes(Map.of(
                ATTR_FILTERED_NAME, List.of(ATTR_FILTERED_VAL),
                ATTR_URL_NAME, List.of(ATTR_URL_VAL)
        ));

        group3.setAttributes(Map.of(
                ATTR_ORG_NAME, List.of("fake group"),
                ATTR_QUOTES_NAME, List.of(ATTR_QUOTES_VAL)
        ));

        parentGroup.setAttributes(Map.of(
                ATTR_ORG_NAME, List.of("parentOrg")
        ));

        childGroup.setAttributes(Map.of(
                ATTR_ORG_NAME, List.of("childOrg")
        ));

        group1.setName(GROUP1);
        group2.setName(GROUP2);
        group3.setName(GROUP3);
        parentGroup.setName(PARENT_GROUP);
        childGroup.setName(CHILD_GROUP);
        secondChildGroup.setName(CHILD_GROUP + "2");
    }

    @Test
    public void querySearch() {
        createGroupWithCleanup(group1);
        createGroupWithCleanup(group2);
        createGroupWithCleanup(group3);

        search(buildSearchQuery(ATTR_ORG_NAME, ATTR_ORG_VAL), GROUP1);
        search(buildSearchQuery(ATTR_URL_NAME, ATTR_URL_VAL), GROUP1, GROUP2);
        search(buildSearchQuery(ATTR_ORG_NAME, ATTR_ORG_VAL, ATTR_URL_NAME, ATTR_URL_VAL), GROUP1);
        search(buildSearchQuery(ATTR_ORG_NAME, "wrong val", ATTR_URL_NAME, ATTR_URL_VAL));
        search(buildSearchQuery(ATTR_QUOTES_NAME_ESCAPED, ATTR_QUOTES_VAL_ESCAPED), GROUP3);

        // "filtered" attribute won't take effect when JPA is used
        search(buildSearchQuery(ATTR_URL_NAME, ATTR_URL_VAL, ATTR_FILTERED_NAME, ATTR_FILTERED_VAL), GROUP1, GROUP2);
    }

    @Test
    public void nestedGroupQuerySearch() {
        String groupUuid = createGroupWithCleanup(parentGroup);
        GroupResource parentGroupResource = managedRealm.admin().groups().group(groupUuid);
        parentGroupResource.subGroup(childGroup);

        // query for the child group by org name
        GroupsResource search = managedRealm.admin().groups();
        String searchQuery = buildSearchQuery(ATTR_ORG_NAME, "childOrg");
        List<GroupRepresentation> found = search.query(searchQuery);

        assertThat(found.size(), is(1));
        assertThat(found.get(0).getName(), is(equalTo(PARENT_GROUP)));

        List<GroupRepresentation> subGroups = found.get(0).getSubGroups();
        assertThat(subGroups.size(), is(1));
        assertThat(subGroups.get(0).getName(), is(equalTo(CHILD_GROUP)));
    }

    @Test
    public void nestedGroupQuerySearchNoHierarchy() throws Exception {
        String groupUuid = createGroupWithCleanup(parentGroup);
        GroupResource parentGroupResource = managedRealm.admin().groups().group(groupUuid);
        parentGroupResource.subGroup(childGroup);

        GroupRepresentation testGroup = new GroupRepresentation();
        testGroup.setName("test_child");
        parentGroupResource.subGroup(testGroup);

        // query for the child group by org name
        GroupsResource search = managedRealm.admin().groups();
        String searchQuery = buildSearchQuery(ATTR_ORG_NAME, "childOrg");
        List<GroupRepresentation> found = search.query(searchQuery, false);

        assertThat(found.size(), is(1));
        assertThat(found.get(0).getName(), is(equalTo(CHILD_GROUP)));

        String path = found.get(0).getPath();
        assertThat(path, is(String.format("/%s/%s", PARENT_GROUP, CHILD_GROUP)));
    }

    @Test
    public void queryPaging() {
        createGroupWithCleanup(group1);
        createGroupWithCleanup(group2);

        String searchQuery = buildSearchQuery(ATTR_URL_NAME, ATTR_URL_VAL);

        List<GroupRepresentation> firstPage = managedRealm.admin().groups()
                .query(searchQuery, true, 0, 1, true);
        assertThat(firstPage, hasSize(1));
        GroupRepresentation firstGroup = firstPage.get(0);
        assertThat(firstGroup.getName(), is(equalTo(GROUP1)));

        List<GroupRepresentation> secondPage = managedRealm.admin().groups()
                .query(searchQuery, true, 1, 1, true);
        assertThat(secondPage, hasSize(1));
        GroupRepresentation secondGroup = secondPage.get(0);
        assertThat(secondGroup.getName(), is(equalTo(GROUP2)));

        List<GroupRepresentation> thirdPage = managedRealm.admin().groups()
                .query(searchQuery, true, 2, 1, true);
        assertThat(thirdPage, is(empty()));
    }

    @Test
    public void queryFullRepresentation() {
        createGroupWithCleanup(group1);
        List<GroupRepresentation> found = managedRealm.admin().groups()
                .query(buildSearchQuery(ATTR_ORG_NAME, ATTR_ORG_VAL), true, 0, 100, false);

        assertThat(found, hasSize(1));
        GroupRepresentation group = found.get(0);
        assertThat(group.getName(), is(equalTo(GROUP1)));
        // attributes are not contained in group representation, only in full representation
        assertThat(group.getAttributes(), is(equalTo(group1.getAttributes())));
    }

    private String createGroupWithCleanup(GroupRepresentation rep) {
        Response resp = managedRealm.admin().groups().add(rep);
        String groupUuid = ApiUtil.getCreatedId(resp);
        managedRealm.cleanup().add(r -> r.groups().group(groupUuid).remove());
        return groupUuid;
    }

    private void search(String searchQuery, String... expectedGroupIds) {
        GroupsResource search = managedRealm.admin().groups();
        List<String> found = search.query(searchQuery).stream()
                .map(GroupRepresentation::getName)
                .collect(Collectors.toList());
        assertThat(found, hasSize(expectedGroupIds.length));
        assertThat(found, containsInAnyOrder(expectedGroupIds));
    }

    public static String buildSearchQuery(String firstAttrName, String firstAttrValue, String... furtherAttrKeysAndValues) {
        if (furtherAttrKeysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid length of furtherAttrKeysAndValues. Must be even, but is: " + furtherAttrKeysAndValues.length);
        }

        String keyValueSep = ":";
        String attributesSep = " ";

        StringBuilder sb = new StringBuilder();
        sb.append(firstAttrName).append(keyValueSep).append(firstAttrValue);

        if (furtherAttrKeysAndValues.length != 0) {
            for (int i = 0; i < furtherAttrKeysAndValues.length; i++) {
                if (i % 2 == 0) {
                    sb.append(attributesSep);
                } else {
                    sb.append(keyValueSep);
                }

                sb.append(furtherAttrKeysAndValues[i]);
            }
        }

        return sb.toString();
    }

    public static class GroupSearchServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("spi-group-jpa-searchable-attributes", String.join(",", ATTR_URL_NAME, ATTR_ORG_NAME, ATTR_QUOTES_NAME));
        }
    }
}
