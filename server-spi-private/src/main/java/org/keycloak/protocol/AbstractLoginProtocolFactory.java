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
            for (ClientModel client : newRealm.getClients()) {
                addDefaultClientScopes(newRealm, client);
            }
        }
    }

    /**
     * Impl should create default client scopes. This is called usually when new realm is created
     */
    protected abstract void createDefaultClientScopesImpl(RealmModel newRealm);


    protected void addDefaultClientScopes(RealmModel realm, ClientModel newClient) {
        for (ClientScopeModel clientScope : realm.getDefaultClientScopes(true)) {
            if (getId().equals(clientScope.getProtocol())) {
                newClient.addClientScope(clientScope, true);
            }
        }
        for (ClientScopeModel clientScope : realm.getDefaultClientScopes(false)) {
            if (getId().equals(clientScope.getProtocol())) {
                newClient.addClientScope(clientScope, false);
            }
        }
    }

    protected abstract void addDefaults(ClientModel realm);

    @Override
    public void close() {

    }
}
