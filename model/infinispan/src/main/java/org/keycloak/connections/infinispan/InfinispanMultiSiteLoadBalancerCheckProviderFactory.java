/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.infinispan;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.health.LoadBalancerCheckProvider;
import org.keycloak.health.LoadBalancerCheckProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;



public class InfinispanMultiSiteLoadBalancerCheckProviderFactory implements LoadBalancerCheckProviderFactory, EnvironmentDependentProviderFactory {

    private LoadBalancerCheckProvider loadBalancerCheckProvider;
    private static final LoadBalancerCheckProvider ALWAYS_HEALTHY = new LoadBalancerCheckProvider() {
        @Override public boolean isDown() { return false; }
        @Override public void close() {}
    };
    private static final Logger LOG = Logger.getLogger(InfinispanMultiSiteLoadBalancerCheckProviderFactory.class);

    @Override
    public LoadBalancerCheckProvider create(KeycloakSession session) {
        if (loadBalancerCheckProvider == null) {
            InfinispanConnectionProvider infinispanConnectionProvider = session.getProvider(InfinispanConnectionProvider.class);
            if (infinispanConnectionProvider == null) {
                LOG.warn("InfinispanConnectionProvider is not available. Load balancer check will be always healthy for Infinispan.");
                loadBalancerCheckProvider = ALWAYS_HEALTHY;
            } else {
                loadBalancerCheckProvider = new InfinispanMultiSiteLoadBalancerCheckProvider(infinispanConnectionProvider);
            }
        }
        return loadBalancerCheckProvider;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "infinispan-multisite";
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.MULTI_SITE);
    }
}
