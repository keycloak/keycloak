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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
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

import org.jboss.logging.Logger;

/**
 * Not thread safe. It's per-request object
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultClientSessionContext implements ClientSessionContext {

    private static final Logger logger = Logger.getLogger(DefaultClientSessionContext.class);

    private final AuthenticatedClientSessionModel clientSession;
    private final Set<ClientScopeModel> requestedScopes;
    private final KeycloakSession session;

    private Set<ClientScopeModel> allowedClientScopes;

    //
    private Set<RoleModel> roles;
    private Set<ProtocolMapperModel> protocolMappers;

    // All roles of user expanded. It doesn't yet take into account permitted clientScopes
    private Set<RoleModel> userRoles;

    private final Map<String, Object> attributes = new HashMap<>();
    private Set<String> clientScopeIds;
    private String scopeString;

    private final Set<String> restrictedScopes;

    private DefaultClientSessionContext(AuthenticatedClientSessionModel clientSession, Set<ClientScopeModel> requestedScopes, Set<String> restrictedScopes, KeycloakSession session) {
        this.requestedScopes = requestedScopes;
        this.restrictedScopes = restrictedScopes;
        this.clientSession = clientSession;
        this.session = session;
        this.session.setAttribute(ClientSessionContext.class.getName(), this);
    }


    /**
     * Useful if we want to "re-compute" client scopes based on the scope parameter
     */
    public static DefaultClientSessionContext fromClientSessionScopeParameter(AuthenticatedClientSessionModel clientSession, KeycloakSession session) {
        return fromClientSessionAndScopeParameter(clientSession, clientSession.getNote(OAuth2Constants.SCOPE), session);
    }


    public static DefaultClientSessionContext fromClientSessionAndScopeParameter(AuthenticatedClientSessionModel clientSession, String scopeParam, KeycloakSession session) {
        Stream<ClientScopeModel> requestedScopes;
        if (Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
            session.getContext().setClient(clientSession.getClient());
            requestedScopes = AuthorizationContextUtil.getClientScopesStreamFromAuthorizationRequestContextWithClient(session, scopeParam);
        } else {
            requestedScopes = TokenManager.getRequestedClientScopes(session, scopeParam, clientSession.getClient(), clientSession.getUserSession().getUser());
        }
        return new DefaultClientSessionContext(clientSession, requestedScopes.collect(Collectors.toSet()), null, session);
    }


    public static DefaultClientSessionContext fromClientSessionAndClientScopes(AuthenticatedClientSessionModel clientSession,
            Set<ClientScopeModel> requestedScopes, Set<String> restrictedScopes, KeycloakSession session) {
        return new DefaultClientSessionContext(clientSession, requestedScopes, restrictedScopes, session);
    }

    @Override
    public AuthenticatedClientSessionModel getClientSession() {
        return clientSession;
    }


    @Override
    public Set<String> getClientScopeIds() {
        if (clientScopeIds == null) {
            clientScopeIds = requestedScopes.stream()
                    .map(ClientScopeModel::getId)
                    .collect(Collectors.toSet());
        }
        return clientScopeIds;
    }


    @Override
    public Stream<ClientScopeModel> getClientScopesStream() {
        // Load client scopes if not yet present
        if (allowedClientScopes == null) {
            allowedClientScopes = requestedScopes.stream().filter(this::isAllowed).collect(Collectors.toSet());
        }
        return allowedClientScopes.stream();
    }

    @Override
    public boolean isOfflineTokenRequested() {
        Boolean offlineAccessRequested = getAttribute(OAuth2Constants.OFFLINE_ACCESS, Boolean.class);
        if (offlineAccessRequested != null) return offlineAccessRequested;

        ClientScopeModel offlineAccessScope = KeycloakModelUtils.getClientScopeByName(clientSession.getRealm(), OAuth2Constants.OFFLINE_ACCESS);
        offlineAccessRequested = offlineAccessScope == null ? false : getClientScopeIds().contains(offlineAccessScope.getId());
        setAttribute(OAuth2Constants.OFFLINE_ACCESS, offlineAccessRequested);
        return offlineAccessRequested;
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
        if (scopeString == null) {
            scopeString = getScopeString(false);
        }
        return scopeString;
    }

    @Override
    public String getScopeString(boolean ignoreIncludeInTokenScope) {
        if (Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
            String scopeParam = buildScopesStringFromAuthorizationRequest(ignoreIncludeInTokenScope);
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
                .filter(scope-> scope.isIncludeInTokenScope() || ignoreIncludeInTokenScope)
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
     * @param ignoreIncludeInTokenScope ignore include in token scope from client scope options
     *
     * @return see description
     */
    private String buildScopesStringFromAuthorizationRequest(boolean ignoreIncludeInTokenScope) {
        return AuthorizationContextUtil.getAuthorizationRequestContextFromScopes(session, clientSession.getNote(OAuth2Constants.SCOPE)).getAuthorizationDetailEntries().stream()
                .filter(authorizationDetails -> authorizationDetails.getSource().equals(AuthorizationRequestSource.SCOPE))
                .filter(authorizationDetails -> authorizationDetails.getClientScope().isIncludeInTokenScope() || ignoreIncludeInTokenScope)
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

    private boolean isAllowed(ClientScopeModel clientScope) {
        if (restrictedScopes != null && !restrictedScopes.contains(clientScope.getName())) {
            logger.tracef("Client scope '%s' is not among the restricted scopes list and will not be processed", clientScope.getName());
            return false;
        }

        if (!isClientScopePermittedForUser(clientScope)) {
            if (logger.isTraceEnabled()) {
                logger.tracef("User '%s' not permitted to have client scope '%s'",
                        clientSession.getUserSession().getUser().getUsername(), clientScope.getName());
            }
            return false;
        }

        return true;
    }

    // Return true if clientScope can be used by the user.
    private boolean isClientScopePermittedForUser(ClientScopeModel clientScope) {
        if (clientScope == null) {
            return false;
        }

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

        //remove roles that are not contained in requested audience
        if (attributes.get(Constants.REQUESTED_AUDIENCE_CLIENTS) != null) {
            final Set<String> requestedClientIdsFromAudience = Arrays.stream(getAttribute(Constants.REQUESTED_AUDIENCE_CLIENTS, ClientModel[].class))
                    .map(ClientModel::getId)
                    .collect(Collectors.toSet());
            clientScopeRoles.removeIf(role-> role.isClientRole() && !requestedClientIdsFromAudience.contains(role.getContainerId()));
        }

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
