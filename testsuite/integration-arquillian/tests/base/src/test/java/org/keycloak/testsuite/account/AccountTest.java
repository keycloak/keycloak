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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import static org.keycloak.testsuite.auth.page.AuthRealm.ADMIN;
import org.keycloak.testsuite.auth.page.account.Account;
import static org.keycloak.testsuite.util.ApiUtil.createUserWithAdminClient;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public class AccountTest extends AbstractAccountManagementTest {

    private static final String USERNAME = "admin";

    private static final String EMAIL = "admin@email.test";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Smith";

    @Page
    private Account testRealmAccount;
    
    @Before
    public void beforeAccountTest() {
        // create user via admin api
        createUserWithAdminClient(testRealmResource, testRealmUser);

        testRealmAccountManagement.navigateTo();
        testRealmLogin.form().login(testRealmUser);
    }

    @After
    public void afterAccountTest() {
        testRealmAccountManagement.navigateTo();
        testRealmAccountManagement.signOut();
    }

    @Test
    public void testEditAccount() {
        testRealmAccountManagement.account();
        assertEquals(testRealmAccount.getUsername(), USERNAME);
        testRealmAccount.setEmail(EMAIL);
        testRealmAccount.setFirstName(FIRST_NAME);
        testRealmAccount.setLastName(LAST_NAME);
        testRealmAccount.save();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        testRealmAccountManagement.signOut();
        testRealmLogin.form().login(USERNAME, ADMIN);

        testRealmAccountManagement.account();
        assertEquals(testRealmAccount.getEmail(), EMAIL);
        assertEquals(testRealmAccount.getFirstName(), FIRST_NAME);
        assertEquals(testRealmAccount.getLastName(), LAST_NAME);
    }

}
