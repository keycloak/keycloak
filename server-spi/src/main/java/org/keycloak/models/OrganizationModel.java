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

public interface OrganizationModel extends RoleContainerModel {

    String ORGANIZATION_ATTRIBUTE = "kc.org";
    String ORGANIZATION_SWITCHABLE_ATTRIBUTE = "kc.org.switchable";
    String ORGANIZATION_NAME_ATTRIBUTE = "kc.org.name";
    String ORGANIZATION_DOMAIN_ATTRIBUTE = "kc.org.domain";
    String ORGANIZATION_EXCLUDED_DOMAIN_ATTRIBUTE = "kc.org.excluded.domains";
    String ALIAS = "alias";
    String HIDE_IDP_ON_LOGIN_WHEN_ORGANIZATION_UNKNOWN = "kc.org.broker.login.hide-when-org-unknown";
    String SHOW_IDP_ON_LOGIN_WHEN_LINKED_ELSEWHERE = "kc.org.broker.login.show-when-linked-elsewhere";

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

    interface OrganizationRemovedEvent extends ProviderEvent {
        OrganizationModel getOrganization();
        KeycloakSession getKeycloakSession();

        static void fire(OrganizationModel organization, KeycloakSession session) {
            session.getKeycloakSessionFactory().publish(new OrganizationRemovedEvent() {
                @Override
                public OrganizationModel getOrganization() {
                    return organization;
                }

                @Override
                public KeycloakSession getKeycloakSession() {
                    return session;
                }
            });
        }
    }

    /**
     * Returns the realm that owns this organization.
     *
     * @return the owning realm
     */
    default RealmModel getRealm() {
        throw new UnsupportedOperationException("Organization realm lookup is not supported by this provider");
    }

    /**
     * Returns the default role assigned to new members of this organization.
     *
     * @return the default organization role
     */
    default RoleModel getDefaultRole() {
        throw new UnsupportedOperationException("Organization roles are not supported by this provider");
    }

    /**
     * Sets the default role assigned to new members of this organization.
     *
     * @param role the organization role to use as the default role
     */
    default void setDefaultRole(RoleModel role) {
        throw new UnsupportedOperationException("Organization roles are not supported by this provider");
    }

    /**
     * Adds a role as a composite of the default organization role.
     *
     * @param role the role to add
     */
    default void addToDefaultRoles(RoleModel role) {
        getDefaultRole().addCompositeRole(role);
    }

    @Override
    default RoleModel getRole(String name) {
        throw new UnsupportedOperationException("Organization roles are not supported by this provider");
    }

    @Override
    default RoleModel addRole(String name) {
        return addRole(null, name);
    }

    @Override
    default RoleModel addRole(String id, String name) {
        throw new UnsupportedOperationException("Organization roles are not supported by this provider");
    }

    @Override
    default boolean removeRole(RoleModel role) {
        throw new UnsupportedOperationException("Organization roles are not supported by this provider");
    }

    @Override
    default Stream<RoleModel> getRolesStream() {
        throw new UnsupportedOperationException("Organization roles are not supported by this provider");
    }

    @Override
    default Stream<RoleModel> getRolesStream(Integer firstResult, Integer maxResults) {
        throw new UnsupportedOperationException("Organization roles are not supported by this provider");
    }

    @Override
    default Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max) {
        throw new UnsupportedOperationException("Organization roles are not supported by this provider");
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
}
