/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.provider.Provider;
import org.keycloak.storage.client.ClientLookupProvider;

/**
 * Provider of the client records.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientProvider extends ClientLookupProvider, Provider {

    /**
     * Returns the clients of the given realm as a stream.
     * @param realm Realm.
     * @param firstResult First result to return. Ignored if negative or {@code null}.
     * @param maxResults Maximum number of results to return. Ignored if negative or {@code null}.
     * @return Stream of the clients. Never returns {@code null}.
     */
    Stream<ClientModel> getClientsStream(RealmModel realm, Integer firstResult, Integer maxResults);

    /**
     * Returns all the clients of the given realm as a stream.
     * Effectively the same as the call {@code getClientsStream(realm, null, null)}.
     * @param realm Realm.
     * @return Stream of the clients. Never returns {@code null}.
     */
    default Stream<ClientModel> getClientsStream(RealmModel realm) {
        return this.getClientsStream(realm, null, null);
    }

    /**
     * Adds a client with given {@code clientId} to the given realm.
     * The internal ID of the client will be created automatically.
     * @param realm Realm owning this client.
     * @param clientId String that identifies the client to the external parties.
     *   Maps to {@code client_id} in OIDC or {@code entityID} in SAML.
     * @return Model of the created client.
     */
    default ClientModel addClient(RealmModel realm, String clientId) {
        return addClient(realm, null, clientId);
    }

    /**
     * Adds a client with given internal ID and {@code clientId} to the given realm.
     * @param realm Realm owning this client.
     * @param id Internal ID of the client or {@code null} if one is to be created by the underlying store
     * @param clientId String that identifies the client to the external parties.
     *   Maps to {@code client_id} in OIDC or {@code entityID} in SAML.
     * @return Model of the created client.
     * @throws IllegalArgumentException If {@code id} does not conform
     *   the format understood by the underlying store.
     */
    ClientModel addClient(RealmModel realm, String id, String clientId);

    /**
     * Returns number of clients in the given realm
     * @param realm Realm.
     * @return Number of the clients in the given realm.
     */
    long getClientsCount(RealmModel realm);

    /**
     * Returns a stream of clients that are expected to always show up in account console.
     * @param realm Realm owning the clients.
     * @return Stream of the clients. Never returns {@code null}.
     */
    Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream(RealmModel realm);

    /**
     * Removes given client from the given realm.
     * @param realm Realm.
     * @param id Internal ID of the client
     * @return {@code true} if the client existed and has been removed, {@code false} otherwise.
     */
    boolean removeClient(RealmModel realm, String id);

    /**
     * Removes all clients from the given realm.
     * @param realm Realm.
     */
    void removeClients(RealmModel realm);

    /**
     * Assign clientScopes to the client. Add as default scopes (if parameter 'defaultScope' is true)
     * or optional scopes (if parameter 'defaultScope' is false)
     *
     * @param realm Realm.
     * @param client Client.
     * @param clientScopes to be assigned
     * @param defaultScope if true the scopes are assigned as default, or optional in case of false
     */
    void addClientScopes(RealmModel realm, ClientModel client, Set<ClientScopeModel> clientScopes, boolean defaultScope);

    /**
     * Unassign clientScope from the client.
     *
     * @param realm Realm.
     * @param client Client.
     * @param clientScope to be unassigned
     */
    void removeClientScope(RealmModel realm, ClientModel client, ClientScopeModel clientScope);

    /**
     * Add specified client scope to all non bearer-only clients in the realm, which have same protocol as specified client scope.
     *
     * Method may be used just for new client scopes, which are not yet assigned to any clients as if specified clientScope is already assigned
     * to some client, there might be issues related to duplicate entries.
     *
     * @param realm Realm
     * @param clientScope client scope from the specified realm, which would be added to all clients
     * @param defaultClientScope If true, then it will be added as "default" client scope. If false, then it will be added as "optional" client scope
     */
    void addClientScopeToAllClients(RealmModel realm, ClientScopeModel clientScope, boolean defaultClientScope);

    /**
     * Returns a map of (rootUrl, {validRedirectUris}) for all enabled clients.
     * @param realm
     * @return
     * @deprecated Do not use, this is only to support a deprecated logout endpoint and will vanish with it's removal
     */
    @Deprecated
    Map<ClientModel, Set<String>> getAllRedirectUrisOfEnabledClients(RealmModel realm);

}
