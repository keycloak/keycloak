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
package org.keycloak.testsuite.console.page.fragment;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 *
 * @author Petr Mensik
 */
public class Menu {

    private static final String MENU_LOCATOR = "ul[class='dropdown-menu']";

    @FindBy(css = MENU_LOCATOR)
    private List<WebElement> menuList;

    @FindBy(css = ".dropdown-toggle")
    private List<WebElement> toggle;

    public void logOut() {
        clickOnMenuElement(MenuType.USER, "Sign Out");
    }

    public void goToAccountManagement() {
        clickOnMenuElement(MenuType.USER, "Manage Account");
    }

    public void switchRealm(String realmName) {
        if (!realmName.equals(getCurrentRealm())) {
            clickOnMenuElement(MenuType.REALM, realmName);
        }
    }

    public String getCurrentRealm() {
        waitUntilElement(By.cssSelector(MENU_LOCATOR)).is().present();
        return toggle.get(1).getText();
    }

    private void clickOnMenuElement(MenuType menuType, String linkText) {
        int menuOrder = 0;
        switch (menuType) {
            case REALM:
                menuOrder = 1;
                break;
            case USER:
                menuOrder = 0;
                break;
        }
        waitUntilElement(By.cssSelector(MENU_LOCATOR)).is().present();
        if (!menuList.get(menuOrder).isDisplayed()) {
            toggle.get(menuOrder).click();
        }
        for (WebElement item : menuList.get(menuOrder).findElements(By.cssSelector(MENU_LOCATOR + " a"))) {
            if (item.getText().contains(linkText)) {
                clickLink(item);
                return;
            }
        }
        throw new RuntimeException("Could not find menu item containing \"" + linkText + "\"");
    }

    private enum MenuType {

        USER, REALM
    }

}
