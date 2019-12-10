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
import org.keycloak.models.RoleModel;
import org.keycloak.storage.client.ClientLookupProvider;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.storage.client.ClientStorageProviderFactory;
import org.keycloak.storage.client.ClientStorageProviderModel;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientStorageManager implements ClientProvider {
    private static final Logger logger = Logger.getLogger(ClientStorageManager.class);

    protected KeycloakSession session;

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


    public static List<ClientStorageProviderModel> getStorageProviders(RealmModel realm) {
        return realm.getClientStorageProviders();
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


    public static <T> List<T> getStorageProviders(KeycloakSession session, RealmModel realm, Class<T> type) {
        List<T> list = new LinkedList<>();
        for (ClientStorageProviderModel model : getStorageProviders(realm)) {
            ClientStorageProviderFactory factory = (ClientStorageProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(ClientStorageProvider.class, model.getProviderId());
            if (factory == null) {
                logger.warnv("Configured ClientStorageProvider {0} of provider id {1} does not exist in realm {2}", model.getName(), model.getProviderId(), realm.getName());
                continue;
            }
            if (Types.supports(type, factory, ClientStorageProviderFactory.class)) {
                list.add(type.cast(getStorageProviderInstance(session, model, factory)));
            }


        }
        return list;
    }


    public static <T> List<T> getEnabledStorageProviders(KeycloakSession session, RealmModel realm, Class<T> type) {
        List<T> list = new LinkedList<>();
        for (ClientStorageProviderModel model : getStorageProviders(realm)) {
            if (!model.isEnabled()) continue;
            ClientStorageProviderFactory factory = (ClientStorageProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(ClientStorageProvider.class, model.getProviderId());
            if (factory == null) {
                logger.warnv("Configured ClientStorageProvider {0} of provider id {1} does not exist in realm {2}", model.getName(), model.getProviderId(), realm.getName());
                continue;
            }
            if (Types.supports(type, factory, ClientStorageProviderFactory.class)) {
                list.add(type.cast(getStorageProviderInstance(session, model, factory)));
            }


        }
        return list;
    }


    public ClientStorageManager(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public ClientModel getClientById(String id, RealmModel realm) {
        StorageId storageId = new StorageId(id);
        if (storageId.getProviderId() == null) {
            return session.clientLocalStorage().getClientById(id, realm);
        }
        ClientLookupProvider provider = (ClientLookupProvider)getStorageProvider(session, realm, storageId.getProviderId());
        if (provider == null) return null;
        if (!isStorageProviderEnabled(realm, storageId.getProviderId())) return null;
        return provider.getClientById(id, realm);
    }

    @Override
    public ClientModel getClientByClientId(String clientId, RealmModel realm) {
        ClientModel client = session.clientLocalStorage().getClientByClientId(clientId, realm);
        if (client != null) {
            return client;
        }
        for (ClientLookupProvider provider : getEnabledStorageProviders(session, realm, ClientLookupProvider.class)) {
            client = provider.getClientByClientId(clientId, realm);
            if (client != null) return client;
        }
        return null;
    }

    @Override
    public List<ClientModel> searchClientsByClientId(String clientId, Integer firstResult, Integer maxResults, RealmModel realm) {
        List<ClientModel> clients = session.clientLocalStorage().searchClientsByClientId(clientId,  firstResult, maxResults, realm);
        if (clients != null) {
            return clients;
        }
        for (ClientLookupProvider provider : getEnabledStorageProviders(session, realm, ClientLookupProvider.class)) {
            clients = provider.searchClientsByClientId(clientId, firstResult, maxResults, realm);
            if (clients != null) return clients;
        }
        return null;
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
    public List<ClientModel> getClients(RealmModel realm, Integer firstResult, Integer maxResults) {
       return session.clientLocalStorage().getClients(realm, firstResult, maxResults);
    }

    @Override
    public List<ClientModel> getClients(RealmModel realm) {
        return session.clientLocalStorage().getClients(realm);
    }

    @Override
    public RoleModel addClientRole(RealmModel realm, ClientModel client, String name) {
        if (!StorageId.isLocalStorage(client.getId())) {
            throw new RuntimeException("Federated clients do not support this operation");
        }
        return session.clientLocalStorage().addClientRole(realm, client, name);
    }

    @Override
    public RoleModel addClientRole(RealmModel realm, ClientModel client, String id, String name) {
        if (!StorageId.isLocalStorage(client.getId())) {
            throw new RuntimeException("Federated clients do not support this operation");
        }
        return session.clientLocalStorage().addClientRole(realm, client, id, name);
    }

    @Override
    public RoleModel getClientRole(RealmModel realm, ClientModel client, String name) {
        if (!StorageId.isLocalStorage(client.getId())) {
            //throw new RuntimeException("Federated clients do not support this operation");
            return null;
        }
        return session.clientLocalStorage().getClientRole(realm, client, name);
    }

    @Override
    public Set<RoleModel> getClientRoles(RealmModel realm, ClientModel client) {
        if (!StorageId.isLocalStorage(client.getId())) {
            //throw new RuntimeException("Federated clients do not support this operation");
            return Collections.EMPTY_SET;
        }
        return session.clientLocalStorage().getClientRoles(realm, client);
    }

    @Override
    public List<ClientModel> getAlwaysDisplayInConsoleClients(RealmModel realm) {
        return session.clientLocalStorage().getAlwaysDisplayInConsoleClients(realm);
    }

    @Override
    public void close() {

    }

    @Override
    public boolean removeClient(String id, RealmModel realm) {
        if (!StorageId.isLocalStorage(id)) {
            throw new RuntimeException("Federated clients do not support this operation");
        }
        return session.clientLocalStorage().removeClient(id, realm);
    }



}
