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

import org.keycloak.testsuite.console.page.AdminConsole;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import static org.keycloak.testsuite.util.SeleniumUtils.pause;

import static org.keycloak.testsuite.util.SeleniumUtils.waitAjaxForElement;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.By.linkText;
import static org.openqa.selenium.By.tagName;

/**
 *
 * @author Filip Kisss
 */
public class Clients extends AdminConsoleRealm {

    @Override
    public String getFragment() {
        return super.getFragment() + "/clients";
    }

    @FindBy(id = "clientId")
    private WebElement clientId;

    @FindBy(id = "name")
    private WebElement nameInput;

    @FindBy(id = "")
    private WebElement enabledSwitchToggle;

    @FindBy(id = "accessType")
    private WebElement accessTypeDropDownMenu;

    @FindBy(id = "newRedirectUri")
    private WebElement redirectUriInput;

    @FindBy(css = "table[class*='table']")
    private WebElement dataTable;

    @FindBy(css = "input[class*='search']")
    private WebElement searchInput;

    @FindBy(id = "createClient")
    private WebElement createClientButton;

    @FindBy(id = "removeClient")
    private WebElement removeClientButton;

    public void addClient(ClientRepresentation client) {
        createClientButton.click();
        waitAjaxForElement(clientId);
        clientId.sendKeys(client.getClientId());
        nameInput.sendKeys(client.getName());
        if (!client.isEnabled()) {
            enabledSwitchToggle.click();
        }

        if (client.isDirectGrantsOnly()) { // TODO verify this one
            accessTypeDropDownMenu.sendKeys("confidential");
        }
        if (client.isBearerOnly()) {
            accessTypeDropDownMenu.sendKeys("bearer-only");
        }
        if (client.isPublicClient()) {
            accessTypeDropDownMenu.sendKeys("public");
        }

        for (String redirectUri : client.getRedirectUris()) {
            addUri(redirectUri);
            pause(100);
        }
        primaryButton.click();
    }

    public void addUri(String uri) {
        redirectUriInput.sendKeys(uri);
    }

    public void confirmAddClient() {
        primaryButton.click();
    }

    public void deleteClient(String clientName) {
        searchInput.sendKeys(clientName);
        driver.findElement(linkText(clientName)).click();
        waitAjaxForElement(removeClientButton);
        removeClientButton.click();
        waitAjaxForElement(deleteConfirmationButton);
        deleteConfirmationButton.click();
    }

    public ClientRepresentation findClient(String clientName) {
        waitAjaxForElement(searchInput);
        searchInput.sendKeys(clientName);
        List<ClientRepresentation> clients = getAllRows();
        if (clients.isEmpty()) {
            return null;
        } else {
            assert 1 == clients.size();
            return clients.get(0);
        }
    }

    public void goToClient(ClientRepresentation client) {
        waitAjaxForElement(dataTable);
        dataTable.findElement(linkText(client.getName())).click();
    }

    private List<ClientRepresentation> getAllRows() {
        List<ClientRepresentation> rows = new ArrayList<>();
        List<WebElement> allRows = dataTable.findElements(cssSelector("tbody tr"));
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

    public void goToCreateClient() {
        createClientButton.click();
    }
}
