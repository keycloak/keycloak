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

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class handles both realm roles and client roles.  It delegates to
 * RealmRolesPartialImport and ClientRolesPartialImport, which are no longer used
 * directly by the PartialImportManager.
 *
 * The strategy is to utilize RepresentationToModel.importRoles().  That way,
 * the complex code for bulk creation of roles is kept in one place.  To do this, the
 * logic for skip needs to remove the roles that are going to be skipped so that
 * importRoles() doesn't know about them.  The logic for overwrite needs to delete
 * the overwritten roles before importRoles() is called.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class RolesPartialImport implements PartialImport<RolesRepresentation> {

    private Set<RoleRepresentation> realmRolesToOverwrite;
    private Set<RoleRepresentation> realmRolesToSkip;

    private Map<String, Set<RoleRepresentation>> clientRolesToOverwrite;
    private Map<String, Set<RoleRepresentation>> clientRolesToSkip;

    private final RealmRolesPartialImport realmRolesPI = new RealmRolesPartialImport();
    private final ClientRolesPartialImport clientRolesPI = new ClientRolesPartialImport();

    @Override
    public void prepare(PartialImportRepresentation rep, RealmModel realm, KeycloakSession session) throws ErrorResponseException {
        prepareRealmRoles(rep, realm, session);
        prepareClientRoles(rep, realm, session);
    }

    private void prepareRealmRoles(PartialImportRepresentation rep, RealmModel realm, KeycloakSession session) throws ErrorResponseException {
        if (!rep.hasRealmRoles()) return;

        realmRolesPI.prepare(rep, realm, session);
        this.realmRolesToOverwrite = realmRolesPI.getToOverwrite();
        this.realmRolesToSkip = realmRolesPI.getToSkip();
    }

    private void prepareClientRoles(PartialImportRepresentation rep, RealmModel realm, KeycloakSession session) throws ErrorResponseException {
        if (!rep.hasClientRoles()) return;

        clientRolesPI.prepare(rep, realm, session);
        this.clientRolesToOverwrite = clientRolesPI.getToOverwrite();
        this.clientRolesToSkip = clientRolesPI.getToSkip();
    }

    @Override
    public void removeOverwrites(RealmModel realm, KeycloakSession session) {
        deleteClientRoleOverwrites(realm);
        deleteRealmRoleOverwrites(realm, session);
    }

    @Override
    public PartialImportResults doImport(PartialImportRepresentation rep, RealmModel realm, KeycloakSession session) throws ErrorResponseException {
        PartialImportResults results = new PartialImportResults();
        if (!rep.hasRealmRoles() && !rep.hasClientRoles()) return results;

        // finalize preparation and add results for skips
        removeRealmRoleSkips(results, rep, realm, session);
        removeClientRoleSkips(results, rep, realm);
        if (rep.hasRealmRoles()) setUniqueIds(rep.getRoles().getRealm());
        if (rep.hasClientRoles()) setUniqueIds(rep.getRoles().getClient());

        try {
            RepresentationToModel.importRoles(rep.getRoles(), realm);
        } catch (Exception e) {
            ServicesLogger.LOGGER.roleImportError(e);
            throw new ErrorResponseException(ErrorResponse.error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR));
        }

        // add "add" results for new roles created
        realmRoleAdds(results, rep, realm, session);
        clientRoleAdds(results, rep, realm);

        // add "overwritten" results for roles overwritten
        addResultsForOverwrittenRealmRoles(results, realm, session);
        addResultsForOverwrittenClientRoles(results, realm);

        return results;
    }

    private void setUniqueIds(List<RoleRepresentation> realmRoles) {
        for (RoleRepresentation realmRole : realmRoles) {
            realmRole.setId(KeycloakModelUtils.generateId());
        }
    }

    private void setUniqueIds(Map<String, List<RoleRepresentation>> clientRoles) {
        for (String clientId : clientRoles.keySet()) {
            for (RoleRepresentation clientRole : clientRoles.get(clientId)) {
                clientRole.setId(KeycloakModelUtils.generateId());
            }
        }
    }

    private void removeRealmRoleSkips(PartialImportResults results,
                                      PartialImportRepresentation rep,
                                      RealmModel realm,
                                      KeycloakSession session) {
        if (isEmpty(realmRolesToSkip)) return;

        for (RoleRepresentation roleRep : realmRolesToSkip) {
            rep.getRoles().getRealm().remove(roleRep);
            String modelId = realmRolesPI.getModelId(realm, session, roleRep);
            results.addResult(realmRolesPI.skipped(modelId, roleRep));
        }
    }

    private void removeClientRoleSkips(PartialImportResults results,
                                       PartialImportRepresentation rep,
                                       RealmModel realm) {
        if (isEmpty(clientRolesToSkip)) return;

        for (String clientId : clientRolesToSkip.keySet()) {
            for (RoleRepresentation roleRep : clientRolesToSkip.get(clientId)) {
                rep.getRoles().getClient().get(clientId).remove(roleRep);
                String modelId = clientRolesPI.getModelId(realm, clientId);
                results.addResult(clientRolesPI.skipped(clientId, modelId, roleRep));
            }
        }
    }

    private void deleteRealmRoleOverwrites(RealmModel realm, KeycloakSession session) {
        if (isEmpty(realmRolesToOverwrite)) return;

        for (RoleRepresentation roleRep : realmRolesToOverwrite) {
            realmRolesPI.remove(realm, session, roleRep);
        }
    }

    private void addResultsForOverwrittenRealmRoles(PartialImportResults results, RealmModel realm, KeycloakSession session) {
        if (isEmpty(realmRolesToOverwrite)) return;

        for (RoleRepresentation roleRep : realmRolesToOverwrite) {
            String modelId = realmRolesPI.getModelId(realm, session, roleRep);
            results.addResult(realmRolesPI.overwritten(modelId, roleRep));
        }
    }

    private void deleteClientRoleOverwrites(RealmModel realm) {
        if (isEmpty(clientRolesToOverwrite)) return;

        for (String clientId : clientRolesToOverwrite.keySet()) {
            for (RoleRepresentation roleRep : clientRolesToOverwrite.get(clientId)) {
                clientRolesPI.deleteRole(realm, clientId, roleRep);
            }
        }
    }

    private void addResultsForOverwrittenClientRoles(PartialImportResults results, RealmModel realm) {
        if (isEmpty(clientRolesToOverwrite)) return;

        for (String clientId : clientRolesToOverwrite.keySet()) {
            for (RoleRepresentation roleRep : clientRolesToOverwrite.get(clientId)) {
                String modelId = clientRolesPI.getModelId(realm, clientId);
                results.addResult(clientRolesPI.overwritten(clientId, modelId, roleRep));
            }
        }
    }

    private boolean isEmpty(Set set) {
        return (set == null) || (set.isEmpty());
    }

    private boolean isEmpty(Map map) {
        return (map == null) || (map.isEmpty());
    }

    private void realmRoleAdds(PartialImportResults results,
                               PartialImportRepresentation rep,
                               RealmModel realm,
                               KeycloakSession session) {
        if (!rep.hasRealmRoles()) return;

        for (RoleRepresentation roleRep : rep.getRoles().getRealm()) {
            if (realmRolesToOverwrite.contains(roleRep)) continue;
            if (realmRolesToSkip.contains(roleRep)) continue;

            String modelId = realmRolesPI.getModelId(realm, session, roleRep);
            results.addResult(realmRolesPI.added(modelId, roleRep));
        }
    }

    private void clientRoleAdds(PartialImportResults results,
                                PartialImportRepresentation rep,
                                RealmModel realm) {
        if (!rep.hasClientRoles()) return;

        Map<String, List<RoleRepresentation>> repList = clientRolesPI.getRepList(rep);
        for (String clientId : repList.keySet()) {
            for (RoleRepresentation roleRep : repList.get(clientId)) {
                if (clientRolesToOverwrite.get(clientId).contains(roleRep)) continue;
                if (clientRolesToSkip.get(clientId).contains(roleRep)) continue;

                String modelId = clientRolesPI.getModelId(realm, clientId);
                results.addResult(clientRolesPI.added(clientId, modelId, roleRep));
            }
        }
    }
}
