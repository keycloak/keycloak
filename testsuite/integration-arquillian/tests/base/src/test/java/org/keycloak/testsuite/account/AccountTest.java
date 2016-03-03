/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
    private Account testRealmAccountPage;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmAccountPage.setAuthRealm(testRealmPage);
    }
    
    @Before
    public void beforeAccountTest() {
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
    }

    @After
    public void afterAccountTest() {
        testRealmAccountManagementPage.navigateTo();
        testRealmAccountManagementPage.signOut();
    }

    @Test
    public void editAccount() {
        testRealmAccountManagementPage.account();
        assertEquals(testRealmAccountPage.getUsername(), testUser.getUsername());
        
        testRealmAccountPage.setEmail(UPDATED_EMAIL);
        testRealmAccountPage.setFirstName(NEW_FIRST_NAME);
        testRealmAccountPage.setLastName(NEW_LAST_NAME);
        testRealmAccountPage.save();
        assertAlertSuccess();

        testRealmAccountManagementPage.signOut();
        testRealmLoginPage.form().login(testUser);
        
        testRealmAccountManagementPage.account();
        assertEquals(testRealmAccountPage.getEmail(), UPDATED_EMAIL);
        assertEquals(testRealmAccountPage.getFirstName(), NEW_FIRST_NAME);
        assertEquals(testRealmAccountPage.getLastName(), NEW_LAST_NAME);
    }

}
