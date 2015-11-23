package org.keycloak.testsuite;

import org.keycloak.testsuite.arquillian.TestContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.NotFoundException;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.util.OAuthClient;
import org.openqa.selenium.WebDriver;
import org.keycloak.testsuite.auth.page.AuthServer;
import org.keycloak.testsuite.auth.page.AuthServerContextRoot;
import static org.keycloak.testsuite.util.URLAssert.*;
import org.keycloak.testsuite.auth.page.AuthRealm;
import static org.keycloak.testsuite.auth.page.AuthRealm.ADMIN;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;
import org.keycloak.testsuite.auth.page.account.Account;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.auth.page.login.UpdatePassword;
import org.keycloak.testsuite.util.Timer;
import org.keycloak.testsuite.util.WaitUtils;

/**
 *
 * @author tkyjovsk
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class AbstractKeycloakTest {

    protected Logger log = Logger.getLogger(this.getClass());

    @ArquillianResource
    protected SuiteContext suiteContext;

    @ArquillianResource
    protected TestContext testContext;
    
    @ArquillianResource
    protected Keycloak adminClient;

    @ArquillianResource
    protected OAuthClient oauthClient;

    protected List<RealmRepresentation> testRealmReps;

    @Drone
    protected WebDriver driver;

    @Page
    protected AuthServerContextRoot authServerContextRootPage;
    @Page
    protected AuthServer authServerPage;

    @Page
    protected AuthRealm masterRealmPage;

    @Page
    protected Account accountPage;
    @Page
    protected OIDCLogin loginPage;
    @Page
    protected UpdatePassword updatePasswordPage;

    protected UserRepresentation adminUser;

    @Before
    public void beforeAbstractKeycloakTest() {
        adminUser = createAdminUserRepresentation();

        setDefaultPageUriParameters();

        driverSettings();

        if (!suiteContext.isAdminPasswordUpdated()) {
            log.debug("updating admin password");
            updateMasterAdminPassword();
            suiteContext.setAdminPasswordUpdated(true);
        }

        importTestRealms();
    }

    @After
    public void afterAbstractKeycloakTest() {
//        removeTestRealms(); // keeping test realms after test to be able to inspect failures, instead deleting existing realms before import
//        keycloak.close(); // keeping admin connection open
        Timer.printStats();
    }

    private void updateMasterAdminPassword() {
        accountPage.navigateTo();
        loginPage.form().login(ADMIN, ADMIN);
        updatePasswordPage.updatePasswords(ADMIN, ADMIN);
        assertCurrentUrlStartsWith(accountPage);
        deleteAllCookiesForMasterRealm();
    }

    public void deleteAllCookiesForMasterRealm() {
        masterRealmPage.navigateTo();
        log.debug("deleting cookies in master realm");
        driver.manage().deleteAllCookies();
    }

    protected void driverSettings() {
        driver.manage().timeouts().pageLoadTimeout(WaitUtils.PAGELOAD_TIMEOUT, TimeUnit.MILLISECONDS);
        driver.manage().timeouts().implicitlyWait(WaitUtils.IMPLICIT_TIMEOUT, TimeUnit.MILLISECONDS);
        driver.manage().timeouts().setScriptTimeout(WaitUtils.SCRIPT_TIMEOUT, TimeUnit.MILLISECONDS);
        driver.manage().window().maximize();
    }

    public void setDefaultPageUriParameters() {
        masterRealmPage.setAuthRealm(MASTER);
        loginPage.setAuthRealm(MASTER);
    }

    public abstract void addTestRealms(List<RealmRepresentation> testRealms);

    private void addTestRealms() {
        log.debug("loading test realms");
        if (testRealmReps == null) {
            testRealmReps = new ArrayList<>();
        }
        if (testRealmReps.isEmpty()) {
            addTestRealms(testRealmReps);
        }
    }

    public void importTestRealms() {
        addTestRealms();
        log.info("importing test realms");
        for (RealmRepresentation testRealm : testRealmReps) {
            importRealm(testRealm);
        }
    }

    public void removeTestRealms() {
        log.info("removing test realms");
        for (RealmRepresentation testRealm : testRealmReps) {
            removeRealm(testRealm);
        }
    }

    private UserRepresentation createAdminUserRepresentation() {
        UserRepresentation adminUserRep = new UserRepresentation();
        adminUserRep.setUsername(ADMIN);
        setPasswordFor(adminUserRep, ADMIN);
        return adminUserRep;
    }

    public void importRealm(RealmRepresentation realm) {
        log.debug("importing realm: " + realm.getRealm());
        try { // TODO - figure out a way how to do this without try-catch
            RealmResource realmResource = adminClient.realms().realm(realm.getRealm());
            RealmRepresentation rRep = realmResource.toRepresentation();
            log.debug("realm already exists on server, re-importing");
            realmResource.remove();
        } catch (NotFoundException nfe) {
            // expected when realm does not exist
        }
        adminClient.realms().create(realm);
    }

    public void removeRealm(RealmRepresentation realm) {
        adminClient.realms().realm(realm.getRealm()).remove();
    }

}
