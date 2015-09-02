package org.keycloak.testsuite.console.page.realm;

import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.keycloak.testsuite.console.page.fragment.Navigation;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class RealmSettings extends AdminConsoleRealm {

    @FindBy(xpath = "//div[@data-ng-controller='RealmTabCtrl']/ul")
    private RealmTabs realmTabs;

    public RealmTabs tabs() {
        return realmTabs;
    }

    public class RealmTabs extends Navigation {

        @FindBy(linkText = "General")
        private WebElement generalSettingsTab;
        @FindBy(linkText = "Login")
        private WebElement loginSettingsTab;
        @FindBy(linkText = "Keys")
        private WebElement keysSettingsTab;
        @FindBy(linkText = "Email")
        private WebElement emailSettingsTab;
        @FindBy(linkText = "Themes")
        private WebElement themeSettingsTab;
        @FindBy(linkText = "Cache")
        private WebElement cacheSettingsTab;
        @FindBy(linkText = "Tokens")
        private WebElement tokenSettingsTab;
        @FindBy(linkText = "Security Defenses")
        private WebElement defenseTab;

        public void general() {
            clickAndWaitForHeader(generalSettingsTab);
        }

        public void login() {
            clickAndWaitForHeader(loginSettingsTab);
        }

        public void keys() {
            clickAndWaitForHeader(keysSettingsTab);
        }

        public void email() {
            clickAndWaitForHeader(emailSettingsTab);
        }

        public void themes() {
            clickAndWaitForHeader(themeSettingsTab);
        }

        public void cache() {
            clickAndWaitForHeader(cacheSettingsTab);
        }

        public void tokens() {
            clickAndWaitForHeader(tokenSettingsTab);
        }

        public void securityDefenses() {
            clickAndWaitForHeader(defenseTab);
        }

    }
}
