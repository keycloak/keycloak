/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
import org.keycloak.common.util.reflections.Types;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.client.ClientLookupProvider;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.storage.client.ClientStorageProviderFactory;
import org.keycloak.storage.client.ClientStorageProviderModel;
import org.keycloak.utils.ServicesUtils;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientStorageManager implements ClientProvider {
    private static final Logger logger = Logger.getLogger(ClientStorageManager.class);

    protected KeycloakSession session;

    private long clientStorageProviderTimeout;

    public static boolean isStorageProviderEnabled(RealmModel realm, String providerId) {
        ClientStorageProviderModel model = getStorageProviderModel(realm, providerId);
        return model.isEnabled();
    }

    public static ClientStorageProviderModel getStorageProviderModel(RealmModel realm, String componentId) {
        ComponentModel model = realm.getComponent(componentId);
        if (model == null) return null;
        return new ClientStorageProviderModel(model);
    }

    public static ClientStorageProvider getStorageProvider(KeycloakSession session, RealmModel realm, String componentId) {
        ComponentModel model = realm.getComponent(componentId);
        if (model == null) return null;
        ClientStorageProviderModel storageModel = new ClientStorageProviderModel(model);
        ClientStorageProviderFactory factory = (ClientStorageProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(ClientStorageProvider.class, model.getProviderId());
        if (factory == null) {
            throw new ModelException("Could not find ClientStorageProviderFactory for: " + model.getProviderId());
        }
        return getStorageProviderInstance(session, storageModel, factory);
    }


    private static <T> Stream<ClientStorageProviderModel> getStorageProviders(RealmModel realm, KeycloakSession session, Class<T> type) {
        return realm.getClientStorageProvidersStream()
                .filter(model -> {
                    ClientStorageProviderFactory factory = getClientStorageProviderFactory(model, session);
                    if (factory == null) {
                        logger.warnv("Configured ClientStorageProvider {0} of provider id {1} does not exist in realm {2}",
                                model.getName(), model.getProviderId(), realm.getName());
                        return false;
                    } else {
                        return Types.supports(type, factory, ClientStorageProviderFactory.class);
                    }
                });
    }

    public static ClientStorageProvider getStorageProviderInstance(KeycloakSession session, ClientStorageProviderModel model, ClientStorageProviderFactory factory) {
        ClientStorageProvider instance = (ClientStorageProvider)session.getAttribute(model.getId());
        if (instance != null) return instance;
        instance = factory.create(session, model);
        if (instance == null) {
            throw new IllegalStateException("ClientStorageProvideFactory (of type " + factory.getClass().getName() + ") produced a null instance");
        }
        session.enlistForClose(instance);
        session.setAttribute(model.getId(), instance);
        return instance;
    }


    public static <T> Stream<T> getStorageProviders(KeycloakSession session, RealmModel realm, Class<T> type) {
        return getStorageProviders(realm, session, type)
                .map(model -> type.cast(getStorageProviderInstance(session, model, getClientStorageProviderFactory(model, session))));
    }

    private static ClientStorageProviderFactory getClientStorageProviderFactory(ClientStorageProviderModel model, KeycloakSession session) {
        return (ClientStorageProviderFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(ClientStorageProvider.class, model.getProviderId());
    }


    public static <T> Stream<T> getEnabledStorageProviders(KeycloakSession session, RealmModel realm, Class<T> type) {
        return getStorageProviders(realm, session, type)
                .filter(ClientStorageProviderModel::isEnabled)
                .map(model -> type.cast(getStorageProviderInstance(session, model, getClientStorageProviderFactory(model, session))));
    }


    public ClientStorageManager(KeycloakSession session, long clientStorageProviderTimeout) {
        this.session = session;
        this.clientStorageProviderTimeout = clientStorageProviderTimeout;
    }

    @Override
    public ClientModel getClientById(RealmModel realm, String id) {
        StorageId storageId = new StorageId(id);
        if (storageId.getProviderId() == null) {
            return session.clientLocalStorage().getClientById(realm, id);
        }
        ClientLookupProvider provider = (ClientLookupProvider)getStorageProvider(session, realm, storageId.getProviderId());
        if (provider == null) return null;
        if (!isStorageProviderEnabled(realm, storageId.getProviderId())) return null;
        return provider.getClientById(realm, id);
    }

    @Override
    public ClientModel getClientByClientId(RealmModel realm, String clientId) {
        ClientModel client = session.clientLocalStorage().getClientByClientId(realm, clientId);
        if (client != null) {
            return client;
        }
        return getEnabledStorageProviders(session, realm, ClientLookupProvider.class)
                .map(provider -> provider.getClientByClientId(realm, clientId))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Obtaining clients from an external client storage is time-bounded. In case the external client storage
     * isn't available at least clients from a local storage are returned. For this purpose
     * the {@link org.keycloak.services.DefaultKeycloakSessionFactory#getClientStorageProviderTimeout()} property is used.
     * Default value is 3000 milliseconds and it's configurable.
     * See {@link org.keycloak.services.DefaultKeycloakSessionFactory} for details.
     */
    @Override
    public Stream<ClientModel> searchClientsByClientIdStream(RealmModel realm, String clientId, Integer firstResult, Integer maxResults) {
        Stream<ClientModel> local = session.clientLocalStorage().searchClientsByClientIdStream(realm, clientId,  firstResult, maxResults);
        Stream<ClientModel> ext = getEnabledStorageProviders(session, realm, ClientLookupProvider.class)
                .flatMap(ServicesUtils.timeBound(session,
                        clientStorageProviderTimeout,
                        p -> ((ClientLookupProvider) p).searchClientsByClientIdStream(realm, clientId, firstResult, maxResults)));

        return Stream.concat(local, ext);
    }

    @Override
    public ClientModel addClient(RealmModel realm, String clientId) {
        return session.clientLocalStorage().addClient(realm, clientId);
    }

    @Override
    public ClientModel addClient(RealmModel realm, String id, String clientId) {
        return session.clientLocalStorage().addClient(realm, id, clientId);
    }

    @Override
    public Stream<ClientModel> getClientsStream(RealmModel realm, Integer firstResult, Integer maxResults) {
       return session.clientLocalStorage().getClientsStream(realm, firstResult, maxResults);
    }

    @Override
    public Stream<ClientModel> getClientsStream(RealmModel realm) {
        return session.clientLocalStorage().getClientsStream(realm);
    }

    @Override
    public long getClientsCount(RealmModel realm) {
        return session.clientLocalStorage().getClientsCount(realm);
    }

    @Override
    public Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream(RealmModel realm) {
        return session.clientLocalStorage().getAlwaysDisplayInConsoleClientsStream(realm);
    }

    @Override
    public void removeClients(RealmModel realm) {
        session.clientLocalStorage().removeClients(realm);
    }

    @Override
    public void close() {

    }

    @Override
    public boolean removeClient(RealmModel realm, String id) {
        if (!StorageId.isLocalStorage(id)) {
            throw new RuntimeException("Federated clients do not support this operation");
        }
        return session.clientLocalStorage().removeClient(realm, id);
    }



}
