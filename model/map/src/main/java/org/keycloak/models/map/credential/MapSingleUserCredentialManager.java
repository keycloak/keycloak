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

package org.keycloak.models.map.credential;

import org.keycloak.common.util.reflections.Types;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.credential.SingleUserCredentialManagerStrategy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUserCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.map.user.MapUserEntity;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Handling credentials for a given user.
 *
 * This serves as a wrapper to specific strategies. The wrapping code implements the logic for {@link CredentialInputUpdater}s
 * and {@link CredentialInputValidator}s. Storage specific strategies can be added like for example in
 * org.keycloak.models.map.credential.MapSingleUserCredentialManagerStrategy.
 *
 * @author Alexander Schwartz
 */
public class MapSingleUserCredentialManager implements SingleUserCredentialManager {

    private final UserModel user;
    private final KeycloakSession session;
    private final RealmModel realm;
    private final SingleUserCredentialManagerStrategy strategy;

    public MapSingleUserCredentialManager(KeycloakSession session, RealmModel realm, UserModel user, MapUserEntity entity) {
        this.user = user;
        this.session = session;
        this.realm = realm;
        this.strategy = new MapSingleUserCredentialManagerStrategy(entity);
    }

    @Override
    public boolean isValid(List<CredentialInput> inputs) {
        if (!isValid(user)) {
            return false;
        }

        List<CredentialInput> toValidate = new LinkedList<>(inputs);

        strategy.validateCredentials(toValidate);

        getCredentialProviders(session, CredentialInputValidator.class)
                .forEach(validator -> validate(realm, user, toValidate, validator));

        return toValidate.isEmpty();
    }

    @Override
    public boolean updateCredential(CredentialInput input) {
        return strategy.updateCredential(input) ||
                getCredentialProviders(session, CredentialInputUpdater.class)
                        .filter(updater -> updater.supportsCredentialType(input.getType()))
                        .anyMatch(updater -> updater.updateCredential(realm, user, input));
    }

    @Override
    public void updateStoredCredential(CredentialModel cred) {
        throwExceptionIfInvalidUser(user);
        strategy.updateStoredCredential(cred);
    }

    @Override
    public CredentialModel createStoredCredential(CredentialModel cred) {
        throwExceptionIfInvalidUser(user);
        return strategy.createStoredCredential(cred);
    }

    @Override
    public boolean removeStoredCredentialById(String id) {
        throwExceptionIfInvalidUser(user);
        return strategy.removeStoredCredentialById(id);
    }

    @Override
    public CredentialModel getStoredCredentialById(String id) {
        return strategy.getStoredCredentialById(id);
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsStream() {
        return strategy.getStoredCredentialsStream();
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsByTypeStream(String type) {
        return strategy.getStoredCredentialsByTypeStream(type);
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(String name, String type) {
        return strategy.getStoredCredentialByNameAndType(name, type);
    }

    @Override
    public boolean moveStoredCredentialTo(String id, String newPreviousCredentialId) {
        throwExceptionIfInvalidUser(user);
        return strategy.moveStoredCredentialTo(id, newPreviousCredentialId);
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
        getCredentialProviders(session, CredentialInputUpdater.class)
                .filter(updater -> updater.supportsCredentialType(credentialType))
                .forEach(updater -> updater.disableCredentialType(realm, user, credentialType));
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream() {
        // TODO: ask the store
        return getCredentialProviders(session, CredentialInputUpdater.class)
                        .flatMap(updater -> updater.getDisableableCredentialTypesStream(realm, user));
    }

    @Override
    public boolean isConfiguredFor(String type) {
        // TODO: ask the store
        return isConfiguredLocally(type);
    }

    @Override
    public boolean isConfiguredLocally(String type) {
        return getCredentialProviders(session, CredentialInputValidator.class)
                .anyMatch(validator -> validator.supportsCredentialType(type) && validator.isConfiguredFor(realm, user, type));
    }

    @Override
    public Stream<String> getConfiguredUserStorageCredentialTypesStream(UserModel user) {
        // TODO ask the store
        return getCredentialProviders(session, CredentialProvider.class).map(CredentialProvider::getType)
                .filter(credentialType -> UserStorageCredentialConfigured.CONFIGURED == isConfiguredThroughUserStorage(realm, user, credentialType));
    }

    @Override
    public CredentialModel createCredentialThroughProvider(CredentialModel model) {
        throwExceptionIfInvalidUser(user);
        return session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(CredentialProvider.class)
                .map(f -> session.getProvider(CredentialProvider.class, f.getId()))
                .filter(provider -> Objects.equals(provider.getType(), model.getType()))
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
        return UserStorageCredentialConfigured.NOT_CONFIGURED;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isValid(UserModel user) {
        Objects.requireNonNull(user);
        return user.getServiceAccountClientLink() == null;
    }

    private void validate(RealmModel realm, UserModel user, List<CredentialInput> toValidate, CredentialInputValidator validator) {
        toValidate.removeIf(input -> validator.supportsCredentialType(input.getType()) && validator.isValid(realm, user, input));
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

}

