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

package org.keycloak.testsuite.admin.page.settings.user;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.admin.model.User;
import org.keycloak.testsuite.admin.page.AbstractPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import org.keycloak.testsuite.admin.model.UserAction;
import static org.keycloak.testsuite.admin.util.SeleniumUtils.waitAjaxForElement;
import static org.openqa.selenium.By.*;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author Filip Kiss
 */
public class UserPage extends AbstractPage {

	@FindBy(id = "username")
	private WebElement usernameInput;

	@FindBy(id = "email")
	private WebElement emailInput;

	@FindBy(id = "firstName")
	private WebElement firstNameInput;

	@FindBy(id = "lastName")
	private WebElement lastNameInput;

	@FindBy(id = "emailVerified")
	private WebElement emailVerifiedSwitchToggle;

	@FindBy(css = "label[for='userEnabled']")
	private WebElement userEnabledSwitchToggle;

	@FindBy(css = "input[class*='select2-input']")
	private WebElement requiredUserActionsInput;

	@FindByJQuery(".select2-offscreen")
	private Select actionsSelect;

	@FindBy(id = "password")
	private WebElement password;

	@FindBy(id = "confirmPassword")
	private WebElement confirmPassword;

	@FindBy(id = "viewAllUsers")
	private WebElement viewAllUsers;

	@FindBy(id = "createUser")
	private WebElement createUser;

	@FindBy(id = "removeUser")
	private WebElement removeUser;

	@FindBy(css = "input[class*='search']")
	private WebElement searchInput;

	@FindBy(css = "table[class*='table']")
	private WebElement dataTable;

	@FindByJQuery("button[kc-cancel] ")
	private WebElement cancel;
	
	@FindBy(css = "div[class='input-group-addon'] i")
	private WebElement searchButton;

	public void addUser(User user) {
		createUser.click();
		waitAjaxForElement(usernameInput);
		usernameInput.sendKeys(user.getUserName());
		emailInput.sendKeys(user.getEmail());
		firstNameInput.sendKeys(user.getFirstName());
		lastNameInput.sendKeys(user.getLastName());
		if (!user.isUserEnabled()) {
			userEnabledSwitchToggle.click();
		}
		if (user.isEmailVerified()) {
			emailVerifiedSwitchToggle.click();
		}
//		requiredUserActionsInput.sendKeys(user.getRequiredUserActions());
		primaryButton.click();
	}

	public void addPasswordForUser(User user) {
		password.sendKeys(user.getPassword());
		confirmPassword.sendKeys(user.getPassword());
		dangerButton.click();
		waitAjaxForElement(deleteConfirmationButton);
		deleteConfirmationButton.click();
	}

	public User findUser(String username) {
		waitAjaxForElement(searchInput);
		searchInput.sendKeys(username);
		searchButton.click();
		List<User> users = getAllRows();
		if (users.isEmpty()) {
			return null;

		} else {
			assertEquals(1, users.size());
			return users.get(0);
		}
	}

	public void editUser(User user) {
		goToUser(user);
		waitAjaxForElement(usernameInput);
		usernameInput.sendKeys(user.getUserName());
		emailInput.sendKeys(user.getEmail());
		if (!user.isUserEnabled()) {
			userEnabledSwitchToggle.click();
		}
		if (user.isEmailVerified()) {
			emailVerifiedSwitchToggle.click();
		}
		requiredUserActionsInput.sendKeys(user.getRequiredUserActions());
		primaryButton.click();
	}

	public void deleteUser(String username) {
		searchInput.sendKeys(username);
		searchButton.click();
		driver.findElement(linkText(username)).click();
		waitAjaxForElement(removeUser);
		removeUser.click();
		waitAjaxForElement(deleteConfirmationButton);
		deleteConfirmationButton.click();
	}

	public void cancel() {
		cancel.click();
	}

	public void showAllUsers() {
		viewAllUsers.click();
	}

	public void goToUser(User user) {
		dataTable.findElement(linkText(user.getUserName())).click();
	}

	public void goToUser(String name) {
		goToUser(new User(name));
	}

	public void addAction(UserAction action) {
		actionsSelect.selectByValue(action.name());
		primaryButton.click();
	}

	public void removeAction(UserAction action) {
		actionsSelect.deselectByValue(action.name());
		primaryButton.click();
	}

	private List<User> getAllRows() {
		List<User> users = new ArrayList<User>();
		List<WebElement> rows = dataTable.findElements(cssSelector("tbody tr"));
		if (rows.size() > 1) {
			for (WebElement rowElement : rows) {
				if (rowElement.isDisplayed()) {
					User user = new User();
					List<WebElement> tds = rowElement.findElements(tagName("td"));
					if (!(tds.isEmpty() || tds.get(0).getText().isEmpty())) {
						user.setUserName(tds.get(0).getText());
						user.setLastName(tds.get(1).getText());
						user.setFirstName(tds.get(2).getText());
						user.setEmail(tds.get(3).getText());
						users.add(user);
					}
				}
			}	
		}
		return users;
	}

}
