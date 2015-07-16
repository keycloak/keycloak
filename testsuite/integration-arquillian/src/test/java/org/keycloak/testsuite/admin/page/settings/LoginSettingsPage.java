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

package org.keycloak.testsuite.admin.page.settings;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.admin.fragment.OnOffSwitch;
import org.keycloak.testsuite.admin.page.AbstractPage;

/**
 *
 * @author Petr Mensik
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
