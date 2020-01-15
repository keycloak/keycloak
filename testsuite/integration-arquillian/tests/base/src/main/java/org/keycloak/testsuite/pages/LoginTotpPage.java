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

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginTotpPage extends LanguageComboboxAwarePage {

    @FindBy(id = "otp")
    private WebElement otpInput;

    @FindBy(id = "password-token")
    private WebElement passwordToken;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(className = "alert-error")
    private WebElement loginErrorMessage;

    @FindBy(id = "selected-credential-id")
    private WebElement selectedCredentialCombobox;

    public void login(String totp) {
        otpInput.clear();
        if (totp != null) otpInput.sendKeys(totp);

        submitButton.click();
    }

    public String getError() {
        return loginErrorMessage != null ? loginErrorMessage.getText() : null;
    }

    public boolean isCurrent() {
        try {
            driver.findElement(By.id("otp"));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException();
    }


    // If false, we don't expect that credentials combobox is available. If true, we expect that it is available on the page
    public void assertOtpCredentialSelectorAvailability(boolean expectedAvailability) {
        try {
            driver.findElement(By.id("selected-credential-id"));
            Assert.assertTrue(expectedAvailability);
        } catch (NoSuchElementException nse) {
            Assert.assertFalse(expectedAvailability);
        }
    }


    public List<String> getAvailableOtpCredentials() {
        return new Select(selectedCredentialCombobox).getOptions()
                .stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }


    public String getSelectedOtpCredential() {
        return new Select(selectedCredentialCombobox).getOptions()
                .stream()
                .filter(webElement -> webElement.getAttribute("selected") != null)
                .findFirst()
                .orElseThrow(() -> {

                    return new AssertionError("Selected OTP credential not found");

                })
                .getText();
    }


    public void selectOtpCredential(String credentialName) {
        new Select(selectedCredentialCombobox).selectByVisibleText(credentialName);
    }

}
