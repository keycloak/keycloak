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

import java.util.Map;
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
import java.util.function.Function;
import java.util.Set;
import java.util.stream.Stream;
import org.keycloak.models.ClientScopeModel;

import static org.keycloak.utils.StreamsUtil.paginatedStream;

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

    public static boolean hasEnabledStorageProviders(KeycloakSession session, RealmModel realm, Class<?> type) {
        return getStorageProviders(realm, session, type).anyMatch(ClientStorageProviderModel::isEnabled);
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

    @Override
    public Stream<ClientModel> searchClientsByClientIdStream(RealmModel realm, String clientId, Integer firstResult, Integer maxResults) {
        return query((p, f, m) -> p.searchClientsByClientIdStream(realm, clientId, f, m), realm, firstResult, maxResults);
    }

    @Override
    public Stream<ClientModel> searchClientsByAttributes(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        return query((p, f, m) -> p.searchClientsByAttributes(realm, attributes, f, m), realm, firstResult, maxResults);
    }

    @FunctionalInterface
    interface PaginatedQuery {
        Stream<ClientModel> query(ClientLookupProvider provider, Integer firstResult, Integer maxResults);
    }

    protected Stream<ClientModel> query(PaginatedQuery paginatedQuery, RealmModel realm, Integer firstResult, Integer maxResults) {
        if (maxResults != null && maxResults == 0) return Stream.empty();

        // when there are external providers involved, we can't do pagination at the lower data layer as we don't know
        // how many results there will be; i.e. we need to query the clients without paginating them and perform pagination
        // later at this level
        if (hasEnabledStorageProviders(session, realm, ClientLookupProvider.class)) {
            Stream<ClientLookupProvider> providersStream = Stream.concat(Stream.of(session.clientLocalStorage()), getEnabledStorageProviders(session, realm, ClientLookupProvider.class));

            /*
              Obtaining clients from an external client storage is time-bounded. In case the external client storage
              isn't available at least clients from a local storage are returned, otherwise both storages are used. For this purpose
              the {@link org.keycloak.services.DefaultKeycloakSessionFactory#getClientStorageProviderTimeout()} property is used.
              Default value is 3000 milliseconds and it's configurable.
              See {@link org.keycloak.services.DefaultKeycloakSessionFactory} for details.
             */
            Function<ClientLookupProvider, Stream<? extends ClientModel>> performQueryWithTimeBound = (p) -> {
                if (p instanceof ClientStorageProvider) {
                    return ServicesUtils.timeBound(session, clientStorageProviderTimeout, p2 -> paginatedQuery.query((ClientLookupProvider) p2, null, null)).apply(p);
                }
                else {
                    return paginatedQuery.query(p, null, null);
                }
            };

            Stream<ClientModel> res = providersStream.flatMap(performQueryWithTimeBound);
            return paginatedStream(res, firstResult, maxResults);
        }
        else {
            return paginatedQuery.query(session.clientLocalStorage(), firstResult, maxResults);
        }
    }

    @Override
    public Map<String, ClientScopeModel> getClientScopes(RealmModel realm, ClientModel client, boolean defaultScopes) {
        StorageId storageId = new StorageId(client.getId());
        if (storageId.getProviderId() == null) {
            return session.clientLocalStorage().getClientScopes(realm, client, defaultScopes);
        }
        ClientLookupProvider provider = (ClientLookupProvider)getStorageProvider(session, client.getRealm(), storageId.getProviderId());
        if (provider == null) return null;
        if (!isStorageProviderEnabled(client.getRealm(), storageId.getProviderId())) return null;
        return provider.getClientScopes(realm, client, defaultScopes);
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
    public void addClientScopes(RealmModel realm, ClientModel client, Set<ClientScopeModel> clientScopes, boolean defaultScope) {
        if (!StorageId.isLocalStorage(client.getId())) {
            throw new RuntimeException("Federated clients do not support this operation");
        }
        session.clientLocalStorage().addClientScopes(realm, client, clientScopes, defaultScope);
    }

    @Override
    public void removeClientScope(RealmModel realm, ClientModel client, ClientScopeModel clientScope) {
        if (!StorageId.isLocalStorage(client.getId())) {
            throw new RuntimeException("Federated clients do not support this operation");
        }
        session.clientLocalStorage().removeClientScope(realm, client, clientScope);
    }

    @Override
    public Map<ClientModel, Set<String>> getAllRedirectUrisOfEnabledClients(RealmModel realm) {
        return session.clientLocalStorage().getAllRedirectUrisOfEnabledClients(realm);
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
