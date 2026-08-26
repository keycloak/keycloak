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
package org.keycloak.tests.login;


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
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginTotpPage;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest
public class ConditionalCredentialAuthenticatorTest {

    @InjectRealm(config = ConditionalCredentialAuthenticatorRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @InjectPage
    protected LoginTotpPage loginTotpPage;

    private static final String USERNAME = "user-with-one-configured-otp";
    private static final String PASSWORD = "password";

    @Test
    public void testPasswordIncluded() {
        configureConditionalCurrentCredentialFlow(Boolean.TRUE, PasswordCredentialModel.TYPE);

        // login with username password
        oauth.openLoginForm();
        oauth.fillLoginForm(USERNAME, PASSWORD);

        // 2FA with otp should be displayed
        loginTotpPage.assertCurrent();
        loginTotpPage.login(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A"));
        checkLoginOk();
    }

    @Test
    public void testPasswordNotIncluded() {
        configureConditionalCurrentCredentialFlow(Boolean.FALSE, PasswordCredentialModel.TYPE);

        // login with username password
        oauth.openLoginForm();
        oauth.fillLoginForm(USERNAME, PASSWORD);

        // 2FA with otp should not be displayed
        checkLoginOk();
    }

    @Test
    public void testWebAuthnNotIncluded() {
        configureConditionalCurrentCredentialFlow(Boolean.FALSE, WebAuthnCredentialModel.TYPE_PASSWORDLESS);

        // login with username password
        oauth.openLoginForm();
        oauth.fillLoginForm(USERNAME, PASSWORD);

        // 2FA with otp should be displayed
        loginTotpPage.assertCurrent();
        loginTotpPage.login(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A"));
        checkLoginOk();
    }

    @Test
    public void testPasswordAndWebAuthnNotIncluded() {
        configureConditionalCurrentCredentialFlow(Boolean.FALSE, WebAuthnCredentialModel.TYPE_PASSWORDLESS, PasswordCredentialModel.TYPE);

        // login with username password
        oauth.openLoginForm();
        oauth.fillLoginForm(USERNAME, PASSWORD);

        // 2FA with otp should not be displayed
        checkLoginOk();
    }

    @Test
    public void testNoConfig() {
        configureConditionalCurrentCredentialFlow(null);

        // login with username password
        oauth.openLoginForm();
        oauth.fillLoginForm(USERNAME, PASSWORD);

        // 2FA with otp should be displayed
        loginTotpPage.assertCurrent();
        loginTotpPage.login(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A"));
        checkLoginOk();
    }

    @Test
    public void testNoneIncluded() {
        configureConditionalCurrentCredentialFlow(Boolean.TRUE, ConditionalCredentialAuthenticatorFactory.NONE_CREDENTIAL);

        // login with username password
        oauth.openLoginForm();
        oauth.fillLoginForm(USERNAME, PASSWORD);

        // 2FA with otp should not be displayed
        checkLoginOk();
    }

    @Test
    public void testNoneNotIncluded() {
        configureConditionalCurrentCredentialFlow(Boolean.FALSE, ConditionalCredentialAuthenticatorFactory.NONE_CREDENTIAL);

        // login with username password
        oauth.openLoginForm();
        oauth.fillLoginForm(USERNAME, PASSWORD);

        // 2FA with otp should be displayed
        loginTotpPage.assertCurrent();
        loginTotpPage.login(new TimeBasedOTP().generateTOTP("DJmQfC73VGFhw7D4QJ8A"));
        checkLoginOk();
    }

    private void configureConditionalCurrentCredentialFlow(Boolean included, String... credentials) {
        // clone the browser flow and add the current credential condition in the 2FA section

        RealmResource realmRes = managedRealm.admin();
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

    private void checkLoginOk() {
        String code = oauth.parseLoginResponse().getCode();
        Assertions.assertNotNull(code);
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        Assertions.assertNull(res.getError());
        Assertions.assertNotNull(res.getAccessToken());

        EventAssertion.expectLoginSuccess(events.poll()).hasUserId().details(Details.USERNAME, USERNAME);
    }


    private static class ConditionalCredentialAuthenticatorRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            // setup normal otp policy but with reusable tokens
            realm.otpAlgorithm("HmacSHA1")
                    .otpDigits(6)
                    .otpInitialCounter(0)
                    .otpLookAheadWindow(1)
                    .otpPeriod(30)
                    .otpType("totp")
                    .otpCodeReusable(Boolean.TRUE);
            
            return realm.users(UserBuilder.create(USERNAME)
                    .password(PASSWORD)
                            .name("John", "Doe")
                    .email("otp1@redhat.com")
                    .totpSecret("DJmQfC73VGFhw7D4QJ8A")
            );
        }
    }
}
