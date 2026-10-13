package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginExpiredPage extends AbstractLoginPage {

    public LoginExpiredPage(ManagedWebDriver driver) {
        super(driver);
    }

    @FindBy(id = "loginRestartLink")
    private WebElement loginRestartLink;

    @FindBy(id = "loginContinueLink")
    private WebElement loginContinueLink;


    public void clickLoginRestartLink() {
        loginRestartLink.click();
    }

    public void clickLoginContinueLink() {
        loginContinueLink.click();
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-page-expired";
    }
}
