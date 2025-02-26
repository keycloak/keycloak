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

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.common.Profile;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DefaultOAuth2ResourceIndicatorResolver implements OAuth2ResourceIndicatorResolver {

    /**
     * A resource type to denote OAuth 2.0 Resource Indicator resource definitions.
     */
    public static final String KEYCLOAK_RESOURCE_INDICATOR_TYPE = "urn:keycloak:oauth2:resource-indicator";

    protected final KeycloakSession session;

    public DefaultOAuth2ResourceIndicatorResolver(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public ClientModel findClientByResourceIndicator(String resourceIndicator) {

        if (resourceIndicator == null) {
            return null;
        }

        URI resourceIndicatorUri = URI.create(resourceIndicator);
        if (resourceIndicatorUri.getScheme() == null) {
            // we have a Keycloak specific client resource indicator, e.g. a clientId
            return resolveClientByClientId(resourceIndicator);
        }

        if (!Profile.isFeatureEnabled(Profile.Feature.AUTHORIZATION)) {
            return null;
        }

        return null;
    }

    /**
     * Attempts to resolve a {@link ClientModel} from the given {@code resourceIndicator} interpreted as {@code clientId}.
     * @param resourceIndicator
     * @return
     */
    protected ClientModel resolveClientByClientId(String resourceIndicator) {
        return session.getContext().getRealm().getClientByClientId(resourceIndicator);
    }

    @Override
    public Set<String> narrowResourceIndicators(ClientModel client, Set<String> resourceIndicatorCandidates) {

        Set<String> allowedResources = findAllowedResourceIndicators(client);

        // Only allow explicitly allowed resources on this client.
        Set<String> result = new HashSet<>(resourceIndicatorCandidates);
        result.retainAll(allowedResources);

        return result;
    }

    /**
     * Returns the set of allowed resource indicators for the given {@link ClientModel}.
     * @param client
     * @return
     */
    protected Set<String> findAllowedResourceIndicators(ClientModel client) {

        if (!Profile.isFeatureEnabled(Profile.Feature.AUTHORIZATION)) {
            return Collections.emptySet();
        }

        AuthorizationProvider authzProvider = session.getProvider(AuthorizationProvider.class);
        StoreFactory storeFactory = authzProvider.getStoreFactory();

        ResourceServer resourceServer = lookupResourceServer(client, storeFactory);
        if (resourceServer == null) {
            // no resource server definition found for the given client
            return Collections.emptySet();
        }

        List<Resource> resources = getResources(storeFactory, resourceServer);
        if (CollectionUtil.isEmpty(resources)) {
            // no resource definitions found for the given client / resource server
            return Collections.emptySet();
        }

        Set<String> allowedResources = new HashSet<>();
        for (Resource resource : resources) {
            allowedResources.addAll(resource.getUris());
        }

        return allowedResources;
    }

    protected List<Resource> getResources(StoreFactory storeFactory, ResourceServer resourceServer) {
        return storeFactory.getResourceStore().findByType(resourceServer, KEYCLOAK_RESOURCE_INDICATOR_TYPE);
    }

    protected ResourceServer lookupResourceServer(ClientModel client, StoreFactory storeFactory) {
        return storeFactory.getResourceServerStore().findByClient(client);
    }

}
