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

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ScopePolicyProviderFactory implements PolicyProviderFactory<ScopePermissionRepresentation> {

    private ScopePolicyProvider provider = new ScopePolicyProvider();

    @Override
    public String getName() {
        return "Scope-Based";
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
    public PolicyProvider create(KeycloakSession session) {
        return null;
    }

    @Override
    public Class<ScopePermissionRepresentation> getRepresentationType() {
        return ScopePermissionRepresentation.class;
    }

    @Override
    public ScopePermissionRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
        ScopePermissionRepresentation representation = new ScopePermissionRepresentation();
        representation.setResourceType(policy.getConfig().get("defaultResourceType"));
        return representation;
    }

    @Override
    public void onCreate(Policy policy, ScopePermissionRepresentation representation, AuthorizationProvider authorization) {
        updateResourceType(policy, representation);
    }

    @Override
    public void onUpdate(Policy policy, ScopePermissionRepresentation representation, AuthorizationProvider authorization) {
        updateResourceType(policy, representation);
    }

    private void updateResourceType(Policy policy, ScopePermissionRepresentation representation) {
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
        return "scope";
    }
}
