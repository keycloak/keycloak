/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.account;

import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.keycloak.testsuite.auth.page.login.Registration;

import org.junit.Before;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;
import static org.keycloak.testsuite.admin.Users.getPasswordOf;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public class RegistrationTest extends AbstractAccountManagementTest {

    @Page
    private Registration testRealmRegistrationPage;

    private UserRepresentation newUser;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmRegistrationPage.setAuthRealm(testRealmPage);
    }

    @Before
    public void beforeUserRegistration() {
        // enable user registration in test realm
        RealmRepresentation testRealmRep = testRealmResource().toRepresentation();
        testRealmRep.setRegistrationAllowed(true);
        testRealmResource().update(testRealmRep);

        newUser = createUserRepresentation("new_user", "new_user@email.test", "new", "user", true);
        setPasswordFor(newUser, PASSWORD);

        testRealmAccountManagementPage.navigateTo();
        assertTrue("Registration should be allowed.", testRealmResource().toRepresentation().isRegistrationAllowed());
        testRealmLoginPage.form().register();
    }

    public void assertUserExistsWithAdminClient(UserRepresentation user) {
        assertNotNull(findUserByUsername(testRealmResource(), user.getUsername()));
    }

    public void assertUserDoesntExistWithAdminClient(UserRepresentation user) {
        assertNull(findUserByUsername(testRealmResource(), user.getUsername()));
    }

    public void assertMessageAttributeMissing(String attributeName) {
        String feedbackTest = testRealmRegistrationPage.getFeedbackText();
        String contains = "Please specify " + attributeName + ".";
        assertTrue("'" + feedbackTest + "' doesn't contain '" + contains + "'", feedbackTest.contains(contains));
    }

    @Test
    public void successfulRegistration() {
        testRealmRegistrationPage.register(newUser);
        assertUserExistsWithAdminClient(newUser);
    }

    @Test
    public void invalidEmail() {
        newUser.setEmail("invalid.email.value");
        testRealmRegistrationPage.register(newUser);
        assertEquals("Invalid email address.", testRealmRegistrationPage.getFeedbackText());
        assertUserDoesntExistWithAdminClient(newUser);
    }

    @Test
    public void emptyAttributes() {
        UserRepresentation newUserEmpty = new UserRepresentation(); // empty user attributes

        testRealmRegistrationPage.register(newUserEmpty);
        assertMessageAttributeMissing("username");

        newUserEmpty.setUsername(newUser.getUsername());
        testRealmRegistrationPage.register(newUserEmpty);
        assertMessageAttributeMissing("first name");

        newUserEmpty.setFirstName(newUser.getFirstName());
        testRealmRegistrationPage.register(newUserEmpty);
        assertMessageAttributeMissing("last name");

        newUserEmpty.setLastName(newUser.getLastName());
        testRealmRegistrationPage.register(newUserEmpty);
        assertMessageAttributeMissing("email");

        newUserEmpty.setEmail(newUser.getEmail());
        testRealmRegistrationPage.register(newUserEmpty);
        assertMessageAttributeMissing("password");

        setPasswordFor(newUserEmpty, getPasswordOf(newUser));
        testRealmRegistrationPage.register(newUser);
        assertUserExistsWithAdminClient(newUserEmpty);
    }

    @Test
    public void notMatchingPasswords() {
        testRealmRegistrationPage.setValues(newUser, "not-matching-password");
        testRealmRegistrationPage.submit();
        assertEquals("Password confirmation doesn't match.", testRealmRegistrationPage.getFeedbackText());

        testRealmRegistrationPage.register(newUser);
        assertUserExistsWithAdminClient(newUser);
    }

}
