/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.utils;

import java.util.Map;
import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.AccessToken;

/**
 * Helper class to ensure that all the user's permitted roles (including composite roles) are loaded just once per request.
 * Then all underlying protocolMappers can consume them.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RoleResolveUtil {

    private static final String RESOLVED_ROLES_ATTR = "RESOLVED_ROLES";


    /**
     * Object (possibly null) containing all the user's realm roles. Including user's groups roles. Composite roles are expanded.
     * Just the roles, which current client has role-scope-mapping for (or it's clientScopes) are included.
     * Current client means the client corresponding to specified clientSessionCtx.
     *
     * @param session
     * @param clientSessionCtx
     * @param createIfMissing
     * @return can return null (just in case that createIfMissing is false)
     */
    public static AccessToken.Access getResolvedRealmRoles(KeycloakSession session, ClientSessionContext clientSessionCtx, boolean createIfMissing) {
        AccessToken rolesToken = getAndCacheResolvedRoles(session, clientSessionCtx);
        AccessToken.Access access = rolesToken.getRealmAccess();
        if (access == null && createIfMissing) {
            access = new AccessToken.Access();
            rolesToken.setRealmAccess(access);
        }

        return access;
    }


    /**
     * Object (possibly null) containing all the user's client roles of client specified by clientId. Including user's groups roles.
     * Composite roles are expanded. Just the roles, which current client has role-scope-mapping for (or it's clientScopes) are included.
     * Current client means the client corresponding to specified clientSessionCtx.
     *
     * @param session
     * @param clientSessionCtx
     * @param clientId
     * @param createIfMissing
     * @return can return null (just in case that createIfMissing is false)
     */
    public static AccessToken.Access getResolvedClientRoles(KeycloakSession session, ClientSessionContext clientSessionCtx, String clientId, boolean createIfMissing) {
        AccessToken rolesToken = getAndCacheResolvedRoles(session, clientSessionCtx);
        AccessToken.Access access = rolesToken.getResourceAccess(clientId);

        if (access == null && createIfMissing) {
            access = rolesToken.addAccess(clientId);
        }

        return access;
    }


    /**
     * Object (but can be empty map) containing all the user's client roles of all clients. Including user's groups roles. Composite roles are expanded.
     * Just the roles, which current client has role-scope-mapping for (or it's clientScopes) are included.
     * Current client means the client corresponding to specified clientSessionCtx.
     *
     * @param session
     * @param clientSessionCtx
     * @return not-null object (can return empty map)
     */
    public static Map<String, AccessToken.Access> getAllResolvedClientRoles(KeycloakSession session, ClientSessionContext clientSessionCtx) {
        return getAndCacheResolvedRoles(session, clientSessionCtx).getResourceAccess();
    }

    private static AccessToken getAndCacheResolvedRoles(KeycloakSession session, ClientSessionContext clientSessionCtx) {
        ClientModel client = clientSessionCtx.getClientSession().getClient();
        String resolvedRolesAttrName = RESOLVED_ROLES_ATTR + ":" + clientSessionCtx.getClientSession().getUserSession().getId() + ":" + client.getId();
        AccessToken token = session.getAttribute(resolvedRolesAttrName, AccessToken.class);

        if (token == null) {
            token = new AccessToken();
            for (RoleModel role : clientSessionCtx.getRoles()) {
                addToToken(token, role);
            }
            session.setAttribute(resolvedRolesAttrName, token);
        }

        return token;
    }

    private static void addToToken(AccessToken token, RoleModel role) {
        AccessToken.Access access = null;
        if (role.getContainer() instanceof RealmModel) {
            access = token.getRealmAccess();
            if (token.getRealmAccess() == null) {
                access = new AccessToken.Access();
                token.setRealmAccess(access);
            } else if (token.getRealmAccess().getRoles() != null && token.getRealmAccess().isUserInRole(role.getName()))
                return;

        } else {
            ClientModel app = (ClientModel) role.getContainer();
            access = token.getResourceAccess(app.getClientId());
            if (access == null) {
                access = token.addAccess(app.getClientId());
                if (app.isSurrogateAuthRequired()) access.verifyCaller(true);
            } else if (access.isUserInRole(role.getName())) return;

        }
        access.addRole(role.getName());
    }

}
