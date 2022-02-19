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
package org.keycloak.models.map.storage.chm;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.StringKeyConvertor;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.UserModel;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.user.MapUserEntity;


public class UserConcurrentHashMapStorage<K> extends ConcurrentHashMapStorage<K, MapUserEntity, UserModel> {

    private final Scope config;

    public UserConcurrentHashMapStorage(Class<UserModel> modelClass, StringKeyConvertor<K> keyConvertor, DeepCloner cloner, Scope config) {
        super(modelClass, keyConvertor, cloner);
        this.config = config;
    }

    @Override
    public MapUserEntity create(MapUserEntity user) {

        updateUsernameLettercase(user);

        return super.create(user);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapKeycloakTransaction<MapUserEntity, UserModel> createTransaction(KeycloakSession session) {
        MapKeycloakTransaction<MapUserEntity, UserModel> sessionTransaction = session.getAttribute("map-transaction-" + hashCode(), MapKeycloakTransaction.class);
        return sessionTransaction == null ? new ConcurrentHashMapKeycloakTransaction<>(this, keyConvertor, cloner, fieldPredicates, config) : sessionTransaction;
    }

    @Override
    public MapModelCriteriaBuilder<K, MapUserEntity, UserModel> createCriteriaBuilder() {
        return new MapModelCriteriaBuilder<>(keyConvertor, fieldPredicates, config);
    }

    private void updateUsernameLettercase(MapUserEntity user) throws IllegalStateException {
        if (config == null) {
            throw new IllegalStateException();
        }
        switch (config.get("username-lettercase", "lowercase")) {
            case "lowercase":
                user.setUsername(user.getUsername().toLowerCase());
                break;
            case "uppercase":
                user.setUsername(user.getUsername().toUpperCase());
                break;
            case "as_is":
                // do nothing
                break;
            default:
                throw new IllegalStateException("Unknown Username lettercase: " + config.get("username-lettercase"));
        }
    }
}
