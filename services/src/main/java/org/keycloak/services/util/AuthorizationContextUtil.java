/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.rar.AuthorizationRequestParserProvider;
import org.keycloak.protocol.oidc.rar.parsers.ClientScopeAuthorizationRequestParserProviderFactory;
import org.keycloak.rar.AuthorizationDetails;
import org.keycloak.rar.AuthorizationRequestContext;
import org.keycloak.rar.AuthorizationRequestSource;


/**
 * @author <a href="mailto:dgozalob@redhat.com">Daniel Gozalo</a>
 * Util class to unify a way to obtain the {@link AuthorizationRequestContext}.
 * <p>
 * As it can be obtained statically from just the OAuth2 scopes parameter, it can be easily referenced from almost anywhere.
 */
public class AuthorizationContextUtil {

    /**
     * Base function to obtain a bare AuthorizationRequestContext with just OAuth2 Scopes
     * @param session
     * @param scope
     * @return an {@link AuthorizationRequestContext} with scope entries
     */
    public static AuthorizationRequestContext getAuthorizationRequestContextFromScopes(KeycloakSession session, String scope) {
        if (!Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
            throw new RuntimeException("The Dynamic Scopes feature is not enabled and the AuthorizationRequestContext hasn't been generated");
        }
        AuthorizationRequestParserProvider clientScopeParser = session.getProvider(AuthorizationRequestParserProvider.class,
                ClientScopeAuthorizationRequestParserProviderFactory.CLIENT_SCOPE_PARSER_ID);

        if (clientScopeParser == null) {
            throw new RuntimeException(String.format("No provider found for authorization requests parser %1s",
                    ClientScopeAuthorizationRequestParserProviderFactory.CLIENT_SCOPE_PARSER_ID));
        }

        return clientScopeParser.parseScopes(scope);
    }

    /**
     * An extension of {@link AuthorizationContextUtil#getAuthorizationRequestContextFromScopes} that appends the current context's client
     * @param session
     * @param scope
     * @return an {@link AuthorizationRequestContext} with scope entries and a ClientModel
     */
    public static AuthorizationRequestContext getAuthorizationRequestContextFromScopesWithClient(KeycloakSession session, String scope) {
        AuthorizationRequestContext authorizationRequestContext = getAuthorizationRequestContextFromScopes(session, scope);
        authorizationRequestContext.getAuthorizationDetailEntries().add(new AuthorizationDetails(session.getContext().getClient()));
        return authorizationRequestContext;
    }

    /**
     * An extension of {@link AuthorizationContextUtil#getAuthorizationRequestContextFromScopesWithClient)} that returns the list as a Stream
     * @param session
     * @param scope
     * @return a Stream of {@link AuthorizationDetails} containing a ClientModel
     */
    public static Stream<AuthorizationDetails> getAuthorizationRequestsStreamFromScopesWithClient(KeycloakSession session, String scope) {
        AuthorizationRequestContext authorizationRequestContext = getAuthorizationRequestContextFromScopesWithClient(session, scope);
        return authorizationRequestContext.getAuthorizationDetailEntries().stream();
    }

    /**
     * Helper method to return a Stream of all the {@link ClientScopeModel} in the current {@link AuthorizationRequestContext}
     * @param session
     * @param scope
     * @return see description
     */
    public static Stream<ClientScopeModel> getClientScopesStreamFromAuthorizationRequestContextWithClient(KeycloakSession session, String scope) {
        return getAuthorizationRequestContextFromScopesWithClient(session, scope).getAuthorizationDetailEntries().stream()
                .filter(authorizationDetails -> authorizationDetails.getSource() == AuthorizationRequestSource.SCOPE)
                .map(AuthorizationDetails::getClientScope);
    }
}
