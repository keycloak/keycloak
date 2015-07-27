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

import org.keycloak.testsuite.AbstractAuthTest;
import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Test;

import static org.keycloak.testsuite.util.Constants.ADMIN_PSSWD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.keycloak.testsuite.console.page.fragment.FlashMessage;
import org.keycloak.testsuite.account.page.Account;
import org.keycloak.testsuite.account.page.AccountManagement;

/**
 *
 * @author Petr Mensik
 */
public class AccountManagementTest extends AbstractAuthTest {

    private static final String USERNAME = "admin";
    private static final String NEW_PASSWORD = "newpassword";
    private static final String WRONG_PASSWORD = "wrongpassword";

    private static final String EMAIL = "admin@email.test";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Smith";
    
    @Page
    protected AccountManagement accountManagement;

    @Page
    protected Account account;

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;

    @Before
    public void beforeAccountTest() {
        accountManagement.navigateTo();
        login.loginAsAdmin();
    }

    @After
    public void afterAccountTest() {
        accountManagement.signOut();
    }

    @Test
    public void passwordPageValidationTest() {
        accountManagement.password();
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
        accountManagement.password();
        passwordPage.setPassword(ADMIN_PSSWD, NEW_PASSWORD);
        passwordPage.save();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        accountManagement.signOut();
        super.login.login(USERNAME, NEW_PASSWORD);
        accountManagement.password();
        passwordPage.setPassword(NEW_PASSWORD, ADMIN_PSSWD);
        passwordPage.save();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
    }

    @Test
    public void testEditAccount() {
        accountManagement.account();
        assertEquals(account.getUsername(), USERNAME);
        account.setEmail(EMAIL);
        account.setFirstName(FIRST_NAME);
        account.setLastName(LAST_NAME);
        account.save();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        accountManagement.signOut();
        super.login.login(USERNAME, ADMIN_PSSWD);

        accountManagement.account();
        assertEquals(account.getEmail(), EMAIL);
        assertEquals(account.getFirstName(), FIRST_NAME);
        assertEquals(account.getLastName(), LAST_NAME);
    }

}
