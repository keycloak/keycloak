package org.keycloak.testsuite.ui.page.settings;

import org.keycloak.testsuite.ui.page.AbstractPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitGuiForElement;
import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitGuiForElementNotPresent;
import org.openqa.selenium.By;

/**
 * Created by fkiss.
 */
public class ThemesSettingsPage extends AbstractPage {

    @FindBy(css = "#loginTheme")
    private Select loginThemeSelect;

    @FindBy(css = "#accountTheme")
    private Select accountThemeSelect;

    @FindBy(css = "#adminTheme")
    private Select adminConsoleThemeSelect;

    @FindBy(css = "#emailTheme")
    private Select emailThemeSelect;

    @FindBy(css = "link[href*='/auth/theme/login/keycloak/']")
    private WebElement keycloakThemeLink;

    @FindBy(css = "link[href*='/auth/theme/login/patternfly/']")
    private WebElement patternFlyThemeLink;

    public void changeLoginTheme(String themeName){
		waitGuiForElement(By.id("loginTheme"));
        loginThemeSelect.selectByVisibleText(themeName);
    }

    public void changeAccountTheme(String themeName){
        accountThemeSelect.selectByVisibleText(themeName);
    }

    public void changeAdminConsoleTheme(String themeName){
        adminConsoleThemeSelect.selectByVisibleText(themeName);
    }

    public void changeEmailTheme(String themeName){
        emailThemeSelect.selectByVisibleText(themeName);
    }

    public void verifyBaseTheme(){
        waitGuiForElementNotPresent(patternFlyThemeLink);
        waitGuiForElementNotPresent(keycloakThemeLink);
    }

    public void verifyKeycloakTheme(){
        waitGuiForElement(keycloakThemeLink);
        waitGuiForElementNotPresent(patternFlyThemeLink);
    }

    public void verifyPatternflyTheme(){
        waitGuiForElement(patternFlyThemeLink);
        waitGuiForElementNotPresent(keycloakThemeLink);
    }

    public void saveTheme() {
        primaryButton.click();
    }

}
