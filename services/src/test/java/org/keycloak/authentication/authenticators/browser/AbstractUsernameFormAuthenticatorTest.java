/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authentication.authenticators.browser;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test for GH-51887 (username-enumeration timing side-channel).
 *
 * <p>An empty password submitted for an existing user must trigger exactly one dummy password-hash
 * operation via {@link AuthenticatorUtils#dummyHash(AuthenticationFlowContext)} - the same single
 * operation performed when the submitted username does not exist at all. Without this, an empty
 * password for a real username responds faster than one for a fake username, leaking which
 * usernames are registered.
 *
 * <p>Note: {@code LoginTest.loginMissingPassword()} only asserts on the HTTP response/error page,
 * which is identical whether or not {@code dummyHash()} runs, so it would NOT catch a regression
 * here. This test verifies the actual call instead, using Mockito's static mocking.
 */
public class AbstractUsernameFormAuthenticatorTest {

    /** Minimal concrete subclass - only the inherited methods under test are exercised. */
    private static class TestAuthenticator extends AbstractUsernameFormAuthenticator {
        @Override
        public void authenticate(AuthenticationFlowContext context) {
            throw new UnsupportedOperationException("not exercised by this test");
        }

        @Override
        public boolean requiresUser() {
            return false;
        }

        @Override
        public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
            return true;
        }

        @Override
        public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
            // no-op - not exercised by this test
        }
    }

    /**
     * Builds an AuthenticationFlowContext mock stubbed just enough for validatePassword()/
     * testInvalidUser() and the challenge()/badPasswordHandler() machinery they call into to run
     * to completion without NPEs. LoginFormsProvider uses RETURNS_SELF so its fluent
     * setExecution()/setError()/addError() chain keeps returning a usable mock.
     */
    private AuthenticationFlowContext mockContext() {
        AuthenticationFlowContext context = mock(AuthenticationFlowContext.class);
        LoginFormsProvider forms = mock(LoginFormsProvider.class, RETURNS_SELF);
        AuthenticationExecutionModel execution = mock(AuthenticationExecutionModel.class);
        AuthenticationSessionModel authSession = mock(AuthenticationSessionModel.class);
        EventBuilder event = mock(EventBuilder.class);

        when(context.form()).thenReturn(forms);
        when(context.getExecution()).thenReturn(execution);
        when(context.getAuthenticationSession()).thenReturn(authSession);
        when(context.getEvent()).thenReturn(event);

        return context;
    }

    @Test
    public void nonExistentUsernamePaysExactlyOneHashOperation() {
        TestAuthenticator authenticator = new TestAuthenticator();
        AuthenticationFlowContext context = mockContext();

        try (MockedStatic<AuthenticatorUtils> utils = mockStatic(AuthenticatorUtils.class)) {
            authenticator.testInvalidUser(context, null);   // user == null -> "username not found" branch

            utils.verify(() -> AuthenticatorUtils.dummyHash(context), times(1));
        }
    }

    @Test
    public void existingUserWithEmptyPasswordPaysExactlyOneHashOperation() {
        TestAuthenticator authenticator = new TestAuthenticator();
        UserModel user = mock(UserModel.class);
        AuthenticationFlowContext context = mockContext();

        MultivaluedMap<String, String> inputData = new MultivaluedHashMap<>();
        inputData.putSingle(CredentialRepresentation.PASSWORD, "");   // empty password, real user

        try (MockedStatic<AuthenticatorUtils> utils = mockStatic(AuthenticatorUtils.class)) {
            authenticator.validatePassword(context, user, inputData, true);

            // The exact assertion that fails if AuthenticatorUtils.dummyHash(context) is removed
            // from validatePassword()'s empty-password branch: it would go from times(1) to times(0).
            utils.verify(() -> AuthenticatorUtils.dummyHash(context), times(1));

            // The real hash path must never be reached for an empty password - that's what causes
            // the IllegalArgumentException crash (GH #15336) the early-return branch exists to avoid.
            verify(user, never()).credentialManager();
        }
    }

    @Test
    public void nonExistentUsernameAndExistingUserEmptyPasswordCostTheSame() {
        TestAuthenticator authenticator = new TestAuthenticator();

        try (MockedStatic<AuthenticatorUtils> utils = mockStatic(AuthenticatorUtils.class)) {
            AuthenticationFlowContext nonExistentUserContext = mockContext();
            authenticator.testInvalidUser(nonExistentUserContext, null);

            UserModel user = mock(UserModel.class);
            AuthenticationFlowContext existingUserContext = mockContext();
            MultivaluedMap<String, String> inputData = new MultivaluedHashMap<>();
            inputData.putSingle(CredentialRepresentation.PASSWORD, "");
            authenticator.validatePassword(existingUserContext, user, inputData, true);

            utils.verify(() -> AuthenticatorUtils.dummyHash(nonExistentUserContext), times(1));
            utils.verify(() -> AuthenticatorUtils.dummyHash(existingUserContext), times(1));
            // Total across both scenarios must be exactly 2 - one each, no more, no less.
            utils.verify(() -> AuthenticatorUtils.dummyHash(org.mockito.ArgumentMatchers.any()), times(2));
        }
    }
}