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
package org.keycloak.tests.forms.page;

import org.keycloak.testframework.ui.page.AbstractPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginConfigTotpPage extends AbstractPage {

    @FindBy(id = "totpSecret")
    private WebElement totpSecret;

    @FindBy(id = "totp")
    private WebElement totpInput;

    @FindBy(id = "userLabel")
    private WebElement totpLabelInput;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(name = "cancel-aia")
    private WebElement cancelAIAButton;

    @FindBy(id = "mode-barcode")
    private WebElement barcodeLink;

    @FindBy(id = "mode-manual")
    private WebElement manualLink;

    @FindBy(css = "div[class^='pf-v5-c-alert'], div[class^='alert-error']")
    private WebElement loginAlertErrorMessage;

    @FindBy(id = "input-error-otp-code")
    private WebElement totpInputCodeError;

    @FindBy(id = "input-error-otp-label")
    private WebElement totpInputLabelError;

    public LoginConfigTotpPage(ManagedWebDriver driver) {
        super(driver);
    }


    public String getTotpSecret() {
        return totpSecret.getAttribute("value");
    }

    public boolean isCurrent() {
        try {
            driver.findElement(By.id("totp"));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean isCancelDisplayed() {
        try {
            return cancelAIAButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-config-totp";
    }
}
