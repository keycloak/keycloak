package org.keycloak.testsuite.springboot;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.account.Sessions;
import org.keycloak.testsuite.util.SecondBrowser;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class SessionSpringBootTest extends AbstractSpringBootTest {

    private static final String SERVLET_URL = BASE_URL + "/SessionServlet";

    static final String USER_LOGIN_CORRECT_2 = "testcorrectuser2";
    static final String USER_EMAIL_CORRECT_2 = "usercorrect2@email.test";
    static final String USER_PASSWORD_CORRECT_2 = "testcorrectpassword2";

    @Page
    private SessionPage sessionPage;

    @Drone
    @SecondBrowser
    private WebDriver driver2;

    @Page
    private Sessions realmSessions;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        realmSessions.setAuthRealm(REALM_NAME);
    }

    private void loginAndCheckSession() {
        driver.navigate().to(SERVLET_URL);
        Assert.assertTrue("Must be on login page", loginPage.isCurrent());
        loginPage.login(USER_LOGIN, USER_PASSWORD);
        WaitUtils.waitUntilElement(By.tagName("body")).is().visible();
        Assert.assertTrue("Must be on servlet page", sessionPage.isCurrent());
        Assert.assertEquals("Counter must be 0", 0, sessionPage.getCounter());

        driver.navigate().to(SERVLET_URL);
        Assert.assertEquals("Counter now must be 1", 1, sessionPage.getCounter());
    }

    private boolean checkCounterInSource(WebDriver driver, int counter) {
        return driver.getPageSource().replaceAll("\\s", "")
                .contains("<spanid=\"counter\">" + counter + "</span>");
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

        // cannot pass to loginAndCheckSession becayse loginPage is not working together with driver2, therefore copypasta
        driver2.navigate().to(SERVLET_URL);
        log.info("current title is " + driver2.getTitle());
        Assert.assertTrue("Must be on login page", driver2.getTitle().toLowerCase().startsWith("log in to"));
        driver2.findElement(By.id("username")).sendKeys(USER_LOGIN);
        driver2.findElement(By.id("password")).sendKeys(USER_PASSWORD);
        driver2.findElement(By.id("password")).submit();
        Assert.assertTrue("Must be on session page", driver2.getTitle().equals(SessionPage.PAGE_TITLE));
        Assert.assertTrue("Counter must be 0", checkCounterInSource(driver2, 0));
        // Counter increased now
        driver2.navigate().to(SERVLET_URL);
        Assert.assertTrue("Counter must be 1", checkCounterInSource(driver2, 1));

        // Logout in browser1
        driver.navigate().to(logoutPage(SERVLET_URL));

        // Assert that I am logged out in browser1
        driver.navigate().to(SERVLET_URL);
        Assert.assertTrue("Must be on login page", loginPage.isCurrent());

        // Assert that I am still logged in browser2 and same session is still preserved
        driver2.navigate().to(SERVLET_URL);
        Assert.assertTrue("Must be on session page", driver2.getTitle().equals(SessionPage.PAGE_TITLE));
        Assert.assertTrue("Counter must be 2", checkCounterInSource(driver2, 2));

        driver2.navigate().to(logoutPage(SERVLET_URL));
        Assert.assertTrue("Must be on login page", driver2.getTitle().toLowerCase().startsWith("log in to"));

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
        Assert.assertNotNull(clientResource);
        clientResource.toRepresentation().setAdminUrl("");
        int origTokenLifespan = realmRep.getAccessCodeLifespan();
        realmRep.setAccessCodeLifespan(1);
        realmResource.update(realmRep);

        // Login
        loginAndCheckSession();

        // Logout
        String logoutUri = logoutPage(SERVLET_URL);
        driver.navigate().to(logoutUri);

        // Assert that http session was invalidated
        driver.navigate().to(SERVLET_URL);
        Assert.assertTrue("Must be on login page", loginPage.isCurrent());
        loginPage.login(USER_LOGIN, USER_PASSWORD);
        Assert.assertTrue("Must be on session page", sessionPage.isCurrent());
        Assert.assertEquals("Counter must be 0", 0, sessionPage.getCounter());

        clientResource.toRepresentation().setAdminUrl(BASE_URL);
        realmRep.setAccessCodeLifespan(origTokenLifespan);
        realmResource.update(realmRep);
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
        Assert.assertTrue("Must be on session page", sessionPage.isCurrent());
        Assert.assertEquals("Counter must be 2", 2, sessionPage.getCounter());
        driver.navigate().to(logoutPage(SERVLET_URL));
    }

    @Test
    public void testAccountManagementSessionsLogout() {
        loginAndCheckSession();
        realmSessions.navigateTo();
        realmSessions.logoutAll();
        // Assert I need to login again (logout was propagated to the app)
        loginAndCheckSession();
    }
}
