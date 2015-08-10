package org.keycloak.testsuite.account;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.auth.page.account.ChangePassword;
import static org.keycloak.testsuite.admin.Users.getPasswordCredentialValueOf;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author tkyjovsk
 */
public class ChangePasswordTest extends AbstractAccountManagementTest {

    private static final String NEW_PASSWORD = "newpassword";
    private static final String WRONG_PASSWORD = "wrongpassword";

    @Page
    private ChangePassword testRealmChangePassword;

    private String correctPassword;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmChangePassword.setAuthRealm(testRealm);
    }

    @Before
    public void beforeChangePasswordTest() {
        correctPassword = getPasswordCredentialValueOf(testRealmUser);
        testRealmAccountManagement.navigateTo();
        testRealmLogin.form().login(testRealmUser);
        testRealmAccountManagement.password();
    }

    @Test
    public void invalidChangeAttempts() {
        testRealmChangePassword.save();
        assertFlashMessageError();

        testRealmChangePassword.changePasswords(WRONG_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);
        assertFlashMessageError();

        testRealmChangePassword.changePasswords(correctPassword, NEW_PASSWORD, NEW_PASSWORD + "-mismatch");
        assertFlashMessageError();
    }

    @Test
    public void successfulChangeAttempts() {
        // change password successfully
        testRealmChangePassword.changePasswords(correctPassword, NEW_PASSWORD, NEW_PASSWORD);
        assertFlashMessageSuccess();

        // login using new password
        testRealmAccountManagement.signOut();
        testRealmLogin.form().login(testRealmUser.getUsername(), NEW_PASSWORD);
        assertCurrentUrlStartsWith(testRealmAccountManagement);

        // change password back
        testRealmAccountManagement.password();
        testRealmChangePassword.changePasswords(NEW_PASSWORD, correctPassword, correctPassword);
        assertFlashMessageSuccess();
    }

}
