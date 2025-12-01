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

package org.keycloak.models.sessions.infinispan.remote;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.sessions.StickySessionEncoderProvider;
import org.keycloak.sessions.StickySessionEncoderProviderFactory;

import org.jboss.logging.Logger;

public class RemoteStickySessionEncoderProviderFactory implements StickySessionEncoderProviderFactory, EnvironmentDependentProviderFactory, StickySessionEncoderProvider {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());
    private static final char SEPARATOR = '.';

    private volatile boolean shouldAttachRoute;
    private volatile String route;

    @Override
    public StickySessionEncoderProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        setShouldAttachRoute(config.getBoolean("shouldAttachRoute", true));
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        try (var session = factory.create()) {
            route = session.getProvider(InfinispanConnectionProvider.class).getNodeInfo().nodeName();
        }
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
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("shouldAttachRoute")
                .type("boolean")
                .helpText("If the route should be attached to cookies to reflect the node that owns a particular session.")
                .defaultValue(true)
                .add()
                .build();
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return InfinispanUtils.isRemoteInfinispan();
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(InfinispanConnectionProvider.class);
    }

    @Override
    public void setShouldAttachRoute(boolean shouldAttachRoute) {
        this.shouldAttachRoute = shouldAttachRoute;
        log.debugf("Should attach route to the sticky session cookie: %b", shouldAttachRoute);
    }

    @Override
    public String encodeSessionId(String message, String ignored) {
        Objects.requireNonNull(message);
        return shouldAttachRoute ? message + SEPARATOR + route : message;
    }

    @Override
    public SessionIdAndRoute decodeSessionIdAndRoute(String encodedSessionId) {
        int index = encodedSessionId.indexOf('.');
        if (index == -1) {
            //route not present
            return new SessionIdAndRoute(encodedSessionId, null);
        }
        return new SessionIdAndRoute(encodedSessionId.substring(0, index), encodedSessionId.substring(index, encodedSessionId.length() - 1));
    }

    @Override
    public boolean shouldAttachRoute() {
        return shouldAttachRoute;
    }

    @Override
    public String sessionIdRoute(String ignored) {
        return shouldAttachRoute ? route : null;
    }
}
