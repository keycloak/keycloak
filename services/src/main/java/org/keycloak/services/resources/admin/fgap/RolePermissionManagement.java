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

import java.util.Map;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.models.RoleModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RolePermissionManagement {
    public static final String MAP_ROLE_SCOPE = "map-role";
    public static final String MAP_ROLE_CLIENT_SCOPE_SCOPE = "map-role-client-scope";
    public static final String MAP_ROLE_COMPOSITE_SCOPE = "map-role-composite";

    boolean isPermissionsEnabled(RoleModel role);
    void setPermissionsEnabled(RoleModel role, boolean enable);

    Map<String, String> getPermissions(RoleModel role);

    Policy mapRolePermission(RoleModel role);

    Policy mapCompositePermission(RoleModel role);

    Policy mapClientScopePermission(RoleModel role);

    Resource resource(RoleModel role);

    ResourceServer resourceServer(RoleModel role);

    Policy manageUsersPolicy(ResourceServer server);

    Policy viewUsersPolicy(ResourceServer server);

    Policy rolePolicy(ResourceServer server, RoleModel role);
}
