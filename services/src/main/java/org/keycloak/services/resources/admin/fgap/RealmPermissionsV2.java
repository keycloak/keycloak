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

import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.models.ClientModel;

class RealmPermissionsV2 extends RealmPermissions {

    public RealmPermissionsV2(MgmtPermissions root) {
        super(root);
    }

    @Override
    public boolean canManageAuthorizationDefault(ResourceServer resourceServer) {
        if (super.canManageAuthorizationDefault(resourceServer)) {
            return true;
        }

        return root.clients().canManage(getClient(resourceServer));
    }

    @Override
    public boolean canViewAuthorizationDefault(ResourceServer resourceServer) {
        if (super.canViewAuthorizationDefault(resourceServer)) {
            return true;
        }

        return root.clients().canView(getClient(resourceServer));
    }

    private ClientModel getClient(ResourceServer resourceServer) {
        ClientModel client = root.session.clients().getClientById(root.realm, resourceServer.getId());
        return client;
    }
}
