package org.keycloak.testsuite.springboot;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.auth.page.account.Sessions;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.SecondBrowser;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

@DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
public class SessionSpringBootTest extends AbstractSpringBootTest {

    private static final String SERVLET_URL = BASE_URL + "/SessionServlet";

    static final String USER_LOGIN_CORRECT_2 = "testcorrectuser2";
    static final String USER_EMAIL_CORRECT_2 = "usercorrect2@email.test";
    static final String USER_PASSWORD_CORRECT_2 = "testcorrectpassword2";

    @Page
    private SessionPage sessionPage;

    @Page
    @SecondBrowser
    private SessionPage secondBrowserSessionPage;

    @Drone
    @SecondBrowser
    private WebDriver driver2;

    @Page
    @SecondBrowser
    private OIDCLogin secondTestRealmLoginPage;

    @Page
    private Sessions realmSessions;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        realmSessions.setAuthRealm(REALM_NAME);
        testRealmLoginPage.setAuthRealm(REALM_NAME);
        secondTestRealmLoginPage.setAuthRealm(REALM_NAME);
    }

    private void loginAndCheckSession() {
        driver.navigate().to(SERVLET_URL);
        waitForPageToLoad();

        assertCurrentUrlStartsWith(testRealmLoginPage, driver);
        testRealmLoginPage.form().login(USER_LOGIN, USER_PASSWORD);

        sessionPage.assertIsCurrent();
        assertThat(sessionPage.getCounter(), is(equalTo(0)));

        driver.navigate().to(SERVLET_URL);
        waitForPageToLoad();

        assertThat(sessionPage.getCounter(), is(equalTo(1)));
    }

    @Before
    public void addUserCorrect2() {
        addUser(USER_LOGIN_CORRECT_2, USER_EMAIL_CORRECT_2, USER_PASSWORD_CORRECT_2, CORRECT_ROLE);
    }

    @After
    public void removeUserCorrect2() {
        UserRepresentation userRep = ApiUtil.findUserByUsername(realmsResouce().realm(REALM_NAME), USER_LOGIN_CORRECT_2);
        if (userRep != null) {
            realmsResouce().realm(REALM_NAME).users().get(userRep.getId()).remove();
        }
    }

    @Test
    public void testSingleSessionInvalidated() {
        loginAndCheckSession();

        DroneUtils.addWebDriver(driver2);

        driver2.navigate().to(SERVLET_URL);
        waitForPageToLoad(); // driver2 will be used because of DroneUtils.addWebDriver()

        log.info("current title is " + driver2.getTitle());
        assertCurrentUrlStartsWith(secondTestRealmLoginPage, driver2);
        secondTestRealmLoginPage.form().login(USER_LOGIN, USER_PASSWORD);

        secondBrowserSessionPage.assertIsCurrent();

        assertThat(secondBrowserSessionPage.getCounter(), is(equalTo(0)));

        // Counter increased now
        driver2.navigate().to(SERVLET_URL);
        waitForPageToLoad(); // driver2 will be used because of DroneUtils.addWebDriver()

        assertThat(secondBrowserSessionPage.getCounter(), is(equalTo(1)));

        DroneUtils.removeWebDriver(); // From now driver will be used instead of driver2

        // Logout in browser1
        driver.navigate().to(logoutPage(SERVLET_URL));
        waitForPageToLoad();

        // Assert that I am logged out in browser1
        driver.navigate().to(SERVLET_URL);
        waitForPageToLoad();

        assertCurrentUrlStartsWith(testRealmLoginPage, driver);

        // Assert that I am still logged in browser2 and same session is still preserved
        DroneUtils.addWebDriver(driver2);
        driver2.navigate().to(SERVLET_URL);
        waitForPageToLoad();

        secondBrowserSessionPage.assertIsCurrent();
        assertThat(secondBrowserSessionPage.getCounter(), is(equalTo(2)));

        driver2.navigate().to(logoutPage(SERVLET_URL));
        waitForPageToLoad();
        assertCurrentUrlStartsWith(secondTestRealmLoginPage, driver2);

        DroneUtils.removeWebDriver();
    }

    @Test
    public void testSessionInvalidatedAfterFailedRefresh() {
        RealmResource realmResource = adminClient.realm(REALM_NAME);
        RealmRepresentation realmRep = realmResource.toRepresentation();
        ClientResource clientResource = null;
        for (ClientRepresentation clientRep : realmResource.clients().findAll()) {
            if (CLIENT_ID.equals(clientRep.getClientId())) {
                clientResource = realmResource.clients().get(clientRep.getId());
            }
        }

        assertThat(clientResource, is(notNullValue()));

        clientResource.toRepresentation().setAdminUrl("");
        int origTokenLifespan = realmRep.getAccessCodeLifespan();
        realmRep.setAccessCodeLifespan(1);
        realmResource.update(realmRep);

        // Login
        loginAndCheckSession();

        // Logout
        String logoutUri = logoutPage(SERVLET_URL);
        driver.navigate().to(logoutUri);
        waitForPageToLoad();

        // Assert that http session was invalidated
        driver.navigate().to(SERVLET_URL);
        waitForPageToLoad();

        assertCurrentUrlStartsWith(testRealmLoginPage, driver);
        testRealmLoginPage.form().login(USER_LOGIN, USER_PASSWORD);

        sessionPage.assertIsCurrent();
        assertThat(sessionPage.getCounter(), is(equalTo(0)));

        clientResource.toRepresentation().setAdminUrl(BASE_URL);
        realmRep.setAccessCodeLifespan(origTokenLifespan);
        realmResource.update(realmRep);

        driver.navigate().to(logoutUri);
        waitForPageToLoad();
    }

    @Test
    public void testAdminApplicationLogout() {
        loginAndCheckSession();

        // logout user2 with admin client
        UserRepresentation correct2 = realmsResouce().realm(REALM_NAME)
                .users().search(USER_LOGIN_CORRECT_2, null, null, null, null, null).get(0);
        realmsResouce().realm(REALM_NAME).users().get(correct2.getId()).logout();

        // user1 should be still logged with original httpSession in our browser window
        driver.navigate().to(SERVLET_URL);
        waitForPageToLoad();

        sessionPage.assertIsCurrent();
        assertThat(sessionPage.getCounter(), is(equalTo(2)));

        driver.navigate().to(logoutPage(SERVLET_URL));
        waitForPageToLoad();
    }

    @Test
    public void testAccountManagementSessionsLogout() {
        loginAndCheckSession();

        realmSessions.navigateTo();
        realmSessions.logoutAll();

        // Assert I need to login again (logout was propagated to the app)
        loginAndCheckSession();

        driver.navigate().to(logoutPage(SERVLET_URL));
        waitForPageToLoad();
    }
}
