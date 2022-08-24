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

package org.keycloak.storage.datastore;

import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientScopeProvider;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.UserProvider;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.UserCache;
import org.keycloak.storage.ClientScopeStorageManager;
import org.keycloak.storage.ClientStorageManager;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.ExportImportManager;
import org.keycloak.storage.GroupStorageManager;
import org.keycloak.storage.LegacyStoreManagers;
import org.keycloak.storage.MigrationManager;
import org.keycloak.storage.RoleStorageManager;
import org.keycloak.storage.UserStorageManager;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

public class LegacyDatastoreProvider implements DatastoreProvider, LegacyStoreManagers {
    private final LegacyDatastoreProviderFactory factory;
    private final KeycloakSession session;

    private ClientProvider clientProvider;
    private ClientScopeProvider clientScopeProvider;
    private GroupProvider groupProvider;
    private RealmProvider realmProvider;
    private RoleProvider roleProvider;
    private UserProvider userProvider;

    private ClientScopeStorageManager clientScopeStorageManager;
    private RoleStorageManager roleStorageManager;
    private GroupStorageManager groupStorageManager;
    private ClientStorageManager clientStorageManager;
    private UserProvider userStorageManager;
    private UserFederatedStorageProvider userFederatedStorageProvider;

    public LegacyDatastoreProvider(LegacyDatastoreProviderFactory factory, KeycloakSession session) {
        this.factory = factory;
        this.session = session;
    }

    @Override
    public void close() {
    }

    public ClientProvider clientStorageManager() {
        if (clientStorageManager == null) {
            clientStorageManager = new ClientStorageManager(session, factory.getClientStorageProviderTimeout());
        }
        return clientStorageManager;
    }

    public ClientScopeProvider clientScopeStorageManager() {
        if (clientScopeStorageManager == null) {
            clientScopeStorageManager = new ClientScopeStorageManager(session);
        }
        return clientScopeStorageManager;
    }

    public RoleProvider roleStorageManager() {
        if (roleStorageManager == null) {
            roleStorageManager = new RoleStorageManager(session, factory.getRoleStorageProviderTimeout());
        }
        return roleStorageManager;
    }

    public GroupProvider groupStorageManager() {
        if (groupStorageManager == null) {
            groupStorageManager = new GroupStorageManager(session);
        }
        return groupStorageManager;
    }

    public UserProvider userStorageManager() {
        if (userStorageManager == null) {
            userStorageManager = new UserStorageManager(session);
        }
        return userStorageManager;
    }

    @Override
    public UserProvider userLocalStorage() {
        return session.getProvider(UserProvider.class);
    }

    @Override
    public UserFederatedStorageProvider userFederatedStorage() {
        if (userFederatedStorageProvider == null) {
            userFederatedStorageProvider = session.getProvider(UserFederatedStorageProvider.class);
        }
        return userFederatedStorageProvider;
    }

    private ClientProvider getClientProvider() {
        // TODO: Extract ClientProvider from CacheRealmProvider and use that instead
        ClientProvider cache = session.getProvider(CacheRealmProvider.class);
        if (cache != null) {
            return cache;
        } else {
            return clientStorageManager();
        }
    }

    private ClientScopeProvider getClientScopeProvider() {
        // TODO: Extract ClientScopeProvider from CacheRealmProvider and use that instead
        ClientScopeProvider cache = session.getProvider(CacheRealmProvider.class);
        if (cache != null) {
            return cache;
        } else {
            return clientScopeStorageManager();
        }
    }

    private GroupProvider getGroupProvider() {
        // TODO: Extract GroupProvider from CacheRealmProvider and use that instead
        GroupProvider cache = session.getProvider(CacheRealmProvider.class);
        if (cache != null) {
            return cache;
        } else {
            return groupStorageManager();
        }
    }

    private RealmProvider getRealmProvider() {
        CacheRealmProvider cache = session.getProvider(CacheRealmProvider.class);
        if (cache != null) {
            return cache;
        } else {
            return session.getProvider(RealmProvider.class);
        }
    }

    private RoleProvider getRoleProvider() {
        // TODO: Extract RoleProvider from CacheRealmProvider and use that instead
        RoleProvider cache = session.getProvider(CacheRealmProvider.class);
        if (cache != null) {
            return cache;
        } else {
            return roleStorageManager();
        }
    }

    private UserProvider getUserProvider() {
        UserCache cache = session.getProvider(UserCache.class);
        if (cache != null) {
            return cache;
        } else {
            return userStorageManager();
        }
    }

    @Override
    public ClientProvider clients() {
        if (clientProvider == null) {
            clientProvider = getClientProvider();
        }
        return clientProvider;
    }

    @Override
    public ClientScopeProvider clientScopes() {
        if (clientScopeProvider == null) {
            clientScopeProvider = getClientScopeProvider();
        }
        return clientScopeProvider;
    }

    @Override
    public GroupProvider groups() {
        if (groupProvider == null) {
            groupProvider = getGroupProvider();
        }
        return groupProvider;
    }

    @Override
    public RealmProvider realms() {
        if (realmProvider == null) {
            realmProvider = getRealmProvider();
        }
        return realmProvider;
    }

    @Override
    public RoleProvider roles() {
        if (roleProvider == null) {
            roleProvider = getRoleProvider();
        }
        return roleProvider;
    }

    @Override
    public UserProvider users() {
        if (userProvider == null) {
            userProvider = getUserProvider();
        }
        return userProvider;
    }

    @Override
    public ExportImportManager getExportImportManager() {
        return new LegacyExportImportManager(session);
    }

    public MigrationManager getMigrationManager() {
        return new LegacyMigrationManager(session);
    }

}
