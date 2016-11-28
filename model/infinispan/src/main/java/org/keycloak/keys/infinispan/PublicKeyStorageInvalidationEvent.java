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

package org.keycloak.keys.infinispan;

import org.keycloak.models.cache.infinispan.events.InvalidationEvent;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PublicKeyStorageInvalidationEvent extends InvalidationEvent {

    private String cacheKey;

    public static PublicKeyStorageInvalidationEvent create(String cacheKey) {
        PublicKeyStorageInvalidationEvent event = new PublicKeyStorageInvalidationEvent();
        event.cacheKey = cacheKey;
        return event;
    }

    @Override
    public String getId() {
        return cacheKey;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    @Override
    public String toString() {
        return "PublicKeyStorageInvalidationEvent [ " + cacheKey + " ]";
    }
}
