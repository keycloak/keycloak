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

import static org.junit.Assert.assertEquals;
import static org.keycloak.userprofile.DeclarativeUserProfileProvider.REALM_USER_PROFILE_ENABLED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.core.Response;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.util.ClientScopeBuilder;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.KeycloakModelUtils;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 * @author Vlastimil Elias <velias@redhat.com>
 */
@AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE)
public class RegisterWithUserProfileTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected RegisterPage registerPage;

    @Page
    protected VerifyEmailPage verifyEmailPage;

    @Page
    protected AccountUpdateProfilePage accountPage;

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    
    private static final String SCOPE_LAST_NAME = "lastName";

    private static ClientRepresentation client_scope_default;
    private static ClientRepresentation client_scope_optional;

    public static String UP_CONFIG_BASIC_ATTRIBUTES = "{\"name\": \"username\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
            + "{\"name\": \"email\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},";
    
    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        
        testRealm.setClientScopes(new ArrayList<>());
        testRealm.getClientScopes().add(ClientScopeBuilder.create().name(SCOPE_LAST_NAME).protocol("openid-connect").build());
        testRealm.getClientScopes().add(ClientScopeBuilder.create().name(VerifyProfileTest.SCOPE_DEPARTMENT).protocol("openid-connect").build());

        List<String> scopes = new ArrayList<>();
        scopes.add(SCOPE_LAST_NAME);
        scopes.add(VerifyProfileTest.SCOPE_DEPARTMENT);

        client_scope_default = KeycloakModelUtils.createClient(testRealm, "client-a");
        client_scope_default.setDefaultClientScopes(scopes);
        client_scope_default.setRedirectUris(Collections.singletonList("*"));
        client_scope_optional = KeycloakModelUtils.createClient(testRealm, "client-b");
        client_scope_optional.setOptionalClientScopes(scopes);
        client_scope_optional.setRedirectUris(Collections.singletonList("*"));
        if (testRealm.getAttributes() == null) {
            testRealm.setAttributes(new HashMap<>());
        }
        testRealm.getAttributes().put(REALM_USER_PROFILE_ENABLED, Boolean.TRUE.toString());
    }

    @Test
    public void testRegisterUserSuccess_lastNameOptional() {
        setUserProfileConfiguration("{\"attributes\": ["
                + UP_CONFIG_BASIC_ATTRIBUTES
                + "{\"name\": \"firstName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + "}"
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
                + "{\"name\": \"firstName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\":{\"scopes\":[\""+SCOPE_LAST_NAME+"\"]}}"
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
                + "{\"name\": \"firstName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\":{\"scopes\":[\""+SCOPE_LAST_NAME+"\"]}}"
                + "]}");

        oauth.scope(SCOPE_LAST_NAME).clientId(client_scope_optional.getClientId()).openLoginForm();

        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "", "registerUserSuccessLastNameRequiredForScope_requested@email", "registerUserSuccessLastNameRequiredForScope_requested", "password", "password");

        //error reported
        registerPage.assertCurrent();
        assertEquals("Please specify lastName field.", registerPage.getInputAccountErrors().getLastNameError());

        //submit correct form
        registerPage.register("firstName", "lastName", "registerUserSuccessLastNameRequiredForScope_requested@email", "registerUserSuccessLastNameRequiredForScope_requested", "password", "password");

        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }

    @Test
    public void testRegisterUserSuccess_lastNameRequiredForScope_clientDefault() {
        setUserProfileConfiguration("{\"attributes\": ["
                + UP_CONFIG_BASIC_ATTRIBUTES
                + "{\"name\": \"firstName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\":{\"scopes\":[\""+SCOPE_LAST_NAME+"\"]}}"
                + "]}");

        oauth.clientId(client_scope_default.getClientId()).openLoginForm();

        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "", "registerUserSuccessLastNameRequiredForScope_clientDefault@email", "registerUserSuccessLastNameRequiredForScope_clientDefault", "password", "password");

        //error reported
        registerPage.assertCurrent();
        assertEquals("Please specify lastName field.", registerPage.getInputAccountErrors().getLastNameError());

        //submit correct form
        registerPage.register("firstName", "lastName", "registerUserSuccessLastNameRequiredForScope_clientDefault@email", "registerUserSuccessLastNameRequiredForScope_clientDefault", "password", "password");

        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }

    @Test
    public void testRegisterUserSuccess_lastNameLengthValidation() {
        setUserProfileConfiguration("{\"attributes\": ["
                + UP_CONFIG_BASIC_ATTRIBUTES
                + "{\"name\": \"firstName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", " + VerifyProfileTest.VALIDATIONS_LENGTH + "}"
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
                + "{\"name\": \"firstName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", " + VerifyProfileTest.VALIDATIONS_LENGTH + "}"
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
                + "{\"name\": \"firstName\",\"displayName\":\"${firstName}\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", \"displayName\" : \"Department\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\":{}}"
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
    public void testRegisterUserSuccess_requiredReadOnlyAttributeNotRenderedAndNotBlockingRegistration() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\",\"displayName\":\"${firstName}\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", \"displayName\" : \"Department\", " + VerifyProfileTest.PERMISSIONS_ADMIN_EDITABLE + ", \"required\":{}}"
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
                + "{\"name\": \"firstName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\""+VerifyProfileTest.SCOPE_DEPARTMENT+"\"]}}"
                + "]}");

        oauth.scope(VerifyProfileTest.SCOPE_DEPARTMENT).clientId(client_scope_optional.getClientId()).openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        //check required validation works
        registerPage.register("FirstAA", "LastAA", "attributeRequiredAndSelectedByScopeMustBeSet@email", "attributeRequiredAndSelectedByScopeMustBeSet", "password", "password", "");
        registerPage.assertCurrent();

        registerPage.register("FirstAA", "LastAA", "attributeRequiredAndSelectedByScopeMustBeSet@email", "attributeRequiredAndSelectedByScopeMustBeSet", "password", "password", "DepartmentAA");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        UserRepresentation user = getUserByUsername("attributeRequiredAndSelectedByScopeMustBeSet");
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertEquals("DepartmentAA", user.firstAttribute(VerifyProfileTest.ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testRegisterUserSuccess_attributeNotRequiredAndSelectedByScopeCanBeIgnored() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"selector\":{\"scopes\":[\""+VerifyProfileTest.SCOPE_DEPARTMENT+"\"]}}"
                + "]}");

        oauth.scope(VerifyProfileTest.SCOPE_DEPARTMENT).clientId(client_scope_optional.getClientId()).openLoginForm();
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
        assertEquals("", user.firstAttribute(VerifyProfileTest.ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testRegisterUserSuccess_attributeNotRequiredAndSelectedByScopeCanBeSet() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"selector\":{\"scopes\":[\""+VerifyProfileTest.SCOPE_DEPARTMENT+"\"]}}"
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
        assertEquals("Department AA", user.firstAttribute(VerifyProfileTest.ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testRegisterUserSuccess_attributeRequiredButNotSelectedByScopeIsNotRenderedAndNotBlockingRegistration() {

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\""+VerifyProfileTest.SCOPE_DEPARTMENT+"\"]}}"
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
        assertEquals(null, user.firstAttribute(VerifyProfileTest.ATTRIBUTE_DEPARTMENT));
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
        assertEquals(lastName, user.getLastName());
    }
    
    protected UserRepresentation getUser(String userId) {
        return testRealm().users().get(userId).toRepresentation();
    }

    protected UserRepresentation getUserByUsername(String username) {
        List<UserRepresentation> users = testRealm().users().search(username);
        if(users!=null && !users.isEmpty())
            return users.get(0);
        return null;
    }

    private void setUserProfileConfiguration(String configuration) {
        Response r = testRealm().users().userProfile().update(configuration);
        if (r.getStatus() != 200) {
            Assert.fail("Configuration not set due to error: " + r.readEntity(String.class));
        }
    }
}
