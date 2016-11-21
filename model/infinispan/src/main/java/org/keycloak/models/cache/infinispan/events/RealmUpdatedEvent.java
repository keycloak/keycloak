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

package org.keycloak.models.cache.infinispan.events;

import java.util.Set;

import org.keycloak.models.cache.infinispan.RealmCacheManager;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RealmUpdatedEvent extends InvalidationEvent implements RealmCacheInvalidationEvent {

    private String realmId;
    private String realmName;

    public static RealmUpdatedEvent create(String realmId, String realmName) {
        RealmUpdatedEvent event = new RealmUpdatedEvent();
        event.realmId = realmId;
        event.realmName = realmName;
        return event;
    }

    @Override
    public String getId() {
        return realmId;
    }

    @Override
    public String toString() {
        return String.format("RealmUpdatedEvent [ realmId=%s, realmName=%s ]", realmId, realmName);
    }

    @Override
    public void addInvalidations(RealmCacheManager realmCache, Set<String> invalidations) {
        realmCache.realmUpdated(realmId, realmName, invalidations);
    }
}
