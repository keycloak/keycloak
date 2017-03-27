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

package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.sessions.infinispan.entities.AuthenticationSessionEntity;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.sessions.AuthenticationSessionProviderFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanAuthenticationSessionProviderFactory implements AuthenticationSessionProviderFactory {


    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public AuthenticationSessionProvider create(KeycloakSession session) {
        InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
        Cache<String, AuthenticationSessionEntity> authSessionsCache = connections.getCache(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME);

        return new InfinispanAuthenticationSessionProvider(session, authSessionsCache);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "infinispan";
    }
}
