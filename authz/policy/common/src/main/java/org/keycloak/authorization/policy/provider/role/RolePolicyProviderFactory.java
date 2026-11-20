/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authorization.policy.provider.role;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.policy.provider.util.PolicyValidationException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation.RoleDefinition;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RolePolicyProviderFactory implements PolicyProviderFactory<RolePolicyRepresentation> {

    public static final String ID = "role";
    private static final String UNRESOLVED_ROLES = "unresolvedRoles";
    private RolePolicyProvider provider = new RolePolicyProvider(this::toRepresentation);

    @Override
    public String getName() {
        return "Role";
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
    public RolePolicyRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
        RolePolicyRepresentation representation = new RolePolicyRepresentation();
        String roles = policy.getConfig().get("roles");

        representation.setRoles(getRoles(roles));

        String fetchRoles = policy.getConfig().get("fetchRoles");

        if (StringUtil.isNotBlank(fetchRoles)) {
            representation.setFetchRoles(Boolean.parseBoolean(fetchRoles));
        }

        return representation;
    }

    @Override
    public Class<RolePolicyRepresentation> getRepresentationType() {
        return RolePolicyRepresentation.class;
    }

    @Override
    public void onCreate(Policy policy, RolePolicyRepresentation representation, AuthorizationProvider authorization) {
        updateRoles(policy, representation, authorization);
    }

    @Override
    public void onUpdate(Policy policy, RolePolicyRepresentation representation, AuthorizationProvider authorization) {
        updateRoles(policy, representation, authorization);
    }

    public void updateFromUma(Policy policy, Set<String> roles, AuthorizationProvider authorization) {
        RealmModel realm = authorization.getRealm();
        Set<String> remainingRoles = new HashSet<>(roles);
        Set<RoleDefinition> unresolvedRoles = new HashSet<>();
        for (RoleDefinition definition : getRoles(policy.getConfig().get("roles"))) {
            String id = definition.getId();
            if (roles.contains(id) && realm.getRoleById(id) == null) {
                unresolvedRoles.add(definition);
                remainingRoles.remove(id);
            }
        }
        Set<RoleDefinition> updates = remainingRoles.stream().map(name -> new RoleDefinition(name, false)).collect(Collectors.toSet());
        updateRoles(policy, authorization, updates, Set.of(), unresolvedRoles, true);
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        onImport(policy, representation, authorization, Map.of());
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization, Map<String, String> previousConfig) {
        Map<String, String> config = representation.getConfig();
        updateRoles(policy, authorization, getRoles(config.get("roles")), getStoredRoleIds(previousConfig),
                getRoles(config.get(UNRESOLVED_ROLES)), false);
        policy.removeConfig(UNRESOLVED_ROLES);
        String fetchRoles = representation.getConfig().get("fetchRoles");

        if (StringUtil.isNotBlank(fetchRoles)) {
            policy.putConfig("fetchRoles", fetchRoles);
        }
    }

    @Override
    public void onExport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorizationProvider) {
        Map<String, String> config = new HashMap<>();
        Set<RolePolicyRepresentation.RoleDefinition> roles = toRepresentation(policy, authorizationProvider).getRoles();

        Set<RoleDefinition> resolved = new LinkedHashSet<>();
        Set<RoleDefinition> unresolved = new LinkedHashSet<>();
        for (RolePolicyRepresentation.RoleDefinition roleDefinition : roles) {
            RoleModel role = authorizationProvider.getRealm().getRoleById(roleDefinition.getId());

            if (role == null) {
                unresolved.add(roleDefinition);
                continue;
            }
            if (role.isClientRole()) {
                roleDefinition.setId(ClientModel.class.cast(role.getContainer()).getClientId() + "/" + role.getName());
            } else {
                roleDefinition.setId(role.getName());
            }
            resolved.add(roleDefinition);
        }

        try {
            config.put("roles", JsonSerialization.writeValueAsString(resolved));
            if (!unresolved.isEmpty()) {
                config.put(UNRESOLVED_ROLES, JsonSerialization.writeValueAsString(unresolved));
            }
        } catch (IOException cause) {
            throw new RuntimeException("Failed to export role policy [" + policy.getName() + "]", cause);
        }

        String fetchRoles = policy.getConfig().get("fetchRoles");

        if (StringUtil.isNotBlank(fetchRoles)) {
            config.put("fetchRoles", fetchRoles);
        }

        representation.setConfig(config);
    }

    private void updateRoles(Policy policy, RolePolicyRepresentation representation, AuthorizationProvider authorization) {
        if (representation.isFetchRoles() != null) {
            policy.putConfig("fetchRoles", String.valueOf(representation.isFetchRoles()));
        }
        updateRoles(policy, authorization, representation.getRoles(), getStoredRoleIds(policy.getConfig()), Set.of(), false);
    }

    private Set<String> getStoredRoleIds(Map<String, String> config) {
        return getRoles(config.get("roles")).stream().map(RoleDefinition::getId).collect(Collectors.toSet());
    }

    private void updateRoles(Policy policy, AuthorizationProvider authorization, Set<RoleDefinition> roles,
            Set<String> storedRoleIds, Set<RoleDefinition> unresolvedRoles, boolean preferNames) {
        Set<RoleDefinition> updatedRoles = new HashSet<>(unresolvedRoles);
        Set<String> processedRoles = new HashSet<>();
        if (roles != null) {
            RealmModel realm = authorization.getRealm();
            for (RolePolicyRepresentation.RoleDefinition definition : roles) {
                boolean storedId = storedRoleIds.contains(definition.getId());
                RoleModel role = storedId ? realm.getRoleById(definition.getId()) : getRole(definition, realm, preferNames);
                if (role == null) {
                    if (storedId || Boolean.TRUE.equals(definition.isRequired()) || Logic.NEGATIVE.equals(policy.getLogic())) {
                        updatedRoles.add(definition);
                    }
                    continue;
                }

                if (!processedRoles.add(role.getId())) {
                    throw new PolicyValidationException("Role can't be specified multiple times - " + role.getName());
                }
                definition.setId(role.getId());
                updatedRoles.add(definition);
            }
        }

        try {
            policy.putConfig("roles", JsonSerialization.writeValueAsString(updatedRoles));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to serialize roles", cause);
        }
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return ID;
    }

    private Set<RoleDefinition> getRoles(String rawRoles) {
        if (rawRoles != null) {
            try {
                return Arrays.stream(JsonSerialization.readValue(rawRoles, RoleDefinition[].class))
                        .sorted()
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            } catch (IOException e) {
                throw new RuntimeException("Could not parse roles from config: [" + rawRoles + "]", e);
            }
        }

        return Collections.emptySet();
    }

    public static final Pattern UUID_PATTERN = Pattern.compile("[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}");

    private RoleModel getRole(RolePolicyRepresentation.RoleDefinition definition, RealmModel realm, boolean preferNames) {
        String roleName = definition.getId();
        String clientId = null;
        int clientIdSeparator = roleName.indexOf("/");

        if (clientIdSeparator != -1) {
            clientId = roleName.substring(0, clientIdSeparator);
            roleName = roleName.substring(clientIdSeparator + 1);
        }

        RoleModel role;

        if (clientId == null) {
            // if the role name looks like a UUID, it is likely that it is a role ID. Then do this look-up first to avoid hitting the database twice
            // TODO: In a future version of the auth feature, make this more strict to avoid the double lookup and any ambiguity
            boolean tryIdFirst = !preferNames && UUID_PATTERN.matcher(roleName).matches();
            role = tryIdFirst ? realm.getRoleById(roleName) : realm.getRole(roleName);

            if (role == null) {
                role = tryIdFirst ? realm.getRole(roleName) : realm.getRoleById(roleName);
            }
        } else {
            ClientModel client = realm.getClientByClientId(clientId);

            if (client == null) {
                return null;
            }

            role = client.getRole(roleName);
        }

        return role;
    }
}
