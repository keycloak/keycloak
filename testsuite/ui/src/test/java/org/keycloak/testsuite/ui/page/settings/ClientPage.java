package org.keycloak.testsuite.ui.page.settings;

import org.keycloak.testsuite.ui.model.Client;
import org.keycloak.testsuite.ui.page.AbstractPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;
import org.jboss.arquillian.graphene.findby.ByJQuery;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitAjaxForElement;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.By.linkText;
import static org.openqa.selenium.By.tagName;

/**
 * Created by fkiss.
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
		waitAjaxForElement(dangerButton);
		dangerButton.click();
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
		driver.findElements(ByJQuery.selector(".btn.btn-primary")).get(0).click();
	}
}
