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
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.fgap.AdminPermissionsSchema.PermissionConfigOption;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ScopePolicyProviderFactory implements PolicyProviderFactory<ScopePermissionRepresentation> {

    public static final String ID = "scope";
    private final ScopePolicyProvider provider = new ScopePolicyProvider();

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
        return provider;
    }

    @Override
    public Class<ScopePermissionRepresentation> getRepresentationType() {
        return ScopePermissionRepresentation.class;
    }

    @Override
    public ScopePermissionRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
        ScopePermissionRepresentation representation = new ScopePermissionRepresentation();
        String resourceType = policy.getConfig().get("defaultResourceType");
        representation.setResourceType(resourceType);

        // only the settings supported by the resource type are exposed, the others are internal. Values that are not
        // valid, as stored through generic representations without validation, are not exposed either so that clients
        // sending the representation back on update are not rejected
        Map<String, String> config = new HashMap<>();

        for (PermissionConfigOption option : AdminPermissionsSchema.SCHEMA.getPermissionConfigOptions(resourceType)) {
            String value = policy.getConfig().get(option.name());
            if (option.isValid(value)) {
                config.put(option.name(), value);
            }
        }

        if (!config.isEmpty()) {
            representation.setConfig(config);
        }

        return representation;
    }

    @Override
    public void onCreate(Policy policy, ScopePermissionRepresentation representation, AuthorizationProvider authorization) {
        updateResourceType(policy, representation);
        clearPermissionCaches(policy, authorization);
    }

    @Override
    public void onUpdate(Policy policy, ScopePermissionRepresentation representation, AuthorizationProvider authorization) {
        updateResourceType(policy, representation);
        clearPermissionCaches(policy, authorization);
    }

    @Override
    public void onImport(Policy policy, PolicyRepresentation representation, AuthorizationProvider authorization) {
        // the config of generic representations is copied as is, so the flag is removed as for typed representations
        removeExtendChildrenIfNotTargetingSpecificGroups(policy);
        clearPermissionCaches(policy, authorization);
    }

    @Override
    public void onRemove(Policy policy, AuthorizationProvider authorization) {
        AdminPermissionsSchema.SCHEMA.removeOrphanResources(policy, authorization);
        clearPermissionCaches(policy, authorization);
    }

    private void clearPermissionCaches(Policy policy, AuthorizationProvider authorization) {
        AdminPermissionsSchema.SCHEMA.clearPermissionCaches(authorization.getKeycloakSession(), policy.getResourceServer());
    }

    private void updateResourceType(Policy policy, ScopePermissionRepresentation representation) {
        if (representation != null) {
            Map<String, String> config = new HashMap<>(policy.getConfig());

            config.compute("defaultResourceType", (key, value) -> {
                String resourceType = representation.getResourceType();
                return resourceType != null ? representation.getResourceType() : null;
            });

            Map<String, String> permissionConfig = representation.getConfig();

            // null means the config is not being changed, so that clients unaware of it do not reset it on update
            if (permissionConfig != null) {
                for (PermissionConfigOption option : AdminPermissionsSchema.SCHEMA.getPermissionConfigOptions(representation.getResourceType())) {
                    String value = permissionConfig.get(option.name());
                    // default values are not stored
                    if (option.isDefault(value)) {
                        config.remove(option.name());
                    } else {
                        config.put(option.name(), value);
                    }
                }
            }

            policy.setConfig(config);
            removeExtendChildrenIfNotTargetingSpecificGroups(policy);
        }
    }

    /**
     * The flag only applies to specific groups. It is removed once the permission no longer targets them, so that a
     * client unaware of it does not silently extend the permission again when targeting specific groups later. The
     * resources of the permission must already be updated.
     */
    private void removeExtendChildrenIfNotTargetingSpecificGroups(Policy policy) {
        if (policy.getConfig().containsKey(AdminPermissionsSchema.EXTEND_CHILDREN) && !AdminPermissionsSchema.SCHEMA.targetsSpecificGroups(policy)) {
            Map<String, String> config = new HashMap<>(policy.getConfig());
            config.remove(AdminPermissionsSchema.EXTEND_CHILDREN);
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
        return ID;
    }
}
