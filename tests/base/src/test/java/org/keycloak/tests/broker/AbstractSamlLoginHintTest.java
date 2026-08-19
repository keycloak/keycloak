package org.keycloak.tests.broker;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of various scenarios related to the use of login hint
 */
@KeycloakIntegrationTest
public abstract class AbstractSamlLoginHintTest extends AbstractInitializedBaseBrokerTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    // KEYCLOAK-13950
    @Test
    public void testPassLoginHintShouldSendSubjectAndPrefillUsername() {
        String username = "all-info-set@localhost.com";
        createUser(bc.providerRealmName(), username, "password");

        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        log.debug("Clicking social " + bc.getIDPAlias());
        addLoginHintOnSocialButton(username);
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"),
                "Driver should be on the provider realm page right now");
        log.debug("Logging in");

        if (isLoginHintOptionEnabled()) {
            assertEquals(loginPage.getUsername(), username, "Username input should contain the SAML subject");
        } else {
            assertEquals(loginPage.getUsername(), "", "Username input should the SAML subject");
        }
    }

    // KEYCLOAK-13950
    @Test
    public void testPassEmptyLoginHintShouldNotSendSubjectAndShouldNotPrefillUsername() {
        String username = "all-info-set@localhost.com";
        createUser(bc.providerRealmName(), username, "password", "FirstName");

        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        log.debug("Clicking social " + bc.getIDPAlias());
        addLoginHintOnSocialButton("");
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"),
                "Driver should be on the provider realm page right now");
        log.debug("Logging in");

        assertEquals(loginPage.getUsername(), "", "Username input should not contain any username");
    }

    abstract boolean isLoginHintOptionEnabled();

    protected void addLoginHintOnSocialButton(String hint) {
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        WebElement button = loginPage.findSocialButton(bc.getIDPAlias());
        String url = button.getAttribute("href") + "&"+ OIDCLoginProtocol.LOGIN_HINT_PARAM+"="+hint;
        executor.executeScript("document.getElementById('"+button.getAttribute("id")+"').setAttribute('href', '"+url+"')");
    }

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcSamlBrokerConfiguration(isLoginHintOptionEnabled());
    }
}
