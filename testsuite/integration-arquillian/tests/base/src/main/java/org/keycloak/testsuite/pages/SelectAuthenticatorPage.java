package org.keycloak.testsuite.pages;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.testsuite.util.DroneUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * Login page with the list of authentication mechanisms, which are available to the user (Password, OTP, WebAuthn...)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SelectAuthenticatorPage extends LanguageComboboxAwarePage {

    @FindBy(id = "authenticators-choice")
    private WebElement authenticatorsSelect;


    public List<String> getAvailableLoginMethods() {
        return new Select(authenticatorsSelect).getOptions()
                .stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }


    public String getSelectedLoginMethod() {
        return new Select(authenticatorsSelect).getOptions()
                .stream()
                .filter(webElement -> webElement.getAttribute("selected") != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Selected login method not found"))
                .getText();
    }


    public void selectLoginMethod(String loginMethod) {
        new Select(authenticatorsSelect).selectByVisibleText(loginMethod);
    }


    @Override
    public boolean isCurrent() {
        // Check the title
        if (!DroneUtils.getCurrentDriver().getTitle().startsWith("Log in to ") && !DroneUtils.getCurrentDriver().getTitle().startsWith("Anmeldung bei ")) {
            return false;
        }

        // Check the authenticators-choice available
        try {
            driver.findElement(By.id("authenticators-choice"));
        } catch (NoSuchElementException nfe) {
            return false;
        }

        return true;
    }


    @Override
    public void open() throws Exception {
        throw new UnsupportedOperationException();
    }


}
