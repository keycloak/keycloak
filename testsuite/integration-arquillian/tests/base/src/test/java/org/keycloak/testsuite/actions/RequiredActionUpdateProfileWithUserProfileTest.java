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
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.forms.RegisterWithUserProfileTest;
import org.keycloak.testsuite.forms.VerifyProfileTest;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfileEditUsernameAllowedPage;
import org.keycloak.testsuite.util.ClientScopeBuilder;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;

import org.apache.commons.lang3.StringUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        UserProfileUtil.setUserProfileConfiguration(testRealm(), null);

        ApiUtil.removeUserByUsername(testRealm(), "test-user@localhost");
        UserRepresentation user = UserBuilder.create().enabled(true)
                .username("test-user@localhost")
                .email("test-user@localhost")
                .firstName("Tom")
                .lastName("Brady")
                .emailVerified(true)
                .requiredAction(UserModel.RequiredAction.UPDATE_PROFILE.name()).build();
        ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");

        ApiUtil.removeUserByUsername(testRealm(), "john-doh@localhost");
        user = UserBuilder.create().enabled(true)
                .username("john-doh@localhost")
                .email("john-doh@localhost")
                .firstName("John")
                .lastName("Doh")
                .emailVerified(true)
                .requiredAction(UserModel.RequiredAction.UPDATE_PROFILE.name()).build();
        ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");
    }

    @Test
    public void testDisplayName() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\",\"displayName\":\"${firstName}\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", \"displayName\" : \"Department\", " + PERMISSIONS_ALL + ", \"required\":{}}"
                + "]}");

        loginPage.open();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();

        //assert field names
        // i18n replaced
        Assert.assertEquals("First name", updateProfilePage.getLabelForField("firstName"));
        // attribute name used if no display name set
        Assert.assertEquals("lastName", updateProfilePage.getLabelForField("lastName"));
        // direct value in display name
        Assert.assertEquals("Department", updateProfilePage.getLabelForField("department"));

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

        loginPage.open();
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

        loginPage.open();
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

        loginPage.open();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();

        RegisterWithUserProfileTest.assertFieldTypes(driver);
    }

    @Test
    public void testUsernameOnlyIfEditAllowed() {
        RealmRepresentation realm = testRealm().toRepresentation();

        boolean r = realm.isEditUsernameAllowed();
        try {
            realm.setEditUsernameAllowed(false);
            testRealm().update(realm);

            loginPage.open();
            loginPage.login(USERNAME1, PASSWORD);

            assertFalse(updateProfilePage.isUsernamePresent());

            realm.setEditUsernameAllowed(true);
            testRealm().update(realm);

            driver.navigate().refresh();
            assertTrue(updateProfilePage.isUsernamePresent());
        } finally {
            realm.setEditUsernameAllowed(r);
            testRealm().update(realm);
        }
    }

    @Test
    public void testOptionalAttribute() {
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "}"
                + "]}");

        loginPage.open();

        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();
        assertFalse(updateProfilePage.isCancelDisplayed());

        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("New first").lastName("").email("new@email.com").submit();

        events.expectRequiredAction(EventType.UPDATE_PROFILE).detail(Details.PREVIOUS_FIRST_NAME, "Tom").detail(Details.UPDATED_FIRST_NAME, "New first")
                .detail(Details.PREVIOUS_LAST_NAME, "Brady")
                .detail(Details.PREVIOUS_EMAIL, USERNAME1).detail(Details.UPDATED_EMAIL, "new@email.com")
                .assertEvent();
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().assertEvent();

        // assert user is really updated in persistent store
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, USERNAME1);
        Assert.assertEquals("New first", user.getFirstName());
        assertThat(StringUtils.isEmpty(user.getLastName()), is(true));
        Assert.assertEquals("new@email.com", user.getEmail());
        Assert.assertEquals(USERNAME1, user.getUsername());
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

        loginPage.open();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();
        //submit with error
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("First").lastName("L").email(USERNAME1).submit();

        updateProfilePage.assertCurrent();
        //submit OK
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("First").lastName("Last").email(USERNAME1).submit();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

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

        loginPage.open();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();
        Assert.assertEquals("Brady", updateProfilePage.getLastName());
        Assert.assertFalse(updateProfilePage.isDepartmentEnabled());

        //update of the other attributes must be successful in this case
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("First").lastName("Last").email(USERNAME1).submit();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

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

        loginPage.open();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();
        Assert.assertEquals("last", updateProfilePage.getLastName());
        Assert.assertFalse(updateProfilePage.isDepartmentEnabled());

        //update of the other attributes must be successful in this case
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("First").lastName("Last").email(USERNAME1).submit();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

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

        loginPage.open();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();
        Assert.assertEquals("Brady", updateProfilePage.getLastName());
        Assert.assertFalse("'department' field is visible", updateProfilePage.isDepartmentPresent());

        //update of the other attributes must be successful in this case
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("First").lastName("Last").email(USERNAME1).submit();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

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

        loginPage.open();
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
        events.expectRequiredAction(EventType.UPDATE_PROFILE)
                .detail(Details.PREVIOUS_FIRST_NAME, "Tom").detail(Details.UPDATED_FIRST_NAME, "FirstCC")
                .detail(Details.PREVIOUS_LAST_NAME, "Brady").detail(Details.UPDATED_LAST_NAME, "LastCC")
                .detail(Details.PREF_UPDATED + "department", "DepartmentCC")
                .assertEvent();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

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

        oauth.scope(SCOPE_DEPARTMENT).clientId(client_scope_optional.getClientId()).openLoginForm();

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

        events.expectRequiredAction(EventType.UPDATE_PROFILE).client(client_scope_optional.getClientId())
                .detail(Details.PREVIOUS_FIRST_NAME, "Tom").detail(Details.UPDATED_FIRST_NAME, "FirstCC")
                .detail(Details.PREVIOUS_LAST_NAME, "Brady").detail(Details.UPDATED_LAST_NAME, "LastCC")
                .assertEvent();


        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

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

        oauth.clientId(client_scope_default.getClientId()).openLoginForm();

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

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

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

        oauth.scope(SCOPE_DEPARTMENT).clientId(client_scope_optional.getClientId()).openLoginForm();

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

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

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

        oauth.scope(SCOPE_DEPARTMENT).clientId(client_scope_optional.getClientId()).openLoginForm();

        loginPage.assertCurrent();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();

        Assert.assertTrue(updateProfilePage.isDepartmentPresent());
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("FirstCC").lastName("LastCC").email(USERNAME1)
                .department("DepartmentCC").submit();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

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

        oauth.clientId(client_scope_optional.getClientId()).openLoginForm();

        loginPage.assertCurrent();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();

        Assert.assertFalse(updateProfilePage.isDepartmentPresent());
        updateProfilePage.prepareUpdate().username(USERNAME1).firstName("FirstCC").lastName("LastCC").email(USERNAME1).submit();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUserByUsername(USERNAME1);
        assertEquals("FirstCC", user.getFirstName());
        assertEquals("LastCC", user.getLastName());
    }

    protected void setUserProfileConfiguration(String configuration) {
        UserProfileUtil.setUserProfileConfiguration(testRealm(), configuration);
    }

    protected UserRepresentation getUserByUsername(String username) {
        return VerifyProfileTest.getUserByUsername(testRealm(), username);
    }

    protected void updateUserByUsername(String username, String firstName, String lastName, String department) {
        UserRepresentation ur = getUserByUsername(username);
        ur.setFirstName(firstName);
        ur.setLastName(lastName);
        ur.singleAttribute(ATTRIBUTE_DEPARTMENT, department);
        testRealm().users().get(ur.getId()).update(ur);
    }

}
