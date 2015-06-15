package org.keycloak.testsuite;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.NotFoundException;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.openqa.selenium.WebDriver;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.ui.fragment.MenuPage;
import org.keycloak.testsuite.ui.fragment.Navigation;
import org.keycloak.testsuite.ui.page.LoginPage;
import org.keycloak.testsuite.ui.page.account.PasswordPage;
import static org.keycloak.testsuite.ui.util.Constants.ADMIN_PSSWD;
import org.keycloak.testsuite.ui.util.URL;
import org.keycloak.util.JsonSerialization;

/**
 *
 * @author tkyjovsk
 */
@RunWith(Arquillian.class)
public abstract class AbstractKeycloakTest {

    public static final String KEYCLOAK_SERVER = "keycloak-managed";

    protected static boolean adminPasswordUpdated = Boolean.parseBoolean(System.getProperty("adminPasswordUpdated", "false"));

    @ArquillianResource
    protected ContainerController controller;

    @ArquillianResource
    protected Deployer deployer;

    protected Keycloak keycloak;

    @Drone
    protected WebDriver driver;

    @Page
    protected LoginPage loginPage;
    @Page
    protected PasswordPage passwordPage;
    @Page
    protected MenuPage menuPage;
    @Page
    protected Navigation navigation;

    public void loginAsAdmin() {
        driver.get(URL.ADMIN_CONSOLE_URL);
        loginPage.loginAsAdmin();
        if (!adminPasswordUpdated) {
            passwordPage.confirmNewPassword(ADMIN_PSSWD);
            passwordPage.submit();
            adminPasswordUpdated = true;
        }
    }

    public void logOut() {
        driver.get(URL.ADMIN_CONSOLE_URL);
        menuPage.logOut();
    }

    public static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load json.", e);
        }
    }

    public void importRealm(InputStream is) {
        RealmRepresentation newRealm = loadJson(is, RealmRepresentation.class);
        System.out.println("importing realm: " + newRealm.getRealm());

//        System.out.println("list of existing realms:");
//        for (RealmRepresentation r : keycloak.realms().findAll()) {
//            System.out.println("id: " + r.getId() + ", name: " + r.getRealm());
//        }

        try { // TODO - figure out a way how to do this without try-catch
            RealmResource rResource = keycloak.realms().realm(newRealm.getRealm());
            RealmRepresentation rRep = rResource.toRepresentation();
            System.out.println("removing existing realm: " + rRep.getRealm());
            rResource.remove();
        } catch (NotFoundException nfe) {
            System.out.println("realm " + newRealm.getRealm() + " not found");
        }
        System.out.println("importing realm");
        keycloak.realms().create(newRealm);
    }

    public void importRealm(String realmConfig) {
        importRealm(getClass().getResourceAsStream(realmConfig));
    }

    protected void driverSettings() {
        driver.manage().timeouts().setScriptTimeout(10, TimeUnit.SECONDS);
        driver.manage().window().maximize();
    }

    @Before
    public void setUp() {
        controller.start(KEYCLOAK_SERVER);
        driverSettings();
        loginAsAdmin();
        keycloak = Keycloak.getInstance(URL.AUTH_SERVER_URL,
                "master", "admin", "admin", Constants.ADMIN_CONSOLE_CLIENT_ID);
    }

    @After
    public void tearDown() {
        keycloak.close();
        logOut();
    }

}
