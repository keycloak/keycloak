package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.rule.WebResource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginTotpPage extends Page {

    @WebResource
    private WebDriver browser;

    @FindBy(id = "totp")
    private WebElement totpInput;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(id = "loginError")
    private WebElement loginErrorMessage;

    public void login(String totp) {
        totpInput.sendKeys(totp);

        submitButton.click();
    }

    public String getError() {
        return loginErrorMessage != null ? loginErrorMessage.getText() : null;
    }

    public boolean isCurrent() {
        if (browser.getTitle().equals("Log in to test")) {
            try {
                browser.findElement(By.id("totp"));
                return true;
            } catch (Throwable t) {
            }
        }
        return false;
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException();
    }

}
