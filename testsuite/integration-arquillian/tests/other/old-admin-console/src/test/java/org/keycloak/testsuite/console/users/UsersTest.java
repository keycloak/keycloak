package org.keycloak.testsuite.console.users;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.Timer;

import javax.ws.rs.core.Response;

import static org.keycloak.testsuite.admin.ApiUtil.getCreatedId;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 *
 * @author tkyjovsk
 */
public class UsersTest extends AbstractUserTest {
    
    @Before
    public void beforeUserAttributesTest() {
        usersPage.navigateTo();
    }
    
    public void createTestUsers(String usernamePrefix, int count) {
//        Timer.DEFAULT.reset();
        for (int i = 0; i < count; i++) {
            String username = String.format("%s%03d", usernamePrefix, i);
            UserRepresentation u = createUserRepresentation(
                    username,
                    username + "@email.test",
                    "First",
                    "Last",
                    true);
            Timer.DEFAULT.reset();
            Response r = testRealmResource().users().create(u);
            String id = getCreatedId(r);
            r.close();
            Timer.DEFAULT.reset("create user");
        }
//        Timer.DEFAULT.reset("create " + count + " users");
    }
    
    @Test
    @Ignore
    public void usersPagination() {
        createTestUsers("test_user_", 100);
        
        usersPage.navigateTo();
        usersPage.table().viewAllUsers();
        pause(120000);
    }
    
}
