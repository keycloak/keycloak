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

package org.keycloak.representations.idm.authorization;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representation of an organization role-based authorization policy.
 * 
 * This policy type allows defining access control based on organization roles
 * that users possess within specific organizations.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationRolePolicyRepresentation extends AbstractPolicyRepresentation {

    @JsonProperty("organizationRoles")
    private Set<OrganizationRoleDefinition> organizationRoles;

    @JsonProperty("fetchRoles")
    private Boolean fetchRoles;

    /**
     * Default constructor.
     */
    public OrganizationRolePolicyRepresentation() {
        setType("organization-role");
    }

    @Override
    public String getType() {
        return "organization-role";
    }

    public Set<OrganizationRoleDefinition> getOrganizationRoles() {
        return organizationRoles;
    }

    public void setOrganizationRoles(Set<OrganizationRoleDefinition> organizationRoles) {
        this.organizationRoles = organizationRoles;
    }

    public void addOrganizationRole(String organizationId, String roleId, boolean required) {
        if (organizationRoles == null) {
            organizationRoles = new HashSet<>();
        }
        organizationRoles.add(new OrganizationRoleDefinition(organizationId, roleId, required));
    }

    public Boolean isFetchRoles() {
        return fetchRoles;
    }

    public void setFetchRoles(Boolean fetchRoles) {
        this.fetchRoles = fetchRoles;
    }

    /**
     * Represents an organization role definition within the policy.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrganizationRoleDefinition {

        @JsonProperty("organizationId")
        private String organizationId;

        @JsonProperty("roleId")
        private String roleId;

        @JsonProperty("required")
        private Boolean required;

        /**
         * Default constructor for JSON deserialization.
         */
        public OrganizationRoleDefinition() {
        }

        /**
         * Constructor for creating organization role definitions.
         *
         * @param organizationId the organization ID
         * @param roleId the role ID within the organization
         * @param required whether this role is required (true) or optional (false)
         */
        public OrganizationRoleDefinition(String organizationId, String roleId, boolean required) {
            this.organizationId = organizationId;
            this.roleId = roleId;
            this.required = required;
        }

        public String getOrganizationId() {
            return organizationId;
        }

        public void setOrganizationId(String organizationId) {
            this.organizationId = organizationId;
        }

        public String getRoleId() {
            return roleId;
        }

        public void setRoleId(String roleId) {
            this.roleId = roleId;
        }

        public Boolean isRequired() {
            return required;
        }

        public void setRequired(Boolean required) {
            this.required = required;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            OrganizationRoleDefinition that = (OrganizationRoleDefinition) obj;

            if (organizationId != null ? !organizationId.equals(that.organizationId) : that.organizationId != null)
                return false;
            return roleId != null ? roleId.equals(that.roleId) : that.roleId == null;
        }

        @Override
        public int hashCode() {
            int result = organizationId != null ? organizationId.hashCode() : 0;
            result = 31 * result + (roleId != null ? roleId.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "OrganizationRoleDefinition{" +
                    "organizationId='" + organizationId + '\'' +
                    ", roleId='" + roleId + '\'' +
                    ", required=" + required +
                    '}';
        }
    }
}
