package org.keycloak.testsuite.cluster;

import org.apache.commons.lang.RandomStringUtils;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.ContainerInfo;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertNull;

/**
 *
 * @author tkyjovsk
 */
public class UserInvalidationClusterTest extends AbstractInvalidationClusterTestWithTestRealm<UserRepresentation, UserResource> {

    @Override
    protected UserRepresentation createTestEntityRepresentation() {
        String firstName = "user";
        String lastName = RandomStringUtils.randomAlphabetic(5);
        UserRepresentation user = new UserRepresentation();
        user.setUsername(firstName + "_" + lastName);
        user.setEmail(user.getUsername() + "@email.test");
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }

    protected UsersResource users(ContainerInfo node) {
        return getAdminClientFor(node).realm(testRealmName).users();
    }

    @Override
    protected UserResource entityResource(UserRepresentation user, ContainerInfo node) {
        return entityResource(user.getId(), node);
    }

    @Override
    protected UserResource entityResource(String id, ContainerInfo node) {
        return users(node).get(id);
    }

    @Override
    protected UserRepresentation createEntity(UserRepresentation user, ContainerInfo node) {
        Response response = users(node).create(user);
        String id = ApiUtil.getCreatedId(response);
        response.close();
        user.setId(id);
        return readEntity(user, node);
    }

    @Override
    protected UserRepresentation readEntity(UserRepresentation user, ContainerInfo node) {
        UserRepresentation u = null;
        try {
            u = entityResource(user, node).toRepresentation();
        } catch (NotFoundException nfe) {
            // expected when user doesn't exist
        }
        return u;
    }

    @Override
    protected UserRepresentation updateEntity(UserRepresentation user, ContainerInfo node) {
        entityResource(user, node).update(user);
        return readEntity(user, node);
    }

    @Override
    protected void deleteEntity(UserRepresentation user, ContainerInfo node) {
        entityResource(user, node).remove();
        assertNull(readEntity(user, node));
    }

    @Override
    protected UserRepresentation testEntityUpdates(UserRepresentation user, boolean backendFailover) {

        // username
        user.setUsername(user.getUsername() + "_updated");
        user = updateEntityOnCurrentFailNode(user, "username");
        verifyEntityUpdateDuringFailover(user, backendFailover);

        // first+lastName
        user.setFirstName(user.getFirstName() + "_updated");
        user.setLastName(user.getLastName() + "_updated");
        user = updateEntityOnCurrentFailNode(user, "firstName/lastName");
        verifyEntityUpdateDuringFailover(user, backendFailover);

        return user;
    }

}
