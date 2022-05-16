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
import static org.keycloak.userprofile.DeclarativeUserProfileProvider.REALM_USER_PROFILE_ENABLED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.VerifyProfilePage;
import org.keycloak.testsuite.util.ClientScopeBuilder;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.userprofile.UserProfileContext;
import org.openqa.selenium.By;

/**
 * @author Vlastimil Elias <velias@redhat.com>
 */
@EnableFeature(value = Profile.Feature.DECLARATIVE_USER_PROFILE)
@AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE)
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

    private static ClientRepresentation client_scope_default;
    private static ClientRepresentation client_scope_optional;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

        enableDynamicUserProfile(testRealm);

        UserRepresentation user = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-test").email("login@test.com").enabled(true).password("password").build();
        userId = user.getId();

        UserRepresentation user2 = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-test2").email("login2@test.com").enabled(true).password("password").build();
        user2Id = user2.getId();

        UserRepresentation user3 = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-test3").email("login3@test.com").enabled(true).password("password").lastName("ExistingLast").build();
        user3Id = user3.getId();

        UserRepresentation user4 = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-test4").email("login4@test.com").enabled(true).password("password").lastName("ExistingLast").build();
        user4Id = user4.getId();

        UserRepresentation user5 = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-test5").email("login5@test.com").enabled(true).password("password").firstName("ExistingFirst").lastName("ExistingLast").build();
        user5Id = user5.getId();

        UserRepresentation user6 = UserBuilder.create().id(UUID.randomUUID().toString()).username("login-test6").email("login6@test.com").enabled(true).password("password").firstName("ExistingFirst").lastName("ExistingLast").build();
        user6Id = user6.getId();

        RealmBuilder.edit(testRealm).user(user).user(user2).user(user3).user(user4).user(user5).user(user6);

        RequiredActionProviderRepresentation action = new RequiredActionProviderRepresentation();
        action.setAlias(UserModel.RequiredAction.VERIFY_PROFILE.name());
        action.setProviderId(UserModel.RequiredAction.VERIFY_PROFILE.name());
        action.setEnabled(true);
        action.setDefaultAction(false);
        action.setPriority(10);

        List<RequiredActionProviderRepresentation> actions = new ArrayList<>();
        actions.add(action);
        testRealm.setRequiredActions(actions);

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

    @Rule
    public AssertEvents events = new AssertEvents(this);

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

        //assert fields and groups location in form
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
        events.expectRequiredAction(EventType.VERIFY_PROFILE).user(user5Id).detail("fields_to_update", "department").assertEvent();

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
    public void testUsernameOnlyIfEditAllowed() {
        RealmRepresentation realm = testRealm().toRepresentation();

        boolean r = realm.isEditUsernameAllowed();
        try {
            setUserProfileConfiguration(null);

            realm.setEditUsernameAllowed(false);
            testRealm().update(realm);

            loginPage.open();
            loginPage.login("login-test", "password");

            assertFalse(verifyProfilePage.isUsernamePresent());

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

    protected void setUserProfileConfiguration(String configuration) {
        setUserProfileConfiguration(testRealm(), configuration);
    }

    public static void enableDynamicUserProfile(RealmRepresentation testRealm) {
        if (testRealm.getAttributes() == null) {
            testRealm.setAttributes(new HashMap<>());
        }
        testRealm.getAttributes().put(REALM_USER_PROFILE_ENABLED, Boolean.TRUE.toString());
    }

    public static void disableDynamicUserProfile(RealmResource realm) {
        RealmRepresentation realmRep = realm.toRepresentation();
        if (realmRep.getAttributes() == null) {
            realmRep.setAttributes(new HashMap<>());
        }
        realmRep.getAttributes().put(REALM_USER_PROFILE_ENABLED, Boolean.FALSE.toString());
        realm.update(realmRep);
    }


    public static void setUserProfileConfiguration(RealmResource testRealm, String configuration) {
        try (Response r = testRealm.users().userProfile().update(configuration)) {
            if (r.getStatus() != 200) {
                Assert.fail("UserProfile Configuration not set due to error: " + r.readEntity(String.class));
            }
        }
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
