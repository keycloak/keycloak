package org.keycloak.testsuite.springboot;

import org.jboss.arquillian.graphene.page.Page;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.test.TestsHelper;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.pages.LoginPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import static org.keycloak.testsuite.admin.ApiUtil.*;

public class SpringBootTest extends AbstractKeycloakTest {

	private static final Logger log = Logger.getLogger(SpringBootTest.class);
    private static final String REALM_NAME = "test";

    private static final String CLIENT_ID = "spring-boot-app";
    private static final String SECRET = "e3789ac5-bde6-4957-a7b0-612823dac101";

    private static final String APPLICATION_URL = "http://localhost:8280";
    private static final String BASE_URL = APPLICATION_URL + "/admin";

    private static final String USER_LOGIN = "testuser";
    private static final String USER_EMAIL = "user@email.test";
    private static final String USER_PASSWORD = "user-password";

    private static final String USER_LOGIN_2 = "testuser2";
    private static final String USER_EMAIL_2 = "user2@email.test";
    private static final String USER_PASSWORD_2 = "user2-password";

    private static final String CORRECT_ROLE = "admin";
    private static final String INCORRECT_ROLE = "wrong-admin";

    @Page
    private LoginPage loginPage;

    @Page
    private SpringApplicationPage applicationPage;

    @Page
    private SpringAdminPage adminPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(REALM_NAME);
        realm.setEnabled(true);

        realm.setClients(Collections.singletonList(createClient()));

        testRealms.add(realm);
    }

    private ClientRepresentation createClient() {
        ClientRepresentation clientRepresentation = new ClientRepresentation();

        clientRepresentation.setId(CLIENT_ID);
        clientRepresentation.setSecret(SECRET);

        clientRepresentation.setBaseUrl(BASE_URL);
        clientRepresentation.setRedirectUris(Collections.singletonList(BASE_URL + "/*"));
        clientRepresentation.setAdminUrl(BASE_URL);

        return clientRepresentation;
    }

    private void addUser(String login, String email, String password, String... roles) {
        UserRepresentation userRepresentation = new UserRepresentation();

        userRepresentation.setUsername(login);
        userRepresentation.setEmail(email);
        userRepresentation.setEmailVerified(true);
        userRepresentation.setEnabled(true);

        RealmResource realmResource = adminClient.realm(REALM_NAME);
        String userId = createUserWithAdminClient(realmResource, userRepresentation);

        resetUserPassword(realmResource.users().get(userId), password, false);

        for (String role : roles)
            assignRealmRoles(realmResource, userId, role);
    }

    private String getAuthRoot(SuiteContext suiteContext) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString();
    }

    private String encodeUrl(String url) {
        String result;
        try {
            result = URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            result = url;
        }

        return result;
    }

    private void waitForPage(WebDriver driver, final String title) {
        WebDriverWait wait = new WebDriverWait(driver, 5);

        ExpectedCondition<Boolean> condition = (WebDriver input) -> input.getTitle().toLowerCase().contains(title);

        wait.until(condition);
    }

    @Before
    public void createRoles() {
        RealmResource realm = realmsResouce().realm(REALM_NAME);

        RoleRepresentation correct = new RoleRepresentation(CORRECT_ROLE, CORRECT_ROLE, false);
        realm.roles().create(correct);

        RoleRepresentation incorrect = new RoleRepresentation(INCORRECT_ROLE, INCORRECT_ROLE, false);
        realm.roles().create(incorrect);
    }

    @Before
    public void addUsers() {
        addUser(USER_LOGIN, USER_EMAIL, USER_PASSWORD, CORRECT_ROLE);
        addUser(USER_LOGIN_2, USER_EMAIL_2, USER_PASSWORD_2, INCORRECT_ROLE);
    }

    @After
    public void cleanupUsers() {
        RealmResource providerRealm = adminClient.realm(REALM_NAME);
        UserRepresentation userRep = ApiUtil.findUserByUsername(providerRealm, USER_LOGIN);
        if (userRep != null) {
            providerRealm.users().get(userRep.getId()).remove();
        }

        RealmResource childRealm = adminClient.realm(REALM_NAME);
        userRep = ApiUtil.findUserByUsername(childRealm, USER_LOGIN_2);
        if (userRep != null) {
            childRealm.users().get(userRep.getId()).remove();
        }
    }

    @After
    public void cleanupRoles() {
        RealmResource realm = realmsResouce().realm(REALM_NAME);

        RoleResource correctRole = realm.roles().get(CORRECT_ROLE);
        correctRole.remove();

        RoleResource incorrectRole = realm.roles().get(INCORRECT_ROLE);
        incorrectRole.remove();
    }

    @Test
    public void testCorrectUser() {
        driver.navigate().to(APPLICATION_URL + "/index.html");

        Assert.assertTrue("Must be on application page", applicationPage.isCurrent());

        applicationPage.goAdmin();

        Assert.assertTrue("Must be on login page", loginPage.isCurrent());

        loginPage.login(USER_LOGIN, USER_PASSWORD);

        Assert.assertTrue("Must be on admin page", adminPage.isCurrent());
        Assert.assertTrue("Admin page must contain correct div",
                driver.getPageSource().contains("You are now admin"));

        driver.navigate().to(getAuthRoot(suiteContext)
                + "/auth/realms/" + REALM_NAME
                + "/protocol/" + "openid-connect"
                + "/logout?redirect_uri=" + encodeUrl(BASE_URL));

        Assert.assertTrue("Must be on login page", loginPage.isCurrent());

    }

    @Test
    public void testIncorrectUser() {
        driver.navigate().to(APPLICATION_URL + "/index.html");

        Assert.assertTrue("Must be on application page", applicationPage.isCurrent());

        applicationPage.goAdmin();

        Assert.assertTrue("Must be on login page", loginPage.isCurrent());


        loginPage.login(USER_LOGIN_2, USER_PASSWORD_2);

        Assert.assertTrue("Must return 403 because of incorrect role",
                driver.getPageSource().contains("There was an unexpected error (type=Forbidden, status=403)")
                || driver.getPageSource().contains("\"status\":403,\"error\":\"Forbidden\""));
    }

    @Test
    public void testIncorrectCredentials() {
        driver.navigate().to(APPLICATION_URL + "/index.html");

        Assert.assertTrue("Must be on application page", applicationPage.isCurrent());

        applicationPage.goAdmin();

        Assert.assertTrue("Must be on login page", loginPage.isCurrent());

        loginPage.login(USER_LOGIN, USER_PASSWORD_2);

        Assert.assertEquals("Error message about password",
                "Invalid username or password.", loginPage.getError());
    }

}
