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

import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.SingleUserCredentialManagerStrategy;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.map.user.MapUserCredentialEntity;
import org.keycloak.models.map.user.MapUserEntity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Implementation of {@link SingleUserCredentialManagerStrategy} for map storages.
 * Will delegate calls to the credential manager in the entity.
 *
 * @author Alexander Schwartz
 */
public class MapSingleUserCredentialManagerStrategy implements SingleUserCredentialManagerStrategy {
    private final MapUserEntity entity;

    public MapSingleUserCredentialManagerStrategy(MapUserEntity entity) {
        this.entity = entity;
    }

    @Override
    public void validateCredentials(List<CredentialInput> toValidate) {
        entity.getUserCredentialManager().validateCredentials(toValidate);
    }

    @Override
    public boolean updateCredential(CredentialInput input) {
        return entity.getUserCredentialManager().updateCredential(input);
    }

    @Override
    public void updateStoredCredential(CredentialModel credentialModel) {
        entity.getCredential(credentialModel.getId()).ifPresent(c -> {
            c.setCreatedDate(credentialModel.getCreatedDate());
            c.setUserLabel(credentialModel.getUserLabel());
            c.setType(credentialModel.getType());
            c.setSecretData(credentialModel.getSecretData());
            c.setCredentialData(credentialModel.getCredentialData());
        });
    }

    @Override
    public CredentialModel createStoredCredential(CredentialModel cred) {
        MapUserCredentialEntity credentialEntity = MapUserCredentialEntity.fromModel(cred);

        if (entity.getCredential(cred.getId()).isPresent()) {
            throw new ModelDuplicateException("A CredentialModel with given id already exists");
        }

        entity.addCredential(credentialEntity);

        return MapUserCredentialEntity.toModel(credentialEntity);
    }

    @Override
    public Boolean removeStoredCredentialById(String id) {
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
        return entity.moveCredential(id, newPreviousCredentialId);
    }
}
