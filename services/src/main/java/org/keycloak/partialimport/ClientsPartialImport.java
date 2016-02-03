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

import java.util.List;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

/**
 * PartialImport handler for Clients.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ClientsPartialImport extends AbstractPartialImport<ClientRepresentation> {

    @Override
    public List<ClientRepresentation> getRepList(PartialImportRepresentation partialImportRep) {
        return partialImportRep.getClients();
    }

    @Override
    public String getName(ClientRepresentation clientRep) {
        return clientRep.getClientId();
    }

    @Override
    public String getModelId(RealmModel realm, KeycloakSession session, ClientRepresentation clientRep) {
        return realm.getClientByClientId(getName(clientRep)).getId();
    }

    @Override
    public boolean exists(RealmModel realm, KeycloakSession session, ClientRepresentation clientRep) {
        return realm.getClientByClientId(getName(clientRep)) != null;
    }

    @Override
    public String existsMessage(ClientRepresentation clientRep) {
        return "Client id '" + getName(clientRep) + "' already exists";
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.CLIENT;
    }

    @Override
    public void remove(RealmModel realm, KeycloakSession session, ClientRepresentation clientRep) {
        ClientModel clientModel = realm.getClientByClientId(getName(clientRep));
        realm.removeClient(clientModel.getId());
    }

    @Override
    public void create(RealmModel realm, KeycloakSession session, ClientRepresentation clientRep) {
        clientRep.setId(KeycloakModelUtils.generateId());

        List<ProtocolMapperRepresentation> mappers = clientRep.getProtocolMappers();
        if (mappers != null) {
            for (ProtocolMapperRepresentation mapper : mappers) {
                mapper.setId(KeycloakModelUtils.generateId());
            }
        }

        RepresentationToModel.createClient(session, realm, clientRep, true);
    }

}
