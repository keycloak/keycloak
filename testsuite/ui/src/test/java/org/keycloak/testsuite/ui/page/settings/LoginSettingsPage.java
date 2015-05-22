/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.page.settings;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.ui.fragment.OnOffSwitch;
import org.keycloak.testsuite.ui.page.AbstractPage;

/**
 *
 * @author pmensik
 */
public class LoginSettingsPage extends AbstractPage {

	@FindByJQuery("div[class='onoffswitch']:eq(0)")
	private OnOffSwitch registrationAllowed;

	@FindByJQuery("div[class='onoffswitch']:eq(1)")
	private OnOffSwitch resetPasswordAllowed;
		
	@FindByJQuery("div[class='onoffswitch']:eq(2)")
	private OnOffSwitch rememberMeEnabled;

	@FindByJQuery("div[class='onoffswitch']:eq(3)")
	private OnOffSwitch verifyEmailEnabled;

	@FindByJQuery("div[class='onoffswitch']:eq(4)")
	private OnOffSwitch directGrantApiEnabled;

	@FindByJQuery("div[class='onoffswitch']:eq(5)")
	private OnOffSwitch requireSsl;
	
	public boolean isUserRegistrationAllowed() {
		return registrationAllowed.isEnabled();
	}
	
	public void enableUserRegistration() {
		registrationAllowed.enable();
		primaryButton.click();
	}
	
	public void disableUserRegistration() {
		registrationAllowed.disable();
		primaryButton.click();
	}
	
}
