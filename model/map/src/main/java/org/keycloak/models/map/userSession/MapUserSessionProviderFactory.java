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
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.map.client.MapClientProvider;
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
public class MapUserSessionProviderFactory extends AbstractMapProviderFactory<MapUserSessionProvider, MapUserSessionEntity, UserSessionModel> implements  UserSessionProviderFactory<MapUserSessionProvider>, InvalidationHandler {

    public MapUserSessionProviderFactory() {
        super(UserSessionModel.class, MapUserSessionProvider.class);
    }

    @Override
    public MapUserSessionProvider createNew(KeycloakSession session) {
        return new MapUserSessionProvider(session, getStorage(session));
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
    public void loadPersistentSessions(KeycloakSessionFactory sessionFactory, int maxErrors, int sessionsPerSegment) {

    }
}
