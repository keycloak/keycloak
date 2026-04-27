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
package org.keycloak.models;

import java.util.stream.Stream;

import org.keycloak.provider.Provider;
import org.keycloak.storage.role.RoleLookupProvider;

/**
 * Provider of the role records.
 * @author vramik
 */
public interface RoleProvider extends Provider, RoleLookupProvider {

    /**
     * Adds a realm role with given {@code name} to the given realm.
     * The internal ID of the role will be created automatically.
     * @param realm Realm owning this role.
     * @param name String name of the role.
     * @return Model of the created role.
     */
    default RoleModel addRealmRole(RealmModel realm, String name) {
        return addRealmRole(realm, null, name);
    }

    /**
     * Adds a realm role with given internal ID and {@code name} to the given realm.
     * @param realm Realm owning this role.
     * @param id Internal ID of the role or {@code null} if one is to be created by the underlying store
     * @param name String name of the role.
     * @return Model of the created client.
     * @throws IllegalArgumentException If {@code id} does not conform
     *   the format understood by the underlying store.
     */
    RoleModel addRealmRole(RealmModel realm, String id, String name);

    /**
     * Returns all the realm roles of the given realm as a stream.
     * Effectively the same as the call {@code getRealmRolesStream(realm, null, null)}.
     * @param realm Realm.
     * @return Stream of the roles. Never returns {@code null}.
     */
    default Stream<RoleModel> getRealmRolesStream(RealmModel realm) {
        return getRealmRolesStream(realm, null, null);
    }

    /**
     * Returns the realm roles of the given realm as a stream.
     * @param realm Realm.
     * @param first First result to return. Ignored if negative or {@code null}.
     * @param max Maximum number of results to return. Ignored if negative or {@code null}.
     * @return Stream of the roles. Never returns {@code null}.
     */
    Stream<RoleModel> getRealmRolesStream(RealmModel realm, Integer first, Integer max);

    /**
     * Returns a paginated stream of roles with given ids and given search value in role names.
     *
     * @param realm Realm. Cannot be {@code null}.
     * @param ids Stream of ids. Returns empty {@code Stream} when {@code null}.
     * @param search Case-insensitive string to search by role's name or description. Ignored if {@code null}.
     * @param first Index of the first result to return. Ignored if negative or {@code null}.
     * @param max Maximum number of results to return. Ignored if negative or {@code null}.
     * @return Stream of desired roles. Never returns {@code null}.
     */
    Stream<RoleModel> getRolesStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max);

    /**
     * Removes given realm role from the given realm.
     * @param role Role to be removed.
     * @return {@code true} if the role existed and has been removed, {@code false} otherwise.
     */
    boolean removeRole(RoleModel role);

    /**
     * Removes all roles from the given realm.
     * @param realm Realm.
     */
    void removeRoles(RealmModel realm);

    /**
     * Adds a client role with given {@code name} to the given client.
     * The internal ID of the role will be created automatically.
     * @param client Client owning this role.
     * @param name String name of the role.
     * @return Model of the created role.
     */
    default RoleModel addClientRole(ClientModel client, String name) {
        return addClientRole(client, null, name);
    }

    /**
     * Adds a client role with given internal ID and {@code name} to the given client.
     * @param client Client owning this role.
     * @param id Internal ID of the client role or {@code null} if one is to be created by the underlying store.
     * @param name String name of the role.
     * @return Model of the created role.
     */
    RoleModel addClientRole(ClientModel client, String id, String name);

    /**
     * Returns all the client roles of the given client.
     * Effectively the same as the call {@code getClientRoles(client, null, null)}.
     * @param client Client.
     * @return Stream of the roles. Never returns {@code null}.
     */
    default Stream<RoleModel> getClientRolesStream(ClientModel client) {
        return getClientRolesStream(client, null, null);
    }

    /**
     * Returns the client roles of the given client.
     * @param client Client.
     * @param first First result to return. Ignored if negative or {@code null}.
     * @param max Maximum number of results to return. Ignored if negative or {@code null}.
     * @return Stream of the roles. Never returns {@code null}.
     */
    Stream<RoleModel> getClientRolesStream(ClientModel client, Integer first, Integer max);

    /**
     * Removes all roles from the given client.
     * @param client Client.
     */
    void removeRoles(ClientModel client);
}
