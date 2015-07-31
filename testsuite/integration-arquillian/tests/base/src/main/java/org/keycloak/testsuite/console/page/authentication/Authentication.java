package org.keycloak.testsuite.console.page.authentication;

import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public abstract class Authentication extends AdminConsoleRealm {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/authentication";
    }

    @FindBy(linkText = "Flows")
    private WebElement flowsTab;
    @FindBy(linkText = "Required Actions")
    private WebElement requiredActionsTab;
    @FindBy(linkText = "Password Policy")
    private WebElement passwordPolicyTab;

    public void flows() {
        flowsTab.click();
    }

    public void requiredActions() {
        requiredActionsTab.click();
    }

    public void passwordPolicy() {
        passwordPolicyTab.click();
    }

}
