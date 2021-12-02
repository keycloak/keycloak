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

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.openqa.selenium.By.tagName;
import static org.openqa.selenium.By.xpath;

/**
 *
 * @author tkyjovsk
 */
public class DataTable {

    @Drone
    protected WebDriver driver;

    @FindBy(css = "input[class*='search']")
    private WebElement searchInput;
    @FindBy(css = "div[class='input-group-addon'] i")
    private WebElement searchButton;

    @FindBy(tagName = "thead")
    private WebElement header;
    @FindBy(css = "tbody")
    private WebElement body;
    @FindBy(xpath = "tbody/tr[@class='ng-scope']")
    private List<WebElement> rows;

    @FindBy(tagName = "tfoot")
    private WebElement footer;

    public void search(String pattern) {
        searchInput.sendKeys(pattern);
        clickLink(searchButton);
    }

    public void clickHeaderButton(String buttonText) {
        clickLink(header.findElement(By.xpath(".//button[text()='" + buttonText + "']")));
    }

    public void clickHeaderLink(String linkText) {
        clickLink(header.findElement(By.linkText(linkText)));
    }

    public WebElement body() {
        return body;
    }

    public WebElement footer() {
        return footer;
    }


    public List<WebElement> rows() {
        waitForPageToLoad();
        pause(500); // wait a bit to ensure the table is no more changing
        return rows;
    }

    public WebElement getRowByLinkText(String text) {
        WebElement row = body.findElement(By.xpath(".//tr[./td/a[text()='" + text + "']]"));
        return row;
    }

    public void clickRowByLinkText(String text) {
        clickLink(body.findElement(By.xpath(".//tr/td/a[text()='" + text + "']")));
    }

    public WebElement getActionButton(WebElement row, String buttonText) {
        return row.findElement(xpath(".//td[contains(@class, 'kc-action-cell') and text()='" + buttonText + "']"));
    }

    public WebElement getActionButton(String rowLinkText, String buttonText) {
        return getActionButton(getRowByLinkText(rowLinkText), buttonText);
    }

    public boolean isActionButtonVisible(String rowLinkText, String buttonText) {
        try {
            return getActionButton(rowLinkText, buttonText).isDisplayed();
        }
        catch (NoSuchElementException e) {
            return false;
        }
    }

    public void clickRowActionButton(WebElement row, String buttonText) {
        clickLink(getActionButton(row, buttonText));
    }

    public void clickRowActionButton(String rowLinkText, String buttonText) {
        clickLink(getActionButton(rowLinkText, buttonText));
    }

    public String getColumnText(WebElement row, int colIndex) {
        return getTextFromElement(row.findElements(tagName("td")).get(colIndex));
    }

    public String getColumnText(String rowLinkText, int colIndex) {
        return getColumnText(getRowByLinkText(rowLinkText), colIndex);
    }

    public boolean isRowPresent(String rowLinkText) {
        try {
            return getRowByLinkText(rowLinkText).isDisplayed();
        }
        catch (NoSuchElementException e) {
            return false;
        }
    }
    
}
