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

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RealmProvider extends Provider {

    // Note: The reason there are so many query methods here is for layering a cache on top of an persistent KeycloakSession
    MigrationModel getMigrationModel();
    RealmModel createRealm(String name);
    RealmModel createRealm(String id, String name);
    RealmModel getRealm(String id);
    RealmModel getRealmByName(String name);

    void moveGroup(RealmModel realm, GroupModel group, GroupModel toParent);

    List<GroupModel> getGroups(RealmModel realm);

    List<GroupModel> getTopLevelGroups(RealmModel realm);

    boolean removeGroup(RealmModel realm, GroupModel group);

    GroupModel createGroup(RealmModel realm, String name);

    GroupModel createGroup(RealmModel realm, String id, String name);

    void addTopLevelGroup(RealmModel realm, GroupModel subGroup);

    ClientModel addClient(RealmModel realm, String clientId);

    ClientModel addClient(RealmModel realm, String id, String clientId);

    List<ClientModel> getClients(RealmModel realm);

    ClientModel getClientById(String id, RealmModel realm);
    ClientModel getClientByClientId(String clientId, RealmModel realm);


    RoleModel addRealmRole(RealmModel realm, String name);

    RoleModel addRealmRole(RealmModel realm, String id, String name);

    RoleModel getRealmRole(RealmModel realm, String name);

    RoleModel addClientRole(RealmModel realm, ClientModel client, String name);

    RoleModel addClientRole(RealmModel realm, ClientModel client, String id, String name);

    Set<RoleModel> getRealmRoles(RealmModel realm);

    RoleModel getClientRole(RealmModel realm, ClientModel client, String name);

    Set<RoleModel> getClientRoles(RealmModel realm, ClientModel client);

    boolean removeRole(RealmModel realm, RoleModel role);

    RoleModel getRoleById(String id, RealmModel realm);

    boolean removeClient(String id, RealmModel realm);

    ClientTemplateModel getClientTemplateById(String id, RealmModel realm);
    GroupModel getGroupById(String id, RealmModel realm);



    List<RealmModel> getRealms();
    boolean removeRealm(String id);
    void close();
}
