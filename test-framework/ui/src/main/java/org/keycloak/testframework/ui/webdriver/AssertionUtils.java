package org.keycloak.testframework.ui.webdriver;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;

public class AssertionUtils {

    private final ManagedWebDriver managed;

    AssertionUtils(ManagedWebDriver managed) {
        this.managed = managed;
    }

    public void assertTitle(String title) {
        String kcPageTitle = managed.findElement(By.id("kc-page-title")).getText();
        Assertions.assertEquals(title, kcPageTitle);
    }

}
