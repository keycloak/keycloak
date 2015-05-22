package org.keycloak.testsuite.ui.test.user;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Test;
import org.keycloak.testsuite.ui.fragment.FlashMessage;
import org.keycloak.testsuite.ui.model.User;
import org.keycloak.testsuite.ui.page.RegisterPage;
import org.keycloak.testsuite.ui.page.settings.UserPage;

import static org.junit.Assert.*;
import org.junit.Before;
import org.keycloak.testsuite.ui.AbstractKeyCloakTest;
import org.keycloak.testsuite.ui.page.settings.LoginSettingsPage;
import static org.keycloak.testsuite.ui.util.Users.*;

/**
 * Created by fkiss.
 */

public class RegisterNewUserTest extends AbstractKeyCloakTest<RegisterPage> {

    @Page
    private UserPage userPage;

	@Page
	private LoginSettingsPage loginSettingsPage;
	
    @FindByJQuery(".alert")
    private FlashMessage flashMessage;
	
	@Before
	public void beforeUserRegistration() {
		navigation.settings();
		navigation.login();
		loginSettingsPage.enableUserRegistration();
		logOut();
		loginPage.goToUserRegistration();
	}
	
	@After
	public void afterUserRegistration() {
		navigation.settings();
		navigation.login();
		loginSettingsPage.disableUserRegistration();
	}

    @Test
    public void registerNewUserTest() {
        page.registerNewUser(TEST_USER1);
        //assertTrue(flashMessage.getText(), flashMessage.isSuccess());
		logOut();
        loginAsAdmin();
        navigation.users();
        userPage.deleteUser(TEST_USER1.getUserName());
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
    }


    @Test
    public void registerNewUserWithWrongEmail() {
        User testUser = TEST_USER1;
		testUser.setEmail("newUser.redhat.com");
        page.registerNewUser(testUser);
        assertTrue(page.isInvalidEmail());
		page.backToLoginPage();
        loginAsAdmin();
        navigation.users();
        assertNull(userPage.findUser(testUser.getUserName()));
    }

    @Test
    public void registerNewUserWithWrongAttributes() {
		User testUser = new User();
		
        page.registerNewUser(testUser);
        assertFalse(page.isAttributeSpecified("first name"));
		testUser.setFirstName("name");
        page.registerNewUser(testUser);
        assertFalse(page.isAttributeSpecified("last name"));
		testUser.setLastName("surname");
        page.registerNewUser(testUser);
        assertFalse(page.isAttributeSpecified("email"));
		testUser.setEmail("mail@redhat.com");
        page.registerNewUser(testUser);
        assertFalse(page.isAttributeSpecified("username"));
		testUser.setUserName("user");
        page.registerNewUser(testUser);
        assertFalse(page.isAttributeSpecified("password"));
		testUser.setPassword("password");
        page.registerNewUser(testUser);
		logOut();
		loginAsAdmin();
        navigation.users();
        userPage.deleteUser(TEST_USER1.getUserName());
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
    }

    @Test
    public void registerNewUserWithNotMatchingPasswords() {
        page.registerNewUser(TEST_USER1, "psswd");
        assertFalse(page.isPasswordSame());
        page.registerNewUser(TEST_USER1);
		logOut();
        loginAsAdmin();
        navigation.users();
        userPage.deleteUser(TEST_USER1.getUserName());
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
    }

}
