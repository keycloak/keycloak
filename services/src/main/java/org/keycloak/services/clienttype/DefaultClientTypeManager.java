/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.services.clienttype;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.client.clienttype.ClientType;
import org.keycloak.client.clienttype.ClientTypeException;
import org.keycloak.client.clienttype.ClientTypeManager;
import org.keycloak.client.clienttype.ClientTypeProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ClientTypeRepresentation;
import org.keycloak.representations.idm.ClientTypesRepresentation;
import org.keycloak.services.clienttype.client.TypeAwareClientModelDelegate;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultClientTypeManager implements ClientTypeManager {

    private static final Logger logger = Logger.getLogger(DefaultClientTypeManager.class);

    // Realm attribute where are client types saved
    private static final String CLIENT_TYPE_REALM_ATTRIBUTE = "client-types";

    private final KeycloakSession session;
    private final List<ClientTypeRepresentation> globalClientTypes;

    public DefaultClientTypeManager(KeycloakSession session, List<ClientTypeRepresentation> globalClientTypes) {
        this.session = session;
        this.globalClientTypes = globalClientTypes;
    }


    @Override
    public ClientTypesRepresentation getClientTypes(RealmModel realm) throws ClientTypeException {
        String asStr = realm.getAttribute(CLIENT_TYPE_REALM_ATTRIBUTE);
        ClientTypesRepresentation result;
        if (asStr == null) {
            result = new ClientTypesRepresentation(new ArrayList<>(), null);
            result.setGlobalClientTypes(globalClientTypes);
        } else {
            try {
                // Skip validation here for performance reasons
                result = JsonSerialization.readValue(asStr, ClientTypesRepresentation.class);
                result.setGlobalClientTypes(globalClientTypes);
            } catch (IOException ioe) {
                logger.errorf("Failed to load client type for realm '%s'.", realm.getName());
                throw ClientTypeException.Message.CLIENT_TYPE_FAILED_TO_LOAD.exception(ioe);
            }
        }
        return result;
    }


    @Override
    public void updateClientTypes(RealmModel realm, ClientTypesRepresentation clientTypes) throws ClientTypeException {
        // Validate before save
        List<ClientTypeRepresentation> validatedClientTypes = validateAndCastConfiguration(session, clientTypes.getRealmClientTypes(), globalClientTypes);

        ClientTypesRepresentation noGlobalsCopy = new ClientTypesRepresentation(validatedClientTypes, null);
        try {
            String asStr = JsonSerialization.writeValueAsString(noGlobalsCopy);
            realm.setAttribute(CLIENT_TYPE_REALM_ATTRIBUTE, asStr);
        } catch (IOException ioe) {
            logger.errorf("Failed to load global client type.");
            throw ClientTypeException.Message.CLIENT_TYPE_FAILED_TO_LOAD.exception(ioe);
        }
    }


    @Override
    public ClientType getClientType(RealmModel realm, String typeName) throws ClientTypeException {
        ClientTypesRepresentation clientTypes = getClientTypes(realm);
        ClientTypeRepresentation clientType = getClientTypeByName(clientTypes, typeName);
        if (clientType == null) {
            logger.errorf("Referenced client type '%s' not found", typeName);
            throw ClientTypeException.Message.CLIENT_TYPE_NOT_FOUND.exception();
        }

        ClientType parent = null;
        if (clientType.getParent() != null) {
            parent = getClientType(realm, clientType.getParent());
        }

        ClientTypeProvider provider = session.getProvider(ClientTypeProvider.class, clientType.getProvider());
        return provider.getClientType(clientType, parent);
    }

    @Override
    public ClientModel augmentClient(ClientModel client) throws ClientTypeException {
        if (client.getType() == null) {
            return client;
        }

        try {
            ClientType clientType = getClientType(client.getRealm(), client.getType());
            return new TypeAwareClientModelDelegate(clientType, () -> client);
        } catch(ClientTypeException cte) {
            logger.errorf("Could not augment client, %s, due to client type exception: %s",
                    client, cte);
            throw cte;
        }
    }

    static List<ClientTypeRepresentation> validateAndCastConfiguration(KeycloakSession session, List<ClientTypeRepresentation> clientTypes, List<ClientTypeRepresentation> globalTypes) {
        Set<String> usedNames = globalTypes.stream()
                .map(ClientTypeRepresentation::getName)
                .collect(Collectors.toSet());

        return clientTypes.stream()
                .map(clientType -> validateAndCastConfiguration(session, clientType, usedNames))
                .collect(Collectors.toList());
    }


    // TODO:client-types some javadoc or comment about how this method works
    private static ClientTypeRepresentation validateAndCastConfiguration(KeycloakSession session, ClientTypeRepresentation clientType, Set<String> currentNames) {
        ClientTypeProvider clientTypeProvider = session.getProvider(ClientTypeProvider.class, clientType.getProvider());
        if (clientTypeProvider == null) {
            logger.errorf("Did not find client type provider '%s' for the client type '%s'", clientType.getProvider(), clientType.getName());
            throw ClientTypeException.Message.INVALID_CLIENT_TYPE_PROVIDER.exception();
        }

        // Validate name is not duplicated
        if (currentNames.contains(clientType.getName())) {
            logger.errorf("Duplicated client type name '%s'", clientType.getName());
            throw ClientTypeException.Message.DUPLICATE_CLIENT_TYPE.exception();
        }

        clientType = clientTypeProvider.checkClientTypeConfig(clientType);
        currentNames.add(clientType.getName());

        return clientType;
    }


    private ClientTypeRepresentation getClientTypeByName(ClientTypesRepresentation clientTypes, String clientTypeName) {
        // Search realm clientTypes
        if (clientTypes.getRealmClientTypes() != null) {
            for (ClientTypeRepresentation clientType : clientTypes.getRealmClientTypes()) {
                if (clientTypeName.equals(clientType.getName())) {
                    return clientType;
                }
            }
        }
        // Search global clientTypes
        if (clientTypes.getGlobalClientTypes() != null) {
            for (ClientTypeRepresentation clientType : clientTypes.getGlobalClientTypes()) {
                if (clientTypeName.equals(clientType.getName())) {
                    return clientType;
                }
            }
        }
        return null;
    }
}
