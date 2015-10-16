package org.keycloak.testsuite.adapter.example;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.JSConsoleExample;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.account.Applications;
import org.keycloak.testsuite.auth.page.login.OAuthGrant;
import org.keycloak.testsuite.console.page.clients.ClientSettings;
import org.keycloak.testsuite.console.page.clients.Clients;
import org.keycloak.testsuite.console.page.events.Config;
import org.keycloak.testsuite.console.page.events.LoginEvents;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.auth.page.AuthRealm.EXAMPLE;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlDoesntStartWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.pause;

public abstract class AbstractJSConsoleExampleAdapterTest extends AbstractExampleAdapterTest {

    @Page
    private JSConsoleExample jsConsoleExamplePage;

    @Page
    private Clients clientsPage;

    @Page
    private ClientSettings clientSettingsPage;

    @Page
    private Config configPage;

    @Page
    private LoginEvents loginEventsPage;

    @Page
    private OAuthGrant oAuthGrantPage;

    @Page
    private Applications applicationsPage;

    public static int TOKEN_LIFESPAN_LEEWAY = 3; // seconds

    @Deployment(name = JSConsoleExample.DEPLOYMENT_NAME)
    private static WebArchive jsConsoleExample() throws IOException {
        return exampleDeployment(JSConsoleExample.CLIENT_ID);
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation jsConsoleRealm = loadRealm(new File(EXAMPLES_HOME_DIR + "/js-console/example-realm.json"));

        fixClientUrisUsingDeploymentUrl(jsConsoleRealm,
                JSConsoleExample.CLIENT_ID, jsConsoleExamplePage.buildUri().toASCIIString());

        jsConsoleRealm.setAccessTokenLifespan(30 + TOKEN_LIFESPAN_LEEWAY); // seconds

        testRealms.add(jsConsoleRealm);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(EXAMPLE);
    }

    @Test
    public void testJSConsoleAuth() {
        jsConsoleExamplePage.navigateTo();
        assertCurrentUrlStartsWith(jsConsoleExamplePage);

        pause(1000);

        jsConsoleExamplePage.logIn();
        testRealmLoginPage.form().login("user", "invalid-password");
        assertCurrentUrlDoesntStartWith(jsConsoleExamplePage);

        testRealmLoginPage.form().login("invalid-user", "password");
        assertCurrentUrlDoesntStartWith(jsConsoleExamplePage);

        testRealmLoginPage.form().login("user", "password");
        assertCurrentUrlStartsWith(jsConsoleExamplePage);
        assertTrue(driver.getPageSource().contains("Init Success (Authenticated)"));
        assertTrue(driver.getPageSource().contains("Auth Success"));

        pause(1000);

        jsConsoleExamplePage.logOut();
        assertCurrentUrlStartsWith(jsConsoleExamplePage);
        assertTrue(driver.getPageSource().contains("Init Success (Not Authenticated)"));
    }

    @Test
    public void testRefreshToken() {
        jsConsoleExamplePage.navigateTo();
        assertCurrentUrlStartsWith(jsConsoleExamplePage);

        jsConsoleExamplePage.refreshToken();
        assertTrue(driver.getPageSource().contains("Failed to refresh token"));

        jsConsoleExamplePage.logIn();
        testRealmLoginPage.form().login("user", "password");
        assertCurrentUrlStartsWith(jsConsoleExamplePage);
        assertTrue(driver.getPageSource().contains("Auth Success"));

        jsConsoleExamplePage.refreshToken();
        assertTrue(driver.getPageSource().contains("Auth Refresh Success"));
    }

    @Test
    public void testRefreshTokenIfUnder30s() {
        jsConsoleExamplePage.navigateTo();
        assertCurrentUrlStartsWith(jsConsoleExamplePage);

        jsConsoleExamplePage.refreshToken();
        assertTrue(driver.getPageSource().contains("Failed to refresh token"));

        jsConsoleExamplePage.logIn();
        testRealmLoginPage.form().login("user", "password");
        assertCurrentUrlStartsWith(jsConsoleExamplePage);
        assertTrue(driver.getPageSource().contains("Auth Success"));

        jsConsoleExamplePage.refreshTokenIfUnder30s();
        assertTrue(driver.getPageSource().contains("Token not refreshed, valid for"));

        pause((TOKEN_LIFESPAN_LEEWAY + 2) * 1000);

        jsConsoleExamplePage.refreshTokenIfUnder30s();
        assertTrue(driver.getPageSource().contains("Auth Refresh Success"));
    }

    @Test
    public void testGetProfile() {
        jsConsoleExamplePage.navigateTo();
        assertCurrentUrlStartsWith(jsConsoleExamplePage);

        jsConsoleExamplePage.getProfile();
        assertTrue(driver.getPageSource().contains("Failed to load profile"));

        jsConsoleExamplePage.logIn();
        testRealmLoginPage.form().login("user", "password");
        assertCurrentUrlStartsWith(jsConsoleExamplePage);
        assertTrue(driver.getPageSource().contains("Auth Success"));

        jsConsoleExamplePage.getProfile();
        assertTrue(driver.getPageSource().contains("Failed to load profile"));
        assertTrue(driver.getPageSource().contains("\"username\": \"user\""));
    }

    @Test
    public void grantBrowserBasedApp() {
        testRealmPage.setAuthRealm(EXAMPLE);
        testRealmLoginPage.setAuthRealm(EXAMPLE);
        clientsPage.setConsoleRealm(EXAMPLE);
        configPage.setConsoleRealm(EXAMPLE);
        loginEventsPage.setConsoleRealm(EXAMPLE);
        applicationsPage.setAuthRealm(EXAMPLE);

        jsConsoleExamplePage.navigateTo();
        driver.manage().deleteAllCookies();

        clientsPage.navigateTo();

        loginPage.form().login("admin", "admin");

        ClientResource clientResource = ApiUtil.findClientResourceByClientId(testRealmResource(), "js-console");
        ClientRepresentation client = clientResource.toRepresentation();
        client.setConsentRequired(true);
        clientResource.update(client);

        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setEventsEnabled(true);
        realm.setEnabledEventTypes(Arrays.asList("REVOKE_GRANT", "LOGIN"));
        testRealmResource().update(realm);

        jsConsoleExamplePage.navigateTo();
        jsConsoleExamplePage.logIn();

        testRealmLoginPage.form().login("user", "password");

        assertTrue(oAuthGrantPage.isCurrent());
        oAuthGrantPage.accept();

        assertTrue(driver.getPageSource().contains("Init Success (Authenticated)"));

        applicationsPage.navigateTo();
        applicationsPage.revokeGrantForApplication("js-console");

        jsConsoleExamplePage.navigateTo();
        jsConsoleExamplePage.logIn();

        assertTrue(oAuthGrantPage.isCurrent());

        loginEventsPage.navigateTo();
        loginEventsPage.table().filter();
        loginEventsPage.table().filterForm().addEventType("REVOKE_GRANT");
        loginEventsPage.table().update();

        List<WebElement> resultList = loginEventsPage.table().rows();

        assertEquals(2, resultList.size());

        resultList.get(0).findElement(By.xpath(".//td[text()='REVOKE_GRANT']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='Client']/../td[text()='account']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='IP Address']/../td[text()='127.0.0.1']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='revoked_client']/../td[text()='js-console']"));

        loginEventsPage.table().reset();
        loginEventsPage.table().filterForm().addEventType("LOGIN");
        loginEventsPage.table().update();
        resultList = loginEventsPage.table().rows();

        assertEquals(7, resultList.size());

        resultList.get(0).findElement(By.xpath(".//td[text()='LOGIN']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='Client']/../td[text()='js-console']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='IP Address']/../td[text()='127.0.0.1']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='username']/../td[text()='user']"));
        resultList.get(0).findElement(By.xpath(".//td[text()='consent']/../td[text()='consent_granted']"));
    }

}
