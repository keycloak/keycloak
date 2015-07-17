/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.admin.test.settings;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import static  org.junit.Assert.*;
import org.junit.Test;
import org.keycloak.testsuite.admin.page.settings.SocialSettingsPage;
import org.keycloak.testsuite.admin.AbstractKeycloakTest;
import org.keycloak.testsuite.admin.fragment.FlashMessage;
import org.keycloak.testsuite.admin.model.Provider;
import org.keycloak.testsuite.admin.model.SocialProvider;
import org.keycloak.testsuite.admin.util.URL;

/**
 *
 * @author Petr Mensik
 */
public class SocialSettingsTest extends AbstractKeycloakTest<SocialSettingsPage> {
	
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
