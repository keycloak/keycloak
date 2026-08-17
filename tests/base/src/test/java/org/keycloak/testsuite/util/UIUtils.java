package org.keycloak.testsuite.util;

import org.openqa.selenium.WebElement;

public final class UIUtils {

    private UIUtils() {
    }

    public static void click(WebElement element) {
        element.click();
    }

    public static void clickLink(WebElement element) {
        element.click();
    }
}
