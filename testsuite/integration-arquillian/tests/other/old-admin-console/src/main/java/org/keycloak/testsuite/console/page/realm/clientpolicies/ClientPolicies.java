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

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ClientPolicies extends BaseClientPoliciesPage {
    @FindBy(tagName = "table")
    private PoliciesTable policiesTable;

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/policies";
    }

    public PoliciesTable policiesTable() {
        return policiesTable;
    }

    public static class PoliciesTable extends DataTable {
        public void clickCreatePolicy() {
            clickHeaderLink("Create");
        }

        public void clickEditPolicy(String policyName) {
            clickRowActionButton(policyName, "Edit");
        }

        public void clickDeletePolicy(String policyName) {
            clickRowActionButton(policyName, "Delete");
        }

        public String getDescription(String policyName) {
            return getColumnText(policyName, 1);
        }

        public boolean isEnabled(String policyName) {
            return Boolean.parseBoolean(getColumnText(policyName, 2));
        }
    }
}
