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

import static org.keycloak.testsuite.util.Constants.ADMIN_PSSWD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.keycloak.testsuite.auth.page.account.AccountManagement;
import org.keycloak.testsuite.console.page.fragment.FlashMessage;

/**
 *
 * @author Petr Mensik
 */
@Ignore // FIXME
public class AccountManagementTest extends AbstractAccountTest {

    private static final String USERNAME = "admin";
    private static final String NEW_PASSWORD = "newpassword";
    private static final String WRONG_PASSWORD = "wrongpassword";

    private static final String EMAIL = "admin@email.test";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Smith";
    
    @Page
    protected AccountManagement accountManagement;

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;

    @Before
    public void beforeAccountTest() {
        accountManagement.navigateTo();
        testLogin.loginAsAdmin();
    }

    @After
    public void afterAccountTest() {
        accountManagement.navigateTo();
        accountManagement.signOut();
    }

    @Test
    public void passwordPageValidationTest() {
        accountManagement.password();
        password.save();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isError());

        password.setPassword(WRONG_PASSWORD, NEW_PASSWORD);
        password.save();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isError());

        password.setOldPasswordField(ADMIN_PSSWD);
        password.setNewPasswordField("something");
        password.setConfirmField("something else");
        password.save();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isError());
    }

    @Test
    public void changePasswordTest() {
        accountManagement.password();
        password.setPassword(ADMIN_PSSWD, NEW_PASSWORD);
        password.save();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        accountManagement.signOut();
        testLogin.login(USERNAME, NEW_PASSWORD);
        accountManagement.password();
        password.setPassword(NEW_PASSWORD, ADMIN_PSSWD);
        password.save();
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
        testLogin.login(USERNAME, ADMIN_PSSWD);

        accountManagement.account();
        assertEquals(account.getEmail(), EMAIL);
        assertEquals(account.getFirstName(), FIRST_NAME);
        assertEquals(account.getLastName(), LAST_NAME);
    }

}
