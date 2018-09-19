/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.ui.account2;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.account2.AbstractLoggedInPage;
import org.keycloak.testsuite.auth.page.account2.PersonalInfoPage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class PersonalInfoTest extends BaseAccountPageTest {
    private UserRepresentation testUser2;
    @Page
    private PersonalInfoPage personalInfoPage;

    @Before
    public void setTestUser() {
        testUser2 = new UserRepresentation();
        testUser2.setUsername("vmuzikar");
        testUser2.setEmail("vmuzikar@redhat.com");
        testUser2.setFirstName("Václav");
        testUser2.setLastName("Muzikář");
        ApiUtil.removeUserByUsername(testRealmResource(), testUser2.getUsername());
    }

    @Override
    protected AbstractLoggedInPage getAccountPage() {
        return personalInfoPage;
    }

    @Test
    public void updateUserInfo() {
        setEditUsernameAllowed(true);

        assertTrue(personalInfoPage.personalInfo().valuesEqual(testUser));
        assertFalse(personalInfoPage.personalInfo().isUsernameDisabled());
        assertTrue(personalInfoPage.personalInfo().isSaveDisabled());

        personalInfoPage.personalInfo().setValues(testUser2);
        assertTrue(personalInfoPage.personalInfo().valuesEqual(testUser2));
        assertFalse(personalInfoPage.personalInfo().isSaveDisabled());
        personalInfoPage.personalInfo().clickSave();
        personalInfoPage.alert().assertSuccess();

        personalInfoPage.navigateTo();
        personalInfoPage.personalInfo().valuesEqual(testUser2);

        // change just first and last name
        testUser2.setFirstName("Another");
        testUser2.setLastName("Name");
        personalInfoPage.personalInfo().setValues(testUser2);
        personalInfoPage.personalInfo().clickSave();
        personalInfoPage.alert().assertSuccess();
        personalInfoPage.navigateTo();
        personalInfoPage.personalInfo().valuesEqual(testUser2);
    }

    @Test
    public void formValidationTest() {
        setEditUsernameAllowed(true);
        
        // clear username
        personalInfoPage.personalInfo().setUsername("");
        assertTrue(personalInfoPage.personalInfo().isSaveDisabled());
        personalInfoPage.personalInfo().setUsername("abc");
        assertFalse(personalInfoPage.personalInfo().isSaveDisabled());

        // clear email
        personalInfoPage.personalInfo().setEmail("");
        assertTrue(personalInfoPage.personalInfo().isSaveDisabled());
        personalInfoPage.personalInfo().setEmail("vmuzikar@redhat.com");
        assertFalse(personalInfoPage.personalInfo().isSaveDisabled());

        // TODO test email validation (blocked by KEYCLOAK-8098)

        // clear first name
        personalInfoPage.personalInfo().setFirstName("");
        assertTrue(personalInfoPage.personalInfo().isSaveDisabled());
        personalInfoPage.personalInfo().setFirstName("abc");
        assertFalse(personalInfoPage.personalInfo().isSaveDisabled());

        // clear last name
        personalInfoPage.personalInfo().setLastName("");
        assertTrue(personalInfoPage.personalInfo().isSaveDisabled());
        personalInfoPage.personalInfo().setLastName("abc");
        assertFalse(personalInfoPage.personalInfo().isSaveDisabled());

        // duplicity tests
        ApiUtil.createUserWithAdminClient(testRealmResource(), testUser2);
        // duplicate username
        personalInfoPage.personalInfo().setUsername(testUser2.getUsername());
        personalInfoPage.personalInfo().clickSave();
        personalInfoPage.alert().assertDanger("Username already exists.");
        personalInfoPage.personalInfo().setUsername(testUser.getUsername());
        // duplicate email
        personalInfoPage.personalInfo().setEmail(testUser2.getEmail());
        personalInfoPage.personalInfo().clickSave();
        personalInfoPage.alert().assertDanger("Email already exists.");
        // check no changes were saved
        personalInfoPage.navigateTo();
        personalInfoPage.personalInfo().valuesEqual(testUser);
    }

    @Test
    public void disabledEditUsername() {
        setEditUsernameAllowed(false);

        assertTrue(personalInfoPage.personalInfo().isUsernameDisabled());
        personalInfoPage.personalInfo().setValues(testUser2);
        personalInfoPage.personalInfo().clickSave();
        personalInfoPage.alert().assertSuccess();

        testUser2.setUsername(testUser.getUsername()); // the username should remain the same
        personalInfoPage.navigateTo();
        personalInfoPage.personalInfo().valuesEqual(testUser2);
    }

    private void setEditUsernameAllowed(boolean value) {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setEditUsernameAllowed(value);
        testRealmResource().update(realm);
        personalInfoPage.navigateTo();
    }
}
