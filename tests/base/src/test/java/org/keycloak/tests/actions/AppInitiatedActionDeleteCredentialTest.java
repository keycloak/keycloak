/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.tests.actions;

import java.util.List;

import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.requiredactions.DeleteCredentialAction;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.events.email.EmailEventListenerProviderFactory;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.DeleteCredentialPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginConfigTotpPage;
import org.keycloak.testframework.ui.page.LoginTotpPage;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.LegacyRealmConfig;
import org.keycloak.testsuite.admin.AdminApiUtil;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.MailUtils;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
public class AppInitiatedActionDeleteCredentialTest extends AbstractAppInitiatedActionTest {

    @InjectRealm(config = AppInitiatedActionDeleteCredentialRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectMailServer
    MailServer mail;

    @Override
    protected String getAiaAction() {
        return DeleteCredentialAction.PROVIDER_ID;
    }

    @InjectPage
    protected LoginTotpPage loginTotpPage;

    @InjectPage
    protected LoginConfigTotpPage totpPage;

    @InjectPage
    protected DeleteCredentialPage deleteCredentialPage;

    @InjectPage
    protected ErrorPage errorPage;

    protected TimeBasedOTP totp = new TimeBasedOTP();

    private String userId;

    @BeforeEach
    public void beforeTest() {
        AdminApiUtil.removeUserByUsername(managedRealm.admin(), "test-user@localhost");
        UserRepresentation user = UserBuilder.create()
                .username("john")
                .email("test-user@localhost")
                .emailVerified(true)
                .firstName("John")
                .lastName("Bar")
                .enabled(true)
                .password("password")
                .totpSecret("mySecret").build();
        Response response = managedRealm.admin().users().create(user);
        userId = ApiUtil.getCreatedId(response);
        response.close();
        getCleanup().addUserId(userId);
    }

    @Test
    public void removeOtpSuccess() throws Exception {
        try (RealmAttributeUpdater updater = new RealmAttributeUpdater(managedRealm.admin())
                .addEventsListener(EmailEventListenerProviderFactory.ID)
                .update()) {

            String credentialId = getCredentialIdByType(OTPCredentialModel.TYPE);
            loginPasswordAndOtp(getKcActionParamForDeleteCredential(credentialId));

            deleteCredentialPage.assertCurrent();
            deleteCredentialPage.assertCredentialInMessage(OTPCredentialModel.TYPE);

            deleteCredentialPage.confirm();

            Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
            assertKcActionStatus("success");

            Assertions.assertNull(getCredentialIdByType(OTPCredentialModel.TYPE));

            EventAssertion.assertSuccess(events.poll()).type(EventType.REMOVE_TOTP)
                    .userId(userId)
                    .details(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE)
                    .details(Details.CREDENTIAL_ID, credentialId)
                    .details(Details.CUSTOM_REQUIRED_ACTION, DeleteCredentialAction.PROVIDER_ID);
            EventAssertion.assertSuccess(events.poll()).type(EventType.REMOVE_CREDENTIAL)
                    .userId(userId)
                    .details(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE)
                    .details(Details.CREDENTIAL_ID, credentialId)
                    .details(Details.CUSTOM_REQUIRED_ACTION, DeleteCredentialAction.PROVIDER_ID);

            MimeMessage[] receivedMessages = mail.getReceivedMessages();
            Assertions.assertEquals(2, receivedMessages.length);

            Assertions.assertEquals("Remove OTP", receivedMessages[0].getSubject());
            Assertions.assertEquals("Remove credential", receivedMessages[1].getSubject());
            MatcherAssert.assertThat(MailUtils.getBody(receivedMessages[1]).getText(),
                    Matchers.startsWith("Credential otp was removed from your account"));
            MatcherAssert.assertThat(MailUtils.getBody(receivedMessages[1]).getHtml(),
                    Matchers.containsString("Credential otp was removed from your account"));
        }
    }

    @Test
    public void removeOtpCancel() throws Exception {
        String credentialId = getCredentialIdByType(OTPCredentialModel.TYPE);

        loginPasswordAndOtp(null);

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        events.clear();

        oauth.loginForm().kcAction(getKcActionParamForDeleteCredential(credentialId)).open();

        // Cancel on the confirmation page
        deleteCredentialPage.assertCurrent();
        deleteCredentialPage.assertCredentialInMessage(OTPCredentialModel.TYPE);
        deleteCredentialPage.cancel();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        Assertions.assertNotNull(getCredentialIdByType(OTPCredentialModel.TYPE));
    }

    @Test
    public void removePasswordShouldFail() throws Exception {
        String credentialId = getCredentialIdByType(PasswordCredentialModel.TYPE);
        loginPasswordAndOtp(null);

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        events.clear();

        oauth.loginForm().kcAction(getKcActionParamForDeleteCredential(credentialId)).open();

        // Cancel on the confirmation page
        deleteCredentialPage.assertCurrent();
        deleteCredentialPage.assertCredentialInMessage(PasswordCredentialModel.TYPE);
        deleteCredentialPage.confirm();

        errorPage.assertCurrent();

        EventAssertion.assertError(events.poll()).type(EventType.REMOVE_CREDENTIAL_ERROR)
                .userId(userId)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
                .details(Details.CREDENTIAL_ID, credentialId)
                .details(Details.CUSTOM_REQUIRED_ACTION, DeleteCredentialAction.PROVIDER_ID)
                .details(Details.REASON, "Credential type cannot be removed")
                .error(Errors.DELETE_CREDENTIAL_FAILED);
    }

    @Test
    public void missingActionId() throws Exception {
        loginPasswordAndOtp(null);

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        events.clear();

        oauth.loginForm().kcAction(DeleteCredentialAction.PROVIDER_ID).open();

        EventAssertion.assertError(events.poll()).type(EventType.CUSTOM_REQUIRED_ACTION_ERROR)
                .userId(userId)
                .error(Errors.MISSING_CREDENTIAL_ID);

        // Redirected to the application. Action will be ignored
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    public void incorrectId() throws Exception {
        loginPasswordAndOtp(null);

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        events.clear();

        oauth.loginForm().kcAction(getKcActionParamForDeleteCredential("incorrect")).open();

        // Redirected to the application. Action will be ignored
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        EventAssertion.assertError(events.poll()).type(EventType.CUSTOM_REQUIRED_ACTION_ERROR)
                .userId(userId)
                .details(Details.CREDENTIAL_ID, "incorrect")
                .error(Errors.CREDENTIAL_NOT_FOUND);
    }

    @Test
    public void requiredActionByAdmin() throws Exception {
        // Add required action by admin. It will be ignored as there is no credentialId
        UserRepresentation user = managedRealm.admin().users().get(userId).toRepresentation();
        user.setRequiredActions(List.of(DeleteCredentialAction.PROVIDER_ID));
        managedRealm.admin().users().get(userId).update(user);

        loginPasswordAndOtp(null);
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        EventAssertion.assertError(events.poll()).type(EventType.CUSTOM_REQUIRED_ACTION_ERROR)
                .userId(userId)
                .error(Errors.MISSING_CREDENTIAL_ID);
    }

    @Test
    public void removeOtpCustomLabel() throws Exception {
        String credentialId = getCredentialIdByType(OTPCredentialModel.TYPE);
        managedRealm.admin().users().get(userId).setCredentialUserLabel(credentialId, "custom-otp-authenticator");

        loginPasswordAndOtp(getKcActionParamForDeleteCredential(credentialId));

        deleteCredentialPage.assertCurrent();
        deleteCredentialPage.assertCredentialInMessage("custom-otp-authenticator");

        deleteCredentialPage.confirm();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        assertKcActionStatus("success");

        Assertions.assertNull(getCredentialIdByType(OTPCredentialModel.TYPE));

        EventAssertion.assertSuccess(events.poll()).type(EventType.REMOVE_TOTP)
                .userId(userId)
                .details(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE)
                .details(Details.CREDENTIAL_ID, credentialId)
                .details(Details.CREDENTIAL_USER_LABEL, "custom-otp-authenticator")
                .details(Details.CUSTOM_REQUIRED_ACTION, DeleteCredentialAction.PROVIDER_ID);
        EventAssertion.assertSuccess(events.poll()).type(EventType.REMOVE_CREDENTIAL)
                .userId(userId)
                .details(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE)
                .details(Details.CREDENTIAL_ID, credentialId)
                .details(Details.CREDENTIAL_USER_LABEL, "custom-otp-authenticator")
                .details(Details.CUSTOM_REQUIRED_ACTION, DeleteCredentialAction.PROVIDER_ID);
    }

    private String getCredentialIdByType(String type) {
        List<CredentialRepresentation> credentials = managedRealm.admin().users().get(userId).credentials();
        return credentials.stream()
                .filter(credential -> type.equals(credential.getType()))
                .findFirst()
                .map(CredentialRepresentation::getId)
                .orElse(null);
    }

    public static String getKcActionParamForDeleteCredential(String credentialId) {
        return DeleteCredentialAction.PROVIDER_ID + ":" + credentialId;
    }

    private void loginPasswordAndOtp(String kcAction) {
        oauth.loginForm().kcAction(kcAction).open();
        loginPage.login("john", "password");
        loginTotpPage.assertCurrent();
        loginTotpPage.login(totp.generateTOTP("mySecret"));
    }


    private static class AppInitiatedActionDeleteCredentialRealmConfig extends LegacyRealmConfig {

        @Override
        public void configureTestRealm(RealmRepresentation testRealm) {
            testRealm.setResetPasswordAllowed(Boolean.TRUE);
        }
    }
}
