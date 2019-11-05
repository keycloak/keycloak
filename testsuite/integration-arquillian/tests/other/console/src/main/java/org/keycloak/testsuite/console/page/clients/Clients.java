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

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.keycloak.testsuite.console.page.fragment.DataTable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
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

    @FindBy(tagName = "table")
    private ClientsTable clientsTable;

    public ClientsTable table() {
        return clientsTable;
    }

    public class ClientsTable extends DataTable {

        public List<ClientRepresentation> searchClients(String searchPattern) {
            search(searchPattern);
            return getClientsFromRows();
        }

        public void createClient() {
            clickHeaderLink(CREATE);
        }

        public void importClient() {
            clickHeaderLink(IMPORT);
        }

        public void clickClient(ClientRepresentation client) {
            clickClient(client.getClientId());
        }

        public void clickClient(String clientId) {
            body().findElement(linkText(clientId)).click();
        }

        public void editClient(String clientId) {
            clickRowActionButton(getRowByLinkText(clientId), EDIT);
        }

        private void clickFooterButton(int index) {
	      footer().findElements(By.tagName("button")).get(index).click();
        }

        public void clickNextPage() {
            clickFooterButton(2);
        }

        public void clickPrevPage() {
            clickFooterButton(1);
        }

        public void clickFirstPage() {
            clickFooterButton(0);
        }

        public void deleteClient(String clientId) {
            clickRowActionButton(getRowByLinkText(clientId), DELETE);
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

        public List<ClientRepresentation> getClientsFromRows() {
            List<ClientRepresentation> rows = new ArrayList<>();
            for (WebElement row : rows()) {
                ClientRepresentation client = getClientFromRow(row);
                if (client != null) {
                    rows.add(client);
                }
            }
            return rows;
        }

        public ClientRepresentation getClientFromRow(WebElement row) {
            ClientRepresentation client = null;
            if (row.isDisplayed()) {
                client = new ClientRepresentation();
                List<WebElement> tds = row.findElements(tagName("td"));
                client.setClientId(getTextFromElement(tds.get(0)));
                List<String> redirectUris = new ArrayList<>();
                redirectUris.add(getTextFromElement(tds.get(2))); // FIXME there can be more than 1 redirect uri
                client.setRedirectUris(redirectUris);
            }
            return client;
        }
    }

}
