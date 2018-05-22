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
package org.keycloak.testsuite.console.page.clients.authorization.scope;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.openqa.selenium.By.tagName;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testsuite.console.page.fragment.ModalDialog;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class Scopes extends Form {

    @FindBy(css = "table[class*='table']")
    private ScopesTable table;

    @FindBy(linkText = "Create")
    private WebElement create;

    @Page
    private Scope scope;

    @Page
    private ModalDialog modalDialog;

    public ScopesTable scopes() {
        return table;
    }

    public void create(ScopeRepresentation representation) {
        create.click();
        scope.form().populate(representation);
    }

    public void update(String name, ScopeRepresentation representation) {
        for (WebElement row : scopes().rows()) {
            ScopeRepresentation actual = scopes().toRepresentation(row);
            if (actual.getName().equalsIgnoreCase(name)) {
                clickLink(row.findElements(tagName("a")).get(0));
                scope.form().populate(representation);
            }
        }
    }

    public void delete(String name) {
        for (WebElement row : scopes().rows()) {
            ScopeRepresentation actual = scopes().toRepresentation(row);
            if (actual.getName().equalsIgnoreCase(name)) {
                clickLink(row.findElements(tagName("a")).get(0));
                scope.form().delete();
            }
        }
    }

    public void deleteFromList(String name) {
        for (WebElement row : scopes().rows()) {
            ScopeRepresentation actual = scopes().toRepresentation(row);
            if (actual.getName().equalsIgnoreCase(name)) {
                WebElement td = row.findElements(tagName("td")).get(2);
                td.findElement(By.className("dropdown-toggle")).click();
                WebElement actions = td.findElement(By.className("dropdown-menu"));

                actions.findElement(By.linkText("Delete")).click();

                modalDialog.confirmDeletion();
            }
        }
    }

    public Scope name(String name) {
        for (WebElement row : scopes().rows()) {
            ScopeRepresentation actual = scopes().toRepresentation(row);
            if (actual.getName().equalsIgnoreCase(name)) {
                clickLink(row.findElements(tagName("a")).get(0));
                WaitUtils.waitForPageToLoad();
                return scope;
            }
        }
        return null;
    }
}