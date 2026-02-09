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

import java.net.URI;
import java.net.URISyntaxException;

import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.UIUtils;
import org.keycloak.testsuite.util.WaitUtils;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Provides some generic utils available on most of login pages (Language combobox, Link "Try another way" etc)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class LanguageComboboxAwarePage extends AbstractPage {

    @FindBy(xpath = "//select[@aria-label='languages']/option[@selected]")
    private WebElement languageText;

    @FindBy(xpath = "//select[@aria-label='languages']")
    private WebElement localeDropdown;

    @FindBy(id = "kc-current-locale-link")
    private WebElement languageTextBase;    // base theme

    @FindBy(id = "kc-locale-dropdown")
    private WebElement localeDropdownBase;  // base theme

    @FindBy(id = "try-another-way")
    private WebElement tryAnotherWayLink;

    @FindBy(id = "kc-attempted-username")
    private WebElement attemptedUsernameLabel;

    @FindBy(id = "reset-login")
    private WebElement resetLoginLink;

    @FindBy(id = "account")
    private WebElement accountLink;

    public String getLanguageDropdownText() {
        try {
            final String text = languageText.getText();
            return text == null ? text : text.trim();
        } catch (NoSuchElementException ex) {
            return languageTextBase.getText();
        }
    }

    public void openLanguage(String language){
        try {
            WebElement langLink = localeDropdown.findElement(By.xpath("//option[text()[contains(.,'" + language + "')]]"));
            String url = langLink.getAttribute("value");
            DroneUtils.getCurrentDriver().navigate().to(new URI(DroneUtils.getCurrentDriver().getCurrentUrl()).resolve(url).toString());
        } catch (NoSuchElementException ex) {
            WebElement langLink = localeDropdownBase.findElement(By.xpath("//a[text()[contains(.,'" + language + "')]]"));
            String url = langLink.getAttribute("href");
            DroneUtils.getCurrentDriver().navigate().to(url);
        } catch (URISyntaxException ex) {
            Assert.fail(ex.getMessage());
        }
        WaitUtils.waitForPageToLoad();
    }

    // If false, we don't expect form "Try another way" link available on the page. If true, we expect that it is available on the page
    public void assertTryAnotherWayLinkAvailability(boolean expectedAvailability) {
        try {
            driver.findElement(By.id("try-another-way"));
            Assert.assertTrue(expectedAvailability);
        } catch (NoSuchElementException nse) {
            Assert.assertFalse(expectedAvailability);
        }
    }

    public void clickTryAnotherWayLink() {
        UIUtils.clickLink(tryAnotherWayLink);
    }

    public void assertAccountLinkAvailability(boolean expectedAvailability) {
        try {
            driver.findElement(By.id("account"));
            Assert.assertTrue(expectedAvailability);
        } catch (NoSuchElementException nse) {
            Assert.assertFalse(expectedAvailability);
        }
    }

    public void clickAccountLink() {
        UIUtils.clickLink(accountLink);
    }

    // If false, we don't expect "attempted username" link available on the page. If true, we expect that it is available on the page
    public void assertAttemptedUsernameAvailability(boolean expectedAvailability) {
        assertAttemptedUsernameAvailability(driver, expectedAvailability);
    }

    public static void assertAttemptedUsernameAvailability(WebDriver driver, boolean expectedAvailability) {
        try {
            driver.findElement(By.id("kc-attempted-username"));
            Assert.assertTrue(expectedAvailability);
            // make sure the username field is not shown if the attempted username field is present
            Assert.assertTrue(driver.findElements(By.id("username")).isEmpty());
        } catch (NoSuchElementException nse) {
            Assert.assertFalse(expectedAvailability);
        }
    }

    public String getAttemptedUsername() {
        String text = attemptedUsernameLabel.getAttribute("value");
        if (text == null) return attemptedUsernameLabel.getText();
        return text;
    }

    public void clickResetLogin() {
        UIUtils.clickLink(resetLoginLink);
    }
}
