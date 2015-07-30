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
import org.junit.Test;
import org.keycloak.testsuite.console.page.fragment.FlashMessage;
import org.keycloak.testsuite.console.page.settings.user.UserPage;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.console.AbstractAdminConsoleTest;

/**
 *
 * @author Filip Kiss
 */
public class AddNewUserTest extends AbstractAdminConsoleTest {
    
    @Page
    private UserPage page;

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;

    private UserRepresentation testUser;
    
    @Before
    public void beforeAddNewUserTest() {
        navigation.users();
        testUser = new UserRepresentation();
    }

    @Test
    public void addUserWithInvalidEmailTest() {
        String testUsername = "testUserInvEmail";
        String invalidEmail = "user.redhat.com";
        testUser.setUsername(testUsername);
        testUser.credential(PASSWORD, "pass");
        testUser.setEmail(invalidEmail);
        page.addUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
        navigation.users();
        assertNull(page.findUser(testUsername));
    }

    @Test
    public void addUserWithNoUsernameTest() {
        page.addUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
    }

    @Ignore
    @Test
    public void addUserWithLongNameTest() {
        String longUserName = "thisisthelongestnameeveranditcannotbeusedwhencreatingnewuserinkeycloak";
        testUser.setUsername(longUserName);
        navigation.users();
        page.addUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
        assertNull(page.findUser(testUser.getUsername()));
    }

    @Test
    public void addDuplicatedUser() {
        String testUsername = "test_duplicated_user";
        testUser.setUsername(testUsername);
        page.addUser(testUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        navigation.users();
        assertNotNull(page.findUser(testUsername));

        UserRepresentation testUser2 = new UserRepresentation();
        testUser2.setUsername(testUsername);
        page.addUser(testUser2);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
        navigation.users();
        page.deleteUser(testUsername);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(page.findUser(testUser2.getUsername()));
    }

    @Test
    public void addDisabledUser() {
        UserRepresentation disabledUser = new UserRepresentation();
        disabledUser.setEnabled(false);
        disabledUser.setUsername("disabled_user");
        page.addUser(disabledUser);
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        navigation.users();
        page.deleteUser(disabledUser.getUsername());
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(page.findUser(disabledUser.getUsername()));
    }

}
