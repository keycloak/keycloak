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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.rar.AuthorizationRequestParserProvider;
import org.keycloak.protocol.oidc.rar.model.IntermediaryScopeRepresentation;
import org.keycloak.rar.AuthorizationDetails;
import org.keycloak.rar.AuthorizationRequestContext;
import org.keycloak.rar.AuthorizationRequestSource;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;

import org.jboss.logging.Logger;

import static org.keycloak.representations.AuthorizationDetailsJSONRepresentation.DYNAMIC_SCOPE_RAR_TYPE;
import static org.keycloak.representations.AuthorizationDetailsJSONRepresentation.STATIC_SCOPE_RAR_TYPE;

/**
 * @author <a href="mailto:dgozalob@redhat.com">Daniel Gozalo</a>
 */
public class ClientScopeAuthorizationRequestParser implements AuthorizationRequestParserProvider {

    protected static final Logger logger = Logger.getLogger(ClientScopeAuthorizationRequestParser.class);

    /**
     * This parser will be created on a per-request basis. When the adapter is created, the request's client is passed
     * as a parameter
     */
    private final ClientModel client;

    public ClientScopeAuthorizationRequestParser(ClientModel client) {
        this.client = client;
    }

    /**
     * Creates a {@link AuthorizationRequestContext} with a list of {@link AuthorizationDetails} that will be parsed from
     * the provided OAuth scopes that have been requested in a given Auth request, together with default client scopes.
     * <p>
     * Dynamic scopes will also be parsed with the extracted parameter, so it can be used later
     *
     * @param scopeParam the OAuth scope param for the current request
     * @return see description
     */
    @Override
    public AuthorizationRequestContext parseScopes(String scopeParam) {
        // Process all the default ClientScopeModels for the current client, and maps them to the DynamicScopeRepresentation to make use of a HashSet
        Set<IntermediaryScopeRepresentation> clientScopeModelSet = this.client.getClientScopes(true).values().stream()
                .filter(clientScopeModel -> !clientScopeModel.isDynamicScope()) // not strictly needed as Dynamic Scopes are going to be Optional scopes for now
                .map(IntermediaryScopeRepresentation::new)
                .collect(Collectors.toSet());

        Set<IntermediaryScopeRepresentation> intermediaryScopeRepresentations = new HashSet<>();
        if (scopeParam != null) {
            // Go through the parsed requested scopes and attempt to match them against the optional scopes list
            intermediaryScopeRepresentations = TokenManager.parseScopeParameter(scopeParam).collect(Collectors.toSet()).stream()
                    .map((String requestScope) -> getMatchingClientScope(requestScope, this.client.getClientScopes(false).values()))
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
        if (intermediaryScopeRepresentation.isDynamic() && intermediaryScopeRepresentation.getParameter() != null) {
            representation.setType(DYNAMIC_SCOPE_RAR_TYPE);
            representation.setCustomData("scope_parameter", intermediaryScopeRepresentation.getParameter());
        }
        return new AuthorizationDetails(intermediaryScopeRepresentation.getScope(), AuthorizationRequestSource.SCOPE, representation);
    }

    /**
     * Gets one of the requested OAuth scopes and obtains the list of all the optional client scope models for the current client and searches whether
     * there is a match.
     * Dynamic scopes are matching using the registered Regexp, while static scopes are matched by name.
     * It returns an Optional of a {@link IntermediaryScopeRepresentation} with either a static scope datra, a dynamic scope data or an empty Optional
     * if there was no match for the regexp.
     *
     * @param requestScope one of the requested OAuth scopes
     * @return see description
     */
    private Optional<IntermediaryScopeRepresentation> getMatchingClientScope(String requestScope, Collection<ClientScopeModel> optionalScopes) {
        for (ClientScopeModel clientScopeModel : optionalScopes) {
            if (clientScopeModel.isDynamicScope()) {
                // The regexp has been stored without a capture group to simplify how it's shown to the user, need to transform it now
                // to capture the parameter value
                Pattern p = Pattern.compile(clientScopeModel.getDynamicScopeRegexp().replace("*", "(.*)"));
                Matcher m = p.matcher(requestScope);
                if (m.matches()) {
                    return Optional.of(new IntermediaryScopeRepresentation(clientScopeModel, m.group(1), requestScope));
                }
            } else {
                if (requestScope.equalsIgnoreCase(clientScopeModel.getName())) {
                    return Optional.of(new IntermediaryScopeRepresentation(clientScopeModel));
                }
            }
        }
        // Nothing matched, returning an empty Optional to avoid working with Nulls
        return Optional.empty();
    }

    @Override
    public void close() {

    }
}
