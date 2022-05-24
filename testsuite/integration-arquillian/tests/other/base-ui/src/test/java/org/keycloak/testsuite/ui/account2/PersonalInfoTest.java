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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import static org.keycloak.testsuite.util.UIUtils.refreshPageAndWaitForLoad;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.ui.account2.page.AbstractLoggedInPage;
import org.keycloak.testsuite.ui.account2.page.PersonalInfoPage;
import org.keycloak.testsuite.util.UserBuilder;

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
        //assertEquals("test user", personalInfoPage.header().getToolbarLoggedInUser());
        assertTrue(personalInfoPage.valuesEqual(testUser2));
        personalInfoPage.assertSaveDisabled(false);
        personalInfoPage.clickSave();
        personalInfoPage.alert().assertSuccess();
        personalInfoPage.assertSaveDisabled(false);

        personalInfoPage.navigateTo();
        personalInfoPage.valuesEqual(testUser2);
        //assertEquals("Václav Muzikář", personalInfoPage.header().getToolbarLoggedInUser());

        // change just first and last name
        testUser2.setFirstName("Another");
        testUser2.setLastName("Name");
        personalInfoPage.setValues(testUser2, true);
        personalInfoPage.clickSave();
        personalInfoPage.alert().assertSuccess();
        personalInfoPage.navigateTo();
        personalInfoPage.valuesEqual(testUser2);
        //assertEquals("Another Name", personalInfoPage.header().getToolbarLoggedInUser());
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
    
    @Test
    public void clickLogoTest() {
        personalInfoPage.clickBrandLink();
        accountWelcomeScreen.assertCurrent();
    }

    @Ignore("Username is not included in the account console anymore, but it should be there.")
    @Test
    public void testNameInToolbar() {
        assertEquals("test user", personalInfoPage.header().getToolbarLoggedInUser());

        UserRepresentation user = new UserRepresentation();
        user.setUsername("edewit");
        user.setEnabled(true);
        setPasswordFor(user, "password");
        try {
            ApiUtil.removeUserByUsername(testRealmResource(), testUser.getUsername());
            personalInfoPage.navigateTo();
            ApiUtil.createUserWithAdminClient(testRealmResource(), user);
            loginPage.form().login(user);

            assertEquals("edewit", personalInfoPage.header().getToolbarLoggedInUser());
        } finally {
            ApiUtil.removeUserByUsername(testRealmResource(), user.getUsername());
        }
    }

    private void setEditUsernameAllowed(boolean value) {
        RealmRepresentation realm = testRealmResource().toRepresentation();
        realm.setEditUsernameAllowed(value);
        testRealmResource().update(realm);
        refreshPageAndWaitForLoad();
    }

    private void addUser(String username, String email) {
        UserRepresentation user = UserBuilder.create()
                .username(username)
                .enabled(true)
                .email(email)
                .firstName("first")
                .lastName("last")
                .build();
        RealmResource testRealm = adminClient.realm("test");
        ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm, user, "password");
    }

    // KEYCLOAK-15634
    @Test
    public void updateProfileWithAttributePresent() {

        RealmResource testRealm = adminClient.realm("test");
        assertEquals(getAccountThemeName(), testRealm.toRepresentation().getAccountTheme());

        // Add a user and set a test attribute
        addUser("keycloak-15634","keycloak-15634@test.local");
        UserRepresentation userRepBefore = ApiUtil.findUserByUsername(testRealm,"keycloak-15634");
        Map<String, List<String>> userAttributes = new HashMap<>();
        userAttributes.put("testAttribute", Collections.singletonList("testValue"));
        userRepBefore.setAttributes(userAttributes);
        testRealm.users().get(userRepBefore.getId()).update(userRepBefore);

        // Check our test user is ok before updating profile with account v2
        UserRepresentation updatedUserRep = ApiUtil.findUserByUsername(testRealm,"keycloak-15634");
        assertEquals("keycloak-15634@test.local", updatedUserRep.getEmail());
        assertEquals("testAttribute should be set", "testValue", updatedUserRep.getAttributes().get("testAttribute").get(0));
        assertFalse("locale attribute should not be set", updatedUserRep.getAttributes().containsKey("locale"));

        personalInfoPage.assertCurrent();
        personalInfoPage.header().clickLogoutBtn();
        personalInfoPage.navigateTo();
        loginPage.assertCurrent();
        loginPage.form().login("keycloak-15634","password");
        personalInfoPage.assertCurrent();

        // Trigger the JS involved in KEYCLOAK-15634
        assertEquals("keycloak-15634@test.local", personalInfoPage.getEmail());
        personalInfoPage.setEmail("keycloak-15634@domain.local");
        personalInfoPage.clickSave();

        // Check if updateProfile went well and if testAttribute is still there
        UserRepresentation userRepAfter = ApiUtil.findUserByUsername(testRealm,"keycloak-15634");
        assertEquals("keycloak-15634@domain.local", userRepAfter.getEmail());
        assertEquals("testAttribute should still be there","testValue", userRepAfter.getAttributes().get("testAttribute").get(0));

        ApiUtil.removeUserByUsername(testRealm, "keycloak-15634");
    }

    @Test
    // https://issues.redhat.com/browse/KEYCLOAK-16890
    // Stored personal info triggers attack via the display of user name in header.
    // If user name is left unsanitized, this test will fail with
    // org.openqa.selenium.UnhandledAlertException: unexpected alert open: {Alert text : XSS}
    public void storedXSSAttack() {
        personalInfoPage.navigateTo();
        testUser.setFirstName("<img src=x onerror=\"alert('XSS');\">");
        personalInfoPage.setValues(testUser, false);
        personalInfoPage.clickSave();

        personalInfoPage.header().clickLogoutBtn();
        accountWelcomeScreen.header().clickLoginBtn();
        loginPage.form().login(testUser);
        personalInfoPage.navigateTo();
    }

}
