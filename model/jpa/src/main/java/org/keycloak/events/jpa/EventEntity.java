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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Entity
@Table(name="EVENT_ENTITY")
public class EventEntity {

    @Id
    @Column(name="ID", length = 36)
    private String id;

    @Column(name="EVENT_TIME")
    private long time;

    @Column(name="TYPE")
    private String type;

    @Column(name="REALM_ID")
    private String realmId;

    @Column(name="CLIENT_ID")
    private String clientId;

    @Column(name="USER_ID")
    private String userId;

    @Column(name="SESSION_ID")
    private String sessionId;

    @Column(name="IP_ADDRESS")
    private String ipAddress;

    @Column(name="ERROR")
    private String error;

    // This is the legacy field which is kept here to be able to read old events without the need to migrate them
    @Column(name="DETAILS_JSON", length = 2550)
    private String detailsJson;

    @Column(name="DETAILS_JSON_LONG_VALUE")
    private String detailsJsonLongValue;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

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

    public String getDetailsJson() {
        return detailsJsonLongValue != null ? detailsJsonLongValue : detailsJson;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJsonLongValue = detailsJson;
    }

}
