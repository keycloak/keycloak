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

import java.util.List;

import static org.keycloak.client.cli.util.HttpUtil.composeResourceUrl;
import static org.keycloak.client.cli.util.HttpUtil.doDeleteJSON;
import static org.keycloak.client.cli.util.HttpUtil.doPostJSON;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class GroupOperations {

    public static String getIdFromName(String rootUrl, String realm, String auth, String groupname) {
        return OperationUtils.getIdForType(rootUrl, realm, auth, "groups", "search", groupname, "name", () -> new String[] { "exact", "true" });
    }

    public static String getIdFromPath(String rootUrl, String realm, String auth, String path) {
        return OperationUtils.getIdForType(rootUrl, realm, auth, "groups", "path", path, "path");
    }

    public static void addRealmRoles(String rootUrl, String realm, String auth, String groupid, List<?> roles) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "groups/" + groupid + "/role-mappings/realm");
        doPostJSON(resourceUrl, auth, roles);
    }

    public static void addClientRoles(String rootUrl, String realm, String auth, String groupid, String idOfClient, List<?> roles) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "groups/" + groupid + "/role-mappings/clients/" + idOfClient);
        doPostJSON(resourceUrl, auth, roles);
    }

    public static void removeRealmRoles(String rootUrl, String realm, String auth, String groupid, List<?> roles) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "groups/" + groupid + "/role-mappings/realm");
        doDeleteJSON(resourceUrl, auth, roles);
    }

    public static void removeClientRoles(String rootUrl, String realm, String auth, String groupid, String idOfClient, List<?> roles) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "groups/" + groupid + "/role-mappings/clients/" + idOfClient);
        doDeleteJSON(resourceUrl, auth, roles);
    }
}
