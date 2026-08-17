package org.keycloak.testsuite.cluster;

import org.keycloak.cookie.CookieType;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

public final class AuthenticationSessionFailoverClusterTest {

    private AuthenticationSessionFailoverClusterTest() {
    }

    public static String getAuthSessionCookieValue(ManagedWebDriver driver) {
        return getAuthSessionCookieValue(driver.driver());
    }

    public static String getAuthSessionCookieValue(WebDriver driver) {
        Cookie cookie = driver.manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName());
        return cookie != null ? cookie.getValue() : null;
    }
}
