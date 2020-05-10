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

package org.keycloak.services.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.util.TokenUtil;

/**
 * Not thread safe. It's per-request object
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultClientSessionContext implements ClientSessionContext {

    private static Logger logger = Logger.getLogger(DefaultClientSessionContext.class);

    private final AuthenticatedClientSessionModel clientSession;
    private final Set<String> clientScopeIds;
    private final KeycloakSession session;

    private Set<ClientScopeModel> clientScopes;

    //
    private Set<RoleModel> roles;
    private Set<ProtocolMapperModel> protocolMappers;

    // All roles of user expanded. It doesn't yet take into account permitted clientScopes
    private Set<RoleModel> userRoles;

    private Map<String, Object> attributes = new HashMap<>();

    private DefaultClientSessionContext(AuthenticatedClientSessionModel clientSession, Set<String> clientScopeIds, KeycloakSession session) {
        this.clientSession = clientSession;
        this.clientScopeIds = clientScopeIds;
        this.session = session;
    }


    /**
     * Useful if we want to "re-compute" client scopes based on the scope parameter
     */
    public static DefaultClientSessionContext fromClientSessionScopeParameter(AuthenticatedClientSessionModel clientSession, KeycloakSession session) {
        return fromClientSessionAndScopeParameter(clientSession, clientSession.getNote(OAuth2Constants.SCOPE), session);
    }


    public static DefaultClientSessionContext fromClientSessionAndScopeParameter(AuthenticatedClientSessionModel clientSession, String scopeParam, KeycloakSession session) {
        Set<ClientScopeModel> requestedClientScopes = TokenManager.getRequestedClientScopes(scopeParam, clientSession.getClient());
        return fromClientSessionAndClientScopes(clientSession, requestedClientScopes, session);
    }


    public static DefaultClientSessionContext fromClientSessionAndClientScopeIds(AuthenticatedClientSessionModel clientSession, Set<String> clientScopeIds, KeycloakSession session) {
        return new DefaultClientSessionContext(clientSession, clientScopeIds, session);
    }


    public static DefaultClientSessionContext fromClientSessionAndClientScopes(AuthenticatedClientSessionModel clientSession, Set<ClientScopeModel> clientScopes, KeycloakSession session) {
        Set<String> clientScopeIds = new HashSet<>();
        for (ClientScopeModel clientScope : clientScopes) {
            clientScopeIds.add(clientScope.getId());
        }

        return new DefaultClientSessionContext(clientSession, clientScopeIds, session);
    }


    @Override
    public AuthenticatedClientSessionModel getClientSession() {
        return clientSession;
    }


    @Override
    public Set<String> getClientScopeIds() {
        return clientScopeIds;
    }


    @Override
    public Set<ClientScopeModel> getClientScopes() {
        // Load client scopes if not yet present
        if (clientScopes == null) {
            clientScopes = loadClientScopes();
        }
        return clientScopes;
    }


    @Override
    public Set<RoleModel> getRoles() {
        // Load roles if not yet present
        if (roles == null) {
            roles = loadRoles();
        }
        return roles;
    }


    @Override
    public Set<ProtocolMapperModel> getProtocolMappers() {
        // Load protocolMappers if not yet present
        if (protocolMappers == null) {
            protocolMappers = loadProtocolMappers();
        }
        return protocolMappers;
    }


    private Set<RoleModel> getUserRoles() {
        // Load userRoles if not yet present
        if (userRoles == null) {
            userRoles = loadUserRoles();
        }
        return userRoles;
    }


    @Override
    public String getScopeString() {
        StringBuilder builder = new StringBuilder();

        // Add both default and optional scopes to scope parameter. Don't add client itself
        boolean first = true;
        for (ClientScopeModel clientScope : getClientScopes()) {
            if (clientScope instanceof ClientModel) {
                continue;
            }

            if (!clientScope.isIncludeInTokenScope()) {
                continue;
            }

            if (first) {
                first = false;
            } else {
                builder.append(" ");
            }
            builder.append(clientScope.getName());
        }

        String scopeParam = builder.toString();

        // See if "openid" scope is requested
        String scopeSent = clientSession.getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeSent)) {
            scopeParam = TokenUtil.attachOIDCScope(scopeParam);
        }

        return scopeParam;
    }


    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }


    @Override
    public <T> T getAttribute(String name, Class<T> clazz) {
        Object value = attributes.get(name);
        return clazz.cast(value);
    }


    // Loading data

    private Set<ClientScopeModel> loadClientScopes() {
        Set<ClientScopeModel> clientScopes = new HashSet<>();
        for (String scopeId : clientScopeIds) {
            ClientScopeModel clientScope = KeycloakModelUtils.findClientScopeById(clientSession.getClient().getRealm(), getClientSession().getClient(), scopeId);
            if (clientScope != null) {
                if (isClientScopePermittedForUser(clientScope)) {
                    clientScopes.add(clientScope);
                } else {
                    if (logger.isTraceEnabled()) {
                        logger.tracef("User '%s' not permitted to have client scope '%s'",
                                clientSession.getUserSession().getUser().getUsername(), clientScope.getName());
                    }
                }
            }
        }
        return clientScopes;
    }


    // Return true if clientScope can be used by the user.
    private boolean isClientScopePermittedForUser(ClientScopeModel clientScope) {
        if (clientScope instanceof ClientModel) {
            return true;
        }

        Set<RoleModel> clientScopeRoles = clientScope.getScopeMappings();

        // Client scope is automatically permitted if it doesn't have any role scope mappings
        if (clientScopeRoles.isEmpty()) {
            return true;
        }

        // Expand (resolve composite roles)
        clientScopeRoles = RoleUtils.expandCompositeRoles(clientScopeRoles);

        // Check if expanded roles of clientScope has any intersection with expanded roles of user. If not, it is not permitted
        clientScopeRoles.retainAll(getUserRoles());
        return !clientScopeRoles.isEmpty();
    }


    private Set<RoleModel> loadRoles() {
        UserModel user = clientSession.getUserSession().getUser();
        ClientModel client = clientSession.getClient();

        Set<ClientScopeModel> clientScopes = getClientScopes();

        return TokenManager.getAccess(user, client, clientScopes);
    }


    private Set<ProtocolMapperModel> loadProtocolMappers() {
        Set<ClientScopeModel> clientScopes = getClientScopes();
        String protocol = clientSession.getClient().getProtocol();

        // Being rather defensive. But protocol should normally always be there
        if (protocol == null) {
            logger.warnf("Client '%s' doesn't have protocol set. Fallback to openid-connect. Please fix client configuration", clientSession.getClient().getClientId());
            protocol = OIDCLoginProtocol.LOGIN_PROTOCOL;
        }

        Set<ProtocolMapperModel> protocolMappers = new HashSet<>();
        for (ClientScopeModel clientScope : clientScopes) {
            Set<ProtocolMapperModel> currentMappers = clientScope.getProtocolMappers();
            for (ProtocolMapperModel currentMapper : currentMappers) {
                if (protocol.equals(currentMapper.getProtocol()) && ProtocolMapperUtils.isEnabled(session, currentMapper)) {
                    protocolMappers.add(currentMapper);
                }
            }
        }

        return protocolMappers;
    }


    private Set<RoleModel> loadUserRoles() {
        UserModel user = clientSession.getUserSession().getUser();
        return RoleUtils.getDeepUserRoleMappings(user);
    }

}
