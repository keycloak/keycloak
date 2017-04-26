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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.fragment.Root;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractMultipleSelect2<R> {

    @Root
    private WebElement root;

    @Drone
    private WebDriver driver;

    @FindBy(xpath = ".//input[contains(@class,'select2-input')]")
    private WebElement search;

    @FindBy(xpath = "//div[contains(@class,'select2-result-label')]")
    private List<WebElement> result;

    public void update(Set<R> values) {
        Set<R> selection = getSelected();

        for (R value : values) {
            if (!selection.contains(value)) {
                select(value);
            }
        }

        for (R selected : selection) {
            boolean isSelected = false;

            for (R value : values) {
                if (selected.equals(value)) {
                    isSelected = true;
                    break;
                }
            }

            if (!isSelected) {
                deselect(selected);
            }
        }
    }

    public void select(R value) {
        root.click();
        WaitUtils.pause(500);

        String id = identity().apply(value);

        Actions actions = new Actions(driver);
        actions.sendKeys(id).perform();
        WaitUtils.pause(500);

        if (result.isEmpty()) {
            actions.sendKeys(Keys.ESCAPE).perform();
            return;
        }

        for (WebElement result : result) {
            if (result.getText().equalsIgnoreCase(id)) {
                result.click();
                return;
            }
        }
    }

    protected abstract Function<R, String> identity();

    public Set<R> getSelected() {
        Set<R> values = new HashSet<>();

        for (WebElement selected : getSelectedElements()) {
            R value = representation().apply(selected);

            if (value != null) {
                values.add(value);
            }
        }

        return values;
    }

    protected abstract List<WebElement> getSelectedElements();

    protected abstract Function<WebElement, R> representation();

    public void deselect(R value) {
        onDeselect(value);
    }

    protected void onDeselect(R value) {
        for (WebElement selected : getSelectedElements()) {
            if (deselect().apply(selected, value)) {
                return;
            }
        }
    }

    protected BiFunction<WebElement, R, Boolean> deselect() {
        return (selected, value) -> {
            WebElement selection = selected.findElements(By.tagName("div")).get(0);
            if (identity().apply(value).equals(selection.getText())) {
                WebElement element = selected.findElement(By.xpath(".//a[contains(@class,'select2-search-choice-close')]"));
                JavascriptExecutor executor = (JavascriptExecutor) driver;
                executor.executeScript("arguments[0].click();", element);
                WaitUtils.pause(500);
                return true;
            }
            return false;
        };
    }

    protected WebElement getRoot() {
        return root;
    }

    protected WebDriver getDriver() {
        return driver;
    }
}
