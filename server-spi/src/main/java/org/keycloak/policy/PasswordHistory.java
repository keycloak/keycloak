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
import org.keycloak.models.Constants;
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
public class PasswordHistory extends BasePasswordPolicy {
    private static final String NAME = "passwordHistory";
    private static final String INVALID_PASSWORD_HISTORY = "invalidPasswordHistoryMessage";
    private final PasswordPolicy passwordPolicy;
    private int passwordHistoryPolicyValue;

    public PasswordHistory(String arg, PasswordPolicy passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
        passwordHistoryPolicyValue = intArg(NAME, 3, arg);
    }

    @Override
    public Error validate(KeycloakSession session, String user, String password) {
        return null;
    }

    @Override
    public Error validate(KeycloakSession session, UserModel user, String password) {
        if (passwordHistoryPolicyValue != -1) {
            UserCredentialValueModel cred = getCredentialValueModel(user, UserCredentialModel.PASSWORD);
            if (cred != null) {
                if(PasswordHashManager.verify(session, passwordPolicy, password, cred)) {
                    return new Error(INVALID_PASSWORD_HISTORY, passwordHistoryPolicyValue);
                }
            }

            List<UserCredentialValueModel> passwordExpiredCredentials = getCredentialValueModels(user, passwordHistoryPolicyValue - 1,
                    UserCredentialModel.PASSWORD_HISTORY);
            for (UserCredentialValueModel credential : passwordExpiredCredentials) {
                if (PasswordHashManager.verify(session, passwordPolicy, password, credential)) {
                    return new Error(INVALID_PASSWORD_HISTORY, passwordHistoryPolicyValue);
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

    private List<UserCredentialValueModel> getCredentialValueModels(UserModel user, int expiredPasswordsPolicyValue,
            String credType) {
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
    public String description() {
        return null;
    }

    @Override
    public PasswordPolicyProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public String getId() {
        return NAME;
    }
}
