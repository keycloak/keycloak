package org.keycloak.testsuite.console.users;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.users.CreateUser;
import org.keycloak.testsuite.console.page.users.Users;

import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractUserTest extends AbstractConsoleTest {

    @Page
    protected Users usersPage;
    @Page
    protected CreateUser createUserPage;

    protected UserRepresentation newTestRealmUser;
    
    @Before
    public void beforeUserTest() {
        newTestRealmUser = new UserRepresentation();
//        manage().users();
    }

    public void createUser(UserRepresentation user) {
        assertCurrentUrlEquals(usersPage);
        usersPage.table().addUser();
        assertCurrentUrlStartsWith(createUserPage);
        createUserPage.form().setValues(user);
        createUserPage.form().save();
    }
    
    public UsersResource usersResource() {
        return testRealmResource().users();
    }
    
    public UserResource userResource(String id) {
        return usersResource().get(id);
    }
    
}
