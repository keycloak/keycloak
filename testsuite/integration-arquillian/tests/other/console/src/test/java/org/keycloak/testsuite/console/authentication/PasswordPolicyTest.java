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
package org.keycloak.testsuite.console.authentication;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.authentication.PasswordPolicy;
import org.keycloak.testsuite.console.page.users.UserCredentials;

import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.console.page.authentication.PasswordPolicy.Type.DIGITS;
import static org.keycloak.testsuite.console.page.authentication.PasswordPolicy.Type.REGEX_PATTERN;

/**
 * @author Petr Mensik
 * @author mhajas
 */
public class PasswordPolicyTest extends AbstractConsoleTest {

    @Page
    private PasswordPolicy passwordPolicyPage;

    @Page
    private UserCredentials testUserCredentialsPage;

    @Before
    public void beforePasswordPolicyTest() {
        testUserCredentialsPage.setId(testUser.getId());
    }

    @Test
    public void testAddAndRemovePolicy() {
        passwordPolicyPage.navigateTo();
        passwordPolicyPage.addPolicy(DIGITS, 5);
        assertAlertSuccess();
        passwordPolicyPage.removePolicy(DIGITS);
        assertAlertSuccess();
    }

    @Test
    public void testInvalidPolicyValues() {
        passwordPolicyPage.navigateTo();
        passwordPolicyPage.addPolicy(DIGITS, "asd");
        assertAlertDanger();
        passwordPolicyPage.removePolicy(DIGITS);

        passwordPolicyPage.addPolicy(REGEX_PATTERN, "([");
        assertAlertDanger();
    }

    @Test
    public void testLengthPolicy() {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setPasswordPolicy("length(8) and ");
        testRealmResource().update(realm);

        testUserCredentialsPage.navigateTo();
        testUserCredentialsPage.resetPassword("1234567");
        assertAlertDanger();

        testUserCredentialsPage.resetPassword("12345678");
        assertAlertSuccess();
    }

    @Test
    public void testDigitsPolicy() {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setPasswordPolicy("digits(2) and ");
        testRealmResource().update(realm);

        testUserCredentialsPage.navigateTo();
        testUserCredentialsPage.resetPassword("invalidPassword1");
        assertAlertDanger();

        testUserCredentialsPage.resetPassword("validPassword12");
        assertAlertSuccess();
    }

    @Test
    public void testLowerCasePolicy() {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setPasswordPolicy("lowerCase(2) and ");
        testRealmResource().update(realm);

        testUserCredentialsPage.navigateTo();
        testUserCredentialsPage.resetPassword("iNVALIDPASSWORD");
        assertAlertDanger();

        testUserCredentialsPage.resetPassword("vaLIDPASSWORD");
        assertAlertSuccess();
    }

    @Test
    public void testUpperCasePolicy() {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setPasswordPolicy("upperCase(2) and ");
        testRealmResource().update(realm);

        testUserCredentialsPage.navigateTo();
        testUserCredentialsPage.resetPassword("Invalidpassword");
        assertAlertDanger();

        testUserCredentialsPage.resetPassword("VAlidpassword");
        assertAlertSuccess();
    }

    @Test
    public void testSpecialCharsPolicy() {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setPasswordPolicy("specialChars(2) and ");
        testRealmResource().update(realm);

        testUserCredentialsPage.navigateTo();
        testUserCredentialsPage.resetPassword("invalidPassword*");
        assertAlertDanger();

        testUserCredentialsPage.resetPassword("validPassword*#");
        assertAlertSuccess();
    }

    @Test
    public void testNotUsernamePolicy() {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setPasswordPolicy("notUsername(1) and ");
        testRealmResource().update(realm);

        testUserCredentialsPage.navigateTo();
        testUserCredentialsPage.resetPassword(testUser.getUsername());
        assertAlertDanger();

        testUserCredentialsPage.resetPassword("validpassword");
        assertAlertSuccess();
    }

    @Test
    public void testRegexPatternsPolicy() {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setPasswordPolicy("regexPattern(^[A-Z]+#[a-z]{8}$) and ");
        testRealmResource().update(realm);

        testUserCredentialsPage.navigateTo();
        testUserCredentialsPage.resetPassword("invalidPassword");
        assertAlertDanger();

        testUserCredentialsPage.resetPassword("VALID#password");
        assertAlertSuccess();
    }

    @Test
    public void testPasswordHistoryPolicy() {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setPasswordPolicy("passwordHistory(2)");
        testRealmResource().update(realm);

        testUserCredentialsPage.navigateTo();
        testUserCredentialsPage.resetPassword("firstPassword");
        assertTrue("Setting the first password should succeed.", alert.isDisplayed() && alert.isSuccess());

        testUserCredentialsPage.resetPassword("secondPassword");
        assertTrue("Setting the second password should succeed.", alert.isDisplayed() && alert.isSuccess());
        
        testUserCredentialsPage.resetPassword("firstPassword");
        assertTrue("Setting a password from recent history should fail.", alert.isDisplayed() && alert.isDanger());

        testUserCredentialsPage.resetPassword("thirdPassword");
        assertTrue("Setting the third password should succeed.", alert.isDisplayed() && alert.isSuccess());

        testUserCredentialsPage.resetPassword("firstPassword");
        assertTrue("Setting an older password should succeed.", alert.isDisplayed() && alert.isSuccess());
    }

}
