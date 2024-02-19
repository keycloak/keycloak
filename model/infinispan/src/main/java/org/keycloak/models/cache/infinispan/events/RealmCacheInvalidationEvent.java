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

import org.keycloak.models.InvalidationManager;
import org.keycloak.models.cache.infinispan.RealmCacheManager;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface RealmCacheInvalidationEvent {

    /**
     * The default implementation of this method is left as a no-op in order to allow for piecemeal conversion from
     * this method to the invalidation manager based approach
     * @param realmCache the cache manager for the realm
     * @param invalidations the invalidation manager for the session
     */
    @Deprecated
    default void addInvalidations(RealmCacheManager realmCache, Set<String> invalidations) {

    }

    /**
     * The default implementation of this method is left as a no-op in order to allow for piecemeal implementation over
     * time from the old deprecated method.
     * @param realmCache the cache manager for the realm
     * @param invalidationManager the invalidation manager for the session
     */
    default void addInvalidations(RealmCacheManager realmCache, InvalidationManager invalidationManager) {

    }

}
