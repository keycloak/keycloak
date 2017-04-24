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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.arquillian.graphene.fragment.Root;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
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

    @FindBy(xpath = ACTIVE_DIV_XPATH + "/button[text()='Delete']")
    private WebElement confirmDelete;

    @FindBy(id = "s2id_policies")
    private PolicyInput policyInput;

    @FindBy(id = "s2id_resources")
    private ResourceInput resourceInput;

    public void populate(ResourcePermissionRepresentation expected) {
        setInputValue(name, expected.getName());
        setInputValue(description, expected.getDescription());
        decisionStrategy.selectByValue(expected.getDecisionStrategy().name());

        resourceTypeSwitch.setOn(expected.getResourceType() != null);

        if (expected.getResourceType() != null) {
            setInputValue(resourceType, expected.getResourceType());
        } else {
            resourceTypeSwitch.setOn(false);
            Set<String> selectedResources = resourceInput.getSelected();
            Set<String> resources = expected.getResources();

            for (String resource : resources) {
                if (!selectedResources.contains(resource)) {
                    resourceInput.select(resource);
                }
            }

            for (String selected : selectedResources) {
                boolean isSelected = false;

                for (String resource : resources) {
                    if (selected.equals(resource)) {
                        isSelected = true;
                        break;
                    }
                }

                if (!isSelected) {
                    resourceInput.unSelect(selected, driver);
                }
            }
        }

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

        WaitUtils.pause(1000);

        save();
    }

    public void delete() {
        deleteButton.click();
        confirmDelete.click();
    }

    public ResourcePermissionRepresentation toRepresentation() {
        ResourcePermissionRepresentation representation = new ResourcePermissionRepresentation();

        representation.setName(getInputValue(name));
        representation.setDescription(getInputValue(description));
        representation.setDecisionStrategy(DecisionStrategy.valueOf(decisionStrategy.getFirstSelectedOption().getText().toUpperCase()));
        representation.setPolicies(policyInput.getSelected());
        representation.setResources(resourceInput.getSelected());

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
                WebElement selection = selected.findElements(By.tagName("div")).get(0);
                if (name.equals(selection.getText())) {
                    WebElement element = selection.findElement(By.xpath("//a[contains(@class,'select2-search-choice-close')]"));
                    JavascriptExecutor executor = (JavascriptExecutor) driver;
                    executor.executeScript("arguments[0].click();", element);
                    WaitUtils.pause(1000);
                    return;
                }
            }
        }
    }

    public class ResourceInput {

        @Root
        private WebElement root;

        @FindBy(xpath = "//input[contains(@class,'select2-input')]")
        private WebElement search;

        @FindBy(xpath = "//div[contains(@class,'select2-result-label')]")
        private List<WebElement> result;

        @FindBy(xpath = "//li[contains(@class,'select2-search-choice')]")
        private List<WebElement> selection;

        public void select(String name) {
            root.click();
            WaitUtils.pause(1000);
            setInputValue(search, name);
            WaitUtils.pause(1000);
            if (result.isEmpty()) {
                search.sendKeys(Keys.ESCAPE);
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