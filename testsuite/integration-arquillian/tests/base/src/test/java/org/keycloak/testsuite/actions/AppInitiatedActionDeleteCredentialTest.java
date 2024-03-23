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

package org.keycloak.testsuite.actions;

import java.util.List;

import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.authentication.requiredactions.DeleteCredentialAction;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.DeleteCredentialPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AppInitiatedActionDeleteCredentialTest extends AbstractAppInitiatedActionTest {

    @Override
    protected String getAiaAction() {
        return DeleteCredentialAction.PROVIDER_ID;
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setResetPasswordAllowed(Boolean.TRUE);
    }

    @Page
    protected LoginTotpPage loginTotpPage;

    @Page
    protected LoginConfigTotpPage totpPage;

    @Page
    protected DeleteCredentialPage deleteCredentialPage;

    @Page
    protected ErrorPage errorPage;

    protected TimeBasedOTP totp = new TimeBasedOTP();

    private String userId;

    @Before
    public void beforeTest() {
        UserRepresentation user = UserBuilder.create()
                .username("john")
                .email("john@email.cz")
                .firstName("John")
                .lastName("Bar")
                .enabled(true)
                .password("password")
                .totpSecret("mySecret").build();
        Response response = testRealm().users().create(user);
        userId = ApiUtil.getCreatedId(response);
        response.close();
        getCleanup().addUserId(userId);
    }

    @Test
    public void removeTotpSuccess() throws Exception {
        String credentialId = getCredentialIdByType(OTPCredentialModel.TYPE);
        oauth.kcAction(getKcActionParamForDeleteCredential(credentialId));

        loginPasswordAndOtp();

        deleteCredentialPage.assertCurrent();
        deleteCredentialPage.assertCredentialInMessage(OTPCredentialModel.TYPE);

        deleteCredentialPage.confirm();

        appPage.assertCurrent();
        assertKcActionStatus("success");

        Assert.assertNull(getCredentialIdByType(OTPCredentialModel.TYPE));

        events.expect(EventType.REMOVE_TOTP)
                .user(userId)
                .detail(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE)
                .detail(Details.CREDENTIAL_ID, credentialId)
                .detail(Details.CUSTOM_REQUIRED_ACTION, DeleteCredentialAction.PROVIDER_ID)
                .assertEvent();
    }

    @Test
    public void removeTotpCancel() throws Exception {
        String credentialId = getCredentialIdByType(OTPCredentialModel.TYPE);

        loginPasswordAndOtp();

        appPage.assertCurrent();
        events.clear();

        oauth.kcAction(getKcActionParamForDeleteCredential(credentialId));
        oauth.openLoginForm();

        // Cancel on the confirmation page
        deleteCredentialPage.assertCurrent();
        deleteCredentialPage.assertCredentialInMessage(OTPCredentialModel.TYPE);
        deleteCredentialPage.cancel();

        appPage.assertCurrent();

        Assert.assertNotNull(getCredentialIdByType(OTPCredentialModel.TYPE));
    }

    @Test
    public void removePasswordShouldFail() throws Exception {
        String credentialId = getCredentialIdByType(PasswordCredentialModel.TYPE);
        loginPasswordAndOtp();

        appPage.assertCurrent();
        events.clear();

        oauth.kcAction(getKcActionParamForDeleteCredential(credentialId));
        oauth.openLoginForm();

        // Cancel on the confirmation page
        deleteCredentialPage.assertCurrent();
        deleteCredentialPage.assertCredentialInMessage(PasswordCredentialModel.TYPE);
        deleteCredentialPage.confirm();

        errorPage.assertCurrent();

        events.expect(EventType.CUSTOM_REQUIRED_ACTION)
                .user(userId)
                .detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE)
                .detail(Details.CREDENTIAL_ID, credentialId)
                .detail(Details.CUSTOM_REQUIRED_ACTION, DeleteCredentialAction.PROVIDER_ID)
                .detail(Details.REASON, "Credential type cannot be removed")
                .error(Errors.DELETE_CREDENTIAL_FAILED)
                .assertEvent();
    }

    @Test
    public void missingActionId() throws Exception {
        loginPasswordAndOtp();

        appPage.assertCurrent();
        events.clear();

        oauth.kcAction(DeleteCredentialAction.PROVIDER_ID);
        oauth.openLoginForm();

        events.expect(EventType.CUSTOM_REQUIRED_ACTION)
                .user(userId)
                .error(Errors.MISSING_CREDENTIAL_ID);

        // Redirected to the application. Action will be ignored
        appPage.assertCurrent();
    }

    @Test
    public void incorrectId() throws Exception {
        loginPasswordAndOtp();

        appPage.assertCurrent();
        events.clear();

        oauth.kcAction(getKcActionParamForDeleteCredential("incorrect"));
        oauth.openLoginForm();

        // Redirected to the application. Action will be ignored
        appPage.assertCurrent();

        events.expect(EventType.CUSTOM_REQUIRED_ACTION)
                .user(userId)
                .detail(Details.CREDENTIAL_ID, "incorrect")
                .error(Errors.CREDENTIAL_NOT_FOUND);
    }

    @Test
    public void requiredActionByAdmin() throws Exception {
        // Add required action by admin. It will be ignored as there is no credentialId
        UserRepresentation user = testRealm().users().get(userId).toRepresentation();
        user.setRequiredActions(List.of(DeleteCredentialAction.PROVIDER_ID));
        testRealm().users().get(userId).update(user);

        loginPasswordAndOtp();
        appPage.assertCurrent();

        events.expect(EventType.CUSTOM_REQUIRED_ACTION)
                .user(userId)
                .error(Errors.MISSING_CREDENTIAL_ID);
    }

    @Test
    public void removeTotpCustomLabel() throws Exception {
        String credentialId = getCredentialIdByType(OTPCredentialModel.TYPE);
        testRealm().users().get(userId).setCredentialUserLabel(credentialId, "custom-otp-authenticator");

        oauth.kcAction(getKcActionParamForDeleteCredential(credentialId));
        loginPasswordAndOtp();

        deleteCredentialPage.assertCurrent();
        deleteCredentialPage.assertCredentialInMessage("custom-otp-authenticator");

        deleteCredentialPage.confirm();

        appPage.assertCurrent();
        assertKcActionStatus("success");

        Assert.assertNull(getCredentialIdByType(OTPCredentialModel.TYPE));

        events.expect(EventType.REMOVE_TOTP)
                .user(userId)
                .detail(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE)
                .detail(Details.CREDENTIAL_ID, credentialId)
                .detail(Details.CREDENTIAL_USER_LABEL, "custom-otp-authenticator")
                .detail(Details.CUSTOM_REQUIRED_ACTION, DeleteCredentialAction.PROVIDER_ID)
                .assertEvent();
    }

    private String getCredentialIdByType(String type) {
        List<CredentialRepresentation> credentials = testRealm().users().get(userId).credentials();
        return credentials.stream()
                .filter(credential -> type.equals(credential.getType()))
                .findFirst()
                .map(CredentialRepresentation::getId)
                .orElse(null);
    }

    public static String getKcActionParamForDeleteCredential(String credentialId) {
        return DeleteCredentialAction.PROVIDER_ID + ":" + credentialId;
    }

    private void loginPasswordAndOtp() {
        oauth.openLoginForm();
        loginPage.login("john", "password");
        loginTotpPage.assertCurrent();
        loginTotpPage.login(totp.generateTOTP("mySecret"));
    }

}
