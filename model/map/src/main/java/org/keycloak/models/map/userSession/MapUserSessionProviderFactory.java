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

import java.util.UUID;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapUserSessionProviderFactory extends AbstractMapProviderFactory<UserSessionProvider>
        implements UserSessionProviderFactory {

    private MapStorage<UUID, MapUserSessionEntity, UserSessionModel> userSessionStore;
    private MapStorage<UUID, MapAuthenticatedClientSessionEntity, AuthenticatedClientSessionModel> clientSessionStore;

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        MapStorageProvider sp = (MapStorageProvider) factory.getProviderFactory(MapStorageProvider.class);
        userSessionStore = sp.getStorage("userSessions", UUID.class, MapUserSessionEntity.class, UserSessionModel.class);
        clientSessionStore = sp.getStorage("clientSessions", UUID.class, MapAuthenticatedClientSessionEntity.class, AuthenticatedClientSessionModel.class);

        factory.register(event -> {
            if (event instanceof UserModel.UserRemovedEvent) {
                UserModel.UserRemovedEvent userRemovedEvent = (UserModel.UserRemovedEvent) event;

                MapUserSessionProvider provider = MapUserSessionProviderFactory.this.create(userRemovedEvent.getKeycloakSession());
                provider.removeUserSessions(userRemovedEvent.getRealm(), userRemovedEvent.getUser());
            }
        });
    }

    @Override
    public void loadPersistentSessions(KeycloakSessionFactory sessionFactory, int maxErrors, int sessionsPerSegment) {

    }

    @Override
    public MapUserSessionProvider create(KeycloakSession session) {
        return new MapUserSessionProvider(session, userSessionStore, clientSessionStore);
    }
}
