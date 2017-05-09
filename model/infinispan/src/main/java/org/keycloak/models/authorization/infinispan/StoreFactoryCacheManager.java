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
package org.keycloak.models.authorization.infinispan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.infinispan.Cache;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.authorization.infinispan.events.AuthorizationInvalidationEvent;
import org.keycloak.models.authorization.infinispan.events.ResourceServerRemovedEvent;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class StoreFactoryCacheManager {

    private static final String AUTHORIZATION_UPDATE_TASK_KEY = "authorization-update";

    private final Cache<String, Map<String, List<Object>>> cache;

    StoreFactoryCacheManager(Cache<String, Map<String, List<Object>>> cache) {
        this.cache = cache;
    }

    void invalidate(AuthorizationInvalidationEvent event) {
        if (event instanceof ResourceServerRemovedEvent) {
            cache.remove(event.getId());
            cache.remove(ResourceServerRemovedEvent.class.cast(event).getClientId());
        } else {
            Map<String, List<Object>> resolveResourceServerCache = resolveResourceServerCache(event.getId());

            for (String key : event.getInvalidations()) {
                resolveResourceServerCache.remove(key);
            }
        }
    }

    public void invalidate(KeycloakSession session, String resourceServerId, Set<String> invalidations) {
        getClusterProvider(session).notify(AUTHORIZATION_UPDATE_TASK_KEY, new AuthorizationInvalidationEvent(resourceServerId, invalidations), false);
    }

    public Map<String, List<Object>> resolveResourceServerCache(String id) {
        return cache.computeIfAbsent(id, key -> new HashMap<>());
    }

    void removeAll(KeycloakSession session, ResourceServer id) {
        getClusterProvider(session).notify(AUTHORIZATION_UPDATE_TASK_KEY, new ResourceServerRemovedEvent(id.getId(), id.getClientId()), false);
    }

    private ClusterProvider getClusterProvider(KeycloakSession session) {
        return session.getProvider(ClusterProvider.class);
    }
}
