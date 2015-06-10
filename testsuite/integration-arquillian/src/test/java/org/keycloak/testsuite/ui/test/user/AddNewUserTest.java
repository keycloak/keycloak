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

package org.keycloak.testsuite.ui.test.user;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.junit.Test;
import org.keycloak.testsuite.ui.fragment.FlashMessage;
import org.keycloak.testsuite.ui.model.User;
import org.keycloak.testsuite.ui.page.settings.user.UserPage;


import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.keycloak.testsuite.ui.AbstractKeyCloakTest;
import static org.keycloak.testsuite.ui.util.Users.TEST_USER1;

/**
 *
 * @author Filip Kiss
 */
public class AddNewUserTest extends AbstractKeyCloakTest<UserPage> {

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;

	@Before
	public void beforeAddNewUserTest() {
		navigation.users();
	}
	
    @Test
    public void addUserWithInvalidEmailTest() {
        String testUsername = "testUserInvEmail";
        String invalidEmail = "user.redhat.com";
        User testUser = new User(testUsername, "pass", invalidEmail);
        page.addUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
        navigation.users();
        assertNull(page.findUser(testUsername));
    }

    @Test
    public void addUserWithNoUsernameTest() {
        User testUser = new User();
        page.addUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
    }

	@Ignore
	@Test
    public void addUserWithLongNameTest() {
        String longUserName = "thisisthelongestnameeveranditcannotbeusedwhencreatingnewuserinkeycloak";
        User testUser = new User(longUserName);
        navigation.users();
        page.addUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
        assertNull(page.findUser(testUser.getUserName()));
    }

    @Test
    public void addDuplicatedUser() {
        String testUsername = "test_duplicated_user";
        User testUser = new User(testUsername);
        page.addUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
		navigation.users();
        assertNotNull(page.findUser(testUsername));

        User testUser2 = new User(testUsername);
        page.addUser(testUser2);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
		navigation.users();
        page.deleteUser(testUsername);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(page.findUser(testUser2.getUserName()));
    }

    @Test
    public void addDisabledUser() {
        User disabledUser = new User(TEST_USER1);
		disabledUser.setUserEnabled(false);
		disabledUser.setUserName("disabled_user");
        page.addUser(disabledUser);
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
		navigation.users();
        page.deleteUser(disabledUser.getUserName());
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(page.findUser(disabledUser.getUserName()));
    }

    



}
