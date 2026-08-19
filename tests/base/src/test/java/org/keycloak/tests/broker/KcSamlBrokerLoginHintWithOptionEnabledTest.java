package org.keycloak.tests.broker;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
public class KcSamlBrokerLoginHintWithOptionEnabledTest extends AbstractSamlLoginHintTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    // KEYCLOAK-13950
    @Test
    public void testPassLoginHintWithXmlCharShouldEncodeIt() {
        String username = "all-info-set@localhost.com";
        createUser(bc.providerRealmName(), username, "password", "FirstName");

        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

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
}
