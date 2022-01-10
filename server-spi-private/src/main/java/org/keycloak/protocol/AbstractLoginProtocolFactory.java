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

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                if (event instanceof ClientModel.ClientProtocolUpdatedEvent) {
                    ClientModel client = ((ClientModel.ClientProtocolUpdatedEvent)event).getClient();
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
            addDefaultClientScopes(newRealm, newRealm.getClientsStream());
        }
    }

    /**
     * Impl should create default client scopes. This is called usually when new realm is created
     */
    protected abstract void createDefaultClientScopesImpl(RealmModel newRealm);


    protected void addDefaultClientScopes(RealmModel realm, ClientModel newClient) {
        addDefaultClientScopes(realm, Stream.of(newClient));
    }

    protected void addDefaultClientScopes(RealmModel realm, Stream<ClientModel> newClients) {
        Set<ClientScopeModel> defaultClientScopes = realm.getDefaultClientScopesStream(true)
                .filter(clientScope -> Objects.equals(getId(), clientScope.getProtocol()))
                .collect(Collectors.toSet());

        Set<ClientScopeModel> nonDefaultClientScopes = realm.getDefaultClientScopesStream(false)
                .filter(clientScope -> Objects.equals(getId(), clientScope.getProtocol()))
                .collect(Collectors.toSet());

        Consumer<ClientModel> addDefault = c -> c.addClientScopes(defaultClientScopes, true);
        Consumer<ClientModel> addNonDefault = c -> c.addClientScopes(nonDefaultClientScopes, false);

        if (!defaultClientScopes.isEmpty() && !nonDefaultClientScopes.isEmpty())
            newClients.forEach(addDefault.andThen(addNonDefault));
        else if (!defaultClientScopes.isEmpty())
            newClients.forEach(addDefault);
        else if (!nonDefaultClientScopes.isEmpty())
            newClients.forEach(addNonDefault);
    }

    protected abstract void addDefaults(ClientModel realm);

    @Override
    public void close() {

    }
}
