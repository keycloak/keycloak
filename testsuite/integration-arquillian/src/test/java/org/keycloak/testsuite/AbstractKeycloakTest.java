package org.keycloak.testsuite;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.NotFoundException;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.openqa.selenium.WebDriver;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.ContainersManager;
import org.keycloak.testsuite.arquillian.ContainersManager.AdminPasswordUpdateTracker;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainer;
import org.keycloak.testsuite.page.console.AdminConsole;
import org.keycloak.testsuite.page.console.AuthServer;
import org.keycloak.testsuite.page.console.AuthServerContextRoot;
import static org.keycloak.testsuite.util.PageAssert.*;
import org.keycloak.testsuite.page.console.fragment.MenuPage;
import org.keycloak.testsuite.page.console.fragment.Navigation;
import org.keycloak.testsuite.page.console.login.LoginPage;
import org.keycloak.testsuite.page.console.account.PasswordPage;
import static org.keycloak.testsuite.util.Constants.ADMIN_PSSWD;
import static org.keycloak.testsuite.util.Json.loadJson;

/**
 *
 * @author tkyjovsk
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class AbstractKeycloakTest {

    protected Keycloak keycloak;

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
        importTestRealm();
    }

    @After
    public void tearDown() {
        removeTestRealm();
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

    public boolean isRelative() {
        return ContainersManager.isRelative(this.getClass());
    }

    public void importTestRealm() {
        // override in test class if needed
    }

    public void removeTestRealm() {
        // override in test class if needed
    }

    public RealmRepresentation loadRealm(String realmConfig) {
        System.out.println("Loading realm from " + realmConfig);
        return loadRealm(getClass().getResourceAsStream(realmConfig));
    }

    public RealmRepresentation loadRealm(InputStream is) {
        RealmRepresentation realm = loadJson(is, RealmRepresentation.class);
        System.out.println("Loaded realm " + realm.getId());
        return realm;
    }

    public void importRealm(RealmRepresentation realm) {
        System.out.println("importing realm: " + realm.getRealm());
        try { // TODO - figure out a way how to do this without try-catch
            RealmResource realmResource = keycloak.realms().realm(realm.getRealm());
            RealmRepresentation rRep = realmResource.toRepresentation();
            System.out.println(" realm already exists on server, removing");
            realmResource.remove();
        } catch (NotFoundException nfe) {
            System.out.println(" realm not found on server");
        }
        keycloak.realms().create(realm);
        System.out.println("realm imported");
    }

}
