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
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.util.InfinispanUtil;
import org.keycloak.sessions.StickySessionEncoderProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanStickySessionEncoderProvider implements StickySessionEncoderProvider {

    private final KeycloakSession session;
    private final boolean shouldAttachRoute;

    public InfinispanStickySessionEncoderProvider(KeycloakSession session, boolean shouldAttachRoute) {
        this.session = session;
        this.shouldAttachRoute = shouldAttachRoute;
    }

    @Override
    public String encodeSessionId(String sessionId) {
        if (!shouldAttachRoute) {
            return sessionId;
        }

        String route = getRoute(sessionId);
        if (route != null) {
            return sessionId + '.' + route;
        } else {
            return sessionId;
        }
    }

    @Override
    public String decodeSessionId(String encodedSessionId) {
        // Try to decode regardless if shouldAttachRoute is true/false. It's possible that some loadbalancers may forward the route information attached by them to the backend keycloak server. We need to remove it then.
        int index = encodedSessionId.indexOf('.');
        return index == -1 ? encodedSessionId : encodedSessionId.substring(0, index);
    }

    @Override
    public boolean shouldAttachRoute() {
        return shouldAttachRoute;
    }

    @Override
    public void close() {

    }


    private String getRoute(String sessionId) {
        InfinispanConnectionProvider ispnProvider = session.getProvider(InfinispanConnectionProvider.class);
        Cache cache = ispnProvider.getCache(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME);
        return InfinispanUtil.getTopologyInfo(session).getRouteName(cache, sessionId);
    }


}
