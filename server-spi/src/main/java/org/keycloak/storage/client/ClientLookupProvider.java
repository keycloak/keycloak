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
package org.keycloak.storage.client;

import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.RealmModel;

/**
 * Abstraction interface for lookoup of clients by id and clientId.  These methods required for participating in login flows.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientLookupProvider {

    /**
     * Exact search for a client by its internal ID.
     * @param realm Realm to limit the search.
     * @param id Internal ID
     * @return Model of the client, or {@code null} if no client is found.
     */
    ClientModel getClientById(RealmModel realm, String id);

    /**
     * Exact search for a client by its public client identifier.
     * @param realm Realm to limit the search for clients.
     * @param clientId String that identifies the client to the external parties.
     *   Maps to {@code client_id} in OIDC or {@code entityID} in SAML.
     * @return Model of the client, or {@code null} if no client is found.
     */
    ClientModel getClientByClientId(RealmModel realm, String clientId);

    /**
     * Case-insensitive search for clients that contain the given string in their public client identifier.
     * @param realm Realm to limit the search for clients.
     * @param clientId Searched substring of the public client
     *   identifier ({@code client_id} in OIDC or {@code entityID} in SAML.)
     * @param firstResult First result to return. Ignored if negative or {@code null}.
     * @param maxResults Maximum number of results to return. Ignored if negative or {@code null}.
     * @return Stream of ClientModel or an empty stream if no client is found. Never returns {@code null}.
     */
    Stream<ClientModel> searchClientsByClientIdStream(RealmModel realm, String clientId, Integer firstResult, Integer maxResults);

    Stream<ClientModel> searchClientsByAttributes(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults);

    default Stream<ClientModel> searchClientsByAuthenticationFlowBindingOverrides(RealmModel realm, Map<String, String> overrides, Integer firstResult, Integer maxResults) {
		Stream<ClientModel> clients = searchClientsByAttributes(realm, Map.of(), null, null)
				.filter(client -> overrides.entrySet().stream().allMatch(override -> override.getValue().equals(client.getAuthenticationFlowBindingOverrides().get(override.getKey()))));
		if (firstResult != null && firstResult >= 0) {
			clients = clients.skip(firstResult);
		}
		if (maxResults != null && maxResults >= 0 ) {
			clients = clients.limit(maxResults);
		}
		return clients;
    }

    /**
     * Return all default scopes (if {@code defaultScope} is {@code true}) or all optional scopes (if {@code defaultScope} is {@code false}) linked with the client
     *
     * @param realm Realm
     * @param client Client
     * @param defaultScopes if true default scopes, if false optional scopes, are returned
     * @return map where key is the name of the clientScope, value is particular clientScope. Returns empty map if no scopes linked (never returns null).
     */
    Map<String, ClientScopeModel> getClientScopes(RealmModel realm, ClientModel client, boolean defaultScopes);
}
