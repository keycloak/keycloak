package org.keycloak.testsuite.console.page.authentication;

import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import static org.keycloak.testsuite.util.WaitUtils.waitAjaxForElement;

/**
 * @author tkyjovsk
 * @author mhajas
 */
public class Authentication extends AdminConsoleRealm {

    @FindBy(xpath = "//h1[text()='Authentication']/..")
    private AuthenticationTabs authenticationTabs;

    @FindBy(xpath = "//div[contains(@class, 'alert-danger')]")
    private WebElement error;
    
    @FindBy(xpath = "//div[contains(@class, 'alert-success')]")
    private WebElement success;
    
    public String getSuccessMessage() {
        waitAjaxForElement(success);
        return success.getText();
    }
    
    public String getErrorMessage() {
        waitAjaxForElement(error);
        return error.getText();
    }
    
    public AuthenticationTabs tabs() {
        return authenticationTabs;
    }

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/authentication";
    }

    public class AuthenticationTabs {
        @FindBy(linkText = "Flows")
        private WebElement flowsTab;
        @FindBy(linkText = "Required Actions")
        private WebElement requiredActionsTab;
        @FindBy(linkText = "Password Policy")
        private WebElement passwordPolicyTab;
        @FindBy(linkText = "Bindings")
        private WebElement binding;
        @FindBy(linkText = "OTP Policy")
        private WebElement otpPolicy;

        public void flows() {
            flowsTab.click();
        }

        public void requiredActions() {
            requiredActionsTab.click();
        }

        public void passwordPolicy() {
            passwordPolicyTab.click();
        }

        public void binding() {
            binding.click();
        }

        public void otpPolicy() {
            otpPolicy.click();
        }
    }
}
