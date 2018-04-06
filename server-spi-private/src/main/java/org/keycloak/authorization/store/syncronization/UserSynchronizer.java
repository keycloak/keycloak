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

package org.keycloak.authorization.store.syncronization;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.UserRemovedEvent;
import org.keycloak.provider.ProviderFactory;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserSynchronizer implements Synchronizer<UserRemovedEvent> {

    @Override
    public void synchronize(UserRemovedEvent event, KeycloakSessionFactory factory) {
        ProviderFactory<AuthorizationProvider> providerFactory = factory.getProviderFactory(AuthorizationProvider.class);
        AuthorizationProvider authorizationProvider = providerFactory.create(event.getKeycloakSession());
        StoreFactory storeFactory = authorizationProvider.getStoreFactory();
        UserModel userModel = event.getUser();
        ResourceStore resourceStore = storeFactory.getResourceStore();
        PolicyStore policyStore = storeFactory.getPolicyStore();
        ResourceServerStore resourceServerStore = storeFactory.getResourceServerStore();
        RealmModel realm = event.getRealm();

        realm.getClients().forEach(clientModel -> {
            ResourceServer resourceServer = resourceServerStore.findById(clientModel.getId());

            if (resourceServer != null) {
                resourceStore.findByOwner(userModel.getId(), resourceServer.getId()).forEach(resource -> {
                    String resourceId = resource.getId();
                    policyStore.findByResource(resourceId, resourceServer.getId()).forEach(policy -> {
                        if (policy.getResources().size() == 1) {
                            policyStore.delete(policy.getId());
                        } else {
                            policy.removeResource(resource);
                        }
                    });
                    resourceStore.delete(resourceId);
                });
            }
        });
    }
}
