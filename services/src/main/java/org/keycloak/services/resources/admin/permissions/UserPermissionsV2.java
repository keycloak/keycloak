/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.resources.admin.permissions;

import java.util.Map;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.models.KeycloakSession;

class UserPermissionsV2 extends UserPermissions {

    UserPermissionsV2(KeycloakSession session, AuthorizationProvider authz, MgmtPermissionsV2 root) {
        super(session, authz, root);
    }

    @Override
    public boolean isPermissionsEnabled() {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public void setPermissionsEnabled(boolean enable) {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Map<String, String> getPermissions() {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Resource resource() {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy managePermission() {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy viewPermission() {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy manageGroupMembershipPermission() {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy mapRolesPermission() {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy adminImpersonatingPermission() {
        throw new UnsupportedOperationException("Not supported in V2");
    }

    @Override
    public Policy userImpersonatedPermission() {
        throw new UnsupportedOperationException("Not supported in V2");
    }
}
