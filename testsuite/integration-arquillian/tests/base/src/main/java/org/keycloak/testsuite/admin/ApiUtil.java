/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.admin;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * Created by st on 28.05.15.
 */
public class ApiUtil {

    public static String getCreatedId(Response response) {
        URI location = response.getLocation();
        if (location == null) {
            return null;
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public static ClientResource findClientResourceByClientId(RealmResource realm, String clientId) {
        for (ClientRepresentation c : realm.clients().findAll()) {
            if (c.getClientId().equals(clientId)) {
                return realm.clients().get(c.getId());
            }
        }
        return null;
    }

    public static ClientRepresentation findClientByClientId(RealmResource realm, String clientId) {
        ClientRepresentation client = null;
        for (ClientRepresentation c : realm.clients().findAll()) {
            if (clientId.equals(c.getClientId())) {
                client = c;
            }
        }
        return client;
    }

    public static UserRepresentation findUserByUsername(RealmResource realm, String username) {
        UserRepresentation user = null;
        List<UserRepresentation> ur = realm.users().search(username, null, null);
        if (ur.size() == 1) {
            user = ur.get(0);
        }
        return user;
    }

    public static String createUserWithAdminClient(RealmResource realm, UserRepresentation user) {
        System.out.println("Creating user '" + user.getUsername() + "'");
        Response response = realm.users().create(user);
        String createdId = getCreatedId(response);
        response.close();
        System.out.println(" created user id: " + createdId);
        return createdId;
    }

    public static void resetUserPassword(UserResource userResource, String newPassword, boolean temporary) {
        CredentialRepresentation newCredential = new CredentialRepresentation();
        newCredential.setType(PASSWORD);
        newCredential.setValue(newPassword);
        newCredential.setTemporary(temporary);
        userResource.resetPassword(newCredential);
    }

    public static void assignClientRoles(UserResource userResource, String clientId, String... roles) {
        RoleScopeResource rsr = userResource.roles().clientLevel(clientId);
        List<String> rolesList = Arrays.asList(roles);
        List<RoleRepresentation> realmMgmtRoles = new ArrayList<>();
        for (RoleRepresentation rr : rsr.listAvailable()) {
            if (rolesList.contains(rr.getName())) {
                realmMgmtRoles.add(rr);
            }
        }
        rsr.add(realmMgmtRoles);
    }

}
