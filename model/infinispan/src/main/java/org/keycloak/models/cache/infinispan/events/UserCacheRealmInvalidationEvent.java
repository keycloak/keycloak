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

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.cache.infinispan.UserCacheManager;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ProtoTypeId(Marshalling.USER_CACHE_REALM_INVALIDATION_EVENT)
public class UserCacheRealmInvalidationEvent  extends InvalidationEvent implements UserCacheInvalidationEvent {

    private UserCacheRealmInvalidationEvent(String id) {
        super(id);
    }

    @ProtoFactory
    public static UserCacheRealmInvalidationEvent create(String id) {
        return new UserCacheRealmInvalidationEvent(id);
    }

    @Override
    public String toString() {
        return String.format("UserCacheRealmInvalidationEvent [ realmId=%s ]", getId());
    }

    @Override
    public void addInvalidations(UserCacheManager userCache, Set<String> invalidations) {
        userCache.invalidateRealmUsers(getId(), invalidations);
    }

}
