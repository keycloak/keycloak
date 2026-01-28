/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.resources.admin.fgap;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
class Helper {
    public static Policy addScopePermission(AuthorizationProvider authz, ResourceServer resourceServer, String name, Resource resource, Scope scope, Policy policy) {
        ScopePermissionRepresentation representation = new ScopePermissionRepresentation();

        representation.setName(name);
        representation.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        representation.setLogic(Logic.POSITIVE);
        representation.addResource(resource.getName());
        representation.addScope(scope.getName());
        representation.addPolicy(policy.getName());

        return authz.getStoreFactory().getPolicyStore().create(resourceServer, representation);
    }

    public static Policy addEmptyScopePermission(AuthorizationProvider authz, ResourceServer resourceServer, String name, Resource resource, Scope scope) {
        ScopePermissionRepresentation representation = new ScopePermissionRepresentation();

        representation.setName(name);
        representation.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        representation.setLogic(Logic.POSITIVE);
        representation.addResource(resource.getName());
        representation.addScope(scope.getName());

        return authz.getStoreFactory().getPolicyStore().create(resourceServer, representation);
    }

    public static Policy createRolePolicy(AuthorizationProvider authz, ResourceServer resourceServer, RoleModel role) {
        String roleName = getRolePolicyName(role);
        return createRolePolicy(authz, resourceServer, role, roleName);
    }

    public static Policy createRolePolicy(AuthorizationProvider authz, ResourceServer resourceServer, RoleModel role, String policyName) {
        PolicyRepresentation representation = new PolicyRepresentation();

        representation.setName(policyName);
        representation.setType("role");
        representation.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        representation.setLogic(Logic.POSITIVE);
        String roleValues = "[{\"id\":\"" + role.getId() + "\",\"required\": true}]";
        Map<String, String> config = new HashMap<>();
        config.put("roles", roleValues);
        representation.setConfig(config);

        return authz.getStoreFactory().getPolicyStore().create(resourceServer, representation);
    }

    public static String getRolePolicyName(RoleModel role) {
        String roleName = "";
        if (role.getContainer() instanceof ClientModel) {
            ClientModel client = (ClientModel) role.getContainer();
            roleName = client.getClientId() + "." + role.getName();
        } else {
            roleName = role.getName();
        }
        roleName = "role.policy." + roleName;
        return roleName;
    }
}
