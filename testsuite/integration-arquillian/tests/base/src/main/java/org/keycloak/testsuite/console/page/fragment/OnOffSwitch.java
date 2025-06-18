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

import org.jboss.arquillian.graphene.fragment.Root;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.isElementVisible;

/**
 *
 * @author Petr Mensik
 */
public class OnOffSwitch {

    @Root
    private WebElement root;

    @FindBy(tagName = "input")
    private WebElement inputTag;

    @FindBy(tagName = "label")
    private WebElement labelTag;

    public boolean isOn() {
        return inputTag.isSelected();
    }

    private void click() {
        labelTag.click();
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

    public boolean isVisible() {
        return isElementVisible(labelTag);
    }

}
