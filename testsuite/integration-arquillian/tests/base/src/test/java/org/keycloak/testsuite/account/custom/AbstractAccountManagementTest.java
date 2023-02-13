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

package org.keycloak.testsuite.account.custom;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.auth.page.account.AccountManagement;
import org.keycloak.testsuite.auth.page.account.fragment.AccountManagementPatternFlyAlert;

import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractAccountManagementTest extends AbstractAuthTest {

    @Page
    protected AccountManagement testRealmAccountManagementPage;

    @Page
    protected AccountManagementPatternFlyAlert alert;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(TEST);
        testRealmAccountManagementPage.setAuthRealm(TEST);
    }

    @Before
    public void beforeAbstractAccountTest() {
        // make user test user exists in test realm
        createTestUserWithAdminClient();
    }

    public void assertAlertSuccess() {
        assertTrue(alert.isSuccess());
    }

    public void assertAlertError() {
        assertTrue(alert.isError());
    }

}
