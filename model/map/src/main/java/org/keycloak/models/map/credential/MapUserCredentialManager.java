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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.map.user.MapUserCredentialEntity;
import org.keycloak.models.map.user.MapUserEntity;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Handling credentials for a given user.
 *
 * This serves as a wrapper to specific strategies. The wrapping code implements the logic for {@link CredentialInputUpdater}s
 * and {@link CredentialInputValidator}s.
 *
 * @author Alexander Schwartz
 */
public class MapUserCredentialManager implements SubjectCredentialManager {

    private final UserModel user;
    private final KeycloakSession session;
    private final RealmModel realm;
    private final MapUserEntity entity;

    public MapUserCredentialManager(KeycloakSession session, RealmModel realm, UserModel user, MapUserEntity entity) {
        this.user = user;
        this.session = session;
        this.realm = realm;
        this.entity = entity;
    }

    @Override
    public boolean isValid(List<CredentialInput> inputs) {
        if (!isValid(user)) {
            return false;
        }

        List<CredentialInput> toValidate = new LinkedList<>(inputs);

        entity.credentialManager().validateCredentials(toValidate);

        getCredentialProviders(session, CredentialInputValidator.class)
                .forEach(validator -> validate(realm, user, toValidate, validator));

        return toValidate.isEmpty();
    }

    @Override
    public boolean updateCredential(CredentialInput input) {
        return entity.credentialManager().updateCredential(input) ||
                getCredentialProviders(session, CredentialInputUpdater.class)
                        .filter(updater -> updater.supportsCredentialType(input.getType()))
                        .anyMatch(updater -> updater.updateCredential(realm, user, input));
    }

    @Override
    public void updateStoredCredential(CredentialModel cred) {
        throwExceptionIfInvalidUser(user);
        entity.getCredential(cred.getId()).ifPresent(c -> {
            c.setCreatedDate(cred.getCreatedDate());
            c.setUserLabel(cred.getUserLabel());
            c.setType(cred.getType());
            c.setSecretData(cred.getSecretData());
            c.setCredentialData(cred.getCredentialData());
        });
    }

    @Override
    public CredentialModel createStoredCredential(CredentialModel cred) {
        throwExceptionIfInvalidUser(user);
        MapUserCredentialEntity credentialEntity = MapUserCredentialEntity.fromModel(cred);

        if (entity.getCredential(cred.getId()).isPresent()) {
            throw new ModelDuplicateException("A CredentialModel with given id already exists");
        }

        entity.addCredential(credentialEntity);

        return MapUserCredentialEntity.toModel(credentialEntity);
    }

    @Override
    public boolean removeStoredCredentialById(String id) {
        throwExceptionIfInvalidUser(user);
        return entity.removeCredential(id);
    }

    @Override
    public CredentialModel getStoredCredentialById(String id) {
        return entity.getCredential(id).map(MapUserCredentialEntity::toModel).orElse(null);
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsStream() {
        return Optional.ofNullable(entity.getCredentials()).orElse(Collections.emptyList()).stream()
                .map(MapUserCredentialEntity::toModel);
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsByTypeStream(String type) {
        return getStoredCredentialsStream()
                .filter(credential -> Objects.equals(type, credential.getType()));
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(String name, String type) {
        return getStoredCredentialsStream()
                .filter(credential -> Objects.equals(name, credential.getUserLabel()))
                .findFirst().orElse(null);
    }

    @Override
    public boolean moveStoredCredentialTo(String id, String newPreviousCredentialId) {
        throwExceptionIfInvalidUser(user);
        return entity.moveCredential(id, newPreviousCredentialId);
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
        return Stream.concat(entity.credentialManager().getDisableableCredentialTypesStream(),
                        getCredentialProviders(session, CredentialInputUpdater.class)
                                .flatMap(updater -> updater.getDisableableCredentialTypesStream(realm, user)))
                .distinct();
    }

    @Override
    public boolean isConfiguredFor(String type) {
        return entity.credentialManager().isConfiguredFor(type) ||
                getCredentialProviders(session, CredentialInputValidator.class)
                        .anyMatch(validator -> validator.supportsCredentialType(type) && validator.isConfiguredFor(realm, user, type));
    }

    @Override
    @Deprecated
    public boolean isConfiguredLocally(String type) {
        throw new IllegalArgumentException("this is not supported for map storage");
    }

    @Override
    @Deprecated
    public Stream<String> getConfiguredUserStorageCredentialTypesStream() {
        // used in the old admin console for users to determine if a password is set for a user
        // not used in the new admin console
        return Stream.empty();
    }

    @Override
    @Deprecated
    public CredentialModel createCredentialThroughProvider(CredentialModel model) {
        // this is still called when importing/creating a user via RepresentationToModel.createCredentials
        throwExceptionIfInvalidUser(user);
        return session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(CredentialProvider.class)
                .map(f -> session.getProvider(CredentialProvider.class, f.getId()))
                .filter(provider -> Objects.equals(provider.getType(), model.getType()))
                .map(cp -> cp.createCredential(realm, user, cp.getCredentialFromModel(model)))
                .findFirst()
                .orElse(null);
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

