package org.keycloak.testsuite.pages;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.testsuite.util.DroneUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Login page with the list of authentication mechanisms, which are available to the user (Password, OTP, WebAuthn...)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SelectAuthenticatorPage extends LanguageComboboxAwarePage {

    // Corresponds to the PasswordForm
    public static final String PASSWORD = "Password";

    // Corresponds to the OTPFormAuthenticator
    public static final String AUTHENTICATOR_APPLICATION = "Authenticator Application";


    @FindBy(id = "authenticators-choice")
    private WebElement authenticatorsSelect;


    /**
     * Return list of names like for example [ "Password", "Authenticator Application", "Security Key" ]
     */
    public List<String> getAvailableLoginMethods() {
        List<WebElement> rows = getLoginMethodsRows();

        return rows.stream()
                .map(this::getLoginMethodNameFromRow)
                .collect(Collectors.toList());
    }


    /**
     *
     * Selects the chosen login method (For example "Password") by click on it.
     *
     * @param loginMethodName name as displayed. For example "Password" or "Authenticator Application"
     *
     */
    public void selectLoginMethod(String loginMethodName) {
        getLoginMethodRowByName(loginMethodName).click();
    }

    /**
     * Return help text corresponding to the named login method
     *
     * @param loginMethodName name as displayed. For example "Password" or "Authenticator Application"
     * @return
     */
    public String getLoginMethodHelpText(String loginMethodName) {
        return getLoginMethodRowByName(loginMethodName).findElement(By.className("list-group-item-text")).getText();
    }


    private List<WebElement> getLoginMethodsRows() {
        return driver.findElements(By.className("list-view-pf-main-info"));
    }

    private String getLoginMethodNameFromRow(WebElement loginMethodRow) {
        return loginMethodRow.findElement(By.className("list-group-item-heading")).getText();
    }

    private WebElement getLoginMethodRowByName(String loginMethodName) {
        return getLoginMethodsRows().stream()
                .filter(loginMethodRow -> loginMethodName.equals(getLoginMethodNameFromRow(loginMethodRow)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Login method '" + loginMethodName + "' not found in the available authentication mechanisms"));
    }


    @Override
    public boolean isCurrent() {
        // Check the title
        if (!DroneUtils.getCurrentDriver().getTitle().startsWith("Log in to ") && !DroneUtils.getCurrentDriver().getTitle().startsWith("Anmeldung bei ")) {
            return false;
        }

        // Check the authenticators-choice available
        try {
            driver.findElement(By.id("kc-select-credential-form"));
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
