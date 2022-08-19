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

package org.keycloak.testsuite.console.page.clients.clientscopes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientScopesSetupForm extends Form {

    @FindBy(id = "available")
    protected Select availableDefaultClientScopesSelect;

    @FindBy(id = "assigned")
    protected Select defaultClientScopesSelect;

    @FindBy(id = "available-opt")
    protected Select availableOptionalClientScopesSelect;

    @FindBy(id = "assigned-opt")
    protected Select optionalClientScopesSelect;


    @FindBy(css = "button[ng-click*='addDefaultClientScope']")
    protected WebElement addSelectedDefaultClientScopesButton;

    @FindBy(css = "button[ng-click*='addOptionalClientScope']")
    protected WebElement addSelectedOptionalClientScopesButton;

    @FindBy(css = "button[ng-click*='deleteDefaultClientScope']")
    protected WebElement removeSelectedDefaultClientScopesButton;

    @FindBy(css = "button[ng-click*='deleteOptionalClientScope']")
    protected WebElement removeSelectedOptionalClientScopesButton;


    public Set<String> getAvailableDefaultClientScopes() {
        return getSelectValues(availableDefaultClientScopesSelect);
    }

    public Set<String> getDefaultClientScopes() {
        return getSelectValues(defaultClientScopesSelect);
    }

    public Set<String> getAvailableOptionalClientScopes() {
        return getSelectValues(availableOptionalClientScopesSelect);
    }

    public Set<String> getOptionalClientScopes() {
        return getSelectValues(optionalClientScopesSelect);
    }


    public void setDefaultClientScopes(Collection<String> scopes) {
        removeRedundantScopes(defaultClientScopesSelect, removeSelectedDefaultClientScopesButton, scopes);
        addMissingScopes(availableDefaultClientScopesSelect, addSelectedDefaultClientScopesButton, scopes);
    }

    public void setOptionalClientScopes(Collection<String> scopes) {
        removeRedundantScopes(optionalClientScopesSelect, removeSelectedOptionalClientScopesButton, scopes);
        addMissingScopes(availableOptionalClientScopesSelect, addSelectedOptionalClientScopesButton, scopes);
    }


    // Static helper methods

    static Set<String> getSelectValues(Select select) {
        Set<String> roles = new HashSet<>();
        for (WebElement option : select.getOptions()) {
            roles.add(getTextFromElement(option).trim());
        }
        return roles;
    }


    static void removeRedundantScopes(Select select, WebElement button, Collection<String> scopes) {
        boolean someRemoved = false;

        select.deselectAll();
        for (String scope : getSelectValues(select)) {
            if (scopes == null // if scopes not provided, remove all
                    || !scopes.contains(scope)) { // if scopes provided, remove only the redundant
                select.selectByVisibleText(scope);
                someRemoved = true;
            }
        }

        if (someRemoved) {
            waitUntilElement(button).is().enabled();
            button.click();
        }
    }


    static void addMissingScopes(Select select, WebElement button, Collection<String> scopes) {
        select.deselectAll();
        if (scopes != null) { // if scopes not provided, don't add any
            boolean someAdded = false;

            for (String scope : getSelectValues(select)) {
                if (scopes.contains(scope)) { // if scopes provided, add only the missing
                    select.selectByVisibleText(scope);
                    someAdded = true;
                }
            }

            if (someAdded) {
                waitUntilElement(button).is().enabled();
                button.click();
            }
        }
    }

}
