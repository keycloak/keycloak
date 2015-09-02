package org.keycloak.testsuite.console.page.authentication;

import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.keycloak.testsuite.console.page.fragment.Navigation;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author tkyjovsk
 * @author mhajas
 */
public class Authentication extends AdminConsoleRealm {

    @FindBy(xpath = "//h1[text()='Authentication']/..")
    private AuthenticationTabs authenticationTabs;

    public AuthenticationTabs tabs() {
        return authenticationTabs;
    }

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/authentication";
    }

    public class AuthenticationTabs extends Navigation {
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
