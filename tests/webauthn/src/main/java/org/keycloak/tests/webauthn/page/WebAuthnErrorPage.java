package org.keycloak.tests.webauthn.page;

import org.keycloak.testframework.ui.page.AbstractLoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;


/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class WebAuthnErrorPage extends AbstractLoginPage {

    @FindBy(id = "kc-try-again")
    private WebElement tryAgainButton;

    // Available only with AIA
    @FindBy(id = "cancelWebAuthnAIA")
    private WebElement cancelRegistrationAIA;

    @FindBy(css = "div[class^='pf-v5-c-alert'], div[class^='alert-error']")
    private WebElement errorMessage;

    public WebAuthnErrorPage(ManagedWebDriver driver) {
        super(driver);
    }

    public void clickTryAgain() {
        tryAgainButton.click();
    }

    public void clickCancelRegistrationAIA() {
        try {
            cancelRegistrationAIA.click();
        } catch (NoSuchElementException e) {
            Assertions.fail("It only works with AIA");
        }
    }

    public String getError() {
        try {
            return errorMessage.getText();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    @Override
    public String getExpectedPageId() {
        return "login-webauthn-error";
    }
}
