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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.provider.ProviderEvent;

public interface OrganizationModel {

    String ORGANIZATION_ATTRIBUTE = "kc.org";
    String ORGANIZATION_NAME_ATTRIBUTE = "kc.org.name";
    String ORGANIZATION_DOMAIN_ATTRIBUTE = "kc.org.domain";
    String ALIAS = "alias";

    enum IdentityProviderRedirectMode {
        EMAIL_MATCH("kc.org.broker.redirect.mode.email-matches");

        private final String key;

        IdentityProviderRedirectMode(String key) {
            this.key = key;
        }

        public boolean isSet(IdentityProviderModel broker) {
            return Boolean.parseBoolean(broker.getConfig().get(key));
        }

        public String getKey() {
            return key;
        }
    }

    interface OrganizationMembershipEvent extends ProviderEvent {
        OrganizationModel getOrganization();
        UserModel getUser();
        KeycloakSession getSession();
    }

    interface OrganizationMemberJoinEvent extends OrganizationMembershipEvent {
        static void fire(OrganizationModel organization, UserModel user, KeycloakSession session) {
            session.getKeycloakSessionFactory().publish(new OrganizationModel.OrganizationMemberJoinEvent() {
                @Override
                public UserModel getUser() {
                    return user;
                }

                @Override
                public OrganizationModel getOrganization() {
                    return organization;
                }

                @Override
                public KeycloakSession getSession() {
                    return session;
                }
            });
        }
    }

    interface OrganizationMemberLeaveEvent extends OrganizationMembershipEvent {
        static void fire(OrganizationModel organization, UserModel user, KeycloakSession session) {
            session.getKeycloakSessionFactory().publish(new OrganizationModel.OrganizationMemberLeaveEvent() {
                @Override
                public UserModel getUser() {
                    return user;
                }

                @Override
                public OrganizationModel getOrganization() {
                    return organization;
                }

                @Override
                public KeycloakSession getSession() {
                    return session;
                }
            });
        }
    }

    String getId();

    void setName(String name);

    String getName();

    String getAlias();

    void setAlias(String alias);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    String getDescription();

    void setDescription(String description);

    String getRedirectUrl();

    void setRedirectUrl(String redirectUrl);

    Map<String, List<String>> getAttributes();

    void setAttributes(Map<String, List<String>> attributes);

    Stream<OrganizationDomainModel> getDomains();

    void setDomains(Set<OrganizationDomainModel> domains);

    Stream<IdentityProviderModel> getIdentityProviders();

    boolean isManaged(UserModel user);

    boolean isMember(UserModel user);

    // Organization Role Management

    /**
     * Creates a new organization role.
     *
     * @param name the role name
     * @return the created organization role
     */
    OrganizationRoleModel addRole(String name);

    /**
     * Creates a new organization role with the specified ID and name.
     *
     * @param id the role ID
     * @param name the role name
     * @return the created organization role
     */
    OrganizationRoleModel addRole(String id, String name);

    /**
     * Retrieves an organization role by name.
     *
     * @param name the role name
     * @return the organization role or null if not found
     */
    OrganizationRoleModel getRole(String name);

    /**
     * Retrieves an organization role by ID.
     *
     * @param id the role ID
     * @return the organization role or null if not found
     */
    OrganizationRoleModel getRoleById(String id);

    /**
     * Removes an organization role.
     *
     * @param role the role to remove
     * @return true if the role was removed, false otherwise
     */
    boolean removeRole(OrganizationRoleModel role);

    /**
     * Returns all organization roles as a stream.
     *
     * @return Stream of organization roles. Never returns null.
     */
    Stream<OrganizationRoleModel> getRolesStream();

    /**
     * Returns organization roles with pagination.
     *
     * @param firstResult the index of the first result
     * @param maxResults the maximum number of results
     * @return Stream of organization roles. Never returns null.
     */
    Stream<OrganizationRoleModel> getRolesStream(Integer firstResult, Integer maxResults);

    /**
     * Searches for organization roles by name.
     *
     * @param search the search term
     * @param first the index of the first result
     * @param max the maximum number of results
     * @return Stream of matching organization roles. Never returns null.
     */
    Stream<OrganizationRoleModel> searchForRolesStream(String search, Integer first, Integer max);

    /**
     * Grants an organization role to a user.
     *
     * @param user the user to grant the role to
     * @param role the organization role to grant
     */
    void grantRole(UserModel user, OrganizationRoleModel role);

    /**
     * Revokes an organization role from a user.
     *
     * @param user the user to revoke the role from
     * @param role the organization role to revoke
     */
    void revokeRole(UserModel user, OrganizationRoleModel role);

    /**
     * Returns organization roles granted to a user as a stream.
     *
     * @param user the user
     * @return Stream of organization roles granted to the user. Never returns null.
     */
    Stream<OrganizationRoleModel> getUserRolesStream(UserModel user);

    /**
     * Checks if a user has the specified organization role.
     *
     * @param user the user
     * @param role the organization role to check
     * @return true if the user has the role, false otherwise
     */
    boolean hasRole(UserModel user, OrganizationRoleModel role);

    /**
     * Returns users who have been granted a specific organization role.
     *
     * @param role the organization role
     * @return Stream of users who have the role. Never returns null.
     */
    Stream<UserModel> getRoleMembersStream(OrganizationRoleModel role);

    /**
     * Returns users who have been granted a specific organization role with pagination.
     *
     * @param role the organization role
     * @param firstResult the index of the first result
     * @param maxResults the maximum number of results
     * @return Stream of users who have the role. Never returns null.
     */
    Stream<UserModel> getRoleMembersStream(OrganizationRoleModel role, Integer firstResult, Integer maxResults);
}
