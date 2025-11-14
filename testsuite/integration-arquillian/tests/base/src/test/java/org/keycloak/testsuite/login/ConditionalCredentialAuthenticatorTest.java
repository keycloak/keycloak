/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.login;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.conditional.ConditionalCredentialAuthenticatorFactory;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.auth.page.login.OneTimeCode;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 *
 * @author rmartinc
 */
public class ConditionalCredentialAuthenticatorTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected LoginTotpPage loginTotpPage;

    @Page
    protected OneTimeCode oneTimeCodePage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        // setup normal otp policy but with reusable tokens
        testRealm.setOtpPolicyAlgorithm("HmacSHA1");
        testRealm.setOtpPolicyDigits(6);
        testRealm.setOtpPolicyInitialCounter(0);
        testRealm.setOtpPolicyLookAheadWindow(1);
        testRealm.setOtpPolicyPeriod(30);
        testRealm.setOtpPolicyType("totp");
        testRealm.setOtpPolicyCodeReusable(Boolean.TRUE);
    }

    @Test
    public void testPasswordIncluded() {
        configureConditionalCurrentCredentialFlow(Boolean.TRUE, PasswordCredentialModel.TYPE);

        // login with username password
        oauth.openLoginForm();
        oauth.fillLoginForm("user-with-one-configured-otp", "password");

        // 2FA with otp should be displayed
        loginTotpPage.assertCurrent();
        oneTimeCodePage.sendCode(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A"));
        checkLoginOk("user-with-one-configured-otp");
    }

    @Test
    public void testPasswordNotIncluded() {
        configureConditionalCurrentCredentialFlow(Boolean.FALSE, PasswordCredentialModel.TYPE);

        // login with username password
        oauth.openLoginForm();
        oauth.fillLoginForm("user-with-one-configured-otp", "password");

        // 2FA with otp should not be displayed
        checkLoginOk("user-with-one-configured-otp");
    }

    @Test
    public void testWebAuthnNotIncluded() {
        configureConditionalCurrentCredentialFlow(Boolean.FALSE, WebAuthnCredentialModel.TYPE_PASSWORDLESS);

        // login with username password
        oauth.openLoginForm();
        oauth.fillLoginForm("user-with-one-configured-otp", "password");

        // 2FA with otp should be displayed
        loginTotpPage.assertCurrent();
        oneTimeCodePage.sendCode(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A"));
        checkLoginOk("user-with-one-configured-otp");
    }

    @Test
    public void testPasswordAndWebAuthnNotIncluded() {
        configureConditionalCurrentCredentialFlow(Boolean.FALSE, WebAuthnCredentialModel.TYPE_PASSWORDLESS, PasswordCredentialModel.TYPE);

        // login with username password
        oauth.openLoginForm();
        oauth.fillLoginForm("user-with-one-configured-otp", "password");

        // 2FA with otp should not be displayed
        checkLoginOk("user-with-one-configured-otp");
    }

    @Test
    public void testNoConfig() {
        configureConditionalCurrentCredentialFlow(null);

        // login with username password
        oauth.openLoginForm();
        oauth.fillLoginForm("user-with-one-configured-otp", "password");

        // 2FA with otp should be displayed
        loginTotpPage.assertCurrent();
        oneTimeCodePage.sendCode(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A"));
        checkLoginOk("user-with-one-configured-otp");
    }

    @Test
    public void testNoneIncluded() {
        configureConditionalCurrentCredentialFlow(Boolean.TRUE, ConditionalCredentialAuthenticatorFactory.NONE_CREDENTIAL);

        // login with username password
        oauth.openLoginForm();
        oauth.fillLoginForm("user-with-one-configured-otp", "password");

        // 2FA with otp should not be displayed
        checkLoginOk("user-with-one-configured-otp");
    }

    @Test
    public void testNoneNotIncluded() {
        configureConditionalCurrentCredentialFlow(Boolean.FALSE, ConditionalCredentialAuthenticatorFactory.NONE_CREDENTIAL);

        // login with username password
        oauth.openLoginForm();
        oauth.fillLoginForm("user-with-one-configured-otp", "password");

        // 2FA with otp should be displayed
        loginTotpPage.assertCurrent();
        oneTimeCodePage.sendCode(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A"));
        checkLoginOk("user-with-one-configured-otp");
    }

    private void configureConditionalCurrentCredentialFlow(Boolean included, String... credentials) {
        // clone the browser flow and add the current credential condition in the 2FA section

        RealmResource realmRes = testRealm();
        AuthenticationManagementResource authRes = realmRes.flows();

        // revert the flows if already changed
        RealmRepresentation realmRep = realmRes.toRepresentation();
        if (!realmRep.getBrowserFlow().equals("browser")) {
            realmRep.setBrowserFlow("browser");
            realmRes.update(realmRep);
            authRes.deleteFlow(authRes.getFlows().stream().filter(f -> "test".equals(f.getAlias())).findAny().get().getId());
        }

        // copy the browser flow into a test one
        authRes.copy("browser", Map.of("newName", "test"));

        // add the conditional current credential step as required
        authRes.addExecution("test Browser - Conditional 2FA", Map.of("provider", ConditionalCredentialAuthenticatorFactory.PROVIDER_ID));
        AuthenticationExecutionInfoRepresentation conditionExec = authRes.getExecutions("test Browser - Conditional 2FA").stream()
                .filter(e -> ConditionalCredentialAuthenticatorFactory.PROVIDER_ID.equals(e.getProviderId())).findAny().orElse(null);
        conditionExec.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
        authRes.updateExecutions("test Browser - Conditional 2FA", conditionExec);

        // add the ocnfiguration if needed
        if (included != null || (credentials != null && credentials.length > 0)) {
            AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
            config.setAlias("test-config-current-credential");
            Map<String,String> configMap = new HashMap<>();
            if (included != null) {
                configMap.put(ConditionalCredentialAuthenticatorFactory.CONF_INCLUDED, Boolean.toString(included));
            }
            if (credentials != null && credentials.length > 0) {
                configMap.put(ConditionalCredentialAuthenticatorFactory.CONF_CREDENTIALS,
                        String.join(Constants.CFG_DELIMITER, Arrays.asList(credentials)));
            }
            config.setConfig(configMap);
            authRes.newExecutionConfig(conditionExec.getId(), config);
        }

        // assign the new flow to the browser binding
        realmRep.setBrowserFlow("test");
        realmRes.update(realmRep);
    }

    private void checkLoginOk(String username) {
        String code = oauth.parseLoginResponse().getCode();
        Assert.assertNotNull(code);
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        Assert.assertNull(res.getError());
        Assert.assertNotNull(res.getAccessToken());

        events.expectLogin().user(AssertEvents.isUUID()).detail(Details.USERNAME, username).assertEvent();
    }

}
