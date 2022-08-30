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

import org.keycloak.representations.idm.ClientProfilesRepresentation;
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
public class ClientProfilesJson extends BaseClientPoliciesPage {
    @FindBy(tagName = "form")
    private JsonForm form;

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/profiles-json";
    }

    public JsonForm form() {
        return form;
    }

    public static class JsonForm extends Form {
        @FindBy(id = "clientProfilesConfig")
        private WebElement textarea;

        @FindBy(xpath = ".//button[text()='Save']")
        private WebElement saveBtn;

        public ClientProfilesRepresentation getProfiles() {
            try {
                return JsonSerialization.readValue(getProfilesAsString(), ClientProfilesRepresentation.class);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public String getProfilesAsString() {
            return getTextInputValue(textarea);
        }

        public void setProfiles(ClientProfilesRepresentation profiles) {
            try {
                setProfilesAsString(JsonSerialization.writeValueAsPrettyString(profiles));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void setProfilesAsString(String profiles) {
            setTextInputValue(textarea, profiles);
        }

        @Override
        public WebElement saveBtn() {
            return saveBtn;
        }
    }
}
