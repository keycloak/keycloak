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

import java.util.function.Supplier;

import org.keycloak.models.KeycloakSession;

/**
 * <p>A functional interface that can be used to return data {@code D} from a source {@code S} where implementations are free to define how and when
 * data is fetched from source as well how it is internally cached.
 *
 * <p>The source does not need to worry about caching data but always fetch data as demanded. The way data will actually be cached is an implementation detail.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 * @see DefaultLazyLoader
 */
public interface LazyLoader<S, D> {

    /**
     * Returns data from the given {@code source}. Data is only fetched from {@code source} once and only if necessary, it is
     * up to implementations to decide the momentum to actually fetch data from source.
     *
     * @param session the session
     * @param source the source from where data will be fetched.
     * @return the data from source
     */
    D get(KeycloakSession session, Supplier<S> source);
}
