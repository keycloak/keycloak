/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.infinispan;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Transport;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.NODE_PREFIX;

public record NodeInfo(String nodeName, String siteName) {

    public NodeInfo {
        Objects.requireNonNull(nodeName);
    }

    public static NodeInfo of(EmbeddedCacheManager cacheManager) {
        var transportConfig = cacheManager.getCacheManagerConfiguration().transport();
        var nodeName = transportConfig.nodeName();

        if (nodeName != null) {
            // user configured a node name, use it.
            return new NodeInfo(nodeName, transportConfig.siteId());
        }

        var transport = GlobalComponentRegistry.componentOf(cacheManager, Transport.class);
        // if we have a cluster, use the node name from JGroups; otherwise generate a random name.
        nodeName = transport == null ?
                NODE_PREFIX + ThreadLocalRandom.current().nextInt(1000000) :
                transport.localNodeName();
        return new NodeInfo(nodeName, transportConfig.siteId());
    }

    public String printInfo() {
        return "Node name: %s, Site name: %s".formatted(nodeName, siteName);
    }
}
