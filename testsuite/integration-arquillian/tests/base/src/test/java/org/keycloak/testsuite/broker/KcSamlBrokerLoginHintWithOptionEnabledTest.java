package org.keycloak.testsuite.broker;

import org.junit.Test;
import org.keycloak.testsuite.Assert;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

public class KcSamlBrokerLoginHintWithOptionEnabledTest extends AbstractSamlLoginHintTest {


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
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        log.debug("Logging in");

        assertEquals("Username input should contain the SAML subject", loginPage.getUsername(), fishyLoginHint);
    }

    @Override
    boolean isLoginHintOptionEnabled() {
        return true;
    }
}
