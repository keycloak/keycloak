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

package org.keycloak.events.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.Event;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.ResourceType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MongoEventStoreProvider implements EventStoreProvider {
    
    private DBCollection events;
    private DBCollection adminEvents;

    public MongoEventStoreProvider(DBCollection events, DBCollection adminEvents) {
        this.events = events;
        this.adminEvents = adminEvents;
    }

    @Override
    public EventQuery createQuery() {
        return new MongoEventQuery(events);
    }

    @Override
    public void clear() {
        events.remove(new BasicDBObject());
    }

    @Override
    public void clear(String realmId) {
        events.remove(new BasicDBObject("realmId", realmId));
    }

    @Override
    public void clear(String realmId, long olderThan) {
        BasicDBObject q = new BasicDBObject();
        q.put("realmId", realmId);
        q.put("time", new BasicDBObject("$lt", olderThan));
        events.remove(q);
    }

    @Override
    public void onEvent(Event event) {
        events.insert(convertEvent(event));
    }

    @Override
    public AdminEventQuery createAdminQuery() {
        return new MongoAdminEventQuery(adminEvents);
    }

    @Override
    public void clearAdmin() {
        adminEvents.remove(new BasicDBObject());
    }

    @Override
    public void clearAdmin(String realmId) {
        adminEvents.remove(new BasicDBObject("realmId", realmId));
    }

    @Override
    public void clearAdmin(String realmId, long olderThan) {
        BasicDBObject q = new BasicDBObject();
        q.put("realmId", realmId);
        q.put("time", new BasicDBObject("$lt", olderThan));
        adminEvents.remove(q);
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        adminEvents.insert(convertAdminEvent(adminEvent, includeRepresentation));
    }

    @Override
    public void close() {
    }

    static DBObject convertEvent(Event event) {
        BasicDBObject e = new BasicDBObject();
        e.put("time", event.getTime());
        e.put("type", event.getType().toString());
        e.put("realmId", event.getRealmId());
        e.put("clientId", event.getClientId());
        e.put("userId", event.getUserId());
        e.put("sessionId", event.getSessionId());
        e.put("ipAddress", event.getIpAddress());
        e.put("error", event.getError());

        BasicDBObject details = new BasicDBObject();
        if (event.getDetails() != null) {
            for (Map.Entry<String, String> entry : event.getDetails().entrySet()) {
                details.put(entry.getKey(), entry.getValue());
            }
        }
        e.put("details", details);

        return e;
    }

    static Event convertEvent(BasicDBObject o) {
        Event event = new Event();
        event.setTime(o.getLong("time"));
        event.setType(EventType.valueOf(o.getString("type")));
        event.setRealmId(o.getString("realmId"));
        event.setClientId(o.getString("clientId"));
        event.setUserId(o.getString("userId"));
        event.setSessionId(o.getString("sessionId"));
        event.setIpAddress(o.getString("ipAddress"));
        event.setError(o.getString("error"));

        BasicDBObject d = (BasicDBObject) o.get("details");
        if (d != null) {
            Map<String, String> details = new HashMap<String, String>();
            for (Object k : d.keySet()) {
                details.put((String) k, d.getString((String) k));
            }
            event.setDetails(details);
        }

        return event;
    }
    
    private static DBObject convertAdminEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        BasicDBObject e = new BasicDBObject();
        e.put("time", adminEvent.getTime());
        e.put("realmId", adminEvent.getRealmId());
        e.put("operationType", adminEvent.getOperationType().toString());
        setAuthDetails(e, adminEvent.getAuthDetails());
        e.put("resourcePath", adminEvent.getResourcePath());
        e.put("error", adminEvent.getError());
        
        if(includeRepresentation) {
            e.put("representation", adminEvent.getRepresentation());
        }

        return e;
    }
    
    static AdminEvent convertAdminEvent(BasicDBObject o) {
        AdminEvent adminEvent = new AdminEvent();
        adminEvent.setTime(o.getLong("time"));
        adminEvent.setRealmId(o.getString("realmId"));
        adminEvent.setOperationType(OperationType.valueOf(o.getString("operationType")));
        if (o.getString("resourceType") != null) {
            adminEvent.setResourceType(ResourceType.valueOf(o.getString("resourceType")));
        }
        setAuthDetails(adminEvent, o);
        adminEvent.setResourcePath(o.getString("resourcePath"));
        adminEvent.setError(o.getString("error"));
        
        if(o.getString("representation") != null) {
            adminEvent.setRepresentation(o.getString("representation"));
        }
        return adminEvent;
    }

    private static void setAuthDetails(BasicDBObject e, AuthDetails authDetails) {
        e.put("authRealmId", authDetails.getRealmId());
        e.put("authClientId", authDetails.getClientId());
        e.put("authUserId", authDetails.getUserId());
        e.put("authIpAddress", authDetails.getIpAddress());
    }
    
    private static void setAuthDetails(AdminEvent adminEvent, BasicDBObject o) {
        AuthDetails authDetails = new AuthDetails();
        authDetails.setRealmId(o.getString("authRealmId"));
        authDetails.setClientId(o.getString("authClientId"));
        authDetails.setUserId(o.getString("authUserId"));
        authDetails.setIpAddress(o.getString("authIpAddress"));
        adminEvent.setAuthDetails(authDetails);
    }

}
