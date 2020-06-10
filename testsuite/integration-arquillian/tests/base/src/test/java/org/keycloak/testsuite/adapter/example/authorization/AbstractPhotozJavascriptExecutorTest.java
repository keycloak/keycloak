package org.keycloak.testsuite.adapter.example.authorization;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.auth.page.login.OAuthGrant;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.util.JavascriptBrowser;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.javascript.JSObjectBuilder;
import org.keycloak.testsuite.util.javascript.JavascriptStateValidator;
import org.keycloak.testsuite.util.javascript.ResponseValidator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.hamcrest.CoreMatchers.containsString;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author mhajas
 */
public abstract class AbstractPhotozJavascriptExecutorTest extends AbstractExampleAdapterTest {

    @FunctionalInterface
    interface QuadFunction<T, U, V, W> {
        void apply(T a, U b, V c, W d);
    }

    protected static final String REALM_NAME = "photoz";

    @Page
    @JavascriptBrowser
    protected OIDCLogin jsDriverTestRealmLoginPage;

    @Page
    @JavascriptBrowser
    private OAuthGrant oAuthGrantPage;

    @Drone
    @JavascriptBrowser
    protected WebDriver jsDriver;

    protected UserRepresentation aliceUser = UserBuilder.create().username("alice").password("alice").build();

    protected UserRepresentation adminUser = UserBuilder.create().username("admin").password("admin").build();

    protected UserRepresentation jdoeUser = UserBuilder.create().username("jdoe").password("jdoe").build();

    @BeforeClass
    public static void checkIfTLSIsTurnedOn() {
        Assume.assumeTrue(AUTH_SERVER_SSL_REQUIRED);
    }

    @Before
    public void setDefaultValues() {
        jsDriverTestRealmLoginPage.setAuthRealm(REALM_NAME);
    }

    protected <T> JavascriptStateValidator buildFunction(QuadFunction<T, WebDriver, Object, WebElement> f, T x) {
        return (y,z,w) -> f.apply(x, y, z, w);
    }

    public void assertOutputContains(String value, WebDriver driver1, Object output, WebElement events) {
        if (output instanceof WebElement) {
            waitUntilElement((WebElement) output).text().contains(value);
        } else {
            Assert.assertThat((String) output, containsString(value));
        }
    }

    protected JSObjectBuilder defaultArguments() {
        return JSObjectBuilder.create().defaultSettings();
    }

    protected void assertSuccessfullyLoggedIn(WebDriver driver1, Object output, WebElement events) {
        buildFunction(this::assertOutputContains, "Init Success (Authenticated)").validate(driver1, output, events);
    }

    protected void assertInitNotAuth(WebDriver driver1, Object output, WebElement events) {
        buildFunction(this::assertOutputContains, "Init Success (Not Authenticated)").validate(driver1, output, events);
    }

    protected void assertOnLoginPage(WebDriver driver1, Object output, WebElement events) {
        waitUntilElement(By.tagName("body")).is().present();
        try {
            assertCurrentUrlStartsWith(jsDriverTestRealmLoginPage, driver1);
        } catch (AssertionError e) {
            System.out.println("Test");
            throw e;
        }
    }

    protected JavascriptStateValidator all(JavascriptStateValidator[] toValidate)  {
        return ((driver1, output, events) -> {
            for (JavascriptStateValidator val : toValidate) {
                val.validate(driver1, output, events);
            }
        });
    }
    protected ResponseValidator all(ResponseValidator[] toValidate) {
        return ((response) -> {
            for (ResponseValidator val : toValidate) {
                val.validate(response);
            }
        });
    }
}
