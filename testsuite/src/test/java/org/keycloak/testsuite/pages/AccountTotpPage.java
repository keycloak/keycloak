package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.Constants;
import org.keycloak.testsuite.rule.WebResource;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class AccountTotpPage extends Page {

    private static String PATH = Constants.AUTH_SERVER_ROOT + "/rest/realms/test/account/totp";

    @WebResource
    private WebDriver browser;

    @FindBy(id = "totpSecret")
    private WebElement totpSecret;

    @FindBy(id = "totp")
    private WebElement totpInput;

    @FindBy(css = "button[type=\"submit\"]")
    private WebElement submitButton;

    public void configure(String totp) {
        totpInput.sendKeys(totp);
        submitButton.click();
    }

    public String getTotpSecret() {
        return totpSecret.getAttribute("value");
    }

    public boolean isCurrent() {
        return browser.getPageSource().contains("Google Authenticator Setup");
    }

    public void open() {
        browser.navigate().to(PATH);
    }

}
