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
package org.keycloak.testsuite.console.user;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Test;
import org.keycloak.testsuite.console.page.fragment.FlashMessage;
import org.keycloak.testsuite.model.User;
import org.keycloak.testsuite.page.auth.Registration;
import org.keycloak.testsuite.console.page.settings.user.UserPage;

import static org.junit.Assert.*;
import org.junit.Before;
import org.keycloak.testsuite.console.AbstractAdminConsoleTest;
import org.keycloak.testsuite.console.page.settings.LoginSettingsPage;
import static org.keycloak.testsuite.util.Users.*;

/**
 *
 * @author Petr Mensik
 */
public class RegisterNewUserTest extends AbstractAdminConsoleTest {

    @Page
    private Registration registration;
    
    @Page
    private UserPage userPage;

    @Page
    private LoginSettingsPage loginSettingsPage;

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;

    @Before
    public void beforeUserRegistration() {
        navigation.settings("master");
        navigation.login("master");
        loginSettingsPage.enableUserRegistration();
        logOut();
        loginPage.goToUserRegistration();
    }

    @After
    public void afterUserRegistration() {
        navigation.settings("master");
        navigation.login("master");
        loginSettingsPage.disableUserRegistration();
    }

    @Test
    public void registerNewUserTest() {
        registration.registerNewUser(TEST_USER1);
        logOut();
        loginAsAdmin();
        navigation.users();
        userPage.deleteUser(TEST_USER1.getUserName());
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
    }

    @Test
    public void registerNewUserWithWrongEmail() {
        User testUser = new User(TEST_USER1);
        testUser.setEmail("newUser.redhat.com");
        registration.registerNewUser(testUser);
        assertTrue(registration.isInvalidEmail());
        registration.backToLoginPage();
        loginAsAdmin();
        navigation.users();
        assertNull(userPage.findUser(testUser.getUserName()));
    }

    @Test
    public void registerNewUserWithWrongAttributes() {
        User testUser = new User();

        registration.registerNewUser(testUser);
        assertFalse(registration.isAttributeSpecified("first name"));
        testUser.setFirstName("name");
        registration.registerNewUser(testUser);
        assertFalse(registration.isAttributeSpecified("last name"));
        testUser.setLastName("surname");
        registration.registerNewUser(testUser);
        assertFalse(registration.isAttributeSpecified("email"));
        testUser.setEmail("mail@redhat.com");
        registration.registerNewUser(testUser);
        assertFalse(registration.isAttributeSpecified("username"));
        testUser.setUserName("user");
        registration.registerNewUser(testUser);
        assertFalse(registration.isAttributeSpecified("password"));
        testUser.setPassword("password");
        registration.registerNewUser(testUser);
        logOut();
        loginAsAdmin();
        navigation.users();
        userPage.deleteUser(TEST_USER1.getUserName());
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
    }

    @Test
    public void registerNewUserWithNotMatchingPasswords() {
        registration.registerNewUser(TEST_USER1, "psswd");
        assertFalse(registration.isPasswordSame());
        registration.registerNewUser(TEST_USER1);
        logOut();
        loginAsAdmin();
        navigation.users();
        userPage.deleteUser(TEST_USER1.getUserName());
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
    }

}
