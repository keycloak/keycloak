package org.keycloak.testsuite.console.realm;

import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import org.keycloak.testsuite.console.page.realm.LoginSettings;
import org.keycloak.testsuite.auth.page.login.Registration;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author tkyjovsk
 */
public class LoginSettingsTest extends AbstractRealmTest {

    @Page
    private LoginSettings loginSettings;

    @Page
    private Registration testRealmRegistration;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmRegistration.setAuthRealm(TEST);
    }
    
    @Before
    public void beforeLoginSettingsTest() {
        tabs().login();
    }

    @Test
    public void testUserRegistration() {

        loginSettings.form().setRegistrationAllowed(true);
        loginSettings.form().save();
        assertTrue(loginSettings.form().isRegistrationAllowed());

        testRealmAdminConsole.navigateTo();
        testRealmLogin.form().waitForRegistrationLinkPresent();
        testRealmLogin.form().register();
        assertCurrentUrlStartsWith(testRealmRegistration);
        testRealmRegistration.waitForUsernameInputPresent();

        // test email as username
        loginSettings.navigateTo();
        loginSettings.form().setEmailAsUsername(true);
        loginSettings.form().save();

        logoutFromTestRealmConsole();
        testRealmLogin.form().waitForRegistrationLinkPresent();
        testRealmLogin.form().register();
        assertCurrentUrlStartsWith(testRealmRegistration);
        testRealmRegistration.waitForUsernameInputNotPresent();

        // test user reg. disabled
        loginSettings.navigateTo();
        loginSettings.form().setRegistrationAllowed(false);
        loginSettings.form().save();
        assertFalse(loginSettings.form().isRegistrationAllowed());
        logoutFromTestRealmConsole();

        testRealmLogin.form().waitForRegistrationLinkNotPresent();
    }

    @Test
    @Ignore
    public void testEditUsername() {
    }

    @Test
    @Ignore
    public void testForgottenPassword() {
    }

    @Test
    @Ignore
    public void testRememberMe() {
    }

    @Test
    @Ignore
    public void testVerifyEmail() {
    }

    @Test
    @Ignore
    public void testRequireSSL() {
    }

}
