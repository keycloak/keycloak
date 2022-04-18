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
import org.keycloak.models.map.common.TimeAdapter;
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
        entity.setTimestamp(TimeAdapter.fromIntegerWithTimeInSecondsToLongWithTimeAsInSeconds(currentTime));
        entity.setExpiration(TimeAdapter.fromIntegerWithTimeInSecondsToLongWithTimeAsInSeconds(expiration));
        entity.setCount(count);
        entity.setRemainingCount(count);
        return entity;
    }

    static ClientInitialAccessModel toModel(MapClientInitialAccessEntity entity) {
        if (entity == null) return null;
        ClientInitialAccessModel model = new ClientInitialAccessModel();
        model.setId(entity.getId());
        Long timestamp = entity.getTimestamp();
        model.setTimestamp(timestamp == null ? 0 : TimeAdapter.fromLongWithTimeInSecondsToIntegerWithTimeInSeconds(timestamp));
        Long expiration = entity.getExpiration();
        model.setExpiration(expiration == null ? 0 : TimeAdapter.fromLongWithTimeInSecondsToIntegerWithTimeInSeconds(expiration));
        Integer count = entity.getCount();
        model.setCount(count == null ? 0 : count);
        Integer remainingCount = entity.getRemainingCount();
        model.setRemainingCount(remainingCount == null ? 0 : remainingCount);
        return model;
    }

    Long getTimestamp();
    void setTimestamp(Long timestamp);

    Long getExpiration();
    void setExpiration(Long expiration);

    Integer getCount();
    void setCount(Integer count);

    Integer getRemainingCount();
    void setRemainingCount(Integer remainingCount);
}
