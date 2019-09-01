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
package org.keycloak.testsuite.admin;

import org.jboss.logging.Logger;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.crypto.Algorithm;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ApiUtil {

    private static final Logger log = Logger.getLogger(ApiUtil.class);

    public static String getCreatedId(Response response) {
        URI location = response.getLocation();
        if (!response.getStatusInfo().equals(Status.CREATED)) {
            StatusType statusInfo = response.getStatusInfo();
            response.bufferEntity();
            String body = response.readEntity(String.class);
            throw new WebApplicationException("Create method returned status "
                    + statusInfo.getReasonPhrase() + " (Code: " + statusInfo.getStatusCode() + "); expected status: Created (201). Response body: " + body, response);
        }
        if (location == null) {
            return null;
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public static ClientResource findClientResourceById(RealmResource realm, String id) {
        for (ClientRepresentation c : realm.clients().findAll()) {
            if (c.getId().equals(id)) {
                return realm.clients().get(c.getId());
            }
        }
        return null;
    }

    public static ClientResource findClientResourceByClientId(RealmResource realm, String clientId) {
        for (ClientRepresentation c : realm.clients().findAll()) {
            if (c.getClientId().equals(clientId)) {
                return realm.clients().get(c.getId());
            }
        }
        return null;
    }

    public static ClientResource findClientResourceByName(RealmResource realm, String name) {
        for (ClientRepresentation c : realm.clients().findAll()) {
            if (name.equals(c.getName())) {
                return realm.clients().get(c.getId());
            }
        }
        return null;
    }

    public static ClientResource findClientByClientId(RealmResource realm, String clientId) {
        for (ClientRepresentation c : realm.clients().findAll()) {
            if (clientId.equals(c.getClientId())) {
                return realm.clients().get(c.getId());
            }
        }
        return null;
    }

    public static RoleResource findClientRoleByName(ClientResource client, String role) {
        return client.roles().get(role);
    }

    public static ProtocolMapperRepresentation findProtocolMapperByName(ClientResource client, String name) {
        for (ProtocolMapperRepresentation p : client.getProtocolMappers().getMappers()) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    public static ClientScopeResource findClientScopeByName(RealmResource realm, String clientScopeName) {
        for (ClientScopeRepresentation clientScope : realm.clientScopes().findAll()) {
            if (clientScopeName.equals(clientScope.getName())) {
                return realm.clientScopes().get(clientScope.getId());
            }
        }
        return null;
    }

    public static RoleResource findRealmRoleByName(RealmResource realm, String role) {
        return realm.roles().get(role);
    }

    public static UserRepresentation findUserByUsername(RealmResource realm, String username) {
        UserRepresentation user = null;
        List<UserRepresentation> ur = realm.users().search(username, null, null, null, 0, Integer.MAX_VALUE);
        if (ur.size() == 1) {
            user = ur.get(0);
        }

        if (ur.size() > 1) { // try to be more specific
            for (UserRepresentation rep : ur) {
                if (rep.getUsername().equalsIgnoreCase(username)) {
                    return rep;
                }
            }
        }

        return user;
    }

    public static UserResource findUserByUsernameId(RealmResource realm, String username) {
        return realm.users().get(findUserByUsername(realm, username).getId());
    }

    /**
     * Creates a user
     * @param realm
     * @param user
     * @param password
     * @return ID of the new user
     */
    public static String createUserWithAdminClient(RealmResource realm, UserRepresentation user) {
        Response response = realm.users().create(user);
        String createdId = getCreatedId(response);
        response.close();
        return createdId;
    }

    /**
     * Creates a user and sets the password
     * @param realm
     * @param user
     * @param password
     * @return ID of the new user
     */
    public static String createUserAndResetPasswordWithAdminClient(RealmResource realm, UserRepresentation user, String password) {
        String id = createUserWithAdminClient(realm, user);
        resetUserPassword(realm.users().get(id), password, false);
        return id;
    }

    public static void resetUserPassword(UserResource userResource, String newPassword, boolean temporary) {
        CredentialRepresentation newCredential = new CredentialRepresentation();
        newCredential.setType(PASSWORD);
        newCredential.setValue(newPassword);
        newCredential.setTemporary(temporary);
        userResource.resetPassword(newCredential);
    }

    public static void assignRealmRoles(RealmResource realm, String userId, String... roles) {
        String realmName = realm.toRepresentation().getRealm();

        List<RoleRepresentation> roleRepresentations = new ArrayList<>();
        for (String roleName : roles) {
            RoleRepresentation role = realm.roles().get(roleName).toRepresentation();
            roleRepresentations.add(role);
        }

        UserResource userResource = realm.users().get(userId);
        log.info("assigning roles " + Arrays.toString(roles) + " to user: \""
                + userResource.toRepresentation().getUsername() + "\" in realm: \"" + realmName + "\"");
        userResource.roles().realmLevel().add(roleRepresentations);
    }

    public static void removeUserByUsername(RealmResource realmResource, String username) {
        UserRepresentation user = findUserByUsername(realmResource, username);
        if (user != null) {
            realmResource.users().delete(user.getId());
        }
    }

    public static void assignClientRoles(RealmResource realm, String userId, String clientName, String... roles) {
        String realmName = realm.toRepresentation().getRealm();
        String clientId = "";
        for (ClientRepresentation clientRepresentation : realm.clients().findAll()) {
            if (clientRepresentation.getClientId().equals(clientName)) {
                clientId = clientRepresentation.getId();
            }
        }

        if (!clientId.isEmpty()) {
            ClientResource clientResource = realm.clients().get(clientId);

            List<RoleRepresentation> roleRepresentations = new ArrayList<>();
            for (String roleName : roles) {
                RoleRepresentation role = clientResource.roles().get(roleName).toRepresentation();
                roleRepresentations.add(role);
            }

            UserResource userResource = realm.users().get(userId);
            log.info("assigning role: " + Arrays.toString(roles) + " to user: \""
                    + userResource.toRepresentation().getUsername() + "\" of client: \""
                    + clientName + "\" in realm: \"" + realmName + "\"");
            userResource.roles().clientLevel(clientId).add(roleRepresentations);
        } else {
            log.warn("client with name " + clientName + " doesn't exist in realm " + realmName);
        }
    }

    public static boolean groupContainsSubgroup(GroupRepresentation group, GroupRepresentation subgroup) {
        boolean contains = false;
        for (GroupRepresentation sg : group.getSubGroups()) {
            if (subgroup.getId().equals(sg.getId())) {
                contains = true;
                break;
            }
        }
        return contains;
    }

    public static AuthorizationResource findAuthorizationSettings(RealmResource realm, String clientId) {
        for (ClientRepresentation c : realm.clients().findAll()) {
            if (c.getClientId().equals(clientId)) {
                return realm.clients().get(c.getId()).authorization();
            }
        }
        return null;
    }

    public static KeysMetadataRepresentation.KeyMetadataRepresentation findActiveKey(RealmResource realm) {
        KeysMetadataRepresentation keyMetadata = realm.keys().getKeyMetadata();
        String activeKid = keyMetadata.getActive().get(Algorithm.RS256);
        for (KeysMetadataRepresentation.KeyMetadataRepresentation rep : keyMetadata.getKeys()) {
            if (rep.getKid().equals(activeKid)) {
                return rep;
            }
        }
        return null;
    }

}
