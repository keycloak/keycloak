/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.rule.WebResource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginPage extends AbstractPage {

    @WebResource
    protected OAuthClient oauth;
    
    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "totp")
    private WebElement totp;

    @FindBy(id = "rememberMe")
    private WebElement rememberMe;

    @FindBy(name = "login")
    private WebElement submitButton;

    @FindBy(name = "cancel")
    private WebElement cancelButton;

    @FindBy(linkText = "Register")
    private WebElement registerLink;

    @FindBy(linkText = "Forgot Password?")
    private WebElement resetPasswordLink;

    @FindBy(linkText = "Username")
    private WebElement recoverUsernameLink;

    @FindBy(className = "feedback-error")
    private WebElement loginErrorMessage;

    @FindBy(className = "feedback-warning")
    private WebElement loginWarningMessage;

    public void login(String username, String password) {
        usernameInput.clear();
        usernameInput.sendKeys(username);

        passwordInput.clear();
        passwordInput.sendKeys(password);

        submitButton.click();
    }

    public void login(String password) {
        passwordInput.clear();
        passwordInput.sendKeys(password);

        submitButton.click();
    }

    public String getUsername() {
        return usernameInput.getAttribute("value");
    }

    public void cancel() {
        cancelButton.click();
    }

    public String getError() {
        return loginErrorMessage != null ? loginErrorMessage.getText() : null;
    }

    public String getWarning() {
        return loginWarningMessage != null ? loginWarningMessage.getText() : null;
    }


    public boolean isCurrent() {
        return driver.getTitle().equals("Log in to test");
    }

    public void clickRegister() {
        registerLink.click();
    }

    public void clickSocial(String providerId) {
        WebElement socialButton = findSocialButton(providerId);
        socialButton.click();
    }

    public WebElement findSocialButton(String providerId) {
        String id = "zocial-" + providerId;
        return this.driver.findElement(By.id(id));
    }

    public void resetPassword() {
        resetPasswordLink.click();
    }

    public void recoverUsername() {
        recoverUsernameLink.click();
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
