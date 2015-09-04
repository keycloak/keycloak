package org.keycloak.testsuite.console.realm;

import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
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
    private LoginSettings loginSettingsPage;

    @Page
    private Registration testRealmRegistrationPage;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmRegistrationPage.setAuthRealm(TEST);
    }
    
    @Before
    public void beforeLoginSettingsTest() {
        tabs().login();
    }

    @Test
    public void userRegistration() {

        loginSettingsPage.form().setRegistrationAllowed(true);
        loginSettingsPage.form().save();
        assertTrue(loginSettingsPage.form().isRegistrationAllowed());

        
        testRealmAdminConsolePage.navigateTo();
        testRealmLoginPage.form().waitForRegisterLinkPresent();
        testRealmLoginPage.form().register();
        assertCurrentUrlStartsWith(testRealmRegistrationPage);
        testRealmRegistrationPage.waitForUsernameInputPresent();

        // test email as username
        loginSettingsPage.navigateTo();
        loginSettingsPage.form().setEmailAsUsername(true);
        loginSettingsPage.form().save();

        testRealmAdminConsolePage.navigateTo();
        testRealmLoginPage.form().waitForRegisterLinkPresent();
        testRealmLoginPage.form().register();
        assertCurrentUrlStartsWith(testRealmRegistrationPage);
        testRealmRegistrationPage.waitForUsernameInputNotPresent();

        // test user reg. disabled
        loginSettingsPage.navigateTo();
        loginSettingsPage.form().setRegistrationAllowed(false);
        loginSettingsPage.form().save();
        assertFalse(loginSettingsPage.form().isRegistrationAllowed());
        
        testRealmAdminConsolePage.navigateTo();
        testRealmLoginPage.form().waitForRegistrationLinkNotPresent();
    }

}
