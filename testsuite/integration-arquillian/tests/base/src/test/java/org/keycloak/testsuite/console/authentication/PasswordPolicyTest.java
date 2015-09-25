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
import org.junit.Test;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.authentication.PasswordPolicy;
import org.keycloak.testsuite.console.page.users.UserCredentials;

import static org.keycloak.testsuite.console.page.authentication.PasswordPolicy.Type.*;

/**
 * @author Petr Mensik
 * @author mhajas
 */
//@Ignore // FIXME still unstable
public class PasswordPolicyTest extends AbstractConsoleTest {

    @Page
    private PasswordPolicy passwordPolicyPage;

    @Page
    private UserCredentials testUserCredentialsPage;

    @Before
    public void beforePasswordPolicyTest() {
        testUserCredentialsPage.setId(testUser.getId());
        passwordPolicyPage.navigateTo();
    }

    @Test
    public void testAddAndRemovePolicy() {
        passwordPolicyPage.addPolicy(HASH_ITERATIONS, 5);
        passwordPolicyPage.removePolicy(HASH_ITERATIONS);
        assertFlashMessageSuccess();
    }

    @Test
    public void testInvalidPolicyValues() {
        passwordPolicyPage.addPolicy(HASH_ITERATIONS, "asd");
        assertFlashMessageDanger();
        passwordPolicyPage.removePolicy(HASH_ITERATIONS);

        passwordPolicyPage.addPolicy(REGEX_PATTERN, "^[A-Z]{8,5}");
        assertFlashMessageDanger();
    }

    @Test
    public void testLengthPolicy() {
        passwordPolicyPage.addPolicy(LENGTH, 8);

        testUserCredentialsPage.navigateTo();
        testUserCredentialsPage.resetPassword("1234567");
        assertFlashMessageDanger();

        testUserCredentialsPage.resetPassword("12345678");
        assertFlashMessageSuccess();
    }

    @Test
    public void testDigitsPolicy() {
        passwordPolicyPage.addPolicy(DIGITS, 2);

        testUserCredentialsPage.navigateTo();
        testUserCredentialsPage.resetPassword("invalidPassword1");
        assertFlashMessageDanger();

        testUserCredentialsPage.resetPassword("validPassword12");
        assertFlashMessageSuccess();
    }

    @Test
    public void testLowerCasePolicy() {
        passwordPolicyPage.addPolicy(LOWER_CASE, 2);

        testUserCredentialsPage.navigateTo();
        testUserCredentialsPage.resetPassword("iNVALIDPASSWORD");
        assertFlashMessageDanger();

        testUserCredentialsPage.resetPassword("vaLIDPASSWORD");
        assertFlashMessageSuccess();
    }

    @Test
    public void testUpperCasePolicy() {
        passwordPolicyPage.addPolicy(UPPER_CASE, 2);

        testUserCredentialsPage.navigateTo();
        testUserCredentialsPage.resetPassword("Invalidpassword");
        assertFlashMessageDanger();

        testUserCredentialsPage.resetPassword("VAlidpassword");
        assertFlashMessageSuccess();
    }

    @Test
    public void testSpecialCharsPolicy() {
        passwordPolicyPage.addPolicy(SPECIAL_CHARS, 2);

        testUserCredentialsPage.navigateTo();
        testUserCredentialsPage.resetPassword("invalidPassword*");
        assertFlashMessageDanger();

        testUserCredentialsPage.resetPassword("validPassword*#");
        assertFlashMessageSuccess();
    }

    @Test
    public void testNotUsernamePolicy() {
        passwordPolicyPage.addPolicy(NOT_USERNAME);

        testUserCredentialsPage.navigateTo();
        testUserCredentialsPage.resetPassword(testUser.getUsername());
        assertFlashMessageDanger();

        testUserCredentialsPage.resetPassword("validpassword");
        assertFlashMessageSuccess();
    }

    @Test
    public void testRegexPatternsPolicy() {
        passwordPolicyPage.addPolicy(REGEX_PATTERN, "^[A-Z]+#[a-z]{8}$");

        testUserCredentialsPage.navigateTo();
        testUserCredentialsPage.resetPassword("invalidPassword");
        assertFlashMessageDanger();

        testUserCredentialsPage.resetPassword("VALID#password");
        assertFlashMessageSuccess();
    }

    @Test
    public void testPasswordHistoryPolicy() {
        passwordPolicyPage.addPolicy(PASSWORD_HISTORY, 2);

        testUserCredentialsPage.navigateTo();
        testUserCredentialsPage.resetPassword("firstPassword");
        assertFlashMessageSuccess();

        testUserCredentialsPage.resetPassword("secondPassword");
        assertFlashMessageSuccess();

        testUserCredentialsPage.resetPassword("firstPassword");
        assertFlashMessageDanger();
    }

}
