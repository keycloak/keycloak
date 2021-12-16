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

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractMapProviderFactory;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.sessions.AuthenticationSessionProviderFactory;

import org.keycloak.sessions.RootAuthenticationSessionModel;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapRootAuthenticationSessionProviderFactory extends AbstractMapProviderFactory<AuthenticationSessionProvider, MapRootAuthenticationSessionEntity, RootAuthenticationSessionModel>
        implements AuthenticationSessionProviderFactory {

    public MapRootAuthenticationSessionProviderFactory() {
        super(RootAuthenticationSessionModel.class);
    }

    @Override
    public AuthenticationSessionProvider create(KeycloakSession session) {
        return new MapRootAuthenticationSessionProvider(session, getStorage(session));
    }

    @Override
    public String getHelpText() {
        return "Authentication session provider";
    }

}
