/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.keycloak.testsuite.ui.fragment;

import java.util.List;
import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitGuiForElement;
import org.openqa.selenium.By;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author pmensik
 */
public class MenuPage {

	private static final String MENU_LOCATOR = "ul[class='dropdown-menu']";
	
    @FindBy(css = MENU_LOCATOR)
	private List<WebElement> menuList;
	
    @FindBy(css = ".dropdown-toggle")
    private List<WebElement> toggle;

    public void logOut() {
		clickOnMenuElement(Menu.USER, "Sign Out");
	}
    
    public void goToAccountManagement() {
		clickOnMenuElement(Menu.USER, "Manage Account");
	}
	
	public void switchRealm(String realmName) {
		clickOnMenuElement(Menu.REALM, realmName);
	}

	private void clickOnMenuElement(Menu menuType, String linkText) {
		int menuOrder = 0;
		switch(menuType) {
			case REALM: menuOrder = 1; break;
			case USER: menuOrder = 0; break;
		}
		waitGuiForElement(By.cssSelector(MENU_LOCATOR));
        if (!menuList.get(menuOrder).isDisplayed()) 
			toggle.get(menuOrder).click();
        for (WebElement item : menuList.get(menuOrder).findElements(By.cssSelector(".dropdown-menu a"))) {
            if (item.getText().contains(linkText)) {
                item.click();
                return;
            }
        }
        throw new RuntimeException("Could not find menu item containing \"" + linkText + "\"");
    }
	
	private enum Menu {
		USER, REALM
	}

}
