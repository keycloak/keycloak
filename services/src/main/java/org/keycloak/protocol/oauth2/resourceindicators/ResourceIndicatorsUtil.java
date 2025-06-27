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
package org.keycloak.protocol.oauth2.resourceindicators;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;

import java.net.URI;
import java.util.Collections;
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
     * See: https://datatracker.ietf.org/doc/html/rfc8693#section-2.1
     * @param resourceIndicatorUri
     * @return
     */
    public static boolean isValidResourceIndicatorUri(String resourceIndicatorUri) {
        try {
            var uri = URI.create(resourceIndicatorUri);

            // check for generic URI
            if (uri.getScheme() == null) {
                return false;
            }
            if (uri.getHost() == null) {
                return false;
            }

            // Must be absolute URI
            if (!uri.isAbsolute()) {
                return false;
            }
            // Must NOT include a fragment
            if (uri.getFragment() != null) {
                return false;
            }

            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
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

        if (clientSession == null || clientSession.getNote(OAuth2Constants.RESOURCE) == null) {
            // This is the initial request, check if client allows the given resource indicator
            Set<String> supported = provider.narrowResourceIndicators(client, resourceIndicatorCandidates);
            return new CheckedResourceIndicators(supported, resourceIndicatorCandidates);
        }

        // this is a subsequent request, check if the given resource indicator was already added to client session
        String encodedClientResourceIndicators = clientSession.getNote(OAuth2Constants.RESOURCE);

        // extract already filtered resource indicators that were stored during initial request
        Set<String> initiallyGrantedResourceIndicators = ResourceIndicatorsUtil.decodeResourceIndicators(encodedClientResourceIndicators);
        return new CheckedResourceIndicators(initiallyGrantedResourceIndicators, resourceIndicatorCandidates);
    }

    private static OAuth2ResourceIndicatorResolver resolveProvider(KeycloakSession session, ClientModel client) {

        // check if the current client wants to use a custom provider implementation
        String providerId = client.getAttribute(OAUTH_2_RESOURCE_INDICATORS_PROVIDER_ATTRIBUTE);
        if (providerId == null) {
            // fallback to the default provider
            providerId = DefaultOAuth2ResourceIndicatorResolverFactory.PROVIDER_ID;
        }

        var provider = session.getProvider(OAuth2ResourceIndicatorResolver.class, providerId);
        if (provider == null) {
            LOG.debugf("No resource indicators provider found. providerId=%s", providerId);
            return null;
        }

        LOG.debugf("Found resource indicators provider. providerId=%s", providerId);
        return provider;
    }
}
