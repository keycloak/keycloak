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
package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.util.UIUtils;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginConfigTotpPage extends LogoutSessionsPage {

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

    public void configure(String totp) {
        totpInput.sendKeys(totp);
        submit();
    }

    public void configure(String totp, String userLabel) {
        totpInput.sendKeys(totp);
        totpLabelInput.sendKeys(userLabel);
        submit();
    }

    public void submit() {
        UIUtils.clickLink(submitButton);
    }

    public void cancel() {
        UIUtils.clickLink(cancelAIAButton);
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

    public void clickManual() {
        UIUtils.clickLink(manualLink);
    }

    public void clickBarcode() {
        UIUtils.clickLink(barcodeLink);
    }

    public String getInputCodeError() {
        try {
            return UIUtils.getTextFromElement(totpInputCodeError);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getInputLabelError() {
        try {
            return UIUtils.getTextFromElement(totpInputLabelError);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getAlertError() {
        try {
            return UIUtils.getTextFromElement(loginAlertErrorMessage);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public boolean isCancelDisplayed() {
        try {
            return cancelAIAButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

}
