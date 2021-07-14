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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ALL;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ADMIN_EDITABLE;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ADMIN_ONLY;
import static org.keycloak.testsuite.forms.VerifyProfileTest.SCOPE_DEPARTMENT;
import static org.keycloak.testsuite.forms.VerifyProfileTest.VALIDATIONS_LENGTH;
import static org.keycloak.testsuite.forms.VerifyProfileTest.ATTRIBUTE_DEPARTMENT;
import static org.keycloak.testsuite.forms.VerifyProfileTest.CONFIGURATION_FOR_USER_EDIT;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.forms.VerifyProfileTest;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.util.ClientScopeBuilder;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.openqa.selenium.By;

/**
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class RequiredActionUpdateProfileWithUserProfileTest extends RequiredActionUpdateProfileTest {
    
    protected static final String PASSWORD = "password";
    protected static final String USERNAME1 = "test-user@localhost";
    
    private static ClientRepresentation client_scope_default;
    private static ClientRepresentation client_scope_optional;

    @Override
    protected boolean isDynamicForm() {
        return true;
    }
    
    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);
        
        VerifyProfileTest.enableDynamicUserProfile(testRealm);
              
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
        VerifyProfileTest.setUserProfileConfiguration(testRealm(),null);
        super.beforeTest();
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
        Assert.assertEquals("First name",updateProfilePage.getLabelForField("firstName"));
        // attribute name used if no display name set
        Assert.assertEquals("lastName",updateProfilePage.getLabelForField("lastName"));
        // direct value in display name
        Assert.assertEquals("Department",updateProfilePage.getLabelForField("department"));
        
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
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();
        
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

        updateProfilePage.update("New first", "", "new@email.com", USERNAME1);

        events.expectRequiredAction(EventType.UPDATE_PROFILE).detail(Details.PREVIOUS_FIRST_NAME, "Tom").detail(Details.UPDATED_FIRST_NAME, "New first")
                .detail(Details.PREVIOUS_LAST_NAME, "Brady")
                .detail(Details.PREVIOUS_EMAIL, USERNAME1).detail(Details.UPDATED_EMAIL, "new@email.com")
                .assertEvent();
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().assertEvent();

        // assert user is really updated in persistent store
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, USERNAME1);
        Assert.assertEquals("New first", user.getFirstName());
        Assert.assertEquals("", user.getLastName());
        Assert.assertEquals("new@email.com", user.getEmail());
        Assert.assertEquals(USERNAME1, user.getUsername());
    }
    
    @Test
    public void testCustomValidationLastName() {
        
        setUserProfileConfiguration(CONFIGURATION_FOR_USER_EDIT);
        updateUserByUsername(USERNAME1, "ExistingFirst", "La", "Department");
        
        setUserProfileConfiguration("{\"attributes\": [" 
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}}," 
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL +","+VALIDATIONS_LENGTH + "}," 
                + "{\"name\": \"department\"," + PERMISSIONS_ADMIN_ONLY + "}"
                + "]}");

        loginPage.open();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();
        //submit with error
        updateProfilePage.update("First", "L", USERNAME1, USERNAME1);

        updateProfilePage.assertCurrent();
        //submit OK
        updateProfilePage.update("First", "Last", USERNAME1, USERNAME1);

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        updateProfilePage.update("First", "Last", USERNAME1, USERNAME1);

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        updateProfilePage.update("First", "Last", USERNAME1, USERNAME1);

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        Assert.assertFalse("'department' field is visible" , updateProfilePage.isDepartmentPresent());
        
        //update of the other attributes must be successful in this case
        updateProfilePage.update("First", "Last", USERNAME1, USERNAME1);

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
        updateProfilePage.updateWithDepartment("FirstCC", "LastCC", "", USERNAME1, USERNAME1);
        updateProfilePage.assertCurrent();
        
        //submit OK
        updateProfilePage.updateWithDepartment("FirstCC", "LastCC", "DepartmentCC", USERNAME1, USERNAME1);

        
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{\"scopes\":[\""+SCOPE_DEPARTMENT+"\"]}}" 
                + "]}");

        oauth.scope(SCOPE_DEPARTMENT).clientId(client_scope_optional.getClientId()).openLoginForm();
        
        loginPage.assertCurrent();
        loginPage.login(USERNAME1, PASSWORD);
        
        updateProfilePage.assertCurrent();
        
        //submit with error
        updateProfilePage.updateWithDepartment("FirstCC", "LastCC", "", USERNAME1, USERNAME1);
        updateProfilePage.assertCurrent();
        
        //submit OK
        updateProfilePage.updateWithDepartment("FirstCC", "LastCC", "DepartmentCC", USERNAME1, USERNAME1);
        
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{\"scopes\":[\""+SCOPE_DEPARTMENT+"\"]}}" 
                + "]}");

        oauth.clientId(client_scope_default.getClientId()).openLoginForm();
        
        loginPage.assertCurrent();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();

        //submit with error
        updateProfilePage.updateWithDepartment("FirstCC", "LastCC", "", USERNAME1, USERNAME1);
        updateProfilePage.assertCurrent();
        
        //submit OK
        updateProfilePage.updateWithDepartment("FirstCC", "LastCC", "DepartmentCC", USERNAME1, USERNAME1);
        
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\""+SCOPE_DEPARTMENT+"\"]}}" 
                + "]}");

        oauth.scope(SCOPE_DEPARTMENT).clientId(client_scope_optional.getClientId()).openLoginForm();
        
        loginPage.assertCurrent();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();

        //submit with error
        updateProfilePage.updateWithDepartment("FirstCC", "LastCC", "", USERNAME1, USERNAME1);
        updateProfilePage.assertCurrent();
        
        //submit OK
        updateProfilePage.updateWithDepartment("FirstCC", "LastCC", "DepartmentCC", USERNAME1, USERNAME1);
        
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"selector\":{\"scopes\":[\""+SCOPE_DEPARTMENT+"\"]}}" 
                + "]}");

        oauth.scope(SCOPE_DEPARTMENT).clientId(client_scope_optional.getClientId()).openLoginForm();
        
        loginPage.assertCurrent();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();
        
        Assert.assertTrue(updateProfilePage.isDepartmentPresent());
        updateProfilePage.updateWithDepartment("FirstCC", "LastCC", "DepartmentCC", USERNAME1, USERNAME1);
        
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

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
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\""+SCOPE_DEPARTMENT+"\"]}}" 
                + "]}");

        oauth.clientId(client_scope_optional.getClientId()).openLoginForm();
        
        loginPage.assertCurrent();
        loginPage.login(USERNAME1, PASSWORD);

        updateProfilePage.assertCurrent();
        
        Assert.assertFalse(updateProfilePage.isDepartmentPresent());
        updateProfilePage.update("FirstCC", "LastCC", USERNAME1, USERNAME1);
        
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        UserRepresentation user = getUserByUsername(USERNAME1);
        assertEquals("FirstCC", user.getFirstName());
        assertEquals("LastCC", user.getLastName());
    }

    @Test
    public void updateProfileWithoutRemoveCustomAttributes() {
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"custom\"," + PERMISSIONS_ALL + "}"
                + "]}");
        super.updateProfileWithoutRemoveCustomAttributes();
    }

    protected void setUserProfileConfiguration(String configuration) {
        VerifyProfileTest.setUserProfileConfiguration(testRealm(), configuration);
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
