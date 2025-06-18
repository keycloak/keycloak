/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.policy.provider.permission;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourcePolicyProviderFactory implements PolicyProviderFactory<ResourcePermissionRepresentation> {

    private ResourcePolicyProvider provider = new ResourcePolicyProvider();

    @Override
    public String getName() {
        return "Resource-Based";
    }

    @Override
    public String getGroup() {
        return "Permission";
    }

    @Override
    public PolicyProvider create(AuthorizationProvider authorization) {
        return provider;
    }

    @Override
    public Class<ResourcePermissionRepresentation> getRepresentationType() {
        return ResourcePermissionRepresentation.class;
    }

    @Override
    public ResourcePermissionRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
        ResourcePermissionRepresentation representation = new ResourcePermissionRepresentation();
        representation.setResourceType(policy.getConfig().get("defaultResourceType"));
        return representation;
    }

    @Override
    public PolicyProvider create(KeycloakSession session) {
        return provider;
    }

    @Override
    public void onCreate(Policy policy, ResourcePermissionRepresentation representation, AuthorizationProvider authorization) {
        updateResourceType(policy, representation);
    }

    @Override
    public void onUpdate(Policy policy, ResourcePermissionRepresentation representation, AuthorizationProvider authorization) {
        updateResourceType(policy, representation);
    }

    private void updateResourceType(Policy policy, ResourcePermissionRepresentation representation) {
        if (representation != null) {
            Map<String, String> config = new HashMap(policy.getConfig());

            config.compute("defaultResourceType", (key, value) -> {
                String resourceType = representation.getResourceType();
                return resourceType != null ? representation.getResourceType() : null;
            });

            policy.setConfig(config);
        }
    }

    @Override
    public void onRemove(Policy policy, AuthorizationProvider authorization) {

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
        return "resource";
    }
}
