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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.console.page.users.UserAttributes;

import static org.junit.Assert.*;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;

/**
 *
 * @author Filip Kiss
 * @author tkyjovsk
 */
public class UserAttributesTest extends AbstractUserTest {

    @Page
    private UserAttributes userAttributesPage;

    @Before
    public void beforeUserAttributesTest() {
        usersPage.navigateTo();
    }
    
    @Test
    public void invalidEmail() {
        String testUsername = "testUserInvEmail";
        String invalidEmail = "user.redhat.com";
        newTestRealmUser.setUsername(testUsername);
        setPasswordFor(newTestRealmUser, "pass");
        newTestRealmUser.setEmail(invalidEmail);
        createUser(newTestRealmUser);
        assertAlertDanger();

        userAttributesPage.backToUsersViaBreadcrumb();
        assertNull(usersPage.table().findUser(testUsername));
    }

    @Test
    public void noUsername() {
        createUser(newTestRealmUser);
        assertAlertDanger();
    }

    @Test
    public void existingUser() {
        String testUsername = "test_duplicated_user";
        newTestRealmUser.setUsername(testUsername);
        createUser(newTestRealmUser);
        assertAlertSuccess();

        userAttributesPage.backToUsersViaBreadcrumb();
        assertNotNull(usersPage.table().findUser(testUsername));

        UserRepresentation testUser2 = new UserRepresentation();
        testUser2.setUsername(testUsername);
        createUser(testUser2);
        assertAlertDanger();
    }

    @Test
    public void disabledUser() {
        UserRepresentation disabledUser = new UserRepresentation();
        disabledUser.setEnabled(false);
        disabledUser.setUsername("disabled_user");
        createUser(disabledUser);
        assertAlertSuccess();
        // TODO try to log in
    }

}
