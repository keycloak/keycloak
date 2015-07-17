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

package org.keycloak.testsuite.account;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Test;

import static org.keycloak.testsuite.admin.util.Constants.ADMIN_PSSWD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.keycloak.testsuite.admin.AbstractKeycloakTest;
import org.keycloak.testsuite.admin.fragment.FlashMessage;
import org.keycloak.testsuite.admin.model.Account;
import org.keycloak.testsuite.admin.page.account.AccountPage;
import org.keycloak.testsuite.admin.page.account.PasswordPage;

/**
 *
 * @author Petr Mensik
 */
public class AccountManagementTest extends AbstractKeycloakTest<AccountPage> {

	@FindByJQuery(".alert")
    private FlashMessage flashMessage;
	
    @Page
    private AccountPage accountPage;

    @Page
    private PasswordPage passwordPage;
	
    private static final String USERNAME = "admin";
    private static final String NEW_PASSWORD = "newpassword";
    private static final String WRONG_PASSWORD = "wrongpassword";

	@Before
	public void beforeAccountTest() {
		menuPage.goToAccountManagement();
	}
	
	@After
	public void afterAccountTest() {
		accountPage.keycloakConsole();
	}
	
	@Test
    public void passwordPageValidationTest() {
	    page.password();
        passwordPage.save();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isError());

        passwordPage.setPassword(WRONG_PASSWORD, NEW_PASSWORD);
        passwordPage.save();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isError());

        passwordPage.setOldPasswordField(ADMIN_PSSWD);
        passwordPage.setNewPasswordField("something");
        passwordPage.setConfirmField("something else");
        passwordPage.save();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isError());
    }

    @Test
    public void changePasswordTest() {
        page.password();
        passwordPage.setPassword(ADMIN_PSSWD, NEW_PASSWORD);
        passwordPage.save();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        page.signOut();
        loginPage.login(USERNAME, NEW_PASSWORD);
        page.password();
        passwordPage.setPassword(NEW_PASSWORD, ADMIN_PSSWD);
        passwordPage.save();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
    }

    @Test
    public void accountPageTest() {
        page.account();
        Account adminAccount = accountPage.getAccount();
        assertEquals(adminAccount.getUsername(), USERNAME);
        adminAccount.setEmail("a@b");
        adminAccount.setFirstName("John");
        adminAccount.setLastName("Smith");
        accountPage.setAccount(adminAccount);
        accountPage.save();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        page.signOut();
        loginPage.login(USERNAME, ADMIN_PSSWD);

        page.account();
        assertEquals(adminAccount, accountPage.getAccount());
    }

}
