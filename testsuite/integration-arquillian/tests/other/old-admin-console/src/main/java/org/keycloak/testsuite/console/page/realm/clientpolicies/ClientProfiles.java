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
public class ClientProfiles extends BaseClientPoliciesPage {
    @FindBy(tagName = "table")
    private ProfilesTable profilesTable;

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/profiles";
    }

    public ProfilesTable profilesTable() {
        return profilesTable;
    }

    public static class ProfilesTable extends DataTable {
        public void clickCreateProfile() {
            clickHeaderLink("Create");
        }

        public void clickEditProfile(String profileName) {
            clickRowActionButton(profileName, "Edit");
        }

        public void clickDeleteProfile(String profileName) {
            clickRowActionButton(profileName, "Delete");
        }

        public boolean isDeleteBtnPresent(String profileName) {
            return isActionButtonVisible(profileName, "Delete");
        }

        public String getDescription(String profileName) {
            return getColumnText(profileName, 1);
        }

        public boolean isGlobal(String profileName) {
            return Boolean.parseBoolean(getColumnText(profileName, 2));
        }
    }
}
