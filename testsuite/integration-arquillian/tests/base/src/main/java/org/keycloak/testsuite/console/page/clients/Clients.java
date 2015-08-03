/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.console.page.clients;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.keycloak.testsuite.console.page.fragment.DataTable;

import static org.keycloak.testsuite.util.SeleniumUtils.waitAjaxForElement;
import static org.openqa.selenium.By.linkText;
import static org.openqa.selenium.By.tagName;

/**
 *
 * @author Filip Kisss
 */
public class Clients extends AdminConsoleRealm {
    
    public static final String CREATE = "Create";
    public static final String IMPORT = "Import";
    
    public static final String EDIT = "Edit";
    public static final String DELETE = "Delete";
    
    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/clients";
    }
    
    @FindBy(css = "table[class*='table']")
    private DataTable table;
    
    public List<ClientRepresentation> searchClients(String searchPattern) {
        table.search(searchPattern);
        return getClientsFromTable();
    }
    
    public void createClient() {
        table.clickHeaderButton(CREATE);
    }
    
    public void importClient() {
        table.clickHeaderButton(IMPORT);
    }
    
    public void clickClient(ClientRepresentation client) {
        clickClient(client.getClientId());
    }

    public void clickClient(String clientId) {
        waitAjaxForElement(table.body());
        table.body().findElement(linkText(clientId)).click();
    }
    
    public void editClient(String clientId) {
        table.clickActionButton(table.getRowByLinkText(clientId), EDIT);
    }
    
    public void deleteClient(String clientId) {
        table.clickActionButton(table.getRowByLinkText(clientId), DELETE);
        waitAjaxForElement(deleteConfirmationButton);
        deleteConfirmationButton.click();
    }
    
    public ClientRepresentation findClient(String clientId) {
        List<ClientRepresentation> clients = searchClients(clientId);
        if (clients.isEmpty()) {
            return null;
        } else {
            assert 1 == clients.size();
            return clients.get(0);
        }
    }
    
    private List<ClientRepresentation> getClientsFromTable() {
        List<ClientRepresentation> rows = new ArrayList<>();
        List<WebElement> allRows = table.rows();
        if (allRows.size() > 1) {
            for (WebElement rowElement : allRows) {
                if (rowElement.isDisplayed()) {
                    ClientRepresentation client = new ClientRepresentation();
                    List<WebElement> tds = rowElement.findElements(tagName("td"));
                    client.setClientId(tds.get(0).getText());
                    List<String> redirectUris = new ArrayList<>();
                    redirectUris.add(tds.get(2).getText()); // FIXME there can be more than 1 redirect uri
                    client.setRedirectUris(redirectUris);
                    rows.add(client);
                }
            }
        }
        return rows;
    }
    
}
