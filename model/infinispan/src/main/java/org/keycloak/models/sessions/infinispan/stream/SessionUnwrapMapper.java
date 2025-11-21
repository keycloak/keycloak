/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.stream;

import java.util.Map;
import java.util.function.Function;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * A {@link Function} to unwrap the {@link SessionEntity} from the {@link SessionEntityWrapper}.
 * <p>
 * The {@link SessionEntityWrapper} is part of the value of {@link Map.Entry}.
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
@ProtoTypeId(Marshalling.SESSION_UNWRAP_MAPPER)
public class SessionUnwrapMapper<K, V extends SessionEntity> implements Function<Map.Entry<K, SessionEntityWrapper<V>>, V> {

    private static final SessionUnwrapMapper<?, ?> INSTANCE = new SessionUnwrapMapper<>();

    private SessionUnwrapMapper() {
    }

    @ProtoFactory
    @SuppressWarnings("unchecked")
    public static <K1, V1 extends SessionEntity> SessionUnwrapMapper<K1, V1> getInstance() {
        return (SessionUnwrapMapper<K1, V1>) INSTANCE;
    }

    @Override
    public V apply(Map.Entry<K, SessionEntityWrapper<V>> entry) {
        return entry.getValue().getEntity();
    }
}
