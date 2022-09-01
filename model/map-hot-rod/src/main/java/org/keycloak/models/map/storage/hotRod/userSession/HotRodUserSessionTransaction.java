/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.hotRod.userSession;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.common.delegate.SimpleDelegateProvider;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapCrudOperations;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapKeycloakTransaction;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntityDelegate;
import org.keycloak.models.map.userSession.MapUserSessionEntity;
import org.keycloak.models.map.userSession.MapUserSessionEntityDelegate;
import org.keycloak.storage.SearchableModelField;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator.IN;

public class HotRodUserSessionTransaction<K> extends ConcurrentHashMapKeycloakTransaction<K, MapUserSessionEntity, UserSessionModel> {

    private final MapKeycloakTransaction<MapAuthenticatedClientSessionEntity, AuthenticatedClientSessionModel> clientSessionTransaction;

    public HotRodUserSessionTransaction(ConcurrentHashMapCrudOperations<MapUserSessionEntity, UserSessionModel> map,
                                        StringKeyConverter<K> keyConverter,
                                        DeepCloner cloner,
                                        Map<SearchableModelField<? super UserSessionModel>, MapModelCriteriaBuilder.UpdatePredicatesFunc<K, MapUserSessionEntity, UserSessionModel>> fieldPredicates,
                                        MapKeycloakTransaction<MapAuthenticatedClientSessionEntity, AuthenticatedClientSessionModel> clientSessionTransaction
    ) {
        super(map, keyConverter, cloner, fieldPredicates);
        this.clientSessionTransaction = clientSessionTransaction;
    }

    @Override
    public void commit() {
        super.commit();
        clientSessionTransaction.commit();
    }

    private MapAuthenticatedClientSessionEntity wrapClientSessionEntityToClientSessionAwareDelegate(MapAuthenticatedClientSessionEntity d) {
        return new MapAuthenticatedClientSessionEntityDelegate(new HotRodAuthenticatedClientSessionEntityDelegateProvider(d) {
            @Override
            public MapAuthenticatedClientSessionEntity loadClientSessionFromDatabase() {
                return clientSessionTransaction.read(d.getId());
            }
        });
    }

    private MapUserSessionEntity wrapUserSessionEntityToClientSessionAwareDelegate(MapUserSessionEntity entity) {
        if (entity == null) return null;

        return new MapUserSessionEntityDelegate(new SimpleDelegateProvider<>(entity)) {
            @Override
            public Set<MapAuthenticatedClientSessionEntity> getAuthenticatedClientSessions() {
                Set<MapAuthenticatedClientSessionEntity> clientSessions = super.getAuthenticatedClientSessions();
                return clientSessions == null ? null : clientSessions.stream()
                        .map(HotRodUserSessionTransaction.this::wrapClientSessionEntityToClientSessionAwareDelegate)
                        .collect(Collectors.toSet());
            }

            @Override
            public Optional<MapAuthenticatedClientSessionEntity> getAuthenticatedClientSession(String clientUUID) {
                return super.getAuthenticatedClientSession(clientUUID)
                        .map(HotRodUserSessionTransaction.this::wrapClientSessionEntityToClientSessionAwareDelegate);
            }

            @Override
            public void addAuthenticatedClientSession(MapAuthenticatedClientSessionEntity clientSession) {
                super.addAuthenticatedClientSession(clientSession);
                clientSessionTransaction.create(clientSession);
            }

            @Override
            public Boolean removeAuthenticatedClientSession(String clientUUID) {
                Optional<MapAuthenticatedClientSessionEntity> clientSession = getAuthenticatedClientSession(clientUUID);
                if (!clientSession.isPresent()) {
                    return false;
                }
                return super.removeAuthenticatedClientSession(clientUUID) && clientSessionTransaction.delete(clientSession.get().getId());
            }

            @Override
            public void clearAuthenticatedClientSessions() {
                Set<MapAuthenticatedClientSessionEntity> clientSessions = super.getAuthenticatedClientSessions();
                if (clientSessions != null) {
                    clientSessionTransaction.delete(QueryParameters.withCriteria(
                            DefaultModelCriteria.<AuthenticatedClientSessionModel>criteria()
                                    .compare(HotRodAuthenticatedClientSessionEntity.ID, IN, clientSessions.stream()
                                            .map(MapAuthenticatedClientSessionEntity::getId))
                    ));
                }
                super.clearAuthenticatedClientSessions();
            }
        };
    }


    @Override
    public MapUserSessionEntity read(String sKey) {
        return wrapUserSessionEntityToClientSessionAwareDelegate(super.read(sKey));
    }

    @Override
    public Stream<MapUserSessionEntity> read(QueryParameters<UserSessionModel> queryParameters) {
        return super.read(queryParameters).map(this::wrapUserSessionEntityToClientSessionAwareDelegate);
    }

    @Override
    public MapUserSessionEntity create(MapUserSessionEntity value) {
        return wrapUserSessionEntityToClientSessionAwareDelegate(super.create(value));
    }

    @Override
    public boolean delete(String key) {
        MapUserSessionEntity uSession = read(key);
        Set<MapAuthenticatedClientSessionEntity> clientSessions = uSession.getAuthenticatedClientSessions();
        if (clientSessions != null) {
            clientSessionTransaction.delete(QueryParameters.withCriteria(
                    DefaultModelCriteria.<AuthenticatedClientSessionModel>criteria()
                            .compare(HotRodAuthenticatedClientSessionEntity.ID, IN, clientSessions.stream()
                                    .map(MapAuthenticatedClientSessionEntity::getId))
            ));
        }

        return super.delete(key);
    }

    @Override
    public long delete(QueryParameters<UserSessionModel> queryParameters) {
        clientSessionTransaction.delete(QueryParameters.withCriteria(
                DefaultModelCriteria.<AuthenticatedClientSessionModel>criteria()
                        .compare(HotRodAuthenticatedClientSessionEntity.ID, IN, read(queryParameters)
                                .flatMap(userSession -> Optional.ofNullable(userSession.getAuthenticatedClientSessions()).orElse(Collections.emptySet()).stream().map(AbstractEntity::getId)))
        ));

        return super.delete(queryParameters);
    }
}
