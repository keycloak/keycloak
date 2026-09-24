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
package org.keycloak.services.managers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;

/**
 * A fixed, account-local counter for recovery authentication code failures.
 */
public final class BruteForceRecoveryCode {

    private static final String FAILURE_KEY_PREFIX = "bf-recovery:";

    private BruteForceRecoveryCode() {
    }

    public static boolean isAttempt(RealmModel realm, Set<String> authenticationCategories) {
        return realm.isBruteForceIndependentRecoveryAuthnCodes()
                && authenticationCategories != null
                && authenticationCategories.contains(RecoveryAuthnCodesCredentialModel.TYPE);
    }

    public static boolean isChannel(RealmModel realm, String authenticationChannel) {
        return realm.isBruteForceIndependentRecoveryAuthnCodes()
                && RecoveryAuthnCodesCredentialModel.TYPE.equals(authenticationChannel);
    }

    public static String failureKey(UserModel user) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return FAILURE_KEY_PREFIX
                    + Base64Url.encode(digest.digest(user.getId().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException cause) {
            throw new IllegalStateException("SHA-256 is not available", cause);
        }
    }

    public static boolean isFailureKey(String failureKey) {
        return failureKey != null && failureKey.startsWith(FAILURE_KEY_PREFIX);
    }

    public static boolean isTemporarilyLocked(KeycloakSession session, RealmModel realm, UserModel user) {
        UserLoginFailureModel failure = getFailure(session, realm, user);
        return failure != null && Time.currentTime() < failure.getFailedLoginNotBefore();
    }

    public static boolean isPermanentlyLocked(KeycloakSession session, RealmModel realm, UserModel user) {
        UserLoginFailureModel failure = getFailure(session, realm, user);
        return failure != null && BruteForceUserProperty.isPermanentlyLocked(realm, failure);
    }

    /**
     * Whether the recovery code budget currently blocks an attempt. Like property counters, it
     * leaves the user record enabled, so an admin unlock has to check it explicitly.
     */
    public static boolean isLocked(KeycloakSession session, RealmModel realm, UserModel user) {
        return isTemporarilyLocked(session, realm, user) || isPermanentlyLocked(session, realm, user);
    }

    public static boolean removeLoginFailure(KeycloakSession session, RealmModel realm, UserModel user) {
        String failureKey = failureKey(user);
        if (session.loginFailures().getUserLoginFailure(realm, failureKey) == null) {
            return false;
        }
        session.loginFailures().removeUserLoginFailure(realm, failureKey);
        return true;
    }

    private static UserLoginFailureModel getFailure(KeycloakSession session, RealmModel realm, UserModel user) {
        return session.loginFailures().getUserLoginFailure(realm, failureKey(user));
    }
}
