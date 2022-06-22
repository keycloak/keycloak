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

package org.keycloak.models.map.datastore;

import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientScopeProvider;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.UserProvider;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.ExportImportManager;

public class MapDatastoreProvider implements DatastoreProvider {

    private final KeycloakSession session;

    public MapDatastoreProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {
    }

    @Override
    public ClientScopeProvider clientScopes() {
        return session.getProvider(ClientScopeProvider.class);
    }

    @Override
    public ClientProvider clients() {
        return session.getProvider(ClientProvider.class);
    }

    @Override
    public GroupProvider groups() {
        return session.getProvider(GroupProvider.class);
    }

    @Override
    public RealmProvider realms() {
        return session.getProvider(RealmProvider.class);
    }

    @Override
    public RoleProvider roles() {
        return session.getProvider(RoleProvider.class);
    }

    @Override
    public UserProvider users() {
        return session.getProvider(UserProvider.class);
    }

    @Override
    public ExportImportManager getExportImportManager() {
        return new MapExportImportManager(session);
    }

}
