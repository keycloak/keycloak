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
package org.keycloak.credential;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.policy.PolicyError;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PasswordCredentialProvider implements CredentialProvider, CredentialInputValidator, CredentialInputUpdater, OnUserCache {

    public static final String PASSWORD_CACHE_KEY = PasswordCredentialProvider.class.getName() + "." + CredentialModel.PASSWORD;
    private static final Logger logger = Logger.getLogger(PasswordCredentialProvider.class);

    protected KeycloakSession session;

    public PasswordCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    protected UserCredentialStore getCredentialStore() {
        return session.userCredentialManager();
    }

    public CredentialModel getPassword(RealmModel realm, UserModel user) {
        List<CredentialModel> passwords = null;
        if (user instanceof CachedUserModel && !((CachedUserModel)user).isMarkedForEviction()) {
            CachedUserModel cached = (CachedUserModel)user;
            passwords = (List<CredentialModel>)cached.getCachedWith().get(PASSWORD_CACHE_KEY);

        } else {
            passwords = getCredentialStore().getStoredCredentialsByType(realm, user, CredentialModel.PASSWORD);
        }
        if (passwords == null || passwords.isEmpty()) return null;
        return passwords.get(0);
    }


    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType())) return false;

        if (!(input instanceof UserCredentialModel)) {
            logger.debug("Expected instance of UserCredentialModel for CredentialInput");
            return false;
        }
        UserCredentialModel cred = (UserCredentialModel)input;
        PasswordPolicy policy = realm.getPasswordPolicy();

        PolicyError error = session.getProvider(PasswordPolicyManagerProvider.class).validate(realm, user, cred.getValue());
        if (error != null) throw new ModelException(error.getMessage(), error.getParameters());


        PasswordHashProvider hash = getHashProvider(policy);
        if (hash == null) {
            return false;
        }
        CredentialModel oldPassword = getPassword(realm, user);

        expirePassword(realm, user, policy);
        CredentialModel newPassword = new CredentialModel();
        newPassword.setType(CredentialModel.PASSWORD);
        long createdDate = Time.currentTimeMillis();
        newPassword.setCreatedDate(createdDate);
        hash.encode(cred.getValue(), policy, newPassword);
        getCredentialStore().createCredential(realm, user, newPassword);
        session.userCache().evict(realm, user);
        return true;
    }

    protected void expirePassword(RealmModel realm, UserModel user, PasswordPolicy policy) {

        CredentialModel oldPassword = getPassword(realm, user);
        if (oldPassword == null) return;
        int expiredPasswordsPolicyValue = policy.getExpiredPasswords();
        if (expiredPasswordsPolicyValue > -1) {
            List<CredentialModel> list = getCredentialStore().getStoredCredentialsByType(realm, user, CredentialModel.PASSWORD_HISTORY);
            List<CredentialModel> history = new LinkedList<>();
            history.addAll(list);
            if (history.size() + 1 >= expiredPasswordsPolicyValue) {
                Collections.sort(history, new Comparator<CredentialModel>() {
                    @Override
                    public int compare(CredentialModel o1, CredentialModel o2) {
                        long o1Date = o1.getCreatedDate() == null ? 0 : o1.getCreatedDate().longValue();
                        long o2Date = o2.getCreatedDate() == null ? 0 : o2.getCreatedDate().longValue();
                        if (o1Date > o2Date) return 1;
                        else if (o1Date < o2Date) return -1;
                        else return 0;
                    }
                });
                for (int i = 0; i < history.size() + 2 - expiredPasswordsPolicyValue; i++) {
                    getCredentialStore().removeStoredCredential(realm, user, history.get(i).getId());
                }

            }
            oldPassword.setType(CredentialModel.PASSWORD_HISTORY);
            getCredentialStore().updateCredential(realm, user, oldPassword);
        } else {
            session.userCredentialManager().removeStoredCredential(realm, user, oldPassword.getId());
        }

    }

    protected PasswordHashProvider getHashProvider(PasswordPolicy policy) {
        PasswordHashProvider hash = session.getProvider(PasswordHashProvider.class, policy.getHashAlgorithm());
        if (hash == null) {
            logger.warnv("Realm PasswordPolicy PasswordHashProvider {0} not found", policy.getHashAlgorithm());
            return session.getProvider(PasswordHashProvider.class, PasswordPolicy.HASH_ALGORITHM_DEFAULT);
        }
        return hash;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) return;
        PasswordPolicy policy = realm.getPasswordPolicy();
        expirePassword(realm, user, policy);
    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        if (!getCredentialStore().getStoredCredentialsByType(realm, user, CredentialModel.PASSWORD).isEmpty()) {
            Set<String> set = new HashSet<>();
            set.add(CredentialModel.PASSWORD);
            return set;
        } else {
            return Collections.EMPTY_SET;
        }
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equals(CredentialModel.PASSWORD);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return getPassword(realm, user) != null;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (! (input instanceof UserCredentialModel)) {
            logger.debug("Expected instance of UserCredentialModel for CredentialInput");
            return false;

        }
        UserCredentialModel cred = (UserCredentialModel)input;
        if (cred.getValue() == null) {
            logger.debugv("Input password was null for user {0} ", user.getUsername());
            return false;
        }
        CredentialModel password = getPassword(realm, user);
        if (password == null) {
            logger.debugv("No password cached or stored for user {0} ", user.getUsername());
            return false;
        }
        PasswordHashProvider hash = session.getProvider(PasswordHashProvider.class, password.getAlgorithm());
        if (hash == null) {
            logger.debugv("PasswordHashProvider {0} not found for user {1} ", password.getAlgorithm(), user.getUsername());
            return false;
        }
        if (!hash.verify(cred.getValue(), password)) {
            logger.debugv("Failed password validation for user {0} ", user.getUsername());
            return false;
        }
        PasswordPolicy policy = realm.getPasswordPolicy();
        if (policy == null) {
            return true;
        }
        hash = getHashProvider(policy);
        if (hash == null) {
            return true;
        }
        if (hash.policyCheck(policy, password)) {
            return true;
        }

        hash.encode(cred.getValue(), policy, password);
        getCredentialStore().updateCredential(realm, user, password);
        session.userCache().evict(realm, user);

        return true;
    }

    @Override
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
        List<CredentialModel> passwords = getCredentialStore().getStoredCredentialsByType(realm, user, CredentialModel.PASSWORD);
        if (passwords != null) {
            user.getCachedWith().put(PASSWORD_CACHE_KEY, passwords);
        }

    }
}
