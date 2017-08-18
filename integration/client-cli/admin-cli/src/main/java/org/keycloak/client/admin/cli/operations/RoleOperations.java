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
package org.keycloak.client.admin.cli.operations;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.ArrayList;
import java.util.List;

import static org.keycloak.client.admin.cli.util.HttpUtil.composeResourceUrl;
import static org.keycloak.client.admin.cli.util.HttpUtil.doDeleteJSON;
import static org.keycloak.client.admin.cli.util.HttpUtil.doGetJSON;
import static org.keycloak.client.admin.cli.util.HttpUtil.doPostJSON;
import static org.keycloak.client.admin.cli.util.HttpUtil.getAttrForType;
import static org.keycloak.client.admin.cli.util.HttpUtil.getIdForType;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class RoleOperations {

    public static class LIST_OF_ROLES extends ArrayList<RoleRepresentation>{};
    public static class LIST_OF_NODES extends ArrayList<ObjectNode>{};

    public static String getIdFromRoleName(String adminRoot, String realm, String auth, String rname) {
        return getIdForType(adminRoot, realm, auth, "roles", "name", rname);
    }

    public static void addRealmRoles(String rootUrl, String realm, String auth, String roleid, List<?> roles) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "roles-by-id/" + roleid + "/composites");
        doPostJSON(resourceUrl, auth, roles);
    }

    public static void addClientRoles(String rootUrl, String realm, String auth, String roleid, List<?> roles) {
        addRealmRoles(rootUrl, realm, auth, roleid, roles);
    }

    public static void removeRealmRoles(String rootUrl, String realm, String auth, String roleid, List<?> roles) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "roles-by-id/" + roleid + "/composites");
        doDeleteJSON(resourceUrl, auth, roles);
    }

    public static void removeClientRoles(String rootUrl, String realm, String auth, String roleid, List<?> roles) {
        removeRealmRoles(rootUrl, realm, auth, roleid, roles);
    }

    public static String getRoleNameFromId(String adminRoot, String realm, String auth, String rid) {
        return getAttrForType(adminRoot, realm, auth, "roles", "id", rid, "name");
    }

    public static String getClientRoleNameFromId(String adminRoot, String realm, String auth, String cid, String rid) {
        return getAttrForType(adminRoot, realm, auth, "clients/" + cid + "/roles", "id", rid, "name");
    }

    public static List<RoleRepresentation> getRealmRoles(String rootUrl, String realm, String auth) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "roles");
        return doGetJSON(LIST_OF_ROLES.class, resourceUrl, auth);
    }

    public static ObjectNode getRealmRole(String rootUrl, String realm, String rolename, String auth) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "roles/" + rolename);
        return doGetJSON(ObjectNode.class, resourceUrl, auth);
    }

    public static List<ObjectNode> getClientRoles(String rootUrl, String realm, String idOfClient, String auth) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "clients/" + idOfClient + "/roles");
        return doGetJSON(LIST_OF_NODES.class, resourceUrl, auth);
    }

    public static ObjectNode getClientRole(String rootUrl, String realm, String idOfClient, String rolename, String auth) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "clients/" + idOfClient + "/roles/" + rolename);
        return doGetJSON(ObjectNode.class, resourceUrl, auth);
    }

    public static List<ObjectNode> getRealmRolesAsNodes(String rootUrl, String realm, String auth) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "roles");
        return doGetJSON(LIST_OF_NODES.class, resourceUrl, auth);
    }

    public static List<ObjectNode> getRealmRolesForUserAsNodes(String rootUrl, String realm, String userid, String auth) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "users/" + userid + "/role-mappings/realm");
        return doGetJSON(LIST_OF_NODES.class, resourceUrl, auth);
    }

    public static List<ObjectNode> getCompositeRealmRolesForUserAsNodes(String rootUrl, String realm, String userid, String auth) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "users/" + userid + "/role-mappings/realm/composite");
        return doGetJSON(LIST_OF_NODES.class, resourceUrl, auth);
    }

    public static List<ObjectNode> getAvailableRealmRolesForUserAsNodes(String rootUrl, String realm, String userid, String auth) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "users/" + userid + "/role-mappings/realm/available");
        return doGetJSON(LIST_OF_NODES.class, resourceUrl, auth);
    }

    public static List<ObjectNode> getClientRolesForUserAsNodes(String rootUrl, String realm, String userid, String idOfClient, String auth) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "users/" + userid + "/role-mappings/clients/" + idOfClient);
        return doGetJSON(LIST_OF_NODES.class, resourceUrl, auth);
    }

    public static List<ObjectNode> getCompositeClientRolesForUserAsNodes(String rootUrl, String realm, String userid, String idOfClient, String auth) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "users/" + userid + "/role-mappings/clients/" + idOfClient + "/composite");
        return doGetJSON(LIST_OF_NODES.class, resourceUrl, auth);
    }

    public static List<ObjectNode> getAvailableClientRolesForUserAsNodes(String rootUrl, String realm, String userid, String idOfClient, String auth) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "users/" + userid + "/role-mappings/clients/" + idOfClient + "/available");
        return doGetJSON(LIST_OF_NODES.class, resourceUrl, auth);
    }

    public static List<ObjectNode> getRealmRolesForGroupAsNodes(String rootUrl, String realm, String groupid, String auth) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "groups/" + groupid + "/role-mappings/realm");
        return doGetJSON(LIST_OF_NODES.class, resourceUrl, auth);
    }

    public static List<ObjectNode> getCompositeRealmRolesForGroupAsNodes(String rootUrl, String realm, String groupid, String auth) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "groups/" + groupid + "/role-mappings/realm/composite");
        return doGetJSON(LIST_OF_NODES.class, resourceUrl, auth);
    }

    public static List<ObjectNode> getAvailableRealmRolesForGroupAsNodes(String rootUrl, String realm, String groupid, String auth) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "groups/" + groupid + "/role-mappings/realm/available");
        return doGetJSON(LIST_OF_NODES.class, resourceUrl, auth);
    }

    public static List<ObjectNode> getClientRolesForGroupAsNodes(String rootUrl, String realm, String groupid, String idOfClient, String auth) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "groups/" + groupid + "/role-mappings/clients/" + idOfClient);
        return doGetJSON(LIST_OF_NODES.class, resourceUrl, auth);
    }

    public static List<ObjectNode> getCompositeClientRolesForGroupAsNodes(String rootUrl, String realm, String groupid, String idOfClient, String auth) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "groups/" + groupid + "/role-mappings/clients/" + idOfClient + "/composite");
        return doGetJSON(LIST_OF_NODES.class, resourceUrl, auth);
    }

    public static List<ObjectNode> getAvailableClientRolesForGroupAsNodes(String rootUrl, String realm, String groupid, String idOfClient, String auth) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "groups/" + groupid + "/role-mappings/clients/" + idOfClient + "/available");
        return doGetJSON(LIST_OF_NODES.class, resourceUrl, auth);
    }
}
