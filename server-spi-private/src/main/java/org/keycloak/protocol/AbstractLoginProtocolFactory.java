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

import org.keycloak.Config;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractLoginProtocolFactory implements LoginProtocolFactory {

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(new ProviderEventListener() {
            @Override
            public void onEvent(ProviderEvent event) {
                if (event instanceof RealmModel.ClientCreationEvent) {
                    ClientModel client = ((RealmModel.ClientCreationEvent)event).getCreatedClient();
                    addDefaultClientScopes(client.getRealm(), client);
                    addDefaults(client);
                }
            }
        });
    }


    @Override
    public void createDefaultClientScopes(RealmModel newRealm, boolean addScopesToExistingClients) {
        createDefaultClientScopesImpl(newRealm);

        // Create default client scopes for realm built-in clients too
        if (addScopesToExistingClients) {
            addDefaultClientScopes(newRealm, newRealm.getClients());
        }
    }

    /**
     * Impl should create default client scopes. This is called usually when new realm is created
     */
    protected abstract void createDefaultClientScopesImpl(RealmModel newRealm);


    protected void addDefaultClientScopes(RealmModel realm, ClientModel newClient) {
        addDefaultClientScopes(realm, Arrays.asList(newClient));
    }

    protected void addDefaultClientScopes(RealmModel realm, List<ClientModel> newClients) {
        Set<ClientScopeModel> defaultClientScopes = realm.getDefaultClientScopes(true).stream()
                .filter(clientScope -> getId().equals(clientScope.getProtocol()))
                .collect(Collectors.toSet());
        for (ClientModel newClient : newClients) {
            newClient.addClientScopes(defaultClientScopes, true);
        }

        Set<ClientScopeModel> nonDefaultClientScopes = realm.getDefaultClientScopes(false).stream()
                .filter(clientScope -> getId().equals(clientScope.getProtocol()))
                .collect(Collectors.toSet());
        for (ClientModel newClient : newClients) {
            newClient.addClientScopes(nonDefaultClientScopes, false);
        }
    }

    protected abstract void addDefaults(ClientModel realm);

    @Override
    public void close() {

    }
}
