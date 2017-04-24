/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.authorization.CachedStoreFactoryProvider;
import org.keycloak.provider.ProviderFactory;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DefaultAuthorizationProviderFactory implements AuthorizationProviderFactory {

    private Map<String, PolicyProviderFactory> policyProviderFactories;

    @Override
    public AuthorizationProvider create(KeycloakSession session) {
        return create(session, session.getContext().getRealm());
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        policyProviderFactories = configurePolicyProviderFactories(factory);
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "authorization";
    }

    @Override
    public AuthorizationProvider create(KeycloakSession session, RealmModel realm) {
        StoreFactory storeFactory = session.getProvider(CachedStoreFactoryProvider.class);
        if (storeFactory == null) {
            storeFactory = session.getProvider(StoreFactory.class);
        }
        return new AuthorizationProvider(session, realm, storeFactory, policyProviderFactories);
    }

    private Map<String, PolicyProviderFactory> configurePolicyProviderFactories(KeycloakSessionFactory keycloakSessionFactory) {
        List<ProviderFactory> providerFactories = keycloakSessionFactory.getProviderFactories(PolicyProvider.class);

        if (providerFactories.isEmpty()) {
            throw new RuntimeException("Could not find any policy provider.");
        }

        HashMap<String, PolicyProviderFactory> providers = new HashMap<>();

        providerFactories.forEach(providerFactory -> providers.put(providerFactory.getId(), (PolicyProviderFactory) providerFactory));

        return providers;
    }

}