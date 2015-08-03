package org.keycloak.testsuite.console.realm;

import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.console.page.realm.LoginSettings;
import org.keycloak.testsuite.page.auth.Login;
import org.keycloak.testsuite.page.auth.Registration;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author tkyjovsk
 */
public class LoginSettingsTest extends AbstractRealmTest {

    @Page
    private LoginSettings loginSettings;

    @Page
    private Login login;
    @Page
    private Registration registration;

    @Before
    public void beforeLoginSettingsTest() {
        tabs().login();
    }

    @Test
    public void testUserRegistration() {

        loginSettings.form().setRegistrationAllowed(true);
        loginSettings.form().save();
        assertTrue(loginSettings.form().isRegistrationAllowed());

        logoutFromTestRealm();
        login.waitForRegistrationPresent(true);
        login.registration();
        assertCurrentUrlStartsWith(registration);
        registration.waitForUsernameInputPresent(true);

        // test email as username
        loginAsTestAdmin();
        configure().realmSettings();
        tabs().login();
        loginSettings.form().setEmailAsUsername(true);
        loginSettings.form().save();

        logoutFromTestRealm();
        login.registration();
        registration.waitForUsernameInputPresent(false); // username input shouldn't be visible

        // test user reg. disabled
        loginAsTestAdmin();
        configure().realmSettings();
        tabs().login();
        loginSettings.form().setRegistrationAllowed(false);
        loginSettings.form().save();
        assertFalse(loginSettings.form().isRegistrationAllowed());
        logoutFromTestRealm();

        login.waitForRegistrationPresent(false);
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
