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
package org.keycloak.testsuite.account;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.representations.account.SessionRepresentation;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.account.AccountCredentialResource;
import org.keycloak.services.resources.account.AccountCredentialResource.PasswordUpdate;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.TokenUtil;
import org.keycloak.testsuite.util.UserBuilder;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;
import static org.keycloak.common.Profile.Feature.ACCOUNT_API;
import static org.keycloak.testsuite.ProfileAssume.assumeFeatureEnabled;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountRestServiceTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public TokenUtil tokenUtil = new TokenUtil();

    @Rule
    public AssertEvents events = new AssertEvents(this);

    private CloseableHttpClient client;

    @Before
    public void before() {
        client = HttpClientBuilder.create().build();
        try {
            checkIfFeatureWorks(false);
            Response response = testingClient.testing().enableFeature(ACCOUNT_API.toString());
            assertEquals(200, response.getStatus());

            assumeFeatureEnabled(ACCOUNT_API);
            checkIfFeatureWorks(true);
        } catch (Exception e) {
            disableFeature();
            throw e;
        }
    }

    @After
    public void after() {
        try {
            disableFeature();
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void disableFeature() {
        Response response = testingClient.testing().disableFeature(ACCOUNT_API.toString());
        assertEquals(200, response.getStatus());
        checkIfFeatureWorks(false);
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.getUsers().add(UserBuilder.create().username("no-account-access").password("password").build());
        testRealm.getUsers().add(UserBuilder.create().username("view-account-access").role("account", "view-profile").password("password").build());
    }

    @Test
    public void testGetProfile() throws IOException {
        UserRepresentation user = SimpleHttp.doGet(getAccountUrl(null), client).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
        assertEquals("Tom", user.getFirstName());
        assertEquals("Brady", user.getLastName());
        assertEquals("test-user@localhost", user.getEmail());
        assertFalse(user.isEmailVerified());
        assertTrue(user.getAttributes().isEmpty());
    }

    @Test
    public void testUpdateProfile() throws IOException {
        UserRepresentation user = SimpleHttp.doGet(getAccountUrl(null), client).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
        String originalFirstName = user.getFirstName();
        String originalLastName = user.getLastName();
        String originalEmail = user.getEmail();
        Map<String, List<String>> originalAttributes = new HashMap<>(user.getAttributes());

        try {
            user.setFirstName("Homer");
            user.setLastName("Simpsons");
            user.getAttributes().put("attr1", Collections.singletonList("val1"));
            user.getAttributes().put("attr2", Collections.singletonList("val2"));

            user = updateAndGet(user);

            assertEquals("Homer", user.getFirstName());
            assertEquals("Simpsons", user.getLastName());
            assertEquals(2, user.getAttributes().size());
            assertEquals(1, user.getAttributes().get("attr1").size());
            assertEquals("val1", user.getAttributes().get("attr1").get(0));
            assertEquals(1, user.getAttributes().get("attr2").size());
            assertEquals("val2", user.getAttributes().get("attr2").get(0));

            // Update attributes
            user.getAttributes().remove("attr1");
            user.getAttributes().get("attr2").add("val3");

            user = updateAndGet(user);

            assertEquals(1, user.getAttributes().size());
            assertEquals(2, user.getAttributes().get("attr2").size());
            assertThat(user.getAttributes().get("attr2"), containsInAnyOrder("val2", "val3"));

            // Update email
            user.setEmail("bobby@localhost");
            user = updateAndGet(user);
            assertEquals("bobby@localhost", user.getEmail());

            user.setEmail("john-doh@localhost");
            updateError(user, 409, Messages.EMAIL_EXISTS);

            user.setEmail("test-user@localhost");
            user = updateAndGet(user);
            assertEquals("test-user@localhost", user.getEmail());

            // Update username
            user.setUsername("updatedUsername");
            user = updateAndGet(user);
            assertEquals("updatedusername", user.getUsername());

            user.setUsername("john-doh@localhost");
            updateError(user, 409, Messages.USERNAME_EXISTS);

            user.setUsername("test-user@localhost");
            user = updateAndGet(user);
            assertEquals("test-user@localhost", user.getUsername());

            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
            realmRep.setEditUsernameAllowed(false);
            adminClient.realm("test").update(realmRep);

            user.setUsername("updatedUsername2");
            updateError(user, 400, Messages.READ_ONLY_USERNAME);
        } finally {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
            realmRep.setEditUsernameAllowed(true);
            adminClient.realm("test").update(realmRep);

            user.setFirstName(originalFirstName);
            user.setLastName(originalLastName);
            user.setEmail(originalEmail);
            user.setAttributes(originalAttributes);
            SimpleHttp.Response response = SimpleHttp.doPost(getAccountUrl(null), client).auth(tokenUtil.getToken()).json(user).asResponse();
            System.out.println(response.asString());
            assertEquals(200, response.getStatus());
        }

    }

    // KEYCLOAK-7572
    @Test
    public void testUpdateProfileWithRegistrationEmailAsUsername() throws IOException {
        RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
        realmRep.setRegistrationEmailAsUsername(true);
        adminClient.realm("test").update(realmRep);

        UserRepresentation user = SimpleHttp.doGet(getAccountUrl(null), client).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
        String originalFirstname = user.getFirstName();

        try {
            user.setFirstName("Homer1");

            user = updateAndGet(user);

            assertEquals("Homer1", user.getFirstName());
        } finally {
            user.setFirstName(originalFirstname);
            int status = SimpleHttp.doPost(getAccountUrl(null), client).auth(tokenUtil.getToken()).json(user).asStatus();
            assertEquals(200, status);
        }
    }

    private UserRepresentation updateAndGet(UserRepresentation user) throws IOException {
        int status = SimpleHttp.doPost(getAccountUrl(null), client).auth(tokenUtil.getToken()).json(user).asStatus();
        assertEquals(200, status);
        return SimpleHttp.doGet(getAccountUrl(null), client).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
    }


    private void updateError(UserRepresentation user, int expectedStatus, String expectedMessage) throws IOException {
        SimpleHttp.Response response = SimpleHttp.doPost(getAccountUrl(null), client).auth(tokenUtil.getToken()).json(user).asResponse();
        assertEquals(expectedStatus, response.getStatus());
        assertEquals(expectedMessage, response.asJson(ErrorRepresentation.class).getErrorMessage());
    }

    @Test
    public void testProfilePermissions() throws IOException {
        TokenUtil noaccessToken = new TokenUtil("no-account-access", "password");
        TokenUtil viewToken = new TokenUtil("view-account-access", "password");

        // Read with no access
        assertEquals(403, SimpleHttp.doGet(getAccountUrl(null), client).header("Accept", "application/json").auth(noaccessToken.getToken()).asStatus());

        // Update with no access
        assertEquals(403, SimpleHttp.doPost(getAccountUrl(null), client).auth(noaccessToken.getToken()).json(new UserRepresentation()).asStatus());

        // Update with read only
        assertEquals(403, SimpleHttp.doPost(getAccountUrl(null), client).auth(viewToken.getToken()).json(new UserRepresentation()).asStatus());
    }
    
    @Test
    public void testProfilePreviewPermissions() throws IOException {
        assumeFeatureEnabled(ACCOUNT_API);
        
        TokenUtil noaccessToken = new TokenUtil("no-account-access", "password");
        TokenUtil viewToken = new TokenUtil("view-account-access", "password");
        
        // Read sessions with no access
        assertEquals(403, SimpleHttp.doGet(getAccountUrl("sessions"), client).header("Accept", "application/json").auth(noaccessToken.getToken()).asStatus());
        
        // Delete all sessions with no access
        assertEquals(403, SimpleHttp.doDelete(getAccountUrl("sessions"), client).header("Accept", "application/json").auth(noaccessToken.getToken()).asStatus());
        
        // Delete all sessions with read only
        assertEquals(403, SimpleHttp.doDelete(getAccountUrl("sessions"), client).header("Accept", "application/json").auth(viewToken.getToken()).asStatus());
        
        // Delete single session with no access
        assertEquals(403, SimpleHttp.doDelete(getAccountUrl("session?id=bogusId"), client).header("Accept", "application/json").auth(noaccessToken.getToken()).asStatus());
        
        // Delete single session with read only
        assertEquals(403, SimpleHttp.doDelete(getAccountUrl("session?id=bogusId"), client).header("Accept", "application/json").auth(viewToken.getToken()).asStatus());
        
        // Read password details with no access
        assertEquals(403, SimpleHttp.doGet(getAccountUrl("credentials/password"), client).header("Accept", "application/json").auth(noaccessToken.getToken()).asStatus());
        
        // Update password with no access
        assertEquals(403, SimpleHttp.doPost(getAccountUrl("credentials/password"), client).auth(noaccessToken.getToken()).json(new PasswordUpdate()).asStatus());
        
        // Update password with read only
        assertEquals(403, SimpleHttp.doPost(getAccountUrl("credentials/password"), client).auth(viewToken.getToken()).json(new PasswordUpdate()).asStatus());
    }

    @Test
    public void testUpdateProfilePermissions() throws IOException {
        TokenUtil noaccessToken = new TokenUtil("no-account-access", "password");
        int status = SimpleHttp.doGet(getAccountUrl(null), client).header("Accept", "application/json").auth(noaccessToken.getToken()).asStatus();
        assertEquals(403, status);

        TokenUtil viewToken = new TokenUtil("view-account-access", "password");
        status = SimpleHttp.doGet(getAccountUrl(null), client).header("Accept", "application/json").auth(viewToken.getToken()).asStatus();
        assertEquals(200, status);
    }

    @Test
    public void testGetSessions() throws IOException {
        assumeFeatureEnabled(ACCOUNT_API);
        
        List<SessionRepresentation> sessions = SimpleHttp.doGet(getAccountUrl("sessions"), client).auth(tokenUtil.getToken()).asJson(new TypeReference<List<SessionRepresentation>>() {});

        assertEquals(1, sessions.size());
    }

    @Test
    public void testGetPasswordDetails() throws IOException {
        assumeFeatureEnabled(ACCOUNT_API);
        
        getPasswordDetails();
    }

    @Test
    public void testPostPasswordUpdate() throws IOException {
        assumeFeatureEnabled(ACCOUNT_API);
        
        //Get the time of lastUpdate
        AccountCredentialResource.PasswordDetails initialDetails = getPasswordDetails();

        //Change the password
        updatePassword("password", "Str0ng3rP4ssw0rd", 200);

        //Get the new value for lastUpdate
        AccountCredentialResource.PasswordDetails updatedDetails = getPasswordDetails();
        assertTrue(initialDetails.getLastUpdate() < updatedDetails.getLastUpdate());

        //Try to change password again; should fail as current password is incorrect
        updatePassword("password", "Str0ng3rP4ssw0rd", 400);

        //Verify that lastUpdate hasn't changed
        AccountCredentialResource.PasswordDetails finalDetails = getPasswordDetails();
        assertEquals(updatedDetails.getLastUpdate(), finalDetails.getLastUpdate());

        //Change the password back
        updatePassword("Str0ng3rP4ssw0rd", "password", 200);
   }

    @Test
    public void testPasswordConfirmation() throws IOException {
        assumeFeatureEnabled(ACCOUNT_API);
        
        updatePassword("password", "Str0ng3rP4ssw0rd", "confirmationDoesNotMatch", 400);

        updatePassword("password", "Str0ng3rP4ssw0rd", "Str0ng3rP4ssw0rd", 200);

        //Change the password back
        updatePassword("Str0ng3rP4ssw0rd", "password", 200);
    }

    private AccountCredentialResource.PasswordDetails getPasswordDetails() throws IOException {
        AccountCredentialResource.PasswordDetails details = SimpleHttp.doGet(getAccountUrl("credentials/password"), client).auth(tokenUtil.getToken()).asJson(new TypeReference<AccountCredentialResource.PasswordDetails>() {});
        assertTrue(details.isRegistered());
        assertNotNull(details.getLastUpdate());
        return details;
    }

    private void updatePassword(String currentPass, String newPass, int expectedStatus) throws IOException {
        updatePassword(currentPass, newPass, null, expectedStatus);
    }

    private void updatePassword(String currentPass, String newPass, String confirmation, int expectedStatus) throws IOException {
        AccountCredentialResource.PasswordUpdate passwordUpdate = new AccountCredentialResource.PasswordUpdate();
        passwordUpdate.setCurrentPassword(currentPass);
        passwordUpdate.setNewPassword(newPass);
        passwordUpdate.setConfirmation(confirmation);
        int status = SimpleHttp.doPost(getAccountUrl("credentials/password"), client).auth(tokenUtil.getToken()).json(passwordUpdate).asStatus();
        assertEquals(expectedStatus, status);
    }

    public void testDeleteSessions() throws IOException {
        TokenUtil viewToken = new TokenUtil("view-account-access", "password");
        oauth.doLogin("view-account-access", "password");
        List<SessionRepresentation> sessions = SimpleHttp.doGet(getAccountUrl("sessions"), client).auth(viewToken.getToken()).asJson(new TypeReference<List<SessionRepresentation>>() {});
        assertEquals(2, sessions.size());
        int status = SimpleHttp.doDelete(getAccountUrl("sessions?current=false"), client).acceptJson().auth(viewToken.getToken()).asStatus();
        assertEquals(200, status);
        sessions = SimpleHttp.doGet(getAccountUrl("sessions"), client).auth(viewToken.getToken()).asJson(new TypeReference<List<SessionRepresentation>>() {});
        assertEquals(1, sessions.size());
    }

    @Test
    public void testDeleteSession() throws IOException {
        assumeFeatureEnabled(ACCOUNT_API);
        
        TokenUtil viewToken = new TokenUtil("view-account-access", "password");
        String sessionId = oauth.doLogin("view-account-access", "password").getSessionState();
        List<SessionRepresentation> sessions = SimpleHttp.doGet(getAccountUrl("sessions"), client).auth(viewToken.getToken()).asJson(new TypeReference<List<SessionRepresentation>>() {});
        assertEquals(2, sessions.size());

        // With `ViewToken` you can only read
        int status = SimpleHttp.doDelete(getAccountUrl("session?id=" + sessionId), client).acceptJson().auth(viewToken.getToken()).asStatus();
        assertEquals(403, status);
        sessions = SimpleHttp.doGet(getAccountUrl("sessions"), client).auth(viewToken.getToken()).asJson(new TypeReference<List<SessionRepresentation>>() {});
        assertEquals(2, sessions.size());

        // Here you can delete the session
        status = SimpleHttp.doDelete(getAccountUrl("session?id=" + sessionId), client).acceptJson().auth(tokenUtil.getToken()).asStatus();
        assertEquals(200, status);
        sessions = SimpleHttp.doGet(getAccountUrl("sessions"), client).auth(tokenUtil.getToken()).asJson(new TypeReference<List<SessionRepresentation>>() {});
        assertEquals(1, sessions.size());
    }

    private String getAccountUrl(String resource) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/test/account" + (resource != null ? "/" + resource : "");
    }

    // Check if the feature really works
    private void checkIfFeatureWorks(boolean shouldWorks) {
        try {
            List<SessionRepresentation> sessions = SimpleHttp.doGet(getAccountUrl("sessions"), client).auth(tokenUtil.getToken())
                    .asJson(new TypeReference<List<SessionRepresentation>>() {
                    });
            assertEquals(1, sessions.size());
            if (!shouldWorks)
                fail("Feature is available, but this moment should be disabled");

        } catch (Exception e) {
            if (shouldWorks) {
                e.printStackTrace();
                fail("Feature is not available");
            }
        }
    }
}
