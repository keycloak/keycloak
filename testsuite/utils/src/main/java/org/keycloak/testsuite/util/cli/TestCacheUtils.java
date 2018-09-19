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

package org.keycloak.testsuite.util.cli;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TestCacheUtils {

    public static void cacheRealmWithEverything(KeycloakSession session, String realmName) {
        RealmModel realm  = session.realms().getRealmByName(realmName);

        for (ClientModel client : realm.getClients()) {
            realm.getClientById(client.getId());
            realm.getClientByClientId(client.getClientId());

            cacheRoles(session, realm, client);
        }

        cacheRoles(session, realm, realm);

        for (GroupModel group : realm.getTopLevelGroups()) {
            cacheGroupRecursive(realm, group);
        }

        for (ClientScopeModel clientScope : realm.getClientScopes()) {
            realm.getClientScopeById(clientScope.getId());
        }

        for (UserModel user : session.users().getUsers(realm)) {
            session.users().getUserById(user.getId(), realm);
            if (user.getEmail() != null) {
                session.users().getUserByEmail(user.getEmail(), realm);
            }
            session.users().getUserByUsername(user.getUsername(), realm);

            session.users().getConsents(realm, user.getId());

            for (FederatedIdentityModel fedIdentity : session.users().getFederatedIdentities(user, realm)) {
                session.users().getUserByFederatedIdentity(fedIdentity, realm);
            }
        }
    }

    private static void cacheRoles(KeycloakSession session, RealmModel realm, RoleContainerModel roleContainer) {
        for (RoleModel role : roleContainer.getRoles()) {
            realm.getRoleById(role.getId());
            roleContainer.getRole(role.getName());
            if (roleContainer instanceof RealmModel) {
                session.realms().getRealmRole(realm, role.getName());
            } else {
                session.realms().getClientRole(realm, (ClientModel) roleContainer, role.getName());
            }
        }
    }

    private static void cacheGroupRecursive(RealmModel realm, GroupModel group) {
        realm.getGroupById(group.getId());
        for (GroupModel sub : group.getSubGroups()) {
            cacheGroupRecursive(realm, sub);
        }
    }
}
