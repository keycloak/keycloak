package org.keycloak.testsuite.console.page.clients.credentials;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:yoshiyuki.tabata.jy@hitachi.com">Yoshiyuki Tabata</a>
 */
public class OIDCClientCredentialsForm extends Form {

    @FindBy(linkText = "Keys")
    private WebElement oidcKeysLink;

    @FindBy(xpath = "//button[@data-ng-click='generateSigningKey()']")
    private WebElement generateNewKeysAndCert; // Generate new keys and certificate

    public void generateNewKeysAndCert() {
        navigateToKeysTab();
        waitUntilElement(generateNewKeysAndCert).is().visible();
        generateNewKeysAndCert.click();
        waitForPageToLoad();
    }

    private void navigateToKeysTab() {
        clickLink(oidcKeysLink);
    }
}
