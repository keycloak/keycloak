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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation.GroupDefinition;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class GroupPolicyProviderFactory implements PolicyProviderFactory<GroupPolicyRepresentation> {

    public static final String ID = "group";

    private GroupPolicyProvider provider = new GroupPolicyProvider(this::toRepresentation);

    @Override
    public String getId() {
        return ID;
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
            representation.setGroups(getGroupsDefinition(policy.getConfig(), authorization));
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
            updatePolicy(policy, representation.getConfig().get("groupsClaim"), getGroupsDefinition(representation.getConfig(), authorization), authorization);
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

            if (group == null) {
                continue;
            }

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
    }

    @Override
    public void close() {

    }

    private void updatePolicy(Policy policy, String groupsClaim, Set<GroupPolicyRepresentation.GroupDefinition> groups, AuthorizationProvider authorization) {
        if (groups == null) {
            throw new RuntimeException("You must provide at least one group");
        }

        Map<String, String> config = new HashMap<>(policy.getConfig());

        if (groupsClaim != null) {
            config.put("groupsClaim", groupsClaim);
        }

        for (GroupPolicyRepresentation.GroupDefinition definition : groups) {
            GroupModel group = getGroup(authorization, definition);

            if (group == null) {
                continue;
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

    private GroupModel getGroup(AuthorizationProvider authorization, GroupDefinition definition) {
        RealmModel realm = authorization.getRealm();
        KeycloakSession session = authorization.getKeycloakSession();
        GroupProvider groups = session.groups();

        if (definition.getId() != null) {
            return realm.getGroupById(definition.getId());
        }

        GroupModel group = null;
        String path = definition.getPath();

        if (path != null) {
            String canonicalPath = path.startsWith("/") ? path.substring(1) : path;
            String[] parts = canonicalPath.split("/");
            GroupModel parent = null;

            for (String part : parts) {
                if (parent == null) {
                    parent = groups.getGroupByName(realm, null, part);
                    if (parent == null) {
                        return null;
                    }
                } else {
                    group = groups.getGroupByName(realm, parent, part);
                    if (group == null) {
                        return null;
                    }
                    parent = group;
                }
            }

            if (parts.length == 1) {
                group = parent;
            }
        }

        return group;
    }

    private Set<GroupPolicyRepresentation.GroupDefinition> getGroupsDefinition(Map<String, String> config, AuthorizationProvider authorization) throws IOException {
        String groups = config.get("groups");

        if (groups == null) {
            return Collections.emptySet();
        }

        return Arrays.stream(JsonSerialization.readValue(groups, GroupPolicyRepresentation.GroupDefinition[].class))
                .filter(d -> getGroup(authorization, d) != null)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
