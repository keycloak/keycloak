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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_ADMIN;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_USER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPAttributeRequired;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
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
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.By;

/**
 * @author Vlastimil Elias <velias@redhat.com>
 */
public class VerifyProfileTest extends AbstractTestRealmKeycloakTest {

    public static final String SCOPE_DEPARTMENT = "department";
    public static final String ATTRIBUTE_DEPARTMENT = "department";

    public static final String PERMISSIONS_ALL = "\"permissions\": {\"view\": [\"admin\", \"user\"], \"edit\": [\"admin\", \"user\"]}";
    public static final String PERMISSIONS_ADMIN_ONLY = "\"permissions\": {\"view\": [\"admin\"], \"edit\": [\"admin\"]}";
    public static final String PERMISSIONS_ADMIN_EDITABLE = "\"permissions\": {\"view\": [\"admin\", \"user\"], \"edit\": [\"admin\"]}";

    public static String VALIDATIONS_LENGTH = "\"validations\": {\"length\": { \"min\": 3, \"max\": 255 }}";

    public static final String CONFIGURATION_FOR_USER_EDIT = "{\"attributes\": ["
            + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + "},"
            + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
            + "{\"name\": \"department\"," + PERMISSIONS_ALL + "}"
            + "]}";


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
        UserRepresentation user = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-test").email("login@test.com").enabled(true).password("password").build();
        UserRepresentation user2 = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-test2").email("login2@test.com").enabled(true).password("password").build();
        UserRepresentation user3 = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-test3").email("login3@test.com").enabled(true).password("password").lastName("ExistingLast").build();
        UserRepresentation user4 = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-test4").email("login4@test.com").enabled(true).password("password").lastName("ExistingLast").build();
        UserRepresentation user5 = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-test5").email("login5@test.com").enabled(true).password("password").firstName("ExistingFirst").lastName("ExistingLast").build();
        UserRepresentation user6 = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-test6").email("login6@test.com").enabled(true).password("password").firstName("ExistingFirst").lastName("ExistingLast").build();
        UserRepresentation userWithoutEmail = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-nomail").enabled(true).password("password").firstName("NoMailFirst").lastName("NoMailLast").build();

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
        loginPage.login("login-test5", "password");

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
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + "},"
                + "{\"name\": \"username\", " + VerifyProfileTest.PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\":{}, \"group\": \"company\"},"
                + "{\"name\": \"email\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"group\": \"contact\"}"
                + "], \"groups\": ["
                + "{\"name\": \"company\", \"displayDescription\": \"Company field desc\" },"
                + "{\"name\": \"contact\" }"
                + "]}");

        loginPage.open();
        loginPage.login("login-test5", "password");

        verifyProfilePage.assertCurrent();
        String htmlFormId="kc-update-profile-form";

        //assert fields and groups location in form, attributes without a group appear first
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(1) > div:nth-child(2) > input#lastName")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(2) > div:nth-child(2) > input#username")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(3) > div:nth-child(2) > input#firstName")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(4) > div:nth-child(1) > label#header-company")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(4) > div:nth-child(2) > label#description-company")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(5) > div:nth-child(2) > input#department")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(6) > div:nth-child(1) > label#header-contact")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(7) > div:nth-child(2) > input#email")
                ).isDisplayed()
        );
    }

    @Test
    public void testAttributeGuiOrder() {

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\":{}},"
                + "{\"name\": \"username\", " + VerifyProfileTest.PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"email\", " + VerifyProfileTest.PERMISSIONS_ALL + "}"
                + "]}");

        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setEditUsernameAllowed(true);
        testRealm().update(realm);

        loginPage.open();
        loginPage.login("login-test5", "password");

        verifyProfilePage.assertCurrent();

        //assert fields location in form
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#kc-update-profile-form > div:nth-child(1) > div:nth-child(2) > input#lastName")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#kc-update-profile-form > div:nth-child(2) > div:nth-child(2) > input#department")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#kc-update-profile-form > div:nth-child(3) > div:nth-child(2) > input#username")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#kc-update-profile-form > div:nth-child(4) > div:nth-child(2) > input#firstName")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#kc-update-profile-form > div:nth-child(5) > div:nth-child(2) > input#email")
                ).isDisplayed()
        );
    }

    @Test
    public void testAttributeInputTypes() {

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"department\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\":{}},"
                + RegisterWithUserProfileTest.UP_CONFIG_PART_INPUT_TYPES
                + "]}");

        loginPage.open();
        loginPage.login("login-test5", "password");

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
        loginPage.login("login-test5", "password");

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
        loginPage.login("login-test", "password");

        //submit with error
        verifyProfilePage.assertCurrent();
        Assert.assertFalse(verifyProfilePage.isDepartmentPresent());
        verifyProfilePage.update("First", " ");

        //submit OK
        verifyProfilePage.assertCurrent();
        Assert.assertFalse(verifyProfilePage.isDepartmentPresent());
        verifyProfilePage.update("First", "Last");


        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
            loginPage.login("login-test5", "password");

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
            loginPage.login("login-test5", "password");

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
    @EnableFeature(Profile.Feature.UPDATE_EMAIL)
    public void testUsernameOnlyIfEmailAsUsernameIsDisabledWithUpdateEmailFeature() throws Exception {
        reconnectAdminClient();
        RealmRepresentation realm = testRealm().toRepresentation();

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser(user5Id, null, "ExistingLast", null);

        try {
            setUserProfileConfiguration(null);

            realm.setEditUsernameAllowed(true);
            realm.setRegistrationEmailAsUsername(true);
            testRealm().update(realm);

            loginPage.open();
            loginPage.login("login-test5", "password");

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
        loginPage.login("login-test2", "password");

        verifyProfilePage.assertCurrent();
        verifyProfilePage.update("First", "");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        loginPage.login("login-test5", "password");

        verifyProfilePage.assertCurrent();
        //submit with error
        verifyProfilePage.update("First", "L");

        verifyProfilePage.assertCurrent();
        //submit OK
        verifyProfilePage.update("First", "Last");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        loginPage.login("login-test5", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
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
            loginPage.login("login6@test.com", "password");

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
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
        loginPage.login("login-test3", "password");

        verifyProfilePage.assertCurrent();
        Assert.assertEquals("ExistingLast", verifyProfilePage.getLastName());
        Assert.assertFalse(verifyProfilePage.isDepartmentEnabled());

        //update of the other attributes must be successful in this case
        verifyProfilePage.update("First", "Last");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        loginPage.login("login-test6", "password");

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
        loginPage.login("login-test6", "password");

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
        loginPage.login("login-test6", "password");

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
        loginPage.login("login-nomail", "password");

        // no email is set => expect verify profile page to be displayed
        verifyProfilePage.assertCurrent();

        // set e-mail with legth 17 => error
        verifyProfilePage.updateEmail("abcdefg0123456789@bar.com", "HasNowMailFirst", "HasNowMailLast");
        verifyProfilePage.assertCurrent();

        // set e-mail, update firstname/lastname and complete login
        verifyProfilePage.updateEmail("abcdef0123456789@bar.com", "HasNowMailFirst", "HasNowMailLast");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        loginPage.login("login-test4", "password");

        verifyProfilePage.assertCurrent();
        Assert.assertEquals("ExistingLast", verifyProfilePage.getLastName());
        Assert.assertFalse("'department' field is visible" , verifyProfilePage.isDepartmentPresent());

        //update of the other attributes must be successful in this case
        verifyProfilePage.update("First", "Last");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        loginPage.login("login-test5", "password");

        verifyProfilePage.assertCurrent();

        //submit with error
        verifyProfilePage.update("FirstCC", "LastCC", " ");
        verifyProfilePage.assertCurrent();

        //submit OK
        verifyProfilePage.update("FirstCC", "LastCC", "DepartmentCC");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        loginPage.login("login-test5", "password");

        verifyProfilePage.assertCurrent();

        //submit with error
        verifyProfilePage.update("FirstCC", "LastCC", " ");
        verifyProfilePage.assertCurrent();

        //submit OK
        verifyProfilePage.update("FirstCC", "LastCC", "DepartmentCC");


        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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

        loginPage.login("login-test5", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        loginPage.login("login-test5", "password");

        verifyProfilePage.assertCurrent();

        verifyProfilePage.update("FirstAA", "LastAA", "DepartmentAA");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        loginPage.login("login-test5", "password");

        verifyProfilePage.assertCurrent();

        //submit with error
        verifyProfilePage.update("FirstBB", "LastBB", " ");
        verifyProfilePage.assertCurrent();

        //submit OK
        verifyProfilePage.update("FirstBB", "LastBB", "DepartmentBB");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        loginPage.login("login-test5", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        loginPage.login("login-test5", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
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
        loginPage.login("login-test5", "password");

        verifyProfilePage.assertCurrent();

        verifyProfilePage.update("FirstAA", "LastAA", "DepartmentAA");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        loginPage.login("login-test5", "password");

        verifyProfilePage.assertCurrent();

        Assert.assertTrue(verifyProfilePage.isDepartmentPresent());
        verifyProfilePage.update("FirstAA", "LastAA", "Department AA");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        loginPage.login("login-test5", "password");

        verifyProfilePage.assertCurrent();

        Assert.assertFalse(verifyProfilePage.isDepartmentPresent());
        verifyProfilePage.update("FirstAA", "LastAA");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        loginPage.login("login-test5", "password");

        verifyProfilePage.assertCurrent();

        //submit with error
        verifyProfilePage.update("FirstCC", "LastCC", "De");
        verifyProfilePage.assertCurrent();

        //submit OK
        verifyProfilePage.update("FirstCC", "LastCC", "DepartmentCC");


        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        loginPage.login("login-test5", "password");

        verifyProfilePage.assertCurrent();

        //submit OK
        verifyProfilePage.updateEmail("newemail@test.org","FirstCC", "LastCC");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        loginPage.login("login-test5", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
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
        UPConfig result = setUserProfileConfiguration(testRealm(), configuration);
        AdminEventRepresentation adminEvent = assertAdminEvents.assertEvent(TEST_REALM_NAME,
                OperationType.UPDATE, AdminEventPaths.userProfilePath(), ResourceType.USER_PROFILE);
        Assert.assertTrue("Incorrect representation in event", StringUtils.isBlank(configuration)
                ? StringUtils.isBlank(adminEvent.getRepresentation())
                : StringUtils.isNotBlank(adminEvent.getRepresentation()));
        return result;
    }

    public static UPConfig setUserProfileConfiguration(RealmResource testRealm, String configuration) {
        try {
            UPConfig config = configuration == null ? null : JsonSerialization.readValue(configuration, UPConfig.class);

            if (config != null) {
                UPAttribute username = config.getAttribute(UserModel.USERNAME);

                if (username == null) {
                    config.addOrReplaceAttribute(new UPAttribute(UserModel.USERNAME));
                }

                UPAttribute email = config.getAttribute(UserModel.EMAIL);

                if (email == null) {
                    config.addOrReplaceAttribute(new UPAttribute(UserModel.EMAIL, new UPAttributePermissions(Set.of(ROLE_USER, ROLE_ADMIN), Set.of(ROLE_USER, ROLE_ADMIN)), new UPAttributeRequired(Set.of(ROLE_USER), Set.of())));
                }
            }

            testRealm.users().userProfile().update(config);

            return config;
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to read configuration", ioe);
        }
    }

    public static UPConfig enableUnmanagedAttributes(UserProfileResource upResource) {
        UPConfig cfg = upResource.getConfiguration();
        cfg.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        upResource.update(cfg);
        return cfg;
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
