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

package org.keycloak.models.cache.infinispan.authorization.events;

import java.util.Set;

import org.keycloak.models.cache.infinispan.authorization.StoreFactoryCacheManager;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PolicyRemovedEvent extends InvalidationEvent implements AuthorizationCacheInvalidationEvent {

    private String id;
    private String name;
    private Set<String> resources;
    private Set<String> resourceTypes;
    private Set<String> scopes;
    private String serverId;

    public static PolicyRemovedEvent create(String id, String name, Set<String> resources, Set<String> resourceTypes, Set<String> scopes, String serverId) {
        PolicyRemovedEvent event = new PolicyRemovedEvent();
        event.id = id;
        event.name = name;
        event.resources = resources;
        event.resourceTypes = resourceTypes;
        event.scopes = scopes;
        event.serverId = serverId;
        return event;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("PolicyRemovedEvent [ id=%s, name=%s]", id, name);
    }

    @Override
    public void addInvalidations(StoreFactoryCacheManager cache, Set<String> invalidations) {
        cache.policyRemoval(id, name, resources, resourceTypes, scopes, serverId, invalidations);
    }
}
