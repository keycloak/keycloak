/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.testsuite.util;

import java.util.HashSet;
import java.util.Set;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class RealmRepUtil {

    // don't allow instance
    private RealmRepUtil() {
    }

    public static UserRepresentation findUser(RealmRepresentation testRealm, String userName) {
        for (UserRepresentation user : testRealm.getUsers()) {
            if (user.getUsername().equals(userName)) return user;
        }

        return null;
    }

    public static ClientRepresentation findClientByClientId(RealmRepresentation testRealm, String clientId) {
        for (ClientRepresentation client : testRealm.getClients()) {
            if (client.getClientId().equals(clientId)) return client;
        }

        return null;
    }

    public static ClientRepresentation findClientById(RealmRepresentation testRealm, String id) {
        for (ClientRepresentation client : testRealm.getClients()) {
            if (client.getId().equals(id)) return client;
        }
        return null;
    }

    public static RoleRepresentation findRealmRole(RealmRepresentation realm, String roleName) {
        if (realm.getRoles() == null) return null;
        if (realm.getRoles().getRealm() == null) return null;
        for (RoleRepresentation role : realm.getRoles().getRealm()) {
            if (role.getName().equals(roleName)) return role;
        }

        return null;
    }

    public static RoleRepresentation findClientRole(RealmRepresentation realm, String clientId, String roleName) {
        if (realm.getRoles() == null) return null;
        if (realm.getRoles().getClient() == null) return null;
        if (realm.getRoles().getClient().get(clientId) == null) return null;
        for (RoleRepresentation role : realm.getRoles().getClient().get(clientId)) {
            if (roleName.equals(role.getName())) return role;
        }

        return null;
    }

    public static String findDefaultRole(RealmRepresentation realm, String roleName) {
        if (realm.getDefaultRoles() == null) return null;
        for (String role : realm.getDefaultRoles()) {
            if (role.equals(roleName)) return role;
        }

        return null;
    }

    public static Set<RoleRepresentation> allRoles(RealmRepresentation realm, UserRepresentation user) {
        Set<RoleRepresentation> allRoles = new HashSet<>();
        for (String roleName : user.getRealmRoles()) {
            allRoles.add(findRealmRole(realm, roleName));
        }

        for (String clientId : user.getClientRoles().keySet()) {
            for (String roleName : user.getClientRoles().get(clientId)) {
                allRoles.add(findClientRole(realm, clientId, roleName));
            }
        }

        return allRoles;
    }
}
