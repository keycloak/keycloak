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

package org.keycloak.models.jpa.session;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NamedQueries({
        @NamedQuery(name="deleteClientSessionsByRealm", query="delete from PersistentClientSessionEntity sess where sess.userSessionId IN (select u.userSessionId from PersistentUserSessionEntity u where u.realmId = :realmId)"),
        @NamedQuery(name="deleteClientSessionsByClient", query="delete from PersistentClientSessionEntity sess where sess.clientId = :clientId"),
        @NamedQuery(name="deleteClientSessionsByUser", query="delete from PersistentClientSessionEntity sess where sess.userSessionId IN (select u.userSessionId from PersistentUserSessionEntity u where u.userId = :userId)"),
        @NamedQuery(name="deleteClientSessionsByUserSession", query="delete from PersistentClientSessionEntity sess where sess.userSessionId = :userSessionId and sess.offline = :offline"),
        @NamedQuery(name="deleteDetachedClientSessions", query="delete from PersistentClientSessionEntity sess where sess.userSessionId NOT IN (select u.userSessionId from PersistentUserSessionEntity u)"),
        @NamedQuery(name="findClientSessionsByUserSession", query="select sess from PersistentClientSessionEntity sess where sess.userSessionId=:userSessionId and sess.offline = :offline"),
        @NamedQuery(name="findClientSessionsByUserSessions", query="select sess from PersistentClientSessionEntity sess where sess.offline = :offline and sess.userSessionId IN (:userSessionIds) order by sess.userSessionId"),
        @NamedQuery(name="updateClientSessionsTimestamps", query="update PersistentClientSessionEntity c set timestamp = :timestamp"),
})
@Table(name="OFFLINE_CLIENT_SESSION")
@Entity
@IdClass(PersistentClientSessionEntity.Key.class)
public class PersistentClientSessionEntity {

    @Id
    @Column(name="CLIENT_SESSION_ID", length = 36)
    protected String clientSessionId;

    @Column(name = "USER_SESSION_ID", length = 36)
    protected String userSessionId;

    @Column(name="CLIENT_ID", length = 36)
    protected String clientId;

    @Column(name="TIMESTAMP")
    protected int timestamp;

    @Id
    @Column(name = "OFFLINE_FLAG")
    protected String offline;

    @Column(name="DATA")
    protected String data;

    public String getClientSessionId() {
        return clientSessionId;
    }

    public void setClientSessionId(String clientSessionId) {
        this.clientSessionId = clientSessionId;
    }

    public String getUserSessionId() {
        return userSessionId;
    }

    public void setUserSessionId(String userSessionId) {
        this.userSessionId = userSessionId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public String getOffline() {
        return offline;
    }

    public void setOffline(String offline) {
        this.offline = offline;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public static class Key implements Serializable {

        protected String clientSessionId;

        protected String offline;

        public Key() {
        }

        public Key(String clientSessionId, String offline) {
            this.clientSessionId = clientSessionId;
            this.offline = offline;
        }

        public String getClientSessionId() {
            return clientSessionId;
        }

        public String getOffline() {
            return offline;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (this.clientSessionId != null ? !this.clientSessionId.equals(key.clientSessionId) : key.clientSessionId != null) return false;
            if (this.offline != null ? !this.offline.equals(key.offline) : key.offline != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = this.clientSessionId != null ? this.clientSessionId.hashCode() : 0;
            result = 31 * result + (this.offline != null ? this.offline.hashCode() : 0);
            return result;
        }
    }
}
