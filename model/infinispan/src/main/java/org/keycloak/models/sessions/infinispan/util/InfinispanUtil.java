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

package org.keycloak.models.sessions.infinispan.util;

import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.persistence.remote.RemoteStore;
import org.infinispan.remoting.transport.Transport;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.infinispan.TopologyInfo;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanUtil {

    public static final int MAXIMUM_REPLACE_RETRIES = 25;

    // See if we have RemoteStore (external JDG) configured for cross-Data-Center scenario
    public static Set<RemoteStore> getRemoteStores(Cache ispnCache) {
        return ispnCache.getAdvancedCache().getComponentRegistry().getComponent(PersistenceManager.class).getStores(RemoteStore.class);
    }


    public static RemoteCache getRemoteCache(Cache ispnCache) {
        Set<RemoteStore> remoteStores = getRemoteStores(ispnCache);
        if (remoteStores.isEmpty()) {
            return null;
        } else {
            return remoteStores.iterator().next().getRemoteCache();
        }
    }


    public static TopologyInfo getTopologyInfo(KeycloakSession session) {
        return session.getProvider(InfinispanConnectionProvider.class).getTopologyInfo();
    }


    /**
     *
     * @param cache
     * @return true if cluster coordinator OR if it's local cache
     */
    public static boolean isCoordinator(Cache cache) {
        Transport transport = cache.getCacheManager().getTransport();
        return transport == null || transport.isCoordinator();
    }

}
