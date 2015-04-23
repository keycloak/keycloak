/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.test.settings;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.ui.AbstractKeyCloakTest;
import org.keycloak.testsuite.ui.model.PasswordPolicy;
import org.keycloak.testsuite.ui.page.settings.CredentialsPage;
/**
 *
 * @author pmensik
 */
public class CredentialsTest extends AbstractKeyCloakTest<CredentialsPage> {
	
	@Before
	public void beforeCredentialsTest() {
		navigation.credentials();
	}
	
	@Test
	public void testDigitsNumber() {
		page.addPolicy(PasswordPolicy.HASH_ITERATIONS, 5);
		page.removePolicy(PasswordPolicy.DIGITS);
	}
}
