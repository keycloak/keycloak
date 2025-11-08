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

import org.keycloak.OAuth2Constants;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.grants.ciba.channel.CIBAAuthenticationRequest;
import org.keycloak.protocol.oidc.grants.ciba.clientpolicy.context.BackchannelAuthenticationRequestContext;
import org.keycloak.protocol.oidc.grants.ciba.clientpolicy.context.BackchannelTokenRequestContext;
import org.keycloak.protocol.oidc.grants.ciba.clientpolicy.context.BackchannelTokenResponseContext;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;
import org.keycloak.services.clientpolicy.context.ServiceAccountTokenRequestContext;
import org.keycloak.services.clientpolicy.context.ServiceAccountTokenResponseContext;
import org.keycloak.services.clientpolicy.context.TokenExchangeRequestContext;
import org.keycloak.services.clientpolicy.context.TokenRequestContext;
import org.keycloak.services.clientpolicy.context.TokenResponseContext;

import org.jboss.logging.Logger;

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
        protected List<String> scopes;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scope) {
            this.scopes = scope;
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
            case TOKEN_RESPONSE:
                if (isScopeMatched(((TokenResponseContext)context).getParseResult().getClientSession())) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            case SERVICE_ACCOUNT_TOKEN_REQUEST:
                if (isScopeMatched(((ServiceAccountTokenRequestContext)context).getClientSession())) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            case SERVICE_ACCOUNT_TOKEN_RESPONSE:
                if (isScopeMatched(((ServiceAccountTokenResponseContext)context).getClientSession())) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            case BACKCHANNEL_AUTHENTICATION_REQUEST:
                if (isScopeMatched(((BackchannelAuthenticationRequestContext)context).getParsedRequest())) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            case BACKCHANNEL_TOKEN_REQUEST:
                if (isScopeMatched(((BackchannelTokenRequestContext)context).getParsedRequest())) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            case BACKCHANNEL_TOKEN_RESPONSE:
                if (isScopeMatched(((BackchannelTokenResponseContext)context).getParsedRequest())) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            case TOKEN_EXCHANGE_REQUEST:
                if (isScopeMatched(((TokenExchangeRequestContext) context).getTokenExchangeContext())) return ClientPolicyVote.YES;
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

    private boolean isScopeMatched(CIBAAuthenticationRequest request) {
        if (request == null || request.getClient() == null) return false;
        return isScopeMatched(request.getScope(), session.getContext().getRealm().getClientByClientId(request.getClient().getClientId()));
    }

    private boolean isScopeMatched(TokenExchangeContext context) {
        if (context == null) return false;
        return isScopeMatched(context.getParams().getScope(), context.getClient());
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

        switch (configuration.getType()) {
            case ClientScopesConditionFactory.DEFAULT:
                expectedScopes.retainAll(defaultScopes);
                return !expectedScopes.isEmpty();

            case ClientScopesConditionFactory.OPTIONAL:
                explicitSpecifiedScopes.retainAll(expectedScopes);
                explicitSpecifiedScopes.retainAll(optionalScopes);
                if (logger.isTraceEnabled()) {
                    explicitSpecifiedScopes.forEach(i->logger.tracev("matched scope = {0}", i));
                }
                return !explicitSpecifiedScopes.isEmpty();

            case ClientScopesConditionFactory.ANY:
                explicitSpecifiedScopes.retainAll(expectedScopes);
                explicitSpecifiedScopes.retainAll(optionalScopes);
                expectedScopes.retainAll(defaultScopes);
                return !expectedScopes.isEmpty() || !explicitSpecifiedScopes.isEmpty();

            default:
                return false;
        }
    }

    private Set<String> getScopesForMatching() {
        List<String> scopes = configuration.getScopes();
        if (scopes == null) return null;
        return new HashSet<>(scopes);
    }
}
