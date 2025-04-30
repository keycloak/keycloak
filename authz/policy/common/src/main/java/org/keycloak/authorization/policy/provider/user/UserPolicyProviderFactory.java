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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
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
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserPolicyProviderFactory implements PolicyProviderFactory<UserPolicyRepresentation> {

    public static final String ID = "user";

    private UserPolicyProvider provider = new UserPolicyProvider(this::toRepresentation);

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

        try {
            String users = policy.getConfig().get("users");

            if (users == null) {
                representation.setUsers(Collections.emptySet());
            } else {
                representation.setUsers((Set<String>) JsonSerialization.readValue(users, Set.class).stream()
                        .filter(id -> getUser((String) id, authorization) != null)
                        .collect(Collectors.toSet()));
            }
        } catch (IOException cause) {
            throw new RuntimeException("Failed to deserialize roles", cause);
        }

        return representation;
    }

    @Override
    public Class<UserPolicyRepresentation> getRepresentationType() {
        return UserPolicyRepresentation.class;
    }

    @Override
    public void onCreate(Policy policy, UserPolicyRepresentation representation, AuthorizationProvider authorization) {
        updateUsers(policy, authorization, representation.getUsers());
    }

    @Override
    public void onUpdate(Policy policy, UserPolicyRepresentation representation, AuthorizationProvider authorization) {
        updateUsers(policy, authorization, representation.getUsers());
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        try {
            updateUsers(policy, authorization, JsonSerialization.readValue(representation.getConfig().get("users"), Set.class));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to deserialize users during import", cause);
        }
    }

    @Override
    public void onExport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorizationProvider) {
        UserPolicyRepresentation userRep = toRepresentation(policy, authorizationProvider);
        Map<String, String> config = new HashMap<>();

        try {
            UserProvider userProvider = authorizationProvider.getKeycloakSession().users();
            RealmModel realm = authorizationProvider.getRealm();

            config.put("users", JsonSerialization.writeValueAsString(userRep.getUsers().stream()
                    .map(id -> {
                        UserModel user = userProvider.getUserById(realm, id);
                        return user != null ? user.getUsername() : null;
                    })
                    .filter(Objects::nonNull).collect(Collectors.toList())));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to export user policy [" + policy.getName() + "]", cause);
        }

        representation.setConfig(config);
    }

    private void updateUsers(Policy policy, AuthorizationProvider authorization, Set<String> users) {
        Set<String> updatedUsers = new HashSet<>();

        if (users != null) {
            for (String userId : users) {
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
