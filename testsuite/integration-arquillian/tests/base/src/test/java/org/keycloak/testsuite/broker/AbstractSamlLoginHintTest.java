package org.keycloak.testsuite.broker;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of various scenarios related to the use of login hint
 */
public abstract class AbstractSamlLoginHintTest extends AbstractInitializedBaseBrokerTest {

    // KEYCLOAK-13950
    @Test
    public void testPassLoginHintShouldSendSubjectAndPrefillUsername() {
        assertLoginHintSubject(isLoginHintOptionEnabled());
    }

    // KEYCLOAK-13950
    @Test
    public void testPassEmptyLoginHintShouldNotSendSubjectAndShouldNotPrefillUsername() {
        String username = "all-info-set@localhost.com";
        createUser(bc.providerRealmName(), username, "password", "FirstName");

        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());

        log.debug("Clicking social " + bc.getIDPAlias());
        addLoginHintOnSocialButton("");
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"),
                "Driver should be on the provider realm page right now");
        log.debug("Logging in");

        assertEquals(loginPage.getUsername(), "", "Username input should not contain any username");
    }

    @Test
    public void testLoginHintForwardedAsQueryParam() {
        assertLoginHintQueryParam(isLoginQueryHintOptionEnabled());
    }

    abstract boolean isLoginHintOptionEnabled();

    abstract boolean isLoginQueryHintOptionEnabled();

    protected void updateLoginHintOptions(boolean loginHint, boolean loginQueryHint) {
        IdentityProviderRepresentation idp = identityProviderResource.toRepresentation();
        idp.getConfig().put(IdentityProviderModel.LOGIN_HINT, String.valueOf(loginHint));
        idp.getConfig().put(SAMLIdentityProviderConfig.LOGIN_QUERY_HINT, String.valueOf(loginQueryHint));
        identityProviderResource.update(idp);
    }

    protected void assertLoginHintSubject(boolean loginHintOptionEnabled) {
        String username = "all-info-set@localhost.com";
        createUser(bc.providerRealmName(), username, "password");

        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());

        log.debug("Clicking social " + bc.getIDPAlias());
        addLoginHintOnSocialButton(username);
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"),
                "Driver should be on the provider realm page right now");
        log.debug("Logging in");

        if (loginHintOptionEnabled) {
            assertEquals(loginPage.getUsername(), username, "Username input should contain the SAML subject");
        } else {
            assertEquals(loginPage.getUsername(), "", "Username input should the SAML subject");
        }
    }

    protected void assertLoginHintQueryParam(boolean loginQueryHintOptionEnabled) {
        String username = "all-info-set@localhost.com";
        createUser(bc.providerRealmName(), username, "password", "FirstName");

        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());

        log.debug("Clicking social " + bc.getIDPAlias());
        addLoginHintOnSocialButton(username);
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"),
                "Driver should be on the provider realm page right now");

        String currentUrl = driver.getCurrentUrl();
        if (!loginQueryHintOptionEnabled) {
            // With the option disabled, SAMLIdentityProvider must NOT append login_hint to the IdP destination URL.
            // We check that the provider page URL does not contain the parameter. This is binding-agnostic.
            Assertions.assertFalse(currentUrl.contains("login_hint="),
                    "Provider page should not contain login_hint parameter");
        } else {
            // Positive verification: when the option is enabled, the IdP destination URL must include login_hint
            String expected = "login_hint=" + java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8);
            Assertions.assertTrue(currentUrl.contains(expected),
                    "Provider page should contain login_hint parameter in query: expected to find '" + expected + "' in URL: " + currentUrl);
        }
    }

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
