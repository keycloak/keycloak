package org.keycloak.testframework.ui.webdriver;

import org.openqa.selenium.Cookie;

public class CookieUtils {

    private final ManagedWebDriver managed;

    CookieUtils(ManagedWebDriver managed) {
        this.managed = managed;
    }

    public void add(Cookie cookie) {
        managed.driver().manage().addCookie(cookie);
    }

    public void deleteAll() {
        managed.driver().manage().deleteAllCookies();
    }

}
