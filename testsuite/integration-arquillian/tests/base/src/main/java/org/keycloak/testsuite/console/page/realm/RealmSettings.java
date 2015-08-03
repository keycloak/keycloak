package org.keycloak.testsuite.console.page.realm;

import org.keycloak.testsuite.console.page.AdminConsoleRealm;
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

    public class RealmTabs {

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
            generalSettingsTab.click();
        }

        public void login() {
            loginSettingsTab.click();
        }

        public void keys() {
            keysSettingsTab.click();
        }

        public void email() {
            emailSettingsTab.click();
        }

        public void themes() {
            themeSettingsTab.click();
        }

        public void cache() {
            cacheSettingsTab.click();
        }

        public void tokens() {
            tokenSettingsTab.click();
        }

        public void securityDefenses() {
            defenseTab.click();
        }

    }
}
