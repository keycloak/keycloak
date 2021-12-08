/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.console.page.idp.mappers;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;
import java.util.NoSuchElementException;

import static org.keycloak.testsuite.util.UIUtils.getTextInputValue;
import static org.keycloak.testsuite.util.UIUtils.setTextInputValue;

/**
 * @author mabartos
 */
public class MultivaluedStringProperty {

    @FindBy(xpath = "//input[@ng-model='config[option.name][i]']")
    private List<WebElement> items;

    @FindBy(xpath = "//button[@data-ng-click='deleteValueFromMultivalued(option.name, $index)']")
    private List<WebElement> minusButtons;

    @FindBy(xpath = "//button[@data-ng-click='addValueToMultivalued(option.name)']")
    private WebElement plusButton;

    protected List<WebElement> getMinusButtons() {
        return minusButtons;
    }

    protected WebElement getPlusButton() {
        return plusButton;
    }

    public boolean isPresent() {
        try {
            return getPlusButton().isDisplayed() && getItems() != null && !getItems().isEmpty();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public void clickAddItem() {
        getPlusButton().click();
    }

    public List<WebElement> getItems() {
        return items;
    }

    public String getItem(int index) {
        validateIndex(index);
        return getTextInputValue(getItems().get(index));
    }

    public void editItem(int index, String item) {
        validateIndex(index);
        setTextInputValue(getItems().get(index), item);
    }

    public void addItem(String item) {
        clickAddItem();

        final List<WebElement> items = getItems();
        final int index = items.size() - 1;

        validateIndex(index);
        WebElement webElement = items.get(index);
        setTextInputValue(webElement, item);
    }

    public void removeItem(int index) {
        validateIndex(index);
        if (index == getItems().size() - 1) {
            editItem(index, "");
        } else {
            getMinusButtons().get(index).click();
        }
    }

    private void validateIndex(int index) {
        if (index < 0 || index >= getItems().size())
            throw new AssertionError("Input with index: " + index + " does not exist.");
    }
}
