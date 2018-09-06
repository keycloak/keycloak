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
package org.keycloak.testsuite.console.page.clients.authorization.resource;

import org.jboss.arquillian.graphene.fragment.Root;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testsuite.console.page.fragment.ModalDialog;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.openqa.selenium.By.tagName;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourceForm extends Form {

    @FindBy(id = "name")
    private WebElement name;

    @FindBy(id = "displayName")
    private WebElement displayName;

    @FindBy(id = "type")
    private WebElement type;

    @FindBy(id = "newUri")
    private WebElement newUri;

    @FindBy(xpath = "//*[@id=\"view\"]/div[1]/form/fieldset/div[5]/div/div/div/button")
    private WebElement addUriButton;

    @FindBy(id = "iconUri")
    private WebElement iconUri;

    @FindBy(id = "resource.owner.name")
    private WebElement owner;

    @FindBy(xpath = "//i[contains(@class,'pficon-delete')]")
    private WebElement deleteButton;

    @FindBy(xpath = "//div[@class='modal-dialog']")
    protected ModalDialog modalDialog;

    @FindBy(id = "s2id_scopes")
    private ScopesInput scopesInput;

    public void populate(ResourceRepresentation expected) {
        while (true) {
            try {
                WebElement e = driver.findElement(By.xpath("//button[@data-ng-click='deleteUri($index)']"));
                e.click();
            } catch (NoSuchElementException e) {
                break;
            }
        }

        UIUtils.setTextInputValue(name, expected.getName());
        UIUtils.setTextInputValue(displayName, expected.getDisplayName());
        UIUtils.setTextInputValue(type, expected.getType());

        for (String uri : expected.getUris()) {
            UIUtils.setTextInputValue(newUri, uri);
            addUriButton.click();
        }

        UIUtils.setTextInputValue(iconUri, expected.getIconUri());

        Set<ScopeRepresentation> scopes = expected.getScopes();

        for (ScopeRepresentation scope : scopes) {
            scopesInput.select(scope.getName());
        }

        Set<ScopeRepresentation> selection = scopesInput.getSelected();

        for (ScopeRepresentation selected : selection) {
            boolean isSelected = false;

            for (ScopeRepresentation scope : scopes) {
                if (selected.getName().equals(scope.getName())) {
                    isSelected = true;
                    break;
                }
            }

            if (!isSelected) {
                scopesInput.unSelect(selected.getName(), driver);
            }
        }

        save();
    }

    public void delete() {
        deleteButton.click();
        modalDialog.confirmDeletion();
    }

    public ResourceRepresentation toRepresentation() {
        ResourceRepresentation representation = new ResourceRepresentation();

        representation.setName(UIUtils.getTextInputValue(name));
        representation.setDisplayName(UIUtils.getTextInputValue(displayName));
        representation.setType(UIUtils.getTextInputValue(type));

        Set<String> uris = new HashSet<>();
        for (WebElement uriInput : driver.findElements(By.xpath("//input[@ng-model='resource.uris[i]']"))) {
            uris.add(UIUtils.getTextInputValue(uriInput));
        }
        representation.setUris(uris);

        representation.setIconUri(UIUtils.getTextInputValue(iconUri));
        representation.setScopes(scopesInput.getSelected());

        return representation;
    }

    public class ScopesInput {

        @Root
        private WebElement root;

        @FindBy(xpath = "//input[contains(@class,'select2-input')]")
        private WebElement search;

        @FindBy(xpath = "//div[contains(@class,'select2-result-label')]")
        private List<WebElement> result;

        @FindBy(xpath = "//li[contains(@class,'select2-search-choice')]")
        private List<WebElement> selection;

        public void select(String name) {
            UIUtils.setTextInputValue(search, name);
            pause(1000);
            for (WebElement result : result) {
                if (result.isDisplayed() && UIUtils.getTextFromElement(result).equalsIgnoreCase(name)) {
                    result.click();
                    return;
                }
            }
        }

        public Set<ScopeRepresentation> getSelected() {
            HashSet<ScopeRepresentation> values = new HashSet<>();

            for (WebElement selected : selection) {
                values.add(new ScopeRepresentation(getTextFromElement(selected.findElements(tagName("div")).get(0))));
            }

            return values;
        }

        public void unSelect(String name, WebDriver driver) {
            for (WebElement selected : selection) {
                if (name.equals(getTextFromElement(selected.findElements(tagName("div")).get(0)))) {
                    WebElement element = selected.findElement(By.xpath("//a[contains(@class,'select2-search-choice-close')]"));
                    JavascriptExecutor executor = (JavascriptExecutor) driver;
                    executor.executeScript("arguments[0].click();", element);
                    pause(1000);
                    return;
                }
            }
        }
    }
}