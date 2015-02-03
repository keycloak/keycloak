/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.test.settings;

import org.junit.Test;
import org.keycloak.testsuite.ui.page.settings.LoginSettingsPage;

import static org.junit.Assert.*;
import org.junit.Before;
import org.keycloak.testsuite.ui.AbstractKeyCloakTest;

/**
 *
 * @author pmensik
 */
public class LoginSettingsTest extends AbstractKeyCloakTest<LoginSettingsPage> {
	
	@Before
	public void beforeLoginSettingsTest() {
		navigation.login();
	}
	
	@Test
	public void testToggleSocialLogin() {
		assertFalse("Social login shoudn't be allowed by default", page.isSocialLoginAllowed());
		page.enableSocialLogin();
		assertTrue("Social login should be allowed", page.isSocialLoginAllowed());
		page.disableSocialLogin();
		assertFalse("Social login shouldn't be allowed", page.isSocialLoginAllowed());
	}
}
