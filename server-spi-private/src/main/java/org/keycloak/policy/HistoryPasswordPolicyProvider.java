/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.policy;

import org.jboss.logging.Logger;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class HistoryPasswordPolicyProvider implements PasswordPolicyProvider {

    private static final Logger logger = Logger.getLogger(HistoryPasswordPolicyProvider.class);
    private static final String ERROR_MESSAGE = "invalidPasswordHistoryMessage";

    private KeycloakSession session;

    public HistoryPasswordPolicyProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public PolicyError validate(String username, String password) {
        return null;
    }

    @Override
    public PolicyError validate(RealmModel realm, UserModel user, String password) {
        PasswordPolicy policy = session.getContext().getRealm().getPasswordPolicy();
        int passwordHistoryPolicyValue = policy.getPolicyConfig(PasswordPolicy.PASSWORD_HISTORY_ID);
        if (passwordHistoryPolicyValue != -1) {
            List<CredentialModel> storedPasswords = session.userCredentialManager().getStoredCredentialsByType(realm, user, CredentialModel.PASSWORD);
            for (CredentialModel cred : storedPasswords) {
                PasswordHashProvider hash = session.getProvider(PasswordHashProvider.class, cred.getAlgorithm());
                if (hash == null) continue;
                if (hash.verify(password, cred)) {
                    return new PolicyError(ERROR_MESSAGE, passwordHistoryPolicyValue);
                }
            }
            List<CredentialModel> passwordHistory = session.userCredentialManager().getStoredCredentialsByType(realm, user, CredentialModel.PASSWORD_HISTORY);
            for (CredentialModel cred : passwordHistory) {
                PasswordHashProvider hash = session.getProvider(PasswordHashProvider.class, cred.getAlgorithm());
                if (hash.verify(password, cred)) {
                    return new PolicyError(ERROR_MESSAGE, passwordHistoryPolicyValue);
                }

            }
        }
        return null;
    }

    @Override
    public Object parseConfig(String value) {
        return parseInteger(value, HistoryPasswordPolicyProviderFactory.DEFAULT_VALUE);
    }

    @Override
    public void close() {
    }

}
