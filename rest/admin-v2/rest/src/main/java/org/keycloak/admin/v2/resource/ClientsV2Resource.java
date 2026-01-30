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

package org.keycloak.admin.v2.resource;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.managers.RealmManager;


@Path("/admin-v2/realms/{realm}/clients")
public class ClientsV2Resource {

    private final KeycloakSession session;

    public ClientsV2Resource(KeycloakSession session) {
        this.session = session;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ClientRepresentation> getClients(@PathParam("realm") String realmName) {

        // TODO: Integrate with AdminPermissionEvaluator once the module dependencies are configured in pom.xml
        // Currently, 'keycloak-services' dependency is not available in this module to avoid circular refs.

        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);

        if (realm == null) {
            throw new NotFoundException("Realm not found: " + realmName);
        }

        return session.clients().getClientsStream(realm)
                .map(model -> {
                    ClientRepresentation rep = new ClientRepresentation();
                    rep.setId(model.getId());
                    rep.setClientId(model.getClientId());
                    rep.setName(model.getName());
                    rep.setDescription(model.getDescription());
                    rep.setEnabled(model.isEnabled());
                    rep.setProtocol(model.getProtocol());
                    return rep;
                })
                .collect(Collectors.toList());
    }
}
