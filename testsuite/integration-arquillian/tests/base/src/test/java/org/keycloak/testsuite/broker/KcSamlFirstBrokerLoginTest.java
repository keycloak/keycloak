package org.keycloak.testsuite.broker;

import org.junit.Test;
import org.keycloak.testsuite.Assert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KcSamlFirstBrokerLoginTest extends AbstractFirstBrokerLoginTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }


    @Test
    @Override
    public void testUpdateProfileIfNotMissingInformation() {
        // skip this test as this provider do not return name and surname so something is missing always
    }


    // KEYCLOAK-13950
    @Test
    public void testPassLoginHintShouldSendSubjectAndPrefillUsername() {
        String username = "all-info-set@localhost.com";
        createUser(bc.providerRealmName(), username, "password");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        log.debug("Clicking social " + bc.getIDPAlias());
        addLoginHintOnSocialButton(username);
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "log in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        log.debug("Logging in");

        assertEquals("Username input should contain the SAML subject", loginPage.getUsername(), username);
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
        waitForPage(driver, "log in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        log.debug("Logging in");

        assertEquals("Username input should not contain any username", loginPage.getUsername(), "");
    }

    // KEYCLOAK-13950
    @Test
    public void testPassLoginHintWithXmlCharShouldEncodeIt() {
        String username = "all-info-set@localhost.com";
        createUser(bc.providerRealmName(), username, "password", "FirstName");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        log.debug("Clicking social " + bc.getIDPAlias());
        String fishyLoginHint = "<an-xml-tag>";
        addLoginHintOnSocialButton(fishyLoginHint);
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "log in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        log.debug("Logging in");

        assertEquals("Username input should contain the SAML subject", loginPage.getUsername(), fishyLoginHint);
    }

    private void addLoginHintOnSocialButton(String hint) {
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        WebElement button = loginPage.findSocialButton(bc.getIDPAlias());
        String url = button.getAttribute("href") + "&login_hint="+hint;
        executor.executeScript("document.getElementById('"+button.getAttribute("id")+"').setAttribute('href', '"+url+"')");
    }
}
