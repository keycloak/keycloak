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
import org.keycloak.testsuite.ui.account2.page.AbstractLoggedInPage;
import org.keycloak.testsuite.ui.account2.page.PersonalInfoPage;

import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.UIUtils.refreshPageAndWaitForLoad;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class PersonalInfoTest extends BaseAccountPageTest {
    @Page
    private PersonalInfoPage personalInfoPage;

    private UserRepresentation testUser2;

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

        assertTrue(personalInfoPage.valuesEqual(testUser));
        personalInfoPage.assertUsernameDisabled(false);
        personalInfoPage.assertSaveDisabled(false);

        personalInfoPage.setValues(testUser2, true);
        assertTrue(personalInfoPage.valuesEqual(testUser2));
        personalInfoPage.assertSaveDisabled(false);
        personalInfoPage.clickSave();
        personalInfoPage.alert().assertSuccess();
        personalInfoPage.assertSaveDisabled(false);

        personalInfoPage.navigateTo();
        personalInfoPage.valuesEqual(testUser2);

        // change just first and last name
        testUser2.setFirstName("Another");
        testUser2.setLastName("Name");
        personalInfoPage.setValues(testUser2, true);
        personalInfoPage.clickSave();
        personalInfoPage.alert().assertSuccess();
        personalInfoPage.navigateTo();
        personalInfoPage.valuesEqual(testUser2);
    }

    @Test
    public void formValidationTest() {
        setEditUsernameAllowed(true);
        
        // clear username
        personalInfoPage.setUsername("");
        personalInfoPage.assertSaveDisabled(true);
        personalInfoPage.assertUsernameValid(false);
        personalInfoPage.setUsername("hsimpson");
        personalInfoPage.assertUsernameValid(true);

        // clear email
        personalInfoPage.setEmail("edewit@");
        personalInfoPage.assertEmailValid(false);
        personalInfoPage.setEmail("");
        personalInfoPage.assertEmailValid(false);
        personalInfoPage.setEmail("hsimpson@springfield.com");
        personalInfoPage.assertEmailValid(true);

        // clear first name
        personalInfoPage.setFirstName("");
        personalInfoPage.assertFirstNameValid(false);
        personalInfoPage.setFirstName("Homer");
        personalInfoPage.assertFirstNameValid(true);

        // clear last name
        personalInfoPage.setLastName("");
        personalInfoPage.assertLastNameValid(false);
        personalInfoPage.setLastName("Simpson");
        personalInfoPage.assertLastNameValid(true);

        // duplicity tests
        ApiUtil.createUserWithAdminClient(testRealmResource(), testUser2);
        // duplicate username
        personalInfoPage.setUsername(testUser2.getUsername());
        personalInfoPage.clickSave();
        personalInfoPage.alert().assertDanger();
        // TODO assert actual error message and that the field is marked as invalid (KEYCLOAK-12102)
        personalInfoPage.setUsername(testUser.getUsername());
        // duplicate email
        personalInfoPage.setEmail(testUser2.getEmail());
        personalInfoPage.clickSave();
        personalInfoPage.alert().assertDanger();
        // TODO assert actual error message and that the field is marked as invalid (KEYCLOAK-12102)
        // check no changes were saved
        personalInfoPage.navigateTo();
        personalInfoPage.valuesEqual(testUser);
    }

    @Test
    public void cancelForm() {
        setEditUsernameAllowed(false);

        personalInfoPage.setValues(testUser2, false);
        personalInfoPage.setEmail("hsimpson@springfield.com");
        personalInfoPage.clickCancel();
        personalInfoPage.valuesEqual(testUser2);
    }

    @Test
    public void disabledEditUsername() {
        setEditUsernameAllowed(false);

        personalInfoPage.assertUsernameDisabled(true);
        personalInfoPage.setValues(testUser2, false);
        personalInfoPage.clickSave();
        personalInfoPage.alert().assertSuccess();

        testUser2.setUsername(testUser.getUsername()); // the username should remain the same
        personalInfoPage.navigateTo();
        personalInfoPage.valuesEqual(testUser2);
    }

    private void setEditUsernameAllowed(boolean value) {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setEditUsernameAllowed(value);
        testRealmResource().update(realm);
        refreshPageAndWaitForLoad();
    }
}
