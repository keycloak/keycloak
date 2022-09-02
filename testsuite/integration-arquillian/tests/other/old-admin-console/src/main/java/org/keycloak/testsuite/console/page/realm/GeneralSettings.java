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

package org.keycloak.testsuite.console.page.realm;

import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author Petr Mensik
 */

public class GeneralSettings extends Form {

    @FindBy(id = "name")
    private WebElement realmName;

    @FindBy(id = "enabled")
    private WebElement realmEnabled;

    @FindBy(id = "updateProfileOnInitialSocialLogin")
    private WebElement updateProfileOnInitialSocialLogin;

    @FindBy(id = "loginTheme")
    private Select loginThemeSelect;

    @FindBy(id = "accountTheme")
    private Select accountThemeSelect;

    @FindBy(id = "adminTheme")
    private Select adminThemeSelect;

    @FindBy(id = "emailTheme")
    private Select emailThemeSelect;

    public void selectLoginTheme(String theme) {
        loginThemeSelect.selectByVisibleText(theme);
    }

    public void selectAccountTheme(String theme) {
        accountThemeSelect.selectByVisibleText(theme);
    }

    public void selectAdminTheme(String theme) {
        adminThemeSelect.selectByVisibleText(theme);
    }

    public void selectEmailTheme(String theme) {
        emailThemeSelect.selectByVisibleText(theme);
    }

    public void setRealmName(String name) {
        UIUtils.setTextInputValue(realmName, name);
    }
}
