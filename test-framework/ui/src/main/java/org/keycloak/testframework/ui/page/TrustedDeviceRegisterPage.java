package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class TrustedDeviceRegisterPage extends AbstractLoginPage {

    @FindBy(id = "kc-trusted-device-yes")
    private WebElement trustButton;

    @FindBy(id = "kc-trusted-device-no")
    private WebElement rejectButton;

    @FindBy(id = "kc-trusted-device-name")
    private WebElement deviceName;

    public TrustedDeviceRegisterPage(ManagedWebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return "login-trusted-device-register";
    }

    public void confirmDevice() {
        trustButton.click();
    }

    public void rejectDevice() {
        rejectButton.click();
    }
}
