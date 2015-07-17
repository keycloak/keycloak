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

package org.keycloak.testsuite.admin.page.settings;

import org.keycloak.testsuite.admin.model.Client;
import org.keycloak.testsuite.admin.page.AbstractPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.admin.util.SeleniumUtils.waitAjaxForElement;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.By.linkText;
import static org.openqa.selenium.By.tagName;

/**
 *
 * @author Filip Kisss
 */
public class ClientPage extends AbstractPage {

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
	private WebElement createButton;

	@FindBy(id = "removeClient")
	protected WebElement removeButton;

	public void addClient(Client client) {
		primaryButton.click();
		waitAjaxForElement(clientId);
		clientId.sendKeys(client.getClientId());
		nameInput.sendKeys(client.getName());
		if (!client.isEnabled()) {
			enabledSwitchToggle.click();
		}
		accessTypeDropDownMenu.sendKeys(client.getAccessType());
		redirectUriInput.sendKeys(client.getUri());
		primaryButton.click();
	}

	public void addUri(String uri) {
		redirectUriInput.sendKeys(uri);
	}

	public void removeUri(Client client) {
	}

	public void confirmAddClient() {
		primaryButton.click();
	}

	public void deleteClient(String clientName) {
		searchInput.sendKeys(clientName);
		driver.findElement(linkText(clientName)).click();
		waitAjaxForElement(removeButton);
		removeButton.click();
		waitAjaxForElement(deleteConfirmationButton);
		deleteConfirmationButton.click();
	}

	public Client findClient(String clientName) {
		waitAjaxForElement(searchInput);
		searchInput.sendKeys(clientName);
		List<Client> clients = getAllRows();
		if (clients.isEmpty()) {
			return null;

		} else {
			assertEquals(1, clients.size());
			return clients.get(0);
		}
	}

	private List<Client> getAllRows() {
		List<Client> rows = new ArrayList<Client>();
		List<WebElement> allRows = dataTable.findElements(cssSelector("tbody tr"));
		if (allRows.size() > 1) {
			for (WebElement rowElement : allRows) {
				if (rowElement.isDisplayed()) {
					Client client = new Client();
					List<WebElement> tds = rowElement.findElements(tagName("td"));
					client.setClientId(tds.get(0).getText());
					client.setUri(tds.get(2).getText());
					rows.add(client);
				}
			}
		}
		return rows;
	}

	public void goToCreateClient() {
		createButton.click();
	}
}
