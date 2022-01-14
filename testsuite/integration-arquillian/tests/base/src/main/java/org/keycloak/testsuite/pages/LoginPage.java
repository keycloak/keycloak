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

import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.OAuthClient;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginPage extends LanguageComboboxAwarePage {

    @ArquillianResource
    protected OAuthClient oauth;

    @FindBy(id = "username")
    protected WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "input-error")
    private WebElement inputError;

    @FindBy(id = "totp")
    private WebElement totp;

    @FindBy(id = "rememberMe")
    private WebElement rememberMe;

    @FindBy(name = "login")
    protected WebElement submitButton;

    @FindBy(name = "cancel")
    private WebElement cancelButton;

    @FindBy(linkText = "Register")
    private WebElement registerLink;

    @FindBy(linkText = "Forgot Password?")
    private WebElement resetPasswordLink;

    @FindBy(linkText = "Username")
    private WebElement recoverUsernameLink;

    @FindBy(className = "alert-error")
    private WebElement loginErrorMessage;

    @FindBy(className = "alert-warning")
    private WebElement loginWarningMessage;

    @FindBy(className = "alert-success")
    private WebElement loginSuccessMessage;


    @FindBy(className = "alert-info")
    private WebElement loginInfoMessage;

    @FindBy(className = "instruction")
    private WebElement instruction;


    public void login(String username, String password) {
        usernameInput.clear();
        usernameInput.sendKeys(username);

        passwordInput.clear();
        passwordInput.sendKeys(password);

        clickLink(submitButton);
    }

    public void login(String password) {
        passwordInput.clear();
        passwordInput.sendKeys(password);

        clickLink(submitButton);
    }

    public void missingPassword(String username) {
        usernameInput.clear();
        usernameInput.sendKeys(username);
        passwordInput.clear();
        clickLink(submitButton);

    }
    public void missingUsername() {
        usernameInput.clear();
        clickLink(submitButton);

    }

    public String getUsername() {
        return usernameInput.getAttribute("value");
    }

    public boolean isUsernameInputEnabled() {
        return usernameInput.isEnabled();
    }

    public boolean isUsernameInputPresent() {
        return !driver.findElements(By.id("username")).isEmpty();
    }

    public boolean isRegisterLinkPresent() {
        return !driver.findElements(By.linkText("Register")).isEmpty();
    }

    public boolean isRememberMeCheckboxPresent() {
        return !driver.findElements(By.id("rememberMe")).isEmpty();
    }

    public String getPassword() {
        return passwordInput.getAttribute("value");
    }

    public void cancel() {
        cancelButton.click();
    }

    public String getInputError() {
        try {
            return getTextFromElement(inputError);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getError() {
        try {
            return getTextFromElement(loginErrorMessage);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getInstruction() {
        return instruction != null ? instruction.getText() : null;
    }

    public String getSuccessMessage() {
        return loginSuccessMessage != null ? loginSuccessMessage.getText() : null;
    }
    public String getInfoMessage() {
        try {
            return getTextFromElement(loginInfoMessage);
        } catch (NoSuchElementException e) {
            return null;
        }
    }


    public boolean isCurrent() {
        String realm = "test";
        return isCurrent(realm);
    }

    public boolean isCurrent(String realm) {
        return DroneUtils.getCurrentDriver().getTitle().equals("Sign in to " + realm) || DroneUtils.getCurrentDriver().getTitle().equals("Anmeldung bei " + realm);
    }

    public void assertCurrent(String realm) {
        String name = getClass().getSimpleName();
        Assert.assertTrue("Expected " + name + " but was " + DroneUtils.getCurrentDriver().getTitle() + " (" + DroneUtils.getCurrentDriver().getCurrentUrl() + ")",
                isCurrent(realm));
    }

    public void clickRegister() {
        registerLink.click();
    }

    public void clickSocial(String alias) {
        WebElement socialButton = findSocialButton(alias);
        clickLink(socialButton);
    }

    public WebElement findSocialButton(String alias) {
        String id = "social-" + alias;
        return DroneUtils.getCurrentDriver().findElement(By.id(id));
    }

    public boolean isSocialButtonPresent(String alias) {
        String id = "social-" + alias;
        return !DroneUtils.getCurrentDriver().findElements(By.id(id)).isEmpty();
    }

    public void resetPassword() {
        clickLink(resetPasswordLink);
    }

    public void recoverUsername() {
        clickLink(recoverUsernameLink);
    }

    public void setRememberMe(boolean enable) {
        boolean current = rememberMe.isSelected();
        if (current != enable) {
            rememberMe.click();
        }
    }

    public boolean isRememberMeChecked() {
        return rememberMe.isSelected();
    }

    @Override
    public void open() {
        oauth.openLoginForm();
        assertCurrent();
    }

}
