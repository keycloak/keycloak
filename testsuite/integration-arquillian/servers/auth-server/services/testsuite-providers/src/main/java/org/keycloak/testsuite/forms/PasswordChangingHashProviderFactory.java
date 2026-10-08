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

package org.keycloak.testsuite.forms;

import org.keycloak.credential.hash.AbstractPbkdf2PasswordHashProviderFactory;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.Pbkdf2PasswordHashProvider;
import org.keycloak.credential.hash.Pbkdf2Sha512PasswordHashProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * PBKDF2-SHA512 password hashing that can change a user's password in a separate transaction while it hashes another
 * password. This simulates a password change that commits while a password is being re-hashed.
 * <p>
 * The change is configured with the realm attributes {@link #USERNAME_ATTRIBUTE} and {@link #PASSWORD_ATTRIBUTE}. It
 * happens once, when a password other than the configured one is hashed, and removes the attributes.
 */
public class PasswordChangingHashProviderFactory extends AbstractPbkdf2PasswordHashProviderFactory {

    public static final String ID = "test-password-changing-hash";

    public static final String USERNAME_ATTRIBUTE = "test.passwordChangingHash.username";
    public static final String PASSWORD_ATTRIBUTE = "test.passwordChangingHash.password";

    @Override
    public PasswordHashProvider create(KeycloakSession session) {
        return new Pbkdf2PasswordHashProvider(ID, Pbkdf2Sha512PasswordHashProviderFactory.PBKDF2_ALGORITHM,
                Pbkdf2Sha512PasswordHashProviderFactory.DEFAULT_ITERATIONS, getMaxPaddingLength()) {

            @Override
            public PasswordCredentialModel encodedCredential(String rawPassword, int iterations) {
                PasswordCredentialModel credential = super.encodedCredential(rawPassword, iterations);
                changePassword(session, rawPassword);
                return credential;
            }
        };
    }

    private static void changePassword(KeycloakSession session, String hashedPassword) {
        RealmModel realm = session.getContext().getRealm();
        String username = realm != null ? realm.getAttribute(USERNAME_ATTRIBUTE) : null;
        String newPassword = realm != null ? realm.getAttribute(PASSWORD_ATTRIBUTE) : null;
        if (username == null || newPassword == null || newPassword.equals(hashedPassword)) {
            return;
        }

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session.getContext(), otherSession -> {
            RealmModel otherRealm = otherSession.getContext().getRealm();
            otherRealm.removeAttribute(USERNAME_ATTRIBUTE);
            otherRealm.removeAttribute(PASSWORD_ATTRIBUTE);
            UserModel user = otherSession.users().getUserByUsername(otherRealm, username);
            user.credentialManager().updateCredential(UserCredentialModel.password(newPassword));
        });
    }

    @Override
    public String getId() {
        return ID;
    }
}
