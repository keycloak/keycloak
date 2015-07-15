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

package org.keycloak.testsuite.login;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.admin.fragment.FlashMessage;
import org.keycloak.testsuite.admin.model.User;
import org.keycloak.testsuite.admin.page.RegisterPage;
import org.keycloak.testsuite.admin.page.settings.user.UserPage;

import static org.junit.Assert.*;
import org.junit.Before;
import org.keycloak.testsuite.admin.AbstractKeycloakTest;
import org.keycloak.testsuite.admin.page.settings.LoginSettingsPage;
import static org.keycloak.testsuite.admin.util.Users.*;

/**
 *
 * @author Petr Mensik
 */
// Ignoring this test as it's not working and it's duplicating existing tests
@Ignore
public class RegisterNewUserTest extends AbstractKeycloakTest<RegisterPage> {

    @Page
    private UserPage userPage;

	@Page
	private LoginSettingsPage loginSettingsPage;
	
    @FindByJQuery(".alert")
    private FlashMessage flashMessage;
	
	@Before
	public void beforeUserRegistration() {
		navigation.settings();
		navigation.login();
		loginSettingsPage.enableUserRegistration();
		logOut();
        openAccount();
		loginPage.goToUserRegistration();
	}
	
	@After
	public void afterUserRegistration() {
		navigation.settings();
		navigation.login();
		loginSettingsPage.disableUserRegistration();
	}

    @Test
    public void registerNewUserTest() {
        page.registerNewUser(TEST_USER1);
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
        page.registerNewUser(testUser);
        assertTrue(page.isInvalidEmail());
		page.backToLoginPage();
        loginAsAdmin();
        navigation.users();
        assertNull(userPage.findUser(testUser.getUserName()));
    }

    @Test
    public void registerNewUserWithWrongAttributes() {
		User testUser = new User();
		
        page.registerNewUser(testUser);
        assertFalse(page.isAttributeSpecified("first name"));
		testUser.setFirstName("name");
        page.registerNewUser(testUser);
        assertFalse(page.isAttributeSpecified("last name"));
		testUser.setLastName("surname");
        page.registerNewUser(testUser);
        assertFalse(page.isAttributeSpecified("email"));
		testUser.setEmail("mail@redhat.com");
        page.registerNewUser(testUser);
        assertFalse(page.isAttributeSpecified("username"));
		testUser.setUserName("user");
        page.registerNewUser(testUser);
        assertFalse(page.isAttributeSpecified("password"));
		testUser.setPassword("password");
        page.registerNewUser(testUser);
		logOut();
		loginAsAdmin();
        navigation.users();
        userPage.deleteUser(TEST_USER1.getUserName());
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
    }

    @Test
    public void registerNewUserWithNotMatchingPasswords() {
        page.registerNewUser(TEST_USER1, "psswd");
        assertFalse(page.isPasswordSame());
        page.registerNewUser(TEST_USER1);
		logOut();
        loginAsAdmin();
        navigation.users();
        userPage.deleteUser(TEST_USER1.getUserName());
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
    }

}
