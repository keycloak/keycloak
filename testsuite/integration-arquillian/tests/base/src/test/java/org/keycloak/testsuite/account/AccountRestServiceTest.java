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
import org.junit.Test;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.representations.account.ClientRepresentation;
import org.keycloak.representations.account.ConsentRepresentation;
import org.keycloak.representations.account.ConsentScopeRepresentation;
import org.keycloak.representations.account.SessionRepresentation;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.account.AccountCredentialResource;
import org.keycloak.services.resources.account.AccountCredentialResource.PasswordUpdate;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.TokenUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;
import static org.keycloak.common.Profile.Feature.ACCOUNT_API;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountRestServiceTest extends AbstractRestServiceTest {

    @Test
    public void testGetProfile() throws IOException {
        UserRepresentation user = SimpleHttp.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
        assertEquals("Tom", user.getFirstName());
        assertEquals("Brady", user.getLastName());
        assertEquals("test-user@localhost", user.getEmail());
        assertFalse(user.isEmailVerified());
        assertTrue(user.getAttributes().isEmpty());
    }

    @Test
    public void testUpdateProfile() throws IOException {
        UserRepresentation user = SimpleHttp.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
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
            SimpleHttp.Response response = SimpleHttp.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asResponse();
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

        UserRepresentation user = SimpleHttp.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
        String originalFirstname = user.getFirstName();

        try {
            user.setFirstName("Homer1");

            user = updateAndGet(user);

            assertEquals("Homer1", user.getFirstName());
        } finally {
            user.setFirstName(originalFirstname);
            int status = SimpleHttp.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asStatus();
            assertEquals(200, status);
        }
    }

    private UserRepresentation updateAndGet(UserRepresentation user) throws IOException {
        int status = SimpleHttp.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asStatus();
        assertEquals(200, status);
        return SimpleHttp.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
    }


    private void updateError(UserRepresentation user, int expectedStatus, String expectedMessage) throws IOException {
        SimpleHttp.Response response = SimpleHttp.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asResponse();
        assertEquals(expectedStatus, response.getStatus());
        assertEquals(expectedMessage, response.asJson(ErrorRepresentation.class).getErrorMessage());
    }

    @Test
    public void testProfilePermissions() throws IOException {
        TokenUtil noaccessToken = new TokenUtil("no-account-access", "password");
        TokenUtil viewToken = new TokenUtil("view-account-access", "password");

        // Read with no access
        assertEquals(403, SimpleHttp.doGet(getAccountUrl(null), httpClient).header("Accept", "application/json").auth(noaccessToken.getToken()).asStatus());

        // Update with no access
        assertEquals(403, SimpleHttp.doPost(getAccountUrl(null), httpClient).auth(noaccessToken.getToken()).json(new UserRepresentation()).asStatus());

        // Update with read only
        assertEquals(403, SimpleHttp.doPost(getAccountUrl(null), httpClient).auth(viewToken.getToken()).json(new UserRepresentation()).asStatus());
    }
    
    @Test
    public void testProfilePreviewPermissions() throws IOException {
        TokenUtil noaccessToken = new TokenUtil("no-account-access", "password");
        TokenUtil viewToken = new TokenUtil("view-account-access", "password");
        
        // Read password details with no access
        assertEquals(403, SimpleHttp.doGet(getAccountUrl("credentials/password"), httpClient).header("Accept", "application/json").auth(noaccessToken.getToken()).asStatus());
        
        // Update password with no access
        assertEquals(403, SimpleHttp.doPost(getAccountUrl("credentials/password"), httpClient).auth(noaccessToken.getToken()).json(new PasswordUpdate()).asStatus());
        
        // Update password with read only
        assertEquals(403, SimpleHttp.doPost(getAccountUrl("credentials/password"), httpClient).auth(viewToken.getToken()).json(new PasswordUpdate()).asStatus());
    }

    @Test
    public void testUpdateProfilePermissions() throws IOException {
        TokenUtil noaccessToken = new TokenUtil("no-account-access", "password");
        int status = SimpleHttp.doGet(getAccountUrl(null), httpClient).header("Accept", "application/json").auth(noaccessToken.getToken()).asStatus();
        assertEquals(403, status);

        TokenUtil viewToken = new TokenUtil("view-account-access", "password");
        status = SimpleHttp.doGet(getAccountUrl(null), httpClient).header("Accept", "application/json").auth(viewToken.getToken()).asStatus();
        assertEquals(200, status);
    }

    @Test
    public void testGetPasswordDetails() throws IOException {
        getPasswordDetails();
    }

    @Test
    public void testPostPasswordUpdate() throws IOException {
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
        updatePassword("password", "Str0ng3rP4ssw0rd", "confirmationDoesNotMatch", 400);

        updatePassword("password", "Str0ng3rP4ssw0rd", "Str0ng3rP4ssw0rd", 200);

        //Change the password back
        updatePassword("Str0ng3rP4ssw0rd", "password", 200);
    }

    private AccountCredentialResource.PasswordDetails getPasswordDetails() throws IOException {
        AccountCredentialResource.PasswordDetails details = SimpleHttp.doGet(getAccountUrl("credentials/password"), httpClient).auth(tokenUtil.getToken()).asJson(new TypeReference<AccountCredentialResource.PasswordDetails>() {});
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
        int status = SimpleHttp.doPost(getAccountUrl("credentials/password"), httpClient).auth(tokenUtil.getToken()).json(passwordUpdate).asStatus();
        assertEquals(expectedStatus, status);
    }

    public void testDeleteSessions() throws IOException {
        TokenUtil viewToken = new TokenUtil("view-account-access", "password");
        oauth.doLogin("view-account-access", "password");
        List<SessionRepresentation> sessions = SimpleHttp.doGet(getAccountUrl("sessions"), httpClient).auth(viewToken.getToken()).asJson(new TypeReference<List<SessionRepresentation>>() {});
        assertEquals(2, sessions.size());
        int status = SimpleHttp.doDelete(getAccountUrl("sessions?current=false"), httpClient).acceptJson().auth(viewToken.getToken()).asStatus();
        assertEquals(200, status);
        sessions = SimpleHttp.doGet(getAccountUrl("sessions"), httpClient).auth(viewToken.getToken()).asJson(new TypeReference<List<SessionRepresentation>>() {});
        assertEquals(1, sessions.size());
    }

    @Test
    public void listApplications() throws IOException {
        TokenUtil token = new TokenUtil("view-applications-access", "password");
        List<ClientRepresentation> applications = SimpleHttp
                .doGet(getAccountUrl("applications"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asJson(new TypeReference<List<ClientRepresentation>>() {
                });
        assertFalse(applications.isEmpty());
    }

    @Test
    public void listApplicationsWithoutPermission() throws IOException {
        TokenUtil token = new TokenUtil("view-account-access", "password");
        SimpleHttp.Response response = SimpleHttp
                .doGet(getAccountUrl("applications"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asResponse();
        assertEquals(403, response.getStatus());
    }

    @Test
    public void getWebConsoleApplication() throws IOException {
        TokenUtil token = new TokenUtil("view-applications-access", "password");
        String appId = "security-admin-console";
        ClientRepresentation webConsole = SimpleHttp
                .doGet(getAccountUrl("applications/" + appId), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asJson(ClientRepresentation.class);
        assertEquals(appId, webConsole.getClientId());
    }

    @Test
    public void getWebConsoleApplicationWithoutPermission() throws IOException {
        TokenUtil token = new TokenUtil("view-account-access", "password");
        String appId = "security-admin-console";
        SimpleHttp.Response response = SimpleHttp
                .doGet(getAccountUrl("applications/" + appId), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asResponse();
        assertEquals(403, response.getStatus());
    }

    @Test
    public void getNotExistingApplication() throws IOException {
        TokenUtil token = new TokenUtil("view-applications-access", "password");
        String appId = "not-existing";
        SimpleHttp.Response response = SimpleHttp
                .doGet(getAccountUrl("applications/" + appId), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asResponse();
        assertEquals(404, response.getStatus());
    }

    @Test
    public void createConsentForClient() throws IOException {
        TokenUtil token = new TokenUtil("manage-consent-access", "password");
        String appId = "security-admin-console";

        ClientScopeRepresentation clientScopeRepresentation = testRealm().clientScopes().findAll().get(0);
        ConsentScopeRepresentation consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        ConsentRepresentation requestedConsent = new ConsentRepresentation();
        requestedConsent.setScopes(Collections.singletonList(consentScopeRepresentation));

        ConsentRepresentation consentRepresentation = SimpleHttp
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation.getCreatedDate() > 0);
        assertTrue(consentRepresentation.getLastUpdatedDate() > 0);
        assertEquals(1, consentRepresentation.getScopes().size());
        assertEquals(consentScopeRepresentation.getId(), consentRepresentation.getScopes().get(0).getId());
    }

    @Test
    public void updateConsentForClient() throws IOException {
        TokenUtil token = new TokenUtil("manage-consent-access", "password");
        String appId = "security-admin-console";

        ClientScopeRepresentation clientScopeRepresentation = testRealm().clientScopes().findAll().get(0);
        ConsentScopeRepresentation consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        ConsentRepresentation requestedConsent = new ConsentRepresentation();
        requestedConsent.setScopes(Collections.singletonList(consentScopeRepresentation));

        ConsentRepresentation consentRepresentation = SimpleHttp
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation.getCreatedDate() > 0);
        assertTrue(consentRepresentation.getLastUpdatedDate() > 0);
        assertEquals(1, consentRepresentation.getScopes().size());
        assertEquals(consentScopeRepresentation.getId(), consentRepresentation.getScopes().get(0).getId());

        clientScopeRepresentation = testRealm().clientScopes().findAll().get(1);
        consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        requestedConsent = new ConsentRepresentation();
        requestedConsent.setScopes(Collections.singletonList(consentScopeRepresentation));

        ConsentRepresentation consentRepresentation2 = SimpleHttp
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation2.getCreatedDate() > 0);
        assertEquals(consentRepresentation.getCreatedDate(), consentRepresentation2.getCreatedDate());
        assertTrue(consentRepresentation2.getLastUpdatedDate() > 0);
        assertTrue(consentRepresentation2.getLastUpdatedDate() > consentRepresentation.getLastUpdatedDate());
        assertEquals(1, consentRepresentation2.getScopes().size());
        assertEquals(consentScopeRepresentation.getId(), consentRepresentation2.getScopes().get(0).getId());
    }

    @Test
    public void createConsentForNotExistingClient() throws IOException {
        TokenUtil token = new TokenUtil("manage-consent-access", "password");
        String appId = "not-existing";

        ClientScopeRepresentation clientScopeRepresentation = testRealm().clientScopes().findAll().get(0);
        ConsentScopeRepresentation consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        ConsentRepresentation requestedConsent = new ConsentRepresentation();
        requestedConsent.setScopes(Collections.singletonList(consentScopeRepresentation));

        SimpleHttp.Response response = SimpleHttp
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asResponse();

        assertEquals(404, response.getStatus());
    }

    @Test
    public void createConsentForClientWithoutPermission() throws IOException {
        TokenUtil token = new TokenUtil("view-consent-access", "password");
        String appId = "security-admin-console";

        ClientScopeRepresentation clientScopeRepresentation = testRealm().clientScopes().findAll().get(0);
        ConsentScopeRepresentation consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        ConsentRepresentation requestedConsent = new ConsentRepresentation();
        requestedConsent.setScopes(Collections.singletonList(consentScopeRepresentation));

        SimpleHttp.Response response = SimpleHttp
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asResponse();

        assertEquals(403, response.getStatus());
    }

    @Test
    public void createConsentForClientWithPut() throws IOException {
        TokenUtil token = new TokenUtil("manage-consent-access", "password");
        String appId = "security-admin-console";

        ClientScopeRepresentation clientScopeRepresentation = testRealm().clientScopes().findAll().get(0);
        ConsentScopeRepresentation consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        ConsentRepresentation requestedConsent = new ConsentRepresentation();
        requestedConsent.setScopes(Collections.singletonList(consentScopeRepresentation));

        ConsentRepresentation consentRepresentation = SimpleHttp
                .doPut(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation.getCreatedDate() > 0);
        assertTrue(consentRepresentation.getLastUpdatedDate() > 0);
        assertEquals(1, consentRepresentation.getScopes().size());
        assertEquals(consentScopeRepresentation.getId(), consentRepresentation.getScopes().get(0).getId());
    }

    @Test
    public void updateConsentForClientWithPut() throws IOException {
        TokenUtil token = new TokenUtil("manage-consent-access", "password");
        String appId = "security-admin-console";

        ClientScopeRepresentation clientScopeRepresentation = testRealm().clientScopes().findAll().get(0);
        ConsentScopeRepresentation consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        ConsentRepresentation requestedConsent = new ConsentRepresentation();
        requestedConsent.setScopes(Collections.singletonList(consentScopeRepresentation));

        ConsentRepresentation consentRepresentation = SimpleHttp
                .doPut(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation.getCreatedDate() > 0);
        assertTrue(consentRepresentation.getLastUpdatedDate() > 0);
        assertEquals(1, consentRepresentation.getScopes().size());
        assertEquals(consentScopeRepresentation.getId(), consentRepresentation.getScopes().get(0).getId());

        clientScopeRepresentation = testRealm().clientScopes().findAll().get(1);
        consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        requestedConsent = new ConsentRepresentation();
        requestedConsent.setScopes(Collections.singletonList(consentScopeRepresentation));

        ConsentRepresentation consentRepresentation2 = SimpleHttp
                .doPut(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation2.getCreatedDate() > 0);
        assertEquals(consentRepresentation.getCreatedDate(), consentRepresentation2.getCreatedDate());
        assertTrue(consentRepresentation2.getLastUpdatedDate() > 0);
        assertTrue(consentRepresentation2.getLastUpdatedDate() > consentRepresentation.getLastUpdatedDate());
        assertEquals(1, consentRepresentation2.getScopes().size());
        assertEquals(consentScopeRepresentation.getId(), consentRepresentation2.getScopes().get(0).getId());
    }

    @Test
    public void createConsentForNotExistingClientWithPut() throws IOException {
        TokenUtil token = new TokenUtil("manage-consent-access", "password");
        String appId = "not-existing";

        ClientScopeRepresentation clientScopeRepresentation = testRealm().clientScopes().findAll().get(0);
        ConsentScopeRepresentation consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        ConsentRepresentation requestedConsent = new ConsentRepresentation();
        requestedConsent.setScopes(Collections.singletonList(consentScopeRepresentation));

        SimpleHttp.Response response = SimpleHttp
                .doPut(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asResponse();

        assertEquals(404, response.getStatus());
    }

    @Test
    public void createConsentForClientWithoutPermissionWithPut() throws IOException {
        TokenUtil token = new TokenUtil("view-consent-access", "password");
        String appId = "security-admin-console";

        ClientScopeRepresentation clientScopeRepresentation = testRealm().clientScopes().findAll().get(0);
        ConsentScopeRepresentation consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        ConsentRepresentation requestedConsent = new ConsentRepresentation();
        requestedConsent.setScopes(Collections.singletonList(consentScopeRepresentation));

        SimpleHttp.Response response = SimpleHttp
                .doPut(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asResponse();

        assertEquals(403, response.getStatus());
    }

    @Test
    public void getConsentForClient() throws IOException {
        TokenUtil token = new TokenUtil("manage-consent-access", "password");
        String appId = "security-admin-console";

        ClientScopeRepresentation clientScopeRepresentation = testRealm().clientScopes().findAll().get(0);
        ConsentScopeRepresentation consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        ConsentRepresentation requestedConsent = new ConsentRepresentation();
        requestedConsent.setScopes(Collections.singletonList(consentScopeRepresentation));

        ConsentRepresentation consentRepresentation1 = SimpleHttp
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation1.getCreatedDate() > 0);
        assertTrue(consentRepresentation1.getLastUpdatedDate() > 0);
        assertEquals(1, consentRepresentation1.getScopes().size());
        assertEquals(consentScopeRepresentation.getId(), consentRepresentation1.getScopes().get(0).getId());

        ConsentRepresentation consentRepresentation2 = SimpleHttp
                .doGet(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asJson(ConsentRepresentation.class);
        assertEquals(consentRepresentation1.getLastUpdatedDate(), consentRepresentation2.getLastUpdatedDate());
        assertEquals(consentRepresentation1.getCreatedDate(), consentRepresentation2.getCreatedDate());
        assertEquals(consentRepresentation1.getScopes().get(0).getId(), consentRepresentation2.getScopes().get(0).getId());
    }

    @Test
    public void getConsentForNotExistingClient() throws IOException {
        TokenUtil token = new TokenUtil("view-consent-access", "password");
        String appId = "not-existing";
        SimpleHttp.Response response = SimpleHttp
                .doGet(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asResponse();
        assertEquals(404, response.getStatus());
    }

    @Test
    public void getNotExistingConsentForClient() throws IOException {
        TokenUtil token = new TokenUtil("view-consent-access", "password");
        String appId = "security-admin-console";
        SimpleHttp.Response response = SimpleHttp
                .doGet(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asResponse();
        assertEquals(204, response.getStatus());
    }

    @Test
    public void getConsentWithoutPermission() throws IOException {
        TokenUtil token = new TokenUtil("view-applications-access", "password");
        String appId = "security-admin-console";
        SimpleHttp.Response response = SimpleHttp
                .doGet(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asResponse();
        assertEquals(403, response.getStatus());
    }

    @Test
    public void deleteConsentForClient() throws IOException {
        TokenUtil token = new TokenUtil("manage-consent-access", "password");
        String appId = "security-admin-console";

        ClientScopeRepresentation clientScopeRepresentation = testRealm().clientScopes().findAll().get(0);
        ConsentScopeRepresentation consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        ConsentRepresentation requestedConsent = new ConsentRepresentation();
        requestedConsent.setScopes(Collections.singletonList(consentScopeRepresentation));

        ConsentRepresentation consentRepresentation = SimpleHttp
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation.getCreatedDate() > 0);
        assertTrue(consentRepresentation.getLastUpdatedDate() > 0);
        assertEquals(1, consentRepresentation.getScopes().size());
        assertEquals(consentScopeRepresentation.getId(), consentRepresentation.getScopes().get(0).getId());

        SimpleHttp.Response response = SimpleHttp
                .doDelete(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asResponse();
        assertEquals(202, response.getStatus());

        response = SimpleHttp
                .doDelete(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asResponse();
        assertEquals(202, response.getStatus());
    }

    @Test
    public void deleteConsentForNotExistingClient() throws IOException {
        TokenUtil token = new TokenUtil("manage-consent-access", "password");
        String appId = "not-existing";
        SimpleHttp.Response response = SimpleHttp
                .doDelete(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asResponse();
        assertEquals(404, response.getStatus());
    }


    @Test
    public void deleteConsentWithoutPermission() throws IOException {
        TokenUtil token = new TokenUtil("view-consent-access", "password");
        String appId = "security-admin-console";
        SimpleHttp.Response response = SimpleHttp
                .doDelete(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asResponse();
        assertEquals(403, response.getStatus());
    }
}
