package org.keycloak.testsuite.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class PageUtils {

    public static String getPageTitle(WebDriver driver) {
        return driver.findElement(By.id("kc-page-title")).getText();
    }

}
