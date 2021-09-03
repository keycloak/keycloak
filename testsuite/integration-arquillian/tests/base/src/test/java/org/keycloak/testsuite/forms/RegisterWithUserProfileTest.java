/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ALL;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ADMIN_EDITABLE;
import static org.keycloak.testsuite.forms.VerifyProfileTest.SCOPE_DEPARTMENT;
import static org.keycloak.testsuite.forms.VerifyProfileTest.VALIDATIONS_LENGTH;
import static org.keycloak.testsuite.forms.VerifyProfileTest.ATTRIBUTE_DEPARTMENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.util.ClientScopeBuilder;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * @author Vlastimil Elias <velias@redhat.com>
 */
@EnableFeature(value = Profile.Feature.DECLARATIVE_USER_PROFILE)
@AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE)
public class RegisterWithUserProfileTest extends RegisterTest {

    private static final String SCOPE_LAST_NAME = "lastName";

    private static ClientRepresentation client_scope_default;
    private static ClientRepresentation client_scope_optional;

    public static String UP_CONFIG_BASIC_ATTRIBUTES = "{\"name\": \"username\"," + PERMISSIONS_ALL + ", \"required\": {}},"
            + "{\"name\": \"email\"," + PERMISSIONS_ALL + ", \"required\": {}},";

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

        super.configureTestRealm(testRealm);

        VerifyProfileTest.enableDynamicUserProfile(testRealm);

        testRealm.setClientScopes(new ArrayList<>());
        testRealm.getClientScopes().add(ClientScopeBuilder.create().name(SCOPE_LAST_NAME).protocol("openid-connect").build());
        testRealm.getClientScopes().add(ClientScopeBuilder.create().name(SCOPE_DEPARTMENT).protocol("openid-connect").build());

        List<String> scopes = new ArrayList<>();
        scopes.add(SCOPE_LAST_NAME);
        scopes.add(SCOPE_DEPARTMENT);

        client_scope_default = KeycloakModelUtils.createClient(testRealm, "client-a");
        client_scope_default.setDefaultClientScopes(scopes);
        client_scope_default.setRedirectUris(Collections.singletonList("*"));
        client_scope_optional = KeycloakModelUtils.createClient(testRealm, "client-b");
        client_scope_optional.setOptionalClientScopes(scopes);
        client_scope_optional.setRedirectUris(Collections.singletonList("*"));
    }

    @Before
    public void beforeTest() {
        VerifyProfileTest.setUserProfileConfiguration(testRealm(),null);
    }

    @Test
    public void testRegisterUserSuccess_lastNameOptional() {
        setUserProfileConfiguration("{\"attributes\": ["
                + UP_CONFIG_BASIC_ATTRIBUTES
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "}"
                + "]}");

        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "", "registerUserSuccessLastNameOptional@email", "registerUserSuccessLastNameOptional", "password", "password");

        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        String userId = events.expectRegister("registerUserSuccessLastNameOptional", "registerUserSuccessLastNameOptional@email").assertEvent().getUserId();
        assertUserRegistered(userId, "registerUserSuccessLastNameOptional", "registerusersuccesslastnameoptional@email", "firstName", "");
    }

    @Test
    public void testRegisterUserSuccess_lastNameRequiredForScope_notRequested() {
        setUserProfileConfiguration("{\"attributes\": ["
                + UP_CONFIG_BASIC_ATTRIBUTES
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\":{\"scopes\":[\""+SCOPE_LAST_NAME+"\"]}}"
                + "]}");

        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "", "registerUserSuccessLastNameRequiredForScope_notRequested@email", "registerUserSuccessLastNameRequiredForScope_notRequested", "password", "password");

        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        String userId = events.expectRegister("registerUserSuccessLastNameRequiredForScope_notRequested", "registerUserSuccessLastNameRequiredForScope_notRequested@email").assertEvent().getUserId();
        assertUserRegistered(userId, "registerUserSuccessLastNameRequiredForScope_notRequested", "registerusersuccesslastnamerequiredforscope_notrequested@email", "firstName", "");
    }

    @Test
    public void testRegisterUserSuccess_lastNameRequiredForScope_requested() {
        setUserProfileConfiguration("{\"attributes\": ["
                + UP_CONFIG_BASIC_ATTRIBUTES
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\":{\"scopes\":[\""+SCOPE_LAST_NAME+"\"]}}"
                + "]}");

        oauth.scope(SCOPE_LAST_NAME).clientId(client_scope_optional.getClientId()).openLoginForm();

        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "", "registerUserSuccessLastNameRequiredForScope_requested@email", "registerUserSuccessLastNameRequiredForScope_requested", "password", "password");

        //error reported
        registerPage.assertCurrent();
        assertEquals("Please specify this field.", registerPage.getInputAccountErrors().getLastNameError());

        //submit correct form
        registerPage.register("firstName", "lastName", "registerUserSuccessLastNameRequiredForScope_requested@email", "registerUserSuccessLastNameRequiredForScope_requested", "password", "password");

        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }

    @Test
    public void testRegisterUserSuccess_lastNameRequiredForScope_clientDefault() {
        setUserProfileConfiguration("{\"attributes\": ["
                + UP_CONFIG_BASIC_ATTRIBUTES
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\":{\"scopes\":[\""+SCOPE_LAST_NAME+"\"]}}"
                + "]}");

        oauth.clientId(client_scope_default.getClientId()).openLoginForm();

        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "", "registerUserSuccessLastNameRequiredForScope_clientDefault@email", "registerUserSuccessLastNameRequiredForScope_clientDefault", "password", "password");

        //error reported
        registerPage.assertCurrent();
        assertEquals("Please specify this field.", registerPage.getInputAccountErrors().getLastNameError());

        //submit correct form
        registerPage.register("firstName", "lastName", "registerUserSuccessLastNameRequiredForScope_clientDefault@email", "registerUserSuccessLastNameRequiredForScope_clientDefault", "password", "password");

        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }

    @Test
    public void testRegisterUserSuccess_lastNameLengthValidation() {
        setUserProfileConfiguration("{\"attributes\": ["
                + UP_CONFIG_BASIC_ATTRIBUTES
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", " + VALIDATIONS_LENGTH + "}"
                + "]}");

        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "last", "registerUserSuccessLastNameLengthValidation@email", "registerUserSuccessLastNameLengthValidation", "password", "password");

        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        String userId = events.expectRegister("registerUserSuccessLastNameLengthValidation", "registerUserSuccessLastNameLengthValidation@email").assertEvent().getUserId();
        assertUserRegistered(userId, "registerUserSuccessLastNameLengthValidation", "registerusersuccesslastnamelengthvalidation@email", "firstName", "last");
    }

    @Test
    public void testRegisterUserInvalidLastNameLength() {
        setUserProfileConfiguration("{\"attributes\": ["
                + UP_CONFIG_BASIC_ATTRIBUTES
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", " + VALIDATIONS_LENGTH + "}"
                + "]}");

        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "L", "registerUserInvalidLastNameLength@email", "registerUserInvalidLastNameLength", "password", "password");

        registerPage.assertCurrent();
        assertEquals("Length must be between 3 and 255.", registerPage.getInputAccountErrors().getLastNameError());

        events.expectRegister("registeruserinvalidlastnamelength", "registerUserInvalidLastNameLength@email")
                .error("invalid_registration").assertEvent();
    }

    @Test
    public void testAttributeDisplayName() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\",\"displayName\":\"${firstName}\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", \"displayName\" : \"Department\", " + PERMISSIONS_ALL + ", \"required\":{}}"
                + "]}");

        loginPage.open();
        loginPage.clickRegister();

        registerPage.assertCurrent();

        //assert field names
        // i18n replaced
        Assert.assertEquals("First name",registerPage.getLabelForField("firstName"));
        // attribute name used if no display name set
        Assert.assertEquals("lastName",registerPage.getLabelForField("lastName"));
        // direct value in display name
        Assert.assertEquals("Department",registerPage.getLabelForField("department"));
    }

    @Test
    public void testAttributeGuiOrder() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\":{}},"
                + "{\"name\": \"username\", " + VerifyProfileTest.PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"email\", " + VerifyProfileTest.PERMISSIONS_ALL + "}"
                + "]}");

        loginPage.open();
        loginPage.clickRegister();

        registerPage.assertCurrent();

        //assert fields location in form
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#kc-register-form > div:nth-child(1) > div:nth-child(2) > input#lastName")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#kc-register-form > div:nth-child(2) > div:nth-child(2) > input#department")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#kc-register-form > div:nth-child(3) > div:nth-child(2) > input#username")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#kc-register-form > div:nth-child(4) > div:nth-child(2) > input#password")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#kc-register-form > div:nth-child(5) > div:nth-child(2) > input#password-confirm")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#kc-register-form > div:nth-child(6) > div:nth-child(2) > input#firstName")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#kc-register-form > div:nth-child(7) > div:nth-child(2) > input#email")
                ).isDisplayed()
        );
    }

    public static final String UP_CONFIG_PART_INPUT_TYPES = "{\"name\": \"defaultType\"," + VerifyProfileTest.PERMISSIONS_ALL + "},"
            + "{\"name\": \"placeholderAttribute\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"annotations\":{\"inputType\":\"text\",\"inputTypePlaceholder\":\"Example.\"}},"
            + "{\"name\": \"helperTexts\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"annotations\":{\"inputType\":\"text\",\"inputHelperTextBefore\":\"Example <b>bold text</b> before.\",\"inputHelperTextAfter\":\"Example <i>i text</i> after.\"}},"
            + "{\"name\": \"textWithBasicAttributes\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"annotations\":{\"inputType\":\"text\",\"inputTypeSize\":\"35\",\"inputTypeMinlength\":\"1\",\"inputTypeMaxlength\":\"10\",\"inputTypePattern\":\".*\"}},"
            + "{\"name\": \"html5NumberWithAttributes\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"annotations\":{\"inputType\":\"html5-number\",\"inputTypeMin\":\"10\",\"inputTypeMax\":\"20\",\"inputTypeStep\":1}},"
            + "{\"name\": \"textareaWithAttributes\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"annotations\":{\"inputType\":\"textarea\",\"inputTypeCols\":\"35\",\"inputTypeRows\":\"7\",\"inputTypeMaxlength\":\"10\"}},"
            + "{\"name\": \"selectWithoutOptions\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"annotations\":{\"inputType\":\"select\",\"inputTypeSize\":\"5\"}},"
            + "{\"name\": \"selectWithOptionsWithoutLabels\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"validations\":{\"options\":{\"options\":[ \"opt1\",\"opt2\"]}}, \"annotations\":{\"inputType\":\"select\"}},"
            + "{\"name\": \"multiselectWithOptionsAndSimpleI18nLabels\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"validations\":{\"options\":{ \"options\":[\"totp\",\"opt2\"]}}, \"annotations\":{\"inputType\":\"multiselect\",\"inputOptionLabelsI18nPrefix\": \"loginTotp\"}},"
            + "{\"name\": \"multiselectWithOptionsAndLabels\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"validations\":{\"options\":{ \"options\":[\"opt1\",\"opt2\",\"opt3\"]}}, \"annotations\":{\"inputType\":\"multiselect\",\"inputOptionLabels\":{\"opt1\": \"Option 1\",\"opt2\":\"${username}\"}}},"
            + "{\"name\": \"selectWithOptionsFromCustomValidatorAndLabels\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"validations\":{\"dummyOptions\":{\"options\" : [\"vopt1\",\"vopt2\",\"vopt3\"]}} ,\"annotations\":{\"inputType\":\"select\",\"inputOptionsFromValidation\":\"dummyOptions\",\"inputOptionLabels\":{\"vopt1\": \"Option 1\",\"vopt2\":\"${username}\"}}},"
            + "{\"name\": \"selectRadiobuttons\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"validations\" : {\"options\" : {\"options\":[\"opt1\",\"opt2\",\"opt3\"]}}, \"annotations\":{\"inputType\":\"select-radiobuttons\",\"inputOptionLabels\":{\"opt1\": \"Option 1\",\"opt2\":\"${username}\"}}},"
            + "{\"name\": \"selectRadiobuttonsWithOptionsFromCustomValidatorAndLabels\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"validations\" : {\"dummyOptions\" : {\"options\" : [\"vopt1\",\"vopt2\",\"vopt3\"]}} ,\"annotations\":{\"inputType\":\"select-radiobuttons\",\"inputOptionsFromValidation\":\"dummyOptions\",\"inputOptionLabels\":{\"vopt1\": \"Option 1\",\"vopt2\":\"${username}\"}}},"
            + "{\"name\": \"multiselectCheckboxes\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"validations\": {\"options\":{\"options\":[\"opt1\",\"opt2\",\"opt3\"]}}, \"annotations\":{\"inputType\":\"multiselect-checkboxes\",\"inputOptionLabels\":{\"opt1\": \"Option 1\",\"opt2\":\"${username}\"}}}";

    @Test
    public void testAttributeInputTypes() {

        setUserProfileConfiguration("{\"attributes\": [" + UP_CONFIG_PART_INPUT_TYPES + "]}");

        loginPage.open();
        loginPage.clickRegister();

        registerPage.assertCurrent();

        assertFieldTypes(driver);
    }

    public static void assertFieldTypes(WebDriver driver) {
        Assert.assertEquals("text", driver.findElement(By.cssSelector("input#defaultType")).getAttribute("type"));

        Assert.assertEquals("text", driver.findElement(By.cssSelector("input#placeholderAttribute")).getAttribute("type"));
        Assert.assertEquals("Example.", driver.findElement(By.cssSelector("input#placeholderAttribute")).getAttribute("placeholder"));

        Assert.assertEquals("Example bold text before.", driver.findElement(By.cssSelector("div#form-help-text-before-helperTexts")).getText());
        Assert.assertEquals("bold text", driver.findElement(By.cssSelector("div#form-help-text-before-helperTexts b")).getText());
        Assert.assertEquals("Example i text after.", driver.findElement(By.cssSelector("div#form-help-text-after-helperTexts")).getText());
        Assert.assertEquals("i text", driver.findElement(By.cssSelector("div#form-help-text-after-helperTexts i")).getText());

        Assert.assertEquals("text", driver.findElement(By.cssSelector("input#textWithBasicAttributes")).getAttribute("type"));
        Assert.assertEquals("35", driver.findElement(By.cssSelector("input#textWithBasicAttributes")).getAttribute("size"));
        Assert.assertEquals("1", driver.findElement(By.cssSelector("input#textWithBasicAttributes")).getAttribute("minlength"));
        Assert.assertEquals("10", driver.findElement(By.cssSelector("input#textWithBasicAttributes")).getAttribute("maxlength"));
        Assert.assertEquals(".*", driver.findElement(By.cssSelector("input#textWithBasicAttributes")).getAttribute("pattern"));

        Assert.assertEquals("number", driver.findElement(By.cssSelector("input#html5NumberWithAttributes")).getAttribute("type"));
        Assert.assertEquals("10", driver.findElement(By.cssSelector("input#html5NumberWithAttributes")).getAttribute("min"));
        Assert.assertEquals("20", driver.findElement(By.cssSelector("input#html5NumberWithAttributes")).getAttribute("max"));
        Assert.assertEquals("1", driver.findElement(By.cssSelector("input#html5NumberWithAttributes")).getAttribute("step"));

        Assert.assertEquals("35", driver.findElement(By.cssSelector("textarea#textareaWithAttributes")).getAttribute("cols"));
        Assert.assertEquals("7", driver.findElement(By.cssSelector("textarea#textareaWithAttributes")).getAttribute("rows"));
        Assert.assertEquals("10", driver.findElement(By.cssSelector("textarea#textareaWithAttributes")).getAttribute("maxlength"));

        Assert.assertEquals("5", driver.findElement(By.cssSelector("select#selectWithoutOptions")).getAttribute("size"));

        Assert.assertEquals(null, driver.findElement(By.cssSelector("select#selectWithOptionsWithoutLabels")).getAttribute("multiple"));
        Assert.assertEquals("opt1", driver.findElement(By.cssSelector("select#selectWithOptionsWithoutLabels option[value=opt1]")).getText());
        Assert.assertEquals("opt2", driver.findElement(By.cssSelector("select#selectWithOptionsWithoutLabels option[value=opt2]")).getText());
        Assert.assertEquals("default empty option is missing in select","", driver.findElement(By.cssSelector("select#selectWithOptionsWithoutLabels option[value='']")).getText());

        Assert.assertEquals("true", driver.findElement(By.cssSelector("select#multiselectWithOptionsAndSimpleI18nLabels")).getAttribute("multiple"));
        Assert.assertEquals("Time-based", driver.findElement(By.cssSelector("select#multiselectWithOptionsAndSimpleI18nLabels option[value=totp]")).getText());
        Assert.assertEquals("loginTotp.opt2", driver.findElement(By.cssSelector("select#multiselectWithOptionsAndSimpleI18nLabels option[value=opt2]")).getText());

        Assert.assertEquals("true", driver.findElement(By.cssSelector("select#multiselectWithOptionsAndLabels")).getAttribute("multiple"));
        Assert.assertEquals("Option 1", driver.findElement(By.cssSelector("select#multiselectWithOptionsAndLabels option[value=opt1]")).getText());
        Assert.assertEquals("Username", driver.findElement(By.cssSelector("select#multiselectWithOptionsAndLabels option[value=opt2]")).getText());
        Assert.assertEquals("opt3", driver.findElement(By.cssSelector("select#multiselectWithOptionsAndLabels option[value=opt3]")).getText());

        Assert.assertEquals(null, driver.findElement(By.cssSelector("select#selectWithOptionsFromCustomValidatorAndLabels")).getAttribute("multiple"));
        Assert.assertEquals("Option 1", driver.findElement(By.cssSelector("select#selectWithOptionsFromCustomValidatorAndLabels option[value=vopt1]")).getText());
        Assert.assertEquals("Username", driver.findElement(By.cssSelector("select#selectWithOptionsFromCustomValidatorAndLabels option[value=vopt2]")).getText());
        Assert.assertEquals("vopt3", driver.findElement(By.cssSelector("select#selectWithOptionsFromCustomValidatorAndLabels option[value=vopt3]")).getText());

        Assert.assertEquals("radio", driver.findElement(By.cssSelector("input#selectRadiobuttons-opt1")).getAttribute("type"));
        Assert.assertEquals("Option 1", driver.findElement(By.cssSelector("label[for=selectRadiobuttons-opt1]")).getText());
        Assert.assertEquals("radio", driver.findElement(By.cssSelector("input#selectRadiobuttons-opt2")).getAttribute("type"));
        Assert.assertEquals("Username", driver.findElement(By.cssSelector("label[for=selectRadiobuttons-opt2]")).getText());
        Assert.assertEquals("radio", driver.findElement(By.cssSelector("input#selectRadiobuttons-opt3")).getAttribute("type"));
        Assert.assertEquals("opt3", driver.findElement(By.cssSelector("label[for=selectRadiobuttons-opt3]")).getText());

        Assert.assertEquals("radio", driver.findElement(By.cssSelector("input#selectRadiobuttonsWithOptionsFromCustomValidatorAndLabels-vopt1")).getAttribute("type"));
        Assert.assertEquals("Option 1", driver.findElement(By.cssSelector("label[for=selectRadiobuttonsWithOptionsFromCustomValidatorAndLabels-vopt1]")).getText());
        Assert.assertEquals("radio", driver.findElement(By.cssSelector("input#selectRadiobuttonsWithOptionsFromCustomValidatorAndLabels-vopt2")).getAttribute("type"));
        Assert.assertEquals("Username", driver.findElement(By.cssSelector("label[for=selectRadiobuttonsWithOptionsFromCustomValidatorAndLabels-vopt2]")).getText());
        Assert.assertEquals("radio", driver.findElement(By.cssSelector("input#selectRadiobuttonsWithOptionsFromCustomValidatorAndLabels-vopt3")).getAttribute("type"));
        Assert.assertEquals("vopt3", driver.findElement(By.cssSelector("label[for=selectRadiobuttonsWithOptionsFromCustomValidatorAndLabels-vopt3]")).getText());

        Assert.assertEquals("checkbox", driver.findElement(By.cssSelector("input#multiselectCheckboxes-opt1")).getAttribute("type"));
        Assert.assertEquals("Option 1", driver.findElement(By.cssSelector("label[for=multiselectCheckboxes-opt1]")).getText());
        Assert.assertEquals("checkbox", driver.findElement(By.cssSelector("input#multiselectCheckboxes-opt2")).getAttribute("type"));
        Assert.assertEquals("Username", driver.findElement(By.cssSelector("label[for=multiselectCheckboxes-opt2]")).getText());
        Assert.assertEquals("checkbox", driver.findElement(By.cssSelector("input#multiselectCheckboxes-opt3")).getAttribute("type"));
        Assert.assertEquals("opt3", driver.findElement(By.cssSelector("label[for=multiselectCheckboxes-opt3]")).getText());
    }

    @Test
    public void testAttributeGrouping() {

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
        loginPage.clickRegister();

        registerPage.assertCurrent();
        String htmlFormId="kc-register-form";

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
                        By.cssSelector("form#kc-register-form > div:nth-child(3) > div:nth-child(2) > input#password")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(4) > div:nth-child(2) > input#password-confirm")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(5) > div:nth-child(2) > input#firstName")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(6) > div:nth-child(1) > label#header-company")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(6) > div:nth-child(2) > label#description-company")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(7) > div:nth-child(2) > input#department")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(8) > div:nth-child(1) > label#header-contact")
                ).isDisplayed()
        );
        Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(9) > div:nth-child(2) > input#email")
                ).isDisplayed()
        );
    }

    @Test
    public void testRegisterUserSuccess_requiredReadOnlyAttributeNotRenderedAndNotBlockingRegistration() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\",\"displayName\":\"${firstName}\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", \"displayName\" : \"Department\", " + PERMISSIONS_ADMIN_EDITABLE + ", \"required\":{}}"
                + "]}");

        loginPage.open();
        loginPage.clickRegister();

        registerPage.assertCurrent();

        Assert.assertFalse(registerPage.isDepartmentPresent());


        registerPage.register("FirstName", "LastName", "requiredReadOnlyAttributeNotRenderedAndNotBlockingRegistration@email", "requiredReadOnlyAttributeNotRenderedAndNotBlockingRegistration", "password", "password");

        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }


    @Test
    public void testRegisterUserSuccess_attributeRequiredAndSelectedByScopeMustBeSet() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\""+SCOPE_DEPARTMENT+"\"]}}"
                + "]}");

        oauth.scope(SCOPE_DEPARTMENT).clientId(client_scope_optional.getClientId()).openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        //check required validation works
        registerPage.register("FirstAA", "LastAA", "attributeRequiredAndSelectedByScopeMustBeSet@email", "attributeRequiredAndSelectedByScopeMustBeSet", "password", "password", "");
        registerPage.assertCurrent();

        registerPage.register("FirstAA", "LastAA", "attributeRequiredAndSelectedByScopeMustBeSet@email", "attributeRequiredAndSelectedByScopeMustBeSet", "password", "password", "DepartmentAA");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        UserRepresentation user = VerifyProfileTest.getUserByUsername(testRealm(),"attributeRequiredAndSelectedByScopeMustBeSet");
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertEquals("DepartmentAA", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testRegisterUserSuccess_attributeNotRequiredAndSelectedByScopeCanBeIgnored() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"selector\":{\"scopes\":[\""+SCOPE_DEPARTMENT+"\"]}}"
                + "]}");

        oauth.scope(SCOPE_DEPARTMENT).clientId(client_scope_optional.getClientId()).openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        Assert.assertTrue(registerPage.isDepartmentPresent());
        registerPage.register("FirstAA", "LastAA", "attributeNotRequiredAndSelectedByScopeCanBeIgnored@email", "attributeNotRequiredAndSelectedByScopeCanBeIgnored", "password", "password", null);

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        String userId = events.expectRegister("attributeNotRequiredAndSelectedByScopeCanBeIgnored", "attributeNotRequiredAndSelectedByScopeCanBeIgnored@email",client_scope_optional.getClientId()).assertEvent().getUserId();
        UserRepresentation user = getUser(userId);
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertThat(StringUtils.isEmpty(user.firstAttribute(ATTRIBUTE_DEPARTMENT)), is(true));
    }

    @Test
    public void testRegisterUserSuccess_attributeNotRequiredAndSelectedByScopeCanBeSet() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"selector\":{\"scopes\":[\""+SCOPE_DEPARTMENT+"\"]}}"
                + "]}");

        oauth.clientId(client_scope_default.getClientId()).openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        Assert.assertTrue(registerPage.isDepartmentPresent());
        registerPage.register("FirstAA", "LastAA", "attributeNotRequiredAndSelectedByScopeCanBeSet@email", "attributeNotRequiredAndSelectedByScopeCanBeSet", "password", "password", "Department AA");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        String userId = events.expectRegister("attributeNotRequiredAndSelectedByScopeCanBeSet", "attributeNotRequiredAndSelectedByScopeCanBeSet@email",client_scope_default.getClientId()).assertEvent().getUserId();
        UserRepresentation user = getUser(userId);
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertEquals("Department AA", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testRegisterUserSuccess_attributeRequiredButNotSelectedByScopeIsNotRenderedAndNotBlockingRegistration() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\""+SCOPE_DEPARTMENT+"\"]}}"
                + "]}");

        oauth.clientId(client_scope_optional.getClientId()).openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        Assert.assertFalse(registerPage.isDepartmentPresent());
        registerPage.register("FirstAA", "LastAA", "attributeRequiredButNotSelectedByScopeIsNotRendered@email", "attributeRequiredButNotSelectedByScopeIsNotRendered", "password", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        String userId = events.expectRegister("attributeRequiredButNotSelectedByScopeIsNotRendered", "attributeRequiredButNotSelectedByScopeIsNotRendered@email",client_scope_optional.getClientId()).assertEvent().getUserId();
        UserRepresentation user = getUser(userId);
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertEquals(null, user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }


    private void assertUserRegistered(String userId, String username, String email, String firstName, String lastName) {
        events.expectLogin().detail("username", username.toLowerCase()).user(userId).assertEvent();

        UserRepresentation user = getUser(userId);
        Assert.assertNotNull(user);
        Assert.assertNotNull(user.getCreatedTimestamp());
        // test that timestamp is current with 10s tollerance
        Assert.assertTrue((System.currentTimeMillis() - user.getCreatedTimestamp()) < 10000);
        // test user info is set from form
        assertEquals(username.toLowerCase(), user.getUsername());
        assertEquals(email.toLowerCase(), user.getEmail());
        assertEquals(firstName, user.getFirstName());

        if (StringUtils.isEmpty(lastName)) {
            assertThat(StringUtils.isEmpty(user.getLastName()), is(true));
        } else {
            assertThat(user.getLastName(), is(lastName));
        }
    }

    protected void setUserProfileConfiguration(String configuration) {
        VerifyProfileTest.setUserProfileConfiguration(testRealm(), configuration);
    }
}