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
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
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
import org.keycloak.rar.AuthorizationRequestContext;
import org.keycloak.rar.AuthorizationRequestSource;
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
        this.clientScopeIds = clientScopeIds;
        this.clientSession = clientSession;
        this.session = session;
    }


    /**
     * Useful if we want to "re-compute" client scopes based on the scope parameter
     */
    public static DefaultClientSessionContext fromClientSessionScopeParameter(AuthenticatedClientSessionModel clientSession, KeycloakSession session) {
        return fromClientSessionAndScopeParameter(clientSession, clientSession.getNote(OAuth2Constants.SCOPE), session);
    }


    public static DefaultClientSessionContext fromClientSessionAndScopeParameter(AuthenticatedClientSessionModel clientSession, String scopeParam, KeycloakSession session) {
        Stream<ClientScopeModel> requestedClientScopes;
        if (Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
            requestedClientScopes = AuthorizationContextUtil.getClientScopesStreamFromAuthorizationRequestContextWithClient(session, scopeParam);
        } else {
            requestedClientScopes = TokenManager.getRequestedClientScopes(scopeParam, clientSession.getClient());
        }
        return fromClientSessionAndClientScopes(clientSession, requestedClientScopes, session);
    }


    public static DefaultClientSessionContext fromClientSessionAndClientScopeIds(AuthenticatedClientSessionModel clientSession, Set<String> clientScopeIds, KeycloakSession session) {
        return new DefaultClientSessionContext(clientSession, clientScopeIds, session);
    }


    // in order to standardize the way we create this object and with that data, it's better to compute the client scopes internally instead of relying on external sources
    // i.e: the TokenManager.getRequestedClientScopes was being called in many places to obtain the ClientScopeModel stream.
    // by changing this method to private, we'll only call it in this class, while also having a single place to put the DYNAMIC_SCOPES feature flag condition
    private static DefaultClientSessionContext fromClientSessionAndClientScopes(AuthenticatedClientSessionModel clientSession,
                                                                               Stream<ClientScopeModel> clientScopes,
                                                                               KeycloakSession session) {
        Set<String> clientScopeIds = clientScopes.map(ClientScopeModel::getId).collect(Collectors.toSet());
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
    public Stream<ClientScopeModel> getClientScopesStream() {
        // Load client scopes if not yet present
        if (clientScopes == null) {
            clientScopes = loadClientScopes();
        }
        return clientScopes.stream();
    }


    @Override
    public Stream<RoleModel> getRolesStream() {
        // Load roles if not yet present
        if (roles == null) {
            roles = loadRoles();
        }
        return roles.stream();
    }


    @Override
    public Stream<ProtocolMapperModel> getProtocolMappersStream() {
        // Load protocolMappers if not yet present
        if (protocolMappers == null) {
            protocolMappers = loadProtocolMappers();
        }
        return protocolMappers.stream();
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
        if (Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
            String scopeParam = buildScopesStringFromAuthorizationRequest();
            logger.tracef("Generated scope param with Dynamic Scopes enabled: %1s", scopeParam);
            String scopeSent = clientSession.getNote(OAuth2Constants.SCOPE);
            if (TokenUtil.isOIDCRequest(scopeSent)) {
                scopeParam = TokenUtil.attachOIDCScope(scopeParam);
            }
            return scopeParam;
        }
        // Add both default and optional scopes to scope parameter. Don't add client itself
        String scopeParam = getClientScopesStream()
                .filter(((Predicate<ClientScopeModel>) ClientModel.class::isInstance).negate())
                .filter(ClientScopeModel::isIncludeInTokenScope)
                .map(ClientScopeModel::getName)
                .collect(Collectors.joining(" "));

        // See if "openid" scope is requested
        String scopeSent = clientSession.getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeSent)) {
            scopeParam = TokenUtil.attachOIDCScope(scopeParam);
        }

        return scopeParam;
    }

    /**
     * Get all the scopes from the {@link AuthorizationRequestContext} by filtering entries by Source and by whether
     * they should be included in tokens or not.
     * Then return the scope name from the data stored in the RAR object representation.
     *
     * @return see description
     */
    private String buildScopesStringFromAuthorizationRequest() {
        return AuthorizationContextUtil.getAuthorizationRequestContextFromScopes(session, clientSession.getNote(OAuth2Constants.SCOPE)).getAuthorizationDetailEntries().stream()
                .filter(authorizationDetails -> authorizationDetails.getSource().equals(AuthorizationRequestSource.SCOPE))
                .filter(authorizationDetails -> authorizationDetails.getClientScope().isIncludeInTokenScope())
                .filter(authorizationDetails -> isClientScopePermittedForUser(authorizationDetails.getClientScope()))
                .map(authorizationDetails -> authorizationDetails.getAuthorizationDetails().getScopeNameFromCustomData())
                .collect(Collectors.joining(" "));
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

    @Override
    public AuthorizationRequestContext getAuthorizationRequestContext() {
        return AuthorizationContextUtil.getAuthorizationRequestContextFromScopes(session, clientSession.getNote(OAuth2Constants.SCOPE));
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

        Set<RoleModel> clientScopeRoles = clientScope.getScopeMappingsStream().collect(Collectors.toSet());

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
        return TokenManager.getAccess(user, client, getClientScopesStream());
    }


    private Set<ProtocolMapperModel> loadProtocolMappers() {
        String protocol = clientSession.getClient().getProtocol();

        // Being rather defensive. But protocol should normally always be there
        if (protocol == null) {
            logger.warnf("Client '%s' doesn't have protocol set. Fallback to openid-connect. Please fix client configuration",
                    clientSession.getClient().getClientId());
            protocol = OIDCLoginProtocol.LOGIN_PROTOCOL;
        }

        String finalProtocol = protocol;
        return getClientScopesStream()
                .flatMap(clientScope -> clientScope.getProtocolMappersStream()
                        .filter(mapper -> Objects.equals(finalProtocol, mapper.getProtocol()) &&
                                ProtocolMapperUtils.isEnabled(session, mapper)))
                .collect(Collectors.toSet());
    }


    private Set<RoleModel> loadUserRoles() {
        UserModel user = clientSession.getUserSession().getUser();
        return RoleUtils.getDeepUserRoleMappings(user);
    }

}
