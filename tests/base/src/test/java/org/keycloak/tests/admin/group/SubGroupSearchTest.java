package org.keycloak.tests.admin.group;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.utils.admin.ApiUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.keycloak.tests.admin.group.GroupSearchTest.ATTR_ORG_NAME;
import static org.keycloak.tests.admin.group.GroupSearchTest.ATTR_QUOTES_NAME;
import static org.keycloak.tests.admin.group.GroupSearchTest.ATTR_QUOTES_VAL;

@KeycloakIntegrationTest
public class SubGroupSearchTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @Test
    public void querySubGroups() {
        // create a parent group
        GroupRepresentation parentGroup = new GroupRepresentation();
        parentGroup.setAttributes(new HashMap<>() {{
            put(ATTR_ORG_NAME, Collections.singletonList("parentOrg"));
        }});
        parentGroup.setName("parentGroup");

        Response resp = managedRealm.admin().groups().add(parentGroup);
        String groupUuid = ApiUtil.getCreatedId(resp);
        managedRealm.cleanup().add(r -> r.groups().group(groupUuid).remove());
        GroupResource parentGroupResource = managedRealm.admin().groups().group(groupUuid);

        // create a subgroups in the parent
        for (int i = 1; i <= 5; i++) {
            GroupRepresentation testGroup = new GroupRepresentation();
            testGroup.setName("kcgroup-" + i);
            testGroup.setAttributes(new HashMap<>() {{
                put(ATTR_ORG_NAME, Collections.singletonList("keycloak org"));
                put(ATTR_QUOTES_NAME, Collections.singletonList(ATTR_QUOTES_VAL));
            }});
            parentGroupResource.subGroup(testGroup);
        }
        for (int i = 1; i <= 3; i++) {
            GroupRepresentation testGroup = new GroupRepresentation();
            testGroup.setName("testgroup-" + i);
            parentGroupResource.subGroup(testGroup);
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
        // attributes should be present in the returned subgroup.
        Map<String, List<String>> attributes = subGroups.get(0).getAttributes();
        assertThat(attributes, not(anEmptyMap()));
        assertThat(attributes.keySet(), hasSize(2));
        assertThat(attributes.keySet(), containsInAnyOrder(ATTR_ORG_NAME, ATTR_QUOTES_NAME));
    }
}
