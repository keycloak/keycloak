package org.keycloak.testsuite.user;

import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.Assert.assertEquals;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;
import static org.keycloak.testsuite.admin.ApiUtil.getCreatedId;

/**
 *
 * @author tkyjovsk
 */
public class AbstractUserTest extends AbstractAuthTest {

    protected UsersResource users() {
        return testRealmResource().users();
    }

    public static UserRepresentation createUserRep(String username) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(username + "@email.test");
        return user;
    }

    public String createUser(UserRepresentation user) {
        return createUser(users(), user);
    }

    public String createUser(UsersResource users, UserRepresentation user) {
        Response response = users.create(user);
        assertEquals(CREATED.getStatusCode(), response.getStatus());
        String createdId = getCreatedId(response);
        response.close();
        return createdId;
    }

}
