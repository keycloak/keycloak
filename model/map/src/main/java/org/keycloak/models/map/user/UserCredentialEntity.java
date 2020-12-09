/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.map.common.UpdatableEntity;

import java.util.Objects;

public class UserCredentialEntity implements UpdatableEntity {

    private String id;
    private String type;
    private String userLabel;
    private Long createdDate;
    private String secretData;
    private String credentialData;
    private boolean updated;

    UserCredentialEntity() {}

    public static UserCredentialEntity fromModel(CredentialModel model) {
        UserCredentialEntity credentialEntity = new UserCredentialEntity();
        String id = model.getId() == null ? KeycloakModelUtils.generateId() : model.getId();
        credentialEntity.setId(id);
        credentialEntity.setCreatedDate(model.getCreatedDate());
        credentialEntity.setUserLabel(model.getUserLabel());
        credentialEntity.setType(model.getType());
        credentialEntity.setSecretData(model.getSecretData());
        credentialEntity.setCredentialData(model.getCredentialData());

        return credentialEntity;
    }

    public static CredentialModel toModel(UserCredentialEntity entity) {
        CredentialModel model = new CredentialModel();
        model.setId(entity.getId());
        model.setType(entity.getType());
        model.setCreatedDate(entity.getCreatedDate());
        model.setUserLabel(entity.getUserLabel());
        model.setSecretData(entity.getSecretData());
        model.setCredentialData(entity.getCredentialData());
        return model;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.updated |= !Objects.equals(this.id, id);
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.updated |= !Objects.equals(this.type, type);
        this.type = type;
    }

    public String getUserLabel() {
        return userLabel;
    }

    public void setUserLabel(String userLabel) {
        this.updated |= !Objects.equals(this.userLabel, userLabel);
        this.userLabel = userLabel;
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.updated |= !Objects.equals(this.createdDate, createdDate);
        this.createdDate = createdDate;
    }

    public String getSecretData() {
        return secretData;
    }

    public void setSecretData(String secretData) {
        this.updated |= !Objects.equals(this.secretData, secretData);
        this.secretData = secretData;
    }

    public String getCredentialData() {
        return credentialData;
    }

    public void setCredentialData(String credentialData) {
        this.updated |= !Objects.equals(this.credentialData, credentialData);
        this.credentialData = credentialData;
    }

    @Override
    public boolean isUpdated() {
        return updated;
    }
}
