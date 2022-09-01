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

import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.testsuite.page.Form;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.io.IOException;

import static org.keycloak.testsuite.util.UIUtils.getTextInputValue;
import static org.keycloak.testsuite.util.UIUtils.setTextInputValue;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ClientPoliciesJson extends BaseClientPoliciesPage {
    @FindBy(tagName = "form")
    private JsonForm form;

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/policies-json";
    }

    public JsonForm form() {
        return form;
    }

    public static class JsonForm extends Form {
        @FindBy(id = "clientPoliciesConfig")
        private WebElement textarea;

        @FindBy(xpath = ".//button[text()='Save']")
        private WebElement saveBtn;

        public ClientPoliciesRepresentation getPolicies() {
            try {
                return JsonSerialization.readValue(getPoliciesAsString(), ClientPoliciesRepresentation.class);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public String getPoliciesAsString() {
            return getTextInputValue(textarea);
        }

        public void setPolicies(ClientPoliciesRepresentation policies) {
            try {
                setPoliciesAsString(JsonSerialization.writeValueAsPrettyString(policies));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void setPoliciesAsString(String policies) {
            setTextInputValue(textarea, policies);
        }

        @Override
        public WebElement saveBtn() {
            return saveBtn;
        }
    }
}
