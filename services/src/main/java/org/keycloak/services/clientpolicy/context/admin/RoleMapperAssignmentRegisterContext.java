/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.clientpolicy.context.admin;

import java.util.List;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleMapperModel;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.resources.admin.AdminAuth;

/**
 * Fired when role assignments are being granted to a user or group through the admin REST API.
 *
 * @see ClientPolicyEvent#REGISTER_ROLE_MAPPING
 */
public class RoleMapperAssignmentRegisterContext extends AbstractRoleMapperAssignmentContext {

    public RoleMapperAssignmentRegisterContext(RoleMapperModel roleMapper,
                                               ClientModel roleContainerClient,
                                               List<RoleRepresentation> roles,
                                               AdminAuth adminAuth) {
        super(roleMapper, roleContainerClient, roles, adminAuth);
    }

    /** @return {@link ClientPolicyEvent#REGISTER_ROLE_MAPPING} */
    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.REGISTER_ROLE_MAPPING;
    }
}
