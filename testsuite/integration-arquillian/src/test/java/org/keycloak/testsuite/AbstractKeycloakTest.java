package org.keycloak.testsuite;

import org.keycloak.testsuite.arquillian.AuthServerContainer;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.NotFoundException;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;
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
import org.keycloak.util.JsonSerialization;

/**
 *
 * @author tkyjovsk
 */
@RunAsClient
@AuthServerContainer("auth-server-wildfly")
public abstract class AbstractKeycloakTest extends KeycloakContainersManager {

    protected static boolean adminPasswordUpdated = Boolean.parseBoolean(System.getProperty("adminPasswordUpdated", "false"));

    public static final String AUTH_SERVER_BASE_URL = "http://localhost:" + Integer.parseInt(
            System.getProperty("keycloak.http.port", "8080"));

    public static final String AUTH_SERVER_URL = AUTH_SERVER_BASE_URL + "/auth";
    public static final String ADMIN_CONSOLE_URL = AUTH_SERVER_URL + "/admin/master/console/index.html";

    public static final String SETTINGS_GENERAL_SETTINGS = ADMIN_CONSOLE_URL + "#/realms/%s";
    public static final String SETTINGS_ROLES = ADMIN_CONSOLE_URL + "#/realms/%s/roles";
    public static final String SETTINGS_LOGIN = ADMIN_CONSOLE_URL + "#/realms/%s/login-settings";
    public static final String SETTINGS_SOCIAL = ADMIN_CONSOLE_URL + "#/realms/%s/social-settings";

    public static final String REALM_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";

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
        driver.get(ADMIN_CONSOLE_URL);
        loginPage.loginAsAdmin();
        if (!adminPasswordUpdated) {
            passwordPage.confirmNewPassword(ADMIN_PSSWD);
            passwordPage.submit();
            adminPasswordUpdated = true;
        }
    }

    public void logOut() {
        driver.get(ADMIN_CONSOLE_URL);
        menuPage.logOut();
    }

    public static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load json.", e);
        }
    }

    public void importRealm(String realmConfig) {
        System.out.println("importing realm from config: " + realmConfig);
        importRealm(getClass().getResourceAsStream(realmConfig));
    }

    public void importRealm(InputStream is) {
        importRealm(loadJson(is, RealmRepresentation.class));
    }

    public void importRealm(RealmRepresentation realm) {
        System.out.println("importing realm: " + realm.getRealm());

//        System.out.println("list of existing realms:");
//        for (RealmRepresentation r : keycloak.realms().findAll()) {
//            System.out.println("id: " + r.getId() + ", name: " + r.getRealm());
//        }
        try { // TODO - figure out a way how to do this without try-catch
            RealmResource realmResource = keycloak.realms().realm(realm.getRealm());
            RealmRepresentation rRep = realmResource.toRepresentation();
            System.out.println("removing existing realm: " + rRep.getRealm());
            realmResource.remove();
        } catch (NotFoundException nfe) {
            System.out.println("realm " + realm.getRealm() + " not found");
        }
        System.out.println("importing realm");
        keycloak.realms().create(realm);
    }
    
    protected void driverSettings() {
        driver.manage().timeouts().setScriptTimeout(10, TimeUnit.SECONDS);
        driver.manage().window().maximize();
    }

    @Before
    public void setUp() {
        driverSettings();
        loginAsAdmin();
        keycloak = Keycloak.getInstance(AUTH_SERVER_URL,
                "master", "admin", "admin", Constants.ADMIN_CONSOLE_CLIENT_ID);
    }

    @After
    public void tearDown() {
        keycloak.close();
        logOut();
    }

}
