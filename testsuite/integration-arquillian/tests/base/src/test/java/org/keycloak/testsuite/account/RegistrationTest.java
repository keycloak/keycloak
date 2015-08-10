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
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;
import static org.keycloak.testsuite.admin.Users.getPasswordCredentialValueOf;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public class RegistrationTest extends AbstractAccountManagementTest {

    @Page
    private Registration testRealmRegistration;

    private UserRepresentation newUser;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmRegistration.setAuthRealm(testRealm);
    }

    @Before
    public void beforeUserRegistration() {
        // enable user registration in test realm
        RealmRepresentation testRealmRep = testRealmResource().toRepresentation();
        testRealmRep.setRegistrationAllowed(true);
        testRealmResource().update(testRealmRep);

        newUser = createUserRepresentation("new_user", "new_user@email.test", "new", "user", true);
        newUser.credential(PASSWORD, PASSWORD);

        testRealmAccountManagement.navigateTo();
        testRealmLogin.form().register();
    }

    public void assertUserExistsWithAdminClient(UserRepresentation user) {
        assertNotNull(findUserByUsername(testRealmResource(), user.getUsername()));
    }

    public void assertUserDoesntExistWithAdminClient(UserRepresentation user) {
        assertNull(findUserByUsername(testRealmResource(), user.getUsername()));
    }

    public void assertMessageAttributeMissing(String attributeName) {
        assertTrue(testRealmRegistration.getFeedbackText()
                .contains("Please specify " + attributeName + "."));
    }

    @Test
    public void successfulRegistration() {
        testRealmRegistration.register(newUser);
        assertUserExistsWithAdminClient(newUser);
    }

    @Test
    public void invalidEmail() {
        newUser.setEmail("invalid.email.value");
        testRealmRegistration.register(newUser);
        assertTrue(testRealmRegistration.getFeedbackText()
                .equals("Invalid email address."));
        assertUserDoesntExistWithAdminClient(newUser);
    }

    @Test
    public void emptyAttributes() {
        UserRepresentation newUserEmpty = new UserRepresentation(); // empty user attributes

        testRealmRegistration.register(newUserEmpty);
        assertMessageAttributeMissing("username");

        newUserEmpty.setUsername(newUser.getUsername());
        testRealmRegistration.register(newUserEmpty);
        assertMessageAttributeMissing("first name");

        newUserEmpty.setFirstName(newUser.getFirstName());
        testRealmRegistration.register(newUserEmpty);
        assertMessageAttributeMissing("last name");

        newUserEmpty.setLastName(newUser.getLastName());
        testRealmRegistration.register(newUserEmpty);
        assertMessageAttributeMissing("email");

        newUserEmpty.setEmail(newUser.getEmail());
        testRealmRegistration.register(newUserEmpty);
        assertMessageAttributeMissing("password");

        newUserEmpty.credential(PASSWORD, getPasswordCredentialValueOf(newUser));
        testRealmRegistration.register(newUser);
        assertUserExistsWithAdminClient(newUserEmpty);
    }

    @Test
    public void notMatchingPasswords() {
        testRealmRegistration.setValues(newUser, "not-matching-password");
        testRealmRegistration.submit();
        assertTrue(testRealmRegistration.getFeedbackText()
                .equals("Password confirmation doesn't match."));

        testRealmRegistration.register(newUser);
        assertUserExistsWithAdminClient(newUser);
    }

}
