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
import static org.keycloak.testsuite.util.RealmUtils.importRealm;
import static org.keycloak.testsuite.util.RealmUtils.removeRealm;
import org.openqa.selenium.WebDriver;
import org.keycloak.testsuite.arquillian.ContainersTestEnricher.AdminPasswordUpdateTracker;
import org.keycloak.testsuite.console.page.AdminConsole;
import org.keycloak.testsuite.console.page.AuthServer;
import org.keycloak.testsuite.console.page.AuthServerContextRoot;
import static org.keycloak.testsuite.console.page.Realm.MASTER;
import static org.keycloak.testsuite.util.PageAssert.*;
import org.keycloak.testsuite.console.page.fragment.MenuPage;
import org.keycloak.testsuite.console.page.fragment.Navigation;
import org.keycloak.testsuite.console.page.login.LoginPage;
import org.keycloak.testsuite.console.page.account.PasswordPage;
import static org.keycloak.testsuite.util.Constants.ADMIN_PSSWD;

/**
 *
 * @author tkyjovsk
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class AbstractKeycloakTest {

    protected Keycloak keycloak;
    private boolean keycloakOpen = false;

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
        setPageUriTemplateValues();

        driverSettings();

        if (!isAdminPasswordUpdated()) {
            updateAdminPassword();
        }

        if (!keycloakOpen) {
            keycloak = Keycloak.getInstance(authServer.toString(),
                    "master", "admin", "admin", Constants.ADMIN_CONSOLE_CLIENT_ID);
            keycloakOpen = true;
        }

        importTestRealms();
    }

    @After
    public void afterAbstractKeycloakTest() {
//        removeTestRealms();
//        keycloak.close();
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
        if (isAdminPasswordUpdated()) {
            assertCurrentUrlStartsWith(adminConsole);
        }
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
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        driver.manage().timeouts().setScriptTimeout(10, TimeUnit.SECONDS);
        driver.manage().window().maximize();
    }

    public void setPageUriTemplateValues() {
        adminConsole.setAdminRealm(MASTER);
    }

    public abstract void loadTestRealmsInto(List<RealmRepresentation> testRealms);

    private void loadTestRealms() {
        System.out.println("loading test realms");
        if (testRealms == null) {
            testRealms = new ArrayList<>();
        }
        if (testRealms.isEmpty()) {
            loadTestRealmsInto(testRealms);
        }
    }

    public void importTestRealms() {
        loadTestRealms();
        System.out.println("importing test realms");
        for (RealmRepresentation testRealm : testRealms) {
            importRealm(keycloak, testRealm);
        }
    }

    public void removeTestRealms() {
        System.out.println("removing test realms");
        for (RealmRepresentation testRealm : testRealms) {
            removeRealm(keycloak, testRealm);
        }
    }

}
