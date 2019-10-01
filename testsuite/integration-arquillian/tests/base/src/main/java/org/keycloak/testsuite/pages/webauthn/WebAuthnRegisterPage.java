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
import org.keycloak.testsuite.pages.PageUtils;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WebAuthnRegisterPage extends AbstractPage {

    @FindBy(id = "firstName")
    private WebElement firstNameInput;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "password-confirm")
    private WebElement passwordConfirmInput;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(className = "alert-error")
    private WebElement loginErrorMessage;

    @FindBy(className = "instruction")
    private WebElement loginInstructionMessage;

    @FindBy(linkText = "« Back to Login")
    private WebElement backToLoginLink;


    public void register(String firstName, String lastName, String email, String username, String password, String passwordConfirm, String authenticatorLabel) {
        driver.findElement(By.id("firstName")).clear();
        if (firstName != null) {
            driver.findElement(By.id("firstName")).sendKeys(firstName);
        }

        driver.findElement(By.id("lastName")).clear();
        if (lastName != null) {
            driver.findElement(By.id("lastName")).sendKeys(lastName);
        }

        driver.findElement(By.id("email")).clear();
        if (email != null) {
            driver.findElement(By.id("email")).sendKeys(email);
        }

        driver.findElement(By.id("username")).clear();
        if (username != null) {
            driver.findElement(By.id("username")).sendKeys(username);
        }

        driver.findElement(By.id("password")).clear();
        if (password != null) {
            driver.findElement(By.id("password")).sendKeys(password);
        }

        driver.findElement(By.id("password-confirm")).clear();
        if (passwordConfirm != null) {
            driver.findElement(By.id("password-confirm")).sendKeys(passwordConfirm);
        }

        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();

        // label edit after registering authenicator by .create()
        WebDriverWait wait = new WebDriverWait(driver, 60);
        Alert promptDialog = wait.until(ExpectedConditions.alertIsPresent());
        //Alert promptDialog = driver.switchTo().alert();
        promptDialog.sendKeys(authenticatorLabel);
        promptDialog.accept();
    }

    public void registerWithEmailAsUsername(String firstName, String lastName, String email, String password, String passwordConfirm) {
        driver.findElement(By.id("firstName")).clear();
        if (firstName != null) {
            driver.findElement(By.id("firstName")).sendKeys(firstName);
        }

        driver.findElement(By.id("lastName")).clear();
        if (lastName != null) {
            driver.findElement(By.id("lastName")).sendKeys(lastName);
        }

        driver.findElement(By.id("email")).clear();
        if (email != null) {
            driver.findElement(By.id("email")).sendKeys(email);
        }

        try {
            driver.findElement(By.id("username")).clear();
            Assert.fail("Form must be without username field");
        } catch (NoSuchElementException e) {
            // OK
        }

        driver.findElement(By.id("password")).clear();
        if (password != null) {
            driver.findElement(By.id("password")).sendKeys(password);
        }

        driver.findElement(By.id("password-confirm")).clear();
        if (passwordConfirm != null) {
            driver.findElement(By.id("password-confirm")).sendKeys(passwordConfirm);
        }
        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();
    }

    public void clickBackToLogin() {
        backToLoginLink.click();
    }

    public String getError() {
        return loginErrorMessage != null ? loginErrorMessage.getText() : null;
    }

    public String getInstruction() {
        try {
            return loginInstructionMessage != null ? loginInstructionMessage.getText() : null;
        } catch (NoSuchElementException e){
            // OK
        }
        return null;
    }

    public String getFirstName() {
        return driver.findElement(By.id("firstName")).getAttribute("value");
    }

    public String getLastName() {
        return driver.findElement(By.id("lastName")).getAttribute("value");
    }

    public String getEmail() {
        return driver.findElement(By.id("email")).getAttribute("value");
    }

    public String getUsername() {
        return driver.findElement(By.id("username")).getAttribute("value");
    }

    public String getPassword() {
        return driver.findElement(By.id("password")).getAttribute("value");
    }

    public String getPasswordConfirm() {
        return driver.findElement(By.id("password-confirm")).getAttribute("value");
    }

    public boolean isCurrent() {
        return PageUtils.getPageTitle(driver).equals("Register");
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException();
    }

}