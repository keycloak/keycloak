package org.keycloak.testsuite;

import org.keycloak.testsuite.arquillian.TestContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.Constants;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.arquillian.SuiteContext;
import static org.keycloak.testsuite.util.RealmUtils.importRealm;
import static org.keycloak.testsuite.util.RealmUtils.removeRealm;
import org.openqa.selenium.WebDriver;
import org.keycloak.testsuite.auth.page.AuthServer;
import org.keycloak.testsuite.auth.page.AuthServerContextRoot;
import static org.keycloak.testsuite.util.PageAssert.*;
import org.keycloak.testsuite.auth.page.AuthRealm;
import static org.keycloak.testsuite.auth.page.AuthRealm.ADMIN;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;
import org.keycloak.testsuite.auth.page.account.Account;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.auth.page.login.UpdatePassword;
import static org.keycloak.testsuite.util.SeleniumUtils.pause;

/**
 *
 * @author tkyjovsk
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class AbstractKeycloakTest {

    @ArquillianResource
    protected SuiteContext suiteContext;
    
    @ArquillianResource
    protected TestContext testContext;

    protected Keycloak adminClient;

    protected List<RealmRepresentation> testRealmReps;

    @Drone
    protected WebDriver driver;

    @Page
    protected AuthServerContextRoot authServerContextRoot;
    @Page
    protected AuthServer authServer;

    @Page
    protected AuthRealm masterRealm;

    @Page
    protected Account account;
    @Page
    protected OIDCLogin login;
    @Page
    protected UpdatePassword updatePassword;

    protected UserRepresentation adminUser;

    @Before
    public void beforeAbstractKeycloakTest() {
        adminUser = createAdminUserRepresentation();

        setDefaultPageUriParameters();

        driverSettings();

        if (!suiteContext.isAdminPasswordUpdated()) {
            updateMasterAdminPassword();
            suiteContext.setAdminPasswordUpdated(true);
        }

        adminClient = testContext.getAdminClient();
        if (adminClient == null) {
            adminClient = Keycloak.getInstance(authServer.toString(),
                    MASTER, ADMIN, ADMIN, Constants.ADMIN_CONSOLE_CLIENT_ID);
            testContext.setAdminClient(adminClient);
        }

        pause(2000);

        importTestRealms();
    }

    @After
    public void afterAbstractKeycloakTest() {
//        removeTestRealms(); // keeping test realms after test to be able to inspect failures, instead deleting existing realms before import
//        keycloak.close(); // keeping admin connection open
    }

    private void updateMasterAdminPassword() {
        account.navigateTo();
        login.form().login(ADMIN, ADMIN);
        updatePassword.updatePassword(ADMIN);
        assertCurrentUrlStartsWith(account);
        deleteAllCookiesForMasterRealm();
    }

    public void deleteAllCookiesForMasterRealm() {
        masterRealm.navigateTo();
        driver.manage().deleteAllCookies();
    }

    protected void driverSettings() {
        driver.manage().timeouts().setScriptTimeout(3, TimeUnit.SECONDS);
        driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
        driver.manage().timeouts().setScriptTimeout(3, TimeUnit.SECONDS);
        driver.manage().window().maximize();
    }

    public void setDefaultPageUriParameters() {
        masterRealm.setAuthRealm(MASTER);
        login.setAuthRealm(MASTER);
    }

    public abstract void addTestRealms(List<RealmRepresentation> testRealms);

    private void addTestRealms() {
        System.out.println("loading test realms");
        if (testRealmReps == null) {
            testRealmReps = new ArrayList<>();
        }
        if (testRealmReps.isEmpty()) {
            addTestRealms(testRealmReps);
        }
    }

    public void importTestRealms() {
        addTestRealms();
        System.out.println("importing test realms");
        for (RealmRepresentation testRealm : testRealmReps) {
            importRealm(adminClient, testRealm);
        }
    }

    public void removeTestRealms() {
        System.out.println("removing test realms");
        for (RealmRepresentation testRealm : testRealmReps) {
            removeRealm(adminClient, testRealm);
        }
    }

    private UserRepresentation createAdminUserRepresentation() {
        UserRepresentation adminUserRep = new UserRepresentation();
        adminUserRep.setUsername(ADMIN);
        adminUserRep.credential(PASSWORD, ADMIN);
        return adminUserRep;
    }

}
