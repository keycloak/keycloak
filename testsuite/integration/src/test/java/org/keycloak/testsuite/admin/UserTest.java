package org.keycloak.testsuite.admin;

import org.junit.Test;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.ClientErrorException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserTest extends AbstractClientTest {

    @Test
    public void createUser() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("user1");
        user.setEmail("user1@localhost");

        realm.users().create(user);
    }

    @Test
    public void createDuplicatedUser() {
        createUser();

        try {
            UserRepresentation user = new UserRepresentation();
            user.setUsername("user1");
            realm.users().create(user);
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(409, e.getResponse().getStatus());
        }
    }

}
