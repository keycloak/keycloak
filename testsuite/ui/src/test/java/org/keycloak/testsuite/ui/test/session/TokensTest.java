/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.test.session;

import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.ui.page.settings.TokensPage;

import static org.jboss.arquillian.graphene.Graphene.waitModel;
import org.keycloak.testsuite.ui.AbstractKeyCloakTest;
import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitGuiForElement;


/**
 *
 * @author pmensik
 */
public class TokensTest extends AbstractKeyCloakTest<TokensPage> {

	private static final int TIMEOUT = 10;
	private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
	
	@Before
	public void beforeTokensTest() {
		navigation.tokens();
	}
	
	@Test
	public void testTimeoutForRealmSession() throws InterruptedException {
		page.setSessionTimeout(TIMEOUT, TIME_UNIT);
		TIME_UNIT.sleep(TIMEOUT + 2); //add 2 secs to timeout
		driver.navigate().refresh();
		waitGuiForElement(loginPage.getLoginPageHeader(), "Home page should be visible after session timeout");
		loginPage.loginAsAdmin();
		page.setSessionTimeout(30, TimeUnit.MINUTES);
	}
	
	@Test
	public void testLifespanOfRealmSession() {
		page.setSessionTimeoutLifespan(TIMEOUT, TIME_UNIT);
		logOut();
		loginAsAdmin();
		waitModel().withTimeout(TIMEOUT + 2, TIME_UNIT) //adds 2 seconds to the timeout
				.pollingEvery(1, TIME_UNIT)
				.until("Home page should be visible after session timeout")
				.element(loginPage.getLoginPageHeader())
				.is()
				.present();
		loginPage.loginAsAdmin();
		navigation.tokens();
		page.setSessionTimeoutLifespan(10, TimeUnit.HOURS);
	}
}
