/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.console.page.clients.authorization.policy;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.openqa.selenium.By.tagName;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.testsuite.console.page.fragment.MultipleStringSelect2;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class PolicySelect extends MultipleStringSelect2 {

    @Override
    protected List<WebElement> getSelectedElements() {
        return getRoot().findElements(By.xpath("(//table[@id='selected-policies'])/tbody/tr")).stream()
                .filter(webElement -> webElement.findElements(tagName("td")).size() > 1)
                .collect(Collectors.toList());
    }

    @Override
    protected BiFunction<WebElement, String, Boolean> deselect() {
        return (webElement, name) -> {
            List<WebElement> tds = webElement.findElements(tagName("td"));

            if (!getTextFromElement(tds.get(0)).isEmpty()) {
                if (getTextFromElement(tds.get(0)).equals(name)) {
                    tds.get(3).click();
                    return true;
                }
            }

            return false;
        };
    }

    @Override
    protected Function<WebElement, String> representation() {
        return webElement -> getTextFromElement(webElement.findElements(tagName("td")).get(0));
    }
}
