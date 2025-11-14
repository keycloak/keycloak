/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.authenticators.util;

import java.io.IOException;
import java.util.Map;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.common.util.Time;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;

import static org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator.USER_SET_BEFORE_USERNAME_PASSWORD_AUTH;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class AuthenticatorUtils {
    private static final Logger logger = Logger.getLogger(AuthenticatorUtils.class);

    public static String getDisabledByBruteForceEventError(BruteForceProtector protector, KeycloakSession session, RealmModel realm, UserModel user) {
        if (realm.isBruteForceProtected()) {
            if (protector.isPermanentlyLockedOut(session, realm, user)) {
                return Errors.USER_DISABLED;
            }
            else if (protector.isTemporarilyDisabled(session, realm, user)) {
                return Errors.USER_TEMPORARILY_DISABLED;
            }
            return null;
        }
        return null;
    }

    public static String getDisabledByBruteForceEventError(AuthenticationFlowContext authnFlowContext, UserModel authenticatedUser) {
        return AuthenticatorUtils.getDisabledByBruteForceEventError(authnFlowContext.getProtector(), authnFlowContext.getSession(), authnFlowContext.getRealm(), authenticatedUser);
    }

    /**
     * This method exists to simulate hashing of some "dummy" password. The purpose is to make the user enumeration harder, so the authentication request with non-existing username also need
     * to simulate the password hashing overhead and takes same time like the request with existing username, but incorrect password.
     *
     * @param context
     */
    public static void dummyHash(AuthenticationFlowContext context) {
        PasswordPolicy passwordPolicy = context.getRealm().getPasswordPolicy();
        PasswordHashProvider provider;
        if (passwordPolicy != null && passwordPolicy.getHashAlgorithm() != null) {
            provider = context.getSession().getProvider(PasswordHashProvider.class, passwordPolicy.getHashAlgorithm());
        } else {
            provider = context.getSession().getProvider(PasswordHashProvider.class);
        }
        int iterations = passwordPolicy != null ? passwordPolicy.getHashIterations() : -1;
        provider.encodedCredential("SlightlyLongerDummyPassword", iterations);
    }

    /**
     * Get all completed authenticator executions from the user session notes.
     * @param note The serialized note value to parse
     * @return A list of execution ids that were successfully completed to create this authentication session
     */
    public static Map<String, Integer> parseCompletedExecutions(String note){
        // default to empty map
        if (note == null){
            note = "{}";
        }

        try {
            return JsonSerialization.readValue(note, new TypeReference<Map<String, Integer>>() {});
        } catch (IOException e) {
            logger.warnf("Invalid format of the completed authenticators map. Saved value was: %s", note);
            throw new IllegalStateException(e);
        }
    }

    /**
     * Update the completed authenticators note on the new auth session
     * @param authSession The current authentication session
     * @param userSession The previous user session
     * @param executionId The completed execution id
     */
    public static void updateCompletedExecutions(AuthenticationSessionModel authSession, UserSessionModel userSession, String executionId){
        Map<String, Integer> completedExecutions = parseCompletedExecutions(authSession.getUserSessionNotes().get(Constants.AUTHENTICATORS_COMPLETED));

        // attempt to fetch previously completed authenticators
        if (userSession != null){
            Map<String, Integer> prevCompleted = parseCompletedExecutions(userSession.getNote(Constants.AUTHENTICATORS_COMPLETED));
            logger.debugf("merging completed executions from previous authentication session %s", prevCompleted);
            completedExecutions.putAll(prevCompleted);
        }

        // set new execution and serialize note
        completedExecutions.put(executionId, Time.currentTime());
        try {
            String updated = JsonSerialization.writeValueAsString(completedExecutions);
            authSession.setUserSessionNote(Constants.AUTHENTICATORS_COMPLETED, updated);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }


    // Make sure that form is setup for "re-authentication" rather than regular authentication if some error happens during re-authentication
    public static void setupReauthenticationInUsernamePasswordFormError(AuthenticationFlowContext context) {
        String userAlreadySetBeforeUsernamePasswordAuth = context.getAuthenticationSession().getAuthNote(USER_SET_BEFORE_USERNAME_PASSWORD_AUTH);

        if (Boolean.parseBoolean(userAlreadySetBeforeUsernamePasswordAuth)) {
            LoginFormsProvider form = context.form();
            form.setAttribute(LoginFormsProvider.USERNAME_HIDDEN, true);
            form.setAttribute(LoginFormsProvider.REGISTRATION_DISABLED, true);
        }
    }

}
