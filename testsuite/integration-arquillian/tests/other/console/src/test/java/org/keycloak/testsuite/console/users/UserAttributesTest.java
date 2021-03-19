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
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.testsuite.console.page.users.UserAttributes;
import org.keycloak.testsuite.console.page.groups.CreateGroup;
import org.keycloak.testsuite.console.page.groups.Groups;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author Filip Kiss
 * @author tkyjovsk
 */
public class UserAttributesTest extends AbstractUserTest {

    @Page
    private UserAttributes userAttributesPage;

    @Page
    protected Groups groupsPage;

    @Page
    protected CreateGroup createGroupPage;

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
    public void createUserEmailAsUserName() {
        RealmRepresentation representation = testRealmResource().toRepresentation();
        representation.setRegistrationEmailAsUsername(true);
        testRealmResource().update(representation);
        
        newTestRealmUser.setEmail("test@keycloak.org");
        createUser(newTestRealmUser);
        assertAlertSuccess();
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

    @Test
    public void createUserWithGroups() {
        GroupRepresentation newGroup = new GroupRepresentation();
        newGroup.setName("mygroup");

        // navigate to Groups creation page
        groupsPage.navigateTo();
        assertCurrentUrlEquals(groupsPage);
        groupsPage.table().addGroup();
        assertCurrentUrlStartsWith(createGroupPage);

        // create the group
        createGroupPage.form().setValues(newGroup);
        createGroupPage.form().save();
        assertAlertSuccess();

        // navigate to Users creation page
        usersPage.navigateTo();
        RealmRepresentation representation = testRealmResource().toRepresentation();
        representation.setRegistrationEmailAsUsername(true);
        testRealmResource().update(representation);
        newTestRealmUser.setEmail("test-with-groups@keycloak.org");
        newTestRealmUser.setGroups(Arrays.asList("mygroup"));
        createUser(newTestRealmUser);
        assertAlertSuccess();
    }

}
