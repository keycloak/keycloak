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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.arquillian.graphene.fragment.Root;
import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
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
    private PolicyInput policyInput;

    @FindBy(xpath = ACTIVE_DIV_XPATH + "/button[text()='Delete']")
    private WebElement confirmDelete;

    public void populate(AggregatePolicyRepresentation expected) {
        setInputValue(name, expected.getName());
        setInputValue(description, expected.getDescription());
        logic.selectByValue(expected.getLogic().name());

        Set<String> selectedPolicies = policyInput.getSelected();
        Set<String> policies = expected.getPolicies();

        for (String policy : policies) {
            if (!selectedPolicies.contains(policy)) {
                policyInput.select(policy, driver);
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
                policyInput.unSelect(selected, driver);
            }
        }

        save();
    }

    public void delete() {
        deleteButton.click();
        confirmDelete.click();
    }

    public AggregatePolicyRepresentation toRepresentation() {
        AggregatePolicyRepresentation representation = new AggregatePolicyRepresentation();

        representation.setName(getInputValue(name));
        representation.setDescription(getInputValue(description));
        representation.setLogic(Logic.valueOf(logic.getFirstSelectedOption().getText().toUpperCase()));
        representation.setPolicies(policyInput.getSelected());

        return representation;
    }

    public class PolicyInput {

        @Root
        private WebElement root;

        @FindBy(xpath = "//input[contains(@class,'select2-input')]")
        private WebElement search;

        @FindBy(xpath = "//div[contains(@class,'select2-result-label')]")
        private List<WebElement> result;

        @FindBy(xpath = "//li[contains(@class,'select2-search-choice')]")
        private List<WebElement> selection;

        public void select(String name, WebDriver driver) {
            root.click();
            WaitUtils.pause(1000);

            Actions actions = new Actions(driver);

            actions.sendKeys(name).perform();
            WaitUtils.pause(1000);

            if (result.isEmpty()) {
                actions.sendKeys(Keys.ESCAPE).perform();
                return;
            }
            for (WebElement result : result) {
                if (result.getText().equalsIgnoreCase(name)) {
                    result.click();
                    return;
                }
            }
        }

        public Set<String> getSelected() {
            HashSet<String> values = new HashSet<>();

            for (WebElement selected : selection) {
                values.add(selected.findElements(By.tagName("div")).get(0).getText());
            }

            return values;
        }

        public void unSelect(String name, WebDriver driver) {
            for (WebElement selected : selection) {
                if (name.equals(selected.findElements(By.tagName("div")).get(0).getText())) {
                    WebElement element = selected.findElement(By.xpath("//a[contains(@class,'select2-search-choice-close')]"));
                    JavascriptExecutor executor = (JavascriptExecutor) driver;
                    executor.executeScript("arguments[0].click();", element);
                    WaitUtils.pause(1000);
                    return;
                }
            }
        }
    }
}