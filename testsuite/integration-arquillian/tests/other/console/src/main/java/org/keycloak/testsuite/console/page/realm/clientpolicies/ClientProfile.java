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
public class ClientProfile extends BaseClientPoliciesPage {
    private static final String NAME = "name";

    @FindBy(tagName = "form")
    private ClientProfileForm form;

    @FindBy(tagName = "table")
    private ExecutorsTable executorsTable;

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/profiles-update/{" + NAME + "}";
    }

    public void setProfileName(String name) {
        setUriParameter(NAME, name);
    }

    public String getProfileName() {
        return getUriParameter(NAME).toString();
    }

    public ClientProfileForm form() {
        return form;
    }

    public ExecutorsTable executorsTable() {
        return executorsTable;
    }

    public static class ExecutorsTable extends DataTable {
        public void clickCreateExecutor() {
            clickHeaderLink("Create");
        }

        public void clickEditExecutor(String executorType) {
            clickRowActionButton(executorType, "Edit");
        }

        public void clickDeleteExecutor(String executorType) {
            clickRowActionButton(executorType, "Delete");
        }

        public boolean isDeleteBtnPresent(String executorType) {
            return isActionButtonVisible(executorType, "Delete");
        }
    }
}
