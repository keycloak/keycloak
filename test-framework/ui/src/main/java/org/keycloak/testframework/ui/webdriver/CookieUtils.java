package org.keycloak.testframework.ui.webdriver;

import java.util.Set;

import org.keycloak.cookie.CookieType;

import org.openqa.selenium.Cookie;

public class CookieUtils {

    private final ManagedWebDriver managed;

    CookieUtils(ManagedWebDriver managed) {
        this.managed = managed;
    }

    public void add(Cookie cookie) {
        managed.driver().manage().addCookie(cookie);
    }

    public Cookie get(CookieType cookieType) {
        return managed.driver().manage().getCookieNamed(cookieType.getName());
    }

    public Set<Cookie> getAll() {
        return managed.driver().manage().getCookies();
    }

    public Cookie get(String name) {
        return managed.driver().manage().getCookieNamed(name);
    }

    public void deleteAll() {
        managed.driver().manage().deleteAllCookies();
    }

}
