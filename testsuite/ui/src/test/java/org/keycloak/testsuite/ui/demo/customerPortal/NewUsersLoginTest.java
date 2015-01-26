/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.demo.customerPortal;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.ui.fragment.FlashMessage;
import org.keycloak.testsuite.ui.fragment.PickList;
import org.keycloak.testsuite.ui.model.UserAction;
import org.keycloak.testsuite.ui.page.settings.SessionAndTokensPage;
import org.keycloak.testsuite.ui.page.settings.UserPage;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.ui.util.Users.TEST_USER1;
import static org.openqa.selenium.By.id;
import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitGuiForElement;
import org.openqa.selenium.By;

/**
 *
 * @author pmensik
 */
public class NewUsersLoginTest extends CustomerPortalAbstractTest {

	@Page
	private UserPage userPage;

	@Page
	private SessionAndTokensPage sessionAndTokensPage;

	@FindBy(className = "changing-selectors")
	private PickList pickList;

	@FindByJQuery(".alert")
	private FlashMessage flashMessage;

	@Before
	public void beforeNewUsersTest() {
		driver.get(getKeycloakConsoleUrl());
		loginAsAdmin();
		navigation.users();
		userPage.addUser(TEST_USER1);
		navigation.roleMappings();
		pickList.setFirstSelect(id("available"));
		pickList.setSecondSelect(id("assigned"));
	}

	@Test
	public void testCanLoginToExampleWithUserRole() {
		pickList.addItems("user");
		flashMessage.waitUntilPresent();
		assertTrue(flashMessage.getText(), flashMessage.isSuccess());
		navigation.credentials();
		userPage.addPasswordForUser(TEST_USER1);
		navigation.attributes();
		userPage.removeAction(UserAction.UPDATE_PASSWORD);
		logOut();
		driver.get(getUrl());
		driver.findElement(customerListingLoc).click();
		waitGuiForElement(loginPage.getLoginPageHeader());
		loginPage.login(TEST_USER1.getUserName(), TEST_USER1.getPassword());
		assertTrue(driver.findElement(By.tagName("body")).getText().contains(TEST_USER1.getUserName()));
		logOutFromDemo();
		driver.get(getKeycloakConsoleUrl());
		loginAsAdmin();
		navigation.users();
		userPage.deleteUser(TEST_USER1.getUserName());
		logOut();
	}

	@Test
	public void testCannotLoginToExampleAdminWithUserRole() {
		pickList.addItems("user");
		flashMessage.waitUntilPresent();
		assertTrue(flashMessage.getText(), flashMessage.isSuccess());
		navigation.credentials();
		userPage.addPasswordForUser(TEST_USER1);
		navigation.attributes();
		userPage.removeAction(UserAction.UPDATE_PASSWORD);
		logOut();
		driver.get(getUrl());
		driver.findElement(customerAdminLoc).click();
		waitGuiForElement(loginPage.getLoginPageHeader());
		loginPage.login(TEST_USER1.getUserName(), TEST_USER1.getPassword());
		assertTrue(driver.findElement(By.tagName("body")).getText().contains("Forbidden"));
		driver.get(getKeycloakConsoleUrl());
		loginAsAdmin();
		navigation.users();
		userPage.deleteUser(TEST_USER1.getUserName());
		logOut();
	}

	@Test
	public void testCanLoginToExampleAdminWithAdminRole() {
		pickList.addItems("admin");
		flashMessage.waitUntilPresent();
		assertTrue(flashMessage.getText(), flashMessage.isSuccess());
		navigation.credentials();
		userPage.addPasswordForUser(TEST_USER1);
		navigation.attributes();
		userPage.removeAction(UserAction.UPDATE_PASSWORD);
		logOut();
		driver.get(getUrl());
		driver.findElement(customerAdminLoc).click();
		waitGuiForElement(loginPage.getLoginPageHeader());
		loginPage.login(TEST_USER1.getUserName(), TEST_USER1.getPassword());
		assertTrue(driver.findElement(By.tagName("body")).getText().contains("Customer Admin Interface"));
		driver.get(getKeycloakConsoleUrl());
		loginAsAdmin();
		navigation.sessions();
		sessionAndTokensPage.logoutAllSessions();
		navigation.users();
		userPage.deleteUser(TEST_USER1.getUserName());
		logOut();
	}

}
