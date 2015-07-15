/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.admin.page;

import static org.keycloak.testsuite.admin.util.Constants.ADMIN_PSSWD;
import static org.keycloak.testsuite.admin.util.SeleniumUtils.waitGuiForElement;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author Petr Mensik
 */
public class LoginPage extends AbstractPage {

    @FindBy(id = "username")
    private WebElement usernameInput;
    
    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(linkText = "Register")
    private WebElement registerLink;

	@FindBy(id = "kc-header")
	private WebElement loginPageHeader;

	public void login(String username, String password) {
		waitGuiForElement(usernameInput, "Login form should be visible");
        usernameInput.sendKeys(username);
        passwordInput.sendKeys(password);
        passwordInput.submit();
    }
    
    public void loginAsAdmin() {
        login("admin", ADMIN_PSSWD);
    }

    public void goToUserRegistration() {
        waitGuiForElement(usernameInput, "Login form should be visible");
        registerLink.click();
    }
	
	public String getLoginPageHeaderText() {
		return loginPageHeader.getText();
	}
	
	public WebElement getLoginPageHeader() {
		return loginPageHeader;
	}
}
