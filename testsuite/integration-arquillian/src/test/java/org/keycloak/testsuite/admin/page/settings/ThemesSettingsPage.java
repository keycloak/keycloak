/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.admin.page.settings;

import org.keycloak.testsuite.admin.page.AbstractPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.admin.util.SeleniumUtils.waitGuiForElement;
import static org.keycloak.testsuite.admin.util.SeleniumUtils.waitGuiForElementNotPresent;
import org.openqa.selenium.By;

/**
 *
 * @author Filip Kiss
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

    @FindBy(css = "link[href*='login/keycloak/css/login.css']")
    private WebElement keycloakTheme;

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
        waitGuiForElementNotPresent(keycloakTheme);
    }

    public void verifyKeycloakTheme(){
        waitGuiForElement(keycloakTheme);
    }

    public void saveTheme() {
        primaryButton.click();
    }

}
