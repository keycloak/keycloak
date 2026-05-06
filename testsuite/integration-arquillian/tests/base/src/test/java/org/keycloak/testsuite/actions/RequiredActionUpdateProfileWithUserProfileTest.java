/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.ClientScopeBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AdminApiUtil;
import org.keycloak.testsuite.forms.RegisterWithUserProfileTest;
import org.keycloak.testsuite.forms.VerifyProfileTest;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfileEditUsernameAllowedPage;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;

import org.apache.commons.lang3.StringUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.keycloak.testsuite.util.userprofile.UserProfileUtil.ATTRIBUTE_DEPARTMENT;
import static org.keycloak.testsuite.util.userprofile.UserProfileUtil.CONFIGURATION_FOR_USER_EDIT;
import static org.keycloak.testsuite.util.userprofile.UserProfileUtil.PERMISSIONS_ADMIN_EDITABLE;
import static org.keycloak.testsuite.util.userprofile.UserProfileUtil.PERMISSIONS_ADMIN_ONLY;
import static org.keycloak.testsuite.util.userprofile.UserProfileUtil.PERMISSIONS_ALL;
import static org.keycloak.testsuite.util.userprofile.UserProfileUtil.SCOPE_DEPARTMENT;
import static org.keycloak.testsuite.util.userprofile.UserProfileUtil.VALIDATIONS_LENGTH;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test update-profile required action with custom user profile configurations
 *
 * @author Vlastimil Elias <velias@redhat.com>
 */
public class RequiredActionUpdateProfileWithUserProfileTest extends AbstractTestRealmKeycloakTest {

    protected static final String PASSWORD = "password";
    protected static final String USERNAME1 = "test-user@localhost";

    private static ClientRepresentation client_scope_default;
    private static ClientRepresentation client_scope_optional;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginUpdateProfileEditUsernameAllowedPage updateProfilePage;

    @Page
    protected ErrorPage errorPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setClientScopes(new ArrayList<>());
        testRealm.getClientScopes().add(ClientScopeBuilder.create().name(SCOPE_DEPARTMENT).protocol("openid-connect").build());
        testRealm.getClientScopes().add(ClientScopeBuilder.create().name("profile").protocol("openid-connect").build());

        client_scope_default = KeycloakModelUtils.createClient(testRealm, "client-a");
        client_scope_default.setDefaultClientScopes(Collections.singletonList(SCOPE_DEPARTMENT));
        client_scope_default.setRedirectUris(Collections.singletonList("*"));
        client_scope_optional = KeycloakModelUtils.createClient(testRealm, "client-b");
        client_scope_optional.setOptionalClientScopes(Collections.singletonList(SCOPE_DEPARTMENT));
        client_scope_optional.setRedirectUris(Collections.singletonList("*"));

    }

    @Before
    public void beforeTest() {
        UserProfileUtil.setUserProfileConfiguration(managedRealm.admin(), null);

        AdminApiUtil.removeUserByUsername(managedRealm.admin(), "test-user@localhost");
        UserRepresentation user = UserBuilder.create().enabled(true)
                .username("test-user@localhost")
                .email("test-user@localhost")
                .firstName("Tom")
                .lastName("Brady")
                .emailVerified(true)
                .requiredActions(UserModel.RequiredAction.UPDATE_PROFILE.name()).build();
        AdminApiUtil.createUserAndResetPasswordWithAdminClient(managedRealm.admin(), user, "password");

        AdminApiUtil.removeUserByUsername(managedRealm.admin(), "john-doh@localhost");
        user = UserBuilder.create().enabled(true)
                .username("john-doh@localhost")
                .email("john-doh@localhost")
                .firstName("John")
                .lastName("Doh")
                .emailVerified(true)
                .requiredActions(UserModel.RequiredAction.UPDATE_PROFILE.name()).build();
        AdminApiUtil.createUserAndResetPasswordWithAdminClient(managedRealm.admin(), user, "password");
    }

    @Test
    public void testDisplayName() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\",\"displayName\":\"${firstName}\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", \"displayName\" : \"Department\", " + PERMISSIONS_ALL + ", \"required\":{}}"
                + "]}");

        oauth.openLoginForm();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();

        //assert field names
        // i18n replaced
        Assertions.assertEquals("First name", updateProfilePage.getLabelForField("firstName"));
        // attribute name used if no display name set
        Assertions.assertEquals("lastName", updateProfilePage.getLabelForField("lastName"));
        // direct value in display name
        Assertions.assertEquals("Department", updateProfilePage.getLabelForField("department"));

    }

    @Test
    public void testAttributeGrouping() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"lastName\"," + UserProfileUtil.PERMISSIONS_ALL + "},"
                + "{\"name\": \"username\", " + UserProfileUtil.PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\"," + UserProfileUtil.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\", " + UserProfileUtil.PERMISSIONS_ALL + ", \"required\":{}, \"group\": \"company\"},"
                + "{\"name\": \"email\", " + UserProfileUtil.PERMISSIONS_ALL + ", \"group\": \"contact\"}"
                + "], \"groups\": ["
                + "{\"name\": \"company\", \"displayDescription\": \"Company field desc\" },"
                + "{\"name\": \"contact\" }"
                + "]}");

        oauth.openLoginForm();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();

        //assert fields and groups location in form, attributes without a group appear first
        List<WebElement> element = driver.findElements(By.cssSelector("form#kc-update-profile-form label"));
        String[] labelOrder = new String[]{"lastName", "username", "firstName", "header-company", "description-company", "department", "header-contact", "email"};
        for (int i = 0; i < element.size(); i++) {
            WebElement webElement = element.get(i);
            String id;
            if (webElement.getAttribute("for") != null) {
                id = webElement.getAttribute("for");
                // see that the label has an element it belongs to
                assertThat("Label with id: " + id + " should have component it belongs to", driver.findElement(By.id(id)).isDisplayed(), is(true));
            } else {
                id = webElement.getAttribute("id");
            }
            assertThat("Label at index: " + i + " with id: " + id + " was not in found in the same order in the dom", id, is(labelOrder[i]));
        }
    }


    @Test
    public void testAttributeGuiOrder() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"lastName\"," + UserProfileUtil.PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", " + UserProfileUtil.PERMISSIONS_ALL + ", \"required\":{}},"
                + "{\"name\": \"username\", " + UserProfileUtil.PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\"," + UserProfileUtil.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"email\", " + UserProfileUtil.PERMISSIONS_ALL + "}"
                + "]}");

        oauth.openLoginForm();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();

        //assert fields location in form
        //assert fields and groups location in form, attributes without a group appear first
        List<WebElement> element = driver.findElements(By.cssSelector("form#kc-update-profile-form input"));
        String[] labelOrder = new String[]{"lastName", "department", "username", "firstName", "email"};
        for (int i = 0; i < labelOrder.length; i++) {
            WebElement webElement = element.get(i);
            String id = webElement.getAttribute("id");
            assertThat("Field at index: " + i + " with id: " + id + " was not in found in the same order in the dom", id, is(labelOrder[i]));
        }
    }

    @Test
    public void testAttributeInputTypes() {

        setUserProfileConfiguration("{\"attributes\": ["
                + RegisterWithUserProfileTest.UP_CONFIG_PART_INPUT_TYPES
                + "]}");

        oauth.openLoginForm();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();

        RegisterWithUserProfileTest.assertFieldTypes(driver);
    }

    @Test
    public void testUsernameOnlyIfEditAllowed() {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();

        boolean r = realm.isEditUsernameAllowed();
        try {
            realm.setEditUsernameAllowed(false);
            managedRealm.admin().update(realm);

            oauth.openLoginForm();
            loginPage.login(USERNAME1, PASSWORD);

            assertFalse(updateProfilePage.isUsernamePresent());

            realm.setEditUsernameAllowed(true);
            managedRealm.admin().update(realm);

            driver.navigate().refresh();
            assertTrue(updateProfilePage.isUsernamePresent());
        } finally {
            realm.setEditUsernameAllowed(r);
            managedRealm.admin().update(realm);
        }
    }

    @Test
    public void testOptionalAttribute() {
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "}"
                + "]}");

        oauth.openLoginForm();

        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();
        assertFalse(updateProfilePage.isCancelDisplayed());

        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("New first").lastName("").email("new@email.com").submit();

        EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_PROFILE).details(Details.PREVIOUS_FIRST_NAME, "Tom").details(Details.UPDATED_FIRST_NAME, "New first")
                .details(Details.PREVIOUS_LAST_NAME, "Brady")
                .details(Details.PREVIOUS_EMAIL, USERNAME1).details(Details.UPDATED_EMAIL, "new@email.com");
        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventAssertion.expectLoginSuccess(events.poll());

        // assert user is really updated in persistent store
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, USERNAME1);
        Assertions.assertEquals("New first", user.getFirstName());
        assertThat(StringUtils.isEmpty(user.getLastName()), is(true));
        Assertions.assertEquals("new@email.com", user.getEmail());
        Assertions.assertEquals(USERNAME1, user.getUsername());
        assertNull(user.getLastName());
    }

    @Test
    public void testCustomValidationLastName() {

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUserByUsername(USERNAME1, "ExistingFirst", "La", "Department");

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "," + VALIDATIONS_LENGTH + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ADMIN_ONLY + "}"
                + "]}");

        oauth.openLoginForm();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();
        //submit with error
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("First").lastName("L").email(USERNAME1).submit();

        updateProfilePage.assertCurrent();
        //submit OK
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("First").lastName("Last").email(USERNAME1).submit();

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUserByUsername(USERNAME1);
        assertEquals("First", user.getFirstName());
        assertEquals("Last", user.getLastName());
        //check that not configured attribute is unchanged
        assertEquals("Department", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testRequiredReadOnlyAttribute() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ADMIN_EDITABLE + ", \"required\":{}}"
                + "]}");

        oauth.openLoginForm();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();
        Assertions.assertEquals("Brady", updateProfilePage.getLastName());
        Assertions.assertFalse(updateProfilePage.isDepartmentEnabled());

        //update of the other attributes must be successful in this case
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("First").lastName("Last").email(USERNAME1).submit();

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUserByUsername(USERNAME1);
        assertEquals("First", user.getFirstName());
        assertEquals("Last", user.getLastName());
    }

    @Test
    public void testRequiredReadOnlyExistingAttribute() {
        updateUserByUsername(USERNAME1, "first", "last", "foo");
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ADMIN_EDITABLE + ", \"required\":{}}"
                + "]}");

        oauth.openLoginForm();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();
        Assertions.assertEquals("last", updateProfilePage.getLastName());
        Assertions.assertFalse(updateProfilePage.isDepartmentEnabled());

        //update of the other attributes must be successful in this case
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("First").lastName("Last").email(USERNAME1).submit();

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUserByUsername(USERNAME1);
        assertEquals("First", user.getFirstName());
        assertEquals("Last", user.getLastName());
    }

    @Test
    public void testAttributeNotVisible() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ADMIN_ONLY + ", \"required\":{}}"
                + "]}");

        oauth.openLoginForm();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();
        Assertions.assertEquals("Brady", updateProfilePage.getLastName());
        Assertions.assertFalse(updateProfilePage.isDepartmentPresent(), "'department' field is visible");

        //update of the other attributes must be successful in this case
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("First").lastName("Last").email(USERNAME1).submit();

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUserByUsername(USERNAME1);
        assertEquals("First", user.getFirstName());
        assertEquals("Last", user.getLastName());
    }

    @Test
    public void testRequiredAttribute() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}}"
                + "]}");

        oauth.openLoginForm();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();

        //submit with error
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("FirstCC").lastName("LastCC").email(USERNAME1)
                .department("").submit();
        updateProfilePage.assertCurrent();

        //submit OK
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("FirstCC").lastName("LastCC").email(USERNAME1)
                .department("DepartmentCC").submit();

        // we also test additional attribute configured to be audited in the event
        EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_PROFILE)
                .details(Details.PREVIOUS_FIRST_NAME, "Tom").details(Details.UPDATED_FIRST_NAME, "FirstCC")
                .details(Details.PREVIOUS_LAST_NAME, "Brady").details(Details.UPDATED_LAST_NAME, "LastCC")
                .details(Details.PREF_UPDATED + "department", "DepartmentCC");

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUserByUsername(USERNAME1);
        assertEquals("FirstCC", user.getFirstName());
        assertEquals("LastCC", user.getLastName());
        assertEquals("DepartmentCC", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testAttributeRequiredForScope() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{\"scopes\":[\"" + SCOPE_DEPARTMENT + "\"]}}"
                + "]}");

        oauth.scope(SCOPE_DEPARTMENT).client(client_scope_optional.getClientId()).openLoginForm();

        loginPage.assertCurrent();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();

        //submit with error
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("FirstCC").lastName("LastCC").email(USERNAME1)
                .department("").submit();
        updateProfilePage.assertCurrent();

        //submit OK
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("FirstCC").lastName("LastCC").email(USERNAME1)
                .department("DepartmentCC").submit();

        EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_PROFILE).clientId(client_scope_optional.getClientId())
                .details(Details.PREVIOUS_FIRST_NAME, "Tom").details(Details.UPDATED_FIRST_NAME, "FirstCC")
                .details(Details.PREVIOUS_LAST_NAME, "Brady").details(Details.UPDATED_LAST_NAME, "LastCC")
                ;


        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUserByUsername(USERNAME1);
        assertEquals("FirstCC", user.getFirstName());
        assertEquals("LastCC", user.getLastName());
        assertEquals("DepartmentCC", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testAttributeRequiredForDefaultScope() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{\"scopes\":[\"" + SCOPE_DEPARTMENT + "\"]}}"
                + "]}");

        oauth.client(client_scope_default.getClientId()).openLoginForm();

        loginPage.assertCurrent();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();

        //submit with error
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("FirstCC").lastName("LastCC").email(USERNAME1)
                .department("").submit();
        updateProfilePage.assertCurrent();

        //submit OK
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("FirstCC").lastName("LastCC").email(USERNAME1)
                .department("DepartmentCC").submit();

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUserByUsername(USERNAME1);
        assertEquals("FirstCC", user.getFirstName());
        assertEquals("LastCC", user.getLastName());
        assertEquals("DepartmentCC", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testAttributeRequiredAndSelectedByScope() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\"" + SCOPE_DEPARTMENT + "\"]}}"
                + "]}");

        oauth.scope(SCOPE_DEPARTMENT).client(client_scope_optional.getClientId()).openLoginForm();

        loginPage.assertCurrent();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();

        //submit with error
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("FirstCC").lastName("LastCC").email(USERNAME1)
                .department("").submit();
        updateProfilePage.assertCurrent();

        //submit OK
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("FirstCC").lastName("LastCC").email(USERNAME1)
                .department("DepartmentCC").submit();

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUserByUsername(USERNAME1);
        assertEquals("FirstCC", user.getFirstName());
        assertEquals("LastCC", user.getLastName());
        assertEquals("DepartmentCC", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testAttributeNotRequiredAndSelectedByScopeCanBeUpdated() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"selector\":{\"scopes\":[\"" + SCOPE_DEPARTMENT + "\"]}}"
                + "]}");

        oauth.scope(SCOPE_DEPARTMENT).client(client_scope_optional.getClientId()).openLoginForm();

        loginPage.assertCurrent();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();

        Assertions.assertTrue(updateProfilePage.isDepartmentPresent());
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("FirstCC").lastName("LastCC").email(USERNAME1)
                .department("DepartmentCC").submit();

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUserByUsername(USERNAME1);
        assertEquals("FirstCC", user.getFirstName());
        assertEquals("LastCC", user.getLastName());
        assertEquals("DepartmentCC", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testAttributeRequiredButNotSelectedByScopeIsNotRendered() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\"" + SCOPE_DEPARTMENT + "\"]}}"
                + "]}");

        oauth.client(client_scope_optional.getClientId()).openLoginForm();

        loginPage.assertCurrent();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();

        Assertions.assertFalse(updateProfilePage.isDepartmentPresent());
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("FirstCC").lastName("LastCC").email(USERNAME1).submit();

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUserByUsername(USERNAME1);
        assertEquals("FirstCC", user.getFirstName());
        assertEquals("LastCC", user.getLastName());
    }

    protected void setUserProfileConfiguration(String configuration) {
        UserProfileUtil.setUserProfileConfiguration(managedRealm.admin(), configuration);
    }

    protected UserRepresentation getUserByUsername(String username) {
        return VerifyProfileTest.getUserByUsername(managedRealm.admin(), username);
    }

    protected void updateUserByUsername(String username, String firstName, String lastName, String department) {
        UserRepresentation ur = getUserByUsername(username);
        ur.setFirstName(firstName);
        ur.setLastName(lastName);
        ur.singleAttribute(ATTRIBUTE_DEPARTMENT, department);
        managedRealm.admin().users().get(ur.getId()).update(ur);
    }

}
