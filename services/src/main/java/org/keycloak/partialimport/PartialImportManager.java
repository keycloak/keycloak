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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ClientResource;
import org.keycloak.services.resources.admin.IdentityProviderResource;
import org.keycloak.services.resources.admin.UsersResource;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class PartialImportManager {
    private List<PartialImport> partialImports = new ArrayList<>();

    private final PartialImportRepresentation rep;
    private final KeycloakSession session;
    private final RealmModel realm;
    private final UriInfo uriInfo;
    private final AdminEventBuilder adminEvent;

    private final Set<UserRepresentation> usersToOverwrite = new HashSet<>();
    private final Set<ClientRepresentation> clientsToOverwrite = new HashSet<>();
    private final Set<IdentityProviderRepresentation> idpsToOverwrite = new HashSet<>();

    private int added = 0;
    private int skipped = 0;
    private int overwritten = 0;

    public PartialImportManager(PartialImportRepresentation rep, KeycloakSession session, RealmModel realm,
                         UriInfo uriInfo, AdminEventBuilder adminEvent) {
        this.rep = rep;
        this.session = session;
        this.realm = realm;
        this.uriInfo = uriInfo;
        this.adminEvent = adminEvent;

        partialImports.add(new UsersPartialImport());
        partialImports.add(new ClientsPartialImport());
        partialImports.add(new IdentityProvidersPartialImport());
        partialImports.add(new RealmRolesPartialImport());
        partialImports.add(new ClientRolesPartialImport());
    }

    public Response saveResources() {

        PartialImportResults results = new PartialImportResults();

        for (PartialImport partialImport : partialImports) {
            try {
                results.addAllResults(partialImport.doImport(rep, realm, session));
            } catch (ErrorResponseException error) {
                if (session.getTransaction().isActive()) session.getTransaction().setRollbackOnly();
                return error.getResponse();
            }
        }

        for (PartialImportResult result : results.getResults()) {
            switch (result.getAction()) {
                case ADDED : addedEvent(result); break;
                case OVERWRITTEN: overwrittenEvent(result); break;
            }
        }

        if (session.getTransaction().isActive()) {
            session.getTransaction().commit();
        }

        return Response.ok(results).build();
    }

    private void addedEvent(PartialImportResult result) {
        adminEvent.operation(OperationType.CREATE)
                  .resourcePath(uriInfo)
                  .representation(result.getRepresentation())
                  .success();
    };

    private void overwrittenEvent(PartialImportResult result) {
        adminEvent.operation(OperationType.UPDATE)
                  .resourcePath(uriInfo)
                  .representation(result.getRepresentation())
                  .success();
    }

       /* Response response = prepareForExistingResources();
        if (response != null) return response;

        response = saveUsers();
        if (response != null) {
            session.getTransaction().rollback();
            return response;
        }

        response = saveClients();
        if (response != null) {
            session.getTransaction().rollback();
            return response;
        }

        response = saveIdps();
        if (response != null) {
            session.getTransaction().rollback();
            return response;
        }

        if (session.getTransaction().isActive()) {
            session.getTransaction().commit();
        }


        return Response.ok(resultsMap()).build();*/
    //}
/*
    private Map resultsMap() {
        Map<String, Integer> results = new HashMap<>();
        results.put("added", added);
        results.put("skipped", skipped);
        results.put("overwritten", overwritten);
        return results;
    }

    // returns an error response or null
    private Response prepareForExistingResources() {

        if (rep.hasUsers()) {
            Response response = prepareUsers();
            if (response != null) return response;
        }

        if (rep.hasClients()) {
            Response response = prepareClients();
            if (response != null) return response;
        }

        if (rep.hasIdps()) {
            Response response = prepareIdps();
            if (response != null) return response;
        }

        return null;
    }

    // returns an error response or null
    private Response prepareClients() {
        Set<ClientRepresentation> toSkip = new HashSet<>();
        for (ClientRepresentation client : rep.getClients()) {
            if (clientExists(client)) {
                switch (rep.getPolicy()) {
                    case SKIP: toSkip.addResult(client); break;
                    case OVERWRITE: clientsToOverwrite.addResult(client); break;
                    default: return ErrorResponse.exists("Client id '" + client.getClientId() + "' already exists");
                }
            }
        }

        for (ClientRepresentation client : toSkip) {
            rep.getClients().remove(client);
            skipped(client);
        }

        return null;
    }

    private boolean clientExists(ClientRepresentation rep) {
        return realm.getClientByClientId(rep.getClientId()) != null;
    }

    // returns an error response or null
    private Response saveClients() {
        if (!rep.hasClients()) return null;

        for (ClientRepresentation client : clientsToOverwrite) {
            ClientModel clientModel = realm.getClientByClientId(client.getClientId());
            ClientResource.updateClientFromRep(client, clientModel, session);
            adminEvent.operation(OperationType.UPDATE).resourcePath(uriInfo).representation(client).success();
            overwritten(client);
        }

        for (ClientRepresentation client : rep.getClients()) {
            if (clientsToOverwrite.contains(client)) continue;

            try {
                RepresentationToModel.createClient(session, realm, client, true);
                added(client);
            } catch (Exception e) {
                if (session.getTransaction().isActive()) session.getTransaction().setRollbackOnly();
                return ErrorResponse.error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
            }
        }

        return null;
    }

    // returns an error response or null
    private Response prepareIdps() {
        Set<IdentityProviderRepresentation> toSkip = new HashSet<>();
        for (IdentityProviderRepresentation idp : rep.getIdentityProviders()) {
            if (idpExists(idp)) {
                switch (rep.getPolicy()) {
                    case SKIP: toSkip.addResult(idp); break;
                    case OVERWRITE: idpsToOverwrite.addResult(idp); break;
                    default: return ErrorResponse.exists("Identity Provider '" + idp.getAlias() + "' already exists");
                }
            }
        }

        for (IdentityProviderRepresentation idp : toSkip) {
            rep.getIdentityProviders().remove(idp);
            skipped(idp);
        }

        return null;
    }

    private boolean idpExists(IdentityProviderRepresentation rep) {
        return realm.getIdentityProviderByAlias(rep.getAlias()) != null;
    }

    // returns an error response or null
    private Response saveIdps() {
        if (!rep.hasIdps()) return null;

        for (IdentityProviderRepresentation idp : idpsToOverwrite) {
            IdentityProviderResource.updateIdpFromRep(idp, realm, session);
            overwritten(idp);
        }

        for (IdentityProviderRepresentation idp : rep.getIdentityProviders()) {
            if (idpsToOverwrite.contains(idp)) continue;

            try {
                IdentityProviderModel identityProvider = RepresentationToModel.toModel(idp);
                realm.addIdentityProvider(identityProvider);
                added(idp);
            } catch (Exception e) {
                if (session.getTransaction().isActive()) session.getTransaction().setRollbackOnly();
                return ErrorResponse.error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
            }
        }

        return null;
    }

    // returns an error response or null
    private Response prepareUsers() {
        Set<UserRepresentation> toSkip = new HashSet<>();
        for (UserRepresentation user : rep.getUsers()) {
            if (session.users().getUserByUsername(user.getUsername(), realm) != null) {
                switch (rep.getPolicy()) {
                    case SKIP: toSkip.addResult(user); break;
                    case OVERWRITE: usersToOverwrite.addResult(user); break;
                    default: return ErrorResponse.exists("User '" + user.getUsername() + "' already exists");
                }
            }
            if ((user.getEmail() != null) && (session.users().getUserByEmail(user.getEmail(), realm) != null)) {
                switch (rep.getPolicy()) {
                    case SKIP: toSkip.addResult(user); break;
                    case OVERWRITE: usersToOverwrite.addResult(user); break;
                    default: FAIL: return ErrorResponse.exists("User email '" + user.getEmail() + "' already exists");
                }
            }
        }

        for (UserRepresentation user : toSkip) {
            rep.getUsers().remove(user);
            skipped(user);
        }

        return null;
    }

    // returns an error response or null
    private Response saveUsers() {
        if (!rep.hasUsers()) return null;

        for (UserRepresentation user: usersToOverwrite) {
            System.out.println("overwriting user " + user.getUsername());
            UserModel userModel = session.users().getUserByUsername(user.getUsername(), realm);
            UsersResource.updateUserFromRep(userModel, user, null, realm, session);
            overwritten(user);
        }

        for (UserRepresentation user : rep.getUsers()) {
            if (usersToOverwrite.contains(user)) continue;
            try {
                System.out.println("saving user " + user.getUsername());
                Map<String, ClientModel> apps = realm.getClientNameMap();
                UserModel userModel = RepresentationToModel.createUser(session, realm, user, apps);
                added(user);
            } catch (Exception e) {
                //e.printStackTrace();
                if (session.getTransaction().isActive()) session.getTransaction().setRollbackOnly();
                return ErrorResponse.error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
            }
        }

        return null;
    }
*/

}
