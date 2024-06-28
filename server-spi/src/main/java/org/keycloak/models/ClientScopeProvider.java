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
package org.keycloak.models;

import java.util.stream.Stream;
import org.keycloak.provider.Provider;
import org.keycloak.storage.clientscope.ClientScopeLookupProvider;

/**
 * Provider of the client scopes records.
 */
public interface ClientScopeProvider extends Provider, ClientScopeLookupProvider {

    /**
     * Returns all the client scopes of the given realm as a stream.
     * @param realm Realm.
     * @return Stream of the client scopes. Never returns {@code null}.
     */
    Stream<ClientScopeModel> getClientScopesStream(RealmModel realm);

    /**
     * Creates new client scope with given {@code name} to the given realm.
     * Spaces in {@code name} will be replaced by underscore so that scope name 
     * can be used as value of scope parameter. The internal ID will be created automatically.
     * @param realm Realm owning this client scope.
     * @param name String name of the client scope.
     * @return Model of the created client scope.
     * @throws ModelDuplicateException if client scope with given name already exists
     */
    default ClientScopeModel addClientScope(RealmModel realm, String name) {
        return ClientScopeProvider.this.addClientScope(realm, null, name);
    }

    /**
     * Creates new client scope with given internal ID and {@code name} to the given realm.
     * Spaces in {@code name} will be replaced by underscore so that scope name 
     * can be used as value of scope parameter.
     * @param realm Realm owning this client scope.
     * @param id Internal ID of the client scope or {@code null} if one is to be created by the underlying store
     * @param name String name of the client scope.
     * @return Model of the created client scope.
     * @throws IllegalArgumentException If {@code id} does not conform
     *   the format understood by the underlying store.
     * @throws ModelDuplicateException if client scope with given name already exists
     */
    ClientScopeModel addClientScope(RealmModel realm, String id, String name);

    /**
     * Removes client scope from the given realm.
     * @param realm Realm.
     * @param id Internal ID of the client scope
     * @return {@code true} if the client scope existed and has been removed, {@code false} otherwise.
     * @throws ModelException if client scope is in use.
     */
    boolean removeClientScope(RealmModel realm, String id);

    /**
     * Removes all client scopes from the given realm.
     * @param realm Realm.
     */
    void removeClientScopes(RealmModel realm);
}
