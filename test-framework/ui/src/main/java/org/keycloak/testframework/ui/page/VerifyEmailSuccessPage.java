package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class VerifyEmailSuccessPage extends AbstractLoginPage {

    @FindBy(className = "instruction")
    private WebElement instruction;

    public VerifyEmailSuccessPage(ManagedWebDriver driver) {
        super(driver);
    }

    public String getInstruction() {
        return instruction.getText();
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-verify-email-success";
    }
}
