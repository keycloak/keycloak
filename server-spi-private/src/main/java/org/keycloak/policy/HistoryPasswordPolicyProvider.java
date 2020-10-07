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
import org.keycloak.models.credential.PasswordCredentialModel;

import java.util.List;
import java.util.stream.Collectors;

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
            List<CredentialModel> storedPasswords = session.userCredentialManager().getStoredCredentialsByType(realm, user, PasswordCredentialModel.TYPE);
            for (CredentialModel cred : storedPasswords) {
                PasswordCredentialModel passwordCredential = PasswordCredentialModel.createFromCredentialModel(cred);
                PasswordHashProvider hash = session.getProvider(PasswordHashProvider.class, passwordCredential.getPasswordCredentialData().getAlgorithm());
                if (hash == null) continue;
                if (hash.verify(password, passwordCredential)) {
                    return new PolicyError(ERROR_MESSAGE, passwordHistoryPolicyValue);
                }
            }

            if (passwordHistoryPolicyValue > 0) {
                List<CredentialModel> passwordHistory = session.userCredentialManager().getStoredCredentialsByType(realm, user, PasswordCredentialModel.PASSWORD_HISTORY);
                List<CredentialModel> recentPasswordHistory = getRecent(passwordHistory, passwordHistoryPolicyValue - 1);
                for (CredentialModel cred : recentPasswordHistory) {
                    PasswordCredentialModel passwordCredential = PasswordCredentialModel.createFromCredentialModel(cred);
                    PasswordHashProvider hash = session.getProvider(PasswordHashProvider.class, passwordCredential.getPasswordCredentialData().getAlgorithm());
                    if (hash.verify(password, passwordCredential)) {
                        return new PolicyError(ERROR_MESSAGE, passwordHistoryPolicyValue);
                    }

                }
            }
        }
        return null;
    }

    private List<CredentialModel> getRecent(List<CredentialModel> passwordHistory, int limit) {
        return passwordHistory.stream()
                .sorted(CredentialModel.comparingByStartDateDesc())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public Object parseConfig(String value) {
        return parseInteger(value, HistoryPasswordPolicyProviderFactory.DEFAULT_VALUE);
    }

    @Override
    public void close() {
    }

}
