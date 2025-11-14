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

import org.infinispan.Cache;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.jboss.logging.Logger;
import org.jgroups.util.NameCache;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanStickySessionEncoderProviderFactory implements StickySessionEncoderProviderFactory, EnvironmentDependentProviderFactory, StickySessionEncoderProvider {

    private static final Logger log = Logger.getLogger(InfinispanStickySessionEncoderProviderFactory.class);
    private static final char SEPARATOR = '.';

    private boolean shouldAttachRoute;
    private boolean clustered;
    private Cache<String, ?> authenticationCache;

    @Override
    public StickySessionEncoderProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        setShouldAttachRoute(config.getBoolean("shouldAttachRoute", true));
    }

    // Used for testing
    @Override
    public void setShouldAttachRoute(boolean shouldAttachRoute) {
        this.shouldAttachRoute = shouldAttachRoute;
        log.debugf("Should attach route to the sticky session cookie: %b", shouldAttachRoute);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        try (var session = factory.create()) {
            var provider = session.getProvider(InfinispanConnectionProvider.class);
            authenticationCache = provider.getCache(AUTHENTICATION_SESSIONS_CACHE_NAME);
            clustered = authenticationCache.getCacheConfiguration().clustering().cacheMode().isClustered();
        }
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return InfinispanUtils.EMBEDDED_PROVIDER_ID;
    }

    @Override
    public int order() {
        return InfinispanUtils.PROVIDER_ORDER;
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(InfinispanConnectionProvider.class);
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
        return InfinispanUtils.isEmbeddedInfinispan();
    }

    @Override
    public String encodeSessionId(String message, String sessionId) {
        Objects.requireNonNull(message);
        String route = sessionIdRoute(sessionId);
        return route == null ? message : message + SEPARATOR + route;
    }

    @Override
    public SessionIdAndRoute decodeSessionIdAndRoute(String encodedSessionId) {
        int index = encodedSessionId.indexOf(SEPARATOR);
        int length = encodedSessionId.length();
        if (index == -1 || index == (length - 1)) {
            //route not present
            return new SessionIdAndRoute(encodedSessionId, null);
        }
        return new SessionIdAndRoute(encodedSessionId.substring(0, index), encodedSessionId.substring(index + 1, length));
    }

    @Override
    public boolean shouldAttachRoute() {
        return shouldAttachRoute;
    }

    @Override
    public String sessionIdRoute(String sessionId) {
        // return null if running in the local mode (start-dev)
        return clustered && shouldAttachRoute ? ownerOf(sessionId) : null;
    }

    private String ownerOf(String sessionId) {
        var primaryOwner = authenticationCache.getAdvancedCache()
                .getDistributionManager()
                .getCacheTopology()
                .getDistribution(Objects.requireNonNull(sessionId))
                .primary();
        // Return null if the logical name is not available yet.
        // The following request may be redirected to the wrong instance, but that's ok.
        // In a healthy/stable cluster, the name cache is correctly populated.
        return primaryOwner instanceof JGroupsAddress jgrpAddr ? NameCache.get(jgrpAddr.getJGroupsAddress()) : null;
    }
}
