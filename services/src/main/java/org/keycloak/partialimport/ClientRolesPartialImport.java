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
package org.keycloak.partialimport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;

/**
 * Partial Import handler for Client Roles.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ClientRolesPartialImport {
    private final Map<String, Set<RoleRepresentation>> toOverwrite = new HashMap<>();
    private final Map<String, Set<RoleRepresentation>> toSkip = new HashMap<>();

    public Map<String, Set<RoleRepresentation>> getToOverwrite() {
        return this.toOverwrite;
    }

    public Map<String, Set<RoleRepresentation>> getToSkip() {
        return this.toSkip;
    }

    public Map<String, List<RoleRepresentation>> getRepList(PartialImportRepresentation partialImportRep) {
        if (partialImportRep.getRoles() == null) return null;
        return partialImportRep.getRoles().getClient();
    }

    public String getName(RoleRepresentation roleRep) {
        if (roleRep.getName() == null)
            throw new IllegalStateException("Client role to import does not have a name");
        return roleRep.getName();
    }

    public String getCombinedName(String clientId, RoleRepresentation roleRep) {
        return clientId + "-->" + getName(roleRep);
    }

    public boolean exists(RealmModel realm, KeycloakSession session, String clientId, RoleRepresentation roleRep) {
        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) return false;

        return client.getRolesStream().anyMatch(role -> Objects.equals(getName(roleRep), role.getName()));
    }

    // check if client currently exists or will exists as a result of this partial import
    private boolean clientExists(PartialImportRepresentation partialImportRep, RealmModel realm, String clientId) {
        if (realm.getClientByClientId(clientId) != null) return true;

        if (partialImportRep.getClients() == null) return false;

        for (ClientRepresentation client : partialImportRep.getClients()) {
            if (clientId.equals(client.getClientId())) return true;
        }

        return false;
    }

    public String existsMessage(String clientId, RoleRepresentation roleRep) {
        return "Client role '" + getName(roleRep) + "' for client '" + clientId + "' already exists.";
    }

    public ResourceType getResourceType() {
        return ResourceType.CLIENT_ROLE;
    }

    public void deleteRole(RealmModel realm, String clientId, RoleRepresentation roleRep) {
        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            // client might have been removed as part of this partial import
            return;
        }
        RoleModel role = client.getRole(getName(roleRep));
        if (role == null) {
            // role might not exist if client was just created as part of the
            // partial import
            return;
        }
        client.removeRole(role);
    }

    public void prepare(PartialImportRepresentation partialImportRep, RealmModel realm, KeycloakSession session) {
        Map<String, List<RoleRepresentation>> repList = getRepList(partialImportRep);
        if (repList == null || repList.isEmpty()) return;

        for (var entry : repList.entrySet()) {
            String clientId = entry.getKey();
            if (!clientExists(partialImportRep, realm, clientId)) {
                throw noClientFound(clientId);
            }

            toOverwrite.put(clientId, new HashSet<>());
            toSkip.put(clientId, new HashSet<>());
            for (RoleRepresentation roleRep : entry.getValue()) {
                if (exists(realm, session, clientId, roleRep)) {
                    switch (partialImportRep.getPolicy()) {
                        case SKIP:
                            toSkip.get(clientId).add(roleRep);
                            break;
                        case OVERWRITE:
                            toOverwrite.get(clientId).add(roleRep);
                            break;
                        default:
                            throw exists(existsMessage(clientId, roleRep));
                    }
                }
            }
        }
    }

    protected ErrorResponseException exists(String message) {
        throw ErrorResponse.exists(message);
    }

    protected ErrorResponseException noClientFound(String clientId) {
        String message = "Can not import client roles for nonexistent client named " + clientId;
        throw ErrorResponse.error(message, Response.Status.PRECONDITION_FAILED);
    }

    public PartialImportResult overwritten(String clientId, String modelId, RoleRepresentation roleRep) {
        return PartialImportResult.overwritten(getResourceType(), getCombinedName(clientId, roleRep), modelId, roleRep);
    }

    public PartialImportResult skipped(String clientId, String modelId, RoleRepresentation roleRep) {
        return PartialImportResult.skipped(getResourceType(), getCombinedName(clientId, roleRep), modelId, roleRep);
    }

    public PartialImportResult added(String clientId, String modelId, RoleRepresentation roleRep) {
        return PartialImportResult.added(getResourceType(), getCombinedName(clientId, roleRep), modelId, roleRep);
    }

    public String getModelId(RealmModel realm, String clientId) {
        return realm.getClientByClientId(clientId).getId();
    }
}
