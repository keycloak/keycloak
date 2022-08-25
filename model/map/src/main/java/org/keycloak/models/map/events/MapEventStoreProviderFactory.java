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

package org.keycloak.models.map.events;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.events.Event;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventStoreProviderFactory;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.common.AbstractMapProviderFactory;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.InvalidationHandler;

import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.REALM_BEFORE_REMOVE;
import static org.keycloak.models.map.common.AbstractMapProviderFactory.uniqueCounter;

public class MapEventStoreProviderFactory implements AmphibianProviderFactory<EventStoreProvider>, EnvironmentDependentProviderFactory, EventStoreProviderFactory, InvalidationHandler {

    public static final String PROVIDER_ID = AbstractMapProviderFactory.PROVIDER_ID;
    private Config.Scope storageConfigScopeAdminEvents;
    private Config.Scope storageConfigScopeLoginEvents;
    private final String uniqueKey = getClass().getName() + uniqueCounter.incrementAndGet();


    @Override
    public void init(Config.Scope config) {
        storageConfigScopeAdminEvents = config.scope(AbstractMapProviderFactory.CONFIG_STORAGE + "-admin-events");
        storageConfigScopeLoginEvents = config.scope(AbstractMapProviderFactory.CONFIG_STORAGE + "-auth-events");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public EventStoreProvider create(KeycloakSession session) {
        MapEventStoreProvider provider = session.getAttribute(uniqueKey, MapEventStoreProvider.class);
        if (provider != null) return provider;

        final MapStorageProvider factoryAe = AbstractMapProviderFactory.getProviderFactoryOrComponentFactory(session, storageConfigScopeAdminEvents).create(session);
        MapStorage<MapAdminEventEntity, AdminEvent> adminEventsStore = factoryAe.getStorage(AdminEvent.class);

        final MapStorageProvider factoryLe = AbstractMapProviderFactory.getProviderFactoryOrComponentFactory(session, storageConfigScopeLoginEvents).create(session);
        MapStorage<MapAuthEventEntity, Event> loginEventsStore = factoryLe.getStorage(Event.class);

        provider = new MapEventStoreProvider(session, loginEventsStore, adminEventsStore);
        session.setAttribute(uniqueKey, provider);
        return provider;
    }

    @Override
    public void invalidate(KeycloakSession session, InvalidationHandler.InvalidableObjectType type, Object... params) {
        if (type == REALM_BEFORE_REMOVE) {
            create(session).clear((RealmModel) params[0]);
            create(session).clearAdmin((RealmModel) params[0]);
        }
    }

    @Override
    public void close() {
        AmphibianProviderFactory.super.close();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Event provider";
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE);
    }
}
