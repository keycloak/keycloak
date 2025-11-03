package org.keycloak.tests.webauthn.pages;

import org.junit.jupiter.api.Assertions;
import org.keycloak.testframework.ui.page.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;


/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class WebAuthnErrorPage extends AbstractPage {

    @FindBy(id = "kc-try-again")
    private WebElement tryAgainButton;

    // Available only with AIA
    @FindBy(id = "cancelWebAuthnAIA")
    private WebElement cancelRegistrationAIA;

    @FindBy(css = "div[class^='pf-v5-c-alert'], div[class^='alert-error']")
    private WebElement errorMessage;

    public WebAuthnErrorPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return "webauthn-error-page";
    }

    public void clickTryAgain() {
        waitForPage();
        tryAgainButton.click();
    }

    public void clickCancelRegistrationAIA() {
        try {
            waitForPage();
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

    public boolean isCurrent() {
        try {
            driver.findElement(By.id("kc-try-again"));
            driver.findElement(By.id("kc-error-credential-form"));
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

}
