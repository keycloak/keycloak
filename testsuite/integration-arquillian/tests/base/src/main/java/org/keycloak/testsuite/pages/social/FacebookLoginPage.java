/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.pages.social;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class FacebookLoginPage extends AbstractSocialLoginPage {
    private static final String continueButtonLocator = "//*[contains(@aria-label,'Continue')]";
    private static final String allowAllCookiesLocator = "//button[text()='Allow all cookies']";

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "pass")
    private WebElement passwordInput;

    @FindBy(id = "loginbutton")
    private WebElement loginButton;

    @FindBy(xpath = continueButtonLocator)
    private WebElement continueButton;

    @Override
    public void login(String user, String password) {
        // Check if allowing cookies is required and eventually allow them
        List<WebElement> allowCookiesButton = driver.findElements(By.xpath(allowAllCookiesLocator));
        if (allowCookiesButton.size() > 0)
            allowCookiesButton.get(0).click();

        if (driver.findElements(By.xpath(continueButtonLocator)).isEmpty()){ //on first login
            emailInput.clear();
            emailInput.sendKeys(user);
            passwordInput.sendKeys(password);
            JavascriptExecutor executor = (JavascriptExecutor) driver;
            executor.executeScript("arguments[0].click();", loginButton);
            continueButton.click();
        }else{ //already logged in in previous testcase, just confirm previous session
            continueButton.click();
        }
    }
}
