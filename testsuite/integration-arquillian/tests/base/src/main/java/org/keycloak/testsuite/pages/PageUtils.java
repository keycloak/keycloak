package org.keycloak.testsuite.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;

@Deprecated(forRemoval = true)
public class PageUtils {

    public static String getPageTitle(WebDriver driver) {
        try {
            return driver.findElement(By.id("kc-page-title")).getText();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

}
