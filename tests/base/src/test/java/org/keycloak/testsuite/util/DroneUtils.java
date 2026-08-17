package org.keycloak.testsuite.util;

import org.openqa.selenium.WebDriver;

public final class DroneUtils {

    private static final ThreadLocal<WebDriver> CURRENT_DRIVER = new ThreadLocal<>();

    private DroneUtils() {
    }

    public static void setCurrentDriver(WebDriver driver) {
        CURRENT_DRIVER.set(driver);
    }

    public static WebDriver getCurrentDriver() {
        return CURRENT_DRIVER.get();
    }
}
