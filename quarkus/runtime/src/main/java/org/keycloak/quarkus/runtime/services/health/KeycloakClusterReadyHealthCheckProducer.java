/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.quarkus.runtime.services.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.infinispan.InfinispanConnectionProviderFactory;
import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;

import io.smallrye.health.api.AsyncHealthCheck;
import org.eclipse.microprofile.health.Readiness;

@ApplicationScoped
public class KeycloakClusterReadyHealthCheckProducer {

    private AsyncHealthCheck instance;
    private boolean ready;
    @Inject
    QuarkusKeycloakSessionFactory sessionFactory;

    @Produces
    @Readiness
    @Dependent
    public AsyncHealthCheck createHealthCheck() {
        if (ready) {
            // JVM branch prediction may optimize this code and saves on reading a static volatile field
            return instance;
        }
        if (!sessionFactory.isBootstrapCompleted()) {
            return null;
        }
        synchronized (this) {
            if (ready) {
                return instance;
            }
            var factory = (InfinispanConnectionProviderFactory) sessionFactory.getProviderFactory(InfinispanConnectionProvider.class);
            if (factory.isClusterHealthSupported()) {
                instance = new KeycloakClusterReadyHealthCheck(factory);
            }
            ready = true;
        }

        return instance;
    }
}
