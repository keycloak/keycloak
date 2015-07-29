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
package org.keycloak.testsuite.account;

import java.util.ArrayList;
import java.util.List;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Test;
import org.keycloak.testsuite.model.User;
import org.keycloak.testsuite.page.auth.Registration;

import static org.junit.Assert.*;
import org.junit.Before;
import org.keycloak.representations.idm.CredentialRepresentation;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.account.page.Account;
import static org.keycloak.testsuite.page.auth.AuthRealm.MASTER;
import static org.keycloak.testsuite.page.auth.AuthRealm.TEST;
import static org.keycloak.testsuite.util.Users.TEST_USER1;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public class RegisterNewUserTest extends AbstractAccountManagementTest {

    private UserRepresentation testUser;

    @Page
    private Account account;

    @Page
    private Registration registration;

//    @Page
//    private UserPage userPage;

    @Before
    public void beforeUserRegistration() {
        RealmRepresentation realm = keycloak.realm(TEST).toRepresentation();
        realm.setRegistrationAllowed(true);
        keycloak.realm(MASTER).update(realm);

        account.navigateTo();
        login.registration();

        testUser = new UserRepresentation();
        testUser.setUsername("user");
        List<CredentialRepresentation> credentials = new ArrayList<>();
        CredentialRepresentation cr = new CredentialRepresentation();
        cr.setType(PASSWORD);
        cr.setValue("password");
        credentials.add(cr);
        testUser.setCredentials(credentials);
        testUser.setEmail("user@redhat.com");
        testUser.setFirstName("user");
        testUser.setLastName("test");
    }

    @After
    public void afterUserRegistration() {
        RealmRepresentation realm = keycloak.realm(MASTER).toRepresentation();
        realm.setRegistrationAllowed(false);
        keycloak.realm(MASTER).update(realm);
    }

    public UserRepresentation findUserByUserName(String userName) {
        UserRepresentation user = null;
        List<UserRepresentation> ur = keycloak.realm(MASTER).users().search(userName, null, null);
        if (ur.size() == 1) {
            user = ur.get(0);
        }
        return user;
    }

    @Test
    public void registerNewUserTest() {
        registration.registerNewUser(TEST_USER1);
        assertNotNull(findUserByUserName(TEST_USER1.getUserName()));
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
        assertNull(findUserByUserName(TEST_USER1.getUserName()));
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

    }

    @Test
    public void registerNewUserWithNotMatchingPasswords() {
        registration.registerNewUser(TEST_USER1, "psswd");
        assertFalse(registration.isPasswordSame());
        registration.registerNewUser(TEST_USER1);
    }

}
