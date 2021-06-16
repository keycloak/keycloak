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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.SetDefaultProvider;
import org.keycloak.testsuite.pages.*;
import org.keycloak.testsuite.pages.AppPage.RequestType;

import org.keycloak.testsuite.util.*;
import org.keycloak.userprofile.UserProfileSpi;
import org.keycloak.userprofile.config.DeclarativeUserProfileProvider;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 * @author Vlastimil Elias <velias@redhat.com>
 */
@EnableFeature(value = Profile.Feature.DECLARATIVE_USER_PROFILE, skipRestart = false)
@SetDefaultProvider(spi = UserProfileSpi.ID, providerId = DeclarativeUserProfileProvider.ID,
        beforeEnableFeature = false,
        onlyUpdateDefault = true
)
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
        
        testRealm.setClientScopes(Collections.singletonList(ClientScopeBuilder.create().name(SCOPE_LAST_NAME).protocol("openid-connect").build()));
        client_scope_default = KeycloakModelUtils.createClient(testRealm, "client-a");
        client_scope_default.setDefaultClientScopes(Collections.singletonList(SCOPE_LAST_NAME));
        client_scope_default.setRedirectUris(Collections.singletonList("*"));
        client_scope_optional = KeycloakModelUtils.createClient(testRealm, "client-b");
        client_scope_optional.setOptionalClientScopes(Collections.singletonList(SCOPE_LAST_NAME));
        client_scope_optional.setRedirectUris(Collections.singletonList("*"));

    }

    @Test
    public void registerUserSuccess_lastNameOptional() {
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
    public void registerUserSuccess_lastNameRequiredForScope_notRequested() {
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
    public void registerUserSuccess_lastNameRequiredForScope_requested() {
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
        assertEquals("Please specify this field.", registerPage.getInputAccountErrors().getLastNameError());

        //submit correct form
        registerPage.register("firstName", "lastName", "registerUserSuccessLastNameRequiredForScope_requested@email", "registerUserSuccessLastNameRequiredForScope_requested", "password", "password");

        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }

    @Test
    public void registerUserSuccess_lastNameRequiredForScope_clientDefault() {
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
        assertEquals("Please specify this field.", registerPage.getInputAccountErrors().getLastNameError());

        //submit correct form
        registerPage.register("firstName", "lastName", "registerUserSuccessLastNameRequiredForScope_clientDefault@email", "registerUserSuccessLastNameRequiredForScope_clientDefault", "password", "password");

        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }

    @Test
    public void registerUserSuccess_lastNameLengthValidation() {
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
    public void registerUserInvalidLastNameLength() {
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

    private void setUserProfileConfiguration(String configuration) {
        Response r = testRealm().users().userProfile().update(configuration);
        if (r.getStatus() != 200) {
            Assert.fail("Configuration not set due to error: " + r.readEntity(String.class));
        }
    }
}
