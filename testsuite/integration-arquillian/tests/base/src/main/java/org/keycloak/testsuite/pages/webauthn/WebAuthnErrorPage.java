package org.keycloak.testsuite.pages.webauthn;

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
    
    public void clickTryAgain() {
        tryAgainButton.click();
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
