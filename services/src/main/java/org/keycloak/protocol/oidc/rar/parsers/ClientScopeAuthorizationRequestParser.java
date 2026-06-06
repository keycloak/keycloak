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
package org.keycloak.protocol.oidc.rar.parsers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.rar.AuthorizationRequestParserProvider;
import org.keycloak.protocol.oidc.rar.model.IntermediaryScopeRepresentation;
import org.keycloak.protocol.oidc.scope.InvalidScopeParameterException;
import org.keycloak.protocol.oidc.scope.ParameterizedScopeTypeProvider;
import org.keycloak.protocol.oidc.scope.StringScopeType;
import org.keycloak.rar.AuthorizationDetails;
import org.keycloak.rar.AuthorizationRequestContext;
import org.keycloak.rar.AuthorizationRequestSource;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.saml.common.util.StringUtil;

import org.jboss.logging.Logger;

import static org.keycloak.representations.AuthorizationDetailsJSONRepresentation.PARAMETERIZED_SCOPE_RAR_TYPE;
import static org.keycloak.representations.AuthorizationDetailsJSONRepresentation.STATIC_SCOPE_RAR_TYPE;

/**
 * @author <a href="mailto:dgozalob@redhat.com">Daniel Gozalo</a>
 */
public class ClientScopeAuthorizationRequestParser implements AuthorizationRequestParserProvider {

    protected static final Logger logger = Logger.getLogger(ClientScopeAuthorizationRequestParser.class);

    private final KeycloakSession session;

    public ClientScopeAuthorizationRequestParser(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Creates a {@link AuthorizationRequestContext} with a list of {@link AuthorizationDetails} that will be parsed from
     * the provided OAuth scopes that have been requested in a given Auth request, together with default client scopes.
     * <p>
     * Parameterized scopes will also be parsed with the extracted parameter, so it can be used later
     *
     * @param scopeParam the OAuth scope param for the current request
     * @return see description
     */
    @Override
    public AuthorizationRequestContext parseScopes(ClientModel client, String scopeParam) {
        // Process all the default ClientScopeModels for the current client, and maps them to the IntermediaryScopeRepresentation to make use of a HashSet
        Set<IntermediaryScopeRepresentation> clientScopeModelSet = client.getClientScopes(true).values().stream()
                .filter(clientScopeModel -> !clientScopeModel.isParameterizedScope()) // not strictly needed as Parameterized Scopes are going to be Optional scopes for now
                .map(IntermediaryScopeRepresentation::new)
                .collect(Collectors.toSet());

        Set<IntermediaryScopeRepresentation> intermediaryScopeRepresentations = new HashSet<>();
        if (scopeParam != null) {
            // Go through the parsed requested scopes and attempt to match them against the optional scopes list
            intermediaryScopeRepresentations = TokenManager.parseScopeParameter(scopeParam).collect(Collectors.toSet()).stream()
                    .map((String requestScope) -> getMatchingClientScope(requestScope, client.getClientScopes(false).values()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
        }

        // merge both sets, avoiding duplicates
        intermediaryScopeRepresentations.addAll(clientScopeModelSet);

        // Map the intermediary scope representations into the final AuthorizationDetails representation to be included into the RAR context
        List<AuthorizationDetails> authorizationDetails = intermediaryScopeRepresentations.stream()
                .map(this::buildAuthorizationDetailsJSONRepresentation)
                .collect(Collectors.toList());

        return new AuthorizationRequestContext(authorizationDetails);

    }

    /**
     * From a {@link IntermediaryScopeRepresentation}, create an {@link AuthorizationDetails} object that serves as the representation of a
     * ClientScope inside a Rich Authorization Request object
     *
     * @param intermediaryScopeRepresentation the intermediary scope representation to be included into the RAR request object
     * @return see description
     */
    private AuthorizationDetails buildAuthorizationDetailsJSONRepresentation(IntermediaryScopeRepresentation intermediaryScopeRepresentation) {
        AuthorizationDetailsJSONRepresentation representation = new AuthorizationDetailsJSONRepresentation();
        representation.setCustomData("access", Collections.singletonList(intermediaryScopeRepresentation.getRequestedScopeString()));
        representation.setType(STATIC_SCOPE_RAR_TYPE);
        if (intermediaryScopeRepresentation.isParameterized() && intermediaryScopeRepresentation.getParameter() != null) {
            representation.setType(PARAMETERIZED_SCOPE_RAR_TYPE);
            representation.setCustomData("scope_parameter", intermediaryScopeRepresentation.getParameter());
        }
        return new AuthorizationDetails(intermediaryScopeRepresentation.getScope(), AuthorizationRequestSource.SCOPE, representation);
    }

    /**
     * Gets one of the requested OAuth scopes and obtains the list of all the optional client scope models for the current client and searches whether
     * there is a match.
     * Parameterized scopes are matching using the registered Regexp, while static scopes are matched by name.
     * It returns an Optional of a {@link IntermediaryScopeRepresentation} with either a static scope data, a parameterized scope data or an empty Optional
     * if there was no match for the regexp.
     *
     * @param requestScope one of the requested OAuth scopes
     * @return see description
     */
    private Optional<IntermediaryScopeRepresentation> getMatchingClientScope(String requestScope, Collection<ClientScopeModel> optionalScopes) {
        for (ClientScopeModel clientScopeModel : optionalScopes) {
            if (clientScopeModel.isParameterizedScope()) {
                String paramValue = clientScopeModel.getParameterFromScope(requestScope).orElse(null);
                if (paramValue == null) {
                    continue;
                }
                try {
                    resolveType(clientScopeModel).validateParameter(clientScopeModel, paramValue);
                } catch (InvalidScopeParameterException e) {
                    logger.warnf("Invalid scope parameter for '%s': %s", requestScope, e.getMessage());
                    return Optional.empty();
                }
                return Optional.of(new IntermediaryScopeRepresentation(clientScopeModel, paramValue, requestScope));
            } else {
                if (requestScope.equalsIgnoreCase(clientScopeModel.getName())) {
                    return Optional.of(new IntermediaryScopeRepresentation(clientScopeModel));
                }
            }
        }
        return Optional.empty();
    }

    private ParameterizedScopeTypeProvider resolveType(ClientScopeModel clientScopeModel) {
        String typeId = clientScopeModel.getAttribute(ClientScopeModel.PARAMETERIZED_SCOPE_TYPE);
        if (StringUtil.isNullOrEmpty(typeId)) {
            logger.warnf("Parameterized scope '%s' has no type set, defaulting to '%s'", clientScopeModel.getName(), StringScopeType.TYPE);
            typeId = StringScopeType.TYPE;
        }
        ParameterizedScopeTypeProvider provider = session.getProvider(ParameterizedScopeTypeProvider.class, typeId);
        if (provider == null) {
            throw new IllegalStateException("Unknown parameterized scope type: " + typeId);
        }
        return provider;
    }

    @Override
    public void close() {

    }
}
