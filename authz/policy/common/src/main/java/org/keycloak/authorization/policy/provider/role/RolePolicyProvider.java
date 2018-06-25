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

import java.util.Set;
import java.util.function.BiFunction;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RolePolicyProvider implements PolicyProvider {

    private final BiFunction<Policy, AuthorizationProvider, RolePolicyRepresentation> representationFunction;

    public RolePolicyProvider(BiFunction<Policy, AuthorizationProvider, RolePolicyRepresentation> representationFunction) {
        this.representationFunction = representationFunction;
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        Policy policy = evaluation.getPolicy();
        Set<RolePolicyRepresentation.RoleDefinition> roleIds = representationFunction.apply(policy, evaluation.getAuthorizationProvider()).getRoles();
        AuthorizationProvider authorizationProvider = evaluation.getAuthorizationProvider();
        RealmModel realm = authorizationProvider.getKeycloakSession().getContext().getRealm();
        Identity identity = evaluation.getContext().getIdentity();

        for (RolePolicyRepresentation.RoleDefinition roleDefinition : roleIds) {
            RoleModel role = realm.getRoleById(roleDefinition.getId());

            if (role != null) {
                boolean hasRole = hasRole(identity, role, realm);

                if (!hasRole && roleDefinition.isRequired()) {
                    evaluation.deny();
                    return;
                } else if (hasRole) {
                    evaluation.grant();
                }
            }
        }
    }

    private boolean hasRole(Identity identity, RoleModel role, RealmModel realm) {
        String roleName = role.getName();
        if (role.isClientRole()) {
            ClientModel clientModel = realm.getClientById(role.getContainerId());
            return identity.hasClientRole(clientModel.getClientId(), roleName);
        }
        return identity.hasRealmRole(roleName);
    }

    @Override
    public void close() {

    }
}