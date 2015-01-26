/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.demo.customerPortal;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.jboss.arquillian.graphene.Graphene.waitModel;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.keycloak.testsuite.ui.page.settings.SessionAndTokensPage;
import org.openqa.selenium.By;

/**
 *
 * @author pmensik
 */
public class TestDefaultSettings extends CustomerPortalAbstractTest {
	
	@Page
	private SessionAndTokensPage sessionAndTokensPage;
	
	@After
	public void afterTestDefaultSettings() {
		driver.get(getKeycloakConsoleUrl());
		loginPage.loginAsAdmin();
		navigation.sessions();
		sessionAndTokensPage.logoutAllSessions();
		logOut();
	}
	
	@Test
	public void testAccessCustomerListing() {
		driver.findElement(customerListingLoc).click();
		waitModel().until()
				.element(loginPage.getLoginPageHeader())
				.text()
				.contains(DEMO_APP.toUpperCase());
		loginPage.login("bburke@redhat.com", "password");
		assertTrue(driver.findElement(By.tagName("body")).getText().contains("Username: bburke@redhat.com"));
	}
	
	@Test
	public void testAccessAdminInterface() {
		driver.findElement(customerAdminLoc).click();
		waitModel().until()
				.element(loginPage.getLoginPageHeader())
				.text()
				.contains(DEMO_APP.toUpperCase());
		loginPage.login("admin", "password");
		assertTrue(driver.findElement(By.tagName("body")).getText().contains("admin"));
	}
	
	@Test
	public void testAccessAdminInterfaceWithWrongRole() {
		driver.findElement(customerAdminLoc).click();
		waitModel().until()
				.element(loginPage.getLoginPageHeader())
				.text()
				.contains(DEMO_APP.toUpperCase());
		loginPage.login("bburke@redhat.com", "password");
		assertTrue(driver.findElement(By.tagName("body")).getText().contains("Forbidden"));
	}
	
}
