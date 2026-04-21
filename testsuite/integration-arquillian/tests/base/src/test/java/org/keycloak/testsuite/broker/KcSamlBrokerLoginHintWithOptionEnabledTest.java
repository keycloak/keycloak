package org.keycloak.testsuite.broker;


import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KcSamlBrokerLoginHintWithOptionEnabledTest extends AbstractSamlLoginHintTest {

    // KEYCLOAK-13950
    @Test
    public void testPassLoginHintWithXmlCharShouldEncodeIt() {
        String username = "all-info-set@localhost.com";
        createUser(bc.providerRealmName(), username, "password", "FirstName");

        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());

        log.debug("Clicking social " + bc.getIDPAlias());
        String fishyLoginHint = "<an-xml-tag>";
        addLoginHintOnSocialButton(fishyLoginHint);
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"),
                "Driver should be on the provider realm page right now");
        log.debug("Logging in");

        assertEquals(loginPage.getUsername(), fishyLoginHint, "Username input should contain the SAML subject");
    }

    @Override
    boolean isLoginHintOptionEnabled() {
        return true;
    }

    @Override
    boolean isLoginQueryHintOptionEnabled() {
        return true;
    }
}
