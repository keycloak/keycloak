/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.user;

import org.keycloak.credential.CredentialModel;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.Comparator;

@GenerateEntityImplementations
@DeepCloner.Root
public interface MapUserCredentialEntity extends UpdatableEntity {

    public static MapUserCredentialEntity fromModel(CredentialModel model) {
        MapUserCredentialEntity credentialEntity = new MapUserCredentialEntityImpl();
        String id = model.getId() == null ? KeycloakModelUtils.generateId() : model.getId();
        credentialEntity.setId(id);
        credentialEntity.setCreatedDate(model.getCreatedDate());
        credentialEntity.setUserLabel(model.getUserLabel());
        credentialEntity.setType(model.getType());
        credentialEntity.setSecretData(model.getSecretData());
        credentialEntity.setCredentialData(model.getCredentialData());

        return credentialEntity;
    }

    public static CredentialModel toModel(MapUserCredentialEntity entity) {
        CredentialModel model = new CredentialModel();
        model.setId(entity.getId());
        model.setType(entity.getType());
        model.setCreatedDate(entity.getCreatedDate());
        model.setUserLabel(entity.getUserLabel());
        model.setSecretData(entity.getSecretData());
        model.setCredentialData(entity.getCredentialData());
        return model;
    }

    String getId();
    void setId(String id);

    String getType();
    void setType(String type);

    String getUserLabel();
    void setUserLabel(String userLabel);

    Long getCreatedDate();
    void setCreatedDate(Long createdDate);

    String getSecretData();
    void setSecretData(String secretData);

    String getCredentialData();
    void setCredentialData(String credentialData);
}
