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

import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;

@GenerateEntityImplementations
@DeepCloner.Root
public interface MapUserFederatedIdentityEntity extends UpdatableEntity {

    public static MapUserFederatedIdentityEntity fromModel(FederatedIdentityModel model) {
        if (model == null) return null;
        MapUserFederatedIdentityEntity entity = new MapUserFederatedIdentityEntityImpl();
        entity.setIdentityProvider(model.getIdentityProvider());
        entity.setUserId(model.getUserId());
        entity.setUserName(model.getUserName().toLowerCase());
        entity.setToken(model.getToken());

        return entity;
    }

    public static FederatedIdentityModel toModel(MapUserFederatedIdentityEntity entity) {
        if (entity == null) return null;
        return new FederatedIdentityModel(entity.getIdentityProvider(), entity.getUserId(), entity.getUserName(), entity.getToken());
    }

    String getToken();
    void setToken(String token);

    String getUserId();
    void setUserId(String userId);

    String getIdentityProvider();
    void setIdentityProvider(String identityProvider);

    String getUserName();
    void setUserName(String userName);
}
