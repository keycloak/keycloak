/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.page.settings;

import org.jboss.arquillian.graphene.angular.findby.FindByNg;
import org.keycloak.testsuite.ui.fragment.OnOffSwitch;
import org.keycloak.testsuite.ui.page.AbstractPage;

/**
 *
 * @author pmensik
 */
public class LoginSettingsPage extends AbstractPage {

	@FindByNg(model = "realm.social")
	private OnOffSwitch socialLoginAllowed;

	@FindByNg(model = "realm.registrationAllowed")
	private OnOffSwitch registrationAllowed;

	@FindByNg(model = "realm.resetPasswordAllowed")
	private OnOffSwitch resetPasswordAllowed;

	@FindByNg(model = "realm.rememberMe")
	private OnOffSwitch rememberMeEnabled;

	@FindByNg(model = "realm.verifyEmail")
	private OnOffSwitch verifyEmailEnabled;
	
	@FindByNg(model = "realm.passwordCredentialGrantAllowed")
	private OnOffSwitch passwordCredentialGrantAllowed;

	@FindByNg(model = "realm.requireSsl")
	private OnOffSwitch requireSsl;

	public boolean isSocialLoginAllowed() {
		return socialLoginAllowed.isEnabled();
	}
	
	public void toggleSocialLogin() {
		socialLoginAllowed.toggle();
	}
}
