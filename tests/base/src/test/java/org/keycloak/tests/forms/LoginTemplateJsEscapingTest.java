package org.keycloak.tests.forms;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;

@KeycloakIntegrationTest
public class LoginTemplateJsEscapingTest {

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @Test
    public void loginPageImportPathsUseDoubleQuotedStrings() {
        oauth.openLoginForm();
        String loginPageUrl = driver.getCurrentUrl();

        String pageSource = (String) ((JavascriptExecutor) driver.driver()).executeScript(
                "var xhr = new XMLHttpRequest();" +
                "xhr.open('GET', arguments[0], false);" +
                "xhr.send();" +
                "return xhr.responseText;", loginPageUrl);

        Assertions.assertTrue(pageSource.contains("import { startSessionPolling } from \""),
                "startSessionPolling import should use a double-quoted path");
        Assertions.assertTrue(pageSource.contains("import { checkAuthSession } from \""),
                "checkAuthSession import should use a double-quoted path");
        Assertions.assertTrue(pageSource.contains("/js/authChecker.js\";"),
                "authChecker.js import path should end with a closing double quote");
        Assertions.assertTrue(pageSource.contains("const DARK_MODE_CLASS = \""),
                "DARK_MODE_CLASS should be assigned a double-quoted string via ?c");
    }
}
