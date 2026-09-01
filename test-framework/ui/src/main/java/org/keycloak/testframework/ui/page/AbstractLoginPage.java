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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    @FindBy(xpath = "//script")
    private List<WebElement> scripts;

    @FindBy(id = "kc-current-locale-link")
    private WebElement languageTextBase;    // base theme

    @FindBy(id = "kc-locale-dropdown")
    private WebElement localeDropdownBase;  // base theme

    @FindBy(id = "kc-attempted-username") // Username during re-authentication
    private WebElement attemptedUsernameLabel;

    @FindBy(className = "pf-m-info")
    private WebElement loginInfoMessage;

    @FindBy(className = "pf-m-danger")
    private WebElement loginErrorMessage;

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

    public List<WebElement> getScripts() {
        if (scripts == null) {
            return Collections.emptyList();
        }
        return scripts;
    }

    public List<WebElement> getInlineScriptsWithoutNonce() {
        if (scripts == null) {
            return Collections.emptyList();
        }
        return scripts
                .stream()
                .filter(s -> isInlineScript(s) && hasNoNonce(s))
                .toList();
    }

    public String getAttemptedUsername() {
        try {
            String text = attemptedUsernameLabel.getAttribute("value");
            if (text == null) return attemptedUsernameLabel.getText();
            return text;
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public Optional<String> getInfoMessage() {
        try {
            return Optional.of(loginInfoMessage.getText());
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    public Optional<String> getErrorMessage() {
        try {
            return Optional.of(loginErrorMessage.getText());
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    private boolean isInlineScript(WebElement scriptElement) {
        checkIsScript(scriptElement);
        var src = scriptElement.getDomAttribute("src");
        return src == null || src.isBlank();
    }

    private boolean hasNoNonce(WebElement scriptElement) {
        checkIsScript(scriptElement);
        var nonce = scriptElement.getDomAttribute("nonce");
        return nonce == null || nonce.isBlank();
    }

    private void checkIsScript(WebElement webElement) {
        if (!"script".equals(webElement.getTagName())) {
            throw new IllegalArgumentException(String.format("Must be a <script> tag, was <%s>", webElement.getTagName()));
        }
    }
}
