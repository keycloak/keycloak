package org.keycloak.testsuite.broker;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.testsuite.Assert;

import org.junit.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.junit.Assert.assertEquals;

/**
 * Test of various scenarios related to the use of login hint
 */
public abstract class AbstractSamlLoginHintTest extends AbstractInitializedBaseBrokerTest {

    // KEYCLOAK-13950
    @Test
    public void testPassLoginHintShouldSendSubjectAndPrefillUsername() {
        String username = "all-info-set@localhost.com";
        createUser(bc.providerRealmName(), username, "password");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        log.debug("Clicking social " + bc.getIDPAlias());
        addLoginHintOnSocialButton(username);
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        log.debug("Logging in");

        if (isLoginHintOptionEnabled()) {
            assertEquals("Username input should contain the SAML subject", loginPage.getUsername(), username);
        } else {
            assertEquals("Username input should the SAML subject", loginPage.getUsername(), "");
        }
    }

    // KEYCLOAK-13950
    @Test
    public void testPassEmptyLoginHintShouldNotSendSubjectAndShouldNotPrefillUsername() {
        String username = "all-info-set@localhost.com";
        createUser(bc.providerRealmName(), username, "password", "FirstName");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        log.debug("Clicking social " + bc.getIDPAlias());
        addLoginHintOnSocialButton("");
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        log.debug("Logging in");

        assertEquals("Username input should not contain any username", loginPage.getUsername(), "");
    }

    @Test
    public void testLoginHintForwardedAsQueryParam() {
        String username = "all-info-set@localhost.com";
        String urlEncodedUsername = "all-info-set%40localhost.com";
        createUser(bc.providerRealmName(), username, "password", "FirstName");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        log.debug("Clicking social " + bc.getIDPAlias());
        addLoginHintOnSocialButton(username);
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        String currentUrl = driver.getCurrentUrl();
        if (!isLoginHintOptionEnabled()) {
            // With the option disabled, SAMLIdentityProvider must NOT append login_hint to the IdP destination URL.
            // We check that the provider page URL does not contain the parameter. This is binding-agnostic.
            Assert.assertTrue("Provider page should not contain login_hint parameter",
                    !currentUrl.contains("login_hint="));
        } else {
            // Positive verification: when the option is enabled, the IdP destination URL must include login_hint
            String expected = "login_hint=" + java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8);
            Assert.assertTrue("Provider page should contain login_hint parameter in query: expected to find '" + expected + "' in URL: " + currentUrl,
                    currentUrl.contains(expected));
        }
    }

    abstract boolean isLoginHintOptionEnabled();

    abstract boolean isLoginQueryHintOptionEnabled();

    protected void addLoginHintOnSocialButton(String hint) {
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        WebElement button = loginPage.findSocialButton(bc.getIDPAlias());
        String encodedHint = java.net.URLEncoder.encode(hint, java.nio.charset.StandardCharsets.UTF_8);
        String url = button.getAttribute("href") + "&" + OIDCLoginProtocol.LOGIN_HINT_PARAM + "=" + encodedHint;
        executor.executeScript("arguments[0].setAttribute('href', arguments[1]);", button, url);
    }

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcSamlBrokerConfiguration(isLoginHintOptionEnabled(), isLoginQueryHintOptionEnabled());
    }
}
