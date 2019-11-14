package org.keycloak.testsuite.pages;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * Login page with the list of credentials, which are available to the user (Password, OTP, WebAuthn...)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class CredentialsComboboxPage extends LanguageComboboxAwarePage {

    @FindBy(id = "authenticators-choice")
    private WebElement credentialsCombobox;


    // If false, we don't expect that credentials combobox is available. If true, we expect that it is available on the page
    public void assertCredentialsComboboxAvailability(boolean expectedAvailability) {
        try {
            driver.findElement(By.id("authenticators-choice"));
            Assert.assertTrue(expectedAvailability);
        } catch (NoSuchElementException nse) {
            Assert.assertFalse(expectedAvailability);
        }
    }


    public List<String> getAvailableCredentials() {
        return new Select(credentialsCombobox).getOptions()
                .stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }


    public String getSelectedCredential() {
        return new Select(credentialsCombobox).getOptions()
                .stream()
                .filter(webElement -> webElement.getAttribute("selected") != null)
                .findFirst()
                .orElseThrow(() -> {

                    return new AssertionError("Selected credential not found");

                })
                .getText();
    }


    public void selectCredential(String credentialName) {
        new Select(credentialsCombobox).selectByVisibleText(credentialName);
    }


}
