package org.keycloak.testsuite.pages.webauthn;

import org.junit.Assert;
import org.keycloak.testsuite.pages.LanguageComboboxAwarePage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class WebAuthnErrorPage extends LanguageComboboxAwarePage {

    @FindBy(id = "kc-try-again")
    private WebElement tryAgainButton;

    // Available only with AIA
    @FindBy(id = "cancelWebAuthnAIA")
    private WebElement cancelRegistrationAIA;

    public void clickTryAgain() {
        tryAgainButton.click();
    }

    public void clickCancelRegistrationAIA() {
        try {
            cancelRegistrationAIA.click();
        } catch (NoSuchElementException e) {
            Assert.fail("It only works with AIA");
        }
    }

    @Override
    public boolean isCurrent() {
        try {
            driver.findElement(By.id("kc-try-again"));
            driver.findElement(By.id("kc-error-credential-form"));
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException();
    }
}
