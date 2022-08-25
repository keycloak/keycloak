/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.authSession;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractMapProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.sessions.AuthenticationSessionProviderFactory;

import org.keycloak.sessions.RootAuthenticationSessionModel;

import java.util.List;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapRootAuthenticationSessionProviderFactory extends AbstractMapProviderFactory<MapRootAuthenticationSessionProvider, MapRootAuthenticationSessionEntity, RootAuthenticationSessionModel>
        implements AuthenticationSessionProviderFactory<MapRootAuthenticationSessionProvider> {

    public static final String AUTH_SESSIONS_LIMIT = "authSessionsLimit";

    public static final int DEFAULT_AUTH_SESSIONS_LIMIT = 300;

    private int authSessionsLimit;

    public MapRootAuthenticationSessionProviderFactory() {
        super(RootAuthenticationSessionModel.class, MapRootAuthenticationSessionProvider.class);
    }

    @Override
    public void init(Config.Scope config) {
        super.init(config);

        // get auth sessions limit from config or use default if not provided
        int configInt = config.getInt(AUTH_SESSIONS_LIMIT, DEFAULT_AUTH_SESSIONS_LIMIT);
        // use default if provided value is not a positive number
        authSessionsLimit = (configInt <= 0) ? DEFAULT_AUTH_SESSIONS_LIMIT : configInt;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("authSessionsLimit")
                .type("int")
                .helpText("The maximum number of concurrent authentication sessions per RootAuthenticationSession.")
                .defaultValue(DEFAULT_AUTH_SESSIONS_LIMIT)
                .add()
                .build();
    }

    @Override
    public MapRootAuthenticationSessionProvider createNew(KeycloakSession session) {
        return new MapRootAuthenticationSessionProvider(session, getStorage(session), authSessionsLimit);
    }

    @Override
    public String getHelpText() {
        return "Authentication session provider";
    }

}
