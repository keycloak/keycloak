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
package org.keycloak.testsuite.console.page.clients.authorization.permission;

import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.testsuite.console.page.fragment.ModalDialog;
import org.keycloak.testsuite.console.page.fragment.MultipleStringSelect2;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourcePermissionForm extends Form {

    @FindBy(id = "name")
    private WebElement name;

    @FindBy(id = "description")
    private WebElement description;

    @FindBy(id = "decisionStrategy")
    private Select decisionStrategy;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='applyToResourceTypeFlag']]")
    private OnOffSwitch resourceTypeSwitch;

    @FindBy(id = "resourceType")
    private WebElement resourceType;

    @FindBy(xpath = "//i[contains(@class,'pficon-delete')]")
    private WebElement deleteButton;

    @FindBy(xpath = "//div[@class='modal-dialog']")
    protected ModalDialog modalDialog;

    @FindBy(id = "s2id_policies")
    private MultipleStringSelect2 policySelect;

    @FindBy(id = "s2id_resources")
    private MultipleStringSelect2 resourceSelect;

    public void populate(ResourcePermissionRepresentation expected) {
        setInputValue(name, expected.getName());
        setInputValue(description, expected.getDescription());
        decisionStrategy.selectByValue(expected.getDecisionStrategy().name());

        resourceTypeSwitch.setOn(expected.getResourceType() != null);

        if (expected.getResourceType() != null) {
            setInputValue(resourceType, expected.getResourceType());
        } else {
            resourceTypeSwitch.setOn(false);
            resourceSelect.update(expected.getResources());
        }

        policySelect.update(expected.getPolicies());

        save();
    }

    public void delete() {
        deleteButton.click();
        modalDialog.confirmDeletion();
    }

    public ResourcePermissionRepresentation toRepresentation() {
        ResourcePermissionRepresentation representation = new ResourcePermissionRepresentation();

        representation.setName(getInputValue(name));
        representation.setDescription(getInputValue(description));
        representation.setDecisionStrategy(DecisionStrategy.valueOf(decisionStrategy.getFirstSelectedOption().getText().toUpperCase()));
        representation.setPolicies(policySelect.getSelected());
        String inputValue = getInputValue(resourceType);

        if (!"".equals(inputValue)) {
            representation.setResourceType(inputValue);
        }

        representation.setResources(resourceSelect.getSelected());

        return representation;
    }
}