/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.policy.provider.identity;

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
import org.keycloak.models.RoleContainerModel.RoleRemovedEvent;
import org.keycloak.models.RoleModel;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RolePolicyProviderFactory implements PolicyProviderFactory {

    @Override
    public String getName() {
        return "Role-Based";
    }

    @Override
    public String getGroup() {
        return "Identity Based";
    }

    @Override
    public PolicyProvider create(Policy policy, AuthorizationProvider authorization) {
        return new RolePolicyProvider(policy, authorization);
    }

    @Override
    public PolicyProviderAdminService getAdminResource(ResourceServer resourceServer) {
        return null;
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return new RolePolicyProvider();
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(event -> {
            if (event instanceof RoleRemovedEvent) {
                KeycloakSession keycloakSession = ((RoleRemovedEvent) event).getKeycloakSession();
                AuthorizationProvider provider = keycloakSession.getProvider(AuthorizationProvider.class);
                PolicyStore policyStore = provider.getStoreFactory().getPolicyStore();
                RoleModel removedRole = ((RoleRemovedEvent) event).getRole();

                policyStore.findByType(getId()).forEach(policy -> {
                    List<String> roles = new ArrayList<>();

                    for (String roleId : getRoles(policy)) {
                        if (!roleId.equals(removedRole.getId())) {
                            roles.add(roleId);
                        }
                    }

                    try {
                        if (roles.isEmpty()) {
                            policyStore.findDependentPolicies(policy.getId()).forEach(dependentPolicy -> {
                                dependentPolicy.removeAssociatedPolicy(policy);
                            });
                            policyStore.delete(policy.getId());
                        } else {
                            policy.getConfig().put("roles", JsonSerialization.writeValueAsString(roles));
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Error while synchronizing roles with policy [" + policy.getName() + "].", e);
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
        return "role";
    }

    static String[] getRoles(Policy policy) {
        String roles = policy.getConfig().get("roles");

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
