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

import org.keycloak.testsuite.rule.WebResource;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RegisterPage extends Page {

    @WebResource
    private WebDriver browser;

    @FindBy(id = "name")
    private WebElement nameInput;

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

    @FindBy(id = "loginError")
    private WebElement loginErrorMessage;

    public void register(String name, String email, String username, String password, String passwordConfirm) {
        if (name != null) {
            nameInput.sendKeys(name);
        }

        if (email != null) {
            emailInput.sendKeys(email);
        }

        if (username != null) {
            usernameInput.sendKeys(username);
        }

        if (password != null) {
            passwordInput.sendKeys(password);
        }

        if (passwordConfirm != null) {
            passwordConfirmInput.sendKeys(passwordConfirm);
        }

        submitButton.click();
    }

    public String getError() {
        return loginErrorMessage != null ? loginErrorMessage.getText() : null;
    }

    public boolean isCurrent() {
        return browser.getTitle().equals("Register with test");
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException();
    }

}
