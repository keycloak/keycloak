/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.page.session;

import org.keycloak.testsuite.ui.page.AbstractPage;

/**
 *
 * @author pmensik
 */
public class SessionsPage extends AbstractPage {

	public void logoutAllSessions() {
		primaryButton.click();
	}
}
