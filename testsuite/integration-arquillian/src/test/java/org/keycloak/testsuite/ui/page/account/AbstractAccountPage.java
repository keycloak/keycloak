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
package org.keycloak.testsuite.ui.page.account;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.ui.page.AbstractPage;
import org.openqa.selenium.WebElement;

/**
 *
 * @author <a href="mailto:pmensik@redhat.com">Petr Mensik</a>
 */
public class AbstractAccountPage extends AbstractPage {

	@FindByJQuery(".nav li:eq(0) a")
	private WebElement keyclockConsole;

	@FindByJQuery(".nav li:eq(1) a")
	private WebElement signOutLink;

	@FindByJQuery(".bs-sidebar ul li:eq(0) a")
	private WebElement accountLink;

	@FindByJQuery(".bs-sidebar ul li:eq(1) a")
	private WebElement passwordLink;

	@FindByJQuery(".bs-sidebar ul li:eq(2) a")
	private WebElement authenticatorLink;

	@FindByJQuery(".bs-sidebar ul li:eq(3) a")
	private WebElement sessionsLink;

	@FindByJQuery("button[value='Save']")
	private WebElement save;

	public void keycloakConsole() {
		keyclockConsole.click();
	}

	public void signOut() {
		signOutLink.click();
	}

	public void account() {
		accountLink.click();
	}

	public void password() {
		passwordLink.click();
	}

	public void authenticator() {
		authenticatorLink.click();
	}

	public void sessions() {
		sessionsLink.click();
	}

	public void save() {
		save.click();
	}
}
