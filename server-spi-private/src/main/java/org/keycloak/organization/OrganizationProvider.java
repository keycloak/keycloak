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
package org.keycloak.organization;

import java.util.Set;
import java.util.stream.Stream;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

/**
 * A {@link Provider} that manages organization and its data within the scope of a realm.
 */
public interface OrganizationProvider extends Provider {

    /**
     * Creates a new organization with given {@code name} to the realm.
     * The internal ID of the organization will be created automatically.
     * @param name String name of the organization.
     * @param domains the domains
     * @throws ModelDuplicateException If there is already an organization with the given name
     * @return Model of the created organization.
     */
    OrganizationModel create(String name, Set<String> domains);

    /**
     * Returns a {@link OrganizationModel} by its {@code id};
     *
     * @param id the id of an organization
     * @return the organization with the given {@code id} or {@code null} if there is no such an organization.
     */
    OrganizationModel getById(String id);

    /**
     * Returns a {@link OrganizationModel} by its internet domain.
     *
     * @param domainName the organization's internet domain (e.g. redhat.com)
     * @return the organization that is linked to the given internet domain
     */
    OrganizationModel getByDomainName(String domainName);

    /**
     * Returns all organizations in the realm.
     *
     * @return a {@link Stream} of the realm's organizations.
     */
    default Stream<OrganizationModel> getAllStream() {
        return getAllStream("", null, null, null);
    }

    /**
     * Returns the organizations in the realm using the specified filters.
     *
     * @param search a {@code String} representing either an organization name or domain.
     * @param exact if {@code true}, the organizations will be searched using exact match for the {@code search} param - i.e.
     *              either the organization name or one of its domains must match exactly the {@code search} param. If false,
     *              the method returns all organizations whose name or (domains) partially match the {@code search} param.
     * @param first index of the first element (pagination offset).
     * @param max the maximum number of results.
     * @return a {@link Stream} of the matched organizations. Never returns {@code null}.
     */
    Stream<OrganizationModel> getAllStream(String search, Boolean exact, Integer first, Integer max);

    /**
     * Removes the given organization from the realm together with the data associated with it, e.g. its members etc.
     *
     * @param organization Organization to be removed.
     * @throws ModelException if the organization doesn't exist or doesn't belong to the realm.
     * @return {@code true} if the organization was removed, {@code false} otherwise
     */
    boolean remove(OrganizationModel organization);

    /**
     * Removes all organizations from the realm.
     */
    void removeAll();

    /**
     * Adds the given {@link UserModel} as a member of the given {@link OrganizationModel}.
     *
     * @param organization the organization
     * @param user the user
     * @throws ModelException if the {@link UserModel} is member of different organization
     * @return {@code true} if the user was added as a member. Otherwise, returns {@code false}
     */
    boolean addMember(OrganizationModel organization, UserModel user);

    /**
     * Returns the members of a given {@link OrganizationModel}.
     *
     * @param organization the organization
     * @return Stream of the members. Never returns {@code null}.
     */
    Stream<UserModel> getMembersStream(OrganizationModel organization);

    /**
     * Returns the member of the {@link OrganizationModel} by its {@code id}.
     *
     * @param organization the organization
     * @param id the member id
     * @return the member of the {@link OrganizationModel} with the given {@code id}
     */
    UserModel getMemberById(OrganizationModel organization, String id);

    /**
     * Returns the {@link OrganizationModel} that the {@code member} belongs to.
     *
     * @param member the member of a organization
     * @return the organization the {@code member} belongs to or {@code null} if the user doesn't belong to any.
     */
    OrganizationModel getByMember(UserModel member);

    /**
     * Associate the given {@link IdentityProviderModel} with the given {@link OrganizationModel}.
     * 
     * @param organization the organization
     * @param identityProvider the identityProvider
     * @return {@code true} if the identityProvider was associated with the organization. Otherwise, returns {@code false}
     */
    boolean addIdentityProvider(OrganizationModel organization, IdentityProviderModel identityProvider);

    /**
     * @param organization the organization
     * @return The identityProvider associated with a given {@code organization} or {@code null} if there is none.
     */
    IdentityProviderModel getIdentityProvider(OrganizationModel organization);

    /**
     * Removes the link between the given {@link OrganizationModel} and identity provider associated with it if such a link exists.
     * 
     * @param organization the organization
     * @return {@code true} if the link was removed, {@code false} otherwise
     */
    boolean removeIdentityProvider(OrganizationModel organization);

    /**
     * Indicates if the current realm supports organization.
     *
     * @return {@code true} if organization is supported. Otherwise, returns {@code false}
     */
    boolean isEnabled();
}
