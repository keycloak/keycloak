/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.partialimport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.ErrorResponse;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class ClientRolesPartialImport implements PartialImport {

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
        System.out.println("**** exists *****");
        System.out.println("clientId =" + clientId);
        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) return false;

        System.out.println("client=" + client);
        for (RoleModel role : client.getRoles()) {
            if (getName(roleRep).equals(role.getName())) return true;
        }

        return false;
    }

    public String existsMessage(String clientId, RoleRepresentation roleRep) {
        return "Client role '" + getName(roleRep) + "' for client '" + clientId + "' already exists.";
    }

    public ResourceType getResourceType() {
        return ResourceType.CLIENT_ROLE;
    }

    public void overwrite(RealmModel realm, KeycloakSession session, String clientId, RoleRepresentation roleRep) {
        ClientModel client = realm.getClientByClientId(clientId);
        checkForComposite(roleRep);
        RoleModel role = client.getRole(getName(roleRep));
        checkForOverwriteComposite(role);
        RealmRolesPartialImport.RoleHelper helper = new RealmRolesPartialImport.RoleHelper(realm);
        helper.updateRole(roleRep, role);
    }

    private void checkForComposite(RoleRepresentation roleRep) {
        if (roleRep.isComposite()) {
            throw new IllegalArgumentException("Composite role '" + getName(roleRep) + "' can not be partially imported");
        }
    }

    private void checkForOverwriteComposite(RoleModel role) {
        if (role.isComposite()) {
            throw new IllegalArgumentException("Composite role '" + role.getName() + "' can not be overwritten.");
        }
    }

    public void create(RealmModel realm, KeycloakSession session, String clientId, RoleRepresentation roleRep) {
        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new IllegalStateException("Client '" + clientId + "' does not exist for client role " + getName(roleRep));
        }
        checkForComposite(roleRep);
        client.addRole(getName(roleRep));
        overwrite(realm, session, clientId, roleRep);
    }

    protected void prepare(PartialImportRepresentation partialImportRep,
            RealmModel realm,
            KeycloakSession session,
            Map<String, Set<RoleRepresentation>> resourcesToOverwrite,
            Map<String, Set<RoleRepresentation>> resourcesToSkip) throws ErrorResponseException {
        Map<String, List<RoleRepresentation>> repList = getRepList(partialImportRep);
        for (String clientId : repList.keySet()) {
            resourcesToOverwrite.put(clientId, new HashSet<RoleRepresentation>());
            resourcesToSkip.put(clientId, new HashSet<RoleRepresentation>());
            for (RoleRepresentation roleRep : repList.get(clientId)) {
                if (exists(realm, session, clientId, roleRep)) {
                    switch (partialImportRep.getPolicy()) {
                        case SKIP:
                            resourcesToSkip.get(clientId).add(roleRep);
                            break;
                        case OVERWRITE:
                            resourcesToOverwrite.get(clientId).add(roleRep);
                            break;
                        default:
                            throw exists(existsMessage(clientId, roleRep));
                    }
                }
            }
        }
    }

    protected ErrorResponseException exists(String message) {
        Response error = ErrorResponse.exists(message);
        return new ErrorResponseException(error);
    }

    protected PartialImportResult overwritten(String clientId, RoleRepresentation roleRep) {
        return PartialImportResult.overwritten(getResourceType(), getCombinedName(clientId, roleRep), roleRep);
    }

    protected PartialImportResult skipped(String clientId, RoleRepresentation roleRep) {
        return PartialImportResult.skipped(getResourceType(), getCombinedName(clientId, roleRep), roleRep);
    }

    protected PartialImportResult added(String clientId, RoleRepresentation roleRep) {
        return PartialImportResult.added(getResourceType(), getCombinedName(clientId, roleRep), roleRep);
    }

    @Override
    public PartialImportResults doImport(PartialImportRepresentation partialImportRep, RealmModel realm, KeycloakSession session) throws ErrorResponseException {
        PartialImportResults results = new PartialImportResults();
        Map<String, List<RoleRepresentation>> repList = getRepList(partialImportRep);
        if ((repList == null) || repList.isEmpty()) return results;

        final Map<String, Set<RoleRepresentation>> toOverwrite = new HashMap<>();
        final Map<String, Set<RoleRepresentation>> toSkip = new HashMap<>();
        prepare(partialImportRep, realm, session, toOverwrite, toSkip);

        for (String clientId : toOverwrite.keySet()) {
            for (RoleRepresentation roleRep : toOverwrite.get(clientId)) {
                System.out.println("overwriting " + getResourceType() + " " + getCombinedName(clientId, roleRep));
                try {
                    overwrite(realm, session, clientId, roleRep);
                } catch (Exception e) {
                    throw new ErrorResponseException(ErrorResponse.error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR));
                }

                results.addResult(overwritten(clientId, roleRep));
            }
        }

        for (String clientId : toSkip.keySet()) {
            for (RoleRepresentation roleRep : toSkip.get(clientId)) {
                System.out.println("skipping " + getResourceType() + " " + getCombinedName(clientId, roleRep));
                results.addResult(skipped(clientId, roleRep));
            }
        }

        for (String clientId : repList.keySet()) {
            for (RoleRepresentation roleRep : repList.get(clientId)) {
                if (toOverwrite.get(clientId).contains(roleRep)) continue;
                if (toSkip.get(clientId).contains(roleRep)) continue;

                try {
                    System.out.println("adding " + getResourceType() + " " + getCombinedName(clientId, roleRep));
                    create(realm, session, clientId, roleRep);
                    results.addResult(added(clientId, roleRep));
                } catch (Exception e) {
                    //e.printStackTrace();
                    throw new ErrorResponseException(ErrorResponse.error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR));
                }
            }
        }

        return results;
    }

}
