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
import org.keycloak.models.ModelException;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.models.cache.UserCache;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.policy.PolicyError;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PasswordCredentialProvider implements CredentialProvider<PasswordCredentialModel>, CredentialInputUpdater.Streams,
        CredentialInputValidator, OnUserCache {

    public static final String PASSWORD_CACHE_KEY = PasswordCredentialProvider.class.getName() + "." + PasswordCredentialModel.TYPE;
    private static final Logger logger = Logger.getLogger(PasswordCredentialProvider.class);

    protected final KeycloakSession session;

    public PasswordCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    protected UserCredentialStore getCredentialStore() {
        return session.userCredentialManager();
    }

    public PasswordCredentialModel getPassword(RealmModel realm, UserModel user) {
        List<CredentialModel> passwords = null;
        if (user instanceof CachedUserModel && !((CachedUserModel) user).isMarkedForEviction()) {
            CachedUserModel cached = (CachedUserModel) user;
            passwords = (List<CredentialModel>) cached.getCachedWith().get(PASSWORD_CACHE_KEY);

        }
        // if the model was marked for eviction while passwords were initialized, override it from credentialStore
        if (!(user instanceof CachedUserModel) || ((CachedUserModel) user).isMarkedForEviction()) {
            passwords = getCredentialStore().getStoredCredentialsByTypeStream(realm, user, getType()).collect(Collectors.toList());
        }
        if (passwords == null || passwords.isEmpty()) return null;

        return PasswordCredentialModel.createFromCredentialModel(passwords.get(0));
    }

    public boolean createCredential(RealmModel realm, UserModel user, String password) {
        PasswordPolicy policy = realm.getPasswordPolicy();

        PolicyError error = session.getProvider(PasswordPolicyManagerProvider.class).validate(realm, user, password);
        if (error != null) throw new ModelException(error.getMessage(), error.getParameters());

        PasswordHashProvider hash = getHashProvider(policy);
        if (hash == null) {
            return false;
        }
        PasswordCredentialModel credentialModel = hash.encodedCredential(password, policy.getHashIterations());
        credentialModel.setCreatedDate(Time.currentTimeMillis());
        createCredential(realm, user, credentialModel);
        return true;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, PasswordCredentialModel credentialModel) {

        PasswordPolicy policy = realm.getPasswordPolicy();
        int expiredPasswordsPolicyValue = policy.getExpiredPasswords();

        // 1) create new or reset existing password
        CredentialModel createdCredential;
        CredentialModel oldPassword = getPassword(realm, user);
        if (credentialModel.getCreatedDate() == null) {
            credentialModel.setCreatedDate(Time.currentTimeMillis());
        }
        if (oldPassword == null) { // no password exists --> create new
            createdCredential = getCredentialStore().createCredential(realm, user, credentialModel);
        } else { // password exists --> update existing
            credentialModel.setId(oldPassword.getId());
            getCredentialStore().updateCredential(realm, user, credentialModel);
            createdCredential = credentialModel;

            // 2) add a password history item based on the old password
            if (expiredPasswordsPolicyValue > 1) {
                oldPassword.setId(null);
                oldPassword.setType(PasswordCredentialModel.PASSWORD_HISTORY);
                getCredentialStore().createCredential(realm, user, oldPassword);
            }
        }
        
        // 3) remove old password history items
        final int passwordHistoryListMaxSize = Math.max(0, expiredPasswordsPolicyValue - 1);
        getCredentialStore().getStoredCredentialsByTypeStream(realm, user, PasswordCredentialModel.PASSWORD_HISTORY)
                .sorted(CredentialModel.comparingByStartDateDesc())
                .skip(passwordHistoryListMaxSize)
                .collect(Collectors.toList())
                .forEach(p -> getCredentialStore().removeStoredCredential(realm, user, p.getId()));

        UserCache userCache = session.userCache();
        if (userCache != null) {
            userCache.evict(realm, user);
        }
        return createdCredential;
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        return getCredentialStore().removeStoredCredential(realm, user, credentialId);
    }

    @Override
    public PasswordCredentialModel getCredentialFromModel(CredentialModel model) {
        return PasswordCredentialModel.createFromCredentialModel(model);
    }


    protected PasswordHashProvider getHashProvider(PasswordPolicy policy) {
        PasswordHashProvider hash = session.getProvider(PasswordHashProvider.class, policy.getHashAlgorithm());
        if (hash == null) {
            logger.warnv("Realm PasswordPolicy PasswordHashProvider {0} not found", policy.getHashAlgorithm());
            return session.getProvider(PasswordHashProvider.class, PasswordPolicy.HASH_ALGORITHM_DEFAULT);
        }
        return hash;
    }

    /*@Override
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
        hash.encode(cred.getValue(), policy.getHashIterations(), newPassword);
        getCredentialStore().createCredential(realm, user, newPassword);
        UserCache userCache = session.userCache();
        if (userCache != null) {
            userCache.evict(realm, user);
        }
        return true;
    }*/

    /*@Override
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
    }*/

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equals(getType());
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        return createCredential(realm, user, input.getChallengeResponse());
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {

    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        return Stream.empty();
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return getPassword(realm, user) != null;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof UserCredentialModel)) {
            logger.debug("Expected instance of UserCredentialModel for CredentialInput");
            return false;

        }
        if (input.getChallengeResponse() == null) {
            logger.debugv("Input password was null for user {0} ", user.getUsername());
            return false;
        }
        PasswordCredentialModel password = getPassword(realm, user);
        if (password == null) {
            logger.debugv("No password cached or stored for user {0} ", user.getUsername());
            return false;
        }
        PasswordHashProvider hash = session.getProvider(PasswordHashProvider.class, password.getPasswordCredentialData().getAlgorithm());
        if (hash == null) {
            logger.debugv("PasswordHashProvider {0} not found for user {1} ", password.getPasswordCredentialData().getAlgorithm(), user.getUsername());
            return false;
        }
        if (!hash.verify(input.getChallengeResponse(), password)) {
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

        PasswordCredentialModel newPassword = hash.encodedCredential(input.getChallengeResponse(), policy.getHashIterations());
        newPassword.setId(password.getId());
        newPassword.setCreatedDate(password.getCreatedDate());
        newPassword.setUserLabel(password.getUserLabel());
        getCredentialStore().updateCredential(realm, user, newPassword);

        UserCache userCache = session.userCache();
        if (userCache != null) {
            userCache.evict(realm, user);
        }

        return true;
    }

    @Override
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
        List<CredentialModel> passwords = getCredentialStore().getStoredCredentialsByTypeStream(realm, user, getType())
                .collect(Collectors.toList());
        user.getCachedWith().put(PASSWORD_CACHE_KEY, passwords);
    }

    @Override
    public String getType() {
        return PasswordCredentialModel.TYPE;
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext) {
        CredentialTypeMetadata.CredentialTypeMetadataBuilder metadataBuilder = CredentialTypeMetadata.builder()
                .type(getType())
                .category(CredentialTypeMetadata.Category.BASIC_AUTHENTICATION)
                .displayName("password-display-name")
                .helpText("password-help-text")
                .iconCssClass("kcAuthenticatorPasswordClass");

        // Check if we are creating or updating password
        UserModel user = metadataContext.getUser();
        if (user != null && session.userCredentialManager().isConfiguredFor(session.getContext().getRealm(), user, getType())) {
            metadataBuilder.updateAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        } else {
            metadataBuilder.createAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        }

        return metadataBuilder
                .removeable(false)
                .build(session);
    }
}
