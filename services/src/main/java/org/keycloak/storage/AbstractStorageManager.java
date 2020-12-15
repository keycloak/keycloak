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

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.util.reflections.Types;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.utils.ServicesUtils;

import java.util.Objects;
import java.util.function.Consumer;
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

    private static final Logger LOG = Logger.getLogger(AbstractStorageManager.class);

    /**
     * Timeouts are used as time boundary for obtaining models from an external storage. Default value is set
     * to 3000 milliseconds and it's configurable.
     */
    private static final Long STORAGE_PROVIDER_DEFAULT_TIMEOUT = 3000L;
    protected final KeycloakSession session;
    private final Class<ProviderType> providerTypeClass;
    private final Class<? extends ProviderFactory> factoryTypeClass;
    private final Function<ComponentModel, StorageProviderModelType> toStorageProviderModelTypeFunction;
    private final String configScope;
    private Long storageProviderTimeout;

    public AbstractStorageManager(KeycloakSession session, Class<? extends ProviderFactory> factoryTypeClass, Class<ProviderType> providerTypeClass, Function<ComponentModel, StorageProviderModelType> toStorageProviderModelTypeFunction, String configScope) {
        this.session = session;
        this.providerTypeClass = providerTypeClass;
        this.factoryTypeClass = factoryTypeClass;
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
     * Returns stream of all storageProviders within the realm that implements the capabilityInterface.
     *
     * @param realm realm
     * @param capabilityInterface class of desired capabilityInterface.
     *                            For example, {@code GroupLookupProvider} or {@code UserQueryProvider}
     * @return enabled storage providers for realm and @{code getProviderTypeClass()}
     */
    protected <T> Stream<T> getEnabledStorageProviders(RealmModel realm, Class<T> capabilityInterface) {
        return getStorageProviderModels(realm, providerTypeClass)
                .map(toStorageProviderModelTypeFunction)
                .filter(StorageProviderModelType::isEnabled)
                .sorted(StorageProviderModelType.comparator)
                .map(storageProviderModelType -> getStorageProviderInstance(storageProviderModelType, capabilityInterface, false))
                .filter(Objects::nonNull);
    }

    /**
     * Gets all enabled StorageProviders that implements the capabilityInterface, applies applyFunction on each of
     * them and then join the results together.
     *
     * !! Each StorageProvider has a limited time to respond, if it fails to do it, empty stream is returned !!
     *
     * @param realm realm
     * @param capabilityInterface class of desired capabilityInterface.
     *                            For example, {@code GroupLookupProvider} or {@code UserQueryProvider}
     * @param applyFunction function that is applied on StorageProviders
     * @param <R> result of applyFunction
     * @return a stream with all results from all StorageProviders
     */
    protected <R, T> Stream<R> flatMapEnabledStorageProvidersWithTimeout(RealmModel realm, Class<T> capabilityInterface, Function<T, ? extends Stream<R>> applyFunction) {
        return getEnabledStorageProviders(realm, capabilityInterface)
                .flatMap(ServicesUtils.timeBound(session, getStorageProviderTimeout(), applyFunction));
    }

    /**
     * Gets all enabled StorageProviders that implements the capabilityInterface, applies applyFunction on each of
     * them and returns the stream.
     *
     * !! Each StorageProvider has a limited time to respond, if it fails to do it, null is returned !!
     *
     * @param realm realm
     * @param capabilityInterface class of desired capabilityInterface.
     *                            For example, {@code GroupLookupProvider} or {@code UserQueryProvider}
     * @param applyFunction function that is applied on StorageProviders
     * @param <R> Result of applyFunction
     * @return First result from StorageProviders
     */
    protected <R, T> Stream<R> mapEnabledStorageProvidersWithTimeout(RealmModel realm, Class<T> capabilityInterface, Function<T, R> applyFunction) {
        return getEnabledStorageProviders(realm, capabilityInterface)
                .map(ServicesUtils.timeBoundOne(session, getStorageProviderTimeout(), applyFunction))
                .filter(Objects::nonNull);
    }

    /**
     * Gets all enabled StorageProviders that implements the capabilityInterface and call applyFunction on each
     *
     * !! Each StorageProvider has a limited time for consuming !!
     *
     * @param realm realm
     * @param capabilityInterface class of desired capabilityInterface.
     *                            For example, {@code GroupLookupProvider} or {@code UserQueryProvider}
     * @param consumer function that is applied on StorageProviders
     */
    protected <T> void consumeEnabledStorageProvidersWithTimeout(RealmModel realm, Class<T> capabilityInterface, Consumer<T> consumer) {
        getEnabledStorageProviders(realm, capabilityInterface)
                .forEachOrdered(ServicesUtils.consumeWithTimeBound(session, getStorageProviderTimeout(), consumer));
    }


    protected <T> T getStorageProviderInstance(RealmModel realm, String providerId, Class<T> capabilityInterface) {
        return getStorageProviderInstance(realm, providerId, capabilityInterface, false);
    }

    /**
     * Returns an instance of provider with the providerId within the realm or null if storage provider with providerId
     * doesn't implement capabilityInterface.
     *
     * @param realm realm
     * @param providerId id of ComponentModel within database/storage
     * @param capabilityInterface class of desired capabilityInterface.
     *                            For example, {@code GroupLookupProvider} or {@code UserQueryProvider}
     * @return an instance of type CreatedProviderType or null if storage provider with providerId doesn't implement capabilityInterface
     */
    protected <T> T getStorageProviderInstance(RealmModel realm, String providerId, Class<T> capabilityInterface, boolean includeDisabled) {
        if (providerId == null || capabilityInterface == null) return null;
        return getStorageProviderInstance(getStorageProviderModel(realm, providerId), capabilityInterface, includeDisabled);
    }

    /**
     * Returns an instance of StorageProvider model corresponding realm and providerId
     * @param realm Realm.
     * @param providerId Id of desired provider.
     * @return An instance of type StorageProviderModelType
     */
    protected StorageProviderModelType getStorageProviderModel(RealmModel realm, String providerId) {
        ComponentModel componentModel = realm.getComponent(providerId);
        if (componentModel == null) {
            return null;
        }

        return toStorageProviderModelTypeFunction.apply(componentModel);
    }

    /**
     * Returns an instance of provider for the model or null if storage provider based on the model doesn't implement capabilityInterface.
     *
     * @param model StorageProviderModel obtained from database/storage
     * @param capabilityInterface class of desired capabilityInterface.
     *                            For example, {@code GroupLookupProvider} or {@code UserQueryProvider}
     * @param <T> Required capability interface type
     * @return an instance of type T or null if storage provider based on the model doesn't exist or doesn't implement the capabilityInterface.
     */
    protected <T> T getStorageProviderInstance(StorageProviderModelType model, Class<T> capabilityInterface) {
        return getStorageProviderInstance(model, capabilityInterface, false);
    }

    /**
     * Returns an instance of provider for the model or null if storage provider based on the model doesn't implement capabilityInterface.
     *
     * @param model StorageProviderModel obtained from database/storage
     * @param capabilityInterface class of desired capabilityInterface.
     *                            For example, {@code GroupLookupProvider} or {@code UserQueryProvider}
     * @param includeDisabled If set to true, the method will return also disabled providers.
     * @return an instance of type T or null if storage provider based on the model doesn't exist or doesn't implement the capabilityInterface.
     */
    protected <T> T getStorageProviderInstance(StorageProviderModelType model, Class<T> capabilityInterface, boolean includeDisabled) {
        if (model == null || (!model.isEnabled() && !includeDisabled) || capabilityInterface == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        ProviderType instance = (ProviderType) session.getAttribute(model.getId());
        if (instance != null && capabilityInterface.isAssignableFrom(instance.getClass())) return capabilityInterface.cast(instance);

        ComponentFactory<? extends ProviderType, ProviderType> factory = getStorageProviderFactory(model.getProviderId());
        if (factory == null) {
            LOG.warnv("Configured StorageProvider {0} of provider id {1} does not exist", model.getName(), model.getProviderId());
            return null;
        }
        if (!Types.supports(capabilityInterface, factory, factoryTypeClass)) {
            return null;
        }

        instance = factory.create(session, model);
        if (instance == null) {
            throw new IllegalStateException("StorageProvideFactory (of type " + factory.getClass().getName() + ") produced a null instance");
        }
        session.enlistForClose(instance);
        session.setAttribute(model.getId(), instance);
        return capabilityInterface.cast(instance);
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
