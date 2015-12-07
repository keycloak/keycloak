package org.keycloak.testsuite.account;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.auth.page.account.ChangePassword;
import static org.keycloak.testsuite.admin.Users.getPasswordOf;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author tkyjovsk
 */
public class ChangePasswordTest extends AbstractAccountManagementTest {

    private static final String NEW_PASSWORD = "newpassword";
    private static final String WRONG_PASSWORD = "wrongpassword";

    @Page
    private ChangePassword testRealmChangePasswordPage;

    private String correctPassword;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmChangePasswordPage.setAuthRealm(testRealmPage);
    }

    @Before
    public void beforeChangePasswordTest() {
        correctPassword = getPasswordOf(testUser);
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        testRealmAccountManagementPage.password();
    }

    @Test
    public void invalidChangeAttempts() {
        testRealmChangePasswordPage.save();
        assertAlertError();

        testRealmChangePasswordPage.changePasswords(WRONG_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);
        assertAlertError();

        testRealmChangePasswordPage.changePasswords(correctPassword, NEW_PASSWORD, NEW_PASSWORD + "-mismatch");
        assertAlertError();
    }

    @Test
    public void successfulChangeAttempts() {
        // change password successfully
        testRealmChangePasswordPage.changePasswords(correctPassword, NEW_PASSWORD, NEW_PASSWORD);
        assertAlertSuccess();

        // login using new password
        testRealmAccountManagementPage.signOut();
        testRealmLoginPage.form().login(testUser.getUsername(), NEW_PASSWORD);
        assertCurrentUrlStartsWith(testRealmAccountManagementPage);

        // change password back
        testRealmAccountManagementPage.password();
        testRealmChangePasswordPage.changePasswords(NEW_PASSWORD, correctPassword, correctPassword);
        assertAlertSuccess();
    }

}
