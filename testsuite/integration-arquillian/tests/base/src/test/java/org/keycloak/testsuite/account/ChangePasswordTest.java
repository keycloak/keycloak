package org.keycloak.testsuite.account;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import org.keycloak.testsuite.auth.page.account.ChangePassword;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrl;
import static org.keycloak.testsuite.admin.Users.getPasswordCredentialValueOf;

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
        testRealmChangePassword.setAuthRealm(TEST);
    }

    @Before
    public void beforeChangePasswordTest() {
        // create user via admin api
        createUserWithAdminClient(testRealmAccountManagement.realmResource(), testRealmUser);

        correctPassword = getPasswordCredentialValueOf(testRealmUser);

        testRealmAccountManagement.password();
    }

    @Test
    public void passwordPageValidationTest() {

        testRealmChangePassword.save();
        assertFlashMessageError();

        testRealmChangePassword.form().setPasswords(WRONG_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);
        testRealmChangePassword.save();
        assertFlashMessageError();

        testRealmChangePassword.form().setPasswords(correctPassword, NEW_PASSWORD, NEW_PASSWORD + "-mismatch");
        testRealmChangePassword.save();
        assertFlashMessageError();
    }

    @Test
    public void changePasswordTest() {
        testRealmChangePassword.form().setPasswords(correctPassword, NEW_PASSWORD, NEW_PASSWORD);
        testRealmChangePassword.save();
        assertFlashMessageSuccess();

        // login using new password
        testRealmAccountManagement.signOut();
        testRealmLogin.form().login(testRealmUser.getUsername(), NEW_PASSWORD);
        assertCurrentUrl(testRealmAccountManagement);

        // change password back
        testRealmAccountManagement.password();
        testRealmChangePassword.form().setPasswords(NEW_PASSWORD, correctPassword, correctPassword);
        testRealmChangePassword.save();
        assertFlashMessageSuccess();
    }

}
