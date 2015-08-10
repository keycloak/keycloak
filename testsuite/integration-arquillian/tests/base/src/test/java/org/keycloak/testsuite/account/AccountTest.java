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
import org.junit.Before;
import org.keycloak.testsuite.auth.page.account.Account;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public class AccountTest extends AbstractAccountManagementTest {

    private static final String UPDATED_EMAIL = "new-name@email.test";
    private static final String NEW_FIRST_NAME = "John";
    private static final String NEW_LAST_NAME = "Smith";

    @Page
    private Account testRealmAccount;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmAccount.setAuthRealm(testRealm);
    }
    
    @Before
    public void beforeAccountTest() {
        testRealmAccountManagement.navigateTo();
        testRealmLogin.form().login(testRealmUser);
    }

    @After
    public void afterAccountTest() {
        testRealmAccountManagement.navigateTo();
        testRealmAccountManagement.signOut();
    }

    @Test
    public void editAccount() {
        testRealmAccountManagement.account();
        assertEquals(testRealmAccount.getUsername(), testRealmUser.getUsername());
        
        testRealmAccount.setEmail(UPDATED_EMAIL);
        testRealmAccount.setFirstName(NEW_FIRST_NAME);
        testRealmAccount.setLastName(NEW_LAST_NAME);
        testRealmAccount.save();
        assertFlashMessageSuccess();

        testRealmAccountManagement.signOut();
        testRealmLogin.form().login(testRealmUser);
        
        testRealmAccountManagement.account();
        assertEquals(testRealmAccount.getEmail(), UPDATED_EMAIL);
        assertEquals(testRealmAccount.getFirstName(), NEW_FIRST_NAME);
        assertEquals(testRealmAccount.getLastName(), NEW_LAST_NAME);
    }

}
