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

import java.util.Set;
import java.util.function.Function;

import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.testsuite.console.page.fragment.MultipleStringSelect2;
import org.keycloak.testsuite.console.page.fragment.SingleStringSelect2;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ScopePermissionForm extends Form {

    @FindBy(id = "name")
    private WebElement name;

    @FindBy(id = "description")
    private WebElement description;

    @FindBy(id = "decisionStrategy")
    private Select decisionStrategy;

    @FindBy(xpath = "//i[contains(@class,'pficon-delete')]")
    private WebElement deleteButton;

    @FindBy(xpath = ACTIVE_DIV_XPATH + "/button[text()='Delete']")
    private WebElement confirmDelete;

    @FindBy(id = "s2id_policies")
    private MultipleStringSelect2 policySelect;

    @FindBy(id = "s2id_scopes")
    private MultipleStringSelect2 scopeSelect;

    @FindBy(id = "s2id_resourceScopes")
    private MultipleStringSelect2 resourceScopeSelect;

    @FindBy(id = "s2id_resources")
    private ResourceSelect resourceSelect;

    public void populate(ScopePermissionRepresentation expected) {
        setInputValue(name, expected.getName());
        setInputValue(description, expected.getDescription());
        decisionStrategy.selectByValue(expected.getDecisionStrategy().name());

        Set<String> resources = expected.getResources();

        if (resources != null && !resources.isEmpty()) {
            resourceSelect.update(resources);
            resourceScopeSelect.update(expected.getScopes());
        } else {
            scopeSelect.update(expected.getScopes());
        }

        policySelect.update(expected.getPolicies());

        save();
    }

    public void delete() {
        deleteButton.click();
        confirmDelete.click();
    }

    public ScopePermissionRepresentation toRepresentation() {
        ScopePermissionRepresentation representation = new ScopePermissionRepresentation();

        representation.setName(getInputValue(name));
        representation.setDescription(getInputValue(description));
        representation.setDecisionStrategy(DecisionStrategy.valueOf(decisionStrategy.getFirstSelectedOption().getText().toUpperCase()));
        representation.setPolicies(policySelect.getSelected());
        representation.setResources(resourceSelect.getSelected());
        representation.setScopes(scopeSelect.getSelected());
        representation.getScopes().addAll(resourceScopeSelect.getSelected());

        return representation;
    }

    public class ResourceSelect extends SingleStringSelect2 {
        @Override
        protected Function<WebElement, String> representation() {
            return super.representation().andThen(s -> "".equals(s) || s.contains("Any resource...") ? null : s);
        }
    }
}