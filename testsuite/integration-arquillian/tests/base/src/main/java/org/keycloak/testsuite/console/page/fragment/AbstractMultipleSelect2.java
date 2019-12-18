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
import org.jboss.arquillian.graphene.fragment.Root;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractMultipleSelect2<R> {

    @Root
    private WebElement root;

    @Drone
    private WebDriver driver;

    @FindBy(xpath = ".//input[contains(@class,'ui-select-search')]")
    private WebElement search;

    @FindBy(xpath = ".//div[contains(@class,'ui-select-choices-row')]")
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
        pause(500);
        root.click();
        pause(500);

        String id = identity().apply(value);

        search.sendKeys(id);
        waitForPageToLoad();

        if (result.isEmpty()) {
            search.sendKeys(Keys.ESCAPE);
            return;
        }

        for (WebElement result : result) {
            if (result.getText().equalsIgnoreCase(id)) {
                clickLink(result);

                // Send escape as a workaround to close option list in multi selects.
                // Otherwise, other elements might be hidden (e.g. RequiredUserActionsTest -> submit button)
                if (search.isDisplayed()) {
                    search.sendKeys(Keys.ESCAPE);
                }
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
            List<WebElement> matchedSelections = selected.findElements(By.xpath(".//span[text()='" + value + "']/../span[contains(@class, 'ui-select-match-close')]"));
            if (matchedSelections.size() != 1) {
                return false;
            }

            matchedSelections.get(0).click();
            pause(500);

            return true;
        };
    }

    protected WebElement getRoot() {
        return root;
    }

    protected WebDriver getDriver() {
        return driver;
    }
}
