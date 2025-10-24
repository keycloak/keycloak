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
import org.keycloak.common.util.Retry;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginTotpPage extends LanguageComboboxAwarePage {

    @FindBy(id = "otp")
    private WebElement otpInput;

    @FindBy(id = "password-token")
    private WebElement passwordToken;

    @FindBy(css = "[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(css = "div[class^='pf-v5-c-alert'], div[class^='alert-error']")
    private WebElement loginErrorMessage;

    @FindBy(id = "input-error-otp")
    private WebElement totpInputCodeError;

    @FindBy(id = "input-error-otp-code")
    private WebElement otpInputCodeError;

    public void login(String totp) {
        otpInput.clear();
        if (totp != null) otpInput.sendKeys(totp);

        UIUtils.clickLink(submitButton);
    }

    public String getAlertError() {
        try {
            return UIUtils.getTextFromElement(loginErrorMessage);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getInputError(){
        try {
            return UIUtils.getTextFromElement(totpInputCodeError);
        } catch (NoSuchElementException e) {
            try {
                return UIUtils.getTextFromElement(otpInputCodeError);
            } catch (NoSuchElementException ex) {
                return null;
            }
        }
    }

    public boolean isCurrent() {
        try {
            driver.findElement(By.id("otp"));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    // If false, we don't expect that credentials combobox is available. If true, we expect that it is available on the page
    public void assertOtpCredentialSelectorAvailability(boolean expectedAvailability) {
        try {
            driver.findElement(By.className("pf-v5-c-tile"));
            Assert.assertTrue(expectedAvailability);
        } catch (NoSuchElementException nse) {
            Assert.assertFalse(expectedAvailability);
        }
    }


    public List<String> getAvailableOtpCredentials() {
        return driver.findElements(getXPathForLookupAllCards())
                .stream().map(WebElement::getText).collect(Collectors.toList());
    }


    public String getSelectedOtpCredential() {
        try {
            WebElement selected = driver.findElement(getCssSelectorForLookupActiveCard());
            return selected.getText();
        } catch (NoSuchElementException nse) {
            // No selected element found
            return null;
        }
    }

    private By getXPathForLookupAllCards() {
        return By.xpath("//span[contains(@class, 'pf-v5-c-tile__title')]");
    }

    private By getCssSelectorForLookupActiveCard() {
        return By.cssSelector(".pf-m-selected");
    }

    private By getXPathForLookupCardWithName(String credentialName) {
        return By.xpath("//div[contains(@class, 'pf-v5-c-tile')][normalize-space() = '"+ credentialName +"']");
    }


    public void selectOtpCredential(String credentialName) {
        WebElement webElement = driver.findElement(
                getXPathForLookupCardWithName(credentialName));
        UIUtils.click(webElement);
    }


    // Workaround, but works with HtmlUnit (WaitUtils.waitForElement doesn't). Find better solution for the future...
    private void waitForElement(By by) {
        Retry.executeWithBackoff((currentCount) -> {

            driver.findElement(by);

        }, 10, 10);
    }

}
