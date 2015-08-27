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
import org.keycloak.testsuite.auth.page.account.ChangePassword;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.authentication.PasswordPolicy;
import org.keycloak.testsuite.util.SeleniumUtils;
import org.openqa.selenium.By;

import static org.keycloak.testsuite.admin.Users.getPasswordOf;
import static org.keycloak.testsuite.console.page.authentication.PasswordPolicy.Type.*;

/**
 * @author Petr Mensik
 * @author mhajas
 */
public class PasswordPolicyTest extends AbstractConsoleTest {

    @Page
    private PasswordPolicy passwordPolicy;

    @Page
    private ChangePassword changePassword;

    @Before
    public void beforePasswordPolicyTest() {
        SeleniumUtils.waitGuiForElement(By.tagName("h2"));
        configure().authentication();
        passwordPolicy.tabs().passwordPolicy();
        changePassword.setAuthRealm("test");
    }

    @Test
    public void testAddPolicy() {
        passwordPolicy.addPolicy(HASH_ITERATIONS, 5);
        assertFlashMessageSuccess();
    }

    @Test
    public void testRemovePolicy() {
        passwordPolicy.addPolicy(HASH_ITERATIONS, 5);

        passwordPolicy.removePolicy(HASH_ITERATIONS);
        assertFlashMessageSuccess();
    }

    @Test
    public void testAddPolicyWithWrongArguments() {
        passwordPolicy.addPolicy(HASH_ITERATIONS, "asd");
        assertFlashMessageDanger();
        passwordPolicy.removePolicy(HASH_ITERATIONS);

        passwordPolicy.addPolicy(REGEX_PATTERNS, "^[A-Z]{8,5}");
        assertFlashMessageDanger();
    }

    @Test
    public void testAddRegexPatternsPolicy() {
        passwordPolicy.addPolicy(REGEX_PATTERNS, "^[A-Z]{5}");
        assertFlashMessageSuccess();
    }

    @Test
    public void testLengthPolicy() {
        passwordPolicy.addPolicy(LENGTH, 8);

        navigateToTestRealmChangePassword();

        changePassword.changePasswords(getPasswordOf(testRealmUser), "1234567", "1234567");
        assertFlashMessageError();

        changePassword.changePasswords(getPasswordOf(testRealmUser), "12345678", "12345678");
        assertFlashMessageSuccess();
    }

    @Test
    public void testDigitsPolicy() {
        passwordPolicy.addPolicy(DIGITS, 2);

        navigateToTestRealmChangePassword();

        changePassword.changePasswords(getPasswordOf(testRealmUser), "invalidPassword1", "invalidPassword1");
        assertFlashMessageError();

        changePassword.changePasswords(getPasswordOf(testRealmUser), "validPassword12", "validPassword12");
        assertFlashMessageSuccess();
    }

    @Test
    public void testLowerCasePolicy() {
        passwordPolicy.addPolicy(LOWER_CASE, 2);

        navigateToTestRealmChangePassword();

        changePassword.changePasswords(getPasswordOf(testRealmUser), "iNVALIDPASSWORD", "iNVALIDPASSWORD");
        assertFlashMessageError();

        changePassword.changePasswords(getPasswordOf(testRealmUser), "vaLIDPASSWORD", "vaLIDPASSWORD");
        assertFlashMessageSuccess();
    }

    @Test
    public void testUpperCasePolicy() {
        passwordPolicy.addPolicy(UPPER_CASE, 2);

        navigateToTestRealmChangePassword();

        changePassword.changePasswords(getPasswordOf(testRealmUser), "Invalidpassword", "Invalidpassword");
        assertFlashMessageError();

        changePassword.changePasswords(getPasswordOf(testRealmUser), "VAlidpassword", "VAlidpassword");
        assertFlashMessageSuccess();
    }

    @Test
    public void testSpecialCharsPolicy() {
        passwordPolicy.addPolicy(SPECIAL_CHARS, 2);

        navigateToTestRealmChangePassword();

        changePassword.changePasswords(getPasswordOf(testRealmUser), "invalidPassword*", "invalidPassword*");
        assertFlashMessageError();

        changePassword.changePasswords(getPasswordOf(testRealmUser), "validPassword*#", "validPassword*#");
        assertFlashMessageSuccess();
    }

    @Test
    public void testNotUsernamePolicy() {
        passwordPolicy.addPolicy(NOT_USERNAME);

        navigateToTestRealmChangePassword();

        changePassword.changePasswords(getPasswordOf(testRealmUser), testRealmUser.getUsername(), testRealmUser.getUsername());
        assertFlashMessageError();

        changePassword.changePasswords(getPasswordOf(testRealmUser), "validpassword", "validpassword");
        assertFlashMessageSuccess();
    }

    @Test
    public void testRegexPatternsPolicy() {
        passwordPolicy.addPolicy(REGEX_PATTERNS, "^[A-Z]+#[a-z]{8}$");

        navigateToTestRealmChangePassword();

        changePassword.changePasswords(getPasswordOf(testRealmUser), "invalidPassword", "invalidPassword");
        assertFlashMessageError();

        changePassword.changePasswords(getPasswordOf(testRealmUser), "VALID#password", "VALID#password");
        assertFlashMessageSuccess();
    }

    @Test
    public void testPasswordHistoryPolicy() {
        passwordPolicy.addPolicy(PASSWORD_HISTORY, 2);

        navigateToTestRealmChangePassword();

        changePassword.changePasswords(getPasswordOf(testRealmUser), "firstPassword", "firstPassword");
        assertFlashMessageSuccess();

        changePassword.changePasswords("firstPassword", "secondPassword", "secondPassword");
        assertFlashMessageSuccess();

        changePassword.changePasswords("secondPassword", "firstPassword", "firstPassword");
        assertFlashMessageError();
    }

    private void navigateToTestRealmChangePassword() {
        changePassword.navigateTo();
        testRealmLogin.form().login(testRealmUser);
        changePassword.password();
    }
}
