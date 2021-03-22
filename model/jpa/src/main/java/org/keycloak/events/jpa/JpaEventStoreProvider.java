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

package org.keycloak.events.jpa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JpaEventStoreProvider implements EventStoreProvider {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference<Map<String, String>> mapType = new TypeReference<Map<String, String>>() {
    };
    private static final Logger logger = Logger.getLogger(JpaEventStoreProvider.class);

    private final EntityManager em;
    private final int maxDetailLength;

    public JpaEventStoreProvider(EntityManager em, int maxDetailLength) {
        this.em = em;
        this.maxDetailLength = maxDetailLength;
    }

    @Override
    public EventQuery createQuery() {
        return new JpaEventQuery(em);
    }

    @Override
    public void clear() {
        em.createQuery("delete from EventEntity").executeUpdate();
    }

    @Override
    public void clear(String realmId) {
        em.createQuery("delete from EventEntity where realmId = :realmId").setParameter("realmId", realmId).executeUpdate();
    }

    @Override
    public void clear(String realmId, long olderThan) {
        em.createQuery("delete from EventEntity where realmId = :realmId and time < :time").setParameter("realmId", realmId).setParameter("time", olderThan).executeUpdate();
    }

    @Override
    public void onEvent(Event event) {
        em.persist(convertEvent(event));
    }

    @Override
    public AdminEventQuery createAdminQuery() {
        return new JpaAdminEventQuery(em);
    }

    @Override
    public void clearAdmin() {
        em.createQuery("delete from AdminEventEntity").executeUpdate();
    }

    @Override
    public void clearAdmin(String realmId) {
        em.createQuery("delete from AdminEventEntity where realmId = :realmId").setParameter("realmId", realmId).executeUpdate();
    }

    @Override
    public void clearAdmin(String realmId, long olderThan) {
        em.createQuery("delete from AdminEventEntity where realmId = :realmId and time < :time").setParameter("realmId", realmId).setParameter("time", olderThan).executeUpdate();
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        em.persist(convertAdminEvent(event, includeRepresentation));
    }

    @Override
    public void close() {
    }

    private EventEntity convertEvent(Event event) {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setId(UUID.randomUUID().toString());
        eventEntity.setTime(event.getTime());
        eventEntity.setType(event.getType().toString());
        eventEntity.setRealmId(event.getRealmId());
        eventEntity.setClientId(event.getClientId());
        eventEntity.setUserId(event.getUserId());
        eventEntity.setSessionId(event.getSessionId());
        eventEntity.setIpAddress(event.getIpAddress());
        eventEntity.setError(event.getError());
        try {
            if (maxDetailLength > 0 && event.getDetails() != null) {
                Map<String, String> result = new HashMap<>(event.getDetails());
                result.entrySet().forEach(t -> t.setValue(trimToMaxLength(t.getValue())));

                eventEntity.setDetailsJson(mapper.writeValueAsString(result));
            } else {
                eventEntity.setDetailsJson(mapper.writeValueAsString(event.getDetails()));
            }
        } catch (IOException ex) {
            logger.error("Failed to write log details", ex);
        }
        return eventEntity;
    }

    private String trimToMaxLength(String detail) {
        if (detail != null && detail.length() > maxDetailLength) {
            // (maxDetailLength - 3) takes "..." into account
            String result = detail.substring(0, maxDetailLength - 3).concat("...");
            logger.warn("Detail was truncated to " + result);
            return result;
        } else {
            return detail;
        }
    }

    static Event convertEvent(EventEntity eventEntity) {
        Event event = new Event();
        event.setTime(eventEntity.getTime());
        event.setType(EventType.valueOf(eventEntity.getType()));
        event.setRealmId(eventEntity.getRealmId());
        event.setClientId(eventEntity.getClientId());
        event.setUserId(eventEntity.getUserId());
        event.setSessionId(eventEntity.getSessionId());
        event.setIpAddress(eventEntity.getIpAddress());
        event.setError(eventEntity.getError());
        try {
            Map<String, String> details = mapper.readValue(eventEntity.getDetailsJson(), mapType);
            event.setDetails(details);
        } catch (IOException ex) {
            logger.error("Failed to read log details", ex);
        }
        return event;
    }
    
    static AdminEventEntity convertAdminEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        AdminEventEntity adminEventEntity = new AdminEventEntity();
        adminEventEntity.setId(UUID.randomUUID().toString());
        adminEventEntity.setTime(adminEvent.getTime());
        adminEventEntity.setRealmId(adminEvent.getRealmId());
        setAuthDetails(adminEventEntity, adminEvent.getAuthDetails());
        adminEventEntity.setOperationType(adminEvent.getOperationType().toString());

        if (adminEvent.getResourceTypeAsString() != null) {
            adminEventEntity.setResourceType(adminEvent.getResourceTypeAsString());
        }

        adminEventEntity.setResourcePath(adminEvent.getResourcePath());
        adminEventEntity.setError(adminEvent.getError());
        
        if(includeRepresentation) {
            adminEventEntity.setRepresentation(adminEvent.getRepresentation());
        }
        return adminEventEntity;
    }

    static AdminEvent convertAdminEvent(AdminEventEntity adminEventEntity) {
        AdminEvent adminEvent = new AdminEvent();
        adminEvent.setTime(adminEventEntity.getTime());
        adminEvent.setRealmId(adminEventEntity.getRealmId());
        setAuthDetails(adminEvent, adminEventEntity);
        adminEvent.setOperationType(OperationType.valueOf(adminEventEntity.getOperationType()));

        if (adminEventEntity.getResourceType() != null) {
            adminEvent.setResourceTypeAsString(adminEventEntity.getResourceType());
        }

        adminEvent.setResourcePath(adminEventEntity.getResourcePath());
        adminEvent.setError(adminEventEntity.getError());
        
        if(adminEventEntity.getRepresentation() != null) {
            adminEvent.setRepresentation(adminEventEntity.getRepresentation());
        }
        return adminEvent;
    }
    
    private static void setAuthDetails(AdminEventEntity adminEventEntity, AuthDetails authDetails) {
        adminEventEntity.setAuthRealmId(authDetails.getRealmId());
        adminEventEntity.setAuthClientId(authDetails.getClientId());
        adminEventEntity.setAuthUserId(authDetails.getUserId());
        adminEventEntity.setAuthIpAddress(authDetails.getIpAddress());
    }
    
    private static void setAuthDetails(AdminEvent adminEvent, AdminEventEntity adminEventEntity) {
        AuthDetails authDetails = new AuthDetails();
        authDetails.setRealmId(adminEventEntity.getAuthRealmId());
        authDetails.setClientId(adminEventEntity.getAuthClientId());
        authDetails.setUserId(adminEventEntity.getAuthUserId());
        authDetails.setIpAddress(adminEventEntity.getAuthIpAddress());
        adminEvent.setAuthDetails(authDetails);
    }

}
