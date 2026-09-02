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
import org.keycloak.models.ClientModel;
import org.keycloak.representations.AccessToken;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientPermissionManagement {
    public static final String MAP_ROLES_SCOPE = "map-roles";
    public static final String MAP_ROLES_CLIENT_SCOPE = "map-roles-client-scope";
    public static final String MAP_ROLES_COMPOSITE_SCOPE = "map-roles-composite";
    public static final String CONFIGURE_SCOPE = "configure";

    boolean isPermissionsEnabled(ClientModel client);

    void setPermissionsEnabled(ClientModel client, boolean enable);

    Resource resource(ClientModel client);

    Map<String, String> getPermissions(ClientModel client);

    boolean canExchangeTo(ClientModel authorizedClient, ClientModel to);

    boolean canExchangeTo(ClientModel authorizedClient, ClientModel to, AccessToken token);

    Policy exchangeToPermission(ClientModel client);

    Policy mapRolesPermission(ClientModel client);

    Policy mapRolesClientScopePermission(ClientModel client);

    Policy mapRolesCompositePermission(ClientModel client);

    Policy managePermission(ClientModel client);

    Policy configurePermission(ClientModel client);

    Policy viewPermission(ClientModel client);

    ResourceServer resourceServer(ClientModel client);
}
