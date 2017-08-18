package org.keycloak.testsuite.console.page.realm;

import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.WaitUtils.*;

/**
 *
 * @author tkyjovsk
 */
public class RealmSettings extends AdminConsoleRealm {
    private static final String navTabsClassName = "nav-tabs";


    @FindBy(className = navTabsClassName)
    private RealmTabs realmTabs;

    public RealmTabs tabs() {
        waitUntilElement(By.className(navTabsClassName)).is().present();
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
            waitUntilElement(generalSettingsTab).is().present();
            generalSettingsTab.click();
        }

        public void login() {
            waitUntilElement(loginSettingsTab).is().present();
            loginSettingsTab.click();
        }

        public void keys() {
            waitUntilElement(keysSettingsTab).is().present();
            keysSettingsTab.click();
        }

        public void email() {
            waitUntilElement(emailSettingsTab).is().present();
            emailSettingsTab.click();
        }

        public void themes() {
            waitUntilElement(themeSettingsTab).is().present();
            themeSettingsTab.click();
        }

        public void cache() {
            waitUntilElement(cacheSettingsTab).is().present();
            cacheSettingsTab.click();
        }

        public void tokens() {
            waitUntilElement(tokenSettingsTab).is().present();
            tokenSettingsTab.click();
        }

        public void securityDefenses() {
            waitUntilElement(defenseTab).is().present();
            defenseTab.click();
        }

    }
}
