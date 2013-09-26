package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.rule.WebResource;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginConfigTotpPage extends Page {

    @WebResource
    private WebDriver browser;

    @FindBy(id = "totpSecret")
    private WebElement totpSecret;

    @FindBy(id = "totp")
    private WebElement totpInput;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    public void configure(String totp) {
        totpInput.sendKeys(totp);
        submitButton.click();
    }

    public String getTotpSecret() {
        return totpSecret.getAttribute("value");
    }

    public boolean isCurrent() {
        return browser.getTitle().equals("Config TOTP");
    }

    public void open() {
        throw new UnsupportedOperationException();
    }

}
