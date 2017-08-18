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

import static org.openqa.selenium.By.tagName;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
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
public class ClientPolicyForm extends Form {

    @FindBy(id = "name")
    private WebElement name;

    @FindBy(id = "description")
    private WebElement description;

    @FindBy(id = "logic")
    private Select logic;

    @FindBy(xpath = "//i[contains(@class,'pficon-delete')]")
    private WebElement deleteButton;

    @FindBy(id = "s2id_clients")
    private ClientSelect clientsInput;

    @FindBy(xpath = "//div[@class='modal-dialog']")
    protected ModalDialog modalDialog;

    public void populate(ClientPolicyRepresentation expected) {
        setInputValue(name, expected.getName());
        setInputValue(description, expected.getDescription());
        logic.selectByValue(expected.getLogic().name());

        clientsInput.update(expected.getClients());

        save();
    }

    public void delete() {
        deleteButton.click();
        modalDialog.confirmDeletion();
    }

    public ClientPolicyRepresentation toRepresentation() {
        ClientPolicyRepresentation representation = new ClientPolicyRepresentation();

        representation.setName(getInputValue(name));
        representation.setDescription(getInputValue(description));
        representation.setLogic(Logic.valueOf(logic.getFirstSelectedOption().getText().toUpperCase()));
        representation.setClients(clientsInput.getSelected());

        return representation;
    }

    public class ClientSelect extends MultipleStringSelect2 {

        @Override
        protected List<WebElement> getSelectedElements() {
            return getRoot().findElements(By.xpath("(//table[@id='selected-clients'])/tbody/tr")).stream()
                    .filter(webElement -> webElement.findElements(tagName("td")).size() > 1)
                    .collect(Collectors.toList());
        }

        @Override
        protected BiFunction<WebElement, String, Boolean> deselect() {
            return (webElement, name) -> {
                List<WebElement> tds = webElement.findElements(tagName("td"));

                if (!tds.get(0).getText().isEmpty()) {
                    if (tds.get(0).getText().equals(name)) {
                        tds.get(1).findElement(By.tagName("button")).click();
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