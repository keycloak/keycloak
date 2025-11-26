package org.keycloak.testframework.ui.webdriver;

import org.junit.jupiter.api.Assertions;

public class AssertionUtils {

    private final ManagedWebDriver managed;

    AssertionUtils(ManagedWebDriver managed) {
        this.managed = managed;
    }

    public void assertTitle(String title) {
        Assertions.assertEquals(title, managed.page().getTitle());
    }

}
