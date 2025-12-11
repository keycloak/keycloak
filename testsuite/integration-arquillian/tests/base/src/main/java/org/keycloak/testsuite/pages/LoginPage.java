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

import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.UIUtils;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
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

    @FindBy(xpath = "//html")
    protected WebElement htmlRoot;

    @FindBy(id = "username")
    protected WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "input-error-username")
    private WebElement userNameInputError;

    @FindBy(id = "input-error-password")
    private WebElement passwordInputError;

    @FindBy(id = "input-error")
    private WebElement inputError;

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

    @FindBy(className = "pf-m-danger")
    private WebElement loginErrorMessage;

    @FindBy(className = "pf-m-success")
    private WebElement loginSuccessMessage;

    @FindBy(className = "pf-m-info")
    private WebElement loginInfoMessage;

    @FindBy(className = "instruction")
    private WebElement instruction;

    public void login(String username, String password) {
        clearUsernameInputAndWaitIfNecessary();
        usernameInput.sendKeys(username);

        passwordInput.clear();
        passwordInput.sendKeys(password);

        clickSignIn();
    }

    public void loginUsername(String username) {
        clearUsernameInputAndWaitIfNecessary();
        usernameInput.sendKeys(username);
        clickSignIn();
    }

    private void clearUsernameInputAndWaitIfNecessary() {
        try {
            usernameInput.clear();
        } catch (NoSuchElementException ex) {
            // we might have clicked on a social login icon and might need to wait for the login to appear.
            // avoid waiting by default to avoid the delay.
            WaitUtils.waitUntilElement(usernameInput).is().present();
            usernameInput.clear();
        }
    }

    public void login(String password) {
        passwordInput.clear();
        passwordInput.sendKeys(password);

        clickSignIn();
    }

    public void clickSignIn() {
        clickLink(submitButton);
    }

    public void missingPassword(String username) {
        clearUsernameInputAndWaitIfNecessary();
        usernameInput.sendKeys(username);
        passwordInput.clear();
        clickSignIn();
    }

    public void missingUsername() {
        clearUsernameInputAndWaitIfNecessary();
        clickSignIn();
    }

    public String getHtmlLanguage() {
        return htmlRoot.getAttribute("lang");
    }

    public String getUsername() {
        return usernameInput.getAttribute("value");
    }

    public boolean isUsernameInputEnabled() {
        return usernameInput.isEnabled();
    }

    public String getUsernameAutocomplete() {
        return usernameInput.getDomAttribute("autocomplete");
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

    public boolean isPasswordInputPresent() {
        return !driver.findElements(By.id("password")).isEmpty();
    }

    public void cancel() {
        cancelButton.click();
    }

    public String getInputError() {
        try {
            return getTextFromElement(userNameInputError);
        } catch (NoSuchElementException ex) {
            try {
                return getTextFromElement(passwordInputError);
            } catch (NoSuchElementException e) {
                try {
                    return getTextFromElement(inputError);
                } catch (NoSuchElementException error) {
                    return null;
                }
            }
        }
    }

    public String getUsernameInputError() {
        try {
            return getTextFromElement(userNameInputError);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getPasswordInputError() {
        try {
            return getTextFromElement(passwordInputError);
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

    @Override
    public boolean isCurrent() {
        String realm = "test";
        return isCurrent(realm);
    }

    @Override
    public boolean isCurrent(String realm) {
        return DroneUtils.getCurrentDriver().getTitle().equals("Sign in to " + realm) || DroneUtils.getCurrentDriver().getTitle().equals("Anmeldung bei " + realm);
    }

    public void assertCurrent(String realm) {
        String name = getClass().getSimpleName();
        Assert.assertTrue("Expected " + name + " but was " + DroneUtils.getCurrentDriver().getTitle() + " (" + DroneUtils.getCurrentDriver().getCurrentUrl() + ")",
                isCurrent(realm));
    }

    public void clickRegister() {
        clickLink(registerLink);
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

    public void setRememberMe(boolean enable) {
        UIUtils.switchCheckbox(rememberMe, enable);
    }

    public boolean isRememberMeChecked() {
        return rememberMe.isSelected();
    }

    /**
     * @deprecated Use {@link OAuthClient#openLoginForm()}
     */
    @Deprecated
    public void open() {
        oauth.openLoginForm();
        assertCurrent();
    }

    /**
     * @deprecated Use {@link OAuthClient#openLoginForm()}
     */
    @Deprecated
    public void open(String realm) {
        oauth.realm(realm);
        oauth.openLoginForm();
        assertCurrent(realm);
    }
}
