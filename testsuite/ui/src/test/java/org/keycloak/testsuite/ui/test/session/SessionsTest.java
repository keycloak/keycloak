/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.test.session;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.ui.AbstractKeyCloakTest;
import org.keycloak.testsuite.ui.page.session.SessionsPage;
import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitGuiForElement;

/**
 *
 * @author pmensik
 */
public class SessionsTest extends AbstractKeyCloakTest<SessionsPage> {
	
	@Before
	public void beforeSessionTest() {
		navigation.sessions();
	}
	
	@Test
	public void testLogoutAllSessions() {
		page.logoutAllSessions();
		waitGuiForElement(loginPage.getLoginPageHeader(), "Home page should be visible after logout");
		loginPage.loginAsAdmin();
	}
}
