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

import org.keycloak.testsuite.console.page.fragment.DataTable;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ClientPolicy extends BaseClientPoliciesPage {
    private static final String NAME = "name";

    @FindBy(tagName = "form")
    private ClientPolicyForm form;

    @FindBy(xpath = "(.//table)[1]")
    private ConditionsTable conditionsTable;

    @FindBy(xpath = "(.//table)[2]")
    private ProfilesTable profilesTable;

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/policies-update/{" + NAME + "}";
    }

    public void setPolicyName(String name) {
        setUriParameter(NAME, name);
    }

    public String getPolicyName() {
        return getUriParameter(NAME).toString();
    }

    public ClientPolicyForm form() {
        return form;
    }

    public ConditionsTable conditionsTable() {
        return conditionsTable;
    }

    public ProfilesTable profilesTable() {
        return profilesTable;
    }

    public static class ConditionsTable extends DataTable {
        public void clickCreateCondition() {
            clickHeaderLink("Create");
        }

        public void clickEditCondition(String conditionType) {
            clickRowActionButton(conditionType, "Edit");
        }

        public void clickDeleteCondition(String conditionType) {
            clickRowActionButton(conditionType, "Delete");
        }
    }

    public static class ProfilesTable extends DataTable {
        @FindBy(tagName = "select")
        private Select addProfileSelect;

        public List<String> getProfiles() {
            return rows().stream().map(r -> getColumnText(r, 0)).collect(Collectors.toList());
        }

        public void clickProfile(String profileName) {
            clickRowByLinkText(profileName);
        }

        public void clickDeleteProfile(String profileName) {
            clickRowActionButton(profileName, "Delete");
        }

        public void addProfile(String profileName) {
            addProfileSelect.selectByVisibleText(profileName);
        }
    }
}
