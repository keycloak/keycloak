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

import org.keycloak.provider.ProviderEvent;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Represents a role within an organization context. Organization roles are similar to realm roles
 * but are scoped to a specific organization, allowing fine-grained access control for organization members.
 */
public interface OrganizationRoleModel {

    interface OrganizationRoleEvent extends ProviderEvent {
        OrganizationModel getOrganization();
        OrganizationRoleModel getRole();
        KeycloakSession getKeycloakSession();
    }

    interface OrganizationRoleCreatedEvent extends OrganizationRoleEvent {
        static void fire(OrganizationModel organization, OrganizationRoleModel role, KeycloakSession session) {
            session.getKeycloakSessionFactory().publish(new OrganizationRoleCreatedEvent() {
                @Override
                public OrganizationModel getOrganization() {
                    return organization;
                }

                @Override
                public OrganizationRoleModel getRole() {
                    return role;
                }

                @Override
                public KeycloakSession getKeycloakSession() {
                    return session;
                }
            });
        }
    }

    interface OrganizationRoleRemovedEvent extends OrganizationRoleEvent {
        static void fire(OrganizationModel organization, OrganizationRoleModel role, KeycloakSession session) {
            session.getKeycloakSessionFactory().publish(new OrganizationRoleRemovedEvent() {
                @Override
                public OrganizationModel getOrganization() {
                    return organization;
                }

                @Override
                public OrganizationRoleModel getRole() {
                    return role;
                }

                @Override
                public KeycloakSession getKeycloakSession() {
                    return session;
                }
            });
        }
    }

    /**
     * Returns the unique identifier for this organization role.
     *
     * @return the role ID
     */
    String getId();

    /**
     * Returns the name of this organization role.
     *
     * @return the role name
     */
    String getName();

    /**
     * Sets the name of this organization role.
     *
     * @param name the role name
     */
    void setName(String name);

    /**
     * Returns the description of this organization role.
     *
     * @return the role description
     */
    String getDescription();

    /**
     * Sets the description of this organization role.
     *
     * @param description the role description
     */
    void setDescription(String description);

    /**
     * Returns the organization that owns this role.
     *
     * @return the organization
     */
    OrganizationModel getOrganization();

    /**
     * Checks if this is a composite role (contains other roles).
     *
     * @return true if composite, false otherwise
     */
    boolean isComposite();

    /**
     * Adds a composite role to this organization role.
     * Can add realm roles, client roles, or other organization roles.
     *
     * @param role the role to add as composite
     */
    void addCompositeRole(RoleModel role);

    /**
     * Adds another organization role as a composite to this role.
     *
     * @param role the organization role to add as composite
     */
    void addCompositeRole(OrganizationRoleModel role);

    /**
     * Removes a composite role from this organization role.
     *
     * @param role the role to remove from composites
     */
    void removeCompositeRole(RoleModel role);

    /**
     * Removes an organization role from composites.
     *
     * @param role the organization role to remove from composites
     */
    void removeCompositeRole(OrganizationRoleModel role);

    /**
     * Returns all composite roles as a stream.
     *
     * @return Stream of composite roles. Never returns null.
     */
    Stream<RoleModel> getCompositesStream();

    /**
     * Returns composite organization roles as a stream.
     *
     * @return Stream of composite organization roles. Never returns null.
     */
    Stream<OrganizationRoleModel> getCompositeOrganizationRolesStream();

    /**
     * Checks if this role has the specified role as a composite.
     *
     * @param role the role to check
     * @return true if the role is a composite of this role
     */
    boolean hasRole(RoleModel role);

    /**
     * Checks if this role has the specified organization role as a composite.
     *
     * @param role the organization role to check
     * @return true if the organization role is a composite of this role
     */
    boolean hasRole(OrganizationRoleModel role);

    /**
     * Sets a single attribute for this organization role.
     *
     * @param name the attribute name
     * @param value the attribute value
     */
    void setSingleAttribute(String name, String value);

    /**
     * Sets multiple values for an attribute of this organization role.
     *
     * @param name the attribute name
     * @param values the attribute values
     */
    void setAttribute(String name, List<String> values);

    /**
     * Removes an attribute from this organization role.
     *
     * @param name the attribute name to remove
     */
    void removeAttribute(String name);

    /**
     * Returns the first value of the specified attribute.
     *
     * @param name the attribute name
     * @return the first attribute value or null if not found
     */
    default String getFirstAttribute(String name) {
        return getAttributeStream(name).findFirst().orElse(null);
    }

    /**
     * Returns all values for the specified attribute as a stream.
     *
     * @param name the attribute name
     * @return Stream of attribute values. Never returns null.
     */
    Stream<String> getAttributeStream(String name);

    /**
     * Returns all attributes of this organization role.
     *
     * @return Map of all attributes
     */
    Map<String, List<String>> getAttributes();
}
