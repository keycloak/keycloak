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
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnPasswordlessAuthenticatorFactory;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.credential.CredentialTypeMetadata;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.account.ClientRepresentation;
import org.keycloak.representations.account.ConsentRepresentation;
import org.keycloak.representations.account.ConsentScopeRepresentation;
import org.keycloak.representations.account.SessionRepresentation;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.account.AccountCredentialResource;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.admin.authentication.AbstractAuthenticationTest;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.TokenUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;
import org.keycloak.services.resources.account.AccountCredentialResource.PasswordUpdate;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
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
        String originalUsername = user.getUsername();
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

            user.setUsername(originalUsername);
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

        // ignore login event
        events.poll();

        //Change the password
        updatePassword("password", "Str0ng3rP4ssw0rd", 200);

        //Get the new value for lastUpdate
        AccountCredentialResource.PasswordDetails updatedDetails = getPasswordDetails();
        assertTrue(initialDetails.getLastUpdate() < updatedDetails.getLastUpdate());
        Assert.assertEquals(EventType.UPDATE_PASSWORD.name(), events.poll().getType());

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

    @Test
    public void testCredentialsGet() throws IOException {
        configureBrowserFlowWithWebAuthnAuthenticator("browser-webauthn");

        List<AccountCredentialResource.CredentialContainer> credentials = getCredentials();

        Assert.assertEquals(4, credentials.size());

        AccountCredentialResource.CredentialContainer password = credentials.get(0);
        assertCredentialContainerExpected(password, PasswordCredentialModel.TYPE, CredentialTypeMetadata.Category.BASIC_AUTHENTICATION.toString(),
                "password-display-name", "password-help-text", "kcAuthenticatorPasswordClass",
                null, UserModel.RequiredAction.UPDATE_PASSWORD.toString(), false, 1);

        CredentialRepresentation password1 = password.getUserCredentials().get(0);
        Assert.assertNull(password1.getSecretData());
        Assert.assertNotNull(password1.getCredentialData());

        AccountCredentialResource.CredentialContainer otp = credentials.get(1);
        assertCredentialContainerExpected(otp, OTPCredentialModel.TYPE, CredentialTypeMetadata.Category.TWO_FACTOR.toString(),
                "otp-display-name", "otp-help-text", "kcAuthenticatorOTPClass",
                UserModel.RequiredAction.CONFIGURE_TOTP.toString(), null, true, 0);

        // WebAuthn credentials will be returned, but createAction will be still null because requiredAction "webauthn register" not yet registered
        AccountCredentialResource.CredentialContainer webauthn = credentials.get(2);
        assertCredentialContainerExpected(webauthn, WebAuthnCredentialModel.TYPE_TWOFACTOR, CredentialTypeMetadata.Category.TWO_FACTOR.toString(),
                "webauthn-display-name", "webauthn-help-text", "kcAuthenticatorWebAuthnClass",
                null, null, true, 0);

        AccountCredentialResource.CredentialContainer webauthnPasswordless = credentials.get(3);
        assertCredentialContainerExpected(webauthnPasswordless, WebAuthnCredentialModel.TYPE_PASSWORDLESS, CredentialTypeMetadata.Category.PASSWORDLESS.toString(),
                "webauthn-passwordless-display-name", "webauthn-passwordless-help-text", "kcAuthenticatorWebAuthnPasswordlessClass",
                null, null, true, 0);

        // Register requiredActions for WebAuthn
        RequiredActionProviderSimpleRepresentation requiredAction = new RequiredActionProviderSimpleRepresentation();
        requiredAction.setId("12345");
        requiredAction.setName(WebAuthnRegisterFactory.PROVIDER_ID);
        requiredAction.setProviderId(WebAuthnRegisterFactory.PROVIDER_ID);
        testRealm().flows().registerRequiredAction(requiredAction);

        requiredAction = new RequiredActionProviderSimpleRepresentation();
        requiredAction.setId("6789");
        requiredAction.setName(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);
        requiredAction.setProviderId(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);
        testRealm().flows().registerRequiredAction(requiredAction);

        // requiredActions should be available
        credentials = getCredentials();
        Assert.assertEquals(WebAuthnRegisterFactory.PROVIDER_ID, credentials.get(2).getCreateAction());
        Assert.assertEquals(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID, credentials.get(3).getCreateAction());

        // disable WebAuthn passwordless required action. It won't be returned then
        RequiredActionProviderRepresentation requiredActionRep = testRealm().flows().getRequiredAction(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);
        requiredActionRep.setEnabled(false);
        testRealm().flows().updateRequiredAction(WebAuthnRegisterFactory.PROVIDER_ID, requiredActionRep);

        credentials = getCredentials();
        Assert.assertNull(credentials.get(2).getCreateAction());

        // Test that WebAuthn won't be returned when removed from the authentication flow
        removeWebAuthnFlow("browser-webauthn");

        credentials = getCredentials();

        Assert.assertEquals(2, credentials.size());
        Assert.assertEquals(PasswordCredentialModel.TYPE, credentials.get(0).getType());
        Assert.assertNotNull(OTPCredentialModel.TYPE, credentials.get(1).getType());

        // Test password-only
        credentials = SimpleHttp.doGet(getAccountUrl("credentials?" + AccountCredentialResource.TYPE + "=password"), httpClient)
                .auth(tokenUtil.getToken()).asJson(new TypeReference<List<AccountCredentialResource.CredentialContainer>>() {});
        Assert.assertEquals(1, credentials.size());
        password = credentials.get(0);
        Assert.assertEquals(PasswordCredentialModel.TYPE, password.getType());
        Assert.assertEquals(1, password.getUserCredentials().size());

        // Test password-only and user-credentials
        credentials = SimpleHttp.doGet(getAccountUrl("credentials?" + AccountCredentialResource.TYPE + "=password&" +
                AccountCredentialResource.USER_CREDENTIALS + "=false"), httpClient)
                .auth(tokenUtil.getToken()).asJson(new TypeReference<List<AccountCredentialResource.CredentialContainer>>() {});
        Assert.assertEquals(1, credentials.size());
        password = credentials.get(0);
        Assert.assertEquals(PasswordCredentialModel.TYPE, password.getType());
        Assert.assertNull(password.getUserCredentials());
    }

    // Send REST request to get all credential containers and credentials of current user
    private List<AccountCredentialResource.CredentialContainer> getCredentials() throws IOException {
        return SimpleHttp.doGet(getAccountUrl("credentials"), httpClient)
                .auth(tokenUtil.getToken()).asJson(new TypeReference<List<AccountCredentialResource.CredentialContainer>>() {});
    }

    @Test
    public void testCredentialsGetDisabledOtp() throws IOException {
        // Disable OTP in all built-in flows

        // Disable parent subflow - that should treat OTP execution as disabled too
        AuthenticationExecutionModel.Requirement currentBrowserReq = setExecutionRequirement(DefaultAuthenticationFlows.BROWSER_FLOW,
                "Browser - Conditional OTP", AuthenticationExecutionModel.Requirement.DISABLED);

        // Disable OTP directly in first-broker-login and direct-grant
        AuthenticationExecutionModel.Requirement currentFBLReq = setExecutionRequirement(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW,
                "OTP Form", AuthenticationExecutionModel.Requirement.DISABLED);
        AuthenticationExecutionModel.Requirement currentDirectGrantReq = setExecutionRequirement(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW,
                "Direct Grant - Conditional OTP", AuthenticationExecutionModel.Requirement.DISABLED);
        try {
            // Test that OTP credential is not included. Only password
            List<AccountCredentialResource.CredentialContainer> credentials = getCredentials();

            Assert.assertEquals(1, credentials.size());
            Assert.assertEquals(PasswordCredentialModel.TYPE, credentials.get(0).getType());

            // Enable browser subflow. OTP should be available then
            setExecutionRequirement(DefaultAuthenticationFlows.BROWSER_FLOW,
                    "Browser - Conditional OTP", currentBrowserReq);
            credentials = getCredentials();
            Assert.assertEquals(2, credentials.size());
            Assert.assertEquals(OTPCredentialModel.TYPE, credentials.get(1).getType());

            // Disable browser subflow and enable FirstBrokerLogin. OTP should be available then
            setExecutionRequirement(DefaultAuthenticationFlows.BROWSER_FLOW,
                    "Browser - Conditional OTP", AuthenticationExecutionModel.Requirement.DISABLED);
            setExecutionRequirement(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW,
                    "OTP Form", currentFBLReq);
            credentials = getCredentials();
            Assert.assertEquals(2, credentials.size());
            Assert.assertEquals(OTPCredentialModel.TYPE, credentials.get(1).getType());
        } finally {
            // Revert flows
            setExecutionRequirement(DefaultAuthenticationFlows.BROWSER_FLOW,
                    "Browser - Conditional OTP", currentBrowserReq);
            setExecutionRequirement(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW,
                    "Direct Grant - Conditional OTP", currentDirectGrantReq);
        }
    }

    @Test
    public void testCredentialsForUserWithoutPassword() throws IOException {
        // This is just to call REST to ensure tokenUtil will authenticate user and create the tokens.
        // We won't be able to authenticate later as user won't have password
        List<AccountCredentialResource.CredentialContainer> credentials = getCredentials();

        // Remove password from the user now
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        for (CredentialRepresentation credential : user.credentials()) {
            if (PasswordCredentialModel.TYPE.equals(credential.getType())) {
                user.removeCredential(credential.getId());
            }
        }

        // Get credentials. Ensure user doesn't have password credential and create action is UPDATE_PASSWORD
        credentials = getCredentials();
        AccountCredentialResource.CredentialContainer password = credentials.get(0);
        assertCredentialContainerExpected(password, PasswordCredentialModel.TYPE, CredentialTypeMetadata.Category.BASIC_AUTHENTICATION.toString(),
                "password-display-name", "password-help-text", "kcAuthenticatorPasswordClass",
                UserModel.RequiredAction.UPDATE_PASSWORD.toString(), null, false, 0);

        // Re-add the password to the user
        ApiUtil.resetUserPassword(user, "password", false);

    }

    // Sets new requirement and returns current requirement
    private AuthenticationExecutionModel.Requirement setExecutionRequirement(String flowAlias, String executionDisplayName, AuthenticationExecutionModel.Requirement newRequirement) {
        List<AuthenticationExecutionInfoRepresentation> executionInfos = testRealm().flows().getExecutions(flowAlias);
        for (AuthenticationExecutionInfoRepresentation exInfo : executionInfos) {
            if (executionDisplayName.equals(exInfo.getDisplayName())) {
                AuthenticationExecutionModel.Requirement currentRequirement = AuthenticationExecutionModel.Requirement.valueOf(exInfo.getRequirement());
                exInfo.setRequirement(newRequirement.toString());
                testRealm().flows().updateExecutions(flowAlias, exInfo);
                return currentRequirement;
            }
        }

        throw new IllegalStateException("Not found execution '" + executionDisplayName + "' in flow '" + flowAlias + "'.");
    }

    private void configureBrowserFlowWithWebAuthnAuthenticator(String newFlowAlias) {
        HashMap<String, String> params = new HashMap<>();
        params.put("newName", newFlowAlias);
        Response response = testRealm().flows().copy("browser", params);
        response.close();
        String flowId = AbstractAuthenticationTest.findFlowByAlias(newFlowAlias, testRealm().flows().getFlows()).getId();

        AuthenticationExecutionRepresentation execution = new AuthenticationExecutionRepresentation();
        execution.setParentFlow(flowId);
        execution.setAuthenticator(WebAuthnAuthenticatorFactory.PROVIDER_ID);
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString());
        response = testRealm().flows().addExecution(execution);
        response.close();

        execution = new AuthenticationExecutionRepresentation();
        execution.setParentFlow(flowId);
        execution.setAuthenticator( WebAuthnPasswordlessAuthenticatorFactory.PROVIDER_ID);
        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE.toString());
        response = testRealm().flows().addExecution(execution);
        response.close();
    }

    private void removeWebAuthnFlow(String flowToDeleteAlias) {
        List<AuthenticationFlowRepresentation> flows = testRealm().flows().getFlows();
        AuthenticationFlowRepresentation flowRepresentation = AbstractAuthenticationTest.findFlowByAlias(flowToDeleteAlias, flows);
        testRealm().flows().deleteFlow(flowRepresentation.getId());
    }

    private void assertCredentialContainerExpected(AccountCredentialResource.CredentialContainer credential, String type, String category, String displayName, String helpText, String iconCssClass,
                                                   String createAction, String updateAction, boolean removeable, int userCredentialsCount) {
        Assert.assertEquals(type, credential.getType());
        Assert.assertEquals(category, credential.getCategory());
        Assert.assertEquals(displayName, credential.getDisplayName());
        Assert.assertEquals(helpText, credential.getHelptext());
        Assert.assertEquals(iconCssClass, credential.getIconCssClass());
        Assert.assertEquals(createAction, credential.getCreateAction());
        Assert.assertEquals(updateAction, credential.getUpdateAction());
        Assert.assertEquals(removeable, credential.isRemoveable());
        Assert.assertEquals(userCredentialsCount, credential.getUserCredentials().size());
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
    public void listApplications() throws Exception {
        oauth.clientId("in-use-client");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("secret1", "view-applications-access", "password");
        Assert.assertNull(tokenResponse.getErrorDescription());

        TokenUtil token = new TokenUtil("view-applications-access", "password");
        List<ClientRepresentation> applications = SimpleHttp
                .doGet(getAccountUrl("applications"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asJson(new TypeReference<List<ClientRepresentation>>() {
                });
        assertFalse(applications.isEmpty());

        Map<String, ClientRepresentation> apps = applications.stream().collect(Collectors.toMap(x -> x.getClientId(), x -> x));
        Assert.assertThat(apps.keySet(), containsInAnyOrder("in-use-client", "always-display-client"));

        assertClientRep(apps.get("in-use-client"), "In Use Client", null, false, true, false, inUseClientAppUri);
        assertClientRep(apps.get("always-display-client"), "Always Display Client", null, false, false, false, alwaysDisplayClientAppUri);
    }

    @Test
    public void listApplicationsOfflineAccess() throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.clientId("offline-client");
        OAuthClient.AccessTokenResponse offlineTokenResponse = oauth.doGrantAccessTokenRequest("secret1", "view-applications-access", "password");
        Assert.assertNull(offlineTokenResponse.getErrorDescription());

        TokenUtil token = new TokenUtil("view-applications-access", "password");
        List<ClientRepresentation> applications = SimpleHttp
                .doGet(getAccountUrl("applications"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asJson(new TypeReference<List<ClientRepresentation>>() {
                });
        assertFalse(applications.isEmpty());

        Map<String, ClientRepresentation> apps = applications.stream().collect(Collectors.toMap(x -> x.getClientId(), x -> x));
        Assert.assertThat(apps.keySet(), containsInAnyOrder("offline-client", "always-display-client"));

        assertClientRep(apps.get("offline-client"), "Offline Client", null, false, true, true, offlineClientAppUri);
    }

    @Test
    public void listApplicationsThirdParty() throws Exception {
        String appId = "third-party";
        TokenUtil token = new TokenUtil("view-applications-access", "password");

        ClientScopeRepresentation clientScopeRepresentation = testRealm().clientScopes().findAll().get(0);
        ConsentScopeRepresentation consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        ConsentRepresentation requestedConsent = new ConsentRepresentation();
        requestedConsent.setGrantedScopes(Collections.singletonList(consentScopeRepresentation));
        SimpleHttp
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asJson(ConsentRepresentation.class);

        List<ClientRepresentation> applications = SimpleHttp
                .doGet(getAccountUrl("applications"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asJson(new TypeReference<List<ClientRepresentation>>() {
                });
        assertFalse(applications.isEmpty());

        SimpleHttp
                .doDelete(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asResponse();

        Map<String, ClientRepresentation> apps = applications.stream().collect(Collectors.toMap(x -> x.getClientId(), x -> x));
        Assert.assertThat(apps.keySet(), containsInAnyOrder(appId, "always-display-client"));

        ClientRepresentation app = apps.get(appId);
        assertClientRep(app, null, "A third party application", true, false, false, "http://localhost:8180/auth/realms/master/app/auth");
        assertFalse(app.getConsent().getGrantedScopes().isEmpty());
        ConsentScopeRepresentation grantedScope = app.getConsent().getGrantedScopes().get(0);
        assertEquals(clientScopeRepresentation.getId(), grantedScope.getId());
        assertEquals(clientScopeRepresentation.getName(), grantedScope.getName());
    }

    private void assertClientRep(ClientRepresentation clientRep, String name, String description, boolean userConsentRequired, boolean inUse, boolean offlineAccess, String baseUrl) {
        assertNotNull(clientRep);
        assertEquals(name, clientRep.getClientName());
        assertEquals(description, clientRep.getDescription());
        assertEquals(userConsentRequired, clientRep.isUserConsentRequired());
        assertEquals(inUse, clientRep.isInUse());
        assertEquals(offlineAccess, clientRep.isOfflineAccess());
        assertEquals(baseUrl, clientRep.getBaseUrl());
    }

    @Test
    public void listApplicationsWithoutPermission() throws IOException {
        TokenUtil token = new TokenUtil("no-account-access", "password");
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
        TokenUtil token = new TokenUtil("no-account-access", "password");
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
        requestedConsent.setGrantedScopes(Collections.singletonList(consentScopeRepresentation));

        ConsentRepresentation consentRepresentation = SimpleHttp
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation.getCreatedDate() > 0);
        assertTrue(consentRepresentation.getLastUpdatedDate() > 0);
        assertEquals(1, consentRepresentation.getGrantedScopes().size());
        assertEquals(consentScopeRepresentation.getId(), consentRepresentation.getGrantedScopes().get(0).getId());
    }

    @Test
    public void updateConsentForClient() throws IOException {
        TokenUtil token = new TokenUtil("manage-consent-access", "password");
        String appId = "security-admin-console";

        ClientScopeRepresentation clientScopeRepresentation = testRealm().clientScopes().findAll().get(0);
        ConsentScopeRepresentation consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        ConsentRepresentation requestedConsent = new ConsentRepresentation();
        requestedConsent.setGrantedScopes(Collections.singletonList(consentScopeRepresentation));

        ConsentRepresentation consentRepresentation = SimpleHttp
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation.getCreatedDate() > 0);
        assertTrue(consentRepresentation.getLastUpdatedDate() > 0);
        assertEquals(1, consentRepresentation.getGrantedScopes().size());
        assertEquals(consentScopeRepresentation.getId(), consentRepresentation.getGrantedScopes().get(0).getId());

        clientScopeRepresentation = testRealm().clientScopes().findAll().get(1);
        consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        requestedConsent = new ConsentRepresentation();
        requestedConsent.setGrantedScopes(Collections.singletonList(consentScopeRepresentation));

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
        assertEquals(1, consentRepresentation2.getGrantedScopes().size());
        assertEquals(consentScopeRepresentation.getId(), consentRepresentation2.getGrantedScopes().get(0).getId());
    }

    @Test
    public void createConsentForNotExistingClient() throws IOException {
        TokenUtil token = new TokenUtil("manage-consent-access", "password");
        String appId = "not-existing";

        ClientScopeRepresentation clientScopeRepresentation = testRealm().clientScopes().findAll().get(0);
        ConsentScopeRepresentation consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        ConsentRepresentation requestedConsent = new ConsentRepresentation();
        requestedConsent.setGrantedScopes(Collections.singletonList(consentScopeRepresentation));

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
        requestedConsent.setGrantedScopes(Collections.singletonList(consentScopeRepresentation));

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
        requestedConsent.setGrantedScopes(Collections.singletonList(consentScopeRepresentation));

        ConsentRepresentation consentRepresentation = SimpleHttp
                .doPut(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation.getCreatedDate() > 0);
        assertTrue(consentRepresentation.getLastUpdatedDate() > 0);
        assertEquals(1, consentRepresentation.getGrantedScopes().size());
        assertEquals(consentScopeRepresentation.getId(), consentRepresentation.getGrantedScopes().get(0).getId());
    }

    @Test
    public void updateConsentForClientWithPut() throws IOException {
        TokenUtil token = new TokenUtil("manage-consent-access", "password");
        String appId = "security-admin-console";

        ClientScopeRepresentation clientScopeRepresentation = testRealm().clientScopes().findAll().get(0);
        ConsentScopeRepresentation consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        ConsentRepresentation requestedConsent = new ConsentRepresentation();
        requestedConsent.setGrantedScopes(Collections.singletonList(consentScopeRepresentation));

        ConsentRepresentation consentRepresentation = SimpleHttp
                .doPut(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation.getCreatedDate() > 0);
        assertTrue(consentRepresentation.getLastUpdatedDate() > 0);
        assertEquals(1, consentRepresentation.getGrantedScopes().size());
        assertEquals(consentScopeRepresentation.getId(), consentRepresentation.getGrantedScopes().get(0).getId());

        clientScopeRepresentation = testRealm().clientScopes().findAll().get(1);
        consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        requestedConsent = new ConsentRepresentation();
        requestedConsent.setGrantedScopes(Collections.singletonList(consentScopeRepresentation));

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
        assertEquals(1, consentRepresentation2.getGrantedScopes().size());
        assertEquals(consentScopeRepresentation.getId(), consentRepresentation2.getGrantedScopes().get(0).getId());
    }

    @Test
    public void createConsentForNotExistingClientWithPut() throws IOException {
        TokenUtil token = new TokenUtil("manage-consent-access", "password");
        String appId = "not-existing";

        ClientScopeRepresentation clientScopeRepresentation = testRealm().clientScopes().findAll().get(0);
        ConsentScopeRepresentation consentScopeRepresentation = new ConsentScopeRepresentation();
        consentScopeRepresentation.setId(clientScopeRepresentation.getId());

        ConsentRepresentation requestedConsent = new ConsentRepresentation();
        requestedConsent.setGrantedScopes(Collections.singletonList(consentScopeRepresentation));

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
        requestedConsent.setGrantedScopes(Collections.singletonList(consentScopeRepresentation));

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
        requestedConsent.setGrantedScopes(Collections.singletonList(consentScopeRepresentation));

        ConsentRepresentation consentRepresentation1 = SimpleHttp
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation1.getCreatedDate() > 0);
        assertTrue(consentRepresentation1.getLastUpdatedDate() > 0);
        assertEquals(1, consentRepresentation1.getGrantedScopes().size());
        assertEquals(consentScopeRepresentation.getId(), consentRepresentation1.getGrantedScopes().get(0).getId());

        ConsentRepresentation consentRepresentation2 = SimpleHttp
                .doGet(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asJson(ConsentRepresentation.class);
        assertEquals(consentRepresentation1.getLastUpdatedDate(), consentRepresentation2.getLastUpdatedDate());
        assertEquals(consentRepresentation1.getCreatedDate(), consentRepresentation2.getCreatedDate());
        assertEquals(consentRepresentation1.getGrantedScopes().get(0).getId(), consentRepresentation2.getGrantedScopes().get(0).getId());
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
        TokenUtil token = new TokenUtil("no-account-access", "password");
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
        requestedConsent.setGrantedScopes(Collections.singletonList(consentScopeRepresentation));

        ConsentRepresentation consentRepresentation = SimpleHttp
                .doPost(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .json(requestedConsent)
                .auth(token.getToken())
                .asJson(ConsentRepresentation.class);
        assertTrue(consentRepresentation.getCreatedDate() > 0);
        assertTrue(consentRepresentation.getLastUpdatedDate() > 0);
        assertEquals(1, consentRepresentation.getGrantedScopes().size());
        assertEquals(consentScopeRepresentation.getId(), consentRepresentation.getGrantedScopes().get(0).getId());

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
