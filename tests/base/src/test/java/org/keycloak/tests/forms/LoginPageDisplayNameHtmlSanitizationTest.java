package org.keycloak.tests.forms;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;

@KeycloakIntegrationTest
public class LoginPageDisplayNameHtmlSanitizationTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @Test
    public void decimalNumericCharacterReferenceJavascriptUrlIsNeutralized() {
        String page = renderLoginPageWithDisplayNameHtml("<a href=\"javascript&#58alert(xss53813)\">pwn</a>");
        assertNoExecutableScheme(page);
    }

    @Test
    public void hexNumericCharacterReferenceJavascriptUrlIsNeutralized() {
        String page = renderLoginPageWithDisplayNameHtml("<a href=\"javascript&#x3aprompt(xss53813)\">pwn</a>");
        assertNoExecutableScheme(page);
    }

    @Test
    public void legitimateDisplayNameHtmlIsPreserved() {
        String page = renderLoginPageWithDisplayNameHtml("<b>ACME53813</b>");
        Assertions.assertTrue(page.contains("<b>ACME53813</b>"), "benign display name markup must render");
    }

    @Test
    public void httpLinkIsPreserved() {
        String page = renderLoginPageWithDisplayNameHtml("<a href=\"http://acme53813.example.com/help?topic=sso&lang=en\">Help</a>");
        Assertions.assertTrue(page.contains("<a href=\"http://acme53813.example.com/help?topic=sso&lang=en\" rel=\"nofollow\">Help</a>"),
                "http link must be preserved");
    }

    @Test
    public void httpsLinkIsPreserved() {
        String page = renderLoginPageWithDisplayNameHtml("<a href=\"https://acme53813.example.com/help?topic=sso&lang=en\">Help</a>");
        Assertions.assertTrue(page.contains("<a href=\"https://acme53813.example.com/help?topic=sso&lang=en\" rel=\"nofollow\">Help</a>"),
                "https link must be preserved");
    }

    @Test
    public void mailtoLinkIsPreserved() {
        String page = renderLoginPageWithDisplayNameHtml("<a href=\"mailto:support@acme53813.example.com\">Mail</a>");
        Assertions.assertTrue(page.contains("<a href=\"mailto:support&#64;acme53813.example.com\" rel=\"nofollow\">Mail</a>"),
                "mailto link must be preserved");
    }

    private String renderLoginPageWithDisplayNameHtml(String displayNameHtml) {
        managedRealm.updateWithCleanup(r -> r.update(realm -> realm.setDisplayNameHtml(displayNameHtml)));
        oauth.openLoginForm();
        loginPage.assertCurrent();
        String loginPageUrl = driver.getCurrentUrl();
        return (String) ((JavascriptExecutor) driver.driver()).executeScript(
                "var xhr = new XMLHttpRequest();" +
                "xhr.open('GET', arguments[0], false);" +
                "xhr.send();" +
                "return xhr.responseText;", loginPageUrl);
    }

    private void assertNoExecutableScheme(String pageSource) {
        String lower = pageSource.toLowerCase();
        Assertions.assertFalse(lower.contains("javascript&#"),
                "login page must not emit a reactivated javascript URL");
        Assertions.assertFalse(lower.contains("javascript:alert") || lower.contains("javascript:prompt"),
                "login page must not emit a live javascript URL");
    }
}
