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

package org.keycloak.models.map.realm.entity;

import java.util.Objects;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.map.common.UpdatableEntity;

public class MapRequiredCredentialEntity extends UpdatableEntity.Impl {

    private String type;
    private String formLabel;
    private Boolean input = false;
    private Boolean secret = false;


    private MapRequiredCredentialEntity() {}

    public static MapRequiredCredentialEntity fromModel(RequiredCredentialModel model) {
        if (model == null) return null;
        MapRequiredCredentialEntity entity = new MapRequiredCredentialEntity();
        entity.setFormLabel(model.getFormLabel());
        entity.setType(model.getType());
        entity.setInput(model.isInput());
        entity.setSecret(model.isSecret());
        return entity;
    }

    public static RequiredCredentialModel toModel(MapRequiredCredentialEntity entity) {
        if (entity == null) return null;
        RequiredCredentialModel model = new RequiredCredentialModel();
        model.setFormLabel(entity.getFormLabel());
        model.setType(entity.getType());
        model.setSecret(entity.isSecret());
        model.setInput(entity.isInput());
        return model;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.updated = !Objects.equals(this.type, type);
        this.type = type;
    }

    public String getFormLabel() {
        return formLabel;
    }

    public void setFormLabel(String formLabel) {
        this.updated = !Objects.equals(this.formLabel, formLabel);
        this.formLabel = formLabel;
    }

    public Boolean isSecret() {
        return secret;
    }

    public void setSecret(boolean secret) {
        this.updated = !Objects.equals(this.formLabel, formLabel);
        this.secret = secret;
    }

    public Boolean isInput() {
        return input;
    }

    public void setInput(boolean input) {
        this.updated = !Objects.equals(this.input, input);
        this.input = input;
    }

    @Override
    public int hashCode() {
        return getType().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MapRequiredCredentialEntity)) return false;
        final MapRequiredCredentialEntity other = (MapRequiredCredentialEntity) obj;
        return Objects.equals(other.getType(), getType());
    }
}
