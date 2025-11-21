/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.webauthn.pages;

import org.keycloak.testframework.ui.page.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Page shown during WebAuthn login. Page is useful with Chrome testing API
 */
public class WebAuthnLoginPage extends AbstractPage {

    @FindBy(id = "authenticateWebAuthnButton")
    private WebElement authenticateButton;

    @FindBy(xpath = "//div[contains(@id,'kc-webauthn-authenticator-label-')]")
    private List<WebElement> authenticatorsLabels;

    private WebAuthnAuthenticatorsListPage authenticators;

    public WebAuthnLoginPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return "webauthn-authenticators";
    }

    public void clickAuthenticate() {
        waitForPage();
        authenticateButton.click();
    }

    public boolean isCurrent() {
        try {
            authenticateButton.getText();
            return driver.findElement(By.id("authenticateWebAuthnButton")).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public WebAuthnAuthenticatorsListPage getAuthenticators() {
        return authenticators;
    }
}
