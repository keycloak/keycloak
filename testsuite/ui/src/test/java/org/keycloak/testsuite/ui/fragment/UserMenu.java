/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.keycloak.testsuite.ui.fragment;

import java.util.List;
import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitGuiForElement;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author pmensik
 */
public class UserMenu {

    @FindBy(css = "ul[class='dropdown-menu']")
	private WebElement menu;
	
    @FindBy(css = ".dropdown-toggle")
    private WebElement toggle;

    @FindBy(css = ".dropdown-menu a")
    private List<WebElement> menuItems;
    
    public void logOut() {
		clickOnMenuElement("Sign Out");
	}
    
    public void goToAccountManagement() {
		clickOnMenuElement("Manage Account");
	}

	public WebElement getMenu() {
		return menu;
	}
    
    private void clickOnMenuElement(String linkText) {
		waitGuiForElement(menu, "User menu should be always visible.");
        if (!menu.isDisplayed()) 
			toggle.click();
        for (WebElement item : menuItems) {
            if (item.getText().contains(linkText)) {
                item.click();
                return;
            }
        }
        throw new RuntimeException("Could not find menu item containing \"" + linkText + "\"");
    }

}
