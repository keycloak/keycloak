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

import java.util.Set;

import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.testsuite.console.page.fragment.ModalDialog;
import org.keycloak.testsuite.console.page.fragment.MultipleStringSelect2;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AggregatePolicyForm extends Form {

    @FindBy(id = "name")
    private WebElement name;

    @FindBy(id = "description")
    private WebElement description;

    @FindBy(id = "logic")
    private Select logic;

    @FindBy(xpath = "//i[contains(@class,'pficon-delete')]")
    private WebElement deleteButton;

    @FindBy(id = "s2id_policies")
    private MultipleStringSelect2 policySelect;

    @FindBy(xpath = "//div[@class='modal-dialog']")
    protected ModalDialog modalDialog;

    public void populate(AggregatePolicyRepresentation expected) {
        setInputValue(name, expected.getName());
        setInputValue(description, expected.getDescription());
        logic.selectByValue(expected.getLogic().name());

        Set<String> selectedPolicies = policySelect.getSelected();
        Set<String> policies = expected.getPolicies();

        for (String policy : policies) {
            if (!selectedPolicies.contains(policy)) {
                policySelect.select(policy);
            }
        }

        for (String selected : selectedPolicies) {
            boolean isSelected = false;

            for (String policy : policies) {
                if (selected.equals(policy)) {
                    isSelected = true;
                    break;
                }
            }

            if (!isSelected) {
                policySelect.deselect(selected);
            }
        }

        save();
    }

    public void delete() {
        deleteButton.click();
        modalDialog.confirmDeletion();
    }

    public AggregatePolicyRepresentation toRepresentation() {
        AggregatePolicyRepresentation representation = new AggregatePolicyRepresentation();

        representation.setName(getInputValue(name));
        representation.setDescription(getInputValue(description));
        representation.setLogic(Logic.valueOf(logic.getFirstSelectedOption().getText().toUpperCase()));
        representation.setPolicies(policySelect.getSelected());

        return representation;
    }
}