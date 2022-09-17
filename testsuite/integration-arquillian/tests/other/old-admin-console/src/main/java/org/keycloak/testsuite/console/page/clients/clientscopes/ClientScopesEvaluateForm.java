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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.testsuite.console.page.clients.authorization.policy.ClientSelectModal;
import org.keycloak.testsuite.console.page.fragment.DataTable;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.openqa.selenium.By.xpath;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientScopesEvaluateForm extends Form {

    @FindBy(id = "scopeParam")
    private WebElement scopeParamInput;

    @FindBy(id = "available")
    protected Select availableClientScopesSelect;

    @FindBy(id = "assigned")
    protected Select assignedClientScopesSelect;

    @FindBy(id = "effective")
    protected Select effectiveClientScopesSelect;

    @FindBy(css = "button[ng-click*='addAppliedClientScope']")
    protected WebElement addAppliedClientScopesButton;

    @FindBy(css = "button[ng-click*='deleteAppliedClientScope']")
    protected WebElement deleteAppliedClientScopesButton;

    @FindBy(css = "button[data-ng-click*='sendEvaluationRequest']")
    protected WebElement evaluateButton;

    // Bottom part of the page (stuff shown after "Evaluate" button clicked)
    @FindBy(css = "li[data-ng-click*='showTab(1)']")
    protected WebElement showProtocolMappersLink;

    @FindBy(css = "li[data-ng-click*='showTab(2)']")
    protected WebElement showRolesLink;

    @FindBy(css = "li[data-ng-click*='showTab(3)']")
    protected WebElement showTokenLink;

    @FindBy(css = "table[data-ng-show*='protocolMappersShown']")
    protected DataTable protocolMappersTable;

    @FindBy(id = "available-realm-roles")
    protected Select notGrantedRealmRolesSelect;

    @FindBy(id = "realm-composite")
    protected Select grantedRealmRolesSelect;

    @FindBy(tagName = "textarea")
    private WebElement accessTokenTextArea;

    @FindBy(id = "s2id_users")
    private ClientSelectModal clientsInput;



    public Set<String> getAvailableClientScopes() {
        return ClientScopesSetupForm.getSelectValues(availableClientScopesSelect);
    }

    public Set<String> getAssignedClientScopes() {
        return ClientScopesSetupForm.getSelectValues(assignedClientScopesSelect);
    }

    public Set<String> getEffectiveClientScopes() {
        return ClientScopesSetupForm.getSelectValues(effectiveClientScopesSelect);
    }

    public void setAssignedClientScopes(Collection<String> scopes) {
        ClientScopesSetupForm.removeRedundantScopes(assignedClientScopesSelect, deleteAppliedClientScopesButton, scopes);
        ClientScopesSetupForm.addMissingScopes(availableClientScopesSelect, addAppliedClientScopesButton, scopes);
    }


    public void selectUser(String username) {
        clientsInput.select(username);
    }


    public void evaluate() {
        evaluateButton.click();
        WaitUtils.waitForPageToLoad();
    }


    public void showProtocolMappers() {
        showProtocolMappersLink.click();
        WaitUtils.waitForPageToLoad();
    }

    public void showRoles() {
        showRolesLink.click();
        WaitUtils.waitForPageToLoad();
    }

    public void showToken() {
        showTokenLink.click();
        WaitUtils.waitForPageToLoad();
    }


    // Bottom part of the page (stuff shown after "Evaluate" button clicked)
    public Set<String> getEffectiveProtocolMapperNames() {
        List<WebElement> rows = protocolMappersTable.rows();

        Set<String> names = rows.stream().map((WebElement row) -> {

            return getTextFromElement(row.findElement(xpath("td[1]")));

        }).collect(Collectors.toSet());

        return names;
    }


    public Set<String> getGrantedRealmRoles() {
        return ClientScopesSetupForm.getSelectValues(grantedRealmRolesSelect);
    }

    public Set<String> getNotGrantedRealmRoles() {
        return ClientScopesSetupForm.getSelectValues(notGrantedRealmRolesSelect);
    }

    public String getAccessToken() {
        return getTextFromElement(accessTokenTextArea);
    }


}
