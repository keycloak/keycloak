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
 */

package org.keycloak.testsuite.console.page.realm.clientpolicies;

import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.getTextInputValue;
import static org.keycloak.testsuite.util.UIUtils.setTextInputValue;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ClientPolicyForm extends Form {
    @FindBy(id = "clientPolicyName")
    private WebElement policyNameInput;

    @FindBy(id = "description")
    private WebElement descriptionInput;

    @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='enabled']]")
    private OnOffSwitch enabledSwitch;

    public String getPolicyName() {
        return getTextInputValue(policyNameInput);
    }

    public void setPolicyName(String policyName) {
        setTextInputValue(policyNameInput, policyName);
    }

    public String getDescription() {
        return getTextInputValue(descriptionInput);
    }

    public void setDescription(String description) {
        setTextInputValue(descriptionInput, description);
    }

    public boolean isEnabled() {
        return enabledSwitch.isOn();
    }

    public void setEnabled(boolean enabled) {
        enabledSwitch.setOn(enabled);
    }
}
