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
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.auth.page.account.ChangePassword;
import static org.keycloak.testsuite.admin.Users.getPasswordOf;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author tkyjovsk
 */
public class ChangePasswordTest extends AbstractAccountManagementTest {

    private static final String NEW_PASSWORD = "newpassword";
    private static final String WRONG_PASSWORD = "wrongpassword";

    @Page
    private ChangePassword testRealmChangePasswordPage;

    private String correctPassword;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmChangePasswordPage.setAuthRealm(testRealmPage);
    }

    @Before
    public void beforeChangePasswordTest() {
        correctPassword = getPasswordOf(testUser);
        testRealmAccountManagementPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        testRealmAccountManagementPage.password();
    }

    @Test
    public void invalidChangeAttempts() {
        testRealmChangePasswordPage.save();
        assertAlertError();

        testRealmChangePasswordPage.changePasswords(WRONG_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);
        assertAlertError();

        testRealmChangePasswordPage.changePasswords(correctPassword, NEW_PASSWORD, NEW_PASSWORD + "-mismatch");
        assertAlertError();

        testRealmChangePasswordPage.changePasswords(correctPassword, " ", " ");
        assertAlertError();
    }

    @Test
    public void successfulChangeAttempts() {
        // change password successfully
        testRealmChangePasswordPage.changePasswords(correctPassword, NEW_PASSWORD, NEW_PASSWORD);
        assertAlertSuccess();

        // login using new password
        testRealmAccountManagementPage.signOut();
        testRealmLoginPage.form().login(testUser.getUsername(), NEW_PASSWORD);
        assertCurrentUrlStartsWith(testRealmAccountManagementPage);

        // change password back
        testRealmAccountManagementPage.password();
        testRealmChangePasswordPage.changePasswords(NEW_PASSWORD, correctPassword, correctPassword);
        assertAlertSuccess();
    }

}
