/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.setTextInputValue;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class KcPassword {
    @Root
    private WebElement inputField;

    @FindBy(xpath = "../span[contains(@class,'input-group-addon') and ./span[contains(@class,'fa-eye')]]")
    private WebElement eyeButton;

    public void setValue(final String value) {
        setTextInputValue(inputField, value);
    }

    public boolean isMasked() {
        return inputField.getAttribute("class").contains("password-conceal");
    }

    public boolean isEyeButtonDisabled() {
        return eyeButton.getAttribute("class").contains("disabled");
    }

    public void clickEyeButton() {
        if (isEyeButtonDisabled()) {
            throw new ElementNotInteractableException("The eye button is disabled and cannot be clicked");
        }
        eyeButton.click();
    }

    public WebElement getElement() {
        return inputField;
    }
}
