/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.keycloak.testsuite.ui.page;

import static org.keycloak.testsuite.ui.util.Constants.ADMIN_PSSWD;
import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitGuiForElement;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author pmensik
 */

public class LoginPage extends AbstractPage {

    @FindBy(id = "username")
    private WebElement usernameInput;
    
    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(linkText = "Register")
    private WebElement registerLink;

	@FindBy(id = "kc-header-wrapper")
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
