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

package org.keycloak.testsuite.console.page.events;

import org.keycloak.testsuite.console.page.fragment.DataTable;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author tkyjovsk
 * @author mhajas
 */
public class AdminEvents extends Events {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/admin-events";
    }

    @FindBy(tagName = "table")
    private AdminEventsTable table;

    public AdminEventsTable table() {
        return table;
    }

    public class AdminEventsTable extends DataTable {

        @FindBy(xpath = "//button[text()[contains(.,'Filter')]]")
        private WebElement filterButton;

        @FindBy(tagName = "form")
        private AdminEventsTableFilterForm filterForm;

        public void update() {
            clickHeaderButton("Update");
        }

        public void reset() {
            clickHeaderButton("Reset");
        }

        public void filter() {
            filterButton.click();
        }

        public AdminEventsTableFilterForm filterForm() {
            return filterForm;
        }

        public class AdminEventsTableFilterForm extends Form {

            @FindBy(id = "resource")
            private WebElement resourcePathInput;

            @FindBy(id = "realm")
            private WebElement realmInput;

            @FindBy(id = "client")
            private WebElement clientInput;

            @FindBy(id = "user")
            private WebElement userInput;

            @FindBy(id = "ipAddress")
            private WebElement ipAddressInput;

            @FindBy(xpath = "//div[@id='s2id_adminEnabledEventOperations']/ul")
            private WebElement operationTypesInput;

            @FindBy(xpath = "//div[@id='select2-drop']")
            private WebElement operationTypesValues;

            public void addOperationType(String type) {
                operationTypesInput.click();
                operationTypesValues.findElement(By.xpath("//div[text() = '" + type + "']")).click();
            }

            public void removeOperationType(String type) {
                operationTypesInput.findElement(By.xpath("//div[text()='" + type + "']/../a")).click();
            }

            public void setResourcePathInput(String value) {
                UIUtils.setTextInputValue(resourcePathInput, value);
            }

            public void setRealmInput(String value) {
                UIUtils.setTextInputValue(realmInput, value);
            }

            public void setClientInput(String value) {
                UIUtils.setTextInputValue(clientInput, value);
            }

            public void setUserInput(String value) {
                UIUtils.setTextInputValue(userInput, value);
            }

            public void setIpAddressInput(String value) {
                UIUtils.setTextInputValue(ipAddressInput, value);
            }
        }

    }

}
