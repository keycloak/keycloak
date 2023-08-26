/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.common;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.provider.Provider;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

public class SessionAttributesUtils {
    private static final AtomicInteger COUNTER_TX = new AtomicInteger();

    /**
     * Returns a new unique counter across whole Keycloak instance
     *
     * @return unique number
     */
    public static int grabNewFactoryIdentifier() {
        return COUNTER_TX.getAndIncrement();
    }

    /**
     * Used for creating a provider instance only once within one
     * KeycloakSession.
     * <p />
     * Checks whether there already exists a provider withing session
     * attributes for given {@code providerClass} and
     * {@code factoryIdentifier}. If exists returns existing provider,
     * otherwise creates a new instance using {@code createNew} function.
     *
     * @param session current Keycloak session
     * @param factoryIdentifier unique factory identifier.
     *                          {@link SessionAttributesUtils#grabNewFactoryIdentifier()}
     *                          can be used for obtaining new identifiers.
     * @param providerClass class of the requested provider
     * @param createNew function that creates a new instance of the provider
     * @return an instance of the provider either from session attributes or freshly created.
     * @param <T> type of the provider
     */
    public static <T extends Provider>  T createProviderIfAbsent(KeycloakSession session,
                                                                 int factoryIdentifier,
                                                                 Class<T> providerClass,
                                                                 Function<KeycloakSession, T> createNew) {
        String uniqueKey = providerClass.getName() + factoryIdentifier;
        T provider = session.getAttribute(uniqueKey, providerClass);

        if (provider != null) {
            return provider;
        }
        provider = createNew.apply(session);

        session.setAttribute(uniqueKey, provider);
        return provider;
    }

    /**
     * Used for creating a store instance only once within one
     * KeycloakSession.
     * <p />
     * Checks whether there already is a store within session attributes
     * for given {@code providerClass}, {@code modelType} and
     * {@code factoryIdentifier}. If exists returns existing provider,
     * otherwise creates a new instance using {@code createNew} supplier.
     *
     * @param session current Keycloak session
     * @param providerType map storage provider class
     * @param modelType model class. Can be null if the store is the same
     *                  for all models.
     * @param factoryId unique factory identifier.
     *                  {@link SessionAttributesUtils#grabNewFactoryIdentifier()}
     *                  can be used for obtaining new identifiers.
     * @param createNew supplier that creates a new instance of the store
     * @return an instance of the store either from session attributes or
     *         freshly created.
     * @param <V> entity type
     * @param <M> model type
     * @param <T> store type
     */
    public static <V extends AbstractEntity & UpdatableEntity, M, T extends MapStorage<V, M>> T createMapStorageIfAbsent(
            KeycloakSession session,
            Class<? extends MapStorageProvider> providerType,
            Class<M> modelType,
            int factoryId,
            Supplier<T> createNew) {
        String sessionAttributeName = providerType.getName() + "-" + (modelType != null ? modelType.getName() : "") + "-" + factoryId;

        T sessionTransaction = (T) session.getAttribute(sessionAttributeName, MapStorage.class);
        if (sessionTransaction == null) {
            sessionTransaction = createNew.get();
            session.setAttribute(sessionAttributeName, sessionTransaction);
        }

        return sessionTransaction;
    }
}
