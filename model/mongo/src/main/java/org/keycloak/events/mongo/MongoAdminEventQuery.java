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

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.OperationType;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import org.keycloak.events.admin.ResourceType;

public class MongoAdminEventQuery implements AdminEventQuery{
    
    private Integer firstResult;
    private Integer maxResults;
    private DBCollection audit;
    private final BasicDBObject query;

    public MongoAdminEventQuery(DBCollection audit) {
        this.audit = audit;
        query = new BasicDBObject();
    }
    
    @Override
    public AdminEventQuery realm(String realmId) {
        query.put("realmId", realmId);
        return this;
    }

    @Override
    public AdminEventQuery operation(OperationType... operations) {
        List<String> operationStrings = new LinkedList<String>();
        for (OperationType e : operations) {
            operationStrings.add(e.toString());
        }
        query.put("operationType", new BasicDBObject("$in", operationStrings));
        return this;
    }

    @Override
    public AdminEventQuery resourceType(ResourceType... resourceTypes) {

        List<String> resourceTypeStrings = new LinkedList<String>();
        for (ResourceType e : resourceTypes) {
            resourceTypeStrings.add(e.toString());
        }
        query.put("resourceType", new BasicDBObject("$in", resourceTypeStrings));

        return this;
    }
    
    @Override
    public AdminEventQuery authRealm(String authRealmId) {
        query.put("authRealmId", authRealmId);
        return this;
    }

    @Override
    public AdminEventQuery authClient(String authClientId) {
        query.put("authClientId", authClientId);
        return this;
    }

    @Override
    public AdminEventQuery authUser(String authUserId) {
        query.put("authUserId", authUserId);
        return this;
    }

    @Override
    public AdminEventQuery authIpAddress(String ipAddress) {
        query.put("authIpAddress", ipAddress);
        return this;
    }
    
    @Override
    public AdminEventQuery resourcePath(String resourcePath) {
        query.put("resourcePath", Pattern.compile(resourcePath));
        return this;
    }

    @Override
    public AdminEventQuery fromTime(Date fromTime) {
        BasicDBObject time = query.containsField("time") ? (BasicDBObject) query.get("time") : new BasicDBObject();
        time.append("$gte", fromTime.getTime());
        query.put("time", time);
        return this;
    }

    @Override
    public AdminEventQuery toTime(Date toTime) {
        BasicDBObject time = query.containsField("time") ? (BasicDBObject) query.get("time") : new BasicDBObject();
        time.append("$lte", toTime.getTime());
        query.put("time", time);
        return this;
    }

    @Override
    public AdminEventQuery firstResult(int firstResult) {
        this.firstResult = firstResult;
        return this;
    }

    @Override
    public AdminEventQuery maxResults(int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    @Override
    public List<AdminEvent> getResultList() {
        DBCursor cur = audit.find(query).sort(new BasicDBObject("time", -1));
        if (firstResult != null) {
            cur.skip(firstResult);
        }
        if (maxResults != null) {
            cur.limit(maxResults);
        }

        List<AdminEvent> events = new LinkedList<AdminEvent>();
        while (cur.hasNext()) {
            events.add(MongoEventStoreProvider.convertAdminEvent((BasicDBObject) cur.next()));
        }

        return events;
    }

}
