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
import org.keycloak.testsuite.auth.page.login.Registration;

import static org.junit.Assert.*;
import org.junit.Before;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.util.ApiUtil.findUserByUsername;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public class RegistrationTest extends AbstractAccountManagementTest {

    @Page
    private Registration testRealmRegistration;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmRegistration.setAuthRealm(TEST);
    }

    @Before
    public void beforeUserRegistration() {
        RealmRepresentation realm = testRealmResource.toRepresentation();
        realm.setRegistrationAllowed(true);
        testRealmResource.update(realm);

        account.navigateTo();
        testRealmLogin.form().register();

        testRealmUser.credential(PASSWORD, PASSWORD);
    }

    @Test
    public void registerNewUserTest() {
        testRealmRegistration.registerNewUser(testRealmUser);
        // verify via admin api
        assertNotNull(findUserByUsername(testRealmResource, testRealmUser.getUsername()));
    }

    @Test
    public void registerNewUserWithWrongEmail() {
        testRealmUser.setEmail("newUser.redhat.com");
        testRealmRegistration.registerNewUser(testRealmUser);
        assertTrue(testRealmRegistration.isInvalidEmail());
        // verify via admin api
        assertNull(findUserByUsername(testRealmResource, testRealmUser.getUsername()));
    }

    @Test
    public void registerNewUserWithWrongAttributes() {
        testRealmUser = new UserRepresentation();

        testRealmRegistration.registerNewUser(testRealmUser);
        assertFalse(testRealmRegistration.isAttributeSpecified("first name"));
        testRealmUser.setFirstName("name");
        testRealmRegistration.registerNewUser(testRealmUser);
        assertFalse(testRealmRegistration.isAttributeSpecified("last name"));
        testRealmUser.setLastName("surname");
        testRealmRegistration.registerNewUser(testRealmUser);
        assertFalse(testRealmRegistration.isAttributeSpecified("email"));
        testRealmUser.setEmail("mail@redhat.com");
        testRealmRegistration.registerNewUser(testRealmUser);
        assertFalse(testRealmRegistration.isAttributeSpecified("username"));
        testRealmUser.setUsername("user");
        testRealmRegistration.registerNewUser(testRealmUser);
        assertFalse(testRealmRegistration.isAttributeSpecified("password"));
        testRealmUser.credential(PASSWORD, PASSWORD);
        testRealmRegistration.registerNewUser(testRealmUser);
        assertNotNull(findUserByUsername(testRealmResource, testRealmUser.getUsername()));
    }

    @Test
    public void registerNewUserWithNotMatchingPasswords() {
        testRealmRegistration.registerNewUser(testRealmUser, "psswd");
        assertFalse(testRealmRegistration.isPasswordSame());
        testRealmRegistration.registerNewUser(testRealmUser);
        assertNotNull(findUserByUsername(testRealmResource, testRealmUser.getUsername()));
    }

}
