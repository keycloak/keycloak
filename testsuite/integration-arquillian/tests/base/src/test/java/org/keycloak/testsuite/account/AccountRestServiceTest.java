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
import org.keycloak.common.Profile;
import org.keycloak.common.enums.AccountRestApiVersion;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.credential.CredentialTypeMetadata;
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
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.account.AccountCredentialResource;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.admin.authentication.AbstractAuthenticationTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.TokenUtil;
import org.keycloak.testsuite.util.UserBuilder;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
@EnableFeature(value = Profile.Feature.WEB_AUTHN, skipRestart = true, onlyForProduct = true)
public class AccountRestServiceTest extends AbstractRestServiceTest {

    @Test
    public void testGetProfile() throws IOException {

        UserRepresentation user = getUser();
        assertEquals("Tom", user.getFirstName());
        assertEquals("Brady", user.getLastName());
        assertEquals("test-user@localhost", user.getEmail());
        assertFalse(user.isEmailVerified());
        assertTrue(user.getAttributes().isEmpty());
    }

    @Test
    public void testUpdateSingleField() throws IOException {
        UserRepresentation user = getUser();
        String originalUsername = user.getUsername();
        String originalFirstName = user.getFirstName();
        String originalLastName = user.getLastName();
        String originalEmail = user.getEmail();
        Map<String, List<String>> originalAttributes = new HashMap<>(user.getAttributes());

        try {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();

            realmRep.setRegistrationEmailAsUsername(false);
            adminClient.realm("test").update(realmRep);

            user.setFirstName(null);
            user.setLastName("Bob");
            user.setEmail(null);
            user.getAttributes().clear();

            user = updateAndGet(user);

            assertEquals(user.getLastName(), "Bob");
            assertNull(user.getFirstName());
            assertNull(user.getEmail());

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
            assertEquals(204, response.getStatus());
        }

    }
    
    /**
     * Reproducer for bugs KEYCLOAK-17424 and KEYCLOAK-17582
     */
    @Test
    public void testUpdateProfileEmailChangeSetsEmailVerified() throws IOException {
        UserRepresentation user = getUser();
        String originalEmail = user.getEmail();
        try {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();

            realmRep.setRegistrationEmailAsUsername(false);
            adminClient.realm("test").update(realmRep);
            
            //set flag over adminClient to initial value
            UserResource userResource = adminClient.realm("test").users().get(user.getId());
            org.keycloak.representations.idm.UserRepresentation ur = userResource.toRepresentation();
            ur.setEmailVerified(true);
            userResource.update(ur);
            //make sure flag is correct before the test 
            user = getUser();
            assertEquals(true, user.isEmailVerified());

            // Update without email change - flag not reset to false
            user.setEmail(originalEmail);
            user = updateAndGet(user);
            assertEquals(originalEmail, user.getEmail());
            assertEquals(true, user.isEmailVerified());

            
            // Update email - flag must be reset to false
            user.setEmail("bobby@localhost");
            user = updateAndGet(user);
            assertEquals("bobby@localhost", user.getEmail());
            assertEquals(false, user.isEmailVerified());

        } finally {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
            realmRep.setEditUsernameAllowed(true);
            adminClient.realm("test").update(realmRep);

            user.setEmail(originalEmail);
            SimpleHttp.Response response = SimpleHttp.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asResponse();
            System.out.println(response.asString());
            assertEquals(204, response.getStatus());
        }

    }

    @Test
    public void testUpdateProfile() throws IOException {
        UserRepresentation user = getUser();
        String originalUsername = user.getUsername();
        String originalFirstName = user.getFirstName();
        String originalLastName = user.getLastName();
        String originalEmail = user.getEmail();
        Map<String, List<String>> originalAttributes = new HashMap<>(user.getAttributes());

        try {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();

            realmRep.setRegistrationEmailAsUsername(false);
            adminClient.realm("test").update(realmRep);

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

            user.setUsername("john-doh@localhost");
            updateError(user, 409, Messages.USERNAME_EXISTS);

            user.setUsername("test-user@localhost");
            user = updateAndGet(user);
            assertEquals("test-user@localhost", user.getUsername());


            realmRep.setRegistrationEmailAsUsername(true);
            adminClient.realm("test").update(realmRep);

            user.setUsername("updatedUsername");
            user = updateAndGet(user);
            assertEquals("test-user@localhost", user.getUsername());

            realmRep.setRegistrationEmailAsUsername(false);
            adminClient.realm("test").update(realmRep);

            user.setUsername("updatedUsername");
            user = updateAndGet(user);
            assertEquals("updatedusername", user.getUsername());


            realmRep.setEditUsernameAllowed(false);
            realmRep.setRegistrationEmailAsUsername(false);
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
            assertEquals(204, response.getStatus());
        }

    }

    @Test
    public void testUpdateProfileCannotChangeThroughAttributes() throws IOException {
        UserRepresentation user = getUser();
        String originalUsername = user.getUsername();
        Map<String, List<String>> originalAttributes = new HashMap<>(user.getAttributes());

        try {
            user.getAttributes().put("username", Collections.singletonList("Username"));
            user.getAttributes().put("attr2", Collections.singletonList("val2"));

            user = updateAndGet(user);

            assertEquals(user.getUsername(), originalUsername);
        } finally {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
            realmRep.setEditUsernameAllowed(true);
            adminClient.realm("test").update(realmRep);

            user.setUsername(originalUsername);
            user.setAttributes(originalAttributes);
            SimpleHttp.Response response = SimpleHttp.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asResponse();
            System.out.println(response.asString());
            assertEquals(204, response.getStatus());
        }
    }

    // KEYCLOAK-7572
    @Test
    public void testUpdateProfileWithRegistrationEmailAsUsername() throws IOException {
        RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
        realmRep.setRegistrationEmailAsUsername(true);
        adminClient.realm("test").update(realmRep);

        UserRepresentation user = getUser();
        String originalFirstname = user.getFirstName();

        try {
            user.setFirstName("Homer1");

            user = updateAndGet(user);

            assertEquals("Homer1", user.getFirstName());
        } finally {
            user.setFirstName(originalFirstname);
            int status = SimpleHttp.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asStatus();
            assertEquals(204, status);
        }
    }

    private UserRepresentation getUser() throws IOException {
        return SimpleHttp.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
    }
    
    private UserRepresentation updateAndGet(UserRepresentation user) throws IOException {
        int status = SimpleHttp.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asStatus();
        assertEquals(204, status);
        return getUser();
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
    public void testUpdateProfilePermissions() throws IOException {
        TokenUtil noaccessToken = new TokenUtil("no-account-access", "password");
        int status = SimpleHttp.doGet(getAccountUrl(null), httpClient).header("Accept", "application/json").auth(noaccessToken.getToken()).asStatus();
        assertEquals(403, status);

        TokenUtil viewToken = new TokenUtil("view-account-access", "password");
        status = SimpleHttp.doGet(getAccountUrl(null), httpClient).header("Accept", "application/json").auth(viewToken.getToken()).asStatus();
        assertEquals(200, status);
    }

    @Test
    public void testCredentialsGet() throws IOException {
        configureBrowserFlowWithWebAuthnAuthenticator("browser-webauthn");

        // Register requiredActions for WebAuthn and WebAuthn Passwordless
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

        List<AccountCredentialResource.CredentialContainer> credentials = getCredentials();

        Assert.assertEquals(4, credentials.size());

        AccountCredentialResource.CredentialContainer password = credentials.get(0);
        assertCredentialContainerExpected(password, PasswordCredentialModel.TYPE, CredentialTypeMetadata.Category.BASIC_AUTHENTICATION.toString(),
                "password-display-name", "password-help-text", "kcAuthenticatorPasswordClass",
                null, UserModel.RequiredAction.UPDATE_PASSWORD.toString(), false, 1);

        CredentialRepresentation password1 = password.getUserCredentials().get(0);
        assertNull(password1.getSecretData());
        Assert.assertNotNull(password1.getCredentialData());

        AccountCredentialResource.CredentialContainer otp = credentials.get(1);
        assertCredentialContainerExpected(otp, OTPCredentialModel.TYPE, CredentialTypeMetadata.Category.TWO_FACTOR.toString(),
                "otp-display-name", "otp-help-text", "kcAuthenticatorOTPClass",
                UserModel.RequiredAction.CONFIGURE_TOTP.toString(), null, true, 0);

        // WebAuthn credentials will be returned, but createAction will be still null because requiredAction "webauthn register" not yet registered
        AccountCredentialResource.CredentialContainer webauthn = credentials.get(2);
        assertCredentialContainerExpected(webauthn, WebAuthnCredentialModel.TYPE_TWOFACTOR, CredentialTypeMetadata.Category.TWO_FACTOR.toString(),
                "webauthn-display-name", "webauthn-help-text", "kcAuthenticatorWebAuthnClass",
                WebAuthnRegisterFactory.PROVIDER_ID, null, true, 0);

        AccountCredentialResource.CredentialContainer webauthnPasswordless = credentials.get(3);
        assertCredentialContainerExpected(webauthnPasswordless, WebAuthnCredentialModel.TYPE_PASSWORDLESS, CredentialTypeMetadata.Category.PASSWORDLESS.toString(),
                "webauthn-passwordless-display-name", "webauthn-passwordless-help-text", "kcAuthenticatorWebAuthnPasswordlessClass",
                WebAuthnPasswordlessRegisterFactory.PROVIDER_ID, null, true, 0);

        // disable WebAuthn passwordless required action. User doesn't have WebAuthnPasswordless credential, so WebAuthnPasswordless credentialType won't be returned
        setRequiredActionEnabledStatus(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID, false);

        credentials = getCredentials();
        assertExpectedCredentialTypes(credentials, PasswordCredentialModel.TYPE, OTPCredentialModel.TYPE, WebAuthnCredentialModel.TYPE_TWOFACTOR);

        // Test that WebAuthn won't be returned when removed from the authentication flow
        removeWebAuthnFlow("browser-webauthn");

        credentials = getCredentials();

        assertExpectedCredentialTypes(credentials, PasswordCredentialModel.TYPE, OTPCredentialModel.TYPE);

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
        assertNull(password.getUserCredentials());
    }


    @Test
    public void testCRUDCredentialOfDifferentUser() throws IOException {
        // Get credential ID of the OTP credential of the different user thant currently logged user
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "user-with-one-configured-otp");
        CredentialRepresentation otpCredential = user.credentials().stream()
                .filter(credentialRep -> OTPCredentialModel.TYPE.equals(credentialRep.getType()))
                .findFirst()
                .get();

        // Test that current user can't update the credential, which belongs to the different user
        SimpleHttp.Response response = SimpleHttp
                .doPut(getAccountUrl("credentials/" + otpCredential.getId() + "/label"), httpClient)
                .auth(tokenUtil.getToken())
                .json("new-label")
                .asResponse();
        assertEquals(404, response.getStatus());

        // Test that current user can't delete the credential, which belongs to the different user
        response = SimpleHttp
                .doDelete(getAccountUrl("credentials/" + otpCredential.getId()), httpClient)
                .acceptJson()
                .auth(tokenUtil.getToken())
                .asResponse();
        assertEquals(404, response.getStatus());

        // Assert credential was not updated or removed
        CredentialRepresentation otpCredentialLoaded = user.credentials().stream()
                .filter(credentialRep -> OTPCredentialModel.TYPE.equals(credentialRep.getType()))
                .findFirst()
                .get();
        Assert.assertTrue(ObjectUtil.isEqualOrBothNull(otpCredential.getUserLabel(), otpCredentialLoaded.getUserLabel()));
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
    public void testCredentialsGetWithDisabledOtpRequiredAction() throws IOException {
        // Assert OTP will be returned by default
        List<AccountCredentialResource.CredentialContainer> credentials = getCredentials();
        assertExpectedCredentialTypes(credentials, PasswordCredentialModel.TYPE, OTPCredentialModel.TYPE);

        // Disable OTP required action
        setRequiredActionEnabledStatus(UserModel.RequiredAction.CONFIGURE_TOTP.name(), false);

        // Assert OTP won't be returned
        credentials = getCredentials();
        assertExpectedCredentialTypes(credentials, PasswordCredentialModel.TYPE);

        // Add OTP credential to the user through admin REST API
        UserResource adminUserResource = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        org.keycloak.representations.idm.UserRepresentation userRep = UserBuilder.edit(adminUserResource.toRepresentation())
                .totpSecret("abcdefabcdef")
                .build();
        adminUserResource.update(userRep);

        // Assert OTP will be returned without requiredAction
        credentials = getCredentials();
        assertExpectedCredentialTypes(credentials, PasswordCredentialModel.TYPE, OTPCredentialModel.TYPE);
        AccountCredentialResource.CredentialContainer otpCredential = credentials.get(1);
        assertNull(otpCredential.getCreateAction());
        assertNull(otpCredential.getUpdateAction());

        // Revert - re-enable requiredAction and remove OTP credential from the user
        setRequiredActionEnabledStatus(UserModel.RequiredAction.CONFIGURE_TOTP.name(), true);

        String otpCredentialId = adminUserResource.credentials().stream()
                .filter(credential -> OTPCredentialModel.TYPE.equals(credential.getType()))
                .findFirst()
                .get()
                .getId();
        adminUserResource.removeCredential(otpCredentialId);
    }

    private void setRequiredActionEnabledStatus(String requiredActionProviderId, boolean enabled) {
        RequiredActionProviderRepresentation requiredActionRep = testRealm().flows().getRequiredAction(requiredActionProviderId);
        requiredActionRep.setEnabled(enabled);
        testRealm().flows().updateRequiredAction(requiredActionProviderId, requiredActionRep);
    }

    private void assertExpectedCredentialTypes(List<AccountCredentialResource.CredentialContainer> credentialTypes, String... expectedCredentialTypes) {
        Assert.assertEquals(credentialTypes.size(), expectedCredentialTypes.length);
        int i = 0;
        for (AccountCredentialResource.CredentialContainer credential : credentialTypes) {
            Assert.assertEquals(credential.getType(), expectedCredentialTypes[i]);
            i++;
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
        assertNull(tokenResponse.getErrorDescription());

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

        assertClientRep(apps.get("in-use-client"), "In Use Client", null, false, true, false, null, inUseClientAppUri);
        assertClientRep(apps.get("always-display-client"), "Always Display Client", null, false, false, false, null, alwaysDisplayClientAppUri);
    }

    @Test
    public void listApplicationsFiltered() throws Exception {
        oauth.clientId("in-use-client");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("secret1", "view-applications-access", "password");
        assertNull(tokenResponse.getErrorDescription());

        TokenUtil token = new TokenUtil("view-applications-access", "password");
        List<ClientRepresentation> applications = SimpleHttp
                .doGet(getAccountUrl("applications"), httpClient)
                .header("Accept", "application/json")
                .param("name", "In Use")
                .auth(token.getToken())
                .asJson(new TypeReference<List<ClientRepresentation>>() {
                });
        assertFalse(applications.isEmpty());

        Map<String, ClientRepresentation> apps = applications.stream().collect(Collectors.toMap(x -> x.getClientId(), x -> x));
        Assert.assertThat(apps.keySet(), containsInAnyOrder("in-use-client"));

        assertClientRep(apps.get("in-use-client"), "In Use Client", null, false, true, false, null, inUseClientAppUri);
    }

    @Test
    public void listApplicationsOfflineAccess() throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.clientId("offline-client");
        OAuthClient.AccessTokenResponse offlineTokenResponse = oauth.doGrantAccessTokenRequest("secret1", "view-applications-access", "password");
        assertNull(offlineTokenResponse.getErrorDescription());

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

        assertClientRep(apps.get("offline-client"), "Offline Client", null, false, true, true, null, offlineClientAppUri);
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
        assertClientRep(app, null, "A third party application", true, false, false, null, "http://localhost:8180/auth/realms/master/app/auth");
        assertFalse(app.getConsent().getGrantedScopes().isEmpty());
        ConsentScopeRepresentation grantedScope = app.getConsent().getGrantedScopes().get(0);
        assertEquals(clientScopeRepresentation.getId(), grantedScope.getId());
        assertEquals(clientScopeRepresentation.getName(), grantedScope.getName());
    }

    @Test
    public void listApplicationsWithRootUrl() throws Exception {
        oauth.clientId("root-url-client");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("password", "view-applications-access", "password");
        assertNull(tokenResponse.getErrorDescription());

        TokenUtil token = new TokenUtil("view-applications-access", "password");
        List<ClientRepresentation> applications = SimpleHttp
                .doGet(getAccountUrl("applications"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asJson(new TypeReference<List<ClientRepresentation>>() {
                });
        assertFalse(applications.isEmpty());

        Map<String, ClientRepresentation> apps = applications.stream().collect(Collectors.toMap(x -> x.getClientId(), x -> x));
        Assert.assertThat(apps.keySet(), containsInAnyOrder("root-url-client", "always-display-client"));

        assertClientRep(apps.get("root-url-client"), null, null, false, true, false, "http://localhost:8180/foo/bar", "/baz");
    }

    private void assertClientRep(ClientRepresentation clientRep, String name, String description, boolean userConsentRequired, boolean inUse, boolean offlineAccess, String rootUrl, String baseUrl) {
        assertNotNull(clientRep);
        assertEquals(name, clientRep.getClientName());
        assertEquals(description, clientRep.getDescription());
        assertEquals(userConsentRequired, clientRep.isUserConsentRequired());
        assertEquals(inUse, clientRep.isInUse());
        assertEquals(offlineAccess, clientRep.isOfflineAccess());
        assertEquals(rootUrl, clientRep.getRootUrl());
        assertEquals(baseUrl, clientRep.getBaseUrl());
        assertEquals(ResolveRelative.resolveRelativeUri(null, null, rootUrl, baseUrl), clientRep.getEffectiveUrl());
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
        assertEquals(204, response.getStatus());

        response = SimpleHttp
                .doDelete(getAccountUrl("applications/" + appId + "/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asResponse();
        assertEquals(204, response.getStatus());
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

    //KEYCLOAK-14344
    @Test
    public void revokeOfflineAccess() throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.clientId("offline-client");
        OAuthClient.AccessTokenResponse offlineTokenResponse = oauth.doGrantAccessTokenRequest("secret1", "view-applications-access", "password");
        assertNull(offlineTokenResponse.getErrorDescription());

        TokenUtil token = new TokenUtil("view-applications-access", "password");

        SimpleHttp.Response response = SimpleHttp
                .doDelete(getAccountUrl("applications/offline-client/consent"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asResponse();
        assertEquals(204, response.getStatus());

        List<ClientRepresentation> applications = SimpleHttp
                .doGet(getAccountUrl("applications"), httpClient)
                .header("Accept", "application/json")
                .auth(token.getToken())
                .asJson(new TypeReference<List<ClientRepresentation>>() {
                });
        assertFalse(applications.isEmpty());

        Map<String, ClientRepresentation> apps = applications.stream().collect(Collectors.toMap(x -> x.getClientId(), x -> x));
        Assert.assertThat(apps.keySet(), containsInAnyOrder("offline-client", "always-display-client"));

        assertClientRep(apps.get("offline-client"), "Offline Client", null, false, true, false, null, offlineClientAppUri);
    }

    @Test
    public void testApiVersion() throws IOException {
        apiVersion = AccountRestApiVersion.DEFAULT.getStrVersion();

        // a smoke test to check API with version works
        testUpdateProfile(); // profile endpoint is the root URL of account REST service, i.e. the URL will be like "/v1/"
        testCredentialsGet(); // "/v1/credentials"
    }

    @Test
    public void testInvalidApiVersion() throws IOException {
        apiVersion = "v2-foo";

        SimpleHttp.Response response = SimpleHttp.doGet(getAccountUrl("credentials"), httpClient).auth(tokenUtil.getToken()).asResponse();
        assertEquals("API version not found", response.asJson().get("error").textValue());
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testAudience() throws Exception {
        oauth.clientId("custom-audience");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");
        assertNull(tokenResponse.getErrorDescription());

        SimpleHttp.Response response = SimpleHttp.doGet(getAccountUrl(null), httpClient)
                .auth(tokenResponse.getAccessToken())
                .header("Accept", "application/json")
                .asResponse();
        assertEquals(401, response.getStatus());

        // update to correct audience
        org.keycloak.representations.idm.ClientRepresentation clientRep = testRealm().clients().findByClientId("custom-audience").get(0);
        ProtocolMapperRepresentation mapperRep = clientRep.getProtocolMappers().stream().filter(m -> m.getName().equals("aud")).findFirst().orElse(null);
        assertNotNull("Audience mapper not found", mapperRep);
        mapperRep.getConfig().put("included.custom.audience", "account");
        testRealm().clients().get(clientRep.getId()).getProtocolMappers().update(mapperRep.getId(), mapperRep);

        tokenResponse = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");
        assertNull(tokenResponse.getErrorDescription());

        response = SimpleHttp.doGet(getAccountUrl(null), httpClient)
                .auth(tokenResponse.getAccessToken())
                .header("Accept", "application/json")
                .asResponse();
        assertEquals(200, response.getStatus());

        // remove audience completely
        testRealm().clients().get(clientRep.getId()).getProtocolMappers().delete(mapperRep.getId());

        tokenResponse = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");
        assertNull(tokenResponse.getErrorDescription());

        response = SimpleHttp.doGet(getAccountUrl(null), httpClient)
                .auth(tokenResponse.getAccessToken())
                .header("Accept", "application/json")
                .asResponse();
        assertEquals(401, response.getStatus());

        // custom-audience client is used only in this test so no need to revert the changes
    }
}
