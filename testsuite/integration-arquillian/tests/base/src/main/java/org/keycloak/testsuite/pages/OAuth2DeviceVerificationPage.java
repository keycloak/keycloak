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
package org.keycloak.testsuite.pages;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
public class OAuth2DeviceVerificationPage extends LanguageComboboxAwarePage {

    private static final String CONSENT_DENIED_MESSAGE = "Consent denied for connecting the device.";
    
    @FindBy(id = "device-user-code")
    private WebElement userCodeInput;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(className = "alert-error")
    private WebElement verifyErrorMessage;

    public void submit(String userCode) {
        userCodeInput.clear();
        if (userCode != null) {
            userCodeInput.sendKeys(userCode);
        }
        submitButton.click();
    }

    public String getError() {
        return verifyErrorMessage != null ? verifyErrorMessage.getText() : null;
    }

    @Override
    public boolean isCurrent() {
        if (driver.getTitle().startsWith("Sign in to ")) {
            try {
                driver.findElement(By.id("device-user-code"));
                return true;
            } catch (Throwable t) {
            }
        }
        return false;
    }

    public void assertApprovedPage() {
        Assert.assertTrue("Expected device approved page but was " + driver.getTitle() + " (" + driver.getCurrentUrl() + ")",
                isApprovedPage());
    }

    public void assertDeniedPage() {
        Assert.assertTrue("Expected device denied page but was " + driver.getTitle() + " (" + driver.getCurrentUrl() + ")",
                isDeniedPage());
    }

    public void assertInvalidUserCodePage() {
        Assert.assertTrue("Expected invalid user code page but was " + driver.getTitle() + " (" + driver.getCurrentUrl() + ")",
            isInvalidUserCodePage());
    }

    public void assertExpiredUserCodePage() {
        Assert.assertTrue("Expected expired user code page but was " + driver.getTitle() + " (" + driver.getCurrentUrl() + ")",
                isExpiredUserCodePage());
    }

    private boolean isApprovedPage() {
        if (driver.getTitle().startsWith("Sign in to ")) {
            try {
                driver.findElement(By.id("kc-page-title")).getText().equals("Device Login Successful");
                return true;
            } catch (Throwable t) {
            }
        }
        return false;
    }

    private boolean isDeniedPage() {
        if (driver.getTitle().startsWith("Sign in to ")) {
            try {
                return driver.findElement(By.id("kc-page-title")).getText().equals("Device Login Failed")
                        && driver.findElement(By.className("instruction")).getText().equals(CONSENT_DENIED_MESSAGE);
            } catch (Throwable t) {
            }
        }
        return false;
    }

    private boolean isInvalidUserCodePage() {
        if (driver.getTitle().startsWith("Sign in to ")) {
            try {
                driver.findElement(By.id("device-user-code"));
                return driver.findElement(By.id("kc-page-title")).getText().equals("Device Login")
                        && driver.findElement(By.className("kc-feedback-text")).getText().equals("Invalid code, please try again.");
            } catch (Throwable t) {
            }
        }
        return false;
    }

    private boolean isExpiredUserCodePage() {
        if (driver.getTitle().startsWith("Sign in to ")) {
            try {
                driver.findElement(By.id("device-user-code"));
                return driver.findElement(By.id("kc-page-title")).getText().equals("Device Login")
                        && driver.findElement(By.className("kc-feedback-text")).getText().equals("The code has expired. Please go back to your device and try connecting again.");
            } catch (Throwable t) {
            }
        }
        return false;
    }

    @Override
    public void open() {
    }
}
