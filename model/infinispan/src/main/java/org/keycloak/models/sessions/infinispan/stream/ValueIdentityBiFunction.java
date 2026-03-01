/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.util.function.BiFunction;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;

import static org.keycloak.marshalling.Marshalling.VALUE_IDENTITY_BI_FUNCTION;

/**
 * A {@link BiFunction} implementation that returns the second argument unchanged, effectively acting as an identity
 * function for the value parameter.
 * <p>
 * This class is used in Infinispan cache operations where a {@link BiFunction} is required but only the value needs to
 * be preserved without any transformation. The key parameter is ignored.
 * <p>
 * The class is implemented as a stateless singleton and is serializable via Infinispan ProtoStream to support
 * distributed cache operations in remote caches.
 *
 * @param <K> The type of the first argument (key). This parameter is ignored by the function.
 * @param <V> The type of the second argument (value). This parameter is returned unchanged.
 */
@ProtoTypeId(VALUE_IDENTITY_BI_FUNCTION)
public final class ValueIdentityBiFunction<K, V> implements BiFunction<K, V, V> {

    private static final ValueIdentityBiFunction<?, ?> INSTANCE = new ValueIdentityBiFunction<>();

    private ValueIdentityBiFunction() {
    }

    /**
     * Returns the singleton instance of this function.
     * <p>
     * This method is annotated with {@link ProtoFactory} to enable Infinispan ProtoStream serialization for remote
     * cache operations.
     *
     * @param <T> The type of the key parameter
     * @param <E> The type of the value parameter
     * @return The singleton instance of {@link ValueIdentityBiFunction}
     */
    @ProtoFactory
    @SuppressWarnings("unchecked")
    public static <T, E> ValueIdentityBiFunction<T, E> getInstance() {
        return (ValueIdentityBiFunction<T, E>) INSTANCE;
    }

    /**
     * Returns the value parameter unchanged, ignoring the key parameter.
     *
     * @param k The key parameter (ignored)
     * @param v The value parameter to return
     * @return The value parameter unchanged
     */
    @Override
    public V apply(K k, V v) {
        return v;
    }
}
