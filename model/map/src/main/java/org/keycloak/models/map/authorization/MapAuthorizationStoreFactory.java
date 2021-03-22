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

package org.keycloak.models.map.authorization;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.AuthorizationStoreFactory;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntity;
import org.keycloak.models.map.authorization.entity.MapPolicyEntity;
import org.keycloak.models.map.authorization.entity.MapResourceEntity;
import org.keycloak.models.map.authorization.entity.MapResourceServerEntity;
import org.keycloak.models.map.authorization.entity.MapScopeEntity;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;

import java.util.UUID;

/**
 * @author mhajas
 */
public class MapAuthorizationStoreFactory implements AuthorizationStoreFactory {

    private MapStorage<UUID, MapPermissionTicketEntity, PermissionTicket> permissionTicketStore;
    private MapStorage<UUID, MapPolicyEntity, Policy> policyStore;
    private MapStorage<String, MapResourceServerEntity, ResourceServer> resourceServerStore;
    private MapStorage<UUID, MapResourceEntity, Resource> resourceStore;
    private MapStorage<UUID, MapScopeEntity, Scope> scopeStore;

    @Override
    public StoreFactory create(KeycloakSession session) {
        AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);
        return new MapAuthorizationStore(session,
                    permissionTicketStore,
                    policyStore,
                    resourceServerStore,
                    resourceStore,
                    scopeStore,
                    provider
                );
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        AuthorizationStoreFactory.super.postInit(factory);
        
        MapStorageProvider mapStorageProvider = (MapStorageProvider) factory.getProviderFactory(MapStorageProvider.class);
        permissionTicketStore = mapStorageProvider.getStorage("authzPermissionTickets", UUID.class, MapPermissionTicketEntity.class, PermissionTicket.class);
        policyStore = mapStorageProvider.getStorage("authzPolicies", UUID.class, MapPolicyEntity.class, Policy.class);
        resourceServerStore = mapStorageProvider.getStorage("authzResourceServers", String.class, MapResourceServerEntity.class, ResourceServer.class);
        resourceStore = mapStorageProvider.getStorage("authzResources", UUID.class, MapResourceEntity.class, Resource.class);
        scopeStore = mapStorageProvider.getStorage("authzScopes", UUID.class, MapScopeEntity.class, Scope.class);

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "map";
    }
}
