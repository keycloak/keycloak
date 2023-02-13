/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.auth.page.login;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.console.page.fragment.LocaleDropdown;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class LoginBase extends AuthRealm {
    @Page
    protected FeedbackMessage feedbackMessage;

    @FindBy(id = "kc-page-title")
    protected WebElement title;

    @FindBy(id = "kc-header-wrapper")
    protected WebElement header;

    @FindBy(id = "kc-locale")
    private LocaleDropdown localeDropdown;

    protected String keycloakThemeCssName;

    public FeedbackMessage feedbackMessage() {
        return feedbackMessage;
    }

    public String getTitleText() {
        return getTextFromElement(title);
    }

    public String getHeaderText() {
        return getTextFromElement(header);
    }

    protected By getKeycloakThemeLocator() {
        if (keycloakThemeCssName == null) {
            throw new IllegalStateException("keycloakThemeCssName property must be set");
        }
        return By.cssSelector("link[href*='login/" + keycloakThemeCssName + "/css/login.css']");
    }

    public void waitForKeycloakThemeNotPresent() {
        waitUntilElement(getKeycloakThemeLocator()).is().not().present();
    }

    public void waitForKeycloakThemePresent() {
        waitUntilElement(getKeycloakThemeLocator()).is().present();
    }

    public void setKeycloakThemeCssName(String name) {
        keycloakThemeCssName = name;
    }

    public LocaleDropdown localeDropdown() {
        return localeDropdown;
    }
}
