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

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.BruteForceProtector;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class AuthenticatorUtils {
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
}
