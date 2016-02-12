package org.keycloak.test.stress.tests;

import org.junit.Assert;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.test.stress.Test;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.core.UriBuilder;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LoginLogout implements Test {
    public WebRule webRule = new WebRule(this);

    protected String securedResourceUrl;
    protected List<String> containsInPage = new LinkedList<>();
    protected String realm;
    protected String authServerUrl;
    protected String username;
    protected String password;

    protected String loginUrl;
    protected String logoutUrl;

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected LoginPage loginPage;

    public LoginLogout securedResourceUrl(String securedResourceUrl) {
        this.securedResourceUrl = securedResourceUrl;
        return this;
    }

    public LoginLogout addPageContains(String contains) {
        containsInPage.add(contains);
        return this;
    }

    public LoginLogout realm(String realm) {
        this.realm = realm;
        return this;
    }

    public LoginLogout authServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
        return this;
    }

    public LoginLogout username(String username) {
        this.username = username;
        return this;
    }

    public LoginLogout password(String password) {
        this.password = password;
        return this;
    }

    @Override
    public void init() {

        loginUrl = OIDCLoginProtocolService.authUrl(UriBuilder.fromUri(authServerUrl)).build(realm).toString();
        logoutUrl = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(authServerUrl))
                .queryParam(OAuth2Constants.REDIRECT_URI, securedResourceUrl).build(realm).toString();
        try {
            webRule.before();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Boolean call() throws Exception {
        driver.navigate().to(securedResourceUrl);
        Assert.assertTrue(driver.getCurrentUrl().startsWith(loginUrl));
        loginPage.login(username, password);
        Assert.assertTrue(driver.getCurrentUrl().startsWith(securedResourceUrl));
        String pageSource = driver.getPageSource();
        for (String contains : containsInPage) {
            Assert.assertTrue(pageSource.contains(contains));

        }

        // test logout
        driver.navigate().to(logoutUrl);
        Assert.assertTrue(driver.getCurrentUrl().startsWith(loginUrl));
        return true;
    }
}
