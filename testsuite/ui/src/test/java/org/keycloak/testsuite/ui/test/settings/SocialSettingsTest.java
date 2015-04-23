/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.test.settings;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import static  org.junit.Assert.*;
import org.junit.Test;
import org.keycloak.testsuite.ui.page.settings.SocialSettingsPage;
import org.keycloak.testsuite.ui.AbstractKeyCloakTest;
import org.keycloak.testsuite.ui.fragment.FlashMessage;
import org.keycloak.testsuite.ui.model.Provider;
import org.keycloak.testsuite.ui.model.SocialProvider;
import org.keycloak.testsuite.ui.util.URL;

/**
 *
 * @author pmensik
 */
public class SocialSettingsTest extends AbstractKeyCloakTest<SocialSettingsPage> {
	
	@FindByJQuery(".alert")
    private FlashMessage flashMessage;
	
//	@Test
	public void testAddNewProvider() {
		page.addNewProvider(new Provider(SocialProvider.FACEBOOK, "klic", "secret"));
		flashMessage.waitUntilPresent();
		assertTrue("Success message should be displayed", flashMessage.isSuccess());
	}
	
//	@Test(expected = NoSuchElementException.class)
	public void testDuplicitProvider() {
		page.addNewProvider(new Provider(SocialProvider.FACEBOOK, "a", "b"));
	}
	
//	@Test
	public void testEditProvider() {
		page.goToPage(URL.SETTINGS_SOCIAL);
		page.editProvider(SocialProvider.FACEBOOK, new Provider(SocialProvider.FACEBOOK, "abc", "def"));
	}
	
//	@Test
	public void testDeleteProvider() {
		
	}
	
	@Test
	public void testAddMultipleProviders() {
	}
}
