/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.keycloak.testsuite.ui.page.settings;

import org.keycloak.testsuite.ui.page.AbstractPage;
import org.keycloak.testsuite.ui.model.Theme;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author pmensik
 */

public class GeneralSettingsPage extends AbstractPage {

    @FindBy(id = "name")
    private WebElement realmName;
    
    @FindBy(id = "enabled")
    private WebElement realmEnabled;
    
    @FindBy(id = "updateProfileOnInitialSocialLogin")
    private WebElement updateProfileOnInitialSocialLogin;
    
    @FindBy(id = "passwordCredentialGrantAllowed")
    private WebElement passwordCredentialGrantAllowed;
    
    @FindBy(id = "loginTheme")
    private Select loginThemeSelect;
    
    @FindBy(id = "accountTheme")
    private Select accountThemeSelect;
    
    @FindBy(id = "adminTheme")
    private Select adminThemeSelect;
    
    @FindBy(id = "emailTheme")
    private Select emailThemeSelect;
    
    @FindBy(className = "btn btn-primary btn-lg")
    private WebElement saveButton;
    
    public void saveSettings() {
        saveButton.click();
    }
    
    public void selectLoginTheme(Theme theme) {
        loginThemeSelect.selectByVisibleText(theme.getName());
    }
    
    public void selecAccountTheme(Theme theme) {
        accountThemeSelect.selectByVisibleText(theme.getName());
    }
    
    public void selectAdminTheme(Theme theme) {
        adminThemeSelect.selectByVisibleText(theme.getName());
    }
    
    public void selectEmailTheme(Theme theme) {
        emailThemeSelect.selectByVisibleText(theme.getName());
    }
}
