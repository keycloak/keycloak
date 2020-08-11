/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.storage;

import org.keycloak.Config;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.utils.ServicesUtils;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 *
 * @param <ProviderType> This type will be used for looking for factories that produce instances of desired providers
 * @param <StorageProviderModelType> Type of model used for creating provider, it needs to extend 
 *                                  CacheableStorageProviderModel as it has {@code isEnabled()} method and also extend
 *                                  PrioritizedComponentModel which is required for sorting providers based on its
 *                                  priorities
 */
public abstract class AbstractStorageManager<ProviderType extends Provider,
        StorageProviderModelType extends CacheableStorageProviderModel> {

    /**
     * Timeouts are used as time boundary for obtaining models from an external storage. Default value is set
     * to 3000 milliseconds and it's configurable.
     */
    private static final Long STORAGE_PROVIDER_DEFAULT_TIMEOUT = 3000L;
    protected final KeycloakSession session;
    private final Class<ProviderType> providerTypeClass;
    private final Function<ComponentModel, StorageProviderModelType> toStorageProviderModelTypeFunction;
    private final String configScope;
    private Long storageProviderTimeout;

    public AbstractStorageManager(KeycloakSession session, Class<ProviderType> providerTypeClass, Function<ComponentModel, StorageProviderModelType> toStorageProviderModelTypeFunction, String configScope) {
        this.session = session;
        this.providerTypeClass = providerTypeClass;
        this.toStorageProviderModelTypeFunction = toStorageProviderModelTypeFunction;
        this.configScope = configScope;
    }

    protected Long getStorageProviderTimeout() {
        if (storageProviderTimeout == null) {
            storageProviderTimeout = Config.scope(configScope).getLong("storageProviderTimeout", STORAGE_PROVIDER_DEFAULT_TIMEOUT);
        }
        return storageProviderTimeout;
    }

    /**
     * Returns a factory with the providerId, which produce instances of type CreatedProviderType
     * @param providerId id of factory that produce desired instances
     * @return A factory that implements {@code ComponentFactory<CreatedProviderType, ProviderType>}
     */
    protected <T extends ProviderType> ComponentFactory<T, ProviderType> getStorageProviderFactory(String providerId) {
        return (ComponentFactory<T, ProviderType>) session.getKeycloakSessionFactory()
                .getProviderFactory(providerTypeClass, providerId);
    }

    /**
     *
     * @param realm realm
     * @return enabled storage providers for realm and @{code getProviderTypeClass()}
     */
    protected Stream<ProviderType> getEnabledStorageProviders(RealmModel realm) {
        return getStorageProviderModels(realm, providerTypeClass)
                .map(toStorageProviderModelTypeFunction)
                .filter(StorageProviderModelType::isEnabled)
                .sorted(StorageProviderModelType.comparator)
                .map(this::getStorageProviderInstance);
    }

    /**
     * Gets all enabled StorageProviders, applies applyFunction on each of them and then join the results together.
     *
     * !! Each StorageProvider has a limited time to respond, if it fails to do it, empty stream is returned !!
     *
     * @param realm realm
     * @param applyFunction function that is applied on StorageProviders
     * @param <R> result of applyFunction
     * @return a stream with all results from all StorageProviders
     */
    protected <R> Stream<R> applyOnEnabledStorageProvidersWithTimeout(RealmModel realm, Function<ProviderType, ? extends Stream<R>> applyFunction) {
        return getEnabledStorageProviders(realm).flatMap(ServicesUtils.timeBound(session,
                    getStorageProviderTimeout(), applyFunction));
    }

    /**
     * Returns an instance of provider with the providerId within the realm.
     * @param realm realm
     * @param providerId id of ComponentModel within database/storage
     * @return an instance of type CreatedProviderType
     */
    protected ProviderType getStorageProviderInstance(RealmModel realm, String providerId) {
        ComponentModel componentModel = realm.getComponent(providerId);
        if (componentModel == null) {
            return null;
        }
        
        return getStorageProviderInstance(toStorageProviderModelTypeFunction.apply(componentModel));
    }

    /**
     * Returns an instance of provider for the model
     * @param model StorageProviderModel obtained from database/storage
     * @return an instance of type CreatedProviderType
     */
    protected ProviderType getStorageProviderInstance(StorageProviderModelType model) {
        if (model == null || !model.isEnabled()) {
            return null;
        }

        @SuppressWarnings("unchecked")
        ProviderType instance = (ProviderType) session.getAttribute(model.getId());
        if (instance != null) return instance;

        ComponentFactory<? extends ProviderType, ProviderType> factory = getStorageProviderFactory(model.getProviderId());
        instance = factory.create(session, model);
        if (instance == null) {
            throw new IllegalStateException("StorageProvideFactory (of type " + factory.getClass().getName() + ") produced a null instance");
        }
        session.enlistForClose(instance);
        session.setAttribute(model.getId(), instance);
        return instance;
    }

    /**
     * Stream of ComponentModels of storageType.
     * @param realm Realm.
     * @param storageType Type.
     * @return Stream of ComponentModels
     */
    public static Stream<ComponentModel> getStorageProviderModels(RealmModel realm, Class<? extends Provider> storageType) {
        return realm.getStorageProviders(storageType);
    }
}
