package org.keycloak.testsuite.console.page.realm;

import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class RealmSettings extends AdminConsoleRealm {

    @FindBy(linkText = "General")
    private WebElement generalSettingsTab;
    @FindBy(linkText = "Login")
    private WebElement loginSettingsTab;
    @FindBy(linkText = "Keys")
    private WebElement keysSettingsTab;
    @FindBy(linkText = "Email")
    private WebElement eamilSettingsTab;
    @FindBy(linkText = "Themes")
    private WebElement themeSettingsTab;
    @FindBy(linkText = "Cache")
    private WebElement cacheSettingsTab;
    @FindBy(linkText = "Tokens")
    private WebElement tokenSettingsTab;
    @FindBy(linkText = "Security Defenses")
    private WebElement defenseTab;

}
