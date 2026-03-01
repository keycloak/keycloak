/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

sealed interface ModelRecord {

    record ClientModelRecord(ClientModel client) implements ModelRecord {
        @Override
        public String getResourceType() {
            return AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE;
        }

        @Override
        public String getId() {
            return client == null ? null : client.getId();
        }
    }

    record GroupModelRecord(GroupModel group) implements ModelRecord {
        @Override
        public String getResourceType() {
            return AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
        }

        @Override
        public String getId() {
            return group == null ? null : group.getId();
        }
    }

    record RoleModelRecord(RoleModel role) implements ModelRecord {
        @Override
        public String getResourceType() {
            return AdminPermissionsSchema.ROLES_RESOURCE_TYPE;
        }

        @Override
        public String getId() {
            return role == null ? null : role.getId();
        }
    }

    record UserModelRecord(UserModel user) implements ModelRecord {
        @Override
        public String getResourceType() {
            return AdminPermissionsSchema.USERS_RESOURCE_TYPE;
        }

        @Override
        public String getId() {
            return user == null ? null : user.getId();
        }
    }

    String getId();
    String getResourceType();
}
