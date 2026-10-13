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

package org.keycloak.events;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Event {

    private String id;

    private long time;

    private EventType type;

    private String realmId;
    private String realmName;

    private String clientId;

    private String userId;

    private String sessionId;

    private String ipAddress;

    private String error;

    private Map<String, String> details;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = maxLength(realmId, 255);
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = maxLength(clientId, 255);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = maxLength(userId, 255);
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = maxLength(sessionId, 255);
    }

    /**
     * Note: will not be an address when a proxy does not provide a valid one
     *
     * @return the ip address
     */
    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    @Override
    public Event clone() {
        Event clone = new Event();
        clone.id = id;
        clone.time = time;
        clone.type = type;
        clone.realmId = realmId;
        clone.realmName = realmName;
        clone.clientId = clientId;
        clone.userId = userId;
        clone.sessionId = sessionId;
        clone.ipAddress = ipAddress;
        clone.error = error;
        clone.details = details != null ? new HashMap<>(details) : null;
        return clone;
    }

    static String maxLength(String string, int length){
        if (string != null && string.length() > length) {
            return string.substring(0, length - 1);
        }
        return string;
    }

}
