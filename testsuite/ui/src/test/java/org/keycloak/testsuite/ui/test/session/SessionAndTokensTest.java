/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.test.session;

import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.ui.page.settings.SessionAndTokensPage;

import static org.jboss.arquillian.graphene.Graphene.waitModel;
import org.keycloak.testsuite.ui.AbstractKeyCloakTest;
import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitGuiForElement;


/**
 *
 * @author pmensik
 */
public class SessionAndTokensTest extends AbstractKeyCloakTest<SessionAndTokensPage> {

	private static final int TIMEOUT = 10;
	private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
	
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
	
	@Test
	public void testTimeoutForRealmSession() throws InterruptedException {
		navigation.timeoutSettings();
		page.setSessionTimeout(TIMEOUT, TIME_UNIT);
		TIME_UNIT.sleep(2 * TIMEOUT);
		driver.navigate().refresh();
		waitGuiForElement(loginPage.getLoginPageHeader(), "Home page should be visible after session timeout");
		loginPage.loginAsAdmin();
		navigation.timeoutSettings();
		page.setSessionTimeout(30, TimeUnit.MINUTES);
	}
	
	@Test
	public void testLifespanOfRealmSession() {
		navigation.timeoutSettings();
		page.setSessionTimeoutLifespan(TIMEOUT, TIME_UNIT);
		logOut();
		loginAsAdmin();
		waitModel().withTimeout(TIMEOUT * 2, TIME_UNIT)
				.pollingEvery(1, TIME_UNIT)
				.until("Home page should be visible after session timeout")
				.element(loginPage.getLoginPageHeader())
				.is()
				.present();
		loginPage.loginAsAdmin();
		navigation.sessions();
		navigation.timeoutSettings();
		page.setSessionTimeoutLifespan(10, TimeUnit.HOURS);
	}
}
