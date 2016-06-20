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

package org.keycloak.examples.providers.events;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;

/**
 * @author <a href="mailto:giriraj.sharma27@gmail.com">Giriraj Sharma</a>
 */
public class MemAdminEventQuery implements AdminEventQuery {
    
    private List<AdminEvent> adminEvents;

    private int first;
    private int max;

    public MemAdminEventQuery(List<AdminEvent> events) {
        this.adminEvents = events;
    }


    @Override
    public AdminEventQuery realm(String realmId) {
        Iterator<AdminEvent> itr = adminEvents.iterator();
        while (itr.hasNext()) {
            if (!itr.next().getRealmId().equals(realmId)) {
                itr.remove();
            }
        }
        return this;
    }

    @Override
    public AdminEventQuery operation(OperationType... operations) {
        Iterator<AdminEvent> itr = this.adminEvents.iterator();
        while (itr.hasNext()) {
            AdminEvent next = itr.next();
            boolean include = false;
            for (OperationType e : operations) {
                if (next.getOperationType().equals(e)) {
                    include = true;
                    break;
                }
            }
            if (!include) {
                itr.remove();
            }
        }
        return this;
    }

    @Override
    public AdminEventQuery resourceType(ResourceType... resourceTypes) {

        Iterator<AdminEvent> itr = this.adminEvents.iterator();
        while (itr.hasNext()) {
            AdminEvent next = itr.next();
            boolean include = false;
            for (ResourceType e : resourceTypes) {
                if (next.getResourceType().equals(e)) {
                    include = true;
                    break;
                }
            }
            if (!include) {
                itr.remove();
            }
        }

        return this;
    }
    
    @Override
    public AdminEventQuery authRealm(String authRealmId) {
        Iterator<AdminEvent> itr = adminEvents.iterator();
        while (itr.hasNext()) {
            if (!itr.next().getAuthDetails().getRealmId().equals(authRealmId)) {
                itr.remove();
            }
        }
        return this;
    }

    @Override
    public AdminEventQuery authClient(String authClientId) {
        Iterator<AdminEvent> itr = adminEvents.iterator();
        while (itr.hasNext()) {
            if (!itr.next().getAuthDetails().getClientId().equals(authClientId)) {
                itr.remove();
            }
        }
        return this;
    }

    @Override
    public AdminEventQuery authUser(String authUserId) {
        Iterator<AdminEvent> itr = adminEvents.iterator();
        while (itr.hasNext()) {
            if (!itr.next().getAuthDetails().getUserId().equals(authUserId)) {
                itr.remove();
            }
        }
        return this;
    }

    @Override
    public AdminEventQuery authIpAddress(String ipAddress) {
        Iterator<AdminEvent> itr = adminEvents.iterator();
        while (itr.hasNext()) {
            if (!itr.next().getAuthDetails().getIpAddress().equals(ipAddress)) {
                itr.remove();
            }
        }
        return this;
    }

    @Override
    public AdminEventQuery resourcePath(String resourcePath) {
        Iterator<AdminEvent> itr = this.adminEvents.iterator();
        while (itr.hasNext()) {
            if(!Pattern.compile(resourcePath).matcher(itr.next().getResourcePath()).find()) {
                itr.remove();
            }
        }
        return (AdminEventQuery) this;
    }

    @Override
    public AdminEventQuery fromTime(Date fromTime) {
        Iterator<AdminEvent> itr = this.adminEvents.iterator();
        while (itr.hasNext()) {
            if (!(itr.next().getTime() >= fromTime.getTime())) {
                itr.remove();
            }
        }
        return this;
    }

    @Override
    public AdminEventQuery toTime(Date toTime) {
        Iterator<AdminEvent> itr = this.adminEvents.iterator();
        while (itr.hasNext()) {
            if (!(itr.next().getTime() <= toTime.getTime())) {
                itr.remove();
            }
        }
        return this;
    }

    @Override
    public AdminEventQuery firstResult(int result) {
        this.first = result;
        return this;
    }

    @Override
    public AdminEventQuery maxResults(int results) {
        this.max = results;
        return this;
    }

    @Override
    public List<AdminEvent> getResultList() {
        if (adminEvents.size() < first) {
            return Collections.emptyList();
        }
        int end = first + max <= adminEvents.size() ? first + max : adminEvents.size();

        return adminEvents.subList(first, end);
    }
        
}
