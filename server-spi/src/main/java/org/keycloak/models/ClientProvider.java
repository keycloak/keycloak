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
package org.keycloak.models;

import org.keycloak.provider.Provider;
import org.keycloak.storage.client.ClientLookupProvider;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientProvider extends ClientLookupProvider, Provider {
    List<ClientModel> getClients(RealmModel realm, Integer firstResult, Integer maxResults);

    List<ClientModel> getClients(RealmModel realm);

    ClientModel addClient(RealmModel realm, String clientId);

    ClientModel addClient(RealmModel realm, String id, String clientId);

    RoleModel addClientRole(RealmModel realm, ClientModel client, String name);

    RoleModel addClientRole(RealmModel realm, ClientModel client, String id, String name);

    RoleModel getClientRole(RealmModel realm, ClientModel client, String name);

    Set<RoleModel> getClientRoles(RealmModel realm, ClientModel client);

    List<ClientModel> getAlwaysDisplayInConsoleClients(RealmModel realm);

    boolean removeClient(String id, RealmModel realm);
}
