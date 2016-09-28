package org.keycloak.testsuite.cluster;

import org.apache.commons.lang.RandomStringUtils;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.arquillian.ContainerInfo;

import javax.ws.rs.NotFoundException;

import static org.junit.Assert.assertNull;

/**
 *
 * @author tkyjovsk
 */
public class RoleInvalidationClusterTest extends AbstractInvalidationClusterTestWithTestRealm<RoleRepresentation, RoleResource> {

    @Override
    protected RoleRepresentation createTestEntityRepresentation() {
        RoleRepresentation role = new RoleRepresentation();
        role.setName("role_" + RandomStringUtils.randomAlphabetic(5));
        role.setComposite(false);
        role.setDescription("description of "+role.getName());
        return role;
    }

    protected RolesResource roles(ContainerInfo node) {
        return getAdminClientFor(node).realm(testRealmName).roles();
    }

    @Override
    protected RoleResource entityResource(RoleRepresentation role, ContainerInfo node) {
        return entityResource(role.getName(), node);
    }

    @Override
    protected RoleResource entityResource(String name, ContainerInfo node) {
        return roles(node).get(name);
    }

    @Override
    protected RoleRepresentation createEntity(RoleRepresentation role, ContainerInfo node) {
        roles(node).create(role);
        return readEntity(role, node);
    }

    @Override
    protected RoleRepresentation readEntity(RoleRepresentation role, ContainerInfo node) {
        RoleRepresentation u = null;
        try {
            u = entityResource(role, node).toRepresentation();
        } catch (NotFoundException nfe) {
            // expected when role doesn't exist
        }
        return u;
    }

    @Override
    protected RoleRepresentation updateEntity(RoleRepresentation role, ContainerInfo node) {
        return updateEntity(role.getName(), role, node);
    }

    private RoleRepresentation updateEntity(String roleName, RoleRepresentation role, ContainerInfo node) {
        entityResource(roleName, node).update(role);
        return readEntity(role, node);
    }

    @Override
    protected void deleteEntity(RoleRepresentation role, ContainerInfo node) {
        entityResource(role, node).remove();
        assertNull(readEntity(role, node));
    }

    @Override
    protected RoleRepresentation testEntityUpdates(RoleRepresentation role, boolean backendFailover) {

        // description
        role.setDescription(role.getDescription()+"_- updated");
        role = updateEntityOnCurrentFailNode(role, "description");
        verifyEntityUpdateDuringFailover(role, backendFailover);
        
        // TODO composites

        return role;
    }

}
