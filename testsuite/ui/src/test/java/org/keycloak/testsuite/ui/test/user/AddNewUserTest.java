package org.keycloak.testsuite.ui.test.user;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.junit.Test;
import org.keycloak.testsuite.ui.fragment.FlashMessage;
import org.keycloak.testsuite.ui.model.User;
import org.keycloak.testsuite.ui.page.settings.UserPage;


import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.keycloak.testsuite.ui.AbstractKeyCloakTest;
import static org.keycloak.testsuite.ui.util.Users.TEST_USER1;

/**
 * Created by fkiss.
 */

public class AddNewUserTest extends AbstractKeyCloakTest<UserPage> {

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;

	@Before
	public void beforeAddNewUserTest() {
		navigation.users();
	}
	
    @Test
    public void addUserWithInvalidEmailTest() {
        String testUsername = "testUserInvEmail";
        String invalidEmail = "user.redhat.com";
        User testUser = new User(testUsername, "pass", invalidEmail);
        page.addUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
        navigation.users();
        assertNull(page.findUser(testUsername));
    }

    @Test
    public void addUserWithNoUsernameTest() {
        User testUser = new User();
        page.addUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
    }

	@Ignore
	@Test
    public void addUserWithLongNameTest() {
        String longUserName = "thisisthelongestnameeveranditcannotbeusedwhencreatingnewuserinkeycloak";
        User testUser = new User(longUserName);
        navigation.users();
        page.addUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
        assertNull(page.findUser(testUser.getUserName()));
    }

    @Test
    public void addDuplicatedUser() {
        String testUsername = "test_duplicated_user";
        User testUser = new User(testUsername);
        page.addUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
		navigation.users();
        assertNotNull(page.findUser(testUsername));

        User testUser2 = new User(testUsername);
        page.addUser(testUser2);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
		navigation.users();
        page.deleteUser(testUsername);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(page.findUser(testUser2.getUserName()));
    }

    @Test
    public void addDisabledUser() {
        page.addUser(TEST_USER1);
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
		navigation.users();
        page.deleteUser(TEST_USER1.getUserName());
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(page.findUser(TEST_USER1.getUserName()));
    }

    



}
