package org.keycloak.testsuite.broker;

import org.junit.Test;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.testsuite.Assert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

/**
 * Test of various scenarios related to the use of login hint
 */
public abstract class AbstractSamlLoginHintTest extends AbstractInitializedBaseBrokerTest {

    // KEYCLOAK-13950
    @Test
    public void testPassLoginHintShouldSendSubjectAndPrefillUsername() {
        String username = "all-info-set@localhost.com";
        createUser(bc.providerRealmName(), username, "password");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        log.debug("Clicking social " + bc.getIDPAlias());
        addLoginHintOnSocialButton("");
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        log.debug("Logging in");

        assertEquals("Username input should not contain any username", loginPage.getUsername(), "");
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
