package org.keycloak.testsuite;

import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.Validate;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.keycloak.admin.client.Keycloak;
import org.openqa.selenium.WebDriver;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.TestRealms.importRealm;
import static org.keycloak.testsuite.TestRealms.removeRealm;
import org.keycloak.testsuite.arquillian.ContainersTestEnricher.AdminPasswordUpdateTracker;
import org.keycloak.testsuite.page.console.AdminConsole;
import org.keycloak.testsuite.page.console.AuthServer;
import org.keycloak.testsuite.page.console.AuthServerContextRoot;
import static org.keycloak.testsuite.util.PageAssert.*;
import org.keycloak.testsuite.page.console.fragment.MenuPage;
import org.keycloak.testsuite.page.console.fragment.Navigation;
import org.keycloak.testsuite.page.console.login.LoginPage;
import org.keycloak.testsuite.page.console.account.PasswordPage;
import static org.keycloak.testsuite.util.Constants.ADMIN_PSSWD;

/**
 *
 * @author tkyjovsk
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class AbstractKeycloakTest implements TestRealmsManager {

    protected Keycloak keycloak;

    protected TestRealms testRealms;

    @Drone
    protected WebDriver driver;

    @Page
    protected AuthServerContextRoot authServerContextRoot;
    @Page
    protected AuthServer authServer;
    @Page
    protected AdminConsole adminConsole;

    @Page
    protected LoginPage loginPage;
    @Page
    protected PasswordPage passwordPage;
    @Page
    protected MenuPage menuPage;
    @Page
    protected Navigation navigation;

    @Before
    public void setUp() {
        driverSettings();
        if (!isAdminPasswordUpdated()) {
            updateAdminPassword();
        }
        keycloak = Keycloak.getInstance(authServer.getUrlString(),
                "master", "admin", "admin", Constants.ADMIN_CONSOLE_CLIENT_ID);
    }

    @After
    public void tearDown() {
        keycloak.close();
        driver.manage().deleteAllCookies();
    }

    public boolean isAdminPasswordUpdated() {
        return AdminPasswordUpdateTracker.isAdminPasswordUpdated(this.getClass());
    }

    public void setAdminPasswordUpdated(boolean updated) {
        AdminPasswordUpdateTracker
                .setAdminPasswordUpdatedFor(this.getClass(), updated);
    }

    public void loginAsAdmin() {
        adminConsole.navigateTo();
        loginPage.loginAsAdmin();
    }

    public void updateAdminPassword() {
        loginAsAdmin();
        passwordPage.confirmNewPassword(ADMIN_PSSWD);
        passwordPage.submit();
        assertCurrentUrlStartsWith(adminConsole);
        logOut();
        setAdminPasswordUpdated(true);
    }

    public void logOut() {
        adminConsole.navigateTo();
        assertCurrentUrlStartsWith(adminConsole);
        menuPage.logOut();
    }

    protected void driverSettings() {
        driver.manage().timeouts().setScriptTimeout(10, TimeUnit.SECONDS);
        driver.manage().window().maximize();
    }

    public abstract TestRealms loadTestRealms();

    public RealmRepresentation getTestRealm(String testRealm) {
        if (testRealms == null) {
            System.out.println("Loading test realms.");
            testRealms = loadTestRealms();
            System.out.println("Loaded test realms: " + testRealms.keySet());
        }
        RealmRepresentation realm = testRealms.get(testRealm);
        Validate.notNull(realm, "Couldn't locate '" + testRealm + "' among the loaded test realms.");
        return realm;
    }

    @Override
    public void importTestRealm(String testRealm) {
        importRealm(keycloak, getTestRealm(testRealm));
    }

    @Override
    public void removeTestRealm(String testRealm) {
        removeRealm(keycloak, getTestRealm(testRealm));
    }

}
