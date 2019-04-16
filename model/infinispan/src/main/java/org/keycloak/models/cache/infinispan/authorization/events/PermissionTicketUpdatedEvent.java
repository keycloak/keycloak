/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
public class PermissionTicketUpdatedEvent extends InvalidationEvent implements AuthorizationCacheInvalidationEvent {

    private String id;
    private String owner;
    private String resource;
    private String scope;
    private String serverId;
    private String requester;
    private String resourceName;

    public static PermissionTicketUpdatedEvent create(String id, String owner, String requester, String resource, String resourceName, String scope, String serverId) {
        PermissionTicketUpdatedEvent event = new PermissionTicketUpdatedEvent();
        event.id = id;
        event.owner = owner;
        event.requester = requester;
        event.resource = resource;
        event.resourceName = resourceName;
        event.scope = scope;
        event.serverId = serverId;
        return event;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("PermissionTicketUpdatedEvent [ id=%s, name=%s]", id, resource);
    }

    @Override
    public void addInvalidations(StoreFactoryCacheManager cache, Set<String> invalidations) {
        cache.permissionTicketUpdated(id, owner, requester, resource, resourceName, scope, serverId, invalidations);
    }
}
