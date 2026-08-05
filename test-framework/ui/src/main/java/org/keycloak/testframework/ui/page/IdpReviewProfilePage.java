package org.keycloak.testframework.ui.page;

import java.time.Duration;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

public class IdpReviewProfilePage extends LoginUpdateProfilePage {

    public IdpReviewProfilePage(ManagedWebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return "login-idp-review-user-profile";
    }

    @Override
    public void assertCurrent() {
        // Broker flows involve multi-hop redirects (consumer -> provider -> consumer)
        // which can take longer than the default 5s timeout in CI
        driver.waiting().waitForPage(this, Duration.ofSeconds(10));
    }
}
