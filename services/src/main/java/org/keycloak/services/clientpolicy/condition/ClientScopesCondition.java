/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.condition;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.services.clientpolicy.AuthorizationRequestContext;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyLogger;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.TokenRequestContext;

public class ClientScopesCondition implements ClientPolicyConditionProvider {

    private static final Logger logger = Logger.getLogger(ClientScopesCondition.class);

    private final KeycloakSession session;
    private final ComponentModel componentModel;

    public ClientScopesCondition(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case AUTHORIZATION_REQUEST:
                if (isScopeMatched(((AuthorizationRequestContext)context).getAuthorizationEndpointRequest())) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            case TOKEN_REQUEST:
                if (isScopeMatched(((TokenRequestContext)context).getParseResult().getClientSession())) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            default:
                return ClientPolicyVote.ABSTAIN;
        }
    }

    @Override
    public String getName() {
        return componentModel.getName();
    }

    @Override
    public String getProviderId() {
        return componentModel.getProviderId();
    }

    private boolean isScopeMatched(AuthenticatedClientSessionModel clientSession) {
        if (clientSession == null) return false;
        return isScopeMatched(clientSession.getNote(OAuth2Constants.SCOPE), clientSession.getClient());
    }

    private boolean isScopeMatched(AuthorizationEndpointRequest request) {
        if (request == null) return false;
        return isScopeMatched(request.getScope(), session.getContext().getRealm().getClientByClientId(request.getClientId()));
    }

    private boolean isScopeMatched(String explicitScopes, ClientModel client) {
        if (explicitScopes == null) explicitScopes = "";
        Collection<String> explicitSpecifiedScopes = new HashSet<>(Arrays.asList(explicitScopes.split(" ")));
        Set<String> defaultScopes = client.getClientScopes(true, true).keySet();
        Set<String> optionalScopes = client.getClientScopes(false, true).keySet();
        Set<String> expectedScopes = getScopesForMatching();
        if (expectedScopes == null) expectedScopes = new HashSet<>();

        if (logger.isTraceEnabled()) {
            explicitSpecifiedScopes.stream().forEach(i -> ClientPolicyLogger.log(logger, " explicit specified client scope = " + i));
            defaultScopes.stream().forEach(i -> ClientPolicyLogger.log(logger, " default client scope = " + i));
            optionalScopes.stream().forEach(i -> ClientPolicyLogger.log(logger, " optional client scope = " + i));
            expectedScopes.stream().forEach(i -> ClientPolicyLogger.log(logger, " expected scope = " + i));
        }

        boolean isDefaultScope = ClientScopesConditionFactory.DEFAULT.equals(componentModel.getConfig().getFirst(ClientScopesConditionFactory.TYPE));

        if (isDefaultScope) {
            expectedScopes.retainAll(defaultScopes);
            return expectedScopes.isEmpty() ? false : true;
        } else {
            explicitSpecifiedScopes.retainAll(expectedScopes);
            explicitSpecifiedScopes.retainAll(optionalScopes);
            if (!explicitSpecifiedScopes.isEmpty()) {
                explicitSpecifiedScopes.stream().forEach(i->{ClientPolicyLogger.log(logger, " matched scope = " + i);});
                return true;
            }
        }
        return false;
    }

    private Set<String> getScopesForMatching() {
        if (componentModel.getConfig() == null) return null;
        List<String> scopes = componentModel.getConfig().get(ClientScopesConditionFactory.SCOPES);
        if (scopes == null) return null;
        return new HashSet<>(scopes);
    }
}