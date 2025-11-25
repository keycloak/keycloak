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

package org.keycloak.authorization.policy.provider.organizationrole;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationRoleModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.authorization.OrganizationRolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.OrganizationRolePolicyRepresentation.OrganizationRoleDefinition;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.util.JsonSerialization;

/**
 * Factory for creating organization role-based policy providers.
 * 
 * This factory handles the creation and configuration of policies that
 * evaluate authorization based on organization roles.
 */
public class OrganizationRolePolicyProviderFactory implements PolicyProviderFactory<OrganizationRolePolicyRepresentation> {

    public static final String ID = "organization-role";

    private OrganizationRolePolicyProvider provider = new OrganizationRolePolicyProvider(this::toRepresentation);

    @Override
    public String getName() {
        return "Organization Role";
    }

    @Override
    public String getGroup() {
        return "Identity Based";
    }

    @Override
    public PolicyProvider create(AuthorizationProvider authorization) {
        return provider;
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return provider;
    }

    @Override
    public OrganizationRolePolicyRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
        OrganizationRolePolicyRepresentation representation = new OrganizationRolePolicyRepresentation();
        String organizationRoles = policy.getConfig().get("organizationRoles");

        if (organizationRoles != null) {
            try {
                @SuppressWarnings("unchecked")
                Set<OrganizationRoleDefinition> roleDefinitions = JsonSerialization.readValue(organizationRoles.getBytes(), Set.class);
                representation.setOrganizationRoles(roleDefinitions);
            } catch (IOException cause) {
                throw new RuntimeException("Failed to deserialize organization roles for policy [" + policy.getName() + "]", cause);
            }
        }

        String fetchRoles = policy.getConfig().get("fetchRoles");
        if (fetchRoles != null) {
            representation.setFetchRoles(Boolean.parseBoolean(fetchRoles));
        }

        representation.setId(policy.getId());
        representation.setName(policy.getName());
        representation.setDescription(policy.getDescription());
        representation.setDecisionStrategy(policy.getDecisionStrategy());
        representation.setLogic(policy.getLogic());

        return representation;
    }

    @Override
    public Class<OrganizationRolePolicyRepresentation> getRepresentationType() {
        return OrganizationRolePolicyRepresentation.class;
    }

    @Override
    public void onCreate(Policy policy, OrganizationRolePolicyRepresentation representation, AuthorizationProvider authorization) {
        updatePolicy(policy, representation, authorization);
    }

    @Override
    public void onUpdate(Policy policy, OrganizationRolePolicyRepresentation representation, AuthorizationProvider authorization) {
        updatePolicy(policy, representation, authorization);
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        String organizationRoles = representation.getConfig().get("organizationRoles");
        if (organizationRoles != null) {
            policy.putConfig("organizationRoles", organizationRoles);
        }
        
        String fetchRoles = representation.getConfig().get("fetchRoles");
        if (fetchRoles != null) {
            policy.putConfig("fetchRoles", fetchRoles);
        }
    }

    @Override
    public void onExport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorizationProvider) {
        Map<String, String> config = new HashMap<>();
        
        String organizationRoles = policy.getConfig().get("organizationRoles");
        if (organizationRoles != null) {
            config.put("organizationRoles", organizationRoles);
        }
        
        String fetchRoles = policy.getConfig().get("fetchRoles");
        if (fetchRoles != null) {
            config.put("fetchRoles", fetchRoles);
        }
        
        representation.setConfig(config);
    }

    private void updatePolicy(Policy policy, OrganizationRolePolicyRepresentation representation, AuthorizationProvider authorization) {
        Set<OrganizationRoleDefinition> organizationRoles = representation.getOrganizationRoles();

        if (organizationRoles != null) {
            validateOrganizationRoles(organizationRoles, authorization);
            
            try {
                policy.putConfig("organizationRoles", JsonSerialization.writeValueAsString(organizationRoles));
            } catch (IOException cause) {
                throw new RuntimeException("Failed to serialize organization roles for policy [" + policy.getName() + "]", cause);
            }
        }
        
        if (representation.isFetchRoles() != null) {
            policy.putConfig("fetchRoles", String.valueOf(representation.isFetchRoles()));
        }
    }

    private void validateOrganizationRoles(Set<OrganizationRoleDefinition> organizationRoles, AuthorizationProvider authorization) {
        KeycloakSession session = authorization.getKeycloakSession();
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);

        for (OrganizationRoleDefinition roleDefinition : organizationRoles) {
            String organizationId = roleDefinition.getOrganizationId();
            String roleId = roleDefinition.getRoleId();

            if (organizationId == null || roleId == null) {
                throw new RuntimeException("Organization ID and Role ID are required for organization role policy");
            }

            OrganizationModel organization = orgProvider.getById(organizationId);
            if (organization == null) {
                throw new RuntimeException("Organization with ID '" + organizationId + "' not found");
            }

            OrganizationRoleModel role = organization.getRoleById(roleId);
            if (role == null) {
                throw new RuntimeException("Role with ID '" + roleId + "' not found in organization '" + organization.getName() + "'");
            }
        }
    }

    @Override
    public void init(Config.Scope config) {
        // No initialization required
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization required
    }

    @Override
    public void close() {
        // No resources to close
    }

    @Override
    public String getId() {
        return ID;
    }
}
