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

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanUtil;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.sessions.StickySessionEncoderProvider;
import org.keycloak.sessions.StickySessionEncoderProviderFactory;

public class RemoteStickySessionEncoderProviderFactory implements StickySessionEncoderProviderFactory, EnvironmentDependentProviderFactory {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());
    private static final char SEPARATOR = '.';

    private static final StickySessionEncoderProvider NO_ROUTER_PROVIDER = new BaseProvider() {
        @Override
        public String encodeSessionId(String sessionId) {
            return sessionId;
        }

        @Override
        public boolean shouldAttachRoute() {
            return false;
        }
    };

    private volatile boolean shouldAttachRoute;
    private volatile StickySessionEncoderProvider provider;

    @Override
    public StickySessionEncoderProvider create(KeycloakSession session) {
        return shouldAttachRoute ? provider : NO_ROUTER_PROVIDER;
    }

    @Override
    public void init(Config.Scope config) {
        setShouldAttachRoute(config.getBoolean("shouldAttachRoute", true));
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        try (var session = factory.create()) {
            provider = new AttachRouteProvider(getRoute(session));
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
    public void setShouldAttachRoute(boolean shouldAttachRoute) {
        this.shouldAttachRoute = shouldAttachRoute;
        log.debugf("Should attach route to the sticky session cookie: %b", shouldAttachRoute);
    }

    private static String getRoute(KeycloakSession session) {
        return InfinispanUtil.getTopologyInfo(session).getMyNodeName();
    }

    private static abstract class BaseProvider implements StickySessionEncoderProvider {

        @Override
        public final String decodeSessionId(String encodedSessionId) {
            // Try to decode regardless if shouldAttachRoute is true/false.
            // It is possible that some loadbalancers may forward the route information attached by them to the backend keycloak server.
            // We need to remove it then.
            int index = encodedSessionId.indexOf(SEPARATOR);
            return index == -1 ? encodedSessionId : encodedSessionId.substring(0, index);
        }

        @Override
        public final void close() {
        }
    }

    private static class AttachRouteProvider extends BaseProvider {

        private final String route;

        private AttachRouteProvider(String route) {
            this.route = route;
        }

        @Override
        public String encodeSessionId(String sessionId) {
            return sessionId + SEPARATOR + route;
        }

        @Override
        public boolean shouldAttachRoute() {
            return true;
        }
    }
}
