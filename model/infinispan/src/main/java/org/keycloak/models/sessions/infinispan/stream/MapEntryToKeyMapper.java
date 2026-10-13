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

import java.lang.invoke.SerializedLambda;
import java.util.Map;
import java.util.function.Function;

import org.keycloak.marshalling.Marshalling;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * A {@link Function} to extract the key from a {@link Map.Entry}.
 * <p>
 * Same as {@code Map.Entry::getKey}.
 * <p>
 * Infinispan can marshall lambdas, by using {@link SerializedLambda} but it is not as efficient and ProtoStream
 * marshaller.
 *
 * @param <K>
 * @param <V>
 */
@ProtoTypeId(Marshalling.MAP_ENTRY_TO_KEY_FUNCTION)
public class MapEntryToKeyMapper<K, V> implements Function<Map.Entry<K, V>, K> {

    private static final MapEntryToKeyMapper<?, ?> INSTANCE = new MapEntryToKeyMapper<>();

    private MapEntryToKeyMapper() {
    }

    @ProtoFactory
    @SuppressWarnings("unchecked")
    public static <K1, V1> MapEntryToKeyMapper<K1, V1> getInstance() {
        return (MapEntryToKeyMapper<K1, V1>) INSTANCE;
    }


    @Override
    public K apply(Map.Entry<K, V> entry) {
        return entry.getKey();
    }
}
