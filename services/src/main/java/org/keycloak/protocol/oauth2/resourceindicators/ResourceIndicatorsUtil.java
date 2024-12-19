/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oauth2.resourceindicators;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Helpers for RFC 8707 OAuth2 Resource Indicators support.
 *
 * @link <a href="https://www.rfc-editor.org/rfc/rfc8707">RFC 8707 OAuth2 Resource Indicators</a>
 */
public class ResourceIndicatorsUtil {

    private static final Logger LOG = Logger.getLogger(CheckedResourceIndicators.class);

    /**
     * clients can use the OAUTH_2_RESOURCE_INDICATORS_PROVIDER_ATTRIBUTE client attribute to customize the resource indicator to use
     */
    public static final String OAUTH_2_RESOURCE_INDICATORS_PROVIDER_ATTRIBUTE = "oauth2.resource-indicators-provider";

    public static final String DEFAULT_PROVIDER_ID = "default";

    /**
     * Encodes the given resource indicators to a string that can be stored in the client session.
     *
     * @param indicators
     * @return
     */
    public static String encodeResourceIndicators(Set<String> indicators) {
        return String.join(Constants.CFG_DELIMITER, indicators);
    }

    /**
     * Decodes the given string from the client session into a Set of resource indicators.
     *
     * @param encodedIndicators
     * @return
     */
    public static Set<String> decodeResourceIndicators(String encodedIndicators) {
        if (encodedIndicators == null) {
            return Collections.emptySet();
        }
        return Set.of(Constants.CFG_DELIMITER_PATTERN.split(encodedIndicators));
    }


    /**
     * Filters the given resource indicators to the supported resource indicators by the given {@link ClientModel client} in the context of the current {@link AuthenticatedClientSessionModel client session}.
     *
     * @param client             the client that provides the resource indicators
     * @param clientSession      may be {@literal null} for the initial request
     * @param resourceIndicatorCandidates the requested resource indicators
     * @return Set of the resource indicators that are supported by the given client.
     */
    public static CheckedResourceIndicators narrowResourceIndicators(KeycloakSession session, ClientModel client,
                                                                     AuthenticatedClientSessionModel clientSession, Set<String> resourceIndicatorCandidates) {

        var provider = resolveProvider(session, client);
        if (provider == null) {
            return CheckedResourceIndicators.EMPTY;
        }

        if (clientSession == null) {
            // This is the initial request, check if client allows the given resource indicator
            Set<String> supported = provider.narrowResourceIndicators(client, resourceIndicatorCandidates);
            return new CheckedResourceIndicators(supported, resourceIndicatorCandidates);
        }

        // this is a subsequent request, check if the given resource indicator was already added to client session
        String encodedClientResourceIndicators = clientSession.getNote(OAuth2Constants.RESOURCE);

        // extract already filtered resource indicators that were stored during initial request
        Set<String> clientResourceIndicators = ResourceIndicatorsUtil.decodeResourceIndicators(encodedClientResourceIndicators);
        return new CheckedResourceIndicators(clientResourceIndicators, resourceIndicatorCandidates);
    }

    private static OAuth2ResourceIndicatorsProvider resolveProvider(KeycloakSession session, ClientModel client) {

        // check if the current client wants to use a custom provider version
        String providerId = client.getAttribute(OAUTH_2_RESOURCE_INDICATORS_PROVIDER_ATTRIBUTE);
        if (providerId == null) {
            // fallback to the default provider
            providerId = DEFAULT_PROVIDER_ID;
        }

        var provider = session.getProvider(OAuth2ResourceIndicatorsProvider.class, providerId);
        if (provider == null) {
            LOG.debug("No resource indicators provider found for providerId: " + providerId);
        }
        return provider;
    }
}
