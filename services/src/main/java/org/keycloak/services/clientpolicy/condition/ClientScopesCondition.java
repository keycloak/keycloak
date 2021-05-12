/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;
import org.keycloak.services.clientpolicy.context.TokenRequestContext;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientScopesCondition extends AbstractClientPolicyConditionProvider<ClientScopesCondition.Configuration> {

    private static final Logger logger = Logger.getLogger(ClientScopesCondition.class);

    public ClientScopesCondition(KeycloakSession session) {
        super(session);
    }

    @Override
    public Class<Configuration> getConditionConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyConditionConfigurationRepresentation {

        protected String type;
        protected List<String> scope;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getScope() {
            return scope;
        }

        public void setScope(List<String> scope) {
            this.scope = scope;
        }
    }

    @Override
    public String getProviderId() {
        return ClientScopesConditionFactory.PROVIDER_ID;
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
        Set<String> defaultScopes = client.getClientScopes(true).keySet();
        Set<String> optionalScopes = client.getClientScopes(false).keySet();
        Set<String> expectedScopes = getScopesForMatching();
        if (expectedScopes == null) return false;

        if (logger.isTraceEnabled()) {
            explicitSpecifiedScopes.forEach(i -> logger.tracev("explicit specified client scope = {0}", i));
            defaultScopes.forEach(i -> logger.tracev("default client scope = {0}", i));
            optionalScopes.forEach(i -> logger.tracev("optional client scope = {0}", i));
            expectedScopes.forEach(i -> logger.tracev("expected scope = {0}", i));
        }

        boolean isDefaultScope = ClientScopesConditionFactory.DEFAULT.equals(configuration.getType());

        if (isDefaultScope) {
            expectedScopes.retainAll(defaultScopes); // may change expectedScopes so that it has needed to be instantiated.
            return expectedScopes.isEmpty() ? false : true;
        } else {
            explicitSpecifiedScopes.retainAll(expectedScopes);
            explicitSpecifiedScopes.retainAll(optionalScopes);
            if (!explicitSpecifiedScopes.isEmpty()) {
                if (logger.isTraceEnabled()) {
                    explicitSpecifiedScopes.forEach(i->logger.tracev("matched scope = {0}", i));
                }
                return true;
            }
        }
        return false;
    }

    private Set<String> getScopesForMatching() {
        List<String> scopes = configuration.getScope();
        if (scopes == null) return null;
        return new HashSet<>(scopes);
    }

}