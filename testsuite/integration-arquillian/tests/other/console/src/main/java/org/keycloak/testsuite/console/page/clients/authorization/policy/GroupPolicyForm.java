/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import static org.openqa.selenium.By.tagName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class GroupPolicyForm extends Form {

    @FindBy(id = "name")
    private WebElement name;

    @FindBy(id = "description")
    private WebElement description;

    @FindBy(id = "groupsClaim")
    private WebElement groupsClaim;

    @FindBy(id = "logic")
    private Select logic;

    @FindBy(xpath = "//i[contains(@class,'pficon-delete')]")
    private WebElement deleteButton;

    @FindBy(xpath = ACTIVE_DIV_XPATH + "/button[text()='Delete']")
    private WebElement confirmDelete;

    @FindBy(id = "selectGroup")
    private WebElement selectGroupButton;

    @Drone
    private WebDriver driver;

    public void populate(GroupPolicyRepresentation expected) {
        setInputValue(name, expected.getName());
        setInputValue(description, expected.getDescription());
        setInputValue(groupsClaim, expected.getGroupsClaim());
        logic.selectByValue(expected.getLogic().name());


        for (GroupPolicyRepresentation.GroupDefinition definition : toRepresentation().getGroups()) {
            boolean isExpected = false;

            for (GroupPolicyRepresentation.GroupDefinition expectedDef : expected.getGroups()) {
                if (definition.getPath().equals(expectedDef.getPath())) {
                    isExpected = true;
                    break;
                }
            }

            if (!isExpected) {
                unselect(definition.getPath());
            }
        }

        for (GroupPolicyRepresentation.GroupDefinition definition : expected.getGroups()) {
            String path = definition.getPath();
            String groupName = path.substring(path.lastIndexOf('/') + 1);
            WebElement element = driver.findElement(By.xpath("//span[text()='" + groupName + "']"));
            element.click();
            selectGroupButton.click();
            driver.findElements(By.xpath("(//table[@id='selected-groups'])/tbody/tr")).stream()
                    .filter(webElement -> webElement.findElements(tagName("td")).size() > 1)
                    .map(webElement -> webElement.findElements(tagName("td")))
                    .filter(tds -> tds.get(0).getText().equals(definition.getPath()))
                    .forEach(tds -> {
                        if (!tds.get(1).findElement(By.tagName("input")).isSelected()) {
                            if (definition.isExtendChildren()) {
                                tds.get(1).findElement(By.tagName("input")).click();
                            }
                        } else {
                            if (!definition.isExtendChildren() && tds.get(1).findElement(By.tagName("input")).isSelected()) {
                                tds.get(1).findElement(By.tagName("input")).click();
                            }
                        }
                    });
        }

        save();
    }

    private void unselect(String path) {
        for (WebElement webElement : driver.findElements(By.xpath("(//table[@id='selected-groups'])/tbody/tr"))) {
            List<WebElement> tds = webElement.findElements(tagName("td"));

            if (tds.size() > 1) {
                if (tds.get(0).getText().equals(path)) {
                    tds.get(2).findElement(By.tagName("button")).click();
                    return;
                }
            }
        }
    }

    public void delete() {
        deleteButton.click();
        confirmDelete.click();
    }

    public GroupPolicyRepresentation toRepresentation() {
        GroupPolicyRepresentation representation = new GroupPolicyRepresentation();

        representation.setName(getInputValue(name));
        representation.setDescription(getInputValue(description));
        representation.setGroupsClaim(getInputValue(groupsClaim));
        representation.setLogic(Logic.valueOf(logic.getFirstSelectedOption().getText().toUpperCase()));
        representation.setGroups(new HashSet<>());

        driver.findElements(By.xpath("(//table[@id='selected-groups'])/tbody/tr")).stream()
                .filter(webElement -> webElement.findElements(tagName("td")).size() > 1)
                .forEach(webElement -> {
                    List<WebElement> tds = webElement.findElements(tagName("td"));
                    representation.addGroupPath(tds.get(0).getText(), tds.get(1).findElement(By.tagName("input")).isSelected());
                });

        return representation;
    }
}