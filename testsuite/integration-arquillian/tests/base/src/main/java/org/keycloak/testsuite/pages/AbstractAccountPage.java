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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractAccountPage extends AbstractPage {

    @FindBy(linkText = "Sign Out")
    private WebElement logoutLink;

    @FindBy(id = "kc-current-locale-link")
    private WebElement languageText;

    @FindBy(id = "kc-locale-dropdown")
    private WebElement localeDropdown;

    public void logout() {
        logoutLink.click();
    }

    public String getLanguageDropdownText() {
        return languageText.getText();
    }

    public void openLanguage(String language){
        WebElement langLink = localeDropdown.findElement(By.xpath("//a[text()='" +language +"']"));
        String url = langLink.getAttribute("href");
        driver.navigate().to(url);
    }
}
