/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.keycloak.common.util.reflections.Types;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.AbstractStorageManager;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.StoreManagers;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.tracing.TracingProvider;

import io.opentelemetry.api.trace.StatusCode;

/**
 * Handling credentials for a given user for the store.
 *
 * @author Alexander Schwartz
 */
public class UserCredentialManager extends AbstractStorageManager<UserStorageProvider, UserStorageProviderModel> implements org.keycloak.models.UserCredentialManager {

    private final UserModel user;
    private final KeycloakSession session;
    private final RealmModel realm;

    /**
     * It is not recommended to use this method directly from your user-storage providers! Please use {@link org.keycloak.models.UserProvider#getUserCredentialManager(UserModel) session.users().getUserCredentialManager(user)} instead.
     */
    public UserCredentialManager(KeycloakSession session, RealmModel realm, UserModel user) {
        super(session, UserStorageProviderFactory.class, UserStorageProvider.class, UserStorageProviderModel::new, "user");
        this.user = user;
        this.session = session;
        this.realm = realm;
    }

    @Override
    public boolean isValid(List<CredentialInput> inputs) {
        if (!isValid(user)) {
            return false;
        }

        List<CredentialInput> toValidate = new LinkedList<>(inputs);

        if (user.isFederated()) {
            UserStorageProviderModel model = getStorageProviderModel(realm, user.getFederationLink());
            if (model == null || !model.isEnabled()) return false;

            CredentialInputValidator validator = getStorageProviderInstance(model, CredentialInputValidator.class);
            if (validator != null) {
                validate(realm, user, toValidate, validator);
            }
        }

        getCredentialProviders(session, CredentialInputValidator.class)
                .forEach(validator -> validate(realm, user, toValidate, validator));

        return toValidate.isEmpty();
    }

    @Override
    public boolean updateCredential(CredentialInput input) {
        if (!StorageId.isLocalStorage(user.getId())) throwExceptionIfInvalidUser(user);

        if (user.isFederated()) {
            UserStorageProviderModel model = getStorageProviderModel(realm, user.getFederationLink());
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
    public void updateStoredCredential(CredentialModel cred) {
        throwExceptionIfInvalidUser(user);
        getStoreForUser(user).updateCredential(realm, user, cred);
    }

    @Override
    public CredentialModel createStoredCredential(CredentialModel cred) {
        throwExceptionIfInvalidUser(user);
        return getStoreForUser(user).createCredential(realm, user, cred);
    }

    @Override
    public boolean removeStoredCredentialById(String id) {
        throwExceptionIfInvalidUser(user);
        return getStoreForUser(user).removeStoredCredential(realm, user, id);
    }

    @Override
    public CredentialModel getStoredCredentialById(String id) {
        return getStoreForUser(user).getStoredCredentialById(realm, user, id);
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsStream() {
        return getStoreForUser(user).getStoredCredentialsStream(realm, user);
    }

    @Override
    public Stream<CredentialModel> getFederatedCredentialsStream() {
        if (user.isFederated()) {
            UserStorageProviderModel model = getStorageProviderModel(realm, user.getFederationLink());

            if (model == null || !model.isEnabled()) {
                return Stream.empty();
            }

            CredentialInputUpdater credentialProvider = getStorageProviderInstance(model, CredentialInputUpdater.class);

            if (credentialProvider != null) {
                return credentialProvider.getCredentials(realm, user);
            }
        }

        return Stream.empty();
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsByTypeStream(String type) {
        return getStoreForUser(user).getStoredCredentialsByTypeStream(realm, user, type);
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(String name, String type) {
        return getStoreForUser(user).getStoredCredentialByNameAndType(realm, user, name, type);
    }

    @Override
    public boolean moveStoredCredentialTo(String id, String newPreviousCredentialId) {
        throwExceptionIfInvalidUser(user);
        return getStoreForUser(user).moveCredentialTo(realm, user, id, newPreviousCredentialId);
    }

    @Override
    public void updateCredentialLabel(String credentialId, String userLabel) {
        throwExceptionIfInvalidUser(user);
        CredentialModel credential = getStoredCredentialById(credentialId);
        credential.setUserLabel(userLabel);
        updateStoredCredential(credential);
    }

    @Override
    public void disableCredentialType(String credentialType) {
        if (!StorageId.isLocalStorage(user.getId())) throwExceptionIfInvalidUser(user);
        if (user.isFederated()) {
            UserStorageProviderModel model = getStorageProviderModel(realm, user.getFederationLink());
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
    public Stream<String> getDisableableCredentialTypesStream() {
        Stream<String> types = Stream.empty();
        if (user.isFederated()) {
            UserStorageProviderModel model = getStorageProviderModel(realm, user.getFederationLink());
            if (model == null || !model.isEnabled()) return types;

            CredentialInputUpdater updater = getStorageProviderInstance(model, CredentialInputUpdater.class);
            if (updater != null) types = updater.getDisableableCredentialTypesStream(realm, user);
        }

        return Stream.concat(types, getCredentialProviders(session, CredentialInputUpdater.class)
                        .flatMap(updater -> updater.getDisableableCredentialTypesStream(realm, user)))
                .distinct();
    }

    @Override
    public boolean isConfiguredFor(String type) {
        UserStorageCredentialConfigured userStorageConfigured = isConfiguredThroughUserStorage(realm, user, type);

        // Check if we can rely just on userStorage to decide if credential is configured for the user or not
        switch (userStorageConfigured) {
            case CONFIGURED: return true;
            case USER_STORAGE_DISABLED: return false;
        }

        // Check locally as a fallback
        return isConfiguredLocally(type);
    }

    @Override
    public boolean isConfiguredLocally(String type) {
        return getCredentialProviders(session, CredentialInputValidator.class)
                .anyMatch(validator -> validator.supportsCredentialType(type) && validator.isConfiguredFor(realm, user, type));
    }

    @Override
    public Stream<String> getConfiguredUserStorageCredentialTypesStream() {
        return getCredentialProviders(session, CredentialProvider.class).map(CredentialProvider::getType)
                .filter(credentialType -> UserStorageCredentialConfigured.CONFIGURED == isConfiguredThroughUserStorage(realm, user, credentialType));
    }

    @Override
    public CredentialModel createCredentialThroughProvider(CredentialModel model) {
        throwExceptionIfInvalidUser(user);
        return session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(CredentialProvider.class)
                .map(f -> session.getProvider(CredentialProvider.class, f.getId()))
                .filter(provider -> provider.supportsCredentialType(model))
                .map(cp -> cp.createCredential(realm, user, cp.getCredentialFromModel(model)))
                .findFirst()
                .orElse(null);
    }

    private enum UserStorageCredentialConfigured {
        CONFIGURED,
        USER_STORAGE_DISABLED,
        NOT_CONFIGURED
    }

    private UserStorageCredentialConfigured isConfiguredThroughUserStorage(RealmModel realm, UserModel user, String type) {
        if (user.isFederated()) {
            UserStorageProviderModel model = getStorageProviderModel(realm, user.getFederationLink());
            if (model == null || !model.isEnabled()) return UserStorageCredentialConfigured.USER_STORAGE_DISABLED;

            CredentialInputValidator validator = getStorageProviderInstance(model, CredentialInputValidator.class);
            if (validator != null && validator.supportsCredentialType(type) && validator.isConfiguredFor(realm, user, type)) {
                return UserStorageCredentialConfigured.CONFIGURED;
            }
        }

        return UserStorageCredentialConfigured.NOT_CONFIGURED;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isValid(UserModel user) {
        Objects.requireNonNull(user);
        return user.getServiceAccountClientLink() == null;
    }

    private void validate(RealmModel realm, UserModel user, List<CredentialInput> toValidate, CredentialInputValidator validator) {
        toValidate.removeIf(input -> {
            if(validator.supportsCredentialType(input.getType())) {
                return session.getProvider(TracingProvider.class).trace(validator.getClass(), "isValid", span -> {
                    boolean valid = validator.isValid(realm, user, input);
                    if (!valid) {
                        span.setStatus(StatusCode.ERROR);
                    }
                    return valid;
                });
            }
            return false;
        });
    }

    private static <T> Stream<T> getCredentialProviders(KeycloakSession session, Class<T> type) {
        //noinspection unchecked
        return session.getKeycloakSessionFactory().getProviderFactoriesStream(CredentialProvider.class)
                .filter(f -> Types.supports(type, f, CredentialProviderFactory.class))
                .map(f -> (T) session.getProvider(CredentialProvider.class, f.getId()));
    }

    private void throwExceptionIfInvalidUser(UserModel user) {
        if (!isValid(user)) {
            throw new RuntimeException("You can not manage credentials for this user");
        }
    }

    private UserCredentialStore getStoreForUser(UserModel user) {
        StoreManagers p = (StoreManagers) session.getProvider(DatastoreProvider.class);
        if (StorageId.isLocalStorage(user.getId())) {
            return (UserCredentialStore) p.userLocalStorage();
        } else {
            return (UserCredentialStore) p.userFederatedStorage();
        }
    }

}
