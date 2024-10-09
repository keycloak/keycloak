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

package org.keycloak.quarkus.runtime.services.metrics.events;

import org.bouncycastle.util.Strings;
import org.keycloak.Config;
import org.keycloak.events.EventType;
import org.keycloak.events.GlobalEventListenerProvider;
import org.keycloak.events.GlobalEventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

import java.util.Locale;

public class MicrometerMetricsEventListenerFactory implements GlobalEventListenerProviderFactory, EnvironmentDependentProviderFactory {

    private static final String ID = "micrometer-metrics";

    private boolean withIdp, withRealm, withClientId;

    @Override
    public GlobalEventListenerProvider create(KeycloakSession session) {
        return new MicrometerMetricsEventListener(session, withIdp, withRealm, withClientId);
    }

    @Override
    public void init(Config.Scope config) {
        String tags = config.get("tags");
        if (tags != null) {
            for (String s : Strings.split(tags, ',')) {
                switch (s.trim()) {
                    case "idp" -> withIdp = true;
                    case "realm" -> withRealm = true;
                    case "clientId" -> withClientId = true;
                    default -> throw new IllegalArgumentException("Unknown tag for collecting user event metrics: '" + s + "'");
                }
            }
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return config.getBoolean("enabled");
    }

}
