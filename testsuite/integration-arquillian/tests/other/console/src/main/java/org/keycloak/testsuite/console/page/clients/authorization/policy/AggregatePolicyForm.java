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

import static org.keycloak.testsuite.util.UIUtils.performOperationWithPageReload;
import static org.openqa.selenium.By.tagName;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.RulePolicyRepresentation;
import org.keycloak.representations.idm.authorization.TimePolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testsuite.console.page.fragment.ModalDialog;
import org.keycloak.testsuite.console.page.fragment.MultipleStringSelect2;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.By;
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
    private PolicySelect policySelect;

    @FindBy(xpath = "//div[@class='modal-dialog']")
    protected ModalDialog modalDialog;

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
    private RulePolicy rulePolicy;

    @Page
    private GroupPolicy groupPolicy;

    public void populate(AggregatePolicyRepresentation expected, boolean save) {
        setInputValue(name, expected.getName());
        setInputValue(description, expected.getDescription());
        logic.selectByValue(expected.getLogic().name());

        Set<String> selectedPolicies = policySelect.getSelected();
        Set<String> policies = expected.getPolicies();

        if (policies != null) {
            for (String policy : policies) {
                if (!selectedPolicies.contains(policy)) {
                    policySelect.select(policy);
                }
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

        if (save) {
            save();
        }
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
        } else if ("rules".equalsIgnoreCase(expected.getType())) {
            rulePolicy.form().populate((RulePolicyRepresentation) expected, true);
        } else if ("group".equalsIgnoreCase(expected.getType())) {
            groupPolicy.form().populate((GroupPolicyRepresentation) expected, true);
        }
    }

    public class PolicySelect extends MultipleStringSelect2 {

        @Override
        protected List<WebElement> getSelectedElements() {
            return getRoot().findElements(By.xpath("(//table[@id='selected-policies'])/tbody/tr")).stream()
                    .filter(webElement -> webElement.findElements(tagName("td")).size() > 1)
                    .collect(Collectors.toList());
        }

        @Override
        protected BiFunction<WebElement, String, Boolean> deselect() {
            return (webElement, name) -> {
                List<WebElement> tds = webElement.findElements(tagName("td"));

                if (!tds.get(0).getText().isEmpty()) {
                    if (tds.get(0).getText().equals(name)) {
                        tds.get(3).click();
                        return true;
                    }
                }

                return false;
            };
        }

        @Override
        protected Function<WebElement, String> representation() {
            return webElement -> webElement.findElements(tagName("td")).get(0).getText();
        }
    }
}