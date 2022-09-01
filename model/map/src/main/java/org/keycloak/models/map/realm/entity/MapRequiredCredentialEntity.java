/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;

@GenerateEntityImplementations
@DeepCloner.Root
public interface MapRequiredCredentialEntity extends UpdatableEntity {
    static MapRequiredCredentialEntity fromModel(RequiredCredentialModel model) {
        if (model == null) return null;
        MapRequiredCredentialEntity entity = new MapRequiredCredentialEntityImpl();
        entity.setFormLabel(model.getFormLabel());
        entity.setType(model.getType());
        entity.setInput(model.isInput());
        entity.setSecret(model.isSecret());
        return entity;
    }

    static RequiredCredentialModel toModel(MapRequiredCredentialEntity entity) {
        if (entity == null) return null;
        RequiredCredentialModel model = new RequiredCredentialModel();
        model.setFormLabel(entity.getFormLabel());
        model.setType(entity.getType());
        Boolean secret = entity.isSecret();
        model.setSecret(secret == null ? false : secret);
        Boolean input = entity.isInput();
        model.setInput(input == null ? false : input);
        return model;
    }

    String getType();
    void setType(String type);

    String getFormLabel();
    void setFormLabel(String formLabel);

    Boolean isSecret();
    void setSecret(Boolean secret);

    Boolean isInput();
    void setInput(Boolean input);
}
