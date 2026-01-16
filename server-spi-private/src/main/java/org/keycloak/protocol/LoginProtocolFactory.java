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

package org.keycloak.protocol;

import java.util.Map;

import jakarta.ws.rs.WebApplicationException;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface LoginProtocolFactory extends ProviderFactory<LoginProtocol> {
    /**
     * List of built in protocol mappers that can be used to apply to clients.
     *
     * @return
     */
    Map<String, ProtocolMapperModel> getBuiltinMappers();


    Object createProtocolEndpoint(KeycloakSession session, EventBuilder event);


    /**
     * Called when new realm is created
     *
     * @param newRealm
     * @param addScopesToExistingClients If true, then existing realm clients will be updated (created realm default scopes will be added to them)
     */
    void createDefaultClientScopes(RealmModel newRealm, boolean addScopesToExistingClients);


    /**
     * Setup default values for new clients. This expects that the representation has already set up the client
     *
     * @param rep
     * @param newClient
     */
    void setupClientDefaults(ClientRepresentation rep, ClientModel newClient);

    /**
     * Add default values to {@link ClientScopeRepresentation}s that refer to the specific login-protocol
     */
    void addClientScopeDefaults(ClientScopeRepresentation clientScope);

    /**
     * Invoked during client-scope creation or update to add additional validation hooks specific to target protocol. May throw errorResponseException in case
     *
     * @param session Keycloak session
     * @param clientScope client scope to create or update
     * @throws WebApplicationException or some of it's subclass if validation fails
     */
    default void validateClientScope(KeycloakSession session, ClientScopeRepresentation clientScope) throws WebApplicationException {
    }

    /**
     * Test if the clientScope is valid for particular client. Usually called during protocol requests
     */
    default boolean isValidClientScope(KeycloakSession session, ClientModel client, ClientScopeModel clientScope) {
        return true;
    }
}
