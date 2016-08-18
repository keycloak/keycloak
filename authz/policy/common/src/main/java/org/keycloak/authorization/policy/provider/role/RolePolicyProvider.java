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
package org.keycloak.authorization.policy.provider.role;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

import java.util.Map;

import static org.keycloak.authorization.policy.provider.role.RolePolicyProviderFactory.getRoles;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RolePolicyProvider implements PolicyProvider {

    private final Policy policy;
    private final AuthorizationProvider authorization;

    public RolePolicyProvider(Policy policy, AuthorizationProvider authorization) {
        this.policy = policy;
        this.authorization = authorization;
    }

    public RolePolicyProvider() {
        this(null, null);
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        Map<String, Object>[] roleIds = getRoles(this.policy);

        if (roleIds.length > 0) {
            Identity identity = evaluation.getContext().getIdentity();

            for (Map<String, Object> current : roleIds) {
                RoleModel role = getCurrentRealm().getRoleById((String) current.get("id"));

                if (role != null) {
                    boolean hasRole = hasRole(identity, role);

                    if (!hasRole && Boolean.valueOf(isRequired(current))) {
                        evaluation.deny();
                        return;
                    } else if (hasRole) {
                        evaluation.grant();
                    }
                }
            }
        }
    }

    private boolean isRequired(Map<String, Object> current) {
        return (boolean) current.getOrDefault("required", false);
    }

    private boolean hasRole(Identity identity, RoleModel role) {
        String roleName = role.getName();
        if (role.isClientRole()) {
            ClientModel clientModel = getCurrentRealm().getClientById(role.getContainerId());
            return identity.hasClientRole(clientModel.getClientId(), roleName);
        }
        return identity.hasRealmRole(roleName);
    }

    private RealmModel getCurrentRealm() {
        return this.authorization.getKeycloakSession().getContext().getRealm();
    }

    @Override
    public void close() {

    }
}