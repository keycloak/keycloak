package org.keycloak.testsuite.ui.test.user;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.InSequence;
import org.junit.Test;
import org.keycloak.testsuite.ui.fragment.FlashMessage;
import org.keycloak.testsuite.ui.model.User;
import org.keycloak.testsuite.ui.page.RegisterPage;
import org.keycloak.testsuite.ui.page.settings.UserPage;

import static org.junit.Assert.*;
import org.keycloak.testsuite.ui.AbstractKeyCloakTest;
import static org.keycloak.testsuite.ui.util.URL.BASE_URL;
import static org.keycloak.testsuite.ui.util.Users.*;

/**
 * Created by fkiss.
 */

public class RegisterNewUserTest extends AbstractKeyCloakTest<RegisterPage> {

    @Page
    private UserPage userPage;

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;

    @Test
    @InSequence(0)
    public void registerNewUserTest() {
        driver.get(BASE_URL);
        loginPage.goToUserRegistration();
        page.registerNewUser(TEST_USER1);
        //assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        loginAsAdmin();
        navigation.users();
        userPage.deleteUser(TEST_USER1.getUserName());
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        logOut();
    }


    @Test
    @InSequence(1)
    public void registerNewUserWithWrongEmail() {
        String invalidEmail = "newUser.redhat.com";
        User testUser = TEST_USER1;
		testUser.setEmail(invalidEmail);
        loginPage.goToUserRegistration();
        page.registerNewUser(testUser);
        assertTrue(page.isInvalidEmail());
        loginAsAdmin();
        navigation.users();
        assertNull(userPage.findUser(testUser.getUserName()));
        logOut();
    }

    @Test
    @InSequence(2)
    public void registerNewUserWithWrongAttributes() {
		User testUser = new User();
		
        loginPage.goToUserRegistration();
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
        assertFalse(page.isAttributeSpecified("password."));
		testUser.setPassword("password");
        page.registerNewUser(testUser);
        loginAsAdmin();
        navigation.users();
        userPage.deleteUser(TEST_USER1.getUserName());
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        logOut();
    }

    @Test
    @InSequence(3)
    public void registerNewUserWithNotMatchingPasswords() {
        loginPage.goToUserRegistration();
        page.registerNewUser(TEST_USER1);
        assertFalse(page.isPasswordSame());
        page.registerNewUser(TEST_USER1);
        loginAsAdmin();
        navigation.users();
        userPage.deleteUser(TEST_USER1.getUserName());
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        logOut();
    }

}
