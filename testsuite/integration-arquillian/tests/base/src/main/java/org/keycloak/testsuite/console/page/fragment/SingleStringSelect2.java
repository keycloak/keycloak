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

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class SingleStringSelect2 extends AbstractMultipleSelect2<String> {

    @Override
    protected Function<String, String> identity() {
        return r -> r.toString();
    }

    @Override
    protected List<WebElement> getSelectedElements() {
        return getRoot().findElements(By.xpath(".//span[contains(@class,'select2-chosen')]"));
    }

    @Override
    protected Function<WebElement, String> representation() {
        return webElement -> {
            String value = webElement.getText();
            return "".equals(value) ? null : value;
        };
    }

    @Override
    protected BiFunction<WebElement, String, Boolean> deselect() {
        return (selected, value) -> {
            if (identity().apply(value).equals(selected.getText())) {
                WebElement element = selected.findElement(By.xpath(".//a[contains(@class,'select2-search-choice-close')]"));
                JavascriptExecutor executor = (JavascriptExecutor) getDriver();
                executor.executeScript("arguments[0].click();", element);
                WaitUtils.pause(500);
                return true;
            }
            return false;
        };
    }
}
