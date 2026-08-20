package org.keycloak.testsuite.auth.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class AccountFields {

    private final ManagedWebDriver driver;

    public AccountFields(ManagedWebDriver driver) {
        this.driver = driver;
    }

    public AccountFields setEmail(String email) {
        set("email", email);
        return this;
    }

    public AccountFields setFirstName(String firstName) {
        set("firstName", firstName);
        return this;
    }

    public AccountFields setLastName(String lastName) {
        set("lastName", lastName);
        return this;
    }

    public String getEmail() {
        return get("email");
    }

    public String getFirstName() {
        return get("firstName");
    }

    public String getLastName() {
        return get("lastName");
    }

    private void set(String id, String value) {
        WebElement element = driver.findElement(By.id(id));
        element.clear();
        element.sendKeys(value);
    }

    private String get(String id) {
        return driver.findElement(By.id(id)).getAttribute("value");
    }
}
