/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.page.settings;

import java.util.concurrent.TimeUnit;
import org.keycloak.testsuite.ui.page.AbstractPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.text.WordUtils.capitalize;

/**
 *
 * @author pmensik
 */
public class SessionAndTokensPage extends AbstractPage {
	
	@FindBy(id = "ssoSessionIdleTimeout")
	private WebElement sessionTimeout;
	
	@FindBy(name = "ssoSessionIdleTimeoutUnit")
	private Select sessionTimeoutUnit;
	
	@FindBy(id = "ssoSessionMaxLifespan")
	private WebElement sessionLifespanTimeout;
	
	@FindBy(name = "ssoSessionMaxLifespanUnit")
	private Select sessionLifespanTimeoutUnit;

	public void logoutAllSessions() {
		primaryButton.click();
	}
	
	public void setSessionTimeout(int timeout, TimeUnit unit) {
		setTimeout(sessionTimeoutUnit, sessionTimeout, timeout, unit);
	}
	
	public void setSessionTimeoutLifespan(int time, TimeUnit unit) {
		setTimeout(sessionLifespanTimeoutUnit, sessionLifespanTimeout, time, unit);
	}
	
	private void setTimeout(Select timeoutElement, WebElement unitElement,
			int timeout, TimeUnit unit) {
		timeoutElement.selectByValue(capitalize(unit.name().toLowerCase()));
		unitElement.clear();
		unitElement.sendKeys(valueOf(timeout));
		primaryButton.click();
	}
}
