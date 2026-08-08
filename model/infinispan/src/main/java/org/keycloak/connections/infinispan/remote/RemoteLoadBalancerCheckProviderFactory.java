/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.infinispan.remote;

import java.util.Set;

import org.keycloak.Config;
import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.infinispan.InfinispanConnectionProviderFactory;
import org.keycloak.health.LoadBalancerCheckProvider;
import org.keycloak.health.LoadBalancerCheckProviderFactory;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.Provider;

public class RemoteLoadBalancerCheckProviderFactory implements LoadBalancerCheckProviderFactory, LoadBalancerCheckProvider, EnvironmentDependentProviderFactory {

    private volatile InfinispanConnectionProviderFactory connectionProviderFactory;

    @Override
    public boolean isSupported(Config.Scope config) {
        return MultiSiteUtils.isMultiSiteEnabled();
    }

    @Override
    public LoadBalancerCheckProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        connectionProviderFactory = (InfinispanConnectionProviderFactory) factory.getProviderFactory(InfinispanConnectionProvider.class);
    }

    @Override
    public boolean isDown() {
        return connectionProviderFactory != null && !connectionProviderFactory.isSiteActive();
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return InfinispanUtils.REMOTE_PROVIDER_ID;
    }

    @Override
    public int order() {
        return InfinispanUtils.PROVIDER_ORDER;
    }

   @Override
   public Set<Class<? extends Provider>> dependsOn() {
      return Set.of(InfinispanConnectionProvider.class);
   }
}
