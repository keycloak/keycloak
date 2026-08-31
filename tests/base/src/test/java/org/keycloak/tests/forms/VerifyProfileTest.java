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
package org.keycloak.tests.forms;

import java.io.IOException;
import java.util.List;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientScopeBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginUpdateProfilePage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.tests.utils.JsonTestUtils;
import org.keycloak.tests.utils.PasswordGenerateUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
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
 * @author Vlastimil Elias <velias@redhat.com>
 */
@KeycloakIntegrationTest
@DatabaseTest
public class VerifyProfileTest {

    private static final String PASSWORD = PasswordGenerateUtil.generatePassword();

    // Fragment shared with the (still legacy) RegisterWithUserProfileTest#UP_CONFIG_PART_INPUT_TYPES; keep in sync
    // until that test is migrated and the two can share a common util.
    private static final String UP_CONFIG_PART_INPUT_TYPES = "{\"name\": \"defaultType\"," + PERMISSIONS_ALL + "},"
            + "{\"name\": \"placeholderAttribute\", " + PERMISSIONS_ALL + ", \"annotations\":{\"inputType\":\"text\",\"inputTypePlaceholder\":\"Example.\"}},"
            + "{\"name\": \"helperTexts\", " + PERMISSIONS_ALL + ", \"annotations\":{\"inputType\":\"text\",\"inputHelperTextBefore\":\"Example <b>bold text</b> before.\",\"inputHelperTextAfter\":\"Example <i>i text</i> after.\"}},"
            + "{\"name\": \"textWithBasicAttributes\", " + PERMISSIONS_ALL + ", \"annotations\":{\"inputType\":\"text\",\"inputTypeSize\":\"35\",\"inputTypeMinlength\":\"1\",\"inputTypeMaxlength\":\"10\",\"inputTypePattern\":\".*\"}},"
            + "{\"name\": \"html5NumberWithAttributes\", " + PERMISSIONS_ALL + ", \"annotations\":{\"inputType\":\"html5-number\",\"inputTypeMin\":\"10\",\"inputTypeMax\":\"20\",\"inputTypeStep\":1}},"
            + "{\"name\": \"textareaWithAttributes\", " + PERMISSIONS_ALL + ", \"annotations\":{\"inputType\":\"textarea\",\"inputTypeCols\":\"35\",\"inputTypeRows\":\"7\",\"inputTypeMaxlength\":\"10\"}},"
            + "{\"name\": \"selectWithoutOptions\", " + PERMISSIONS_ALL + ", \"annotations\":{\"inputType\":\"select\",\"inputTypeSize\":\"5\"}},"
            + "{\"name\": \"selectWithOptionsWithoutLabels\", " + PERMISSIONS_ALL + ", \"validations\":{\"options\":{\"options\":[ \"opt1\",\"opt2\"]}}, \"annotations\":{\"inputType\":\"select\"}},"
            + "{\"name\": \"multiselectWithOptionsAndSimpleI18nLabels\", " + PERMISSIONS_ALL + ", \"validations\":{\"options\":{ \"options\":[\"totp\",\"opt2\"]}}, \"annotations\":{\"inputType\":\"multiselect\",\"inputOptionLabelsI18nPrefix\": \"loginTotp\"}},"
            + "{\"name\": \"multiselectWithOptionsAndLabels\", " + PERMISSIONS_ALL + ", \"validations\":{\"options\":{ \"options\":[\"opt1\",\"opt2\",\"opt3\"]}}, \"annotations\":{\"inputType\":\"multiselect\",\"inputOptionLabels\":{\"opt1\": \"Option 1\",\"opt2\":\"${username}\"}}},"
            + "{\"name\": \"selectWithOptionsFromCustomValidatorAndLabels\", " + PERMISSIONS_ALL + ", \"validations\":{\"dummyOptions\":{\"options\" : [\"vopt1\",\"vopt2\",\"vopt3\"]}} ,\"annotations\":{\"inputType\":\"select\",\"inputOptionsFromValidation\":\"dummyOptions\",\"inputOptionLabels\":{\"vopt1\": \"Option 1\",\"vopt2\":\"${username}\"}}},"
            + "{\"name\": \"selectRadiobuttons\", " + PERMISSIONS_ALL + ", \"validations\" : {\"options\" : {\"options\":[\"opt1\",\"opt2\",\"opt3\"]}}, \"annotations\":{\"inputType\":\"select-radiobuttons\",\"inputOptionLabels\":{\"opt1\": \"Option 1\",\"opt2\":\"${username}\"}}},"
            + "{\"name\": \"selectRadiobuttonsWithOptionsFromCustomValidatorAndLabels\", " + PERMISSIONS_ALL + ", \"validations\" : {\"dummyOptions\" : {\"options\" : [\"vopt1\",\"vopt2\",\"vopt3\"]}} ,\"annotations\":{\"inputType\":\"select-radiobuttons\",\"inputOptionsFromValidation\":\"dummyOptions\",\"inputOptionLabels\":{\"vopt1\": \"Option 1\",\"vopt2\":\"${username}\"}}},"
            + "{\"name\": \"multiselectCheckboxes\", " + PERMISSIONS_ALL + ", \"validations\": {\"options\":{\"options\":[\"opt1\",\"opt2\",\"opt3\"]}}, \"annotations\":{\"inputType\":\"multiselect-checkboxes\",\"inputOptionLabels\":{\"opt1\": \"Option 1\",\"opt2\":\"${username}\"}}}";

    @InjectRealm(config = VerifyProfileRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginUpdateProfilePage verifyProfilePage;

    private boolean userProfileResetRegistered;

    @Test
    public void testDisplayName() {
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser("login-test5", "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\",\"displayName\":\"${firstName}\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", \"displayName\" : \"Department\", " + PERMISSIONS_ALL + ", \"required\":{}}"
                + "]}");

        oauth.openLoginForm();
        login("login-test5");

        verifyProfilePage.assertCurrent();

        //assert field names
        // i18n replaced
        assertEquals("First name", verifyProfilePage.getLabelForField("firstName"));
        // attribute name used if no display name set
        assertEquals("lastName", verifyProfilePage.getLabelForField("lastName"));
        // direct value in display name
        assertEquals("Department", verifyProfilePage.getLabelForField("department"));
    }

    @Test
    public void testAttributeGrouping() {
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser("login-test5", "ExistingFirst", "ExistingLast", null);

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

        oauth.openLoginForm();
        login("login-test5");

        verifyProfilePage.assertCurrent();

        //assert fields and groups location in form, attributes without a group appear first
        WebDriver webDriver = driver.driver();
        List<WebElement> element = webDriver.findElements(By.cssSelector("form#kc-update-profile-form label"));
        String[] labelOrder = new String[]{"lastName", "username", "firstName", "header-company", "description-company", "department", "header-contact", "email"};
        for (int i = 0; i < element.size(); i++) {
            WebElement webElement = element.get(i);
            String id;
            if (webElement.getAttribute("for") != null) {
                id = webElement.getAttribute("for");
                // see that the label has an element it belongs to
                assertThat("Label with id: " + id + " should have component it belongs to", webDriver.findElement(By.id(id)).isDisplayed(), is(true));
            } else {
                id = webElement.getAttribute("id");
            }
            assertThat("Label at index: " + i + " with id: " + id + " was not in found in the same order in the dom", id, is(labelOrder[i]));
        }
    }

    @Test
    public void testAttributeGuiOrder() {
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser("login-test5", "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", " + PERMISSIONS_ALL + ", \"required\":{}},"
                + "{\"name\": \"username\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"email\", " + PERMISSIONS_ALL + "}"
                + "]}");

        managedRealm.updateWithCleanup(r -> r.editUsernameAllowed(true));

        oauth.openLoginForm();
        login("login-test5");

        verifyProfilePage.assertCurrent();

        //assert fields location in form
        List<WebElement> element = driver.driver().findElements(By.cssSelector("form#kc-update-profile-form input"));
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
        updateUser("login-test5", "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"department\", " + PERMISSIONS_ALL + ", \"required\":{}},"
                + UP_CONFIG_PART_INPUT_TYPES
                + "]}");

        oauth.openLoginForm();
        login("login-test5");

        verifyProfilePage.assertCurrent();

        assertFieldTypes(driver.driver());
    }

    @Test
    public void testEvents() {
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser("login-test5", "ExistingFirst", "ExistingLast", null);
        String user5Id = getUser("login-test5").getId();

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", " + PERMISSIONS_ALL + ", \"required\":{}}"
                + "]}");

        oauth.openLoginForm();
        login("login-test5");

        verifyProfilePage.assertCurrent();
        //event when form is shown
        EventAssertion.expectRequiredAction(events.poll()).type(EventType.VERIFY_PROFILE).userId(user5Id)
                .details(Details.FIELDS_TO_UPDATE, "department");

        verifyProfilePage.prepareUpdate().firstName("First").lastName("Last").department("Department").submit();
        managedRealm.cleanup().add(RealmResource::logoutAll);
        //event after profile is updated
        // we also test additional attribute configured to be audited in the event
        EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_PROFILE).userId(user5Id)
                .details(Details.CONTEXT, UserProfileContext.UPDATE_PROFILE.name())
                .details(Details.PREVIOUS_FIRST_NAME, "ExistingFirst").details(Details.UPDATED_FIRST_NAME, "First")
                .details(Details.PREVIOUS_LAST_NAME, "ExistingLast").details(Details.UPDATED_LAST_NAME, "Last")
                .details(Details.PREF_UPDATED + "department", "Department");
    }

    @Test
    public void testDefaultProfile() {
        registerUserCleanup("login-test");
        doTestDefaultProfile();
    }

    @Test
    public void testIgnoreCustomAttributeWhenUserProfileIsDisabled() {
        registerUserCleanup("login-test");
        runOnServer.run(setEmptyFirstNameAndCustomAttribute());
        doTestDefaultProfile();
    }

    private void doTestDefaultProfile() {
        setUserProfileConfiguration(null);

        runOnServer.run(setEmptyFirstNameAndCustomAttribute());

        oauth.openLoginForm();
        login("login-test");

        //submit with error
        verifyProfilePage.assertCurrent();
        assertFalse(verifyProfilePage.isDepartmentPresent());
        verifyProfilePage.update("First", " ");

        //submit OK
        verifyProfilePage.assertCurrent();
        assertFalse(verifyProfilePage.isDepartmentPresent());
        verifyProfilePage.update("First", "Last");
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getUser("login-test");
        assertEquals("First", user.getFirstName());
        assertEquals("Last", user.getLastName());
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
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser("login-test5", null, "ExistingLast", null);

        setUserProfileConfiguration(null);

        managedRealm.updateWithCleanup(r -> r.editUsernameAllowed(false));

        oauth.openLoginForm();
        login("login-test5");

        verifyProfilePage.assertCurrent();
        assertFalse(verifyProfilePage.isUsernamePresent());
        assertTrue(verifyProfilePage.isEmailInputPresent());

        managedRealm.updateWithCleanup(r -> r.editUsernameAllowed(true));

        driver.navigate().refresh();
        assertTrue(verifyProfilePage.isUsernamePresent());
    }

    @Test
    public void testUsernameOnlyIfEmailAsUsernameIsDisabled() {
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser("login-test5", null, "ExistingLast", null);

        setUserProfileConfiguration(null);

        managedRealm.updateWithCleanup(r -> r.editUsernameAllowed(true).registrationEmailAsUsername(true));

        oauth.openLoginForm();
        login("login-test5");

        verifyProfilePage.assertCurrent();
        assertFalse(verifyProfilePage.isUsernamePresent());
        assertTrue(verifyProfilePage.isEmailInputPresent());

        managedRealm.updateWithCleanup(r -> r.editUsernameAllowed(false).registrationEmailAsUsername(true));

        driver.navigate().refresh();
        verifyProfilePage.assertCurrent();
        assertFalse(verifyProfilePage.isUsernamePresent());
        assertFalse(verifyProfilePage.isEmailInputPresent());

        managedRealm.updateWithCleanup(r -> r.editUsernameAllowed(true).registrationEmailAsUsername(false));

        driver.navigate().refresh();
        verifyProfilePage.assertCurrent();
        assertTrue(verifyProfilePage.isUsernamePresent());
        assertTrue(verifyProfilePage.isEmailInputPresent());
    }

    @Test
    public void testUsernameOnlyIfEmailAsUsernameIsDisabledWithUpdateEmailFeature() {
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser("login-test5", null, "ExistingLast", null);

        managedRealm.cleanup().add(r -> setRequiredActionEnabled(r, RequiredAction.UPDATE_EMAIL, false));
        setRequiredActionEnabled(managedRealm.admin(), RequiredAction.UPDATE_EMAIL, true);

        setUserProfileConfiguration(null);

        managedRealm.updateWithCleanup(r -> r.editUsernameAllowed(true).registrationEmailAsUsername(true));

        oauth.openLoginForm();
        login("login-test5");

        verifyProfilePage.assertCurrent();
        assertFalse(verifyProfilePage.isUsernamePresent());
        assertFalse(verifyProfilePage.isEmailInputPresent());

        managedRealm.updateWithCleanup(r -> r.editUsernameAllowed(false).registrationEmailAsUsername(true));

        driver.navigate().refresh();
        verifyProfilePage.assertCurrent();
        assertFalse(verifyProfilePage.isUsernamePresent());
        assertFalse(verifyProfilePage.isEmailInputPresent());

        managedRealm.updateWithCleanup(r -> r.editUsernameAllowed(true).registrationEmailAsUsername(false));

        driver.navigate().refresh();
        verifyProfilePage.assertCurrent();
        assertTrue(verifyProfilePage.isUsernamePresent());
        assertFalse(verifyProfilePage.isEmailInputPresent());
    }

    @Test
    public void testOptionalAttribute() {
        registerUserCleanup("login-test2");

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "}"
                + "]}");

        oauth.openLoginForm();
        login("login-test2");

        verifyProfilePage.assertCurrent();
        verifyProfilePage.update("First", "");
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getUser("login-test2");
        assertEquals("First", user.getFirstName());
        assertTrue(StringUtil.isBlank(user.getLastName()));
    }

    @Test
    public void testCustomValidationLastName() {
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser("login-test5", "ExistingFirst", "La", "Department");

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "," + VALIDATIONS_LENGTH + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ADMIN_ONLY + "}"
                + "]}");

        oauth.openLoginForm();
        login("login-test5");

        verifyProfilePage.assertCurrent();
        //submit with error
        verifyProfilePage.update("First", "L");

        verifyProfilePage.assertCurrent();
        //submit OK
        verifyProfilePage.update("First", "Last");
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getUser("login-test5");
        assertEquals("First", user.getFirstName());
        assertEquals("Last", user.getLastName());
        //check that not configured attribute is unchanged
        assertEquals("Department", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testNoActionIfNoValidationError() {
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser("login-test5", "ExistingFirst", "ExistingLast", "Department");

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "," + VALIDATIONS_LENGTH + "}"
                + "]}");

        oauth.openLoginForm();
        login("login-test5");
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void testDoNotValidateUsernameWhenRegistrationAsEmailEnabled() {
        // toggles registrationEmailAsUsername and mutates a user whose username differs from its email; recreate the
        // realm fresh afterwards rather than reasoning about cleanup ordering under that realm state
        managedRealm.dirty();

        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser("login-test6", "ExistingFirst", "ExistingLast", "Department");

        managedRealm.updateWithCleanup(r -> r.registrationEmailAsUsername(true));

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "," + VALIDATIONS_LENGTH + "}"
                + "]}");

        oauth.openLoginForm();
        login("login6@test.com");

        assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void testRequiredReadOnlyAttribute() {
        registerUserCleanup("login-test3");

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ADMIN_EDITABLE + ", \"required\":{}}"
                + "]}");

        oauth.openLoginForm();
        login("login-test3");

        verifyProfilePage.assertCurrent();
        assertEquals("ExistingLast", verifyProfilePage.getLastName());
        assertFalse(verifyProfilePage.isDepartmentEnabled());

        //update of the other attributes must be successful in this case
        verifyProfilePage.update("First", "Last");
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getUser("login-test3");
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

        oauth.openLoginForm();
        login("login-test6");

        verifyProfilePage.assertCurrent();
        assertEquals("ExistingLast", verifyProfilePage.getLastName());
        assertFalse(verifyProfilePage.isDepartmentPresent(), "Admin-only attribute should not be visible for user");
    }

    @Test
    public void testUsernameReadOnlyInProfile() {
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"username\"," + PERMISSIONS_ADMIN_EDITABLE + "},"
                + "{\"name\": \"requiredAttrToTriggerVerifyPage\"," + PERMISSIONS_ALL + ", \"required\": {}}"
                + "]}");

        oauth.openLoginForm();
        login("login-test6");

        verifyProfilePage.assertCurrent();
        assertEquals("ExistingLast", verifyProfilePage.getLastName());

        assertFalse(verifyProfilePage.isUsernameEnabled(), "username should not be editable by user");
    }

    @Test
    public void testUsernameReadNotVisibleInProfile() {
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"username\"," + PERMISSIONS_ADMIN_ONLY + "},"
                + "{\"name\": \"requiredAttrToTriggerVerifyPage\"," + PERMISSIONS_ALL + ", \"required\": {}}"
                + "]}");

        oauth.openLoginForm();
        login("login-test6");

        verifyProfilePage.assertCurrent();
        assertEquals("ExistingLast", verifyProfilePage.getLastName());

        assertFalse(verifyProfilePage.isUsernamePresent(), "username should not be shown to user");
    }

    @Test
    public void testEMailRequiredInProfileWithLocalPartLength() {
        registerUserCleanup("login-nomail");

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"username\"," + PERMISSIONS_ADMIN_ONLY + "},"
                + "{\"name\": \"email\"," + PERMISSIONS_ALL + ", \"required\":{\"roles\":[\"user\"]}, \"validations\": {\"email\": {\"max-local-length\": \"16\"}}}"
                + "]}");

        oauth.openLoginForm();
        login("login-nomail");

        // no email is set => expect verify profile page to be displayed
        verifyProfilePage.assertCurrent();

        // set e-mail with legth 17 => error
        verifyProfilePage.prepareUpdate().email("abcdefg0123456789@bar.com").firstName("HasNowMailFirst").lastName("HasNowMailLast").submit();
        verifyProfilePage.assertCurrent();

        // set e-mail, update firstname/lastname and complete login
        verifyProfilePage.prepareUpdate().email("abcdef0123456789@bar.com").firstName("HasNowMailFirst").lastName("HasNowMailLast").submit();
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getUser("login-nomail");
        assertEquals("HasNowMailFirst", user.getFirstName());
        assertEquals("HasNowMailLast", user.getLastName());
        assertEquals("abcdef0123456789@bar.com", user.getEmail());
    }

    @Test
    public void testAttributeNotVisible() {
        registerUserCleanup("login-test4");

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ADMIN_ONLY + ", \"required\":{}}"
                + "]}");

        oauth.openLoginForm();
        login("login-test4");

        verifyProfilePage.assertCurrent();
        assertEquals("ExistingLast", verifyProfilePage.getLastName());
        assertFalse(verifyProfilePage.isDepartmentPresent(), "'department' field is visible");

        //update of the other attributes must be successful in this case
        verifyProfilePage.update("First", "Last");
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getUser("login-test4");
        assertEquals("First", user.getFirstName());
        assertEquals("Last", user.getLastName());
    }

    @Test
    public void testRequiredAttribute() {
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser("login-test5", "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}}"
                + "]}");

        oauth.openLoginForm();
        login("login-test5");

        verifyProfilePage.assertCurrent();

        //submit with error
        verifyProfilePage.prepareUpdate().firstName("FirstCC").lastName("LastCC").department(" ").submit();
        verifyProfilePage.assertCurrent();

        //submit OK
        verifyProfilePage.prepareUpdate().firstName("FirstCC").lastName("LastCC").department("DepartmentCC").submit();
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getUser("login-test5");
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

        updateUser("login-test5", "ExistingFirst", "ExistingLast", null);

        oauth.openLoginForm();
        login("login-test5");

        verifyProfilePage.assertCurrent();

        //submit with error
        verifyProfilePage.prepareUpdate().firstName("FirstCC").lastName("LastCC").department(" ").submit();
        verifyProfilePage.assertCurrent();

        //submit OK
        verifyProfilePage.prepareUpdate().firstName("FirstCC").lastName("LastCC").department("DepartmentCC").submit();
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getUser("login-test5");
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

        updateUser("login-test5", "ExistingFirst", "ExistingLast", null);

        oauth.client("client-b").openLoginForm();

        login("login-test5");
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getUser("login-test5");
        assertEquals("ExistingFirst", user.getFirstName());
        assertEquals("ExistingLast", user.getLastName());
    }

    @Test
    public void testAttributeRequiredForScope() {
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{\"scopes\":[\"" + SCOPE_DEPARTMENT + "\"]}}"
                + "]}");

        updateUser("login-test5", "ExistingFirst", "ExistingLast", null);

        oauth.scope(SCOPE_DEPARTMENT).client("client-b").openLoginForm();

        loginPage.assertCurrent();
        login("login-test5");

        verifyProfilePage.assertCurrent();

        verifyProfilePage.prepareUpdate().firstName("FirstAA").lastName("LastAA").department("DepartmentAA").submit();
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getUser("login-test5");
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertEquals("DepartmentAA", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testAttributeRequiredForDefaultScope() {
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{\"scopes\":[\"" + SCOPE_DEPARTMENT + "\"]}}"
                + "]}");

        updateUser("login-test5", "ExistingFirst", "ExistingLast", null);

        oauth.client("client-a").openLoginForm();

        loginPage.assertCurrent();
        login("login-test5");

        verifyProfilePage.assertCurrent();

        //submit with error
        verifyProfilePage.prepareUpdate().firstName("FirstBB").lastName("LastBB").department(" ").submit();
        verifyProfilePage.assertCurrent();

        //submit OK
        verifyProfilePage.prepareUpdate().firstName("FirstBB").lastName("LastBB").department("DepartmentBB").submit();
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getUser("login-test5");
        assertEquals("FirstBB", user.getFirstName());
        assertEquals("LastBB", user.getLastName());
        assertEquals("DepartmentBB", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testNoActionIfValidForScope() {
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{\"scopes\":[\"" + SCOPE_DEPARTMENT + "\"]}}"
                + "]}");

        updateUser("login-test5", "ExistingFirst", "ExistingLast", "ExistingDepartment");

        oauth.client("client-a").openLoginForm();

        loginPage.assertCurrent();
        login("login-test5");
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getUser("login-test5");
        assertEquals("ExistingFirst", user.getFirstName());
        assertEquals("ExistingLast", user.getLastName());
        assertEquals("ExistingDepartment", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testAttributeRequiredButNotSelectedByScopeDoesntForceVerificationScreen() {
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser("login-test5", "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\"" + SCOPE_DEPARTMENT + "\"]}}"
                + "]}");

        oauth.client("client-b").openLoginForm();

        loginPage.assertCurrent();
        login("login-test5");
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void testAttributeRequiredAndSelectedByScope() {
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser("login-test5", "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\"" + SCOPE_DEPARTMENT + "\"]}}"
                + "]}");

        oauth.scope(SCOPE_DEPARTMENT).client("client-b").openLoginForm();

        loginPage.assertCurrent();
        login("login-test5");

        verifyProfilePage.assertCurrent();

        verifyProfilePage.prepareUpdate().firstName("FirstAA").lastName("LastAA").department("DepartmentAA").submit();
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getUser("login-test5");
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertEquals("DepartmentAA", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testAttributeNotRequiredAndSelectedByScopeCanBeUpdatedFromVerificationScreenForcedByAnotherAttribute() {
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser("login-test5", "ExistingFirst", null, null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"selector\":{\"scopes\":[\"" + SCOPE_DEPARTMENT + "\"]}}"
                + "]}");

        oauth.scope(SCOPE_DEPARTMENT).client("client-b").openLoginForm();

        loginPage.assertCurrent();
        login("login-test5");

        verifyProfilePage.assertCurrent();

        assertTrue(verifyProfilePage.isDepartmentPresent());
        verifyProfilePage.prepareUpdate().firstName("FirstAA").lastName("LastAA").department("Department AA").submit();
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getUser("login-test5");
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertEquals("Department AA", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testAttributeRequiredButNotSelectedByScopeIsNotRenderedOnVerificationScreenForcedByAnotherAttribute() {
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser("login-test5", "ExistingFirst", null, null);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\"" + SCOPE_DEPARTMENT + "\"]}}"
                + "]}");

        oauth.client("client-b").openLoginForm();

        loginPage.assertCurrent();
        login("login-test5");

        verifyProfilePage.assertCurrent();

        assertFalse(verifyProfilePage.isDepartmentPresent());
        verifyProfilePage.update("FirstAA", "LastAA");
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getUser("login-test5");
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertNull(user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testCustomValidationInCustomAttribute() {
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser("login-test5", "ExistingFirst", "ExistingLast", "D");

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", " + VALIDATIONS_LENGTH + "}"
                + "]}");

        oauth.openLoginForm();
        login("login-test5");

        verifyProfilePage.assertCurrent();

        //submit with error
        verifyProfilePage.prepareUpdate().firstName("FirstCC").lastName("LastCC").department("De").submit();
        verifyProfilePage.assertCurrent();

        //submit OK
        verifyProfilePage.prepareUpdate().firstName("FirstCC").lastName("LastCC").department("DepartmentCC").submit();
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getUser("login-test5");
        assertEquals("FirstCC", user.getFirstName());
        assertEquals("LastCC", user.getLastName());
        assertEquals("DepartmentCC", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testEmailChangeSetsEmailVerified() {
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser("login-test5", true, "", "ExistingLast");

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "}"
                + "]}");

        oauth.openLoginForm();
        login("login-test5");

        verifyProfilePage.assertCurrent();

        //submit OK
        verifyProfilePage.prepareUpdate().email("newemail@test.org").firstName("FirstCC").lastName("LastCC").submit();
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getUser("login-test5");
        assertEquals("newemail@test.org", user.getEmail());
        assertFalse(user.isEmailVerified());
    }

    @Test
    public void testNoActionIfSuccessfulValidationForCustomAttribute() {
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUser("login-test5", "ExistingFirst", "ExistingLast", "Department");

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", " + VALIDATIONS_LENGTH + "}"
                + "]}");

        oauth.openLoginForm();
        login("login-test5");
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void testConfigurationPersisted() throws IOException {
        String customConfig = "{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", " + VALIDATIONS_LENGTH + "}"
                + "]}";

        UPConfig persistedConfig = setUserProfileConfiguration(customConfig);

        JsonTestUtils.assertJsonEquals(JsonSerialization.writeValueAsString(persistedConfig), managedRealm.admin().users().userProfile().getConfiguration());
    }

    private void login(String username) {
        loginPage.fillLogin(username, PASSWORD);
        loginPage.submit();
    }

    private UPConfig setUserProfileConfiguration(String configuration) {
        if (!userProfileResetRegistered) {
            managedRealm.cleanup().add(r -> UserProfileUtil.setUserProfileConfiguration(r, null));
            userProfileResetRegistered = true;
        }
        return UserProfileUtil.setUserProfileConfiguration(managedRealm.admin(), configuration);
    }

    private UserResource getUserResource(String username) {
        return AdminApiUtil.findUserByUsernameId(managedRealm.admin(), username);
    }

    private UserRepresentation getUser(String username) {
        return getUserResource(username).toRepresentation();
    }

    private void registerUserCleanup(String username) {
        UserRepresentation original = getUser(username);
        managedRealm.cleanup().add(r -> r.users().get(original.getId()).update(original));
    }

    private void updateUser(String username, String firstName, String lastName, String department) {
        UserResource userResource = getUserResource(username);
        UserRepresentation original = userResource.toRepresentation();
        managedRealm.cleanup().add(r -> r.users().get(original.getId()).update(original));

        UserRepresentation user = userResource.toRepresentation();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.singleAttribute(ATTRIBUTE_DEPARTMENT, department);
        userResource.update(user);
    }

    private void updateUser(String username, boolean emailVerified, String firstName, String lastName) {
        UserResource userResource = getUserResource(username);
        UserRepresentation original = userResource.toRepresentation();
        managedRealm.cleanup().add(r -> r.users().get(original.getId()).update(original));

        UserRepresentation user = userResource.toRepresentation();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmailVerified(emailVerified);
        userResource.update(user);
    }

    private static void setRequiredActionEnabled(RealmResource realm, RequiredAction action, boolean enabled) {
        RequiredActionProviderRepresentation requiredAction = realm.flows().getRequiredActions().stream()
                .filter(a -> action.name().equals(a.getAlias()))
                .findAny().orElseThrow(() -> new IllegalStateException("Required action not found: " + action.name()));
        requiredAction.setEnabled(enabled);
        realm.flows().updateRequiredAction(requiredAction.getAlias(), requiredAction);
    }

    private static void assertFieldTypes(WebDriver driver) {
        assertEquals("text", driver.findElement(By.cssSelector("input#defaultType")).getAttribute("type"));

        assertEquals("text", driver.findElement(By.cssSelector("input#placeholderAttribute")).getAttribute("type"));
        assertEquals("Example.", driver.findElement(By.cssSelector("input#placeholderAttribute")).getAttribute("placeholder"));

        assertEquals("Example bold text before.", driver.findElement(By.cssSelector("div#form-help-text-before-helperTexts")).getText());
        assertEquals("bold text", driver.findElement(By.cssSelector("div#form-help-text-before-helperTexts b")).getText());
        assertEquals("Example i text after.", driver.findElement(By.cssSelector("div#form-help-text-after-helperTexts")).getText());
        assertEquals("i text", driver.findElement(By.cssSelector("div#form-help-text-after-helperTexts i")).getText());

        assertEquals("text", driver.findElement(By.cssSelector("input#textWithBasicAttributes")).getAttribute("type"));
        assertEquals("35", driver.findElement(By.cssSelector("input#textWithBasicAttributes")).getAttribute("size"));
        assertEquals("1", driver.findElement(By.cssSelector("input#textWithBasicAttributes")).getAttribute("minlength"));
        assertEquals("10", driver.findElement(By.cssSelector("input#textWithBasicAttributes")).getAttribute("maxlength"));
        assertEquals(".*", driver.findElement(By.cssSelector("input#textWithBasicAttributes")).getAttribute("pattern"));

        assertEquals("number", driver.findElement(By.cssSelector("input#html5NumberWithAttributes")).getAttribute("type"));
        assertEquals("10", driver.findElement(By.cssSelector("input#html5NumberWithAttributes")).getAttribute("min"));
        assertEquals("20", driver.findElement(By.cssSelector("input#html5NumberWithAttributes")).getAttribute("max"));
        assertEquals("1", driver.findElement(By.cssSelector("input#html5NumberWithAttributes")).getAttribute("step"));

        assertEquals("35", driver.findElement(By.cssSelector("textarea#textareaWithAttributes")).getAttribute("cols"));
        assertEquals("7", driver.findElement(By.cssSelector("textarea#textareaWithAttributes")).getAttribute("rows"));
        assertEquals("10", driver.findElement(By.cssSelector("textarea#textareaWithAttributes")).getAttribute("maxlength"));

        assertEquals("5", driver.findElement(By.cssSelector("select#selectWithoutOptions")).getAttribute("size"));

        assertEquals(null, driver.findElement(By.cssSelector("select#selectWithOptionsWithoutLabels")).getAttribute("multiple"));
        assertEquals("opt1", driver.findElement(By.cssSelector("select#selectWithOptionsWithoutLabels option[value=opt1]")).getText());
        assertEquals("opt2", driver.findElement(By.cssSelector("select#selectWithOptionsWithoutLabels option[value=opt2]")).getText());
        assertEquals("", driver.findElement(By.cssSelector("select#selectWithOptionsWithoutLabels option[value='']")).getText(), "default empty option is missing in select");

        assertEquals("true", driver.findElement(By.cssSelector("select#multiselectWithOptionsAndSimpleI18nLabels")).getAttribute("multiple"));
        assertEquals("Time-based", driver.findElement(By.cssSelector("select#multiselectWithOptionsAndSimpleI18nLabels option[value=totp]")).getText());
        assertEquals("loginTotp.opt2", driver.findElement(By.cssSelector("select#multiselectWithOptionsAndSimpleI18nLabels option[value=opt2]")).getText());

        assertEquals("true", driver.findElement(By.cssSelector("select#multiselectWithOptionsAndLabels")).getAttribute("multiple"));
        assertEquals("Option 1", driver.findElement(By.cssSelector("select#multiselectWithOptionsAndLabels option[value=opt1]")).getText());
        assertEquals("Username", driver.findElement(By.cssSelector("select#multiselectWithOptionsAndLabels option[value=opt2]")).getText());
        assertEquals("opt3", driver.findElement(By.cssSelector("select#multiselectWithOptionsAndLabels option[value=opt3]")).getText());

        assertEquals(null, driver.findElement(By.cssSelector("select#selectWithOptionsFromCustomValidatorAndLabels")).getAttribute("multiple"));
        assertEquals("Option 1", driver.findElement(By.cssSelector("select#selectWithOptionsFromCustomValidatorAndLabels option[value=vopt1]")).getText());
        assertEquals("Username", driver.findElement(By.cssSelector("select#selectWithOptionsFromCustomValidatorAndLabels option[value=vopt2]")).getText());
        assertEquals("vopt3", driver.findElement(By.cssSelector("select#selectWithOptionsFromCustomValidatorAndLabels option[value=vopt3]")).getText());

        assertEquals("radio", driver.findElement(By.cssSelector("input#selectRadiobuttons-opt1")).getAttribute("type"));
        assertEquals("Option 1", driver.findElement(By.cssSelector("label[for=selectRadiobuttons-opt1]")).getText());
        assertEquals("radio", driver.findElement(By.cssSelector("input#selectRadiobuttons-opt2")).getAttribute("type"));
        assertEquals("Username", driver.findElement(By.cssSelector("label[for=selectRadiobuttons-opt2]")).getText());
        assertEquals("radio", driver.findElement(By.cssSelector("input#selectRadiobuttons-opt3")).getAttribute("type"));
        assertEquals("opt3", driver.findElement(By.cssSelector("label[for=selectRadiobuttons-opt3]")).getText());

        assertEquals("radio", driver.findElement(By.cssSelector("input#selectRadiobuttonsWithOptionsFromCustomValidatorAndLabels-vopt1")).getAttribute("type"));
        assertEquals("Option 1", driver.findElement(By.cssSelector("label[for=selectRadiobuttonsWithOptionsFromCustomValidatorAndLabels-vopt1]")).getText());
        assertEquals("radio", driver.findElement(By.cssSelector("input#selectRadiobuttonsWithOptionsFromCustomValidatorAndLabels-vopt2")).getAttribute("type"));
        assertEquals("Username", driver.findElement(By.cssSelector("label[for=selectRadiobuttonsWithOptionsFromCustomValidatorAndLabels-vopt2]")).getText());
        assertEquals("radio", driver.findElement(By.cssSelector("input#selectRadiobuttonsWithOptionsFromCustomValidatorAndLabels-vopt3")).getAttribute("type"));
        assertEquals("vopt3", driver.findElement(By.cssSelector("label[for=selectRadiobuttonsWithOptionsFromCustomValidatorAndLabels-vopt3]")).getText());

        assertEquals("checkbox", driver.findElement(By.cssSelector("input#multiselectCheckboxes-opt1")).getAttribute("type"));
        assertEquals("Option 1", driver.findElement(By.cssSelector("label[for=multiselectCheckboxes-opt1]")).getText());
        assertEquals("checkbox", driver.findElement(By.cssSelector("input#multiselectCheckboxes-opt2")).getAttribute("type"));
        assertEquals("Username", driver.findElement(By.cssSelector("label[for=multiselectCheckboxes-opt2]")).getText());
        assertEquals("checkbox", driver.findElement(By.cssSelector("input#multiselectCheckboxes-opt3")).getAttribute("type"));
        assertEquals("opt3", driver.findElement(By.cssSelector("label[for=multiselectCheckboxes-opt3]")).getText());
    }

    public static class VerifyProfileRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.users(
                    UserBuilder.create().username("login-test").email("login@test.com").password(PASSWORD),
                    UserBuilder.create().username("login-test2").email("login2@test.com").password(PASSWORD),
                    UserBuilder.create().username("login-test3").email("login3@test.com").lastName("ExistingLast").password(PASSWORD),
                    UserBuilder.create().username("login-test4").email("login4@test.com").lastName("ExistingLast").password(PASSWORD),
                    UserBuilder.create().username("login-test5").email("login5@test.com").name("ExistingFirst", "ExistingLast").password(PASSWORD),
                    UserBuilder.create().username("login-test6").email("login6@test.com").name("ExistingFirst", "ExistingLast").password(PASSWORD),
                    UserBuilder.create().username("login-nomail").name("NoMailFirst", "NoMailLast").password(PASSWORD)
            );

            // Provide an explicit client scope list so the built-in default scopes are NOT created: the scope-selector
            // tests rely on 'profile' never being present on client-b (see testAttributeNotRequiredWhenMissingScope).
            realm.clientScopes(
                    ClientScopeBuilder.create().name(SCOPE_DEPARTMENT).protocol("openid-connect").build(),
                    ClientScopeBuilder.create().name("profile").protocol("openid-connect").build()
            );

            realm.clients(
                    ClientBuilder.create("client-a").publicClient().redirectUris("*").defaultClientScopes(SCOPE_DEPARTMENT),
                    ClientBuilder.create("client-b").publicClient().redirectUris("*").optionalClientScopes(SCOPE_DEPARTMENT)
            );

            return realm;
        }
    }
}
