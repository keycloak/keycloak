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
package org.keycloak.authorization.admin;

import static org.keycloak.models.utils.RepresentationToModel.toModel;

import java.io.IOException;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.services.resources.admin.RealmAuth;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PermissionResourceService extends PolicyResourceService {

    public PermissionResourceService(Policy policy, ResourceServer resourceServer, AuthorizationProvider authorization, RealmAuth auth) {
        super(policy, resourceServer, authorization, auth);
    }

    @Override
    protected void doUpdate(Policy policy, String payload) {
        String type = policy.getType();
        PolicyProviderAdminService provider = getPolicyProviderAdminResource(type);
        AbstractPolicyRepresentation representation = toRepresentation(type, payload, provider);

        policy = toModel(representation, policy.getResourceServer(), authorization);

        try {
            provider.onUpdate(policy, representation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AbstractPolicyRepresentation toRepresentation(String type, String payload, PolicyProviderAdminService provider) {
        Class<? extends AbstractPolicyRepresentation> representationType = provider.getRepresentationType();

        if (representationType == null) {
            throw new RuntimeException("Policy provider for type [" + type + "] returned a null representation type.");
        }

        AbstractPolicyRepresentation representation;

        try {
            representation = JsonSerialization.readValue(payload, representationType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize JSON using policy provider for type [" + type + "].", e);
        }
        return representation;
    }

    @Override
    protected Object toRepresentation(Policy policy) {
        PolicyProviderAdminService provider = getPolicyProviderAdminResource(policy.getType());
        return toRepresentation(policy, provider.toRepresentation(policy));
    }

    private AbstractPolicyRepresentation toRepresentation(Policy policy, AbstractPolicyRepresentation representation) {
        representation.setId(policy.getId());
        representation.setName(policy.getName());
        representation.setDescription(policy.getDescription());
        representation.setType(policy.getType());
        representation.setDecisionStrategy(policy.getDecisionStrategy());
        representation.setLogic(policy.getLogic());
        representation.addResource(policy.getResources().stream().map(resource -> resource.getId()).findFirst().orElse(null));
        representation.addPolicies(policy.getAssociatedPolicies().stream().map(associated -> associated.getId()).toArray(value -> new String[value]));
        representation.addScopes(policy.getScopes().stream().map(associated -> associated.getId()).toArray(value -> new String[value]));

        return representation;
    }
}
