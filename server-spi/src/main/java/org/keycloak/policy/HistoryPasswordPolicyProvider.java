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

import org.keycloak.hash.PasswordHashManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class HistoryPasswordPolicyProvider implements PasswordPolicyProvider {

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
    public PolicyError validate(UserModel user, String password) {
        PasswordPolicy policy = session.getContext().getRealm().getPasswordPolicy();
        int passwordHistoryPolicyValue = policy.getPolicyConfig(HistoryPasswordPolicyProviderFactory.ID);
        if (passwordHistoryPolicyValue != -1) {
            UserCredentialValueModel cred = getCredentialValueModel(user, UserCredentialModel.PASSWORD);
            if (cred != null) {
                if(PasswordHashManager.verify(session, policy, password, cred)) {
                    return new PolicyError(ERROR_MESSAGE, passwordHistoryPolicyValue);
                }
            }

            List<UserCredentialValueModel> passwordExpiredCredentials = getCredentialValueModels(user, passwordHistoryPolicyValue - 1,
                    UserCredentialModel.PASSWORD_HISTORY);
            for (UserCredentialValueModel credential : passwordExpiredCredentials) {
                if (PasswordHashManager.verify(session, policy, password, credential)) {
                    return new PolicyError(ERROR_MESSAGE, passwordHistoryPolicyValue);
                }
            }
        }
        return null;
    }

    private UserCredentialValueModel getCredentialValueModel(UserModel user, String credType) {
        for (UserCredentialValueModel model : user.getCredentialsDirectly()) {
            if (model.getType().equals(credType)) {
                return model;
            }
        }
        return null;
    }

    private List<UserCredentialValueModel> getCredentialValueModels(UserModel user, int expiredPasswordsPolicyValue, String credType) {
        List<UserCredentialValueModel> credentialModels = new ArrayList<UserCredentialValueModel>();
        for (UserCredentialValueModel model : user.getCredentialsDirectly()) {
            if (model.getType().equals(credType)) {
                credentialModels.add(model);
            }
        }

        Collections.sort(credentialModels, new Comparator<UserCredentialValueModel>() {
            public int compare(UserCredentialValueModel credFirst, UserCredentialValueModel credSecond) {
                if (credFirst.getCreatedDate() > credSecond.getCreatedDate()) {
                    return -1;
                } else if (credFirst.getCreatedDate() < credSecond.getCreatedDate()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        if (credentialModels.size() > expiredPasswordsPolicyValue) {
            return credentialModels.subList(0, expiredPasswordsPolicyValue);
        }
        return credentialModels;
    }

    @Override
    public Object parseConfig(String value) {
        return value != null ? Integer.parseInt(value) : HistoryPasswordPolicyProviderFactory.DEFAULT_VALUE;
    }

    @Override
    public void close() {
    }

}
