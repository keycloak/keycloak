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

package org.keycloak.models.map.events;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.events.Event;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.common.ExpirableEntity;
import org.keycloak.models.map.common.HasRealmId;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;

import java.util.stream.Stream;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.map.common.ExpirationUtils.isExpired;
import static org.keycloak.models.map.events.EventUtils.modelToEntity;

public class MapEventStoreProvider implements EventStoreProvider {

    private static final Logger LOG = Logger.getLogger(MapEventStoreProvider.class);
    private final KeycloakSession session;
    private final MapKeycloakTransaction<MapAuthEventEntity, Event> authEventsTX;
    private final MapKeycloakTransaction<MapAdminEventEntity, AdminEvent> adminEventsTX;
    private final boolean adminTxHasRealmId;
    private final boolean authTxHasRealmId;

    public MapEventStoreProvider(KeycloakSession session, MapStorage<MapAuthEventEntity, Event> loginEventsStore, MapStorage<MapAdminEventEntity, AdminEvent> adminEventsStore) {
        this.session = session;
        this.authEventsTX = loginEventsStore.createTransaction(session);
        this.adminEventsTX = adminEventsStore.createTransaction(session);

        session.getTransactionManager().enlistAfterCompletion(this.authEventsTX);
        session.getTransactionManager().enlistAfterCompletion(this.adminEventsTX);
        this.authTxHasRealmId = this.authEventsTX instanceof HasRealmId;
        this.adminTxHasRealmId = this.adminEventsTX instanceof HasRealmId;
    }

    private MapKeycloakTransaction<MapAdminEventEntity, AdminEvent> adminTxInRealm(String realmId) {
        if (adminTxHasRealmId) {
            ((HasRealmId) adminEventsTX).setRealmId(realmId);
        }
        return adminEventsTX;
    }

    private MapKeycloakTransaction<MapAdminEventEntity, AdminEvent> adminTxInRealm(RealmModel realm) {
        return adminTxInRealm(realm == null ? null : realm.getId());
    }

    private MapKeycloakTransaction<MapAuthEventEntity, Event> authTxInRealm(String realmId) {
        if (authTxHasRealmId) {
            ((HasRealmId) authEventsTX).setRealmId(realmId);
        }
        return authEventsTX;
    }

    private MapKeycloakTransaction<MapAuthEventEntity, Event> authTxInRealm(RealmModel realm) {
        return authTxInRealm(realm == null ? null : realm.getId());
    }

    /** LOGIN EVENTS **/
    @Override
    public void onEvent(Event event) {
        LOG.tracef("onEvent(%s)%s", event, getShortStackTrace());
        String id = event.getId();
        String realmId = event.getRealmId();

        if (id != null && authTxInRealm(realmId).exists(id)) {
            throw new ModelDuplicateException("Event already exists: " + id);
        }

        MapAuthEventEntity entity = modelToEntity(event);
        if (realmId != null) {
            RealmModel realm = session.realms().getRealm(realmId);
            if (realm != null && realm.getEventsExpiration() > 0) {
                entity.setExpiration(Time.currentTimeMillis() + (realm.getEventsExpiration() * 1000));
            }
        }

        authTxInRealm(realmId).create(entity);
    }

    @Override
    public EventQuery createQuery() {
        LOG.tracef("createQuery()%s", getShortStackTrace());
        return new MapAuthEventQuery() {
            private boolean filterExpired(ExpirableEntity event) {
                // Check if entity is expired
                if (isExpired(event, true)) {
                    // Remove entity
                    authTxInRealm(realmId).delete(event.getId());

                    return false; // Do not include entity in the resulting stream
                }

                return true; // Entity is not expired
            }

            @Override
            protected Stream<Event> read(QueryParameters<Event> queryParameters) {
                return authTxInRealm(realmId).read(queryParameters)
                  .filter(this::filterExpired)
                  .map(EventUtils::entityToModel);
            }
        };
    }

    @Override
    public void clear() {
        LOG.tracef("clear()%s", getShortStackTrace());
        authTxInRealm((String) null).delete(QueryParameters.withCriteria(DefaultModelCriteria.criteria()));
    }

    @Override
    public void clear(RealmModel realm) {
        LOG.tracef("clear(%s)%s", realm, getShortStackTrace());
        authTxInRealm(realm).delete(QueryParameters.withCriteria(DefaultModelCriteria.<Event>criteria()
                .compare(Event.SearchableFields.REALM_ID, ModelCriteriaBuilder.Operator.EQ, realm.getId())));
    }

    @Override
    public void clear(RealmModel realm, long olderThan) {
        LOG.tracef("clear(%s, %d)%s", realm, olderThan, getShortStackTrace());
        authTxInRealm(realm).delete(QueryParameters.withCriteria(DefaultModelCriteria.<Event>criteria()
                .compare(Event.SearchableFields.REALM_ID, ModelCriteriaBuilder.Operator.EQ, realm.getId())
                .compare(Event.SearchableFields.TIMESTAMP, ModelCriteriaBuilder.Operator.LT, olderThan)
        ));
    }

    @Override
    public void clearExpiredEvents() {
        LOG.tracef("clearExpiredEvents()%s", getShortStackTrace());
        LOG.warnf("Clearing expired entities should not be triggered manually. It is responsibility of the store to clear these.");
    }

    /** ADMIN EVENTS **/

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        LOG.tracef("onEvent(%s, %s)%s", event, includeRepresentation, getShortStackTrace());
        String id = event.getId();
        String realmId = event.getRealmId();
        if (id != null && adminTxInRealm(realmId).exists(id)) {
            throw new ModelDuplicateException("Event already exists: " + id);
        }
        MapAdminEventEntity entity = modelToEntity(event,includeRepresentation);
        if (realmId != null) {
            RealmModel realm = session.realms().getRealm(realmId);
            if (realm != null) {
                Long expiration = realm.getAttribute("adminEventsExpiration",0L);
                if (expiration > 0) {
                    entity.setExpiration(Time.currentTimeMillis() + (expiration * 1000));
                }
            }
        }
        adminTxInRealm(realmId).create(entity);
    }

    @Override
    public AdminEventQuery createAdminQuery() {
        LOG.tracef("createAdminQuery()%s", getShortStackTrace());
        return new MapAdminEventQuery() {
            private boolean filterExpired(ExpirableEntity event) {
                // Check if entity is expired
                if (isExpired(event, true)) {
                    // Remove entity
                    authTxInRealm(realmId).delete(event.getId());

                    return false; // Do not include entity in the resulting stream
                }

                return true; // Entity is not expired
            }

            @Override
            protected Stream<AdminEvent> read(QueryParameters<AdminEvent> queryParameters) {
                return adminTxInRealm(realmId).read(queryParameters)
                  .filter(this::filterExpired)
                  .map(EventUtils::entityToModel);
            }
        };
    }

    @Override
    public void clearAdmin() {
        LOG.tracef("clearAdmin()%s", getShortStackTrace());
        adminTxInRealm((String) null).delete(QueryParameters.withCriteria(DefaultModelCriteria.criteria()));
    }

    @Override
    public void clearAdmin(RealmModel realm) {
        LOG.tracef("clearAdmin(%s)%s", realm, getShortStackTrace());
        adminTxInRealm(realm).delete(QueryParameters.withCriteria(DefaultModelCriteria.<AdminEvent>criteria()
                .compare(AdminEvent.SearchableFields.REALM_ID, ModelCriteriaBuilder.Operator.EQ, realm.getId())));
    }

    @Override
    public void clearAdmin(RealmModel realm, long olderThan) {
        LOG.tracef("clearAdmin(%s, %d)%s", realm, olderThan, getShortStackTrace());
        adminTxInRealm(realm).delete(QueryParameters.withCriteria(DefaultModelCriteria.<AdminEvent>criteria()
                .compare(AdminEvent.SearchableFields.REALM_ID, ModelCriteriaBuilder.Operator.EQ, realm.getId())
                .compare(AdminEvent.SearchableFields.TIMESTAMP, ModelCriteriaBuilder.Operator.LT, olderThan)
        ));
    }

    @Override
    public void close() {

    }
}
