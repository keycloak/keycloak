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
package org.keycloak.models.map.userSession;

import org.keycloak.Config.Scope;
import org.keycloak.common.Profile;
import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.map.common.AbstractMapProviderFactory;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;

import org.keycloak.models.map.storage.MapStorageSpi;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import static org.keycloak.models.utils.KeycloakModelUtils.getComponentFactory;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapUserSessionProviderFactory<UK, CK> implements AmphibianProviderFactory<UserSessionProvider>, UserSessionProviderFactory, ProviderEventListener, EnvironmentDependentProviderFactory {

    public static final String CONFIG_STORAGE_USER_SESSIONS = "storage-user-sessions";
    public static final String CONFIG_STORAGE_CLIENT_SESSIONS = "storage-client-sessions";
    public static final String PROVIDER_ID = AbstractMapProviderFactory.PROVIDER_ID;

    private Scope storageConfigScopeUserSessions;
    private Scope storageConfigScopeClientSessions;

    private Runnable onClose;

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void init(Scope config) {
        storageConfigScopeUserSessions = config.scope(AbstractMapProviderFactory.CONFIG_STORAGE + "-user-sessions");
        storageConfigScopeClientSessions = config.scope(AbstractMapProviderFactory.CONFIG_STORAGE + "-client-sessions");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(this);
        onClose = () -> factory.unregister(this);
    }

    @Override
    public void close() {
        AmphibianProviderFactory.super.close();
        onClose.run();
    }

    @Override
    public void loadPersistentSessions(KeycloakSessionFactory sessionFactory, int maxErrors, int sessionsPerSegment) {
    }

    @Override
    public MapUserSessionProvider<UK, CK> create(KeycloakSession session) {
        MapStorageProviderFactory storageProviderFactoryUs = (MapStorageProviderFactory) getComponentFactory(session.getKeycloakSessionFactory(),
          MapStorageProvider.class, storageConfigScopeUserSessions, MapStorageSpi.NAME);
        final MapStorageProvider factoryUs = storageProviderFactoryUs.create(session);
        MapStorage userSessionStore = factoryUs.getStorage(MapUserSessionEntity.class, UserSessionModel.class);

        MapStorageProviderFactory storageProviderFactoryCs = (MapStorageProviderFactory) getComponentFactory(session.getKeycloakSessionFactory(),
          MapStorageProvider.class, storageConfigScopeClientSessions, MapStorageSpi.NAME);
        final MapStorageProvider factoryCs = storageProviderFactoryCs.create(session);
        MapStorage clientSessionStore = factoryCs.getStorage(MapAuthenticatedClientSessionEntity.class, AuthenticatedClientSessionModel.class);

        return new MapUserSessionProvider<>(session, userSessionStore, clientSessionStore);
    }

    @Override
    public String getHelpText() {
        return "User session provider";
    }

    @Override
    public void onEvent(ProviderEvent event) {
        if (event instanceof UserModel.UserRemovedEvent) {
            UserModel.UserRemovedEvent userRemovedEvent = (UserModel.UserRemovedEvent) event;

            MapUserSessionProvider provider = MapUserSessionProviderFactory.this.create(userRemovedEvent.getKeycloakSession());
            provider.removeUserSessions(userRemovedEvent.getRealm(), userRemovedEvent.getUser());
        }
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE);
    }
}
