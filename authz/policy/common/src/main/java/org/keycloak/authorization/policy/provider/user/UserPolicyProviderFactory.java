/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.authorization.policy.provider.user;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserPolicyProviderFactory implements PolicyProviderFactory<UserPolicyRepresentation> {

    public static final String ID = "user";
    private static final String UNRESOLVED_USERS = "unresolvedUsers";

    private UserPolicyProvider provider = new UserPolicyProvider();

    @Override
    public String getName() {
        return "User";
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
    public UserPolicyRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
        UserPolicyRepresentation representation = new UserPolicyRepresentation();

        Set<String> users = getUsers(policy);
        representation.setUsers(Logic.NEGATIVE.equals(policy.getLogic()) ? users : users.stream()
                .filter(id -> getUserById(id, authorization) != null)
                .collect(Collectors.toSet()));

        return representation;
    }

    @Override
    public Class<UserPolicyRepresentation> getRepresentationType() {
        return UserPolicyRepresentation.class;
    }

    @Override
    public void onCreate(Policy policy, UserPolicyRepresentation representation, AuthorizationProvider authorization) {
        updateUsers(policy, authorization, representation.getUsers(), Set.of(), Set.of());
    }

    @Override
    public void onUpdate(Policy policy, UserPolicyRepresentation representation, AuthorizationProvider authorization) {
        updateUsers(policy, authorization, representation.getUsers(), getUsers(policy), Set.of());
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        onImport(policy, representation, authorization, Map.of());
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization, Map<String, String> previousConfig) {
        Map<String, String> config = representation.getConfig();
        String users = config.get("users");
        if (users == null) {
            throw new RuntimeException("No users provided.");
        }
        updateUsers(policy, authorization, readUsers(users), readUsers(previousConfig.get("users")),
                readUsers(config.get(UNRESOLVED_USERS)));
        policy.removeConfig(UNRESOLVED_USERS);
    }

    @Override
    public void onExport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorizationProvider) {
        Map<String, String> config = new HashMap<>();
        Set<String> resolved = new HashSet<>();
        Set<String> unresolved = new HashSet<>();
        for (String id : getUsers(policy)) {
            UserModel user = getUserById(id, authorizationProvider);
            if (user == null) {
                unresolved.add(id);
            } else {
                resolved.add(user.getUsername());
            }
        }

        try {
            config.put("users", JsonSerialization.writeValueAsString(resolved));
            if (!unresolved.isEmpty()) {
                config.put(UNRESOLVED_USERS, JsonSerialization.writeValueAsString(unresolved));
            }
        } catch (IOException cause) {
            throw new RuntimeException("Failed to export user policy [" + policy.getName() + "]", cause);
        }

        representation.setConfig(config);
    }

    private void updateUsers(Policy policy, AuthorizationProvider authorization, Set<String> users,
            Set<String> existingUsers, Set<String> unresolvedUsers) {
        Set<String> updatedUsers = new HashSet<>(unresolvedUsers);

        if (users != null) {
            for (String userId : users) {
                if (existingUsers.contains(userId)) {
                    updatedUsers.add(userId);
                    continue;
                }
                UserModel user = getUser(userId, authorization);

                if (user == null) {
                    throw new RuntimeException("Error while updating policy [" + policy.getName()  + "]. User [" + userId + "] could not be found.");
                }

                updatedUsers.add(user.getId());
            }
        }

        try {

            policy.putConfig("users", JsonSerialization.writeValueAsString(updatedUsers));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to serialize users", cause);
        }
    }

    static Set<String> getUsers(Policy policy) {
        return readUsers(policy.getConfig().get("users"));
    }

    private static Set<String> readUsers(String users) {
        if (users == null) {
            return Collections.emptySet();
        }
        try {
            return Set.copyOf(Arrays.asList(JsonSerialization.readValue(users, String[].class)));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to deserialize users", cause);
        }
    }

    static UserModel getUserById(String userId, AuthorizationProvider authorization) {
        return authorization.getKeycloakSession().users().getUserById(authorization.getRealm(), userId);
    }

    private static UserModel getUser(String userId, AuthorizationProvider authorization) {
        if (userId == null) {
            return null;
        }

        KeycloakSession session = authorization.getKeycloakSession();
        RealmModel realm = authorization.getRealm();
        UserProvider userProvider = session.users();
        UserModel user = userProvider.getUserById(realm, userId);

        if (user == null) {
            // fallback - userId is possibly a username
            user = userProvider.getUserByUsername(realm, userId);
        }

        return user;
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
}
