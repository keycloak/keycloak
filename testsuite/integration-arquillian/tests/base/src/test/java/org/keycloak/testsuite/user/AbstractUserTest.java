package org.keycloak.testsuite.user;

import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.CREATED;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import static org.keycloak.testsuite.admin.ApiUtil.getCreatedId;
import static org.junit.Assert.assertEquals;
import org.keycloak.testsuite.AbstractAuthTest;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractUserTest extends AbstractAuthTest {

    protected UsersResource users() {
        return testRealmResource().users();
    }

    protected UserResource user(UserRepresentation user) {
        if (user.getId()==null) {
            throw new IllegalStateException("User id cannot be null.");
        }
        return user(user.getId());
    }

    protected UserResource user(String id) {
        return users().get(id);
    }

    public static UserRepresentation createUserRep(String username) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(username + "@email.test");
        return user;
    }

    public UserRepresentation createUser(UserRepresentation user) {
        return createUser(users(), user);
    }

    public UserRepresentation createUser(UsersResource users, UserRepresentation user) {
        Response response = users.create(user);
        assertEquals(CREATED.getStatusCode(), response.getStatus());
        user.setId(getCreatedId(response));
        response.close();
        return user;
    }

}
