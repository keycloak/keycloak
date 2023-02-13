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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author Petr Mensik
 */

public class PickList {

    @Root
    private WebElement root;

    private Select firstSelect;
    private Select secondSelect;
    
    @FindBy(className = "kc-icon-arrow-right")
    private WebElement rightArrow;
    
    @FindBy(className = "kc-icon-arrow-left")
    private WebElement leftArrow;
    
    public void addItems(String... values) {
        for(String value : values) {
            firstSelect.selectByVisibleText(value);
        }
        rightArrow.click();
    }
    
    public void setFirstSelect(By locator) {
        firstSelect = new Select(root.findElement(locator));
    }
    
    public void setSecondSelect(By locator) {
        secondSelect = new Select(root.findElement(locator));
    }
    
}
