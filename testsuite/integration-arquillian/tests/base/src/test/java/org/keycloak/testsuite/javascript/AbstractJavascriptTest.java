package org.keycloak.testsuite.javascript;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.BeforeClass;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.JavascriptBrowser;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RolesBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.javascript.JavascriptStateValidator;
import org.keycloak.testsuite.util.javascript.ResponseValidator;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Options;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author mhajas
 */
public abstract class AbstractJavascriptTest extends AbstractAuthTest {

    @FunctionalInterface
    interface QuadFunction<T, U, V, W> {
        void apply(T a, U b, V c, W d);
    }

    public static final String NIP_IO_URL = "js-app-127-0-0-1.nip.io";
    public static final String CLIENT_ID = "js-console";
    public static final String REALM_NAME = "test";
    public static final String SPACE_REALM_NAME = "Example realm";
    public static final String JAVASCRIPT_URL = "/auth/realms/" + REALM_NAME + "/testing/javascript";
    public static final String JAVASCRIPT_ENCODED_SPACE_URL = "/auth/realms/Example%20realm/testing/javascript";
    public static final String JAVASCRIPT_SPACE_URL = "/auth/realms/Example realm/testing/javascript";
    public static int TOKEN_LIFESPAN_LEEWAY = 3; // seconds


    protected JavascriptExecutor jsExecutor;

    // Javascript browser needed KEYCLOAK-4703
    @Drone
    @JavascriptBrowser
    protected WebDriver jsDriver;

    @Page
    @JavascriptBrowser
    protected OIDCLogin jsDriverTestRealmLoginPage;

    @FindBy(id = "output")
    @JavascriptBrowser
    protected WebElement outputArea;

    @FindBy(id = "events")
    @JavascriptBrowser
    protected WebElement eventsArea;

    public static final UserRepresentation testUser;
    public static final UserRepresentation unauthorizedUser;

    static {
        testUser = UserBuilder.create().username("test-user@localhost").password("password").build();
        unauthorizedUser = UserBuilder.create().username("unauthorized").password("password").build();
    }

    @BeforeClass
    public static void enabledOnlyWithSSL() {
        ContainerAssume.assumeAuthServerSSL();
    }

    @Before
    public void beforeJavascriptTest() {
        jsExecutor = (JavascriptExecutor) jsDriver;
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(updateRealm(RealmBuilder.create()
                .name(REALM_NAME)
                .roles(
                        RolesBuilder.create()
                                .realmRole(new RoleRepresentation("user", "", false))
                                .realmRole(new RoleRepresentation("admin", "", false))
                )
                .user(
                        UserBuilder.create()
                                .username("test-user@localhost").password("password")
                                .addRoles("user")
                                .role("realm-management", "view-realm")
                                .role("realm-management", "manage-users")
                                .role("account", "view-profile")
                                .role("account", "manage-account")
                )
                .user(
                        UserBuilder.create()
                                .username("unauthorized").password("password")
                )
                .client(
                        ClientBuilder.create()
                                .clientId(CLIENT_ID)
                                .redirectUris(oauth.SERVER_ROOT.replace("localhost", NIP_IO_URL) + JAVASCRIPT_URL + "/*", oauth.SERVER_ROOT + JAVASCRIPT_ENCODED_SPACE_URL + "/*")
                                .addWebOrigin(oauth.SERVER_ROOT.replace("localhost", NIP_IO_URL))
                                .publicClient()
                )
                .accessTokenLifespan(30 + TOKEN_LIFESPAN_LEEWAY)
                .testEventListener()
        ));
    }

    protected <T> JavascriptStateValidator buildFunction(QuadFunction<T, WebDriver, Object, WebElement> f, T x) {
        return (y,z,w) -> f.apply(x, y, z, w);
    }

    protected void setImplicitFlowForClient() {
        ClientResource clientResource = ApiUtil.findClientResourceByClientId(adminClient.realms().realm(REALM_NAME), CLIENT_ID);
        ClientRepresentation client = clientResource.toRepresentation();
        client.setImplicitFlowEnabled(true);
        client.setStandardFlowEnabled(false);
        clientResource.update(client);
    }

    protected void setStandardFlowForClient() {
        ClientResource clientResource = ApiUtil.findClientResourceByClientId(adminClient.realms().realm(REALM_NAME), CLIENT_ID);
        ClientRepresentation client = clientResource.toRepresentation();
        client.setImplicitFlowEnabled(false);
        client.setStandardFlowEnabled(true);
        clientResource.update(client);
    }

    protected abstract RealmRepresentation updateRealm(RealmBuilder builder);

    protected void assertSuccessfullyLoggedIn(WebDriver driver1, Object output, WebElement events) {
        buildFunction(this::assertOutputContains, "Init Success (Authenticated)").validate(driver1, output, events);
        waitUntilElement(events).text().contains("Auth Success");
    }

    protected void assertInitNotAuth(WebDriver driver1, Object output, WebElement events) {
        buildFunction(this::assertOutputContains, "Init Success (Not Authenticated)").validate(driver1, output, events);
    }

    protected void assertOnLoginPage(WebDriver driver1, Object output, WebElement events) {
        waitUntilElement(By.tagName("body")).is().present();
        assertCurrentUrlStartsWith(jsDriverTestRealmLoginPage, driver1);
    }

    public void assertOutputWebElementContains(String value, WebDriver driver1, Object output, WebElement events) {
        waitUntilElement((WebElement) output).text().contains(value);
    }
    
    public void assertLocaleCookie(String locale, WebDriver driver1, Object output, WebElement events) {
        waitForPageToLoad();
        Options ops = driver1.manage();
        Cookie cookie = ops.getCookieNamed("KEYCLOAK_LOCALE");
        Assert.assertNotNull(cookie);
        Assert.assertEquals(locale, cookie.getValue());
    }
    
    public JavascriptStateValidator assertLocaleIsSet(String locale) {
        return buildFunction(this::assertLocaleCookie, locale);
    }

    public void assertOutputContains(String value, WebDriver driver1, Object output, WebElement events) {
        if (output instanceof WebElement) {
            waitUntilElement((WebElement) output).text().contains(value);
        } else {
            Assert.assertThat((String) output, containsString(value));
        }
    }

    public void assertEventsWebElementContains(String value, WebDriver driver1, Object output, WebElement events) {
        waitUntilElement(events).text().contains(value);
    }

    public ResponseValidator assertResponseStatus(long status) {
        return output -> Assert.assertThat(output, hasEntry("status", status));
    }

    public JavascriptStateValidator assertOutputContains(String text) {
        return buildFunction(this::assertOutputContains, text);
    }

    public JavascriptStateValidator assertEventsContains(String text) {
        return buildFunction(this::assertEventsWebElementContains, text);
    }
}
