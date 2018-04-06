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
import org.infinispan.distribution.DistributionManager;
import org.infinispan.remoting.transport.Address;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.StickySessionEncoderProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanStickySessionEncoderProvider implements StickySessionEncoderProvider {

    private final KeycloakSession session;
    private final String myNodeName;
    private final boolean shouldAttachRoute;

    public InfinispanStickySessionEncoderProvider(KeycloakSession session, String myNodeName, boolean shouldAttachRoute) {
        this.session = session;
        this.myNodeName = myNodeName;
        this.shouldAttachRoute = shouldAttachRoute;
    }

    @Override
    public String encodeSessionId(String sessionId) {
        if (!shouldAttachRoute) {
            return sessionId;
        }

        String nodeName = getNodeName(sessionId);
        if (nodeName != null) {
            return sessionId + '.' + nodeName;
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


    private String getNodeName(String sessionId) {
        InfinispanConnectionProvider ispnProvider = session.getProvider(InfinispanConnectionProvider.class);
        Cache cache = ispnProvider.getCache(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME);
        DistributionManager distManager = cache.getAdvancedCache().getDistributionManager();

        if (distManager != null) {
            // Sticky session to the node, who owns this authenticationSession
            Address address = distManager.getPrimaryLocation(sessionId);
            return address.toString();
        } else {
            // Fallback to jbossNodeName if authSession cache is local
            return myNodeName;
        }
    }


}
