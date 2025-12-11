package org.keycloak.testsuite.forms;

import java.net.URISyntaxException;

import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.testsuite.AbstractChangeImportedUserPasswordsTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.WaitUtils;

import org.apache.http.client.utils.URIBuilder;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

// real browser needed as it needs the account console and keycloak-js
@IgnoreBrowserDriver(value={ChromeDriver.class, FirefoxDriver.class}, negate=true)
public class AccountConsoleTest extends AbstractChangeImportedUserPasswordsTest {

    @Page
    protected LoginPage loginPage;

    @Test
    public void redirectToLoginIfNotAuthenticated() {
        driver.navigate().to(getAccount());
        WaitUtils.waitForPageToLoad();
        loginPage.assertCurrent();
        Assert.assertTrue(driver.getCurrentUrl().contains("client_id=" + Constants.ACCOUNT_CONSOLE_CLIENT_ID));
    }

    @Test
    public void testScopesPresentInAuthorizationRequest() {
        String expectedScopes = "openid phone";
        String redirectLocation = getAccount(expectedScopes);

        // should render the account with the phone scope
        driver.navigate().to(redirectLocation);
        WaitUtils.waitForPageToLoad();
        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));
        Assert.assertTrue(driver.getPageSource().contains("\"scope\": \"" + expectedScopes + "\""));

        // should render the account with the address scope only
        expectedScopes = "openid address";
        redirectLocation = getAccount(expectedScopes);
        driver.navigate().to(redirectLocation);
        WaitUtils.waitForPageToLoad();
        Assert.assertTrue(driver.getPageSource().contains("\"scope\": \"" + expectedScopes + "\""));

        // should render the account with the phone and address scopes
        expectedScopes = "openid phone address";
        redirectLocation = getAccount(expectedScopes);
        driver.navigate().to(redirectLocation);
        WaitUtils.waitForPageToLoad();
        Assert.assertTrue(driver.getPageSource().contains("\"scope\": \"" + expectedScopes + "\""));

        // should keep previously requested scopes when not setting the scope parameter
        redirectLocation = getAccount();
        driver.navigate().to(redirectLocation);
        WaitUtils.waitForPageToLoad();
        Assert.assertTrue(driver.getPageSource().contains("\"scope\": \"" + expectedScopes + "\""));
    }

    private String getAccount() {
        return getAccount(null);
    }

    private String getAccount(String scope) {
        try {
            var uriBuilder = new URIBuilder(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/test/account");

            if (scope != null) {
                uriBuilder.setParameter(OIDCLoginProtocol.SCOPE_PARAM, scope);
            }

            return uriBuilder.build().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
