/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.clientpolicy.executor;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.TokenExchangeRequestContext;

/**
 *
 * @author rmartinc
 */
public class DownscopeAssertionGrantEnforcerExecutor implements ClientPolicyExecutorProvider {

    private final KeycloakSession session;

    public DownscopeAssertionGrantEnforcerExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getProviderId() {
        return DownscopeAssertionGrantEnforcerExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case TOKEN_EXCHANGE_REQUEST -> {
                TokenExchangeContext tokenExchangeContext = ((TokenExchangeRequestContext) context).getTokenExchangeContext();
                Set<String> restrictedScopes = checkDownscope(tokenExchangeContext.getClient(),
                        getAccessTokenFromSubjectToken(tokenExchangeContext),
                        tokenExchangeContext.getParams().getScope());
                tokenExchangeContext.setRestrictedScopes(restrictedScopes);
            }
        }
    }

    private AccessToken getAccessTokenFromSubjectToken(TokenExchangeContext context) throws ClientPolicyException {
        if (!OAuth2Constants.ACCESS_TOKEN_TYPE.equals(context.getParams().getSubjectTokenType())) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Parameter 'subject_token' should be access_token for the executor");
        }
        try {
            return new JWSInput(context.getParams().getSubjectToken())
                    .readJsonContent(AccessToken.class);
        } catch (JWSInputException e) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Parameter 'subject_token' contains an invalid access token");
        }
    }

    private Set<String> checkDownscope(ClientModel client, AccessToken token, String scopeParam) throws ClientPolicyException {
        Set<String> tokenScopes = token.getScope() != null
                ? TokenManager.parseScopeParameter(token.getScope()).collect(Collectors.toSet())
                : Collections.emptySet();

        if (scopeParam != null) {
            // the user requested specific scopes, check they are allowed
            Set<String> requestedScopes = TokenManager.parseScopeParameter(scopeParam).collect(Collectors.toSet());
            // check all requested scopes are inside the token
            requestedScopes.removeAll(tokenScopes);
            if (!requestedScopes.isEmpty()) {
                throw new ClientPolicyException(OAuthErrorException.INVALID_SCOPE,
                        String.format("Scopes %s not present in the initial access token %s", requestedScopes, tokenScopes));
            }
        }

        // always add as allowed restricted scopes the ones that are default and not included in token
        Set<String> restrictedScopes = client.getClientScopes(true).values().stream()
                .filter(Predicate.not(ClientScopeModel::isIncludeInTokenScope))
                .map(ClientScopeModel::getName)
                .collect(Collectors.toSet());
        restrictedScopes.addAll(tokenScopes);
        return restrictedScopes;
    }
}
