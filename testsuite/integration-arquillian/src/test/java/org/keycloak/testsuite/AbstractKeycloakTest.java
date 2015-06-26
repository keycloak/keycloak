package org.keycloak.testsuite;

import org.keycloak.testsuite.arquillian.annotation.AuthServerContainer;
import java.io.IOException;
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
import org.keycloak.testsuite.console.page.AdminConsole;
import org.keycloak.testsuite.console.page.AuthServer;
import org.keycloak.testsuite.console.page.AuthServerContextRoot;
import static org.keycloak.testsuite.console.page.PageAssert.*;
import org.keycloak.testsuite.ui.fragment.MenuPage;
import org.keycloak.testsuite.ui.fragment.Navigation;
import org.keycloak.testsuite.ui.page.LoginPage;
import org.keycloak.testsuite.ui.page.account.PasswordPage;
import static org.keycloak.testsuite.ui.util.Constants.ADMIN_PSSWD;
import org.keycloak.util.JsonSerialization;

/**
 *
 * @author tkyjovsk
 */
@RunWith(Arquillian.class)
@RunAsClient
@AuthServerContainer("auth-server-wildfly")
public abstract class AbstractKeycloakTest {

    public static final String REALM_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";
    protected static boolean adminPasswordUpdated = Boolean.parseBoolean(System.getProperty("adminPasswordUpdated", "false"));

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
        updateAdminPassword();
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

    public void loginAsAdmin() {
        adminConsole.navigateTo();
        assertCurrentUrlDoesntStartWith(adminConsole);
        loginPage.loginAsAdmin();
    }

    public void updateAdminPassword() {
        if (!adminPasswordUpdated) {
            loginAsAdmin();
            passwordPage.confirmNewPassword(ADMIN_PSSWD);
            passwordPage.submit();
            assertCurrentUrlStartsWith(adminConsole);
            logOut();
            adminPasswordUpdated = true;
        }
    }

    public void logOut() {
        adminConsole.navigateTo();
        assertCurrentUrlStartsWith(adminConsole);
        menuPage.logOut();
    }

    public static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load json.", e);
        }
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
