package org.keycloak.testsuite;

import java.io.IOException;
import java.io.InputStream;
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
import org.openqa.selenium.WebDriver;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.ui.fragment.MenuPage;
import org.keycloak.testsuite.ui.page.LoginPage;
import org.keycloak.testsuite.ui.page.account.PasswordPage;
import static org.keycloak.testsuite.ui.util.Constants.ADMIN_PSSWD;
import org.keycloak.testsuite.ui.util.URL;
import static org.keycloak.testsuite.ui.util.URL.ADMIN_CONSOLE_INDEX_URL;
import org.keycloak.util.JsonSerialization;

/**
 *
 * @author tkyjovsk
 */
@RunWith(Arquillian.class)
public abstract class AbstractKeycloakTest {

    private static final String KEYCLOAK_SERVER = System.getProperty("keycloak.server", "keycloak-managed");

    private static boolean adminPasswordUpdated = Boolean.parseBoolean(System.getProperty("adminPasswordUpdated", "false"));

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

    public void updateAdminPassword() {
        if (!adminPasswordUpdated) {
            driver.get(ADMIN_CONSOLE_INDEX_URL);
            loginPage.loginAsAdmin();
            passwordPage.confirmNewPassword(ADMIN_PSSWD);
            passwordPage.submit();
            menuPage.logOut();
            adminPasswordUpdated = true;
        }
    }

    public static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load json.", e);
        }
    }

    public void importRealm(String realmConfig) {
        System.out.println("importing realm: " + realmConfig);
        RealmRepresentation realm = loadJson(
                getClass().getResourceAsStream(realmConfig),
                RealmRepresentation.class);
        keycloak.realms().create(realm);
    }

    @Before
    public void startKeycloakServer() {
        controller.start(KEYCLOAK_SERVER);
        updateAdminPassword();
        keycloak = Keycloak.getInstance(URL.ADMIN_CONSOLE_BASE_URL,
                "master", "admin", "admin", Constants.ADMIN_CONSOLE_CLIENT_ID);
    }

    @After
    public void after() {
        keycloak.close();
    }

}
