/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.util.UIUtils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LogoutConfirmPage extends LanguageComboboxAwarePage {

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement confirmLogoutButton;

    @FindBy(linkText = "« Back to Application")
    private WebElement backToApplicationLink;

    @Override
    public boolean isCurrent() {
        return isCurrent(driver);
    }

    public boolean isCurrent(WebDriver driver1) {
        return "Logging out".equals(PageUtils.getPageTitle(driver1));
    }

    public void confirmLogout() {
        UIUtils.clickLink(confirmLogoutButton);
    }

    public void confirmLogout(WebDriver driver) {
        UIUtils.clickLink(driver.findElement(By.cssSelector("input[type=\"submit\"]")));
    }

    public void clickBackToApplicationLink() {
        UIUtils.clickLink(backToApplicationLink);
    }
}
