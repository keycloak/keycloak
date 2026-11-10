package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.By;

public class SelectOrganizationPage extends AbstractLoginPage {

    public SelectOrganizationPage(ManagedWebDriver driver) {
        super(driver);
    }

    public void selectOrganization(String alias) {
        driver.findElement(By.id("organization-" + alias)).click();
    }

    public boolean isOrganizationButtonPresent(String alias) {
        return !driver.driver().findElements(By.id("organization-" + alias)).isEmpty();
    }

    @Override
    public String getExpectedPageId() {
        return "login-select-organization";
    }
}
