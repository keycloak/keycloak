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
import org.keycloak.common.Profile;
import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntity;
import org.keycloak.models.map.authorization.entity.MapPolicyEntity;
import org.keycloak.models.map.authorization.entity.MapResourceEntity;
import org.keycloak.models.map.authorization.entity.MapResourceServerEntity;
import org.keycloak.models.map.authorization.entity.MapScopeEntity;
import org.keycloak.models.map.common.AbstractMapProviderFactory;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.InvalidationHandler;

import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.REALM_BEFORE_REMOVE;
import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.RESOURCE_SERVER_BEFORE_REMOVE;
import static org.keycloak.models.map.common.AbstractMapProviderFactory.uniqueCounter;

/**
 * @author mhajas
 */
public class MapAuthorizationStoreFactory implements AmphibianProviderFactory<StoreFactory>, AuthorizationStoreFactory, EnvironmentDependentProviderFactory, InvalidationHandler {

    public static final String PROVIDER_ID = AbstractMapProviderFactory.PROVIDER_ID;

    private Config.Scope storageConfigScope;
    private final String uniqueKey = MapAuthorizationStoreFactory.class.getName() + uniqueCounter.incrementAndGet();

    @Override
    public StoreFactory create(KeycloakSession session) {
        MapAuthorizationStore authzStore = session.getAttribute(uniqueKey, MapAuthorizationStore.class);

        if (authzStore != null) return authzStore;

        final MapStorageProvider mapStorageProvider = AbstractMapProviderFactory.getProviderFactoryOrComponentFactory(session, storageConfigScope).create(session);
        AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);

        MapStorage<MapPermissionTicketEntity, PermissionTicket> permissionTicketStore = mapStorageProvider.getStorage(PermissionTicket.class);
        MapStorage<MapPolicyEntity, Policy> policyStore = mapStorageProvider.getStorage(Policy.class);
        MapStorage<MapResourceServerEntity, ResourceServer> resourceServerStore = mapStorageProvider.getStorage(ResourceServer.class);
        MapStorage<MapResourceEntity, Resource> resourceStore = mapStorageProvider.getStorage(Resource.class);
        MapStorage<MapScopeEntity, Scope> scopeStore = mapStorageProvider.getStorage(Scope.class);

        authzStore = new MapAuthorizationStore(session,
            permissionTicketStore,
            policyStore,
            resourceServerStore,
            resourceStore,
            scopeStore,
            provider
        );

        session.setAttribute(uniqueKey, authzStore);
        return authzStore;
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
        this.storageConfigScope = config.scope("storage");
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Authorization store provider";
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE);
    }

    @Override
    public void invalidate(KeycloakSession session, InvalidableObjectType type, Object... params) {
        if (type == REALM_BEFORE_REMOVE) {
            MapAuthorizationStore authorizationStore = (MapAuthorizationStore) session.getProvider(StoreFactory.class);
            RealmModel realm = (RealmModel) params[0];

            authorizationStore.getScopeStore().preRemove(realm);
            authorizationStore.getPolicyStore().preRemove(realm);
            authorizationStore.getResourceStore().preRemove(realm);
            authorizationStore.getPermissionTicketStore().preRemove(realm);
            authorizationStore.getResourceServerStore().preRemove(realm);
        } else if (type == RESOURCE_SERVER_BEFORE_REMOVE) {
            MapAuthorizationStore authorizationStore = (MapAuthorizationStore) session.getProvider(StoreFactory.class);
            ResourceServer resourceServer = (ResourceServer) params[0];

            authorizationStore.getScopeStore().preRemove(resourceServer);
            authorizationStore.getPolicyStore().preRemove(resourceServer);
            authorizationStore.getResourceStore().preRemove(resourceServer);
            authorizationStore.getPermissionTicketStore().preRemove(resourceServer);
        }
    }
}
