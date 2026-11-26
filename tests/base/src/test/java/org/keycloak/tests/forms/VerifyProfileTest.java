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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPAttributeRequired;
import org.keycloak.representations.userprofile.config.UPAttributeSelector;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPGroup;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
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
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.tests.utils.JsonTestUtils;
import org.keycloak.tests.utils.PasswordGenerateUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.models.UserModel.EMAIL;
import static org.keycloak.models.UserModel.FIRST_NAME;
import static org.keycloak.models.UserModel.LAST_NAME;
import static org.keycloak.models.UserModel.USERNAME;
import static org.keycloak.testsuite.util.userprofile.UserProfileUtil.ATTRIBUTE_DEPARTMENT;
import static org.keycloak.testsuite.util.userprofile.UserProfileUtil.SCOPE_DEPARTMENT;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_ADMIN;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_USER;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlastimil Elias <velias@redhat.com>
 */
@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
@DatabaseTest
public class VerifyProfileTest {

    private static final String PASSWORD = PasswordGenerateUtil.generatePassword();

    private static final UPAttributePermissions ALL = new UPAttributePermissions(Set.of(ROLE_ADMIN, ROLE_USER), Set.of(ROLE_ADMIN, ROLE_USER));
    private static final UPAttributePermissions ADMIN_ONLY = new UPAttributePermissions(Set.of(ROLE_ADMIN), Set.of(ROLE_ADMIN));
    private static final UPAttributePermissions ADMIN_EDITABLE = new UPAttributePermissions(Set.of(ROLE_ADMIN, ROLE_USER), Set.of(ROLE_ADMIN));

    private static final List<String> OPTIONS = List.of("opt1", "opt2", "opt3");
    private static final Map<String, String> OPTION_LABELS = Map.of("opt1", "Option 1", "opt2", "${username}");
    private static final List<String> VALIDATOR_OPTIONS = List.of("vopt1", "vopt2", "vopt3");
    private static final Map<String, String> VALIDATOR_OPTION_LABELS = Map.of("vopt1", "Option 1", "vopt2", "${username}");

    @InjectRealm(config = VerifyProfileRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @InjectAdminEvents
    AdminEvents adminEvents;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginUpdateProfilePage verifyProfilePage;

    private boolean userProfileResetRegistered;

    @Test
    public void testDisplayName() {
        prepareUser("login-test5", "ExistingFirst", "ExistingLast", null);

        UPAttribute firstName = required(FIRST_NAME, ALL);
        firstName.setDisplayName("${firstName}");
        UPAttribute department = required(ATTRIBUTE_DEPARTMENT, ALL);
        department.setDisplayName("Department");
        setUserProfileConfiguration(firstName, attribute(LAST_NAME, ALL), department);

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
        prepareUser("login-test5", "ExistingFirst", "ExistingLast", null);

        UPAttribute department = required(ATTRIBUTE_DEPARTMENT, ALL);
        department.setGroup("company");
        UPAttribute email = attribute(EMAIL, ALL);
        email.setGroup("contact");
        UPGroup company = new UPGroup("company");
        company.setDisplayDescription("Company field desc");
        setUserProfileConfiguration(config(attribute(LAST_NAME, ALL), attribute(USERNAME, ALL), required(FIRST_NAME, ALL), department, email)
                .addGroup(company)
                .addGroup(new UPGroup("contact")));

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
        prepareUser("login-test5", "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration(
                attribute(LAST_NAME, ALL),
                required(ATTRIBUTE_DEPARTMENT, ALL),
                attribute(USERNAME, ALL),
                required(FIRST_NAME, ALL),
                attribute(EMAIL, ALL));

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
        prepareUser("login-test5", "ExistingFirst", "ExistingLast", null);

        UPConfig config = config(required(ATTRIBUTE_DEPARTMENT, ALL));
        inputTypeAttributes().forEach(config::addOrReplaceAttribute);
        setUserProfileConfiguration(config);

        oauth.openLoginForm();
        login("login-test5");

        verifyProfilePage.assertCurrent();

        assertFieldTypes();
    }

    @Test
    public void testEvents() {
        prepareUser("login-test5", "ExistingFirst", "ExistingLast", null);
        String user5Id = getUser("login-test5").getId();

        setUserProfileConfiguration(required(FIRST_NAME, ALL), attribute(LAST_NAME, ALL), required(ATTRIBUTE_DEPARTMENT, ALL));

        oauth.openLoginForm();
        login("login-test5");

        verifyProfilePage.assertCurrent();
        //event when form is shown
        EventAssertion.expectRequiredAction(events.poll()).type(EventType.VERIFY_PROFILE).userId(user5Id)
                .details(Details.FIELDS_TO_UPDATE, "department");

        verifyProfilePage.prepareUpdate().firstName("First").lastName("Last").department("Department").submit();
        managedRealm.cleanup().add(RealmResource::logoutAll);
        // wait for the flow to complete so the UPDATE_PROFILE event is recorded before polling for it
        driver.waiting().waitForOAuthCallback();
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
        resetUserProfileConfiguration();

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
        prepareUser("login-test5", null, "ExistingLast", null);

        resetUserProfileConfiguration();

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
        prepareUser("login-test5", null, "ExistingLast", null);

        resetUserProfileConfiguration();

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
        prepareUser("login-test5", null, "ExistingLast", null);

        managedRealm.cleanup().add(r -> setRequiredActionEnabled(r, RequiredAction.UPDATE_EMAIL, false));
        setRequiredActionEnabled(managedRealm.admin(), RequiredAction.UPDATE_EMAIL, true);

        resetUserProfileConfiguration();

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
        setUserProfileConfiguration(required(FIRST_NAME, ALL), attribute(LAST_NAME, ALL));

        registerUserCleanup("login-test2");

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
        prepareUser("login-test5", "ExistingFirst", "La", "Department");

        setUserProfileConfiguration(required(FIRST_NAME, ALL), lengthValidated(LAST_NAME), attribute(ATTRIBUTE_DEPARTMENT, ADMIN_ONLY));

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
        prepareUser("login-test5", "ExistingFirst", "ExistingLast", "Department");

        setUserProfileConfiguration(required(FIRST_NAME, ALL), lengthValidated(LAST_NAME));

        oauth.openLoginForm();
        login("login-test5");
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void testDoNotValidateUsernameWhenRegistrationAsEmailEnabled() {
        managedRealm.dirty();

        prepareUser("login-test6", "ExistingFirst", "ExistingLast", "Department");

        managedRealm.updateWithCleanup(r -> r.registrationEmailAsUsername(true));

        setUserProfileConfiguration(required(FIRST_NAME, ALL), lengthValidated(LAST_NAME));

        oauth.openLoginForm();
        login("login6@test.com");

        assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void testRequiredReadOnlyAttribute() {
        setUserProfileConfiguration(required(FIRST_NAME, ALL), attribute(LAST_NAME, ALL), required(ATTRIBUTE_DEPARTMENT, ADMIN_EDITABLE));

        registerUserCleanup("login-test3");

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
        setUserProfileConfiguration(
                required(FIRST_NAME, ALL),
                attribute(LAST_NAME, ALL),
                attribute(ATTRIBUTE_DEPARTMENT, ADMIN_ONLY),
                required("requiredAttrToTriggerVerifyPage", ALL));

        oauth.openLoginForm();
        login("login-test6");

        verifyProfilePage.assertCurrent();
        assertEquals("ExistingLast", verifyProfilePage.getLastName());
        assertFalse(verifyProfilePage.isDepartmentPresent(), "Admin-only attribute should not be visible for user");
    }

    @Test
    public void testUsernameReadOnlyInProfile() {
        setUserProfileConfiguration(
                required(FIRST_NAME, ALL),
                attribute(LAST_NAME, ALL),
                attribute(USERNAME, ADMIN_EDITABLE),
                required("requiredAttrToTriggerVerifyPage", ALL));

        oauth.openLoginForm();
        login("login-test6");

        verifyProfilePage.assertCurrent();
        assertEquals("ExistingLast", verifyProfilePage.getLastName());

        assertFalse(verifyProfilePage.isUsernameEnabled(), "username should not be editable by user");
    }

    @Test
    public void testUsernameReadNotVisibleInProfile() {
        setUserProfileConfiguration(
                required(FIRST_NAME, ALL),
                attribute(LAST_NAME, ALL),
                attribute(USERNAME, ADMIN_ONLY),
                required("requiredAttrToTriggerVerifyPage", ALL));

        oauth.openLoginForm();
        login("login-test6");

        verifyProfilePage.assertCurrent();
        assertEquals("ExistingLast", verifyProfilePage.getLastName());

        assertFalse(verifyProfilePage.isUsernamePresent(), "username should not be shown to user");
    }

    @Test
    public void testEMailRequiredInProfileWithLocalPartLength() {
        UPAttribute email = requiredForRole(EMAIL, ROLE_USER);
        email.addValidation("email", Map.of("max-local-length", "16"));
        setUserProfileConfiguration(required(FIRST_NAME, ALL), attribute(LAST_NAME, ALL), attribute(USERNAME, ADMIN_ONLY), email);

        registerUserCleanup("login-nomail");

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
        setUserProfileConfiguration(required(FIRST_NAME, ALL), attribute(LAST_NAME, ALL), required(ATTRIBUTE_DEPARTMENT, ADMIN_ONLY));

        registerUserCleanup("login-test4");

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
        prepareUser("login-test5", "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration(required(FIRST_NAME, ALL), attribute(LAST_NAME, ALL), required(ATTRIBUTE_DEPARTMENT, ALL));

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
        prepareUser("login-test5", "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration(required(FIRST_NAME, ALL), attribute(LAST_NAME, ALL), requiredForRole(ATTRIBUTE_DEPARTMENT, ROLE_USER));

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
        prepareUser("login-test5", "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration(required(FIRST_NAME, ALL), attribute(LAST_NAME, ALL), requiredForScope(ATTRIBUTE_DEPARTMENT, "profile"));

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
        prepareUser("login-test5", "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration(required(FIRST_NAME, ALL), attribute(LAST_NAME, ALL), requiredForScope(ATTRIBUTE_DEPARTMENT, SCOPE_DEPARTMENT));

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
        prepareUser("login-test5", "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration(required(FIRST_NAME, ALL), attribute(LAST_NAME, ALL), requiredForScope(ATTRIBUTE_DEPARTMENT, SCOPE_DEPARTMENT));

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
        prepareUser("login-test5", "ExistingFirst", "ExistingLast", "ExistingDepartment");

        setUserProfileConfiguration(required(FIRST_NAME, ALL), attribute(LAST_NAME, ALL), requiredForScope(ATTRIBUTE_DEPARTMENT, SCOPE_DEPARTMENT));

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
        prepareUser("login-test5", "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration(required(FIRST_NAME, ALL), attribute(LAST_NAME, ALL), selectedByScope(required(ATTRIBUTE_DEPARTMENT, ALL), SCOPE_DEPARTMENT));

        oauth.client("client-b").openLoginForm();

        loginPage.assertCurrent();
        login("login-test5");
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void testAttributeRequiredAndSelectedByScope() {
        prepareUser("login-test5", "ExistingFirst", "ExistingLast", null);

        setUserProfileConfiguration(required(FIRST_NAME, ALL), attribute(LAST_NAME, ALL), selectedByScope(required(ATTRIBUTE_DEPARTMENT, ALL), SCOPE_DEPARTMENT));

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
        prepareUser("login-test5", "ExistingFirst", null, null);

        setUserProfileConfiguration(required(FIRST_NAME, ALL), required(LAST_NAME, ALL), selectedByScope(attribute(ATTRIBUTE_DEPARTMENT, ALL), SCOPE_DEPARTMENT));

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
        prepareUser("login-test5", "ExistingFirst", null, null);

        setUserProfileConfiguration(required(FIRST_NAME, ALL), required(LAST_NAME, ALL), selectedByScope(required(ATTRIBUTE_DEPARTMENT, ALL), SCOPE_DEPARTMENT));

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
        prepareUser("login-test5", "ExistingFirst", "ExistingLast", "D");

        setUserProfileConfiguration(required(FIRST_NAME, ALL), attribute(LAST_NAME, ALL), lengthValidated(ATTRIBUTE_DEPARTMENT));

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
        prepareUser("login-test5", user -> {
            user.setEmailVerified(true);
            user.setFirstName("");
            user.setLastName("ExistingLast");
        });

        setUserProfileConfiguration(required(FIRST_NAME, ALL), attribute(LAST_NAME, ALL));

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
        prepareUser("login-test5", "ExistingFirst", "ExistingLast", "Department");

        setUserProfileConfiguration(required(FIRST_NAME, ALL), attribute(LAST_NAME, ALL), lengthValidated(ATTRIBUTE_DEPARTMENT));

        oauth.openLoginForm();
        login("login-test5");
        managedRealm.cleanup().add(RealmResource::logoutAll);

        assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void testConfigurationPersisted() throws IOException {
        UPConfig persistedConfig = setUserProfileConfiguration(required(FIRST_NAME, ALL), attribute(LAST_NAME, ALL), lengthValidated(ATTRIBUTE_DEPARTMENT));

        JsonTestUtils.assertJsonEquals(JsonSerialization.writeValueAsString(persistedConfig), managedRealm.admin().users().userProfile().getConfiguration());
    }

    @Test
    public void testConfigurationEmitsAdminEvent() {
        // updating the user profile configuration emits an UPDATE admin event on the user-profile resource, and the event representation is present only for a non-blank configuration
        setUserProfileConfiguration(attribute(FIRST_NAME, ALL), attribute(LAST_NAME, ALL), attribute(ATTRIBUTE_DEPARTMENT, ALL));
        AdminEventRepresentation event = adminEvents.poll();
        AdminEventAssertion.assertEvent(event, OperationType.UPDATE, AdminEventPaths.userProfilePath(), ResourceType.USER_PROFILE);
        assertFalse(StringUtil.isBlank(event.getRepresentation()), "representation should be present for a non-blank configuration");

        resetUserProfileConfiguration();
        event = adminEvents.poll();
        AdminEventAssertion.assertEvent(event, OperationType.UPDATE, AdminEventPaths.userProfilePath(), ResourceType.USER_PROFILE);
        assertTrue(StringUtil.isBlank(event.getRepresentation()), "representation should be blank for a blank configuration");
    }

    private void login(String username) {
        loginPage.fillLogin(username, PASSWORD);
        loginPage.submit();
    }

    private UPConfig setUserProfileConfiguration(UPAttribute... attributes) {
        return setUserProfileConfiguration(config(attributes));
    }

    private UPConfig setUserProfileConfiguration(UPConfig config) {
        if (!userProfileResetRegistered) {
            managedRealm.cleanup().add(r -> UserProfileUtil.updateUserProfileConfiguration(r, null));
            userProfileResetRegistered = true;
        }
        return UserProfileUtil.updateUserProfileConfiguration(managedRealm.admin(), config);
    }

    private void resetUserProfileConfiguration() {
        setUserProfileConfiguration((UPConfig) null);
    }

    private static UPConfig config(UPAttribute... attributes) {
        UPConfig config = new UPConfig();
        for (UPAttribute attribute : attributes) {
            config.addOrReplaceAttribute(attribute);
        }
        return config;
    }

    private static UPAttribute attribute(String name, UPAttributePermissions permissions) {
        return new UPAttribute(name, permissions);
    }

    private static UPAttribute required(String name, UPAttributePermissions permissions) {
        return new UPAttribute(name, permissions, new UPAttributeRequired());
    }

    private static UPAttribute requiredForRole(String name, String role) {
        return new UPAttribute(name, ALL, new UPAttributeRequired(Set.of(role), Set.of()));
    }

    private static UPAttribute requiredForScope(String name, String scope) {
        return new UPAttribute(name, ALL, new UPAttributeRequired(Set.of(), Set.of(scope)));
    }

    private static UPAttribute selectedByScope(UPAttribute attribute, String scope) {
        attribute.setSelector(new UPAttributeSelector(Set.of(scope)));
        return attribute;
    }

    private static UPAttribute lengthValidated(String name) {
        UPAttribute attribute = attribute(name, ALL);
        attribute.addValidation("length", Map.of("min", 3, "max", 255));
        return attribute;
    }

    private static UPAttribute input(String name, Map<String, Object> annotations) {
        UPAttribute attribute = attribute(name, ALL);
        attribute.setAnnotations(annotations);
        return attribute;
    }

    private static UPAttribute options(String name, String validator, List<String> options, Map<String, Object> annotations) {
        UPAttribute attribute = input(name, annotations);
        attribute.addValidation(validator, Map.of("options", options));
        return attribute;
    }

    // The same input types the legacy RegisterWithUserProfileTest#UP_CONFIG_PART_INPUT_TYPES covers, keep the two in sync until that test is migrated.
    private static List<UPAttribute> inputTypeAttributes() {
        return List.of(
                attribute("defaultType", ALL),
                input("placeholderAttribute", Map.of("inputType", "text", "inputTypePlaceholder", "Example.")),
                input("helperTexts", Map.of("inputType", "text", "inputHelperTextBefore", "Example <b>bold text</b> before.", "inputHelperTextAfter", "Example <i>i text</i> after.")),
                input("textWithBasicAttributes", Map.of("inputType", "text", "inputTypeSize", "35", "inputTypeMinlength", "1", "inputTypeMaxlength", "10", "inputTypePattern", ".*")),
                input("html5NumberWithAttributes", Map.of("inputType", "html5-number", "inputTypeMin", "10", "inputTypeMax", "20", "inputTypeStep", 1)),
                input("textareaWithAttributes", Map.of("inputType", "textarea", "inputTypeCols", "35", "inputTypeRows", "7", "inputTypeMaxlength", "10")),
                input("selectWithoutOptions", Map.of("inputType", "select", "inputTypeSize", "5")),
                options("selectWithOptionsWithoutLabels", "options", List.of("opt1", "opt2"), Map.of("inputType", "select")),
                options("multiselectWithOptionsAndSimpleI18nLabels", "options", List.of("totp", "opt2"), Map.of("inputType", "multiselect", "inputOptionLabelsI18nPrefix", "loginTotp")),
                options("multiselectWithOptionsAndLabels", "options", OPTIONS, Map.of("inputType", "multiselect", "inputOptionLabels", OPTION_LABELS)),
                options("selectWithOptionsFromCustomValidatorAndLabels", "dummyOptions", VALIDATOR_OPTIONS, Map.of("inputType", "select", "inputOptionsFromValidation", "dummyOptions", "inputOptionLabels", VALIDATOR_OPTION_LABELS)),
                options("selectRadiobuttons", "options", OPTIONS, Map.of("inputType", "select-radiobuttons", "inputOptionLabels", OPTION_LABELS)),
                options("selectRadiobuttonsWithOptionsFromCustomValidatorAndLabels", "dummyOptions", VALIDATOR_OPTIONS, Map.of("inputType", "select-radiobuttons", "inputOptionsFromValidation", "dummyOptions", "inputOptionLabels", VALIDATOR_OPTION_LABELS)),
                options("multiselectCheckboxes", "options", OPTIONS, Map.of("inputType", "multiselect-checkboxes", "inputOptionLabels", OPTION_LABELS))
        );
    }

    private UserResource getUserResource(String username) {
        return AdminApiUtil.findUserByUsernameId(managedRealm.admin(), username);
    }

    private UserRepresentation getUser(String username) {
        return getUserResource(username).toRepresentation();
    }

    private void registerUserCleanup(String username) {
        registerUserCleanup(getUserResource(username));
    }

    private void registerUserCleanup(UserResource userResource) {
        UserRepresentation original = userResource.toRepresentation();
        managedRealm.cleanup().add(r -> r.users().get(original.getId()).update(original));
    }

    private void prepareUser(String username, String firstName, String lastName, String department) {
        prepareUser(username, user -> {
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.singleAttribute(ATTRIBUTE_DEPARTMENT, department);
        });
    }

    private void prepareUser(String username, Consumer<UserRepresentation> update) {
        setUserProfileConfiguration(attribute(FIRST_NAME, ALL), attribute(LAST_NAME, ALL), attribute(ATTRIBUTE_DEPARTMENT, ALL));

        UserResource userResource = getUserResource(username);
        registerUserCleanup(userResource);

        UserRepresentation user = userResource.toRepresentation();
        update.accept(user);
        userResource.update(user);
    }

    private static void setRequiredActionEnabled(RealmResource realm, RequiredAction action, boolean enabled) {
        RequiredActionProviderRepresentation requiredAction = realm.flows().getRequiredActions().stream()
                .filter(a -> action.name().equals(a.getAlias()))
                .findAny().orElseThrow(() -> new IllegalStateException("Required action not found: " + action.name()));
        requiredAction.setEnabled(enabled);
        realm.flows().updateRequiredAction(requiredAction.getAlias(), requiredAction);
    }

    private void assertFieldTypes() {
        assertAttributes("input#defaultType", Map.of("type", "text"));

        assertAttributes("input#placeholderAttribute", Map.of("type", "text", "placeholder", "Example."));

        assertText("div#form-help-text-before-helperTexts", "Example bold text before.");
        assertText("div#form-help-text-before-helperTexts b", "bold text");
        assertText("div#form-help-text-after-helperTexts", "Example i text after.");
        assertText("div#form-help-text-after-helperTexts i", "i text");

        assertAttributes("input#textWithBasicAttributes", Map.of("type", "text", "size", "35", "minlength", "1", "maxlength", "10", "pattern", ".*"));

        assertAttributes("input#html5NumberWithAttributes", Map.of("type", "number", "min", "10", "max", "20", "step", "1"));

        assertAttributes("textarea#textareaWithAttributes", Map.of("cols", "35", "rows", "7", "maxlength", "10"));

        assertAttributes("select#selectWithoutOptions", Map.of("size", "5"));

        assertSelect("selectWithOptionsWithoutLabels", Map.of("opt1", "opt1", "opt2", "opt2"));

        assertMultiselect("multiselectWithOptionsAndSimpleI18nLabels", Map.of("totp", "Time-based", "opt2", "loginTotp.opt2"));

        assertMultiselect("multiselectWithOptionsAndLabels", Map.of("opt1", "Option 1", "opt2", "Username", "opt3", "opt3"));

        assertSelect("selectWithOptionsFromCustomValidatorAndLabels", Map.of("vopt1", "Option 1", "vopt2", "Username", "vopt3", "vopt3"));

        assertChoices("selectRadiobuttons", "radio", Map.of("opt1", "Option 1", "opt2", "Username", "opt3", "opt3"));

        assertChoices("selectRadiobuttonsWithOptionsFromCustomValidatorAndLabels", "radio", Map.of("vopt1", "Option 1", "vopt2", "Username", "vopt3", "vopt3"));

        assertChoices("multiselectCheckboxes", "checkbox", Map.of("opt1", "Option 1", "opt2", "Username", "opt3", "opt3"));
    }

    // The selector carries the tag on purpose, the attribute must be rendered as that element
    private void assertAttributes(String selector, Map<String, String> expected) {
        WebElement element = driver.findElement(By.cssSelector(selector));
        expected.forEach((name, value) -> assertEquals(value, element.getAttribute(name), name + " of " + selector));
    }

    private void assertText(String selector, String expected) {
        assertEquals(expected, driver.findElement(By.cssSelector(selector)).getText(), selector);
    }

    // a single select is rendered with a leading empty option, a multiselect is not
    private void assertSelect(String id, Map<String, String> optionLabels) {
        Map<String, String> expected = new HashMap<>(optionLabels);
        expected.put("", "");
        assertOptions(id, false, expected);
    }

    private void assertMultiselect(String id, Map<String, String> optionLabels) {
        assertOptions(id, true, optionLabels);
    }

    private void assertOptions(String id, boolean multiple, Map<String, String> optionLabels) {
        Select select = new Select(driver.findElement(By.id(id)));
        assertEquals(multiple, select.isMultiple(), "multiple of " + id);
        Map<String, String> options = select.getOptions().stream().collect(Collectors.toMap(option -> option.getAttribute("value"), WebElement::getText));
        assertEquals(optionLabels, options, "options of " + id);
    }

    private void assertChoices(String id, String type, Map<String, String> optionLabels) {
        optionLabels.forEach((value, label) -> {
            assertAttributes("input#" + id + "-" + value, Map.of("type", type));
            assertEquals(label, verifyProfilePage.getLabelForField(id + "-" + value), "label of " + id + "-" + value);
        });
    }

    public static class VerifyProfileRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.editUsernameAllowed(true);

            realm.users(
                    UserBuilder.create().username("login-test").email("login@test.com").password(PASSWORD),
                    UserBuilder.create().username("login-test2").email("login2@test.com").password(PASSWORD),
                    UserBuilder.create().username("login-test3").email("login3@test.com").lastName("ExistingLast").password(PASSWORD),
                    UserBuilder.create().username("login-test4").email("login4@test.com").lastName("ExistingLast").password(PASSWORD),
                    UserBuilder.create().username("login-test5").email("login5@test.com").name("ExistingFirst", "ExistingLast").password(PASSWORD),
                    UserBuilder.create().username("login-test6").email("login6@test.com").name("ExistingFirst", "ExistingLast").password(PASSWORD),
                    UserBuilder.create().username("login-nomail").name("NoMailFirst", "NoMailLast").password(PASSWORD)
            );


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
