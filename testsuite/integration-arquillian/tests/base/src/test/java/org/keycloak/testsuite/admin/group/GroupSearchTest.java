package org.keycloak.testsuite.admin.group;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.GroupProvider;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.containers.AbstractQuarkusDeployableContainer;
import org.keycloak.testsuite.updaters.Creator;

public class GroupSearchTest extends AbstractGroupTest {
    @ArquillianResource
    protected ContainerController controller;

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

    private static final String SEARCHABLE_ATTRS_PROP = "keycloak.group.searchableAttributes";

    GroupRepresentation group1;
    GroupRepresentation group2;
    GroupRepresentation group3;
    GroupRepresentation parentGroup;
    GroupRepresentation childGroup;

    @Before
    public void init() {
        group1 = new GroupRepresentation();
        group2 = new GroupRepresentation();
        group3 = new GroupRepresentation();
        parentGroup = new GroupRepresentation();
        childGroup = new GroupRepresentation();

        group1.setAttributes(new HashMap<>() {{
            put(ATTR_ORG_NAME, Collections.singletonList(ATTR_ORG_VAL));
            put(ATTR_URL_NAME, Collections.singletonList(ATTR_URL_VAL));
        }});

        group2.setAttributes(new HashMap<>() {{
            put(ATTR_FILTERED_NAME, Collections.singletonList(ATTR_FILTERED_VAL));
            put(ATTR_URL_NAME, Collections.singletonList(ATTR_URL_VAL));
        }});

        group3.setAttributes(new HashMap<>() {{
            put(ATTR_ORG_NAME, Collections.singletonList("fake group"));
            put(ATTR_QUOTES_NAME, Collections.singletonList(ATTR_QUOTES_VAL));
        }});

        childGroup.setAttributes(new HashMap<>() {{
            put(ATTR_ORG_NAME, Collections.singletonList("parentOrg"));
        }});

        childGroup.setAttributes(new HashMap<>() {{
            put(ATTR_ORG_NAME, Collections.singletonList("childOrg"));
        }});

        group1.setName(GROUP1);
        group2.setName(GROUP2);
        group3.setName(GROUP3);
        parentGroup.setName(PARENT_GROUP);
        childGroup.setName(CHILD_GROUP);
    }

    public RealmResource testRealmResource() {
        return adminClient.realm(TEST);
    }

    @Test
    public void querySearch() throws Exception {
        configureSearchableAttributes();
        try (Creator<GroupResource> groupCreator1 = Creator.create(testRealmResource(), group1);
             Creator<GroupResource> groupCreator2 = Creator.create(testRealmResource(), group2);
             Creator<GroupResource> groupCreator3 = Creator.create(testRealmResource(), group3)) {
            search(buildSearchQuery(ATTR_ORG_NAME, ATTR_ORG_VAL), GROUP1);
            search(buildSearchQuery(ATTR_URL_NAME, ATTR_URL_VAL), GROUP1, GROUP2);
            search(buildSearchQuery(ATTR_ORG_NAME, ATTR_ORG_VAL, ATTR_URL_NAME, ATTR_URL_VAL),
                    GROUP1);
            search(buildSearchQuery(ATTR_ORG_NAME, "wrong val", ATTR_URL_NAME, ATTR_URL_VAL));
            search(buildSearchQuery(ATTR_QUOTES_NAME_ESCAPED, ATTR_QUOTES_VAL_ESCAPED), GROUP3);

            // "filtered" attribute won't take effect when JPA is used
            String[] expectedRes = isLegacyJpaStore() ? new String[]{GROUP1, GROUP2} : new String[]{GROUP2};
            search(buildSearchQuery(ATTR_URL_NAME, ATTR_URL_VAL, ATTR_FILTERED_NAME, ATTR_FILTERED_VAL), expectedRes);
        } finally {
            resetSearchableAttributes();
        }
    }

    @Test
    public void nestedGroupQuerySearch() throws Exception {
        configureSearchableAttributes();
        try (Creator<GroupResource> parentGroupCreator = Creator.create(testRealmResource(), parentGroup)) {
            parentGroupCreator.resource().subGroup(childGroup);
            // query for the child group by org name
            GroupsResource search = testRealmResource().groups();
            String searchQuery = buildSearchQuery(ATTR_ORG_NAME, "childOrg");
            List<GroupRepresentation> found = search.query(searchQuery);

            assertThat(found.size(), is(1));
            assertThat(found.get(0).getName(), is(equalTo(PARENT_GROUP)));

            List<GroupRepresentation> subGroups = found.get(0).getSubGroups();
            assertThat(subGroups.size(), is(1));
            assertThat(subGroups.get(0).getName(), is(equalTo(CHILD_GROUP)));
        } finally {
            resetSearchableAttributes();
        }
    }

    @Test
    public void nestedGroupQuerySearchNoHierarchy() throws Exception {
        configureSearchableAttributes();
        try (Creator<GroupResource> parentGroupCreator = Creator.create(testRealmResource(), parentGroup)) {
            parentGroupCreator.resource().subGroup(childGroup);

            GroupRepresentation testGroup = new GroupRepresentation();
            testGroup.setName("test_child");
            parentGroupCreator.resource().subGroup(testGroup);

            // query for the child group by org name
            GroupsResource search = testRealmResource().groups();
            String searchQuery = buildSearchQuery(ATTR_ORG_NAME, "childOrg");
            List<GroupRepresentation> found = search.query(searchQuery, false);

            assertThat(found.size(), is(1));
            assertThat(found.get(0).getName(), is(equalTo(CHILD_GROUP)));

            String path = found.get(0).getPath();
            assertThat(path, is(String.format("/%s/%s", PARENT_GROUP, CHILD_GROUP)));
        } finally {
            resetSearchableAttributes();
        }
    }

    @Test
    public void queryPaging() throws Exception {
        configureSearchableAttributes();
        try (Creator<GroupResource> group1Creator = Creator.create(testRealmResource(), group1);
             Creator<GroupResource> group2Creator = Creator.create(testRealmResource(), group2)) {
            String searchQuery = buildSearchQuery(ATTR_URL_NAME, ATTR_URL_VAL);

            List<GroupRepresentation> firstPage = testRealmResource().groups()
                    .query(searchQuery, true, 0, 1, true);
            assertThat(firstPage, hasSize(1));
            GroupRepresentation firstGroup = firstPage.get(0);
            assertThat(firstGroup.getName(), is(equalTo(GROUP1)));

            List<GroupRepresentation> secondPage = testRealmResource().groups()
                    .query(searchQuery, true, 1, 1, true);
            assertThat(secondPage, hasSize(1));
            GroupRepresentation secondGroup = secondPage.get(0);
            assertThat(secondGroup.getName(), is(equalTo(GROUP2)));

            List<GroupRepresentation> thirdPage = testRealmResource().groups()
                    .query(searchQuery, true, 2, 1, true);
            assertThat(thirdPage, is(empty()));
        } finally {
            resetSearchableAttributes();
        }
    }

    @Test
    public void queryFullRepresentation() throws Exception {
        configureSearchableAttributes();
        try (Creator<GroupResource> group1Creator = Creator.create(testRealmResource(), group1)) {
            List<GroupRepresentation> found = testRealmResource().groups()
                    .query(buildSearchQuery(ATTR_ORG_NAME, ATTR_ORG_VAL), true, 0, 100, false);

            assertThat(found, hasSize(1));
            GroupRepresentation group = found.get(0);
            assertThat(group.getName(), is(equalTo(GROUP1)));
            // attributes are not contained in group representation, only in full representation
            assertThat(group.getAttributes(), is(equalTo(group1.getAttributes())));
        } finally {
            resetSearchableAttributes();
        }
    }

    private void search(String searchQuery, String... expectedGroupIds) {
        GroupsResource search = testRealmResource().groups();
        List<String> found = search.query(searchQuery).stream()
                .map(GroupRepresentation::getName)
                .collect(Collectors.toList());
        assertThat(found, hasSize(expectedGroupIds.length));
        assertThat(found, containsInAnyOrder(expectedGroupIds));
    }

    void configureSearchableAttributes() throws Exception {
        String[] searchableAttributes = new String[]{ATTR_URL_NAME, ATTR_ORG_NAME, ATTR_QUOTES_NAME};
        log.infov("Configuring searchableAttributes");
        if (suiteContext.getAuthServerInfo().isUndertow()) {
            controller.stop(suiteContext.getAuthServerInfo().getQualifier());
            System.setProperty(SEARCHABLE_ATTRS_PROP, String.join(",", searchableAttributes));
            controller.start(suiteContext.getAuthServerInfo().getQualifier());
        } else if (suiteContext.getAuthServerInfo().isQuarkus()) {
            String s = String.join(",", searchableAttributes);
            controller.stop(suiteContext.getAuthServerInfo().getQualifier());
            AbstractQuarkusDeployableContainer container = (AbstractQuarkusDeployableContainer) suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer();
            container.setAdditionalBuildArgs(
                    Collections.singletonList("--spi-group-jpa-searchable-attributes=" + s));
            controller.start(suiteContext.getAuthServerInfo().getQualifier());
        } else {
            throw new RuntimeException("Don't know how to config");
        }
        reconnectAdminClient();
    }

    void resetSearchableAttributes() throws Exception {
        log.info("Reset searchableAttributes");

        if (suiteContext.getAuthServerInfo().isUndertow()) {
            controller.stop(suiteContext.getAuthServerInfo().getQualifier());
            System.clearProperty(SEARCHABLE_ATTRS_PROP);
            controller.start(suiteContext.getAuthServerInfo().getQualifier());
        } else if (suiteContext.getAuthServerInfo().isQuarkus()) {
            AbstractQuarkusDeployableContainer container = (AbstractQuarkusDeployableContainer) suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer();
            container.setAdditionalBuildArgs(Collections.emptyList());
            container.restartServer();
        } else {
            throw new RuntimeException("Don't know how to config");
        }
        reconnectAdminClient();
    }

    private static String buildSearchQuery(String firstAttrName, String firstAttrValue, String... furtherAttrKeysAndValues) {
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

    private boolean isLegacyJpaStore() {
        return keycloakUsingProviderWithId(GroupProvider.class, "jpa");
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        loadTestRealm(testRealmReps);
    }
}
