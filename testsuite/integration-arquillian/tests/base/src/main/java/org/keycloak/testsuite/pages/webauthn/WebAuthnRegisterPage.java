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

package org.keycloak.testsuite.pages.webauthn;

import org.junit.Assert;
import org.keycloak.testsuite.pages.AbstractPage;
import org.openqa.selenium.Alert;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * WebAuthnRegisterPage, which is displayed when WebAuthnRegister required action is triggered. It is useful with Chrome testing API.
 *
 * Page will be displayed after successful JS call of "navigator.credentials.create", which will register WebAuthn credential
 * with the browser
 */
public class WebAuthnRegisterPage extends AbstractPage {


    public void registerWebAuthnCredential(String authenticatorLabel) {
        // label edit after registering authenicator by .create()
        WebDriverWait wait = new WebDriverWait(driver, 60);
        Alert promptDialog = wait.until(ExpectedConditions.alertIsPresent());

        Assert.assertEquals("Please input your registered authenticator's label", promptDialog.getText());

        promptDialog.sendKeys(authenticatorLabel);
        promptDialog.accept();
    }


    public boolean isCurrent() {
        // Cant verify the page in case that prompt is shown. Prompt is shown immediately when WebAuthnRegisterPage is displayed
        throw new UnsupportedOperationException();
    }


    @Override
    public void open() {
        throw new UnsupportedOperationException();
    }

}
