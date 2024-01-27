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
package org.keycloak.storage;

import java.util.stream.Stream;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientScopeProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.clientscope.ClientScopeLookupProvider;
import org.keycloak.storage.clientscope.ClientScopeStorageProvider;
import org.keycloak.storage.clientscope.ClientScopeStorageProviderFactory;
import org.keycloak.storage.clientscope.ClientScopeStorageProviderModel;

public class ClientScopeStorageManager extends AbstractStorageManager<ClientScopeStorageProvider, ClientScopeStorageProviderModel> implements ClientScopeProvider {

    public ClientScopeStorageManager(KeycloakSession session) {
        super(session, ClientScopeStorageProviderFactory.class, ClientScopeStorageProvider.class,
                ClientScopeStorageProviderModel::new, "clientscope");
    }

    private ClientScopeProvider localStorage() {
        return session.getProvider(ClientScopeProvider.class);
    }

    /* CLIENT SCOPE PROVIDER LOOKUP METHODS - implemented by client scope storage providers */

    @Override
    public ClientScopeModel getClientScopeById(RealmModel realm, String id) {
        StorageId storageId = new StorageId(id);
        if (storageId.getProviderId() == null) {
            return localStorage().getClientScopeById(realm, id);
        }

        ClientScopeLookupProvider provider = getStorageProviderInstance(realm, storageId.getProviderId(), ClientScopeLookupProvider.class);
        if (provider == null) return null;

        return provider.getClientScopeById(realm, id);
    }

    /* CLIENT SCOPE PROVIDER METHODS - provided only by local storage (e.g. not supported by storage providers) */

    @Override
    public Stream<ClientScopeModel> getClientScopesStream(RealmModel realm) {
        return localStorage().getClientScopesStream(realm);
    }

    @Override
    public ClientScopeModel addClientScope(RealmModel realm, String id, String name) {
        return localStorage().addClientScope(realm, id, name);
    }

    @Override
    public boolean removeClientScope(RealmModel realm, String id) {
        return localStorage().removeClientScope(realm, id);
    }

    @Override
    public void removeClientScopes(RealmModel realm) {
        localStorage().removeClientScopes(realm);
    }

    @Override
    public void close() {
    }
}
