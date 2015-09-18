/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.console.page.fragment;

import org.jboss.arquillian.graphene.fragment.Root;
import org.jboss.arquillian.test.api.ArquillianResource;
import static org.keycloak.testsuite.util.WaitUtils.waitAjaxForElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 *
 * @author Petr Mensik
 */
public class OnOffSwitch {

    @Root
    private WebElement root;

    @ArquillianResource
    private Actions actions;

    public boolean isOn() {
        waitAjaxForElement(root);
        return root.findElement(By.tagName("input")).isSelected();
    }

    private void click() {
        waitAjaxForElement(root);
        actions.moveToElement(root.findElements(By.tagName("span")).get(0))
                .click().build().perform();
    }

    public void toggle() {
        click();
    }

    public void on() {
        if (!isOn()) {
            click();
        }
    }

    public void off() {
        if (isOn()) {
            click();
        }
    }

    public void setOn(boolean on) {
        if ((on && !isOn())
                || (!on && isOn())) {
            click(); // click if requested value differs from the actual value
        }
    }

}
