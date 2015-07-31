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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.testsuite.page.auth.Registration;

import static org.junit.Assert.*;
import org.junit.Before;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public class RegistrationTest extends AbstractAccountTest {

    @Page
    private Registration registration;

    private UserRepresentation testUser;

    @Before
    public void beforeUserRegistration() {
        RealmRepresentation realm = testRealmResource.toRepresentation();
        realm.setRegistrationAllowed(true);
        testRealmResource.update(realm);

        account.navigateTo();
        testLogin.registration();

        testUser = new UserRepresentation();
        testUser.setUsername("user");
        testUser.credential(PASSWORD, "password");
        testUser.setEmail("user@redhat.com");
        testUser.setFirstName("user");
        testUser.setLastName("test");
    }

    @Test
    public void registerNewUserTest() {
        registration.registerNewUser(testUser);
        assertNotNull(findUserByUsername(testRealmResource, testUser.getUsername()));
    }

    @Test
    public void registerNewUserWithWrongEmail() {
        testUser.setEmail("newUser.redhat.com");
        registration.registerNewUser(testUser);
        assertTrue(registration.isInvalidEmail());
        assertNull(findUserByUsername(testRealmResource, testUser.getUsername()));
    }

    @Test
    public void registerNewUserWithWrongAttributes() {
        testUser = new UserRepresentation();

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
        testUser.setUsername("user");
        registration.registerNewUser(testUser);
        assertFalse(registration.isAttributeSpecified("password"));
        testUser.credential(PASSWORD, "password");
        registration.registerNewUser(testUser);
        assertNotNull(findUserByUsername(testRealmResource, testUser.getUsername()));
    }

    @Test
    public void registerNewUserWithNotMatchingPasswords() {
        registration.registerNewUser(testUser, "psswd");
        assertFalse(registration.isPasswordSame());
        registration.registerNewUser(testUser);
        assertNotNull(findUserByUsername(testRealmResource, testUser.getUsername()));
    }

}
