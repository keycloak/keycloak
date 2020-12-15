/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.util;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.ScopeContainerModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ClientMappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ScopeMappedUtil {
    public static ClientMappingsRepresentation toClientMappingsRepresentation(ClientModel client, ScopeContainerModel scopeContainer) {
        Set<RoleModel> roleMappings = KeycloakModelUtils.getClientScopeMappings(client, scopeContainer);

        if (!roleMappings.isEmpty()) {

            ClientMappingsRepresentation mappings = new ClientMappingsRepresentation();
            mappings.setId(client.getId());
            mappings.setClient(client.getClientId());
            List<RoleRepresentation> roles = new LinkedList<>();
            mappings.setMappings(roles);
            for (RoleModel role : roleMappings) {
                roles.add(ModelToRepresentation.toBriefRepresentation(role));
            }

            return mappings;
        } else {
            return null;
        }
    }
}
