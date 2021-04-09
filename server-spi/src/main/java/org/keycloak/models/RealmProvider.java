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
import org.keycloak.provider.Provider;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RealmProvider extends Provider /* TODO: Remove in future version */, ClientProvider, ClientScopeProvider, GroupProvider, RoleProvider /* up to here */ {

    /**
     * Creates new realm with the given name. The internal ID will be generated automatically.
     * @param name String name of the realm
     * @return Model of the created realm.
     */
    RealmModel createRealm(String name);

    /**
     * Created new realm with given ID and name.
     * @param id Internal ID of the realm or {@code null} if one is to be created by the underlying store
     * @param name String name of the realm
     * @return Model of the created realm.
     */
    RealmModel createRealm(String id, String name);

    /**
     * Exact search for a realm by its internal ID.
     * @param id Internal ID of the realm.
     * @return Model of the realm
     */
    RealmModel getRealm(String id);

    /**
     * Exact search for a realm by its name.
     * @param name String name of the realm
     * @return Model of the realm
     */
    RealmModel getRealmByName(String name);

    /**
     * Returns realms as a stream.
     * @return Stream of {@link RealmModel}. Never returns {@code null}.
     */
    Stream<RealmModel> getRealmsStream();

    /**
     * Returns stream of realms which has component with the given provider type.
     * @param type {@code Class<?>} Type of the provider.
     * @return Stream of {@link RealmModel}. Never returns {@code null}.
     */
    Stream<RealmModel> getRealmsWithProviderTypeStream(Class<?> type);

    /**
     * Removes realm with the given id.
     * @param id of realm.
     * @return {@code true} if the realm was successfully removed.
     */
    boolean removeRealm(String id);

    default ClientInitialAccessModel createClientInitialAccessModel(RealmModel realm, int expiration, int count) {
        return realm.createClientInitialAccessModel(expiration, count);
    }
    default ClientInitialAccessModel getClientInitialAccessModel(RealmModel realm, String id) {
        return realm.getClientInitialAccessModel(id);
    }
    default void removeClientInitialAccessModel(RealmModel realm, String id) {
        realm.removeClientInitialAccessModel(id);
    }

    /**
     * Returns client's initial access as a stream.
     * @param realm {@link RealmModel} The realm where to list client's initial access.
     * @return Stream of {@link ClientInitialAccessModel}. Never returns {@code null}.
     */
    default Stream<ClientInitialAccessModel> listClientInitialAccessStream(RealmModel realm) {
        return realm.getClientInitialAccesses();
    }

    /**
     * Removes all expired client initial accesses from all realms.
     */
    void removeExpiredClientInitialAccess();
    
    default void decreaseRemainingCount(RealmModel realm, ClientInitialAccessModel clientInitialAccess) { // Separate provider method to ensure we decrease remainingCount atomically instead of doing classic update
        realm.decreaseRemainingCount(clientInitialAccess);
    }

    void saveLocalizationText(RealmModel realm, String locale, String key, String text);

    void saveLocalizationTexts(RealmModel realm, String locale, Map<String, String> localizationTexts);

    boolean updateLocalizationText(RealmModel realm, String locale, String key, String text);

    boolean deleteLocalizationTextsByLocale(RealmModel realm, String locale);

    boolean deleteLocalizationText(RealmModel realm, String locale, String key);

    String getLocalizationTextsById(RealmModel realm, String locale, String key);

    // The methods below are going to be removed in future version of Keycloak
    // Sadly, we have to copy-paste the declarations from the respective interfaces
    // including the "default" body to be able to add a note on deprecation

    /**
     * @deprecated Use {@link #getRealmsStream() getRealmsStream} instead.
     */
    @Deprecated
    default List<RealmModel> getRealms() {
        return getRealmsStream().collect(Collectors.toList());
    }

    /**
     * @deprecated Use {@link #getRealmsWithProviderTypeStream(Class) getRealmsWithProviderTypeStream} instead.
     */
    @Deprecated
    default List<RealmModel> getRealmsWithProviderType(Class<?> type) {
        return getRealmsWithProviderTypeStream(type).collect(Collectors.toList());
    }

    /**
     * @deprecated Use {@link #listClientInitialAccessStream(RealmModel) listClientInitialAccessStream} instead.
     */
    @Deprecated
    default List<ClientInitialAccessModel> listClientInitialAccess(RealmModel realm) {
        return listClientInitialAccessStream(realm).collect(Collectors.toList());
    }

    /**
     * @deprecated Use the corresponding method from {@link ClientProvider}. */
    @Override
    public ClientModel addClient(RealmModel realm, String id, String clientId);

    /**
     * @deprecated Use the corresponding method from {@link ClientProvider}. */
    @Override
    default ClientModel addClient(RealmModel realm, String clientId) {
        return addClient(realm, null, clientId);
    }

    /**
     * @deprecated Use the corresponding method from {@link ClientProvider}. */
    @Override
    default List<ClientModel> getClients(RealmModel realm) {
        return this.getClients(realm, null, null);
    }

    /**
     * @deprecated Use the corresponding method from {@link ClientProvider}. */
    @Override
    default List<ClientModel> getClients(RealmModel realm, Integer firstResult, Integer maxResults) {
        return getClientsStream(realm, firstResult, maxResults).collect(Collectors.toList());
    }

    /**
     * @deprecated Use the corresponding method from {@link ClientProvider}. */
    @Override
    default List<ClientModel> searchClientsByClientId(String clientId, Integer firstResult, Integer maxResults, RealmModel realm) {
        return searchClientsByClientIdStream(realm, clientId, firstResult, maxResults).collect(Collectors.toList());
    }

    /**
     * @deprecated Use the corresponding method from {@link ClientProvider}. */
    @Override
    default ClientModel getClientByClientId(String clientId, RealmModel realm) { return getClientByClientId(realm, clientId); }

    /**
     * @deprecated Use the corresponding method from {@link ClientProvider}. */
    @Override
    default ClientModel getClientById(String id, RealmModel realm) { return getClientById(realm, id); }

    /**
     * @deprecated Use the corresponding method from {@link ClientProvider}. */
    @Override
    default boolean removeClient(String id, RealmModel realm) { return this.removeClient(realm, id); }

    /**
     * @deprecated Use the corresponding method from {@link ClientProvider}. */
    @Override
    default  List<ClientModel> getAlwaysDisplayInConsoleClients(RealmModel realm) {
        return getAlwaysDisplayInConsoleClientsStream(realm).collect(Collectors.toList());
    }

    /**
     * @deprecated Use the corresponding method from {@link ClientProvider}. */
    @Override
    long getClientsCount(RealmModel realm);

    /**
     * @deprecated Use the corresponding method from {@link ClientScopeProvider}. */
    default ClientScopeModel getClientScopeById(String id, RealmModel realm) {
        return getClientScopeById(realm, id);
    }

    /**
     * @deprecated Use the corresponding method from {@link ClientScopeProvider}. */
    @Override
    ClientScopeModel getClientScopeById(RealmModel realm, String id);

    //Role-related methods
    /**
     * @deprecated Use the corresponding method from {@link RoleProvider}. */
    @Override
    default RoleModel addRealmRole(RealmModel realm, String name) { return addRealmRole(realm, null, name); }

    /**
     * @deprecated Use the corresponding method from {@link RoleProvider}. */
    @Override
    RoleModel addRealmRole(RealmModel realm, String id, String name);

    /**
     * @deprecated Use the corresponding method from {@link RoleProvider}. */
    @Override
    RoleModel getRealmRole(RealmModel realm, String name);

    /**
     * @deprecated Use the corresponding method from {@link RoleProvider}. */
    default RoleModel getRoleById(String id, RealmModel realm) {
        return getRoleById(realm, id);
    }

    /**
     * @deprecated Use the corresponding method from {@link RoleProvider}. */
    @Override
    default Set<RoleModel> getRealmRoles(RealmModel realm) {
        return getRealmRoles(realm, null, null);
    }

    /**
     * @deprecated Use the corresponding method from {@link RoleProvider}. */
    default Set<RoleModel> getRealmRoles(RealmModel realm, Integer first, Integer max) {
        return getRealmRolesStream(realm, first, max).collect(Collectors.toSet());
    }

    /**
     * @deprecated Use the corresponding method from {@link RoleProvider}. */
    default Set<RoleModel> searchForRoles(RealmModel realm, String search, Integer first, Integer max) {
        return searchForRolesStream(realm, search, first, max).collect(Collectors.toSet());
    }

    /**
     * @deprecated Use the corresponding method from {@link RoleProvider}. */
    default boolean removeRole(RealmModel realm, RoleModel role) {
        return removeRole(role);
    }

    /**
     * @deprecated Use the corresponding method from {@link RoleProvider}. */
    default RoleModel addClientRole(RealmModel realm, ClientModel client, String name) {
        return addClientRole(client, name);
    }

    /**
     * @deprecated Use the corresponding method from {@link RoleProvider}. */
    default RoleModel addClientRole(RealmModel realm, ClientModel client, String id, String name) {
        return addClientRole(client, id, name);
    }

    /**
     * @deprecated Use the corresponding method from {@link RoleProvider}. */
    default RoleModel getClientRole(RealmModel realm, ClientModel client, String name) {
        return getClientRole(client, name);
    }

    /**
     * @deprecated Use the corresponding method from {@link RoleProvider}. */
    default Set<RoleModel> getClientRoles(RealmModel realm, ClientModel client) {
        return getClientRolesStream(client).collect(Collectors.toSet());
    }

    /**
     * @deprecated Use the corresponding method from {@link RoleProvider}. */
    default Set<RoleModel> getClientRoles(RealmModel realm, ClientModel client, Integer first, Integer max) {
        return getClientRolesStream(client, first, max).collect(Collectors.toSet());
    }

    /**
     * @deprecated Use the corresponding method from {@link RoleProvider}. */
    default Set<RoleModel> searchForClientRoles(RealmModel realm, ClientModel client, String search, Integer first, Integer max) {
        return searchForClientRolesStream(client, search, first, max).collect(Collectors.toSet());
    }

    /* GROUP PROVIDER METHODS */

    /**
     * @deprecated Use the corresponding method from {@link GroupProvider}. */
    @Override
    void moveGroup(RealmModel realm, GroupModel group, GroupModel toParent);

    /**
     * @deprecated Use the corresponding method from {@link GroupProvider}. */
    @Override
    GroupModel getGroupById(RealmModel realm, String id);

    /**
     * @deprecated Use the corresponding method from {@link GroupProvider}. */
    @Override
    default GroupModel getGroupById(String id, RealmModel realm) {
        return getGroupById(realm, id);
    }

    /**
     * @deprecated Use the corresponding method from {@link GroupProvider}. */
    @Override
    default List<GroupModel> getGroups(RealmModel realm) {
        return getGroupsStream(realm).collect(Collectors.toList());
    }

    /**
     * @deprecated Use the corresponding method from {@link GroupProvider}. */
    @Override
    Long getGroupsCount(RealmModel realm, Boolean onlyTopGroups);

    /**
     * @deprecated Use the corresponding method from {@link GroupProvider}. */
    @Override
    Long getGroupsCountByNameContaining(RealmModel realm, String search);

    /**
     * @deprecated Use the corresponding method from {@link GroupProvider}. */
    @Override
    default List<GroupModel> getGroupsByRole(RealmModel realm, RoleModel role, int firstResult, int maxResults) {
        return getGroupsByRoleStream(realm, role, firstResult, maxResults).collect(Collectors.toList());
    }

    /**
     * @deprecated Use the corresponding method from {@link GroupProvider}. */
    @Override
    default List<GroupModel> getTopLevelGroups(RealmModel realm) {
        return getTopLevelGroupsStream(realm).collect(Collectors.toList());
    }

    /**
     * @deprecated Use the corresponding method from {@link GroupProvider}. */
    @Override
    default List<GroupModel> getTopLevelGroups(RealmModel realm, Integer first, Integer max) {
        return getTopLevelGroupsStream(realm, first, max).collect(Collectors.toList());
    }

    /**
     * @deprecated Use the corresponding method from {@link GroupProvider}. */
    @Override
    default List<GroupModel> searchForGroupByName(RealmModel realm, String search, Integer first, Integer max) {
        return searchForGroupByNameStream(realm, search, first, max).collect(Collectors.toList());
    }

    /**
     * @deprecated Use the corresponding method from {@link GroupProvider}. */
    @Override
    boolean removeGroup(RealmModel realm, GroupModel group);

    /**
     * @deprecated Use the corresponding method from {@link GroupProvider}. */
    @Override
    default GroupModel createGroup(RealmModel realm, String name) {
        return createGroup(realm, null, name, null);
    }

    /**
     * @deprecated Use the corresponding method from {@link GroupProvider}. */
    @Override
    default GroupModel createGroup(RealmModel realm, String id, String name) {
        return createGroup(realm, id, name, null);
    }

    /**
     * @deprecated Use the corresponding method from {@link GroupProvider}. */
    @Override
    default GroupModel createGroup(RealmModel realm, String name, GroupModel toParent) {
        return createGroup(realm, null, name, toParent);
    }

    /**
     * @deprecated Use the corresponding method from {@link GroupProvider}. */
    @Override
    GroupModel createGroup(RealmModel realm, String id, String name, GroupModel toParent);

    /**
     * @deprecated Use the corresponding method from {@link GroupProvider}. */
    @Override
    void addTopLevelGroup(RealmModel realm, GroupModel subGroup);
}
