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
package org.keycloak.testsuite.console.page.clients.authorization.policy;

import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JSPolicyForm extends Form {

    @FindBy(id = "name")
    private WebElement name;

    @FindBy(id = "description")
    private WebElement description;

    @FindBy(id = "logic")
    private Select logic;

    @FindBy(xpath = "//i[contains(@class,'pficon-delete')]")
    private WebElement deleteButton;

    @FindBy(xpath = ACTIVE_DIV_XPATH + "/button[text()='Delete']")
    private WebElement confirmDelete;

    public void populate(JSPolicyRepresentation expected) {
        setInputValue(name, expected.getName());
        setInputValue(description, expected.getDescription());
        logic.selectByValue(expected.getLogic().name());

        JavascriptExecutor scriptExecutor = (JavascriptExecutor) driver;

        scriptExecutor.executeScript("angular.element(document.getElementById('code')).scope().policy.code = '" + expected.getCode() + "'");

        save();
    }

    public void delete() {
        deleteButton.click();
        confirmDelete.click();
    }

    public JSPolicyRepresentation toRepresentation() {
        JSPolicyRepresentation representation = new JSPolicyRepresentation();

        representation.setName(getInputValue(name));
        representation.setDescription(getInputValue(description));
        representation.setLogic(Logic.valueOf(logic.getFirstSelectedOption().getText().toUpperCase()));

        JavascriptExecutor scriptExecutor = (JavascriptExecutor) driver;

        representation.setCode((String) scriptExecutor.executeScript("return angular.element(document.getElementById('code')).scope().policy.code;"));

        return representation;
    }
}