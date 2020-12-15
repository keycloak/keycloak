/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authorization.policy.provider.group;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class GroupPolicyProviderFactory implements PolicyProviderFactory<GroupPolicyRepresentation> {

    private GroupPolicyProvider provider = new GroupPolicyProvider(this::toRepresentation);

    @Override
    public String getId() {
        return "group";
    }

    @Override
    public String getName() {
        return "Group";
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
    public GroupPolicyRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
        GroupPolicyRepresentation representation = new GroupPolicyRepresentation();

        representation.setGroupsClaim(policy.getConfig().get("groupsClaim"));

        try {
            representation.setGroups(getGroupsDefinition(policy.getConfig()));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to deserialize groups", cause);
        }
        return representation;
    }

    @Override
    public Class<GroupPolicyRepresentation> getRepresentationType() {
        return GroupPolicyRepresentation.class;
    }

    @Override
    public void onCreate(Policy policy, GroupPolicyRepresentation representation, AuthorizationProvider authorization) {
        updatePolicy(policy, representation.getGroupsClaim(), representation.getGroups(), authorization);
    }

    @Override
    public void onUpdate(Policy policy, GroupPolicyRepresentation representation, AuthorizationProvider authorization) {
        updatePolicy(policy, representation.getGroupsClaim(), representation.getGroups(), authorization);
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        try {
            updatePolicy(policy, representation.getConfig().get("groupsClaim"), getGroupsDefinition(representation.getConfig()), authorization);
        } catch (IOException cause) {
            throw new RuntimeException("Failed to deserialize groups", cause);
        }
    }

    @Override
    public void onExport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        Map<String, String> config = new HashMap<>();
        GroupPolicyRepresentation groupPolicy = toRepresentation(policy, authorization);
        Set<GroupPolicyRepresentation.GroupDefinition> groups = groupPolicy.getGroups();

        for (GroupPolicyRepresentation.GroupDefinition definition: groups) {
            GroupModel group = authorization.getRealm().getGroupById(definition.getId());
            definition.setId(null);
            definition.setPath(ModelToRepresentation.buildGroupPath(group));
        }

        try {
            String groupsClaim = groupPolicy.getGroupsClaim();

            if (groupsClaim != null) {
                config.put("groupsClaim", groupsClaim);
            }

            config.put("groups", JsonSerialization.writeValueAsString(groups));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to export group policy [" + policy.getName() + "]", cause);
        }

        representation.setConfig(config);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(event -> {
        });
    }

    @Override
    public void close() {

    }

    private void updatePolicy(Policy policy, String groupsClaim, Set<GroupPolicyRepresentation.GroupDefinition> groups, AuthorizationProvider authorization) {
        if (groups == null || groups.isEmpty()) {
            throw new RuntimeException("You must provide at least one group");
        }

        Map<String, String> config = new HashMap<>(policy.getConfig());

        if (groupsClaim != null) {
            config.put("groupsClaim", groupsClaim);
        }

        List<GroupModel> topLevelGroups = authorization.getRealm().getTopLevelGroupsStream().collect(Collectors.toList());

        for (GroupPolicyRepresentation.GroupDefinition definition : groups) {
            GroupModel group = null;

            if (definition.getId() != null) {
                group = authorization.getRealm().getGroupById(definition.getId());
            }

            String path = definition.getPath();
            
            if (group == null && path != null) {
                String canonicalPath = path.startsWith("/") ? path.substring(1, path.length()) : path;

                if (canonicalPath != null) {
                    String[] parts = canonicalPath.split("/");
                    GroupModel parent = null;

                    for (String part : parts) {
                        if (parent == null) {
                            parent = topLevelGroups.stream().filter(groupModel -> groupModel.getName().equals(part)).findFirst().orElseThrow(() -> new RuntimeException("Top level group with name [" + part + "] not found"));
                        } else {
                            group = parent.getSubGroupsStream().filter(groupModel -> groupModel.getName().equals(part)).findFirst().orElseThrow(() -> new RuntimeException("Group with name [" + part + "] not found"));
                            parent = group;
                        }
                    }

                    if (parts.length == 1) {
                        group = parent;
                    }
                }
            }

            if (group == null) {
                throw new RuntimeException("Group with id [" + definition.getId() + "] not found");
            }

            definition.setId(group.getId());
            definition.setPath(null);
        }

        try {
            config.put("groups", JsonSerialization.writeValueAsString(groups));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to serialize groups", cause);
        }

        policy.setConfig(config);
    }

    private HashSet<GroupPolicyRepresentation.GroupDefinition> getGroupsDefinition(Map<String, String> config) throws IOException {
        return new HashSet<>(Arrays.asList(JsonSerialization.readValue(config.get("groups"), GroupPolicyRepresentation.GroupDefinition[].class)));
    }
}
