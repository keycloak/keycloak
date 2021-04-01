package org.keycloak.testsuite.cluster;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.util.GroupBuilder;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.Assert.assertNames;

/**
 *
 * @author tkyjovsk
 */
public class GroupInvalidationClusterTest extends AbstractInvalidationClusterTestWithTestRealm<GroupRepresentation, GroupResource> {

    @Before
    public void setExcludedComparisonFields() {
        excludedComparisonFields.add("subGroups");
    }

    @Override
    protected GroupRepresentation createTestEntityRepresentation() {
        GroupRepresentation group = new GroupRepresentation();
        group.setName("group_" + RandomStringUtils.randomAlphabetic(5));
        group.setAttributes(new HashMap<String, List<String>>());
        group.getAttributes().put("attr1", Arrays.asList(new String[]{"attr1 value"}));
        group.getAttributes().put("attr2", Arrays.asList(new String[]{"attr2 value", "attr2 value2"}));
        return group;
    }

    protected GroupsResource groups(ContainerInfo node) {
        return getAdminClientFor(node).realm(testRealmName).groups();
    }

    @Override
    protected GroupResource entityResource(GroupRepresentation group, ContainerInfo node) {
        return entityResource(group.getId(), node);
    }

    @Override
    protected GroupResource entityResource(String id, ContainerInfo node) {
        return groups(node).group(id);
    }

    @Override
    protected GroupRepresentation createEntity(GroupRepresentation group, ContainerInfo node) {
        Response response = groups(node).add(group);
        String id = ApiUtil.getCreatedId(response);
        response.close();
        group.setId(id);
        return readEntity(group, node);
    }

    @Override
    protected GroupRepresentation readEntity(GroupRepresentation group, ContainerInfo node) {
        GroupRepresentation u = null;
        try {
            u = entityResource(group, node).toRepresentation();
        } catch (NotFoundException nfe) {
            // expected when group doesn't exist
        }
        return u;
    }

    @Override
    protected GroupRepresentation updateEntity(GroupRepresentation group, ContainerInfo node) {
        entityResource(group, node).update(group);
        return readEntity(group, node);
    }

    @Override
    protected void deleteEntity(GroupRepresentation group, ContainerInfo node) {
        entityResource(group, node).remove();
        assertNull(readEntity(group, node));
    }

    @Override
    protected GroupRepresentation testEntityUpdates(GroupRepresentation group, boolean backendFailover) {

        // groupname
        group.setName(group.getName() + "_updated");
        group = updateEntityOnCurrentFailNode(group, "name");
        verifyEntityUpdateDuringFailover(group, backendFailover);

        // attributes - add new
        group.getAttributes().put("attr3", Arrays.asList(new String[]{"attr3 value"}));
        group = updateEntityOnCurrentFailNode(group, "attributes - adding");
        verifyEntityUpdateDuringFailover(group, backendFailover);

        // attributes - remove
        group.getAttributes().remove("attr3");
        group = updateEntityOnCurrentFailNode(group, "attributes - removing");
        verifyEntityUpdateDuringFailover(group, backendFailover);

        // attributes - update 1
        group.getAttributes().get("attr1").set(0,
                group.getAttributes().get("attr1").get(0) + " - updated");
        group = updateEntityOnCurrentFailNode(group, "attributes");
        verifyEntityUpdateDuringFailover(group, backendFailover);

        // attributes - update 2
        group.getAttributes().get("attr2").set(1,
                group.getAttributes().get("attr2").get(1) + " - updated");
        group = updateEntityOnCurrentFailNode(group, "attributes");
        verifyEntityUpdateDuringFailover(group, backendFailover);

        // move 
        log.info("Updating Group parent on " + getCurrentFailNode());
        GroupRepresentation parentGroup = new GroupRepresentation();
        parentGroup.setName("parent");
        parentGroup = createEntityOnCurrentFailNode(parentGroup);
        assertEquals("/" + parentGroup.getName(), parentGroup.getPath());

        Response r = entityResourceOnCurrentFailNode(parentGroup).subGroup(group);
        r.close();
        parentGroup = readEntityOnCurrentFailNode(parentGroup);
        group = readEntityOnCurrentFailNode(group);

        assertTrue(ApiUtil.groupContainsSubgroup(parentGroup, group));
        assertEquals(parentGroup.getPath() + "/" + group.getName(), group.getPath());

        verifyEntityUpdateDuringFailover(group, backendFailover);
        parentGroup = readEntityOnCurrentFailNode(parentGroup);

        // Add new child
        GroupRepresentation childGroup2 = GroupBuilder.create()
                .name("childGroup2")
                .build();
        r = entityResourceOnCurrentFailNode(parentGroup).subGroup(childGroup2);
        String childGroup2Id = ApiUtil.getCreatedId(r);
        childGroup2.setId(childGroup2Id);


        parentGroup = readEntityOnCurrentFailNode(parentGroup);
        verifyEntityUpdateDuringFailover(parentGroup, backendFailover);

        // Verify same child groups on both nodes
        GroupRepresentation parentGroupOnOtherNode = readEntityOnCurrentFailNode(parentGroup);
        assertNames(parentGroup.getSubGroups(), group.getName(), "childGroup2");
        assertNames(parentGroupOnOtherNode.getSubGroups(), group.getName(), "childGroup2");

        // Remove childGroup2
        deleteEntityOnCurrentFailNode(childGroup2);

        return group;
    }

}
