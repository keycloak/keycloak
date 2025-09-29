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

package org.keycloak.testsuite.auth.page;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.UIUtils.setTextInputValue;
import static org.keycloak.testsuite.util.UIUtils.clickLink;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class WelcomePage extends AuthServer {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "password-confirmation")
    private WebElement passwordConfirmationInput;

    @FindBy(tagName = "button")
    private WebElement createButton;

    @FindBy(css = ".welcome-header h1")
    private WebElement welcomeMessage;

    public boolean isPasswordSet() {
        return !(driver.getPageSource().contains("Create an administrative user") ||
                driver.getPageSource().contains("You will need local access to create the administrative user."));
    }

    public void setPassword(String username, String password) {
        setTextInputValue(usernameInput, username);
        setTextInputValue(passwordInput, password);
        setTextInputValue(passwordConfirmationInput, password);

        clickLink(createButton);

        if (!driver.getPageSource().contains("User created")) {
            throw new RuntimeException("Failed to updated password");
        }
    }

    public void navigateToAdminConsole() {
        clickLink(driver.findElement(By.linkText("Administration Console")));
    }

    public String getWelcomeMessage() {
        return getTextFromElement(welcomeMessage);
    }

}
