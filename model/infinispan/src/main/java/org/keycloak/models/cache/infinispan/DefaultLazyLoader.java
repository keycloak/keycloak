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

import java.util.function.Function;
import java.util.function.Supplier;

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
}
