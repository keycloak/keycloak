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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;

import static org.keycloak.testsuite.util.UIUtils.getTextInputValue;

/**
 *
 * @author tkyjovsk
 */
public class InputList {
    
    @FindBy(xpath=".//input[@ng-model='client.redirectUris[i]']")
    private List<WebElement> inputs;
    
    public List<String> getValues() {
        List<String> values = new ArrayList<>();
        for (WebElement input: inputs) {
            values.add(getTextInputValue(input));
        }
        return values;
    }
    
    
    
}
