/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public abstract class AbstractLoginPage extends AbstractPage {

    @FindBy(xpath = "//select[@aria-label='languages']/option[@selected]")
    private WebElement selectedLanguage;

    @FindBy(xpath = "//select[@aria-label='languages']")
    private WebElement languages;

    @FindBy(id = "kc-current-locale-link")
    private WebElement languageTextBase;    // base theme

    @FindBy(id = "kc-locale-dropdown")
    private WebElement localeDropdownBase;  // base theme

    public AbstractLoginPage(ManagedWebDriver driver) {
        super(driver);
    }

    public String getSelectedLanguage() {
        try {
            final String text = selectedLanguage.getText();
            return text == null ? text : text.trim();
        } catch (NoSuchElementException ex) {
            // Fallback for Login v1
            return languageTextBase.getText();
        }
    }

    public void selectLanguage(String language){
        try {
            WebElement langLink = languages.findElement(By.xpath("//option[text()[contains(.,'" + language + "')]]"));
            langLink.click();
        } catch (NoSuchElementException ex) {
            // Fallback for Login v1
            WebElement langLink = localeDropdownBase.findElement(By.xpath("//a[text()[contains(.,'" + language + "')]]"));
            langLink.click();
        }
    }

}
