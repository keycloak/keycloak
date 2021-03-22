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
package org.keycloak.testsuite.auth.page.login;

import org.keycloak.testsuite.util.UIUtils;
import org.keycloak.testsuite.util.URLUtils;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class OneTimeCode extends Authenticate {
    @FindBy(id = "otp")
    private WebElement otpInputField;

    @FindBy(xpath = ".//label[@for='otp']")
    private WebElement otpInputLabel;

    @FindBy(className = "alert-error")
    private WebElement loginErrorMessage;

    public String getOtpLabel() {
        return getTextFromElement(otpInputLabel);
    }

    public boolean isOtpLabelPresent() {
        try {
            return otpInputLabel.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public void sendCode(String code) {
        setOtp(code);
        submit();
    }

    public String getError() {
        return loginErrorMessage != null ? loginErrorMessage.getText() : null;
    }

    @Override
    public boolean isCurrent() {
        return URLUtils.currentUrlStartsWith(toString() + "?") && isOtpLabelPresent();
    }

    public void setOtp(String value) {
        UIUtils.setTextInputValue(otpInputField, value);
    }
}
