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
package org.keycloak.models.map.authSession;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.map.common.AbstractMapProviderFactory;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.sessions.AuthenticationSessionProviderFactory;

import org.keycloak.sessions.RootAuthenticationSessionModel;
import java.util.UUID;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapRootAuthenticationSessionProviderFactory extends AbstractMapProviderFactory<AuthenticationSessionProvider>
        implements AuthenticationSessionProviderFactory {

    private MapStorage<UUID, MapRootAuthenticationSessionEntity, RootAuthenticationSessionModel> store;

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        MapStorageProvider sp = (MapStorageProvider) factory.getProviderFactory(MapStorageProvider.class);
        this.store = sp.getStorage("sessions", UUID.class, MapRootAuthenticationSessionEntity.class, RootAuthenticationSessionModel.class);
    }

    @Override
    public AuthenticationSessionProvider create(KeycloakSession session) {
        return new MapRootAuthenticationSessionProvider(session, store);
    }
}
