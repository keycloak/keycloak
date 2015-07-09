package org.keycloak.testsuite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.TestRealms.importRealm;
import static org.keycloak.testsuite.TestRealms.removeRealm;
import org.openqa.selenium.WebDriver;
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
public abstract class AbstractKeycloakTest {

    protected Keycloak keycloak;

    protected List<RealmRepresentation> testRealms;

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
    public void beforeAbstractKeycloakTest() {
        driverSettings();

        if (!isAdminPasswordUpdated()) {
            updateAdminPassword();
        }

        keycloak = Keycloak.getInstance(authServer.getUrlString(),
                "master", "admin", "admin", Constants.ADMIN_CONSOLE_CLIENT_ID);

        importTestRealms();
    }

    @After
    public void afterAbstractKeycloakTest() {
        removeTestRealms();
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

    public abstract void loadTestRealmsInto(List<RealmRepresentation> testRealms);

    private void loadTestRealms() {
        if (testRealms == null) {
            testRealms = new ArrayList<>();
        }
        if (testRealms.isEmpty()) {
            loadTestRealmsInto(testRealms);
        }
    }
    
    public void importTestRealms() {
        loadTestRealms();
        for (RealmRepresentation testRealm : testRealms) {
            importRealm(keycloak, testRealm);
        }
    }

    public void removeTestRealms() {
        for (RealmRepresentation testRealm : testRealms) {
            removeRealm(keycloak, testRealm);
        }
    }

}
