/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import com.sun.org.apache.xpath.internal.operations.Mult;
import org.apache.commons.collections4.MultiValuedMap;
import org.keycloak.common.util.MultivaluedHashMap;

import javax.swing.plaf.multi.MultiViewportUI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Default implementation of {@link DefaultLazyLoader} that only fetches data once. This implementation is not thread-safe
 * and cached data is assumed to not be shared across different threads to sync state.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DefaultLazyLoader<S, D> implements LazyLoader<S, D> {

    private final Function<S, D> loader;
    private Supplier<D> fallback;
    private D data;

    public DefaultLazyLoader(Function<S, D> loader, Supplier<D> fallback) {
        this.loader = loader;
        this.fallback = fallback;
    }

    @Override
    public D get(Supplier<S> sourceSupplier) {
        if (data == null) {
            S source = sourceSupplier.get();
            data = source == null ? fallback.get() : this.loader.apply(source);
        }
        return data;
    }

    public static <S, D> LazyLoader<S, D> create(Function<S, D> loader, Supplier<D> fallback) {
        return new DefaultLazyLoader<>(loader, fallback);
    }

    public static <S, D> LazyLoader<S, List<D>> forList(Function<S, List<D>> loader) {
        return create(loader, Collections::emptyList);
    }

    public static <S, D> LazyLoader<S, Set<D>> forSet(Function<S, Set<D>> loader) {
        return create(loader, Collections::emptySet);
    }

    public static <S, K, V> LazyLoader<S, Map<K, V>> forMap(Function<S, Map<K, V>> loader) {
        return create(loader, Collections::emptyMap);
    }

    public static <S, K, V> LazyLoader<S, MultivaluedHashMap<K, V>> forMultivaluedMap(
            Function<S, MultivaluedHashMap<K, V>> loader) {
        return create(loader, MultivaluedHashMap::new);
    }

    public static <S, D, K, V> LazyLoader<S, MultivaluedHashMap<K, V>> forMultivaluedMap(
            Function<S, Iterable<D>> loader, Function<D, K> keyMapper, Function<D, V> valueMapper) {
        return create(s -> {
            MultivaluedHashMap<K, V> result = new MultivaluedHashMap<>();
            loader.apply(s).forEach(d -> result.add(keyMapper.apply(d), valueMapper.apply(d)));
            return result;
        }, MultivaluedHashMap::new);
    }

    public static <S, K, V> LazyLoader<S, MultivaluedHashMap<K, V>> forMultivaluedMap(
            Function<S, Iterable<V>> loader, Function<V, K> keyMapper) {
        return forMultivaluedMap(loader, keyMapper, v -> v);
    }

    public static <S, D> LazyLoader<S, List<D>> forStreamAsList(Function<S, Stream<D>> loader) {
        return forList(s -> loader.apply(s).collect(Collectors.toList()));
    }

    public static <S, D, K, V> LazyLoader<S, Map<K, V>> forStreamAsMap(
            Function<S, Stream<D>> loader, Function<D, K> keyMapper, Function<D, V> valueMapper) {
        return forMap(s -> loader.apply(s).collect(Collectors.toMap(keyMapper, valueMapper)));
    }

    public static <S, K, V> LazyLoader<S, Map<K, V>> forStreamAsMap(Function<S, Stream<V>> loader, Function<V, K> keyMapper) {
        return forStreamAsMap(loader, keyMapper, v -> v);
    }
}
