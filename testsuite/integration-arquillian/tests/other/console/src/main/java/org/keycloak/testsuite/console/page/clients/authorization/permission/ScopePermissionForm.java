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

import static org.keycloak.testsuite.util.UIUtils.performOperationWithPageReload;

import java.util.Set;
import java.util.function.Function;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.TimePolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testsuite.console.page.clients.authorization.policy.ClientPolicy;
import org.keycloak.testsuite.console.page.clients.authorization.policy.GroupPolicy;
import org.keycloak.testsuite.console.page.clients.authorization.policy.JSPolicy;
import org.keycloak.testsuite.console.page.clients.authorization.policy.PolicySelect;
import org.keycloak.testsuite.console.page.clients.authorization.policy.RolePolicy;
import org.keycloak.testsuite.console.page.clients.authorization.policy.TimePolicy;
import org.keycloak.testsuite.console.page.clients.authorization.policy.UserPolicy;
import org.keycloak.testsuite.console.page.fragment.ModalDialog;
import org.keycloak.testsuite.console.page.fragment.MultipleStringSelect2;
import org.keycloak.testsuite.console.page.fragment.SingleStringSelect2;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
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

    @FindBy(xpath = "//div[@class='modal-dialog']")
    protected ModalDialog modalDialog;

    @FindBy(id = "s2id_policies")
    private PolicySelect policySelect;

    @FindBy(id = "s2id_scopes")
    private MultipleStringSelect2 scopeSelect;

    @FindBy(id = "s2id_resourceScopes")
    private MultipleStringSelect2 resourceScopeSelect;

    @FindBy(id = "s2id_resources")
    private ResourceSelect resourceSelect;

    @FindBy(className = "select2-search-choice-close")
    private WebElement resourceSelectRemoveChoice;

    @FindBy(id = "create-policy")
    private Select createPolicySelect;

    @Page
    private RolePolicy rolePolicy;

    @Page
    private UserPolicy userPolicy;

    @Page
    private ClientPolicy clientPolicy;

    @Page
    private JSPolicy jsPolicy;

    @Page
    private TimePolicy timePolicy;

    @Page
    private GroupPolicy groupPolicy;

    public void populate(ScopePermissionRepresentation expected, boolean save) {
        UIUtils.setTextInputValue(name, expected.getName());
        UIUtils.setTextInputValue(description, expected.getDescription());
        decisionStrategy.selectByValue(expected.getDecisionStrategy().name());

        Set<String> resources = expected.getResources();

        if (resources != null && !resources.isEmpty()) {
            resourceSelect.update(resources);
            resourceScopeSelect.update(expected.getScopes());
        } else {
            if (resourceSelectRemoveChoice.isDisplayed()) {
                resourceSelectRemoveChoice.click();
            }
            scopeSelect.update(expected.getScopes());
        }

        if (expected.getPolicies() != null) {
            policySelect.update(expected.getPolicies());
        }

        if (save) {
            save();
        }
    }

    public void delete() {
        deleteButton.click();
        modalDialog.confirmDeletion();
    }

    public ScopePermissionRepresentation toRepresentation() {
        ScopePermissionRepresentation representation = new ScopePermissionRepresentation();

        representation.setName(UIUtils.getTextInputValue(name));
        representation.setDescription(UIUtils.getTextInputValue(description));
        representation.setDecisionStrategy(DecisionStrategy.valueOf(UIUtils.getTextFromElement(decisionStrategy.getFirstSelectedOption()).toUpperCase()));
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

    public void createPolicy(AbstractPolicyRepresentation expected) {
        performOperationWithPageReload(() -> createPolicySelect.selectByValue(expected.getType()));

        if ("role".equals(expected.getType())) {
            rolePolicy.form().populate((RolePolicyRepresentation) expected, true);
        } else if ("user".equalsIgnoreCase(expected.getType())) {
            userPolicy.form().populate((UserPolicyRepresentation) expected, true);
        } else if ("client".equalsIgnoreCase(expected.getType())) {
            clientPolicy.form().populate((ClientPolicyRepresentation) expected, true);
        } else if ("js".equalsIgnoreCase(expected.getType())) {
            jsPolicy.form().populate((JSPolicyRepresentation) expected, true);
        } else if ("time".equalsIgnoreCase(expected.getType())) {
            timePolicy.form().populate((TimePolicyRepresentation) expected, true);
        } else if ("group".equalsIgnoreCase(expected.getType())) {
            groupPolicy.form().populate((GroupPolicyRepresentation) expected, true);
        }
    }
}