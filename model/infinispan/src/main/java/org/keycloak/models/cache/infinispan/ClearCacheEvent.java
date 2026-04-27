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
package org.keycloak.models.cache.infinispan;

import org.keycloak.cluster.ClusterEvent;
import org.keycloak.marshalling.Marshalling;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@ProtoTypeId(Marshalling.CLEAR_CACHE_EVENT)
public final class ClearCacheEvent implements ClusterEvent {

    private static final ClearCacheEvent INSTANCE = new ClearCacheEvent();

    private ClearCacheEvent() {}

    @ProtoFactory
    public static ClearCacheEvent getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClearCacheEvent;
    }

    @Override
    public int hashCode() {
        return 1;
    }
}
