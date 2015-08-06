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
package org.keycloak.testsuite.console.users;

import org.junit.Test;

import static org.junit.Assert.*;
import org.junit.Ignore;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.representations.idm.UserRepresentation;

/**
 *
 * @author Filip Kiss
 */
public class UserAttributesTest extends AbstractUserTest {
    
    @Test
    public void addUserWithInvalidEmailTest() {
        String testUsername = "testUserInvEmail";
        String invalidEmail = "user.redhat.com";
        newTestRealmUser.setUsername(testUsername);
        newTestRealmUser.credential(PASSWORD, "pass");
        newTestRealmUser.setEmail(invalidEmail);
        createUser(newTestRealmUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
        users.navigateTo();
        assertNull(users.findUser(testUsername));
    }

    @Test
    public void addUserWithNoUsernameTest() {
        createUser(newTestRealmUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
    }

    @Ignore
    @Test
    public void addUserWithLongNameTest() {
        String longUserName = "thisisthelongestnameeveranditcannotbeusedwhencreatingnewuserinkeycloak";
        newTestRealmUser.setUsername(longUserName);
        createUser(newTestRealmUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
        assertNull(users.findUser(newTestRealmUser.getUsername()));
    }

    @Test
    public void addDuplicatedUser() {
        String testUsername = "test_duplicated_user";
        newTestRealmUser.setUsername(testUsername);
        createUser(newTestRealmUser);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        users.navigateTo();
        assertNotNull(users.findUser(testUsername));

        UserRepresentation testUser2 = new UserRepresentation();
        testUser2.setUsername(testUsername);
        createUser(testUser2);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
        users.navigateTo();
        users.deleteUser(testUsername);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(users.findUser(testUser2.getUsername()));
    }

    @Test
    public void addDisabledUser() {
        UserRepresentation disabledUser = new UserRepresentation();
        disabledUser.setEnabled(false);
        disabledUser.setUsername("disabled_user");
        createUser(disabledUser);
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        users.navigateTo();
        users.deleteUser(disabledUser.getUsername());
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(users.findUser(disabledUser.getUsername()));
    }
    
}
