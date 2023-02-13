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
import org.keycloak.models.UserModel;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class OTPSetup extends RequiredActions {
    @Page
    private LoginForm.TotpSetupForm form;

    @FindBy(id = "kc-totp-secret-qr-code")
    private WebElement barcodeImg;

    @FindBy(id = "kc-totp-secret-key")
    private WebElement secretKey;

    @FindBy(id = "mode-manual")
    private WebElement manualModeLink;

    @FindBy(id = "mode-barcode")
    private WebElement barcodeModeLink;

    @FindBy(id = "kc-totp-type")
    private WebElement otpType;

    @FindBy(id = "kc-totp-algorithm")
    private WebElement otpAlgorithm;

    @FindBy(id = "kc-totp-digits")
    private WebElement otpDigits;

    @FindBy(id = "kc-totp-period")
    private WebElement otpPeriod;

    @FindBy(id = "kc-totp-counter")
    private WebElement otpCounter;

    public void setTotp(String value) {
        form.setTotp(value);
    }

    public boolean isBarcodePresent() {
        try {
            return barcodeImg.isDisplayed();
        }
        catch (NoSuchElementException e) {
            return false;
        }
    }

    public String getSecretKey() {
        return secretKey.getText().replace(" ", "");
    }

    public boolean isSecretKeyPresent() {
        try {
            return secretKey.isDisplayed();
        }
        catch (NoSuchElementException e) {
            return false;
        }
    }

    public void clickManualMode() {
        clickLink(manualModeLink);
    }

    public void clickBarcodeMode() {
        clickLink(barcodeModeLink);
    }

    public String getOtpType() {
        return otpType.getText();
    }

    public String getOtpAlgorithm() {
        return otpAlgorithm.getText();
    }

    public String getOtpDigits() {
        return otpDigits.getText();
    }

    public String getOtpPeriod() {
        return otpPeriod.getText();
    }

    public String getOtpCounter() {
        return otpCounter.getText();
    }

    public void setUserLabel(String value) {
        form.setUserLabel(value);
    }

    @Override
    public String getActionId() {
        return UserModel.RequiredAction.CONFIGURE_TOTP.name();
    }
}
