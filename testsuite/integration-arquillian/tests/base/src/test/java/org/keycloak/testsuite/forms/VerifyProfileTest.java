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
package org.keycloak.testsuite.forms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testsuite.AbstractChangeImportedUserPasswordsTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.VerifyProfilePage;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.AssertAdminEvents;
import org.keycloak.testsuite.util.ClientScopeBuilder;
import org.keycloak.testsuite.util.JsonTestUtils;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.util.JsonSerialization;

import org.apache.commons.lang3.StringUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
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
import static org.junit.Assert.assertTrue;

/**
 * @author Vlastimil Elias <velias@redhat.com>
 */
public class VerifyProfileTest extends AbstractChangeImportedUserPasswordsTest {

    private static String userId;

    private static String user2Id;

    private static String user3Id;

    private static String user4Id;

    private static String user5Id;

    private static String user6Id;

    private static String userWithoutEmailId;

    private static ClientRepresentation client_scope_default;
    private static ClientRepresentation client_scope_optional;

    @Override
    protected boolean removeVerifyProfileAtImport() {
        // we need the verify profile action enabled as default
        return false;
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);
        UserRepresentation user = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-test").email("login@test.com").enabled(true).password(generatePassword("login-test")).build();
        UserRepresentation user2 = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-test2").email("login2@test.com").enabled(true).password(generatePassword("login-test2")).build();
        UserRepresentation user3 = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-test3").email("login3@test.com").enabled(true).password(generatePassword("login-test3")).lastName("ExistingLast").build();
        UserRepresentation user4 = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-test4").email("login4@test.com").enabled(true).password(generatePassword("login-test4")).lastName("ExistingLast").build();
        UserRepresentation user5 = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-test5").email("login5@test.com").enabled(true).password(generatePassword("login-test5")).firstName("ExistingFirst").lastName("ExistingLast").build();
        UserRepresentation user6 = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-test6").email("login6@test.com").enabled(true).password(generatePassword("login-test6")).firstName("ExistingFirst").lastName("ExistingLast").build();
        UserRepresentation userWithoutEmail = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-nomail").enabled(true).password(generatePassword("login-nomail")).firstName("NoMailFirst").lastName("NoMailLast").build();

        RealmBuilder.edit(testRealm).user(user).user(user2).user(user3).user(user4).user(user5).user(user6).user(userWithoutEmail);

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

    @Override
    public void importTestRealms() {
        super.importTestRealms();
        userId = adminClient.realm("test").users().search("login-test", true).get(0).getId();
        user2Id = adminClient.realm("test").users().search("login-test2", true).get(0).getId();
        user3Id = adminClient.realm("test").users().search("login-test3", true).get(0).getId();
        user4Id = adminClient.realm("test").users().search("login-test4", true).get(0).getId();
        user5Id = adminClient.realm("test").users().search("login-test5", true).get(0).getId();
        user6Id = adminClient.realm("test").users().search("login-test6", true).get(0).getId();
        userWithoutEmailId = adminClient.realm("test").users().search("login-nomail", true).get(0).getId();
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public AssertAdminEvents assertAdminEvents = new AssertAdminEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected VerifyProfilePage verifyProfilePage;

    @ArquillianResource
    protected OAuthClient oauth;

    @Test
    public void testDisplayName() {

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\",\"displayName\":\"${firstName}\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", \"displayName\" : \"Department\", " + PERMISSIONS_ALL + ", \"required\":{}}"
                + "]}");

        loginPage.open();
        loginPage.login("login-test5", getPassword("login-test5"));

        verifyProfilePage.assertCurrent();

        //assert field names
        // i18n replaced
        Assert.assertEquals("First name",verifyProfilePage.getLabelForField("firstName"));
        // attribute name used if no display name set
        Assert.assertEquals("lastName",verifyProfilePage.getLabelForField("lastName"));
        // direct value in display name
        Assert.assertEquals("Department",verifyProfilePage.getLabelForField("department"));
    }

    @Test
    public void testAttributeGrouping() {

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"username\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\", " + PERMISSIONS_ALL + ", \"required\":{}, \"group\": \"company\"},"
                + "{\"name\": \"email\", " + PERMISSIONS_ALL + ", \"group\": \"contact\"}"
                + "], \"groups\": ["
                + "{\"name\": \"company\", \"displayDescription\": \"Company field desc\" },"
                + "{\"name\": \"contact\" }"
                + "]}");

        loginPage.open();
        loginPage.login("login-test5", getPassword("login-test5"));

        verifyProfilePage.assertCurrent();

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

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", " + PERMISSIONS_ALL + ", \"required\":{}},"
                + "{\"name\": \"username\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"email\", " + PERMISSIONS_ALL + "}"
                + "]}");

        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setEditUsernameAllowed(true);
        testRealm().update(realm);

        loginPage.open();
        loginPage.login("login-test5", getPassword("login-test5"));

        verifyProfilePage.assertCurrent();

        //assert fields location in form
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

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"department\", " + PERMISSIONS_ALL + ", \"required\":{}},"
                + RegisterWithUserProfileTest.UP_CONFIG_PART_INPUT_TYPES
                + "]}");

        loginPage.open();
        loginPage.login("login-test5", getPassword("login-test5"));

        verifyProfilePage.assertCurrent();

        RegisterWithUserProfileTest.assertFieldTypes(driver);
    }

    @Test
    public void testEvents() {

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", " + PERMISSIONS_ALL + ", \"required\":{}}"
                + "]}");

        loginPage.open();
        loginPage.login("login-test5", getPassword("login-test5"));

        verifyProfilePage.assertCurrent();
        //event when form is shown
        events.expectRequiredAction(EventType.VERIFY_PROFILE).user(user5Id)
                .detail(Details.FIELDS_TO_UPDATE, "department")
                .assertEvent();

        verifyProfilePage.update("First", "Last", "Department");
        //event after profile is updated
        // we also test additional attribute configured to be audited in the event
        events.expectRequiredAction(EventType.UPDATE_PROFILE).user(user5Id)
                .detail(Details.CONTEXT, UserProfileContext.UPDATE_PROFILE.name())
                .detail(Details.PREVIOUS_FIRST_NAME, "ExistingFirst").detail(Details.UPDATED_FIRST_NAME, "First")
                .detail(Details.PREVIOUS_LAST_NAME, "ExistingLast").detail(Details.UPDATED_LAST_NAME, "Last")
                .detail(Details.PREF_UPDATED+"department", "Department")
                .assertEvent();
    }

    @Test
    public void testDefaultProfile() {
        setUserProfileConfiguration(null);

        testingClient.server(TEST_REALM_NAME).run(setEmptyFirstNameAndCustomAttribute());

        loginPage.open();
        loginPage.login("login-test", getPassword("login-test"));

        //submit with error
        verifyProfilePage.assertCurrent();
        Assert.assertFalse(verifyProfilePage.isDepartmentPresent());
        verifyProfilePage.update("First", " ");

        //submit OK
        verifyProfilePage.assertCurrent();
        Assert.assertFalse(verifyProfilePage.isDepartmentPresent());
        verifyProfilePage.update("First", "Last");


        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUser(userId);
        assertEquals("First", user.getFirstName());
        assertEquals("Last", user.getLastName());
    }

    @Test
    public void testIgnoreCustomAttributeWhenUserProfileIsDisabled() {
        testingClient.server(TEST_REALM_NAME).run(setEmptyFirstNameAndCustomAttribute());
        testDefaultProfile();
    }

    private static RunOnServer setEmptyFirstNameAndCustomAttribute() {
        return session -> {
            UserModel user = session.users().getUserByUsername(session.getContext().getRealm(), "login-test");

            // need to set directly to the model because user profile does not allow empty values
            // an empty value should fail validation and force rendering the verify profile page
            user.setFirstName("");
            // this attribute does not exist in the default user profile configuration
            user.setAttribute("test", List.of("test"));
        };
    }

    @Test
    public void testUsernameOnlyIfEditAllowed() {
        RealmRepresentation realm = testRealm().toRepresentation();

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, null, "ExistingLast", null);

        boolean r = realm.isEditUsernameAllowed();
        try {
            setUserProfileConfiguration(null);

            realm.setEditUsernameAllowed(false);
            testRealm().update(realm);

            loginPage.open();
            loginPage.login("login-test5", getPassword("login-test5"));

            verifyProfilePage.assertCurrent();
            assertFalse(verifyProfilePage.isUsernamePresent());
            assertTrue(verifyProfilePage.isEmailPresent());

            realm.setEditUsernameAllowed(true);
            testRealm().update(realm);

            driver.navigate().refresh();
            assertTrue(verifyProfilePage.isUsernamePresent());
        } finally {
            realm.setEditUsernameAllowed(r);
            testRealm().update(realm);
        }
    }

    @Test
    public void testUsernameOnlyIfEmailAsUsernameIsDisabled() {
        RealmRepresentation realm = testRealm().toRepresentation();

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, null, "ExistingLast", null);

        try {
            setUserProfileConfiguration(null);

            realm.setEditUsernameAllowed(true);
            realm.setRegistrationEmailAsUsername(true);
            testRealm().update(realm);

            loginPage.open();
            loginPage.login("login-test5", getPassword("login-test5"));

            verifyProfilePage.assertCurrent();
            assertFalse(verifyProfilePage.isUsernamePresent());
            assertTrue(verifyProfilePage.isEmailPresent());

            realm.setEditUsernameAllowed(false);
            realm.setRegistrationEmailAsUsername(true);
            testRealm().update(realm);

            driver.navigate().refresh();
            verifyProfilePage.assertCurrent();
            assertFalse(verifyProfilePage.isUsernamePresent());
            assertFalse(verifyProfilePage.isEmailPresent());

            realm.setEditUsernameAllowed(true);
            realm.setRegistrationEmailAsUsername(false);
            testRealm().update(realm);

            driver.navigate().refresh();
            verifyProfilePage.assertCurrent();
            assertTrue(verifyProfilePage.isUsernamePresent());
            assertTrue(verifyProfilePage.isEmailPresent());
        } finally {
            realm.setEditUsernameAllowed(false);
            realm.setRegistrationEmailAsUsername(false);
            testRealm().update(realm);
        }
    }

    @Test
    public void testUsernameOnlyIfEmailAsUsernameIsDisabledWithUpdateEmailFeature() throws Exception {
        reconnectAdminClient();
        RealmRepresentation realm = testRealm().toRepresentation();

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, null, "ExistingLast", null);

        ApiUtil.enableRequiredAction(testRealm(), RequiredAction.UPDATE_EMAIL, true);

        try {
            setUserProfileConfiguration(null);

            realm.setEditUsernameAllowed(true);
            realm.setRegistrationEmailAsUsername(true);
            testRealm().update(realm);

            loginPage.open();
            loginPage.login("login-test5", getPassword("login-test5"));

            verifyProfilePage.assertCurrent();
            assertFalse(verifyProfilePage.isUsernamePresent());
            assertFalse(verifyProfilePage.isEmailPresent());

            realm.setEditUsernameAllowed(false);
            realm.setRegistrationEmailAsUsername(true);
            testRealm().update(realm);

            driver.navigate().refresh();
            verifyProfilePage.assertCurrent();
            assertFalse(verifyProfilePage.isUsernamePresent());
            assertFalse(verifyProfilePage.isEmailPresent());

            realm.setEditUsernameAllowed(true);
            realm.setRegistrationEmailAsUsername(false);
            testRealm().update(realm);

            driver.navigate().refresh();
            verifyProfilePage.assertCurrent();
            assertTrue(verifyProfilePage.isUsernamePresent());
            assertFalse(verifyProfilePage.isEmailPresent());
        } finally {
            ApiUtil.enableRequiredAction(testRealm(), RequiredAction.UPDATE_EMAIL, false);
            realm.setEditUsernameAllowed(false);
            realm.setRegistrationEmailAsUsername(false);
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
        loginPage.login("login-test2", getPassword("login-test2"));

        verifyProfilePage.assertCurrent();
        verifyProfilePage.update("First", "");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUser(user2Id);
        assertEquals("First", user.getFirstName());
        assertThat(StringUtils.isEmpty(user.getLastName()), is(true));
    }

    @Test
    public void testCustomValidationLastName() {

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, "ExistingFirst", "La", "Department");

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL +","+VALIDATIONS_LENGTH + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ADMIN_ONLY + "}"
                + "]}");

        loginPage.open();
        loginPage.login("login-test5", getPassword("login-test5"));

        verifyProfilePage.assertCurrent();
        //submit with error
        verifyProfilePage.update("First", "L");

        verifyProfilePage.assertCurrent();
        //submit OK
        verifyProfilePage.update("First", "Last");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUser(user5Id);
        assertEquals("First", user.getFirstName());
        assertEquals("Last", user.getLastName());
        //check that not configured attribute is unchanged
        assertEquals("Department", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testNoActionIfNoValidationError() {

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, "ExistingFirst", "ExistingLast", "Department");

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL +","+VALIDATIONS_LENGTH + "}"
                + "]}");

        loginPage.open();
        loginPage.login("login-test5", getPassword("login-test5"));

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());
    }

    @Test
    public void testDoNotValidateUsernameWhenRegistrationAsEmailEnabled() {
        RealmResource realmResource = testRealm();
        RealmRepresentation realm = realmResource.toRepresentation();

        try {
            setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
            updateUser(user6Id, "ExistingFirst", "ExistingLast", "Department");

            realm.setRegistrationEmailAsUsername(true);

            realmResource.update(realm);

            setUserProfileConfiguration("{\"attributes\": ["
                    + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                    + "{\"name\": \"lastName\"," + PERMISSIONS_ALL +","+VALIDATIONS_LENGTH + "}"
                    + "]}");

            loginPage.open();
            loginPage.login("login6@test.com", getPassword("login-test6"));

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.parseLoginResponse().getCode());
        } finally {
            realm.setRegistrationEmailAsUsername(false);
            realmResource.update(realm);
        }
    }

    @Test
    public void testRequiredReadOnlyAttribute() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ADMIN_EDITABLE + ", \"required\":{}}"
                + "]}");

        loginPage.open();
        loginPage.login("login-test3", getPassword("login-test3"));

        verifyProfilePage.assertCurrent();
        Assert.assertEquals("ExistingLast", verifyProfilePage.getLastName());
        Assert.assertFalse(verifyProfilePage.isDepartmentEnabled());

        //update of the other attributes must be successful in this case
        verifyProfilePage.update("First", "Last");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUser(user3Id);
        assertEquals("First", user.getFirstName());
        assertEquals("Last", user.getLastName());
    }

    @Test
    public void testAdminOnlyAttributeNotVisibleToUser() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ADMIN_ONLY + "},"
                + "{\"name\": \"requiredAttrToTriggerVerifyPage\"," + PERMISSIONS_ALL + ", \"required\": {}}"
                + "]}");

        loginPage.open();
        loginPage.login("login-test6", getPassword("login-test6"));

        verifyProfilePage.assertCurrent();
        Assert.assertEquals("ExistingLast", verifyProfilePage.getLastName());
        Assert.assertFalse("Admin-only attribute should not be visible for user", verifyProfilePage.isDepartmentPresent());
    }


    @Test
    public void testUsernameReadOnlyInProfile() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"username\"," + PERMISSIONS_ADMIN_EDITABLE + "},"
                + "{\"name\": \"requiredAttrToTriggerVerifyPage\"," + PERMISSIONS_ALL + ", \"required\": {}}"
                + "]}");

        loginPage.open();
        loginPage.login("login-test6", getPassword("login-test6"));

        verifyProfilePage.assertCurrent();
        Assert.assertEquals("ExistingLast", verifyProfilePage.getLastName());

        Assert.assertFalse("username should not be editable by user", verifyProfilePage.isUsernameEnabled());
    }

    @Test
    public void testUsernameReadNotVisibleInProfile() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"username\"," + PERMISSIONS_ADMIN_ONLY + "},"
                + "{\"name\": \"requiredAttrToTriggerVerifyPage\"," + PERMISSIONS_ALL + ", \"required\": {}}"
                + "]}");

        loginPage.open();
        loginPage.login("login-test6", getPassword("login-test6"));

        verifyProfilePage.assertCurrent();
        Assert.assertEquals("ExistingLast", verifyProfilePage.getLastName());

        Assert.assertFalse("username should not be shown to user", verifyProfilePage.isUsernamePresent());
    }

    @Test
    public void testEMailRequiredInProfileWithLocalPartLength() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"username\"," + PERMISSIONS_ADMIN_ONLY + "},"
                + "{\"name\": \"email\"," + PERMISSIONS_ALL + ", \"required\":{\"roles\":[\"user\"]}, \"validations\": {\"email\": {\"max-local-length\": \"16\"}}}"
                + "]}");

        loginPage.open();
        loginPage.login("login-nomail", getPassword("login-nomail"));

        // no email is set => expect verify profile page to be displayed
        verifyProfilePage.assertCurrent();

        // set e-mail with legth 17 => error
        verifyProfilePage.updateEmail("abcdefg0123456789@bar.com", "HasNowMailFirst", "HasNowMailLast");
        verifyProfilePage.assertCurrent();

        // set e-mail, update firstname/lastname and complete login
        verifyProfilePage.updateEmail("abcdef0123456789@bar.com", "HasNowMailFirst", "HasNowMailLast");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUser(userWithoutEmailId);
        assertEquals("HasNowMailFirst", user.getFirstName());
        assertEquals("HasNowMailLast", user.getLastName());
        assertEquals("abcdef0123456789@bar.com", user.getEmail());
    }

    @Test
    public void testAttributeNotVisible() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ADMIN_ONLY + ", \"required\":{}}"
                + "]}");

        loginPage.open();
        loginPage.login("login-test4", getPassword("login-test4"));

        verifyProfilePage.assertCurrent();
        Assert.assertEquals("ExistingLast", verifyProfilePage.getLastName());
        Assert.assertFalse("'department' field is visible" , verifyProfilePage.isDepartmentPresent());

        //update of the other attributes must be successful in this case
        verifyProfilePage.update("First", "Last");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUser(user4Id);
        assertEquals("First", user.getFirstName());
        assertEquals("Last", user.getLastName());
    }

    @Test
    public void testRequiredAttribute() {

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}}"
                + "]}");

        loginPage.open();
        loginPage.login("login-test5", getPassword("login-test5"));

        verifyProfilePage.assertCurrent();

        //submit with error
        verifyProfilePage.update("FirstCC", "LastCC", " ");
        verifyProfilePage.assertCurrent();

        //submit OK
        verifyProfilePage.update("FirstCC", "LastCC", "DepartmentCC");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUser(user5Id);
        assertEquals("FirstCC", user.getFirstName());
        assertEquals("LastCC", user.getLastName());
        assertEquals("DepartmentCC", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testRequiredOnlyIfUser() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{\"roles\":[\"user\"]}}"
                + "]}");

        updateUser(user5Id, "ExistingFirst", "ExistingLast", null);


        loginPage.open();
        loginPage.login("login-test5", getPassword("login-test5"));

        verifyProfilePage.assertCurrent();

        //submit with error
        verifyProfilePage.update("FirstCC", "LastCC", " ");
        verifyProfilePage.assertCurrent();

        //submit OK
        verifyProfilePage.update("FirstCC", "LastCC", "DepartmentCC");


        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUser(user5Id);
        assertEquals("FirstCC", user.getFirstName());
        assertEquals("LastCC", user.getLastName());
        assertEquals("DepartmentCC", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testAttributeNotRequiredWhenMissingScope() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{\"scopes\":[\"profile\"]}}"
                + "]}");

        updateUser(user5Id, "ExistingFirst", "ExistingLast", null);

        oauth.clientId(client_scope_optional.getClientId()).openLoginForm();

        loginPage.login("login-test5", getPassword("login-test5"));

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUser(user5Id);
        assertEquals("ExistingFirst", user.getFirstName());
        assertEquals("ExistingLast", user.getLastName());
    }

    @Test
    public void testAttributeRequiredForScope() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{\"scopes\":[\""+SCOPE_DEPARTMENT+"\"]}}"
                + "]}");

        updateUser(user5Id, "ExistingFirst", "ExistingLast", null);

        oauth.scope(SCOPE_DEPARTMENT).clientId(client_scope_optional.getClientId()).openLoginForm();

        loginPage.assertCurrent();
        loginPage.login("login-test5", getPassword("login-test5"));

        verifyProfilePage.assertCurrent();

        verifyProfilePage.update("FirstAA", "LastAA", "DepartmentAA");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUser(user5Id);
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertEquals("DepartmentAA", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testAttributeRequiredForDefaultScope() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{\"scopes\":[\""+SCOPE_DEPARTMENT+"\"]}}"
                + "]}");

        updateUser(user5Id, "ExistingFirst", "ExistingLast", null);

        oauth.clientId(client_scope_default.getClientId()).openLoginForm();

        loginPage.assertCurrent();
        loginPage.login("login-test5", getPassword("login-test5"));

        verifyProfilePage.assertCurrent();

        //submit with error
        verifyProfilePage.update("FirstBB", "LastBB", " ");
        verifyProfilePage.assertCurrent();

        //submit OK
        verifyProfilePage.update("FirstBB", "LastBB", "DepartmentBB");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUser(user5Id);
        assertEquals("FirstBB", user.getFirstName());
        assertEquals("LastBB", user.getLastName());
        assertEquals("DepartmentBB", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testNoActionIfValidForScope() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{\"scopes\":[\""+SCOPE_DEPARTMENT+"\"]}}"
                + "]}");

        updateUser(user5Id, "ExistingFirst", "ExistingLast", "ExistingDepartment");

        oauth.clientId(client_scope_default.getClientId()).openLoginForm();

        loginPage.assertCurrent();
        loginPage.login("login-test5", getPassword("login-test5"));

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUser(user5Id);
        assertEquals("ExistingFirst", user.getFirstName());
        assertEquals("ExistingLast", user.getLastName());
        assertEquals("ExistingDepartment", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testAttributeRequiredButNotSelectedByScopeDoesntForceVerificationScreen() {

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\""+SCOPE_DEPARTMENT+"\"]}}"
                + "]}");

        oauth.clientId(client_scope_optional.getClientId()).openLoginForm();

        loginPage.assertCurrent();
        loginPage.login("login-test5", getPassword("login-test5"));

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());
    }

    @Test
    public void testAttributeRequiredAndSelectedByScope() {

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\""+SCOPE_DEPARTMENT+"\"]}}"
                + "]}");

        oauth.scope(SCOPE_DEPARTMENT).clientId(client_scope_optional.getClientId()).openLoginForm();

        loginPage.assertCurrent();
        loginPage.login("login-test5", getPassword("login-test5"));

        verifyProfilePage.assertCurrent();

        verifyProfilePage.update("FirstAA", "LastAA", "DepartmentAA");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUser(user5Id);
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertEquals("DepartmentAA", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testAttributeNotRequiredAndSelectedByScopeCanBeUpdatedFromVerificationScreenForcedByAnotherAttribute() {

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, "ExistingFirst", null, null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"selector\":{\"scopes\":[\""+SCOPE_DEPARTMENT+"\"]}}"
                + "]}");

        oauth.scope(SCOPE_DEPARTMENT).clientId(client_scope_optional.getClientId()).openLoginForm();

        loginPage.assertCurrent();
        loginPage.login("login-test5", getPassword("login-test5"));

        verifyProfilePage.assertCurrent();

        Assert.assertTrue(verifyProfilePage.isDepartmentPresent());
        verifyProfilePage.update("FirstAA", "LastAA", "Department AA");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUser(user5Id);
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertEquals("Department AA", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testAttributeRequiredButNotSelectedByScopeIsNotRenderedOnVerificationScreenForcedByAnotherAttribute() {

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, "ExistingFirst", null, null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\""+SCOPE_DEPARTMENT+"\"]}}"
                + "]}");

        oauth.clientId(client_scope_optional.getClientId()).openLoginForm();

        loginPage.assertCurrent();
        loginPage.login("login-test5", getPassword("login-test5"));

        verifyProfilePage.assertCurrent();

        Assert.assertFalse(verifyProfilePage.isDepartmentPresent());
        verifyProfilePage.update("FirstAA", "LastAA");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUser(user5Id);
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertEquals(null, user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testCustomValidationInCustomAttribute() {

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, "ExistingFirst", "ExistingLast", "D");

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", "+VALIDATIONS_LENGTH+"}"
                + "]}");

        loginPage.open();
        loginPage.login("login-test5", getPassword("login-test5"));

        verifyProfilePage.assertCurrent();

        //submit with error
        verifyProfilePage.update("FirstCC", "LastCC", "De");
        verifyProfilePage.assertCurrent();

        //submit OK
        verifyProfilePage.update("FirstCC", "LastCC", "DepartmentCC");


        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUser(user5Id);
        assertEquals("FirstCC", user.getFirstName());
        assertEquals("LastCC", user.getLastName());
        assertEquals("DepartmentCC", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testEmailChangeSetsEmailVerified() {
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, true, "", "ExistingLast");

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "}"
                + "]}");

        loginPage.open();
        loginPage.login("login-test5", getPassword("login-test5"));

        verifyProfilePage.assertCurrent();

        //submit OK
        verifyProfilePage.updateEmail("newemail@test.org","FirstCC", "LastCC");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());

        UserRepresentation user = getUser(user5Id);
        assertEquals("newemail@test.org", user.getEmail());
        assertEquals(false, user.isEmailVerified());
    }

    @Test
    public void testNoActionIfSuccessfulValidationForCustomAttribute() {

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, "ExistingFirst", "ExistingLast", "Department");

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", "+VALIDATIONS_LENGTH+"}"
                + "]}");

        loginPage.open();
        loginPage.login("login-test5", getPassword("login-test5"));

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());
    }

    @Test
    public void testConfigurationPersisted() throws IOException {
        String customConfig = "{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", " + VALIDATIONS_LENGTH + "}"
                + "]}";

        UPConfig persistedConfig = setUserProfileConfiguration(customConfig);

        JsonTestUtils.assertJsonEquals(JsonSerialization.writeValueAsString(persistedConfig), testRealm().users().userProfile().getConfiguration());
    }

    protected UserRepresentation getUser(String userId) {
        return getUser(testRealm(), userId);
    }

    protected void updateUser(String userId, String firstName, String lastName, String department) {
        updateUser(testRealm(), userId, firstName, lastName, department);
    }

    protected void updateUser(String userId, boolean emailVerified, String firstName, String lastName) {
        UserRepresentation ur = getUser(testRealm(), userId);
        ur.setFirstName(firstName);
        ur.setLastName(lastName);
        ur.setEmailVerified(emailVerified);
        testRealm().users().get(userId).update(ur);
    }

    protected UPConfig setUserProfileConfiguration(String configuration) {
        assertAdminEvents.clear();
        UPConfig result = UserProfileUtil.setUserProfileConfiguration(testRealm(), configuration);
        AdminEventRepresentation adminEvent = assertAdminEvents.assertEvent(TEST_REALM_NAME,
                OperationType.UPDATE, AdminEventPaths.userProfilePath(), ResourceType.USER_PROFILE);
        Assert.assertTrue("Incorrect representation in event", StringUtils.isBlank(configuration)
                ? StringUtils.isBlank(adminEvent.getRepresentation())
                : StringUtils.isNotBlank(adminEvent.getRepresentation()));
        return result;
    }

    public static UserRepresentation getUser(RealmResource testRealm, String userId) {
        return testRealm.users().get(userId).toRepresentation();
    }

    public static UserRepresentation getUserByUsername(RealmResource testRealm, String username) {
        List<UserRepresentation> users = testRealm.users().search(username);
        if(users!=null && !users.isEmpty())
            return users.get(0);
        return null;
    }

    public static void updateUser(RealmResource testRealm, String userId, String firstName, String lastName, String department) {
        UserRepresentation ur = getUser(testRealm, userId);
        ur.setFirstName(firstName);
        ur.setLastName(lastName);
        ur.singleAttribute(ATTRIBUTE_DEPARTMENT, department);
        testRealm.users().get(userId).update(ur);
    }

}
