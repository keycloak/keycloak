package org.keycloak.testsuite.cluster;

import org.keycloak.cookie.CookieType;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class ClusterTestUtils {

    private ClusterTestUtils() {
    }

    public static String getAuthSessionCookieValue(WebDriver driver) {
        Cookie authSessionCookie = driver.manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName());
        assertNotNull(authSessionCookie);
        return authSessionCookie.getValue();
    }
}
