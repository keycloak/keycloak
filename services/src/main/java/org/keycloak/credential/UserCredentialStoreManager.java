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

import org.keycloak.common.util.reflections.Types;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.models.cache.UserCache;
import org.keycloak.storage.AbstractStorageManager;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserCredentialStoreManager extends AbstractStorageManager<UserStorageProvider, UserStorageProviderModel>
        implements UserCredentialManager.Streams, OnUserCache {

    public UserCredentialStoreManager(KeycloakSession session) {
        super(session, UserStorageProviderFactory.class, UserStorageProvider.class, UserStorageProviderModel::new, "user");
    }

    protected UserCredentialStore getStoreForUser(UserModel user) {
        if (StorageId.isLocalStorage(user)) {
            return (UserCredentialStore) session.userLocalStorage();
        } else {
            return (UserCredentialStore) session.userFederatedStorage();
        }
    }

    @Override
    public void updateCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        throwExceptionIfInvalidUser(user);
        getStoreForUser(user).updateCredential(realm, user, cred);
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        throwExceptionIfInvalidUser(user);
        return getStoreForUser(user).createCredential(realm, user, cred);
    }

    @Override
    public boolean removeStoredCredential(RealmModel realm, UserModel user, String id) {
        throwExceptionIfInvalidUser(user);
        boolean removalResult = getStoreForUser(user).removeStoredCredential(realm, user, id);
        UserCache userCache = session.userCache();
        if (userCache != null) {          
          userCache.evict(realm, user);
        }
        return removalResult;
    }

    @Override
    public CredentialModel getStoredCredentialById(RealmModel realm, UserModel user, String id) {
        return getStoreForUser(user).getStoredCredentialById(realm, user, id);
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsStream(RealmModel realm, UserModel user) {
        return getStoreForUser(user).getStoredCredentialsStream(realm, user);
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsByTypeStream(RealmModel realm, UserModel user, String type) {
        return getStoreForUser(user).getStoredCredentialsByTypeStream(realm, user, type);
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(RealmModel realm, UserModel user, String name, String type) {
        return getStoreForUser(user).getStoredCredentialByNameAndType(realm, user, name, type);
    }

    @Override
    public boolean moveCredentialTo(RealmModel realm, UserModel user, String id, String newPreviousCredentialId){
        throwExceptionIfInvalidUser(user);
        return getStoreForUser(user).moveCredentialTo(realm, user, id, newPreviousCredentialId);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput... inputs) {
        return isValid(realm, user, Arrays.asList(inputs));
    }

    @Override
    public CredentialModel createCredentialThroughProvider(RealmModel realm, UserModel user, CredentialModel model){
        throwExceptionIfInvalidUser(user);
        return session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(CredentialProvider.class)
                .map(f -> session.getProvider(CredentialProvider.class, f.getId()))
                .filter(provider -> Objects.equals(provider.getType(), model.getType()))
                .map(cp -> cp.createCredential(realm, user, cp.getCredentialFromModel(model)))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void updateCredentialLabel(RealmModel realm, UserModel user, String credentialId, String userLabel){
        throwExceptionIfInvalidUser(user);
        CredentialModel credential = getStoredCredentialById(realm, user, credentialId);
        credential.setUserLabel(userLabel);
        getStoreForUser(user).updateCredential(realm, user, credential);
        UserCache userCache = session.userCache();
        if (userCache != null) {
            userCache.evict(realm, user);
        }
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, List<CredentialInput> inputs) {
        if (!isValid(user)) {
            return false;
        }

        List<CredentialInput> toValidate = new LinkedList<>(inputs);
        String providerId = StorageId.isLocalStorage(user) ? user.getFederationLink() : StorageId.resolveProviderId(user);
        if (providerId != null) {
            UserStorageProviderModel model = getStorageProviderModel(realm, providerId);
            if (model == null || !model.isEnabled()) return false;

            CredentialInputValidator validator = getStorageProviderInstance(model, CredentialInputValidator.class);
            if (validator != null) {
                validate(realm, user, toValidate, validator);
            }
        }

        if (toValidate.isEmpty()) return true;

        getCredentialProviders(session, CredentialInputValidator.class)
                .forEach(validator -> validate(realm, user, toValidate, validator));

        return toValidate.isEmpty();
    }

    private void validate(RealmModel realm, UserModel user, List<CredentialInput> toValidate, CredentialInputValidator validator) {
        toValidate.removeIf(input -> validator.supportsCredentialType(input.getType()) && validator.isValid(realm, user, input));
    }

    public static <T> Stream<T> getCredentialProviders(KeycloakSession session, Class<T> type) {
        return session.getKeycloakSessionFactory().getProviderFactoriesStream(CredentialProvider.class)
                .filter(f -> Types.supports(type, f, CredentialProviderFactory.class))
                .map(f -> (T) session.getProvider(CredentialProvider.class, f.getId()));
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        String providerId = StorageId.isLocalStorage(user) ? user.getFederationLink() : StorageId.resolveProviderId(user);
        if (!StorageId.isLocalStorage(user)) throwExceptionIfInvalidUser(user);

        if (providerId != null) {
            UserStorageProviderModel model = getStorageProviderModel(realm, providerId);
            if (model == null || !model.isEnabled()) return false;

            CredentialInputUpdater updater = getStorageProviderInstance(model, CredentialInputUpdater.class);
            if (updater != null && updater.supportsCredentialType(input.getType())) {
                if (updater.updateCredential(realm, user, input)) return true;
            }
        }

        return getCredentialProviders(session, CredentialInputUpdater.class)
                .filter(updater -> updater.supportsCredentialType(input.getType()))
                .anyMatch(updater -> updater.updateCredential(realm, user, input));
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        String providerId = StorageId.isLocalStorage(user) ? user.getFederationLink() : StorageId.resolveProviderId(user);
        if (!StorageId.isLocalStorage(user)) throwExceptionIfInvalidUser(user);
        if (providerId != null) {
            UserStorageProviderModel model = getStorageProviderModel(realm, providerId);
            if (model == null || !model.isEnabled()) return;

            CredentialInputUpdater updater = getStorageProviderInstance(model, CredentialInputUpdater.class);
            if (updater.supportsCredentialType(credentialType)) {
                updater.disableCredentialType(realm, user, credentialType);
            }
        }

        getCredentialProviders(session, CredentialInputUpdater.class)
                .filter(updater -> updater.supportsCredentialType(credentialType))
                .forEach(updater -> updater.disableCredentialType(realm, user, credentialType));
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        Stream<String> types = Stream.empty();
        String providerId = StorageId.isLocalStorage(user) ? user.getFederationLink() : StorageId.resolveProviderId(user);
        if (providerId != null) {
            UserStorageProviderModel model = getStorageProviderModel(realm, providerId);
            if (model == null || !model.isEnabled()) return types;

            CredentialInputUpdater updater = getStorageProviderInstance(model, CredentialInputUpdater.class);
            if (updater != null) types = updater.getDisableableCredentialTypesStream(realm, user);
        }

        return Stream.concat(types, getCredentialProviders(session, CredentialInputUpdater.class)
                .flatMap(updater -> updater.getDisableableCredentialTypesStream(realm, user)))
                .distinct();
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String type) {
        UserStorageCredentialConfigured userStorageConfigured = isConfiguredThroughUserStorage(realm, user, type);

        // Check if we can rely just on userStorage to decide if credential is configured for the user or not
        switch (userStorageConfigured) {
            case CONFIGURED: return true;
            case USER_STORAGE_DISABLED: return false;
        }

        // Check locally as a fallback
        return isConfiguredLocally(realm, user, type);
    }


    private enum UserStorageCredentialConfigured {
        CONFIGURED,
        USER_STORAGE_DISABLED,
        NOT_CONFIGURED
    }


    private UserStorageCredentialConfigured isConfiguredThroughUserStorage(RealmModel realm, UserModel user, String type) {
        String providerId = StorageId.isLocalStorage(user) ? user.getFederationLink() : StorageId.resolveProviderId(user);
        if (providerId != null) {
            UserStorageProviderModel model = getStorageProviderModel(realm, providerId);
            if (model == null || !model.isEnabled()) return UserStorageCredentialConfigured.USER_STORAGE_DISABLED;

            CredentialInputValidator validator = getStorageProviderInstance(model, CredentialInputValidator.class);
            if (validator != null && validator.supportsCredentialType(type) && validator.isConfiguredFor(realm, user, type)) {
                return UserStorageCredentialConfigured.CONFIGURED;
            }
        }

        return UserStorageCredentialConfigured.NOT_CONFIGURED;
    }

    @Override
    public boolean isConfiguredLocally(RealmModel realm, UserModel user, String type) {
        return getCredentialProviders(session, CredentialInputValidator.class)
                .anyMatch(validator -> validator.supportsCredentialType(type) && validator.isConfiguredFor(realm, user, type));
    }

    @Override
    public CredentialValidationOutput authenticate(KeycloakSession session, RealmModel realm, CredentialInput input) {
        Stream<CredentialAuthentication> credentialAuthenticationStream = getEnabledStorageProviders(realm, CredentialAuthentication.class);
        credentialAuthenticationStream = Stream.concat(credentialAuthenticationStream,
                getCredentialProviders(session, CredentialAuthentication.class));

        return credentialAuthenticationStream
                .filter(credentialAuthentication -> credentialAuthentication.supportsCredentialAuthenticationFor(input.getType()))
                .map(credentialAuthentication -> credentialAuthentication.authenticate(realm, input))
                .findFirst().orElse(null);
    }

    @Override
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
        getCredentialProviders(session, OnUserCache.class).forEach(validator -> validator.onCache(realm, user, delegate));
    }

    @Override
    public Stream<String> getConfiguredUserStorageCredentialTypesStream(RealmModel realm, UserModel user) {
        return getCredentialProviders(session, CredentialProvider.class).map(CredentialProvider::getType)
                .filter(credentialType -> UserStorageCredentialConfigured.CONFIGURED == isConfiguredThroughUserStorage(realm, user, credentialType));
    }

    @Override
    public void close() {

    }

    private boolean isValid(UserModel user) {
        return user != null && user.getServiceAccountClientLink() == null;
    }
    
    private void throwExceptionIfInvalidUser(UserModel user) {
        if (user == null || isValid(user)) {
            return;
        }
        throw new RuntimeException("You can not manage credentials for this user");
    }
}
