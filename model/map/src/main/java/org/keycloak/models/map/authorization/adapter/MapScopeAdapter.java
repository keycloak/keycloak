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

package org.keycloak.models.map.authorization.adapter;


import java.util.Objects;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.authorization.entity.MapScopeEntity;

public class MapScopeAdapter extends AbstractScopeModel<MapScopeEntity> {

    private final RealmModel realm;
    private ResourceServer resourceServer;

    public MapScopeAdapter(RealmModel realm, ResourceServer resourceServer, MapScopeEntity entity, StoreFactory storeFactory) {
        super(entity, storeFactory);
        Objects.requireNonNull(realm);
        this.realm = realm;
        this.resourceServer = resourceServer;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public void setName(String name) {
        throwExceptionIfReadonly();
        entity.setName(name);
    }

    @Override
    public String getDisplayName() {
        return entity.getDisplayName();
    }

    @Override
    public void setDisplayName(String name) {
        throwExceptionIfReadonly();
        entity.setDisplayName(name);
    }

    @Override
    public String getIconUri() {
        return entity.getIconUri();
    }

    @Override
    public void setIconUri(String iconUri) {
        throwExceptionIfReadonly();
        entity.setIconUri(iconUri);
    }

    @Override
    public ResourceServer getResourceServer() {
        if (resourceServer == null) {
            resourceServer = storeFactory.getResourceServerStore().findById(realm, entity.getResourceServerId());
        }
        return resourceServer;
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), System.identityHashCode(this));
    }
}
