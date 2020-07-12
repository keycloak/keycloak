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

import org.keycloak.migration.MigrationModel;
import org.keycloak.provider.Provider;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RealmProvider extends Provider /* TODO: Remove in future version */, ClientProvider, RoleProvider /* up to here */ {

    // Note: The reason there are so many query methods here is for layering a cache on top of an persistent KeycloakSession
    MigrationModel getMigrationModel();
    RealmModel createRealm(String name);
    RealmModel createRealm(String id, String name);
    RealmModel getRealm(String id);
    RealmModel getRealmByName(String name);

    void moveGroup(RealmModel realm, GroupModel group, GroupModel toParent);

    List<GroupModel> getGroups(RealmModel realm);

    Long getGroupsCount(RealmModel realm, Boolean onlyTopGroups);

    Long getGroupsCountByNameContaining(RealmModel realm, String search);
    
    List<GroupModel> getGroupsByRole(RealmModel realm, RoleModel role, int firstResult, int maxResults);

    List<GroupModel> getTopLevelGroups(RealmModel realm);

    List<GroupModel> getTopLevelGroups(RealmModel realm, Integer first, Integer max);

    List searchForGroupByName(RealmModel realm, String search, Integer first, Integer max);

    boolean removeGroup(RealmModel realm, GroupModel group);

    default GroupModel createGroup(RealmModel realm, String name) {
        return createGroup(realm, null, name, null);
    }

    default GroupModel createGroup(RealmModel realm, String id, String name) {
        return createGroup(realm, id, name, null);
    }

    default GroupModel createGroup(RealmModel realm, String name, GroupModel toParent) {
        return createGroup(realm, null, name, toParent);
    }

    GroupModel createGroup(RealmModel realm, String id, String name, GroupModel toParent);

    void addTopLevelGroup(RealmModel realm, GroupModel subGroup);

    ClientScopeModel getClientScopeById(String id, RealmModel realm);
    GroupModel getGroupById(String id, RealmModel realm);

    List<RealmModel> getRealms();
    List<RealmModel> getRealmsWithProviderType(Class<?> type);
    boolean removeRealm(String id);

    ClientInitialAccessModel createClientInitialAccessModel(RealmModel realm, int expiration, int count);
    ClientInitialAccessModel getClientInitialAccessModel(RealmModel realm, String id);
    void removeClientInitialAccessModel(RealmModel realm, String id);
    List<ClientInitialAccessModel> listClientInitialAccess(RealmModel realm);
    void removeExpiredClientInitialAccess();
    void decreaseRemainingCount(RealmModel realm, ClientInitialAccessModel clientInitialAccess); // Separate provider method to ensure we decrease remainingCount atomically instead of doing classic update

    // The methods below are going to be removed in future version of Keycloak
    // Sadly, we have to copy-paste the declarations from the respective interfaces
    // including the "default" body to be able to add a note on deprecation

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

}
