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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
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
import org.keycloak.testsuite.arquillian.annotation.SetDefaultProvider;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.VerifyProfilePage;
import org.keycloak.testsuite.util.ClientScopeBuilder;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.userprofile.UserProfileSpi;
import org.keycloak.userprofile.config.DeclarativeUserProfileProvider;

/**
 * @author Vlastimil Elias <velias@redhat.com>
 */
@EnableFeature(value = Profile.Feature.DECLARATIVE_USER_PROFILE, skipRestart = false)
@SetDefaultProvider(spi = UserProfileSpi.ID, providerId = DeclarativeUserProfileProvider.ID,
        beforeEnableFeature = false,
        onlyUpdateDefault = true)
@AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE)
public class VerifyProfileTest extends AbstractTestRealmKeycloakTest {
    
    private static final String SCOPE_DEPARTMENT = "department";
    private static final String ATTRIBUTE_DEPARTMENT = "department";
    
    public static String PERMISSIONS_ALL = "\"permissions\": {\"view\": [\"admin\", \"user\"], \"edit\": [\"admin\", \"user\"]}";
    public static String PERMISSIONS_ADMIN_ONLY = "\"permissions\": {\"view\": [\"admin\"], \"edit\": [\"admin\"]}";
    public static String PERMISSIONS_ADMIN_EDITABLE = "\"permissions\": {\"view\": [\"admin\", \"user\"], \"edit\": [\"admin\"]}";
    
    public static String VALIDATIONS_LENGTH = "\"validations\": {\"length\": { \"min\": 3, \"max\": 255 }}";

    private static final String CONFIGURATION_FOR_USER_EDIT = "{\"attributes\": [" 
            + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + "}," 
            + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
            + "{\"name\": \"department\"," + PERMISSIONS_ALL + "}" 
            + "]}";
    

    private static String userId;

    private static String user2Id;
    
    private static String user3Id;
    
    private static String user4Id;
    
    private static String user5Id;
    
    private static ClientRepresentation client_scope_default;
    private static ClientRepresentation client_scope_optional;
    
    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
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

        RealmBuilder.edit(testRealm).user(user).user(user2).user(user3).user(user4).user(user5);

        RequiredActionProviderRepresentation action = new RequiredActionProviderRepresentation();
        action.setAlias(UserModel.RequiredAction.VERIFY_PROFILE.name());
        action.setProviderId(UserModel.RequiredAction.VERIFY_PROFILE.name());
        action.setEnabled(true);
        action.setDefaultAction(false);
        action.setPriority(10);

        List<RequiredActionProviderRepresentation> actions = new ArrayList<>();
        actions.add(action);
        testRealm.setRequiredActions(actions);
        
        testRealm.setClientScopes(Collections.singletonList(ClientScopeBuilder.create().name(SCOPE_DEPARTMENT).protocol("openid-connect").build()));
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

        events.expectRequiredAction(EventType.VERIFY_PROFILE).user(userId).assertEvent();

        UserRepresentation user = getUser(userId);
        assertEquals("First", user.getFirstName());
        assertEquals("Last", user.getLastName());
    }

    @Test
    public void testUsernameOnlyIfEditAllowed() {
        RealmRepresentation realm = testRealm().toRepresentation();

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
            realm.setEditUsernameAllowed(false);
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

        events.expectRequiredAction(EventType.VERIFY_PROFILE).user(user2Id).assertEvent();

        UserRepresentation user = getUser(user2Id);
        assertEquals("First", user.getFirstName());
        assertEquals("", user.getLastName());
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

        events.expectRequiredAction(EventType.VERIFY_PROFILE).user(user5Id).assertEvent();

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

        events.expectRequiredAction(EventType.VERIFY_PROFILE).user(user3Id).assertEvent();

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

        events.expectRequiredAction(EventType.VERIFY_PROFILE).user(user4Id).assertEvent();

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

        events.expectRequiredAction(EventType.VERIFY_PROFILE).user(user5Id).assertEvent();
        
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

        events.expectRequiredAction(EventType.VERIFY_PROFILE).user(user5Id).assertEvent();
        
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

        events.expectRequiredAction(EventType.VERIFY_PROFILE).client(client_scope_optional).user(user5Id).assertEvent();
        
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

        events.expectRequiredAction(EventType.VERIFY_PROFILE).client(client_scope_default).user(user5Id).assertEvent();
        
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

        events.expectRequiredAction(EventType.VERIFY_PROFILE).user(user5Id).assertEvent();
        
        UserRepresentation user = getUser(user5Id);
        assertEquals("FirstCC", user.getFirstName());
        assertEquals("LastCC", user.getLastName());
        assertEquals("DepartmentCC", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
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
        return testRealm().users().get(userId).toRepresentation();
    }

    protected void updateUser(String userId, String firstName, String lastName, String department) {
        UserRepresentation ur = testRealm().users().get(userId).toRepresentation();
        ur.setFirstName(firstName);
        ur.setLastName(lastName);
        ur.singleAttribute(ATTRIBUTE_DEPARTMENT, department);
        testRealm().users().get(userId).update(ur);
    }

    protected void setUserProfileConfiguration(String configuration) {
        Response r = testRealm().users().userProfile().update(configuration);
        if (r.getStatus() != 200) {
            Assert.fail("Configuration not set due to error: " + r.readEntity(String.class));
        }
    }

}
