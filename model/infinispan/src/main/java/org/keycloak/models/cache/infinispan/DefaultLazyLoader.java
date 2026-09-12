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
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.keycloak.models.KeycloakSession;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.runWithoutAuthorization;

/**
 * Default implementation of {@link LazyLoader} that lazily loads and caches data. Data loaded from a source rejected by
 * the configured cacheability predicate is returned without being cached, so a subsequent invocation attempts to load
 * the data again. This implementation is thread-safe as cached data is used in instances of
 * {@link org.keycloak.models.cache.infinispan.entities.CachedRealm} which are shared between multiple threads within a
 * Keycloak instance.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DefaultLazyLoader<S, D> implements LazyLoader<S, D> {

    private final Function<S, D> loader;
    private final Supplier<D> fallback;
    private final Predicate<S> cacheable;
    private volatile D data;

    public DefaultLazyLoader(Function<S, D> loader, Supplier<D> fallback) {
        this(loader, fallback, source -> true);
    }

    /**
     * Creates a loader that caches loaded data only when {@code cacheable} accepts its source.
     *
     * @param loader function used to load data from a non-null source
     * @param fallback supplier used when the source is null
     * @param cacheable predicate evaluated against a non-null source after its data has been loaded. If it returns
     *        {@code false}, the loaded data is returned without being cached and a subsequent invocation attempts to load
     *        again. When the source is null, the predicate is not evaluated and the fallback result is stored instead.
     */
    public DefaultLazyLoader(Function<S, D> loader, Supplier<D> fallback, Predicate<S> cacheable) {
        this.loader = loader;
        this.fallback = fallback;
        this.cacheable = cacheable;
    }

    @Override
    public D get(KeycloakSession session, Supplier<S> sourceSupplier) {
        if (data == null) {
            synchronized (this) {
                if (data == null) {
                    return runWithoutAuthorization(session, () -> {
                        // make sure caching does not include partial results when FGAP is enabled
                        S source = sourceSupplier.get();
                        D loaded = source == null ? fallback.get() : loader.apply(source);
                        if (source == null || cacheable.test(source)) {
                            data = loaded;
                        }
                        return loaded;
                    });
                }
            }
        }
        return data;
    }
}
