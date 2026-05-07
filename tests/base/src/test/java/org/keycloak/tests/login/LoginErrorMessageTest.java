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

import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.page.PasswordPage;
import org.keycloak.tests.common.BasicUserConfig;
import org.keycloak.testsuite.util.FlowUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that login error messages do not leak whether a username is valid,
 * preventing user enumeration attacks.
 *
 * In an identity-first flow (UsernameForm → UsernamePasswordForm), after the
 * username step resolves the user, UsernamePasswordForm runs with
 * USER_SET_BEFORE_USERNAME_PASSWORD_AUTH=true. A wrong password must show
 * a generic "Invalid username or password." error, not "Invalid password."
 * which would confirm the user exists.
 */
@KeycloakIntegrationTest
public class LoginErrorMessageTest {

    private static final String IDENTITY_FIRST_FLOW = "identity-first-browser";

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectUser(config = BasicUserConfig.class)
    ManagedUser user;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginUsernamePage usernamePage;

    @InjectPage
    PasswordPage passwordPage;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void testWrongPasswordShowsGenericErrorWhenUserPreEstablished() {
        configureIdentityFirstFlow();

        // Step 1: enter valid username on the username-only page
        oauth.openLoginForm();
        usernamePage.assertCurrent();
        usernamePage.fillLoginWithUsernameOnly("basic-user");
        usernamePage.submit();

        // Step 2: UsernamePasswordForm renders with username hidden (user was pre-set).
        // Use PasswordPage to interact with the password-only form.
        passwordPage.fillPassword("wrong-password");
        passwordPage.submit();

        // The error must be generic "Invalid username or password." — not
        // "Invalid password." which would confirm the username is valid.
        assertEquals("Invalid username or password.", passwordPage.getPasswordError());
    }

    private void configureIdentityFirstFlow() {
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(IDENTITY_FIRST_FLOW));
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(IDENTITY_FIRST_FLOW)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, "auth-username-form")
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, "auth-username-password-form")
                )
                .defineAsBrowserFlow()
        );
    }
}
