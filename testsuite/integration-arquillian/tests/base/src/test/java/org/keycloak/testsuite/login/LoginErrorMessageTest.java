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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.pages.LoginUsernameOnlyPage;
import org.keycloak.testsuite.pages.PasswordPage;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.UserBuilder;

import static org.junit.Assert.assertEquals;

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
public class LoginErrorMessageTest extends AbstractTestRealmKeycloakTest {

    private static final String IDENTITY_FIRST_FLOW = "identity-first-browser";

    @Page
    protected LoginUsernameOnlyPage loginUsernameOnlyPage;

    @Page
    protected PasswordPage passwordPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        UserRepresentation user = UserBuilder.create()
                .username("basic-user")
                .password("password")
                .email("basic@localhost")
                .firstName("First")
                .lastName("Last")
                .enabled(true)
                .build();
        testRealm.getUsers().add(user);
    }

    @Test
    public void testWrongPasswordShowsGenericErrorWhenUserPreEstablished() {
        configureIdentityFirstFlow();

        // Step 1: enter valid username on the username-only page
        oauth.openLoginForm();
        loginUsernameOnlyPage.login("basic-user");

        // Step 2: UsernamePasswordForm renders with username hidden (user was pre-set).
        // Use PasswordPage to interact with the password-only form.
        passwordPage.login("wrong-password");

        // The error must be generic "Invalid username or password." — not
        // "Invalid password." which would confirm the username is valid.
        assertEquals("Invalid username or password.", passwordPage.getPasswordError());
    }

    private void configureIdentityFirstFlow() {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(IDENTITY_FIRST_FLOW));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
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
