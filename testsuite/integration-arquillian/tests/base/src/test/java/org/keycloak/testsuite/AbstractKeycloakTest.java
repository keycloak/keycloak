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
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import org.keycloak.testsuite.arquillian.SuiteContext;
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
import org.keycloak.testsuite.util.Timer;

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

    @ArquillianResource
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

        importTestRealms();
    }

    @After
    public void afterAbstractKeycloakTest() {
//        removeTestRealms(); // keeping test realms after test to be able to inspect failures, instead deleting existing realms before import
//        keycloak.close(); // keeping admin connection open
        Timer.printStats();
    }

    private void updateMasterAdminPassword() {
        account.navigateTo();
        login.form().login(ADMIN, ADMIN);
        updatePassword.updatePasswords(ADMIN, ADMIN);
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
            importRealm(testRealm);
        }
    }

    public void removeTestRealms() {
        System.out.println("removing test realms");
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
        System.out.println("importing realm: " + realm.getRealm());
        try { // TODO - figure out a way how to do this without try-catch
            RealmResource realmResource = adminClient.realms().realm(realm.getRealm());
            RealmRepresentation rRep = realmResource.toRepresentation();
            System.out.println(" realm already exists on server, re-importing");
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
