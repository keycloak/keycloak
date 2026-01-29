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

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;
import org.keycloak.representations.idm.MembershipType;

/**
 * A {@link Provider} that manages organization and its data within the scope of a realm.
 */
public interface OrganizationProvider extends Provider {

    /**
     * Creates a new organization with given {@code name} and {@code alias} to the realm.
     * The internal ID of the organization will be created automatically.
     * @param name the name of the organization.
     * @param alias the alias of the organization. If not set, defaults to the value set to {@code name}. Once set, the alias is immutable.
     * @throws ModelDuplicateException If there is already an organization with the given name or alias
     * @return Model of the created organization.
     */
    default OrganizationModel create(String name, String alias) {
        return create(null, name, alias);
    }

    /**
     * Creates a new organization with given {@code id}, {@code name}, and {@code alias} to the realm
     * @param id the id of the organization.
     * @param name the name of the organization.
     * @param alias the alias of the organization. If not set, defaults to the value set to {@code name}. Once set, the alias is immutable.
     * @throws ModelDuplicateException If there is already an organization with the given name or alias
     * @return Model of the created organization.
     */
    OrganizationModel create(String id, String name, String alias);

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
     * Returns all organizations in the realm filtered according to the specified parameters.
     *
     * @param search a {@code String} representing either an organization name or domain.
     * @param exact if {@code true}, the organizations will be searched using exact match for the {@code search} param - i.e.
     *              either the organization name or one of its domains must match exactly the {@code search} param. If false,
     *              the method returns all organizations whose name or (domains) partially match the {@code search} param.
     * @param first the position of the first result to be processed (pagination offset). Ignored if negative or {@code null}.
     * @param max the maximum number of results to be returned. Ignored if negative or {@code null}.
     * @return a {@link Stream} of the matched organizations. Never returns {@code null}.
     */
    Stream<OrganizationModel> getAllStream(String search, Boolean exact, Integer first, Integer max);

    /**
     * Returns all organizations in the realm filtered according to the specified parameters.
     *
     * @param attributes a {@code Map} containing the attributes (name/value) that must match organization attributes.
     * @param first the position of the first result to be processed (pagination offset). Ignored if negative or {@code null}.
     * @param max the maximum number of results to be returned. Ignored if negative or {@code null}.
     * @return a {@link Stream} of the matched organizations. Never returns {@code null}.
     */
    Stream<OrganizationModel> getAllStream(Map<String, String> attributes, Integer first, Integer max);

    /**
     * Returns the number of organizations in the realm filtered according to the specified parameters.
     *
     * @param search a {@code String} representing either an organization name or domain.
     * @param exact if {@code true}, the organizations will be searched using exact match for the {@code search} param - i.e.
     *              either the organization name or one of its domains must match exactly the {@code search} param. If false,
     *              the method returns all organizations whose name or (domains) partially match the {@code search} param.
     * @return the number matched organizations.
     */
    default long count(String search, Boolean exact) {
        return getAllStream(search, exact, null, null).count();
    }

    /**
     * Returns the number of organizations in the realm filtered according to the specified parameters.
     *
     * @param attributes a {@code Map} containing the attributes (name/value) that must match organization attributes.
     * @return the number matched organizations.
     */
    default long count(Map<String, String> attributes) {
        return getAllStream(attributes, null, null).count();
    }

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
     * Adds the given {@link UserModel} as a managed member of the given {@link OrganizationModel}.
     *
     * @param organization the organization
     * @param user the user
     * @throws ModelException if the {@link UserModel} is member of different organization
     * @return {@code true} if the user was added as a member. Otherwise, returns {@code false}
     */
    boolean addManagedMember(OrganizationModel organization, UserModel user);

    /**
     * Adds the given {@link UserModel} as an unmanaged member of the given {@link OrganizationModel}.
     *
     * @param organization the organization
     * @param user the user
     * @throws ModelException if the {@link UserModel} is member of different organization
     * @return {@code true} if the user was added as a member. Otherwise, returns {@code false}
     */
    boolean addMember(OrganizationModel organization, UserModel user);

    /**
     * Returns the members of a given {@link OrganizationModel} filtered according to the specified parameters.
     *
     * @param organization the organization
     * @return Stream of the members. Never returns {@code null}.
     * @deprecated Use {@link #getMembersStream(OrganizationModel, Map, Boolean, Integer, Integer)} instead.
     */
    @Deprecated(forRemoval = true, since = "26")
    Stream<UserModel> getMembersStream(OrganizationModel organization, String search, Boolean exact, Integer first, Integer max);

    /**
     * Returns the members of a given {@link OrganizationModel} filtered according to the specified {@code filters}.
     *
     * @param organization the organization
     * @return Stream of the members. Never returns {@code null}.
     */
    default Stream<UserModel> getMembersStream(OrganizationModel organization, Map<String, String> filters, Boolean exact, Integer first, Integer max) {
        var result = getMembersStream(organization, Optional.ofNullable(filters).orElse(Map.of()).get(UserModel.SEARCH), exact, first, max);
        var membershipType = Optional.ofNullable(filters.get(MembershipType.NAME)).map(MembershipType::valueOf).orElse(null);

        if (membershipType != null) {
            return result.filter(userModel -> MembershipType.MANAGED.equals(membershipType) ? isManagedMember(organization, userModel) : !isManagedMember(organization, userModel));
        }

        return result;
    }

    /**
     * Returns number of members in the organization.
     * @param organization the organization
     * @return Number of members in the organization.
     */
    long getMembersCount(OrganizationModel organization);

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
     * @param member the member of an organization
     * @return the organizations the {@code member} belongs to or an empty stream if the user doesn't belong to any.
     */
    Stream<OrganizationModel> getByMember(UserModel member);

    /**
     * Creates a new group within the given {@link OrganizationModel}.
     * The internal ID of the group will be created automatically.
     * The created group will be of type {@link org.keycloak.models.GroupModel.Type#ORGANIZATION}.
     * If {@code toParent} is {@code null}, the group will be created as a top-level organization group,
     * as a direct child of the organization's internal group structure.
     * If {@code toParent} is provided, the group will be created as a subgroup of the specified parent.
     *
     * @param organization the organization to create the group in
     * @param name the name of the group to create
     * @param toParent the parent group under which to create the new group. If {@code null},
     *                 the group is created as a top-level organization group. If provided, must be
     *                 an organization group (type {@link org.keycloak.models.GroupModel.Type#ORGANIZATION})
     *                 belonging to the same organization.
     * @return the newly created {@link GroupModel}
     * @throws ModelException if {@code organization} or {@code name} is {@code null}
     * @throws ModelValidationException if {@code toParent} is not an organization group or does not
     *                                  belong to the specified organization
     */
    default GroupModel createGroup(OrganizationModel organization, String name, GroupModel toParent) {
        return createGroup(organization, null, name, toParent);
    }

    /**
     * Creates a new group with the given {@code id} within the given {@link OrganizationModel}.
     * The created group will be of type {@link org.keycloak.models.GroupModel.Type#ORGANIZATION}.
     * If {@code toParent} is {@code null}, the group will be created as a top-level organization group,
     * as a direct child of the organization's internal group structure.
     * If {@code toParent} is provided, the group will be created as a subgroup of the specified parent.
     *
     * @param organization the organization to create the group in
     * @param id the id of the group. If {@code null}, an id will be generated automatically.
     * @param name the name of the group to create
     * @param toParent the parent group under which to create the new group. If {@code null},
     *                 the group is created as a top-level organization group. If provided, must be
     *                 an organization group (type {@link org.keycloak.models.GroupModel.Type#ORGANIZATION})
     *                 belonging to the same organization.
     * @return the newly created {@link GroupModel}
     * @throws ModelException if {@code organization} or {@code name} is {@code null}
     * @throws ModelValidationException if {@code toParent} is not an organization group or does not
     *                                  belong to the specified organization
     */
    GroupModel createGroup(OrganizationModel organization, String id, String name, GroupModel toParent);

    /**
     * Returns the top-level groups of the given {@link OrganizationModel}.
     *
     * @param organization the organization
     * @param firstResult the position of the first result to be processed (pagination offset). Ignored if negative or {@code null}.
     * @param maxResults the maximum number of results to be returned. Ignored if negative or {@code null}.
     * @return Stream of top-level groups in the organization. Never returns {@code null}.
     */
    Stream<GroupModel> getTopLevelGroups(OrganizationModel organization, Integer firstResult, Integer maxResults);

    /**
     * Returns groups of the given {@link OrganizationModel} filtered by group name.
     *
     * @param organization the organization
     * @param search the string to search for in group names. Case-sensitive.
     * @param exact if {@code true}, the groups will be searched using exact match. If {@code false}, partial match is used.
     * @param firstResult the position of the first result to be processed (pagination offset). Ignored if negative or {@code null}.
     * @param maxResults the maximum number of results to be returned. Ignored if negative or {@code null}.
     * @return Stream of groups matching the search criteria. Never returns {@code null}.
     */
    Stream<GroupModel> searchGroupsByName(OrganizationModel organization, String search, Boolean exact, Integer firstResult, Integer maxResults);

    /**
     * Returns groups of the given {@link OrganizationModel} filtered by group attributes.
     *
     * @param organization the organization
     * @param attributes a {@code Map} containing the attributes (name/value) that must match group attributes.
     * @param firstResult the position of the first result to be processed (pagination offset). Ignored if negative or {@code null}.
     * @param maxResults the maximum number of results to be returned. Ignored if negative or {@code null}.
     * @return Stream of groups matching the attribute criteria. Never returns {@code null}.
     */
    Stream<GroupModel> searchGroupsByAttributes(OrganizationModel organization, Map<String, String> attributes, Integer firstResult, Integer maxResults);

    /**
     * Returns all organization groups that the given {@code member} explicitly belongs to within the given {@code organization}.
     * Only returns groups of type {@link org.keycloak.models.GroupModel.Type#ORGANIZATION} that belong to the specified organization.
     * Membership is explicit - being a member of a child group does not imply membership in parent groups.
     *
     * @param organization the organization whose groups to check
     * @param member the user whose group memberships to retrieve
     * @return Stream of organization groups the member belongs to. Never returns {@code null}.
     */
    Stream<GroupModel> getOrganizationGroupsByMember(OrganizationModel organization, UserModel member);

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
     * @return Stream of the identity providers associated with the given {@code organization}. Never returns {@code null}.
     */
    Stream<IdentityProviderModel> getIdentityProviders(OrganizationModel organization);

    /**
     * Removes the link between the given {@link OrganizationModel} and the identity provider associated with it if such a link exists.
     *
     * @param organization the organization
     * @param identityProvider the identity provider
     * @return {@code true} if the link was removed, {@code false} otherwise
     */
    boolean removeIdentityProvider(OrganizationModel organization, IdentityProviderModel identityProvider);

    /**
     * Indicates if the current realm supports organization.
     *
     * @return {@code true} if organization is supported. Otherwise, returns {@code false}
     */
    boolean isEnabled();

    /**
     * <p>Indicates if the given {@code member} is managed by the organization.
     *
     * <p>A member is managed by the organization whenever the member cannot exist without the organization they belong
     * to so that their lifecycle is bound to the organization lifecycle. For instance, when a member is federated from
     * the identity provider associated with an organization, there is a strong relationship between the member identity
     * and the organization.
     *
     * <p>On the other hand, existing realm users whose identities are not intrinsically linked to an organization but
     * are eventually joining an organization are not managed by the organization. They have a lifecycle that does not
     * depend on the organization they are linked to.
     *
     * @param organization the organization
     * @param member the member
     * @return {@code true} if the {@code member} is managed by the given {@code organization}. Otherwise, returns {@code false}
     */
    boolean isManagedMember(OrganizationModel organization, UserModel member);

    /**
     * Indicates if the given {@code user} is a member of the given {@code organization}.
     *
     * @param organization the organization
     * @param user the member
     * @return {@code true} if the user is a member. Otherwise, {@code false}
     */
    default boolean isMember(OrganizationModel organization, UserModel user) {
        return getMemberById(organization, user.getId()) != null;
    }

    /**
     * <p>Removes a member from the organization.
     *
     * <p>This method can either remove the given {@code member} entirely from the realm (and the organization) or only
     * remove the link to the {@code organization} so that the user still exists but is no longer a member of the organization.
     * The decision to remove the user entirely or only the link depends on whether the user is managed by the organization or not, respectively.
     *
     * @param organization the organization
     * @param member the member
     * @return {@code true} if the given {@code member} is a member and was successfully removed from the organization. Otherwise, returns {@code false}
     */
    boolean removeMember(OrganizationModel organization, UserModel member);

    /**
     * Returns number of organizations in the realm.
     * @return long Number of organizations
     */
    long count();

    /**
     * Returns an {@link OrganizationModel} with the given {@code alias}.
     *
     * @param alias the alias
     * @return the organization
     */
    default OrganizationModel getByAlias(String alias) {
        return getAllStream(Map.of(OrganizationModel.ALIAS, alias), 0, 1).findAny().orElse(null);
    }

    /**
     * Returns a {@link InvitationManager} for managing invitations
     *
     * @return the invitation manager
     */
    InvitationManager getInvitationManager();
}
