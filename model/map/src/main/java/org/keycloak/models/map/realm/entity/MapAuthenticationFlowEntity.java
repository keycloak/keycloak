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
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

public class MapAuthenticationFlowEntity extends UpdatableEntity.Impl {

    private String id;
    private String alias;
    private String description;
    private String providerId;
    private Boolean builtIn = false;
    private Boolean topLevel = false;


    private MapAuthenticationFlowEntity() {}

    public static MapAuthenticationFlowEntity fromModel(AuthenticationFlowModel model) {
        if (model == null) return null;
        MapAuthenticationFlowEntity entity = new MapAuthenticationFlowEntity();
        String id = model.getId() == null ? KeycloakModelUtils.generateId() : model.getId();
        entity.setId(id);
        entity.setAlias(model.getAlias());
        entity.setBuiltIn(model.isBuiltIn());
        entity.setDescription(model.getDescription());
        entity.setProviderId(model.getProviderId());
        entity.setTopLevel(model.isTopLevel());

        return entity;
    }

    public static AuthenticationFlowModel toModel(MapAuthenticationFlowEntity entity) {
        if (entity == null) return null;
        AuthenticationFlowModel model = new AuthenticationFlowModel();
        model.setId(entity.getId());
        model.setAlias(entity.getAlias());
        model.setBuiltIn(entity.isBuiltIn());
        model.setDescription(entity.getDescription());
        model.setProviderId(entity.getProviderId());
        model.setTopLevel(entity.isTopLevel());
        return model;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.updated = !Objects.equals(this.id, id);
        this.id = id;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.updated = !Objects.equals(this.alias, alias);
        this.alias = alias;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.updated = !Objects.equals(this.description, description);
        this.description = description;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.updated = !Objects.equals(this.providerId, providerId);
        this.providerId = providerId;
    }

    public Boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.updated = !Objects.equals(this.builtIn, builtIn);
        this.builtIn = builtIn;
    }

    public Boolean isTopLevel() {
        return topLevel;
    }

    public void setTopLevel(boolean topLevel) {
        this.updated = !Objects.equals(this.topLevel, topLevel);
        this.topLevel = topLevel;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MapAuthenticationFlowEntity)) return false;
        final MapAuthenticationFlowEntity other = (MapAuthenticationFlowEntity) obj;
        return Objects.equals(other.getId(), getId());
    }
}
