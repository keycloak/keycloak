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

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.UserRemovedEvent;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserPolicyProviderFactory implements PolicyProviderFactory {

    @Override
    public String getName() {
        return "User-Based";
    }

    @Override
    public String getGroup() {
        return "Identity Based";
    }

    @Override
    public PolicyProvider create(Policy policy, AuthorizationProvider authorization) {
        return new UserPolicyProvider(policy);
    }

    @Override
    public PolicyProviderAdminService getAdminResource(ResourceServer resourceServer) {
        return null;
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return null;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(event -> {
            if (event instanceof UserRemovedEvent) {
                KeycloakSession keycloakSession = ((UserRemovedEvent) event).getKeycloakSession();
                AuthorizationProvider provider = keycloakSession.getProvider(AuthorizationProvider.class);
                PolicyStore policyStore = provider.getStoreFactory().getPolicyStore();
                UserModel removedUser = ((UserRemovedEvent) event).getUser();

                policyStore.findByType(getId()).forEach(policy -> {
                    List<String> users = new ArrayList<>();

                    for (String userId : getUsers(policy)) {
                        if (!userId.equals(removedUser.getId())) {
                            users.add(userId);
                        }
                    }

                    try {
                        if (users.isEmpty()) {
                            policyStore.findDependentPolicies(policy.getId()).forEach(dependentPolicy -> {
                                dependentPolicy.removeAssociatedPolicy(policy);
                            });
                            policyStore.delete(policy.getId());
                        } else {
                            policy.getConfig().put("users", JsonSerialization.writeValueAsString(users));
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Error while synchronizing users with policy [" + policy.getName() + "].", e);
                    }
                });
            }
        });
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "user";
    }

    static String[] getUsers(Policy policy) {
        String roles = policy.getConfig().get("users");

        if (roles != null) {
            try {
                return JsonSerialization.readValue(roles.getBytes(), String[].class);
            } catch (IOException e) {
                throw new RuntimeException("Could not parse roles [" + roles + "] from policy config [" + policy.getName() + ".", e);
            }
        }

        return new String[]{};
    }
}
