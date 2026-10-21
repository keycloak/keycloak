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

package org.keycloak.tracing;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class NoopTracingProviderFactory implements TracingProviderFactory {
    public static final String PROVIDER_ID = "noop";
    private static TracingProvider SINGLETON;

    @Override
    public TracingProvider create(KeycloakSession session) {
        if (SINGLETON == null) {
            SINGLETON = new NoopTracingProvider();
        }
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {
        SINGLETON = null;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public int order() {
        // If OTel is disabled, this provider should be the default - no tracing
        // If OTel is enabled, this provider should be the last one to consider
        return !Profile.isFeatureEnabled(Profile.Feature.OPENTELEMETRY) ? 1000 : -1000;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return true;
    }
}
