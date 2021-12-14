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

package org.keycloak.testsuite.webauthn.pages;

import org.hamcrest.CoreMatchers;
import org.keycloak.testsuite.pages.AbstractPage;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * WebAuthnRegisterPage, which is displayed when WebAuthnRegister required action is triggered. It is useful with Chrome testing API.
 * <p>
 * Page will be displayed after successful JS call of "navigator.credentials.create", which will register WebAuthn credential
 * with the browser
 */
public class WebAuthnRegisterPage extends AbstractPage {

    @FindBy(id = "registerWebAuthn")
    private WebElement registerButton;

    // Available only with AIA
    @FindBy(id = "cancelWebAuthnAIA")
    private WebElement cancelAIAButton;

    @FindBy(id = "kc-page-title")
    private WebElement formTitle;

    public void clickRegister() {
        WaitUtils.waitUntilElement(registerButton).is().clickable();
        registerButton.click();
    }

    public void cancelAIA() {
        assertThat("It only works with AIA", isAIA(), CoreMatchers.is(true));
        WaitUtils.waitUntilElement(cancelAIAButton).is().clickable();
        cancelAIAButton.click();
    }

    public void registerWebAuthnCredential(String authenticatorLabel) {
        // label edit after registering authenticator by .create()
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
        Alert promptDialog = wait.until(ExpectedConditions.alertIsPresent());

        assertThat(promptDialog.getText(), CoreMatchers.is("Please input your registered authenticator's label"));

        promptDialog.sendKeys(authenticatorLabel);
        promptDialog.accept();
    }

    public boolean isAIA() {
        try {
            cancelAIAButton.getText();
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public String getFormTitle() {
        try {
            WaitUtils.waitUntilElement(formTitle).is().present();
            return formTitle.getText();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    @Override
    public boolean isCurrent() {
        final String formTitle = getFormTitle();
        return formTitle != null && formTitle.equals("Security Key Registration") &&
                driver.getPageSource().contains("navigator.credentials.create");
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException();
    }

}
