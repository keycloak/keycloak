package org.keycloak.testframework.ui.page;

import java.util.List;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Login page with the list of authentication mechanisms, which are available to the user (Password, OTP, WebAuthn...)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:pzaoral@redhat.com">Peter Zaoral</a>
 */
public class SelectAuthenticatorPage extends AbstractLoginPage {

    // Corresponds to the PasswordForm
    public static final String PASSWORD = "Password";

    // Corresponds to the WebAuthn authenticators
    public static final String SECURITY_KEY = "Passkey";

    public SelectAuthenticatorPage(ManagedWebDriver driver) {
        super(driver);
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
        return getLoginMethodRowByName(loginMethodName).findElement(By.className("select-auth-box-desc")).getText();
    }


    private List<WebElement> getLoginMethodsRows() {
        return driver.driver().findElements(By.className("select-auth-box-parent"));
    }

    private String getLoginMethodNameFromRow(WebElement loginMethodRow) {
        return loginMethodRow.findElement(By.className("select-auth-box-headline")).getText();
    }

    private WebElement getLoginMethodRowByName(String loginMethodName) {
        return getLoginMethodsRows().stream()
                .filter(loginMethodRow -> loginMethodName.equals(getLoginMethodNameFromRow(loginMethodRow)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Login method '" + loginMethodName + "' not found in the available authentication mechanisms"));
    }

    @Override
    public String getExpectedPageId() {
        return "login-select-authenticator";
    }
}
