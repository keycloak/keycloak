package org.keycloak.testsuite.console.realm;

import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import org.keycloak.testsuite.console.page.realm.LoginSettings;
import org.keycloak.testsuite.auth.page.login.Registration;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

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
//        tabs().login();
        loginSettingsPage.navigateTo();
        assertCurrentUrlEquals(loginSettingsPage);
    }

    @Test
    public void userRegistration() {

        log.info("enabling registration");
        loginSettingsPage.form().setRegistrationAllowed(true);
        loginSettingsPage.form().save();
        log.debug("enabled");
        
        testRealmAdminConsolePage.navigateTo();
        testRealmLoginPage.form().waitForRegisterLinkPresent();
        testRealmLoginPage.form().register();
        assertCurrentUrlStartsWith(testRealmRegistrationPage);
        testRealmRegistrationPage.waitForUsernameInputPresent();
        log.info("verified registration is enabled");

        // test email as username
        log.info("enabling email as username");
        loginSettingsPage.navigateTo();
        loginSettingsPage.form().setEmailAsUsername(true);
        loginSettingsPage.form().save();
        log.debug("enabled");

        testRealmAdminConsolePage.navigateTo();
        testRealmLoginPage.form().waitForRegisterLinkPresent();
        testRealmLoginPage.form().register();
        assertCurrentUrlStartsWith(testRealmRegistrationPage);
        testRealmRegistrationPage.waitForUsernameInputNotPresent();
        log.info("verified email as username");

        // test user reg. disabled
        log.info("disabling registration");
        loginSettingsPage.navigateTo();
        loginSettingsPage.form().setRegistrationAllowed(false);
        loginSettingsPage.form().save();
        assertFalse(loginSettingsPage.form().isRegistrationAllowed());
        log.debug("disabled");
        
        testRealmAdminConsolePage.navigateTo();
        testRealmLoginPage.form().waitForRegisterLinkNotPresent();
        log.info("verified regisration is disabled");
    }

}
