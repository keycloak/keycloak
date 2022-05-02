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
import org.keycloak.models.RealmModel;
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
import org.keycloak.provider.InvalidationHandler;

import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.USER_BEFORE_REMOVE;
import static org.keycloak.models.map.common.AbstractMapProviderFactory.uniqueCounter;
import static org.keycloak.models.utils.KeycloakModelUtils.getComponentFactory;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapUserSessionProviderFactory<UK, CK> implements AmphibianProviderFactory<UserSessionProvider>, UserSessionProviderFactory, EnvironmentDependentProviderFactory, InvalidationHandler {

    public static final String CONFIG_STORAGE_USER_SESSIONS = "storage-user-sessions";
    public static final String CONFIG_STORAGE_CLIENT_SESSIONS = "storage-client-sessions";
    public static final String PROVIDER_ID = AbstractMapProviderFactory.PROVIDER_ID;

    private final String uniqueKey = getClass().getName() + uniqueCounter.incrementAndGet();

    private Scope storageConfigScopeUserSessions;
    private Scope storageConfigScopeClientSessions;

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
    }

    @Override
    public void close() {
        AmphibianProviderFactory.super.close();
    }

    @Override
    public void loadPersistentSessions(KeycloakSessionFactory sessionFactory, int maxErrors, int sessionsPerSegment) {
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapUserSessionProvider create(KeycloakSession session) {
        MapUserSessionProvider provider = session.getAttribute(uniqueKey, MapUserSessionProvider.class);

        if (provider != null) return provider;

        MapStorageProviderFactory storageProviderFactoryUs = (MapStorageProviderFactory) getComponentFactory(session.getKeycloakSessionFactory(),
          MapStorageProvider.class, storageConfigScopeUserSessions, MapStorageSpi.NAME);
        final MapStorageProvider factoryUs = storageProviderFactoryUs.create(session);
        MapStorage userSessionStore = factoryUs.getStorage(UserSessionModel.class);

        MapStorageProviderFactory storageProviderFactoryCs = (MapStorageProviderFactory) getComponentFactory(session.getKeycloakSessionFactory(),
          MapStorageProvider.class, storageConfigScopeClientSessions, MapStorageSpi.NAME);
        final MapStorageProvider factoryCs = storageProviderFactoryCs.create(session);
        MapStorage clientSessionStore = factoryCs.getStorage(AuthenticatedClientSessionModel.class);

        provider = new MapUserSessionProvider(session, userSessionStore, clientSessionStore);
        session.setAttribute(uniqueKey, provider);
        return provider;
    }

    @Override
    public String getHelpText() {
        return "User session provider";
    }

    @Override
    public void invalidate(KeycloakSession session, InvalidableObjectType type, Object... params) {
        if (type == USER_BEFORE_REMOVE) {
            create(session).removeUserSessions((RealmModel) params[0], (UserModel) params[1]);
        }
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE);
    }
}
