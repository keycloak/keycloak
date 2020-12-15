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

package org.keycloak.models.map.client;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientModel.ClientUpdatedEvent;
import org.keycloak.models.ClientModel.SearchableFields;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;

import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.common.Serialization;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.keycloak.models.map.storage.MapStorage;

import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.utils.StreamsUtil.paginatedStream;

public class MapClientProvider implements ClientProvider {

    private static final Logger LOG = Logger.getLogger(MapClientProvider.class);
    private static final Predicate<MapClientEntity> ALWAYS_FALSE = c -> { return false; };
    private final KeycloakSession session;
    final MapKeycloakTransaction<UUID, MapClientEntity, ClientModel> tx;
    private final MapStorage<UUID, MapClientEntity, ClientModel> clientStore;
    private final ConcurrentMap<UUID, ConcurrentMap<String, Integer>> clientRegisteredNodesStore;

    private static final Comparator<MapClientEntity> COMPARE_BY_CLIENT_ID = Comparator.comparing(MapClientEntity::getClientId);

    public MapClientProvider(KeycloakSession session, MapStorage<UUID, MapClientEntity, ClientModel> clientStore, ConcurrentMap<UUID, ConcurrentMap<String, Integer>> clientRegisteredNodesStore) {
        this.session = session;
        this.clientStore = clientStore;
        this.clientRegisteredNodesStore = clientRegisteredNodesStore;
        this.tx = clientStore.createTransaction();
        session.getTransactionManager().enlist(tx);
    }

    private ClientUpdatedEvent clientUpdatedEvent(ClientModel c) {
        return new ClientModel.ClientUpdatedEvent() {
            @Override
            public ClientModel getUpdatedClient() {
                return c;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        };
    }

    private MapClientEntity registerEntityForChanges(MapClientEntity origEntity) {
        final MapClientEntity res = tx.read(origEntity.getId(), id -> Serialization.from(origEntity));
        tx.updateIfChanged(origEntity.getId(), res, MapClientEntity::isUpdated);
        return res;
    }

    private Function<MapClientEntity, ClientModel> entityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller

        return origEntity -> new MapClientAdapter(session, realm, registerEntityForChanges(origEntity)) {
            @Override
            public void updateClient() {
                LOG.tracef("updateClient(%s)%s", realm, origEntity.getId(), getShortStackTrace());
                session.getKeycloakSessionFactory().publish(clientUpdatedEvent(this));
            }

            /** This is runtime information and should have never been part of the adapter */
            @Override
            public Map<String, Integer> getRegisteredNodes() {
                return clientRegisteredNodesStore.computeIfAbsent(entity.getId(), k -> new ConcurrentHashMap<>());
            }

            @Override
            public void registerNode(String nodeHost, int registrationTime) {
                Map<String, Integer> value = getRegisteredNodes();
                value.put(nodeHost, registrationTime);
            }

            @Override
            public void unregisterNode(String nodeHost) {
                getRegisteredNodes().remove(nodeHost);
            }

        };
    }

    private Predicate<MapClientEntity> entityRealmFilter(RealmModel realm) {
        if (realm == null || realm.getId() == null) {
            return MapClientProvider.ALWAYS_FALSE;
        }
        String realmId = realm.getId();
        return entity -> Objects.equals(realmId, entity.getRealmId());
    }

    @Override
    public Stream<ClientModel> getClientsStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        return paginatedStream(getClientsStream(realm), firstResult, maxResults);
    }

    @Override
    public Stream<ClientModel> getClientsStream(RealmModel realm) {
        ModelCriteriaBuilder<ClientModel> mcb = clientStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        return tx.getUpdatedNotRemoved(mcb)
          .sorted(COMPARE_BY_CLIENT_ID)
          .map(entityToAdapterFunc(realm))
        ;
    }

    @Override
    public ClientModel addClient(RealmModel realm, String id, String clientId) {
        final UUID entityId = id == null ? UUID.randomUUID() : UUID.fromString(id);

        if (clientId == null) {
            clientId = entityId.toString();
        }

        LOG.tracef("addClient(%s, %s, %s)%s", realm, id, clientId, getShortStackTrace());

        MapClientEntity entity = new MapClientEntity(entityId, realm.getId());
        entity.setClientId(clientId);
        entity.setEnabled(true);
        entity.setStandardFlowEnabled(true);
        if (tx.read(entity.getId()) != null) {
            throw new ModelDuplicateException("Client exists: " + id);
        }
        tx.create(entity.getId(), entity);
        final ClientModel resource = entityToAdapterFunc(realm).apply(entity);

        // TODO: Sending an event should be extracted to store layer
        session.getKeycloakSessionFactory().publish((ClientModel.ClientCreationEvent) () -> resource);
        resource.updateClient();        // This is actualy strange contract - it should be the store code to call updateClient

        return resource;
    }

    @Override
    public Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream(RealmModel realm) {
        return getClientsStream(realm)
                .filter(ClientModel::isAlwaysDisplayInConsole);
    }

    @Override
    public void removeClients(RealmModel realm) {
        LOG.tracef("removeClients(%s)%s", realm, getShortStackTrace());

        getClientsStream(realm)
          .map(ClientModel::getId)
          .collect(Collectors.toSet())  // This is necessary to read out all the client IDs before removing the clients
          .forEach(cid -> removeClient(realm, cid));
    }

    @Override
    public boolean removeClient(RealmModel realm, String id) {
        if (id == null) {
            return false;
        }

        LOG.tracef("removeClient(%s, %s)%s", realm, id, getShortStackTrace());

        // TODO: Sending an event (and client role removal) should be extracted to store layer
        final ClientModel client = getClientById(realm, id);
        if (client == null) return false;
        session.users().preRemove(realm, client);
        session.roles().removeRoles(client);

        session.getKeycloakSessionFactory().publish(new ClientModel.ClientRemovedEvent() {
            @Override
            public ClientModel getClient() {
                return client;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });
        // TODO: ^^^^^^^ Up to here

        tx.delete(UUID.fromString(id));

        return true;
    }

    @Override
    public long getClientsCount(RealmModel realm) {
        ModelCriteriaBuilder<ClientModel> mcb = clientStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        return this.clientStore.getCount(mcb);
    }

    @Override
    public ClientModel getClientById(RealmModel realm, String id) {
        if (id == null) {
            return null;
        }

        LOG.tracef("getClientById(%s, %s)%s", realm, id, getShortStackTrace());

        MapClientEntity entity = tx.read(UUID.fromString(id));
        return (entity == null || ! entityRealmFilter(realm).test(entity))
          ? null
          : entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public ClientModel getClientByClientId(RealmModel realm, String clientId) {
        if (clientId == null) {
            return null;
        }
        LOG.tracef("getClientByClientId(%s, %s)%s", realm, clientId, getShortStackTrace());

        ModelCriteriaBuilder<ClientModel> mcb = clientStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.CLIENT_ID, Operator.ILIKE, clientId);

        return tx.getUpdatedNotRemoved(mcb)
          .map(entityToAdapterFunc(realm))
          .findFirst()
          .orElse(null)
        ;
    }

    @Override
    public Stream<ClientModel> searchClientsByClientIdStream(RealmModel realm, String clientId, Integer firstResult, Integer maxResults) {
        if (clientId == null) {
            return Stream.empty();
        }

        ModelCriteriaBuilder<ClientModel> mcb = clientStore.createCriteriaBuilder()
          .compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
          .compare(SearchableFields.CLIENT_ID, Operator.ILIKE, "%" + clientId + "%");

        Stream<MapClientEntity> s = tx.getUpdatedNotRemoved(mcb)
          .sorted(COMPARE_BY_CLIENT_ID);

        return paginatedStream(s, firstResult, maxResults).map(entityToAdapterFunc(realm));
    }

    @Override
    public void close() {
        
    }

}
