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

import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.map.common.UpdatableEntity;

import java.util.Objects;

public class UserFederatedIdentityEntity implements UpdatableEntity {
    private String token;
    private String userId;
    private String identityProvider;
    private String userName;
    private boolean updated;
    
    private UserFederatedIdentityEntity() {}

    public static UserFederatedIdentityEntity fromModel(FederatedIdentityModel model) {
        if (model == null) return null;
        UserFederatedIdentityEntity entity = new UserFederatedIdentityEntity();
        entity.setIdentityProvider(model.getIdentityProvider());
        entity.setUserId(model.getUserId());
        entity.setUserName(model.getUserName().toLowerCase());
        entity.setToken(model.getToken());

        return entity;
    }

    public static FederatedIdentityModel toModel(UserFederatedIdentityEntity entity) {
        if (entity == null) return null;
        return new FederatedIdentityModel(entity.getIdentityProvider(), entity.getUserId(), entity.getUserName(), entity.getToken());
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.updated |= !Objects.equals(this.token, token);
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.updated |= !Objects.equals(this.userId, userId);
        this.userId = userId;
    }

    public String getIdentityProvider() {
        return identityProvider;
    }

    public void setIdentityProvider(String identityProvider) {
        this.updated |= !Objects.equals(this.identityProvider, identityProvider);
        this.identityProvider = identityProvider;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.updated |= !Objects.equals(this.userName, userName);
        this.userName = userName;
    }

    @Override
    public boolean isUpdated() {
        return updated;
    }
}
