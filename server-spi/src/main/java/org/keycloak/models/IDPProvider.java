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
package org.keycloak.models;

import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.provider.Provider;

/**
 * The {@code IDPProvider} is concerned with the storage/retrieval of the configured identity providers in Keycloak. In
 * other words, it is a provider of identity providers (IDPs) and, as such, handles the CRUD operations for IDPs.
 *
 * It is not to be confused with the {@code IdentityProvider} found in server-spi-private as that provider is meant to be
 * implemented by actual identity providers that handle the logic of authenticating users with third party brokers, such
 * as Microsoft, Google, Github, LinkedIn, etc.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public interface IDPProvider extends Provider {

    /**
     * Creates a new identity provider from the specified model.
     *
     * @param model a {@link IdentityProviderModel} containing the identity provider's data.
     * @return the model of the created identity provider.
     */
    IdentityProviderModel create(IdentityProviderModel model);

    /**
     * Updates the identity provider using the specified model.
     *
     * @param model a {@link IdentityProviderModel} containing the identity provider's data.
     */
    void update(IdentityProviderModel model);

    /**
     * Removes the identity provider with the specified alias.
     *
     * @param providerAlias the alias of the identity provider to be removed.
     * @return {@code true} if an IDP with the specified alias was found and removed; {@code false} otherwise.
     */
    boolean remove(String providerAlias);

    /**
     * Removes all identity providers from the realm.
     */
    void removeAll();

    /**
     * Obtains the identity provider with the specified internal id.
     *
     * @param internalId the identity provider's internal id.
     * @return a reference to the identity provider, or {@code null} if no provider is found.
     */
    IdentityProviderModel getById(String internalId);

    /**
     * Obtains the identity provider with the specified alias.
     *
     * @param alias the identity provider's alias.
     * @return a reference to the identity provider, or {@code null} if no provider is found.
     */
    IdentityProviderModel getByAlias(String alias);

    /**
     * Obtains the identity provider whose id or alias match the specified key.
     *
     * @param key a {@code String} representing either the identity provider's id or alias.
     * @return a reference to the identity provider, or {@code null} if no provider is found.
     */
    default IdentityProviderModel getByIdOrAlias(String key) {
        IdentityProviderModel identityProvider = getById(key);
        return identityProvider != null ? identityProvider : getByAlias(key);
    }

    /**
     * Returns all identity providers in the current realm.
     *
     * @return a non-null stream of {@link IdentityProviderModel}s.
     */
    default Stream<IdentityProviderModel> getAllStream() {
        return getAllStream("", null, null);
    }

    /**
     * Returns all identity providers in the realm filtered according to the specified parameters.
     *
     * @param search a {@link String} representing an identity provider alias (partial or exact). If the value is enclosed
     *               in double quotes, the method treats it as an exact search (e.g. {@code "name"}). If the value is enclosed in
     *               wildcards, the method treats it as an infix search (e.g. {@code *name*}). Otherwise, the method treats it as a
     *               prefix search (i.e. {@code name*} and {@code name} return the same results).
     * @param first the position of the first result to be processed (pagination offset). Ignored if negative or {@code null}.
     * @param max the maximum number of results to be returned. Ignored if negative or {@code null}.
     * @return a non-null stream of {@link IdentityProviderModel}s that match the search criteria.
    */
    Stream<IdentityProviderModel> getAllStream(String search, Integer first, Integer max);

    /**
     * Returns all identity providers in the realm filtered according to the specified parameters.
     *
     * @param attrs a {@code Map} containig identity provider config attributes that must be matched.
     * @param first the position of the first result to be processed (pagination offset). Ignored if negative or {@code null}.
     * @param max the maximum number of results to be returned. Ignored if negative or {@code null}.
     * @return a non-null stream of {@link IdentityProviderModel}s that match the search criteria.
     */
    Stream<IdentityProviderModel> getAllStream(Map<String, String> attrs, Integer first, Integer max);

    /**
     * Returns all identity providers associated with the organization with the provided id.
     *
     * @param orgId the id of the organization.
     * @param first the position of the first result to be processed (pagination offset). Ignored if negative or {@code null}.
     * @param max the maximum number of results to be returned. Ignored if negative or {@code null}.
     * @return a non-null stream of {@link IdentityProviderModel}s that match the search criteria.
     */
    default Stream<IdentityProviderModel> getByOrganization(String orgId, Integer first, Integer max) {
        return getAllStream(Map.of(OrganizationModel.ORGANIZATION_ATTRIBUTE, orgId), first, max);
    }


    /**
     * Returns the number of IDPs in the realm.
     *
     * @return the number of IDPs found in the realm.
     */
    long count();

    /**
     * Checks whether the realm has any configured identity providers or not.
     *
     * @return {@code true} if the realm has at least one configured identity provider (federation is enabled); {@code false}
     * otherwise.
     */
    default boolean isIdentityFederationEnabled() {
        return count() > 0;
    }
}
