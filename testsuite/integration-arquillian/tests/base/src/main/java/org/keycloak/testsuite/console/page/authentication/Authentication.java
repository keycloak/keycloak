package org.keycloak.testsuite.console.page.authentication;

import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class Authentication extends AdminConsoleRealm {

    @Override
    public String getFragment() {
        return super.getFragment() + "/authentication";
    }

    @FindBy(linkText = "Flows")
    private WebElement flowsTab;
    @FindBy(linkText = "Required Actions")
    private WebElement requiredActionsTab;
    @FindBy(linkText = "Password Policy")
    private WebElement passwordPolicyTab;

    public void clickFlows() {
        flowsTab.click();
    }

    public void clickRequiredActions() {
        requiredActionsTab.click();
    }

    public void clickPasswordPolicy() {
        passwordPolicyTab.click();
    }

}
