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

import org.keycloak.common.util.Time;
import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

@GenerateEntityImplementations
@DeepCloner.Root
public interface MapClientInitialAccessEntity extends UpdatableEntity, AbstractEntity {
    static MapClientInitialAccessEntity createEntity(int expiration, int count) {
        int currentTime = Time.currentTime();

        MapClientInitialAccessEntity entity = new MapClientInitialAccessEntityImpl();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setTimestamp(currentTime);
        entity.setExpiration(expiration);
        entity.setCount(count);
        entity.setRemainingCount(count);
        return entity;
    }

    static ClientInitialAccessModel toModel(MapClientInitialAccessEntity entity) {
        if (entity == null) return null;
        ClientInitialAccessModel model = new ClientInitialAccessModel();
        model.setId(entity.getId());
        Integer timestamp = entity.getTimestamp();
        model.setTimestamp(timestamp == null ? 0 : timestamp);
        Integer expiration = entity.getExpiration();
        model.setExpiration(expiration == null ? 0 : expiration);
        Integer count = entity.getCount();
        model.setCount(count == null ? 0 : count);
        Integer remainingCount = entity.getRemainingCount();
        model.setRemainingCount(remainingCount == null ? 0 : remainingCount);
        return model;
    }

    Integer getTimestamp();
    void setTimestamp(Integer timestamp);

    Integer getExpiration();
    void setExpiration(Integer expiration);

    Integer getCount();
    void setCount(Integer count);

    Integer getRemainingCount();
    void setRemainingCount(Integer remainingCount);
}
